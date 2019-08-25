/*
 *  Copyright (c) xlightweb.org, 2008 - 2010. All rights reserved.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Please refer to the LGPL license at: http://www.gnu.org/copyleft/lesser.txt
 * The latest copy of this software may be found on http://www.xlightweb.org/
 */
package org.xlightweb;


import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;


import org.xlightweb.BodyDataSource;
import org.xlightweb.BodyDataSink;
import org.xlightweb.HttpRequestHeader;
import org.xlightweb.HttpResponse;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.IBodyDataHandler;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.NonBlockingBodyDataSource;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.ILifeCycle;
import org.xsocket.connection.IConnection.FlushMode;





/**
*
* @author grro@xlightweb.org
*/
public final class ClientHealthyCheckTest  {


	@Test
	public void testSimple() throws Exception {

		Context rootCtx = new Context("");
		
		// set heartbeat support 
		HeartbeatServiceHandler heartbeatServiceHandler = new HeartbeatServiceHandler();
		rootCtx.addHandler("/Heartbeat/*", heartbeatServiceHandler);
		
		// set business handler
		rootCtx.addHandler("/*", new BusinessHandler());
		
		// run the server
		HttpServer server = new HttpServer(rootCtx);
		server.start();
		
		
		// heartbeat link of good clients
		for (int i = 0; i < 5; i++) {
			new HeartbeatLink("localhost", server.getLocalPort(), true).open();
		}
		
		// b link of bad clients
		for (int i = 0; i < 3; i++) {
			new HeartbeatLink("localhost", server.getLocalPort(), false).open();
		}
		
		
		QAUtil.sleep(5000);
		
		// validate statistics
		int size = heartbeatServiceHandler.getRegistered().size();
		if (size != 5) {
			String msg = "size registered is " + size + "expected is 5";
			System.out.println(msg);;
			Assert.fail(msg);
		}
		
		int dsize = heartbeatServiceHandler.getCountDeregistered();
		if (dsize != 3) {
			String msg = "size deregistered is " + size + "expected is 3";
			System.out.println(msg);;
			Assert.fail(msg);
		}
		
		
		server.close();
	}
	

	
	private static final class HeartbeatLink implements IHttpResponseHandler {
		
		
		private HttpClientConnection con  = null;
		private BodyDataSink outChannel = null; 
		private boolean isGood = true;

		
		public HeartbeatLink(String host, int port, boolean isGood) throws IOException {
			this.isGood = isGood;
			
			con = new HttpClientConnection(host, port);
		}
		
		public void open() throws IOException {
			outChannel = con.send(new HttpRequestHeader("POST", "/Heartbeat/RegisterClient", "text/plain; charset=ISO-8859-1"), this);
			outChannel.setFlushmode(FlushMode.ASYNC);
			outChannel.flush();
		}
		
		
		public void onResponse(IHttpResponse response) throws IOException {
			
			IBodyDataHandler heartbeatHandler = new IBodyDataHandler() {
				
				public boolean onData(NonBlockingBodyDataSource inChannel) throws BufferUnderflowException {
					
					try {
						String req = inChannel.readStringByDelimiter("\r\n");
						
						// is good case -> send pong, is bad case do nothing (simulating dead client) 
						if (isGood) {
							if (req.equals("ping")) {
								outChannel.write("pong\r\n");
							}
						}
					} catch (IOException ioe) {
						// todo handle
					}
					
					return true;
				}
			};
			
			response.getNonBlockingBody().setDataHandler(heartbeatHandler);
		}
		
		public void onException(IOException ioe) {
			// todo handle
		}
	}

	
	

	private static final class HeartbeatServiceHandler implements IHttpRequestHandler, ILifeCycle {
		
		private ArrayList<Link> links = new ArrayList<Link>();
		private volatile boolean isWatchdogOpen = true;
		private int countDeregistered = 0; 
		
		private Runnable watchdog = null;
		
		
		
		
		public void onInit() {
			
			watchdog = new Runnable() {
			
				@SuppressWarnings("unchecked")
				public void run() {
					
					while (isWatchdogOpen) {
						
						ArrayList<Link> linksCopy = null;
						synchronized (links) {
							linksCopy = (ArrayList<Link>) links.clone();
						}
						
						for (Link link : linksCopy) {	
							try {
								link.getOutChannel().write("ping\r\n");
								String response = link.getInChannel().readStringByDelimiter("\r\n");
								if (!response.equals("pong")) {
									deregister(link);
								}
							} catch (IOException ioe) {
								deregister(link);
							}
						}
						
						try {
							Thread.sleep(200);  // check period 
						} catch (InterruptedException ignore) { }
						
					}			
				}
			};
			
			Thread t = new Thread(watchdog);
			t.setDaemon(true);
			t.start();
		}
		
		
		public void onDestroy() throws IOException {
			isWatchdogOpen = false;
		}
		
		
	
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			IHttpRequest request = exchange.getRequest();
			
			// only POST is supported
			if (request.getMethod().equalsIgnoreCase("POST")) {
				
				if (request.getRequestURI().endsWith("/RegisterClient")) {
					BodyDataSource inChannel = request.getBody();
					inChannel.setReceiveTimeoutSec(1);
					
					BodyDataSink outChannel = exchange.send(new HttpResponseHeader(200, "text/plain"));
					outChannel.setFlushmode(FlushMode.ASYNC);
					outChannel.flush();
					
					register(new Link(outChannel, inChannel));
				} else {
					exchange.sendError(404);
				}
				
				
			// ... and nothing else
			} else {
				exchange.sendError(404);
			}
		}
		
		
		private void register(Link link) {
			synchronized (links) {
				links.add(link);
			}
		}
		
		private void deregister(Link link) {
			synchronized (links) {
				boolean deregistered = links.remove(link);
				if (deregistered) {
					countDeregistered++;
				}
			}
		}
		
		List<Link> getRegistered() {
			return Collections.unmodifiableList(links);
		}
		
		int getCountDeregistered() {
			return countDeregistered;		
		}	
	}
	
		
		
	private static final class Link {
		
		private BodyDataSink outChannel = null;
		private BodyDataSource inChannel = null;
		
		Link(BodyDataSink outChannel, BodyDataSource inChannel) {
			this.outChannel = outChannel;
			this.inChannel = inChannel;
			
		}

		public BodyDataSink getOutChannel() {
			return outChannel;
		}

		public BodyDataSource getInChannel() {
			return inChannel;
		}
	}
	
	

	
	private static final class BusinessHandler implements IHttpRequestHandler {
		
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			exchange.send(new HttpResponse(200, "text/plain", "it works"));						
		}
	}

}

