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

import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.BodyDataSink;
import org.xlightweb.HttpResponse;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.IHttpConnectHandler;
import org.xlightweb.IHttpConnection;
import org.xlightweb.IHttpDisconnectHandler;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.ILifeCycle;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.ConnectionUtils;





/**
*
* @author grro@xlightweb.org
*/
public final class ServerHandlerTest  {

	
	
	@Test
	public void test500Response() throws Exception {
		HttpServer server = new HttpServer(new ErrorResponseHandler());
		server.start();
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		Assert.assertEquals(500, response.getStatus());
		
		httpClient.close();
		server.close();
	}
	

	@Test
	public void testLifeCycle() throws Exception {
		
	    Thread.currentThread().setName("testmain");
	    
		MultithreadedServerHandler hdl = new MultithreadedServerHandler(); 
		
		IServer server = new HttpServer(hdl);
		ConnectionUtils.start(server);

		Assert.assertEquals(1, hdl.getCountOnInitCalled());
		Assert.assertEquals("testmain", hdl.getOnInitThreadname());
		
		server.close();
		
		QAUtil.sleep(200);
		
		Assert.assertEquals(1, hdl.getCountOnDestroyCalled());
		Assert.assertEquals(Thread.currentThread().getName(), hdl.getOnDestroyThreadname());
	}
	
	
	@Test
	public void testConnectionMultithreaded() throws Exception {
		
		MultithreadedServerHandler hdl = new MultithreadedServerHandler(); 
		
		HttpServer server = new HttpServer(hdl);
		server.setConnectionTimeoutMillis(1 * 1000);
		server.addConnectionHandler(hdl);
		server.start();
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		
		QAUtil.sleep(1000);

		Assert.assertEquals(1, hdl.getCountOnConnectCalled());
		Assert.assertTrue(hdl.getOnConnectThreadName().startsWith("xWorker"));

		
		QAUtil.sleep(1500);
		
		Assert.assertEquals(1, hdl.getCountOnDisconnectCalled());
		Assert.assertFalse(hdl.getOnDisconnectThreadName().startsWith("xIoTimer"));


		con.close();
		server.close();
	}

	
	
	
	@Test
	public void testConnectionNonthreaded() throws Exception {
		
		NonThreadedServerHandler hdl = new NonThreadedServerHandler(); 
		
		HttpServer server = new HttpServer(hdl);
		server.setConnectionTimeoutMillis(1 * 1000);
		server.addConnectionHandler(hdl);
		server.start();
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		
		QAUtil.sleep(1000);

		Assert.assertEquals(1, hdl.getCountOnConnectCalled());
		Assert.assertFalse(hdl.getOnConnectThreadName().startsWith("xWorker"));

		
		QAUtil.sleep(1000);
		
		Assert.assertEquals(1, hdl.getCountOnDisconnectCalled());
	      Assert.assertFalse(hdl.getOnConnectThreadName().startsWith("xWorker"));


		con.close();
		server.close();
	}

	
	
	@Test
	public void testRequestMultithreaded() throws Exception {
		MultithreadedServerHandler hdl = new MultithreadedServerHandler(); 
		
		HttpServer server = new HttpServer(hdl);
		server.setRequestTimeoutMillis(1000);
		server.addConnectionHandler(hdl);
		server.start();
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		Assert.assertTrue(header.indexOf("200") != -1);
		
		
		QAUtil.sleep(100);

		Assert.assertEquals(1, hdl.getCountOnRequestCalled());
		Assert.assertTrue(hdl.getOnRequestThreadname().startsWith("xWorker"));

		QAUtil.sleep(1500);
		
		Assert.assertEquals(1, hdl.getCountOnRequestTimeoutCalled());
		Assert.assertTrue(hdl.getOnRequestTimeoutThreadname().startsWith("xWorker"));

		Assert.assertEquals(1, hdl.getCountOnDisconnectCalled());
		Assert.assertTrue(hdl.getOnDisconnectThreadName().startsWith("xWorker"));


		con.close();
		server.close();
	}

	
	@Test
	public void testInvokeOnMessage() throws Exception {
		InvokeOnMessageRequestHandler hdl = new InvokeOnMessageRequestHandler(); 
		
		HttpServer server = new HttpServer(hdl);
		server.start();
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("POST / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "Content-Length: 5\r\n" +
				  "\r\n" +
				  "123");
		
		QAUtil.sleep(300);
		
		Assert.assertNull(hdl.getRequest());
		
		con.write("45");
		QAUtil.sleep(1000);
		
		Assert.assertNotNull(hdl.getRequest());
		

		con.close();
		server.close();
	}

	
	@Test
	public void testRequestNonthreaded() throws Exception {
		
		NonThreadedServerHandler hdl = new NonThreadedServerHandler(); 
		
		HttpServer server = new HttpServer(hdl);
		server.setRequestTimeoutMillis(1000);
		server.addConnectionHandler(hdl);
		server.start();
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		Assert.assertTrue(header.indexOf("200") != -1);
		
		
		QAUtil.sleep(250);

		Assert.assertEquals(1, hdl.getCountOnRequestCalled());
		Assert.assertTrue(hdl.getOnRequestThreadname().startsWith("xDispatcher"));

		QAUtil.sleep(1500);
		
		Assert.assertEquals(1, hdl.getCountOnRequestTimeoutCalled());
		Assert.assertTrue(hdl.getOnRequestTimeoutThreadname().equals("xHttpTimer"));

		Assert.assertEquals(1, hdl.getCountOnDisconnectCalled());


		con.close();
		server.close();
	}

	

	
	private static class InvokeOnMessageRequestHandler implements IHttpRequestHandler {
		
		private IHttpRequest request = null;
		
		@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			IHttpRequest request = exchange.getRequest();
			this.request = request;
			
		}
		
		IHttpRequest getRequest() {
			return request;
		}
	}
	
	
	
	private static final class MultithreadedServerHandler implements IHttpRequestHandler, IHttpConnectHandler, IHttpDisconnectHandler, IHttpRequestTimeoutHandler, ILifeCycle {
		
		private int countOnInitCalled = 0;
		private String onInitThreadname = null;
		private int countOnDestroyCalled = 0;
		private String onDestroyThreadname = null;
		private int countOnConnectCalled = 0;
		private String onConnectThreadName = null;
		private int countOnDisconnectCalled = 0;
		private String onDisconnectThreadName = null;
		private int countOnRequestCalled = 0;
		private String onRequestThreadname = null;
		private int countOnRequestTimeoutCalled = 0;
		private String onRequestTimeoutThreadname = null;
		
	
		public void onInit() {
			countOnInitCalled++;
			onInitThreadname = Thread.currentThread().getName();
		}
	
		public void onDestroy() throws IOException {
			countOnDestroyCalled++;
			onDestroyThreadname = Thread.currentThread().getName();
		}
		
		
		public boolean onConnect(IHttpConnection httpConnection) throws IOException {
			countOnConnectCalled++;
			onConnectThreadName = Thread.currentThread().getName();
			return true;
		}
		
		public boolean onDisconnect(IHttpConnection httpConnection) throws IOException {
			countOnDisconnectCalled++;
			onDisconnectThreadName = Thread.currentThread().getName();
			return true;
		}
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			countOnRequestCalled++;
			onRequestThreadname = Thread.currentThread().getName();
			
			exchange.send(new HttpResponse(200));
		}

		public boolean onRequestTimeout(IHttpConnection connection) throws IOException {
			countOnRequestTimeoutCalled++;
			onRequestTimeoutThreadname = Thread.currentThread().getName();
			return false;
		}
		

		public int getCountOnInitCalled() {
			return countOnInitCalled;
		}

		public String getOnInitThreadname() {
			return onInitThreadname;
		}

		public int getCountOnDestroyCalled() {
			return countOnDestroyCalled;
		}

		public String getOnDestroyThreadname() {
			return onDestroyThreadname;
		}

		public int getCountOnConnectCalled() {
			return countOnConnectCalled;
		}

		public String getOnConnectThreadName() {
			return onConnectThreadName;
		}

		public int getCountOnDisconnectCalled() {
			return countOnDisconnectCalled;
		}

		public String getOnDisconnectThreadName() {
			return onDisconnectThreadName;
		}

		public int getCountOnRequestCalled() {
			return countOnRequestCalled;
		}

		public String getOnRequestThreadname() {
			return onRequestThreadname;
		}

		public int getCountOnRequestTimeoutCalled() {
			return countOnRequestTimeoutCalled;
		}

		public String getOnRequestTimeoutThreadname() {
			return onRequestTimeoutThreadname;
		}
	}
	
	
	@Execution(Execution.NONTHREADED)
	private static final class NonThreadedServerHandler implements IHttpRequestHandler, IHttpConnectHandler, IHttpDisconnectHandler, IHttpRequestTimeoutHandler, ILifeCycle {
		
		private int countOnInitCalled = 0;
		private String onInitThreadname = null;
		private int countOnDestroyCalled = 0;
		private String onDestroyThreadname = null;
		private int countOnConnectCalled = 0;
		private String onConnectThreadName = null;
		private int countOnDisconnectCalled = 0;
		private String onDisconnectThreadName = null;
		private int countOnRequestCalled = 0;
		private String onRequestThreadname = null;
		private int countOnRequestTimeoutCalled = 0;
		private String onRequestTimeoutThreadname = null;
		
	
		public void onInit() {
			countOnInitCalled++;
			onInitThreadname = Thread.currentThread().getName();
		}
	
		public void onDestroy() throws IOException {
			countOnDestroyCalled++;
			onDestroyThreadname = Thread.currentThread().getName();
		}
		
		
		public boolean onConnect(IHttpConnection httpConnection) throws IOException {
			countOnConnectCalled++;
			onConnectThreadName = Thread.currentThread().getName();
			return true;
		}
		
		public boolean onDisconnect(IHttpConnection httpConnection) throws IOException {
			countOnDisconnectCalled++;
			onDisconnectThreadName = Thread.currentThread().getName();
			return false;
		}
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			countOnRequestCalled++;
			onRequestThreadname = Thread.currentThread().getName();
			
			exchange.send(new HttpResponse(200));
		}

		public boolean onRequestTimeout(IHttpConnection connection) throws IOException {
			countOnRequestTimeoutCalled++;
			onRequestTimeoutThreadname = Thread.currentThread().getName();
			return false;
		}
		

		public int getCountOnInitCalled() {
			return countOnInitCalled;
		}

		public String getOnInitThreadname() {
			return onInitThreadname;
		}

		public int getCountOnDestroyCalled() {
			return countOnDestroyCalled;
		}

		public String getOnDestroyThreadname() {
			return onDestroyThreadname;
		}

		public int getCountOnConnectCalled() {
			return countOnConnectCalled;
		}

		public String getOnConnectThreadName() {
			return onConnectThreadName;
		}

		public int getCountOnDisconnectCalled() {
			return countOnDisconnectCalled;
		}

		public String getOnDisconnectThreadName() {
			return onDisconnectThreadName;
		}

		public int getCountOnRequestCalled() {
			return countOnRequestCalled;
		}

		public String getOnRequestThreadname() {
			return onRequestThreadname;
		}

		public int getCountOnRequestTimeoutCalled() {
			return countOnRequestTimeoutCalled;
		}

		public String getOnRequestTimeoutThreadname() {
			return onRequestTimeoutThreadname;
		}
	}


	@Test
	public void testBound() throws Exception {
		
		IServer server = new HttpServer(new BoundServerHandler());
		ConnectionUtils.start(server);
	
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET /test HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		Assert.assertTrue(header.indexOf("200") != -1);		
		
		Assert.assertEquals("/test", con.readStringByLength(5));
		
		server.close();		
	}

	
	@Test
	public void testChunked() throws Exception {
		
		IServer server = new HttpServer(new ChunkedServerHandler());
		ConnectionUtils.start(server);
	
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET /test HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		Assert.assertTrue(header.indexOf("200") != -1);		

		String chunk = con.readStringByLength(3 + 7 + 3);		
		Assert.assertTrue(chunk.indexOf("/test") != -1);

		
		server.close();		
	}
	
	private static final class BoundServerHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			String msg = exchange.getRequest().getRequestURI();
			
			HttpResponseHeader resp = new HttpResponseHeader(200, "text/plain");
			resp.setContentLength(msg.length());
			
			BodyDataSink channel = exchange.send(resp, msg.length());
			channel.write(msg);
			channel.close();
		}
	}
	
	
	private static final class ChunkedServerHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			String msg = exchange.getRequest().getRequestURI();
			
			HttpResponseHeader resp = new HttpResponseHeader(200, "text/plain");
			
			BodyDataSink channel = exchange.send(resp);
			channel.write(msg);
			channel.close();
			
			
		}	
	}
	
	
	public static final class ErrorResponseHandler implements IHttpRequestHandler {

		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			exchange.send(new HttpResponse(500));
		}
	}
}

