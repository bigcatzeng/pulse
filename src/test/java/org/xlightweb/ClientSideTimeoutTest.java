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
import java.net.SocketTimeoutException;



import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.BodyDataSink;
import org.xlightweb.GetRequest;
import org.xlightweb.HttpRequestHeader;
import org.xlightweb.HttpResponse;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.IHttpSocketTimeoutHandler;
import org.xlightweb.InvokeOn;
import org.xlightweb.ReceiveTimeoutException;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IServer;





/**
*
* @author grro@xlightweb.org
*/
public final class ClientSideTimeoutTest  {
	
	
	public static void main(String[] args)  throws Exception {
		
		for (int i = 0; i < 1000; i++) {
			new ClientSideTimeoutTest().testBodyDataReceiveTimeout();
		}
	}
	

	@Test
	public void testConnectionTimeoutHandled() throws Exception {

		IServer server = new HttpServer(new ServerHandler3());
		ConnectionUtils.start(server);
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		con.setConnectionTimeoutMillis(1000);
		
		ResponseHandler respHdl = new ResponseHandler();
		HttpRequestHeader reqHdr = new HttpRequestHeader("POST", "/");

		BodyDataSink bodyDataSink = con.send(reqHdr, respHdl);
		bodyDataSink.write("er");
		
		QAUtil.sleep(1500);
		
		Assert.assertEquals(1, respHdl.getCountSocketException());
		Assert.assertEquals(0, respHdl.getCountIOException());
		
		con.close();
		server.close();
	}

	
	@Test
	public void testConnectionTimeout() throws Exception {

		IServer server = new HttpServer(new ServerHandler3());
		ConnectionUtils.start(server);
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		con.setConnectionTimeoutMillis(1000);
		
		ResponseHandler2 respHdl = new ResponseHandler2();
		HttpRequestHeader reqHdr = new HttpRequestHeader("POST", "/");

		BodyDataSink bodyDataSink = con.send(reqHdr, respHdl);
		bodyDataSink.write("er");
		
		QAUtil.sleep(2000);
		
		Assert.assertEquals(1, respHdl.getCountIOException());
		
		con.close();
		server.close();
	}


	@Test
	public void testIdleTimeout() throws Exception {

		IServer server = new HttpServer(new ServerHandler());
		ConnectionUtils.start(server);
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		con.setIdleTimeoutMillis(500);
		
		QAUtil.sleep(1000);
		Assert.assertFalse(con.isOpen());
		
		con.close();
		server.close();
	}
	
	
	@Test
	public void testIdleTimeout2() throws Exception {
		IServer server = new HttpServer(new ServerHandler());
		ConnectionUtils.start(server);
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		con.setIdleTimeoutMillis(500);
		
		GetRequest request = new GetRequest("/");
		request.setHeader("sleep-time", Integer.toString(1000));
		try {
			con.call(request);
			Assert.fail("SocketTimeoutException expected");
		} catch (IOException expected) { }
		
		con.close();
		server.close();
	}
 
	
	@Test
	public void testBodyDataReceiveTimeout() throws Exception {
		
		IServer server = new HttpServer(new ServerHandler2());
		server.start();
	
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		con.setBodyDataReceiveTimeoutMillis(1000);

		IHttpResponse response = con.call(new GetRequest("/?loops=3&waittime=200"));
		response.getBody().readString();
		
		response = con.call(new GetRequest("/?loops=1&waittime=20000"));
		
		try {
			response.getBody().readString();
			Assert.fail("ReceiveTimeoutException expected");
		} catch (ReceiveTimeoutException expected) { }

		
		con.close();
		server.close();
	}

	
	@Test
	public void testResponseTimeoutHandler() throws Exception {

		IServer server = new HttpServer(new ServerHandler());
		ConnectionUtils.start(server);
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		con.setResponseTimeoutMillis(1000);
		
		ResponseHandler respHdl = new ResponseHandler();
		HttpRequestHeader reqHdr = new HttpRequestHeader("GET", "/");
		reqHdr.setHeader("sleep-time", Integer.toString(1000));

		con.send(reqHdr, respHdl);
		
		QAUtil.sleep(1500);
		
		Assert.assertEquals(0, respHdl.getCountIOException());
		Assert.assertEquals(1, respHdl.getCountSocketException());
		
		con.close();
		server.close();
	}

	
	
	@Test
	public void testResponseTimeout() throws Exception {

		IServer server = new HttpServer(new ServerHandler());
		ConnectionUtils.start(server);
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		con.setResponseTimeoutMillis(1000);
		
		ResponseHandler2 respHdl = new ResponseHandler2();
		HttpRequestHeader reqHdr = new HttpRequestHeader("GET", "/");
		reqHdr.setHeader("sleep-time", Integer.toString(1000));

		con.send(reqHdr, respHdl);
		
		QAUtil.sleep(1500);
		
		Assert.assertEquals(1, respHdl.getCountIOException());
		
		con.close();
		server.close();
	}
	
	
	
	private static final class ResponseHandler implements IHttpResponseHandler, IHttpSocketTimeoutHandler {
		
		private int countIOException = 0;
		private int countSocketException = 0;
		
		public void onResponse(IHttpResponse response) throws IOException {
		}
		
		public void onException(IOException ioe) {
			countIOException++;
		}
		
		public void onException(SocketTimeoutException stoe) {
			countSocketException++;
		}
		
		
		
		int getCountIOException() {
			return countIOException;
		}
		
		int getCountSocketException() {
			return countSocketException;
		}

	}
	
	
	private static final class ResponseHandler2 implements IHttpResponseHandler {
		
		private int countIOException = 0;
		
		public void onResponse(IHttpResponse response) throws IOException {
		}
		
		public void onException(IOException ioe) {
			countIOException++;
		}
		
		
		int getCountIOException() {
			return countIOException;
		}
	}
	
	
	
	private static final class ServerHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			IHttpRequest request = exchange.getRequest();
			if (request.getHeader("sleep-time") != null) {
				int sleepTime = Integer.parseInt(request.getHeader("sleep-time"));
				QAUtil.sleep(sleepTime);
			}
			 
			exchange.send(new HttpResponse(200, "text/plain", "OK"));
		}
	}
	
	private static final class ServerHandler2 implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			int loops = exchange.getRequest().getIntParameter("loops");
			int waittime = exchange.getRequest().getIntParameter("waittime");
			
			HttpResponseHeader header = new HttpResponseHeader(200, "text/plain");
			BodyDataSink bodyDataSink = exchange.send(header);
			
			for (int i = 0; i < loops; i++) {
				bodyDataSink.write("1234567890");
				QAUtil.sleep(waittime);
			}
			
			bodyDataSink.close();
		}
	}
	
	
	
	private static final class ServerHandler3 implements IHttpRequestHandler {
		
		@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
		public void onRequest(IHttpExchange exchange) throws IOException {

			exchange.send(new HttpResponse(200, "text/plain", "OK"));
		}
	}
}