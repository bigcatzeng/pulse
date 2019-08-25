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
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;

import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.IHttpSocketTimeoutHandler;
import org.xlightweb.InvokeOn;
import org.xlightweb.RequestHandlerChain;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IServerListener;
import org.xsocket.connection.IConnection.FlushMode;


 
public class ExampleHttpProxy { 


	public static void main(String[] args) throws Exception {
		
		
		
		if (args.length == 0) {
			System.out.println("usage java [options] org.xsocket.connection.http.Proxy <listen port>\r\n");
			System.out.println("where options include:");
			System.out.println("   -debug          activate debug");
			System.out.println("   -printMessages  prints the message to console");
			System.exit(-1);
		} 
		
		ArrayList<String> params = new ArrayList<String>();
		params.addAll(Arrays.asList(args));
		
		boolean isPrintMessagesActivated = false;
		

		
		for (String arg : args) {
			
			if (arg.equalsIgnoreCase("-printMessages")) {
				System.out.println("printing messages");
				params.remove(arg);
				isPrintMessagesActivated = true;
			
			
			} else if (arg.equalsIgnoreCase("-debug")) {
				params.remove(arg);
				
				System.setProperty(IHttpExchange.SHOW_DETAILED_ERROR_KEY, "true");

				
				FileHandler fh = new FileHandler("proxy.log", 500000, 2);
				fh.setLevel(Level.FINE);
				fh.setFormatter(new LogFormatter());

				
				ConsoleHandler ch = new ConsoleHandler();
				ch.setLevel(Level.FINE);
				ch.setFormatter(new LogFormatter());
				
				//Logger http = Logger.getLogger("proxy");
			    Logger http = Logger.getLogger("org.xlightweb");
				http.setLevel(Level.FINE);
				http.addHandler(ch);		
				
				Logger http2 = Logger.getLogger("org.xsocket.connection.NonBlockingConnectionPool");
				http2.setLevel(Level.FINE);
				http2.addHandler(ch);
			}
			
			
			if (arg.equalsIgnoreCase("-printMessage")) {
				params.remove(arg);
			}
		}
		
	
		new ExampleHttpProxy(Integer.parseInt(params.get(0)), isPrintMessagesActivated);
	}
	
	
	
	
	public ExampleHttpProxy(int listenport, boolean isPrintMessagesrActivated) throws IOException, JMException {
				
		int receiveTimeoutSec = 60;
		
		IHttpRequestHandler handler = null;
		
		final HttpClient httpClient = new HttpClient();
		httpClient.setMaxIdle(0);
//		httpClient.setResponseTimeoutSec(receiveTimeoutSec);
		
		if (isPrintMessagesrActivated) {
			RequestHandlerChain reqHdl = new RequestHandlerChain();
			reqHdl.addLast(new LogFilter());
			reqHdl.addLast(new ForwardHandler(httpClient));
			handler = reqHdl;
			
		} else {
			handler = new ForwardHandler(httpClient);
		}
		
		HttpServer proxy = new HttpServer(listenport, handler);
		httpClient.setWorkerpool(proxy.getWorkerpool());
		
		
		try {
			ConnectionUtils.registerMBean(httpClient);
		} catch (JMException ignore) { }

		
		IServerListener sl = new IServerListener() {
			
			public void onInit() {
			}
			
			public void onDestroy() throws IOException {
				httpClient.close();
			}
		};
		proxy.addListener(sl);

		
			
		proxy.setWorkerpool(Executors.newFixedThreadPool(10));
		proxy.setMaxTransactions(50);
		proxy.setRequestTimeoutMillis(receiveTimeoutSec * 5 * 1000);
		
		proxy.setFlushmode(FlushMode.ASYNC);

		ConnectionUtils.registerMBean(proxy);
		
		proxy.run();
	}	
	
	
	private static final class ForwardHandler implements IHttpRequestHandler {

		private HttpClient httpClient = null;
		
		
		public ForwardHandler(HttpClient httpClient) {
			this.httpClient = httpClient;
		}
		

		@Execution(Execution.MULTITHREADED)
		@InvokeOn(InvokeOn.HEADER_RECEIVED)
		public void onRequest(IHttpExchange exchange) throws IOException {

			IHttpRequest req = exchange.getRequest();

							
			if (req.getMethod().equalsIgnoreCase("CONNECT")) {
				exchange.sendError(501);
			}
			
			
			String path = req.getRequestUrl().getFile();
			URL target = new URL(path);
			
			req.setRequestUrl(target);
			
			
			
			// add via header
			req.addHeader("Via", "myProxy");
		
			
				
			// .. and forward the request
			try {
				httpClient.send(req, new ReverseHandler(exchange));
			} catch (ConnectException ce) {
				exchange.sendError(502, ce.getMessage());
			}
		}	
	}

	
	private static final class ReverseHandler implements IHttpResponseHandler, IHttpSocketTimeoutHandler {
		
		private IHttpExchange exchange = null;
		
			
		public ReverseHandler(IHttpExchange exchange) {
			this.exchange = exchange;
		}


		@Execution(Execution.NONTHREADED)
		@InvokeOn(InvokeOn.HEADER_RECEIVED)
		public void onResponse(IHttpResponse resp) throws IOException {
			
				
			// add via header
			resp.addHeader("Via", "myProxy");
			
			// 	return the response 
			exchange.send(resp);
		}

		@Execution(Execution.NONTHREADED)
		public void onException(IOException ioe) {
			exchange.sendError(500, ioe.toString());
		}
		
		@Execution(Execution.NONTHREADED)
		public void onException(SocketTimeoutException stoe) {
			exchange.sendError(504, stoe.toString());
		}
	}
}
