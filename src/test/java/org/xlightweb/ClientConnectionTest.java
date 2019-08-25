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
import java.net.InetAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.BodyDataSink;
import org.xlightweb.FutureResponseHandler;
import org.xlightweb.GetRequest;
import org.xlightweb.HttpRequest;
import org.xlightweb.HttpRequestHeader;
import org.xlightweb.HttpResponse;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpMessage;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.PostRequest;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.IDisconnectHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.NonBlockingConnection;
import org.xsocket.connection.IConnection.FlushMode;




/**
*
* @author grro@xlightweb.org
*/
public final class ClientConnectionTest  {

 
	@Test
	public void testGet() throws Exception {
	    
	    System.out.println("testGet");
		
		
		HttpServer server = new HttpServer(new RequestHandler());
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		IHttpResponse response = con.call(new GetRequest("/"));
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("it works", response.getBody().toString());
	
		Assert.assertTrue(con.isOpen());
		
		con.close();
		server.close();
	}
	
	

	@Test
	public void testPersistentConnection() throws Exception {
	    
	    System.out.println("testPersistentConnection");
		
		HttpServer server = new HttpServer(new RequestHandler());
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		for (int i = 0; i < 10; i++) {
			IHttpResponse response = con.call(new GetRequest("/"));
			
			Assert.assertEquals(200, response.getStatus());
			Assert.assertEquals("it works", response.getBody().toString());
		
			Assert.assertTrue(con.isOpen());
		}
			
		con.close();
		server.close();
	}

	
	
	
	
	@Test
	public void testNonPersistentConnection() throws Exception {
	    
	    System.out.println("testNonPersistentConnection");
		
		HttpServer server = new HttpServer(new RequestHandler());
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());

		GetRequest request = new GetRequest("/");
		request.setHeader("Connection", "close");
		IHttpResponse response = con.call(request);
			
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("it works", response.getBody().toString());
		
		QAUtil.sleep(500);
		Assert.assertFalse(con.isOpen());

		
		request = new GetRequest("/");
		try {
			con.call(request);
			Assert.fail("ClosedChannelException expected");
		} catch (ClosedChannelException expected) { }

		con.close();
		server.close();
	}
	
	
	
	@Test
	public void testGetWithConnectionCloseHeader() throws Exception {
	    
	    System.out.println("testGetWithConnectionCloseHeader");
		
		
		HttpServer server = new HttpServer(new RequestHandler());
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		GetRequest req = new GetRequest("/");
		req.setHeader("Connection", "close");
		
		IHttpResponse response = con.call(req);
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("it works", response.getBody().toString());
	
		QAUtil.sleep(500);
		Assert.assertFalse(con.isOpen());
		
		con.close();
		server.close();
	}
	
	
	
	@Test
	public void testPost() throws Exception {
	    
	    System.out.println("testPost");
		
		HttpServer server = new HttpServer(new RequestHandler());
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		IHttpResponse response = con.call(new PostRequest("/", "test/plain", "test"));
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("it works", response.getBody().toString());
	
		con.close();
		server.close();
	}
	
	

	@Test
	public void testPostBound() throws Exception {
	    System.out.println("testPostBound");
		
		HttpServer server = new HttpServer(new RequestHandler());
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		BodyDataSink bodyDataSink = con.send(new HttpRequestHeader("POST", "/"), 100, respHdl);
		bodyDataSink.write(QAUtil.generateByteArray(100));
		bodyDataSink.close();
		
		IHttpResponse response = respHdl.getResponse();
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("it works", response.getBody().toString());
	
		con.close();
		server.close();
	}
	
	

	@Test
	public void testPostChunked() throws Exception {
	    System.out.println("testPostChunked");
		
		HttpServer server = new HttpServer(new RequestHandler());
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		BodyDataSink bodyDataSink = con.send(new HttpRequestHeader("POST", "/"), respHdl);
		bodyDataSink.write(QAUtil.generateByteArray(100));
		bodyDataSink.close();
		
		IHttpResponse response = respHdl.getResponse();
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("it works", response.getBody().toString());
	
		con.close();
		server.close();
	}
		
	
	
	@Test
	public void testGetChunkedResponse() throws Exception {
	    System.out.println("testGetChunkedResponse");
		
		
		HttpServer server = new HttpServer(new RequestHandler2());
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		IHttpResponse response = con.call(new GetRequest("/"));
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("it works", response.getBody().toString());
	
		Assert.assertTrue(con.isOpen());
		
		con.close();
		server.close();
	}
	
	

	@Test
	public void testHead() throws Exception {
	    System.out.println("testHead");
		
		
		HttpServer server = new HttpServer(new RequestHandler3());
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		for (int i = 0; i < 5; i++) {
			HttpRequest request = new HttpRequest("HEAD", "/");
			IHttpResponse response = con.call(request);
			
			Assert.assertEquals(200, response.getStatus());
		}
	
		
		con.close();
		server.close();
	}
	
	

	@Test
	public void testTrace() throws Exception {
	    
	    System.out.println("testTrace");
		
		
		HttpServer server = new HttpServer(new RequestHandler3());
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		for (int i = 0; i < 5; i++) {
			HttpRequest request = new HttpRequest("TRACE", "/");
			IHttpResponse response = con.call(request);
			String body = response.getBody().readString();
			
			Assert.assertEquals(200, response.getStatus());
			Assert.assertTrue(body.indexOf("TRACE /") != -1);
		}
	
		
		con.close();
		server.close();
	}
	
	


	@Test
	public void testPut() throws Exception {
	    System.out.println("testPut");
		
		
		HttpServer server = new HttpServer(new RequestHandler3());
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		for (int i = 0; i < 5; i++) {
			HttpRequest request = new HttpRequest("PUT", "/", "text/plain", "test123456789");
			IHttpResponse response = con.call(request);
			
			Assert.assertEquals(201, response.getStatus());
		}
	
		
		con.close();
		server.close();
	}
	
	
	@Test
	public void testDelete() throws Exception {
	    
	    System.out.println("testDelete");
		
		
		HttpServer server = new HttpServer(new RequestHandler3());
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		for (int i = 0; i < 5; i++) {
			HttpRequest request = new HttpRequest("DELETE", "/someResource");
			IHttpResponse response = con.call(request);
			
			Assert.assertEquals(200, response.getStatus());
		}
	
		
		con.close();
		server.close();
	}
	
	
	@Test
	public void testOptions() throws Exception {
	    
	    System.out.println("testOptions");
		
		
		HttpServer server = new HttpServer(new RequestHandler3());
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		for (int i = 0; i < 5; i++) {
			HttpRequest request = new HttpRequest("OPTIONS", "/");
			IHttpResponse response = con.call(request);
			
			Assert.assertEquals(200, response.getStatus());
			Assert.assertTrue(response.getHeader("allow").indexOf("GET") != -1);
			
		}
	
		
		con.close();
		server.close();
	}

	
	

	@Test
	public void testConnect() throws Exception {
	    
	    System.out.println("testConnect");
			
		HttpServer securedServer = new HttpServer(0, new RequestHandler(), SSLTestContextFactory.getSSLContext(), true);
		securedServer.start();
		
		HttpServer proxy = new HttpServer(new Proxy());
		proxy.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", proxy.getLocalPort());
		
		// setup a tunnel
		HttpRequest request = new HttpRequest("CONNECT", "localhost:" + securedServer.getLocalPort());
		IHttpResponse response = con.call(request);
			
		Assert.assertEquals(200, response.getStatus());
		

		// send the requests 
		for (int i = 0; i < 10; i++) {
			response = con.call(new GetRequest("/"));
			Assert.assertEquals(200, response.getStatus());
			Assert.assertTrue(response.getBody().readString().indexOf("works") != -1);
		}
		
		con.close();
		securedServer.close();
	}

	
	

	private static final class RequestHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			exchange.send(new HttpResponse(200, "text/plain", "it works"));
		}
	}
	
	private static final class RequestHandler2 implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			BodyDataSink bodyDataSink = exchange.send(new HttpResponseHeader(200, "text/plain"));
			bodyDataSink.write("it works");
			bodyDataSink.close();
		}
	}
	
	
	
	private static final class RequestHandler3 implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			if (exchange.getRequest().getMethod().equals(IHttpMessage.HEAD_METHOD)) {
				exchange.send(new HttpResponse(200));
				
			} else if (exchange.getRequest().getMethod().equals(IHttpMessage.TRACE_METHOD)) {
				String requestHeader = exchange.getRequest().getRequestHeader().toString();
				exchange.send(new HttpResponse(200, "text/plain", requestHeader));
				
			} else if (exchange.getRequest().getMethod().equals(IHttpMessage.PUT_METHOD)) {
				exchange.send(new HttpResponse(201, "text/plain", "created"));

			} else if (exchange.getRequest().getMethod().equals(IHttpMessage.DELETE_METHOD)) {
				exchange.send(new HttpResponse(200, "text/plain", "deleted"));

			} else if (exchange.getRequest().getMethod().equals(IHttpMessage.OPTIONS_METHOD)) {
				HttpResponse response = new HttpResponse(200);
				response.addHeader("Allow", "GET,POST");
				exchange.send(response);
			}						
		}
	}
	
	
	private static final class Proxy implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			if (exchange.getRequest().getMethod().equals(IHttpMessage.CONNECT_METHOD)) {
				String target = exchange.getRequest().getRequestURI();
				
				String host = "";
				int port = 443;
				int idx = target.indexOf(":");
				if (idx != -1) {
					host = target.substring(0, idx);
					port = Integer.parseInt(target.substring(idx + 1, target.length()).trim());
				}
				
				
				INonBlockingConnection tcpConnection = exchange.getConnection().getUnderlyingTcpConnection();
				
				SSLClientToProxyHandler proxyHandler = new SSLClientToProxyHandler(host, port, tcpConnection);
				tcpConnection.setHandler(proxyHandler);
				
				HttpResponse response = new HttpResponse(200);
				exchange.send(response);
			}
		}
	}


	
	@Execution(Execution.NONTHREADED)
	private static class ProxyHandler implements IDataHandler, IDisconnectHandler {
		
	
		public boolean onDisconnect(INonBlockingConnection connection) throws IOException {
			INonBlockingConnection reverseConnection = (INonBlockingConnection) connection.getAttachment();
			if (reverseConnection != null) {
				connection.setAttachment(null);
				reverseConnection.close();
			}
			return true;
		}
		
		
		public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
			INonBlockingConnection forwardConnection = (INonBlockingConnection) connection.getAttachment();

			ByteBuffer[] data = connection.readByteBufferByLength(connection.available());
			forwardConnection.write(data);
			forwardConnection.flush();
			
			return true;
		}
	}


	
	@Execution(Execution.NONTHREADED)
	private static final class SSLClientToProxyHandler extends ProxyHandler {
		
		
		public SSLClientToProxyHandler(String forwardHost, int forwardPort, INonBlockingConnection clientToProxyConnection) throws IOException {
			clientToProxyConnection.setFlushmode(FlushMode.ASYNC); // set flush mode async for performance reasons
			clientToProxyConnection.setAutoflush(true);
			
			INonBlockingConnection proxyToServerConnection = new NonBlockingConnection(InetAddress.getByName(forwardHost), forwardPort, new ProxyHandler(), 60 * 1000, SSLTestContextFactory.getSSLContext(), true, clientToProxyConnection.getWorkerpool());
			
			proxyToServerConnection.setFlushmode(FlushMode.ASYNC); // set flush mode async for performance reasons
			proxyToServerConnection.setAutoflush(true);
			proxyToServerConnection.setAttachment(clientToProxyConnection);
			
			clientToProxyConnection.setAttachment(proxyToServerConnection);
		}
		
		
		public boolean onDisconnect(INonBlockingConnection clientToProxyConnection) throws IOException {
			return super.onDisconnect(clientToProxyConnection);
		}
		
		
		public boolean onData(INonBlockingConnection clientToProxyConnection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
			return super.onData(clientToProxyConnection);
		}
	}	

	
	
}