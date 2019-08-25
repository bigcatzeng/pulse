/*
 *  Copyright (c) xlightweb.org, 2006 - 2009. All rights reserved.
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

import java.io.File;

import java.io.IOException;


import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;


import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.DataConverter;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;
import org.xsocket.connection.IConnection.FlushMode;

 

/**
*
* @author grro@xlightweb.org
*/
public final class HttpProxyTest  {
  
	
	public static void main(String[] args) throws Exception {

		for (int i = 0; i < 1000; i ++) {
			new HttpProxyTest().testContinueSupportingProxy();
		}
	}

	
	
	@BeforeClass
	public static void setUp() {
	    System.setProperty("org.xlightweb.showDetailedError", "true");
	}
		
	
	@Test
	public void testNonPersistentClientProxyConnection() throws Exception {
		
		System.out.println("testNonPersistentClientProxyConnection");
		
		HttpServer server = new HttpServer(new ItWorksRequestHandler());
		server.start();
		
		HttpProxy proxy =  new HttpProxy(0, "localhost", server.getLocalPort(), false, 60, 30);
		proxy.start();

		
		HttpClient httpClient = new HttpClient();
		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + proxy.getLocalPort() + "/test", "text/plain");
		BodyDataSink bodyDataSink = httpClient.send(header, 1000, respHdl);
		
		bodyDataSink.write(QAUtil.generateByteArray(500));
		QAUtil.sleep(200);
		
		bodyDataSink.write(QAUtil.generateByteArray(500));
		bodyDataSink.close();
		
		IHttpResponse response = respHdl.getResponse();
		Assert.assertEquals(200, response.getStatus());
		
		QAUtil.sleep(1000);
		
		// proxy -> server connection is not persistent
		Assert.assertEquals(0, httpClient.getNumActive());
		Assert.assertEquals(0, httpClient.getNumIdle());
		Assert.assertEquals(1, httpClient.getNumDestroyed());

		
		
		httpClient.close();
		proxy.close();
		server.close();
	}

	
	
	
	@Test
	public void testServersideDataReceiveTimeout() throws Exception {
		System.out.println("testServersideDataReceiveTimeout");
		
		ItWorksRequestHandler itWorksHandler = new ItWorksRequestHandler();
		HttpServer server = new HttpServer(itWorksHandler);
		server.setBodyDataReceiveTimeoutMillis(500);
		server.addConnectionHandler(itWorksHandler);
		server.start();
		
		HttpProxy proxy =  new HttpProxy(0, "localhost", server.getLocalPort(), true, 60, 30);
		proxy.start();

		
		HttpClient httpClient = new HttpClient();
		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		BodyDataSink bodyDataSink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + proxy.getLocalPort() + "/test", "text/plain"), 1000, respHdl);
		
		bodyDataSink.write(QAUtil.generateByteArray(500));
		QAUtil.sleep(2000);
		
		
		Assert.assertNotNull(itWorksHandler.getException());
		Assert.assertEquals(500, respHdl.getResponse().getStatus());

			
		proxy.close();
		server.close();
	}

	@Test
	public void testServersideClose() throws Exception {

		System.out.println("testServersideClose");
		
		HttpServer server = new HttpServer(new ClosingServer());
		server.start();
		
		HttpProxy proxy =  new HttpProxy(0, "localhost", server.getLocalPort(), true, Integer.MAX_VALUE, Integer.MAX_VALUE);
		proxy.start();

		HttpClientConnection con = new HttpClientConnection("localhost", proxy.getLocalPort());
		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		
		String msg = "test";
		BodyDataSink bodyDataSink = con.send(new HttpRequestHeader("POST", "http://localhost:" + proxy.getLocalPort() + "/test", "text/plain"), msg.getBytes().length, respHdl);
		bodyDataSink.write(msg);
		
		IHttpResponse response = respHdl.getResponse();
		Assert.assertEquals(500, response.getStatus());
		
		QAUtil.sleep(1000);
		Assert.assertFalse(con.isOpen());
	
			
		proxy.close();
		server.close();
	}

	
	@Test
	public void testServerReturns404() throws Exception {

		System.setProperty("org.xlightweb.showDetailedError", "true");
		
		System.out.println("testServerReturns404");
		
		HttpServer server = new HttpServer(new NotFoundServer());
		server.start();
		
		HttpProxy proxy =  new HttpProxy(0, "localhost", server.getLocalPort(), true, Integer.MAX_VALUE, Integer.MAX_VALUE);
		proxy.start();

		HttpClientConnection con = new HttpClientConnection("localhost", proxy.getLocalPort());

		IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		Assert.assertEquals(404, response.getStatus());
		
			
		proxy.close();
		server.close();
	}

	
	@Test
	public void testServerReturns404_2() throws Exception {
		System.setProperty("org.xlightweb.showDetailedError", "true");
		
		System.out.println("testServerReturns404_2");

		
		IDataHandler dh = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection con) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
				
				con.readStringByDelimiter("\r\n\r\n");
				con.write("HTTP/1.0 404 Not Found\r\n" +
						  "Server: me\r\n" +
						  "Date: Mon, 02 Feb 2009 15:56:01 GMT\r\n" +
						  "Connection: close\r\n" +
						  "Content-Type: text/html; charset=utf-8\r\n" +
						  "\r\n");
				
				return true;
			}
		};
		
		IServer server = new Server(dh);
		server.start();
		
		HttpProxy proxy =  new HttpProxy(0, "localhost", server.getLocalPort(), true, Integer.MAX_VALUE, 1000);
		proxy.start();

		HttpClientConnection con = new HttpClientConnection("localhost", proxy.getLocalPort());

		IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		Assert.assertEquals(404, response.getStatus());
			
		proxy.close();
		server.close();
	}

	@Test
	public void testServerReturns404_3() throws Exception {
		
		System.setProperty("org.xlightweb.showDetailedError", "true");
		
		System.out.println("testServerReturns404_3");

		
		IDataHandler dh = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection con) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
				
				con.readStringByDelimiter("\r\n\r\n");
				con.write("HTTP/1.0 404 Not Found\r\n" +
						  "Server: me\r\n" +
						  "Date: Mon, 02 Feb 2009 15:56:01 GMT\r\n" +
						  "Connection: close\r\n" +
						  "Content-Type: text/html; charset=utf-8\r\n" +
						  "\r\n");
				
				con.close();
				return true;
			}
		};
		
		IServer server = new Server(dh);
		server.start();
		
		HttpProxy proxy =  new HttpProxy(0, "localhost", server.getLocalPort(), true, Integer.MAX_VALUE, Integer.MAX_VALUE);
		proxy.start();

		HttpClientConnection con = new HttpClientConnection("localhost", proxy.getLocalPort());

		IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		Assert.assertEquals(404, response.getStatus());
		
		QAUtil.sleep(1000);
		Assert.assertFalse(con.isOpen());
	
			
		proxy.close();
		server.close();
	}
	
	
	
	
    
	

	@Test
	public void testClientSideResponseTimeout() throws Exception {
		
		System.out.println("testClientSideResponseTimeout");
		
		SlowItWorksRequestHandler itWorksHandler = new SlowItWorksRequestHandler();
		HttpServer server = new HttpServer(itWorksHandler);
		server.addConnectionHandler(itWorksHandler);
		server.setBodyDataReceiveTimeoutMillis(500);
		server.start();
		
		HttpProxy proxy =  new HttpProxy(0, "localhost", server.getLocalPort(), true, 60, 30);
		proxy.start();

		
		HttpClient httpClient = new HttpClient();
		httpClient.setResponseTimeoutMillis(500);
		
		
		try {
			httpClient.call(new GetRequest("http://localhost:" + proxy.getLocalPort() + "/test?loops=3&pause=1000"));
		} catch (SocketTimeoutException expected) { }
		
		QAUtil.sleep(2000);   // > 1000!
		
		Assert.assertEquals(0, httpClient.getNumActive());
		Assert.assertEquals(0, httpClient.getNumIdle());
		Assert.assertEquals(1, httpClient.getNumDestroyed());
		
		Assert.assertEquals(1, itWorksHandler.getCountDisconnect());
		
		httpClient.close();
		proxy.close();
		server.close();
	}

	
	

	

	

	@Test
	public void testConnectionClosedBeforeSend() throws Exception {
		
		System.out.println("testConnectionClosedBeforeSend");
 
		NonResponsiveServer hdl = new NonResponsiveServer();
		IServer server = new HttpServer(hdl);
		server.start();

		final HttpProxy proxy = new HttpProxy(0, "localhost", server.getLocalPort(), true, 1, 60);
		proxy.start();

		HttpHandler httpHandler = new HttpHandler();
		HttpClientConnection con1 = new HttpClientConnection("localhost", proxy.getLocalPort(), httpHandler);

		QAUtil.sleep(1500);
		try {
			con1.call(new PostRequest("/", "text/plain", "UTF-8", "hello"));
			Assert.fail("IOException expected");
		} catch (IOException expected) {  }

		
		con1.close();
		proxy.close();
		server.close();
	}
	
	
	@Test
	public void testNonResponsiveBackend() throws Exception {
		
		System.out.println("testNonResponsiveBackend");
		
		NonResponsiveServer hdl = new NonResponsiveServer();
		IServer server = new HttpServer(hdl);
		server.start();

		final HttpProxy proxy = new HttpProxy(0, "localhost", server.getLocalPort(), true, 60, 1);
		proxy.start();

		HttpHandler httpHandler = new HttpHandler();
		HttpClientConnection con1 = new HttpClientConnection("localhost", proxy.getLocalPort(), httpHandler);
		IHttpResponse response = con1.call(new PostRequest("/", "text/plain", "UTF-8", "hello"));
		
		Assert.assertEquals(504, response.getStatus());

		
		con1.close();
		proxy.close();
		server.close();
	}
	
	

	@Test
	public void testHangingStreamingBackend() throws Exception {
	    
		System.out.println("testHangingStreamingBackend");
		
		HangingStreamServer hdl = new HangingStreamServer();
		IServer server = new HttpServer(hdl);
		server.start();

		final HttpProxy proxy = new HttpProxy(0, "localhost", server.getLocalPort(), true, 60, 1);
		proxy.start();

		HttpHandler httpHandler = new HttpHandler();
		HttpClientConnection con1 = new HttpClientConnection("localhost", proxy.getLocalPort(), httpHandler);
		IHttpResponse response = con1.call(new PostRequest("/", "text/plain", "UTF-8", "hello"));
		
		Assert.assertEquals(200, response.getStatus());
		
		try {
			response.getBody().readString();
			Assert.fail("IOException expected");
		} catch (IOException expected) {  }

		
		con1.close();
		proxy.close();
		server.close();
	}
	

	@Test
	public void testServiceTimeoutSmallerThanBackendTimeout() throws Exception {
		
		System.out.println("testServiceTimeoutSmallerThanBackendTimeout");
 
		NonResponsiveServer hdl = new NonResponsiveServer();
		IServer server = new HttpServer(hdl);
		server.start();

		final HttpProxy proxy = new HttpProxy(0, "localhost", server.getLocalPort(), true, 1, 60 * 60);
		proxy.start();

		HttpHandler httpHandler = new HttpHandler();
		HttpClientConnection con1 = new HttpClientConnection("localhost", proxy.getLocalPort(), httpHandler);

		try {
			con1.call(new PostRequest("/", "text/plain", "UTF-8", "hello"));
			Assert.fail("IOException expected");
		} catch (IOException expected) { }

		
		con1.close();
		proxy.close();
		server.close();
	}
	
	

	@Test
    public void testPersistentClientProxyConnection() throws Exception {
    	
    	System.out.println("testPersistentClientProxyConnection");
    	
    	HttpServer server = new HttpServer(new ItWorksRequestHandler());
    	server.start();
    	
    	HttpProxy proxy =  new HttpProxy(0, "localhost", server.getLocalPort(), true, Integer.MAX_VALUE, Integer.MAX_VALUE);
    	proxy.start();
    
    	
    	HttpClient httpClient = new HttpClient();
    	
    	FutureResponseHandler respHdl = new FutureResponseHandler();
    	HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + proxy.getLocalPort() + "/test", "text/plain");
    	BodyDataSink bodyDataSink = httpClient.send(header, 1000, respHdl);
    	
    	bodyDataSink.write(QAUtil.generateByteArray(500));
    	QAUtil.sleep(200);
    	
    	bodyDataSink.write(QAUtil.generateByteArray(500));
    	bodyDataSink.close();
    	
    	IHttpResponse response = respHdl.getResponse();
    	Assert.assertEquals(200, response.getStatus());
    	
    	QAUtil.sleep(1000);
    	
    	// client -> proxy connection is persistent
    	Assert.assertEquals(0, httpClient.getNumActive());
    	Assert.assertEquals(1, httpClient.getNumIdle());
    	Assert.assertEquals(0, httpClient.getNumDestroyed());
    	
    	httpClient.close();
    	proxy.close();
    	server.close();
    }

    @Test
    public void testContinueSupportingProxy() throws Exception {
        
        HttpServer server = new HttpServer(new EchoHandler());
        server.start();
        
        ContinueableHttpProxy2 proxy =  new ContinueableHttpProxy2(0, false);
        proxy.start();
    
        HttpClient httpClient = new HttpClient();
        httpClient.setProxyHost("localhost");
        httpClient.setProxyPort(proxy.getLocalPort());
        
        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/test", "text/plain", "12345678");
        request.setHeader("Expect", "100-continue");
        
        IHttpResponse response = httpClient.call(request);

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345678", response.getBody().readString());
        
        
        httpClient.close();
        proxy.close();
        server.close();
    }

    
    @Test
    public void testContinue() throws Exception {
        HttpServer server = new HttpServer(new EchoHandler());
        server.start();
        
        HttpProxy2 proxy =  new HttpProxy2(0, false);
        proxy.start();
    
        HttpClient httpClient = new HttpClient();
        httpClient.setProxyHost("localhost");
        httpClient.setProxyPort(proxy.getLocalPort());
        
        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/test", "text/plain", "12345678");
        request.setHeader("Expect", "100-continue");
        
        IHttpResponse response = httpClient.call(request);

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345678", response.getBody().readString());
        
        
        httpClient.close();
        proxy.close();
        server.close();
    }
    

    
    @Test
    public void testCompresses() throws Exception {
        File file = QAUtil.copyToTempfile("compressedfile.zip");
        String path = file.getParent();
        
        HttpServer httpServer = new HttpServer(new FileServiceRequestHandler(path, true));
        httpServer.start();
        
        HttpProxy2 proxy =  new HttpProxy2(0, false);
        proxy.start();
    
        HttpClient httpClient = new HttpClient();
        httpClient.setProxyHost("localhost");
        httpClient.setProxyPort(proxy.getLocalPort());
        
        for (int i = 0; i < 500; i++) {
            GetRequest req = new GetRequest("http://localhost:" + httpServer.getLocalPort() + "/" + file.getName());
            req.setHeader("Accept-Encoding", "gzip");
            
            IHttpResponse resp = httpClient.call(req);
            Assert.assertEquals(5867, resp.getContentLength());
        }
        
        file.delete();
        httpClient.close();
        httpServer.close();
        proxy.close();
    }
    
    
    @Test
    public void testHttp1_0() throws Exception {
    	
    	IDataHandler dh = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection con) throws IOException {
				con.readStringByDelimiter("\r\n\r\n");
				
				con.write("HTTP/1.0 200 OK\r\n" +
						  "Content-Type: text/html;charset=ISO-8859-1\r\n" +
						  "Content-Length: 10000\r\n" +
						  "\r\n" +
						  new String(QAUtil.generateByteArray(10000)));
				
				return true;
			}
		};
		Server server = new Server(dh);
		server.start();
        
        HttpProxy2 proxy =  new HttpProxy2(0, false);
        proxy.start();
    
        HttpClient httpClient = new HttpClient();
        httpClient.setProxyHost("localhost");
        httpClient.setProxyPort(proxy.getLocalPort());
        
        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/test", "text/plain", "12345678");
        request.setHeader("Accept-Encoding", "gzip");
        
        IHttpResponse response = httpClient.call(request);

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(new String(QAUtil.generateByteArray(10000)), response.getBody().readString());
        
        
        httpClient.close();
        proxy.close();
        server.close();
    }
        
    
    
    private static final class HttpHandler implements IHttpConnectHandler, IHttpDisconnectHandler {


		private final AtomicBoolean connectCalled = new AtomicBoolean(false);
		private final AtomicBoolean disconnectCalled = new AtomicBoolean(false);;



		public boolean onConnect(IHttpConnection httpConnection) throws IOException {
			connectCalled.set(true);
			return true;
		}


		public boolean onDisconnect(IHttpConnection httpConnection) throws IOException {
			disconnectCalled.set(true);
			return true;
		}


		boolean isConnectCalled() {
			return connectCalled.get();
		}

		boolean isDisconnectCalled() {
			return disconnectCalled.get();
		}

	}
	
	
	private static final class ClosingServer implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			exchange.getConnection().close();
		}		
	}

	
	private static final class NonResponsiveServer implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
		}
	}
	
	private static final class HangingStreamServer implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			BodyDataSink outChannel = exchange.send(new HttpResponseHeader(200, "text/plain"));
			outChannel.write("test");
			
			// don't close the channel -> hanging server
		}	
	}

	
	private static final class NotFoundServer implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			HttpResponse response = new HttpResponse(404);
			response.setContentType("text/html");
			response.setHeader("Connection", "close");
			exchange.send(response);
		}
	}
	
	
	
	private static final class EchoHandler implements IHttpRequestHandler {
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
	    }
	}
	
	
	
	private static final class ItWorksRequestHandler implements IHttpRequestHandler, IHttpDisconnectHandler {
		
		private IOException ioe = null;
		private int countDisconnect = 0;
		
		public boolean onDisconnect(IHttpConnection httpConnection) throws IOException {
			countDisconnect++;
			return true;
		}
		
		public void onRequest(final IHttpExchange exchange) throws IOException {
			
			IHttpRequest request = exchange.getRequest();
			
			IBodyDataHandler bodyHandler = new IBodyDataHandler() {
				
				public boolean onData(NonBlockingBodyDataSource bodyDataSource) throws BufferUnderflowException {

					try  {
						int available = bodyDataSource.available();
						if (available > 0) {
							bodyDataSource.readByteBufferByLength(available);
						} else if (available == -1) {
							exchange.send(new HttpResponse(200, "text/plain", "it works"));
						}
					} catch (IOException e) {
						ioe = e;
					}
					
					return true;
				}
			};
			request.getNonBlockingBody().setDataHandler(bodyHandler);
		}
		
		public IOException getException() {
			return ioe;
		}
		
		public int getCountDisconnect() {
			return countDisconnect;
		}
	}
	
	
	private static final class SlowItWorksRequestHandler implements IHttpRequestHandler, IHttpDisconnectHandler {
		
		private IOException ioe = null;
		private int countDisconnect = 0;
		
		public boolean onDisconnect(IHttpConnection httpConnection) throws IOException {
			countDisconnect++;
			return true;
		}
		
		
		public void onRequest(final IHttpExchange exchange) throws IOException {
			
			IHttpRequest request = exchange.getRequest();
			int pause = request.getIntParameter("pause");
			int loops = request.getIntParameter("loops");

		
			BodyDataSink bodyDataSink = exchange.send(new HttpResponseHeader(200, "text/plain"));
			
			for (int i = 0; i < loops; i++) {
				QAUtil.sleep(pause);
				try {
					bodyDataSink.write(QAUtil.generateByteArray(100));
				} catch (IOException e) {
					ioe = e;
					throw e;
				}
			}
			bodyDataSink.close();
		}
		
		public IOException getException() {
			return ioe;
		}
		
		public int getCountDisconnect() {
			return countDisconnect;
		}
	}	
}
