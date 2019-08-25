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



import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicReference;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.client.IHttpClientEndpoint;
import org.xlightweb.client.HttpClient.FollowsRedirectMode;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IConnection.FlushMode;




/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientTest  {

    
    @Test
    public void testPlainGet() throws Exception {
        System.out.println("testPlainGet");
        
        HttpServer server = new HttpServer(new ItWorksRequetHandler());
        server.start();

        
        HttpClient httpClient = new HttpClient();
        httpClient.setAutoHandleCookies(false);

    
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/index.html"));
        Assert.assertEquals(200, response.getStatus());
        QAUtil.sleep(200);

        
        // does the pooling really work?
        Assert.assertTrue(httpClient.getNumCreated() == 1);
        
        httpClient.close();
        server.close();
    }
    
    
	@Test
	public void testGet() throws Exception {
	    System.out.println("testGet");
	
	    HttpServer server = new HttpServer(new ItWorksRequetHandler());
		server.start();

		
		HttpClient httpClient = new HttpClient();
		ConnectionUtils.registerMBean(httpClient);

	
		for (int i = 0; i < 5; i++) {
			IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/index.html"));
			Assert.assertEquals(200, response.getStatus());
			Assert.assertEquals("it works", response.getBody().readString());
			QAUtil.sleep(100);
		}

		
		// does the pooling really work?
 		
		httpClient.close();
		server.close();
	}
	
	

	
	@Test
	public void testGetAsync() throws Exception {
	    System.out.println("testGetAsync");
		
		HttpServer server = new HttpServer(new ItWorksRequetHandler());
		server.start();

		
		HttpClient httpClient = new HttpClient();
		ConnectionUtils.registerMBean(httpClient);

	
		for (int i = 0; i < 5; i++) {
			
			FutureResponseHandler respHdl = new FutureResponseHandler();
			httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/index.html"), respHdl);
			
			IHttpResponse response = respHdl.getResponse();
			Assert.assertEquals(200, response.getStatus());
			
			Assert.assertEquals("it works", response.getBody().readString());
			QAUtil.sleep(100);
		}

		
		// does the pooling really work?
		Assert.assertEquals(1, httpClient.getNumCreated());
		
		httpClient.close();
		server.close();
	}

	
	
	@Test
	public void testNoReuse() throws Exception {
	    System.out.println("testNoReuse");
		
		HttpServer server = new HttpServer(new ItWorksRequetHandler());
		server.start();

		
		HttpClient httpClient = new HttpClient();
		httpClient.setMaxIdle(0);
	
		for (int i = 0; i < 10; i++) {
			
			FutureResponseHandler respHdl = new FutureResponseHandler();
			BodyDataSink bodyDataSink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/index.html", "text/plain"), 100, respHdl);
			bodyDataSink.write(QAUtil.generateByteArray(100));
			bodyDataSink.close();
			
			IHttpResponse response = respHdl.getResponse();
			Assert.assertEquals(200, response.getStatus());
			Assert.assertEquals("it works", response.getBody().readString());
		}

		// does the pooling really work?
		Assert.assertTrue(httpClient.getNumCreated() == 10);
		
		httpClient.close();
		server.close();
	}

	
	@Test
	public void testPostAsync() throws Exception {
	    System.out.println("testPostAsync");
	    
		HttpServer server = new HttpServer(new ItWorksRequetHandler());
		server.start();
		
		HttpClient httpClient = new HttpClient();
		ConnectionUtils.registerMBean(httpClient);

	
		for (int i = 0; i < 5; i++) {
			FutureResponseHandler respHdl = new FutureResponseHandler();
			BodyDataSink bodyDataSink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/index.html", "text/plain"), 100, respHdl);
			bodyDataSink.write(QAUtil.generateByteArray(100));
			bodyDataSink.close();
			
			IHttpResponse response = respHdl.getResponse();
			if (response.getStatus() != 200) {
				Assert.fail("response status should be 200. got " + response.getStatus());
			}
			Assert.assertEquals("it works", response.getBody().readString());
		}

		// does the pooling really work?
		Assert.assertTrue("pooling has not been worked. " + httpClient.getNumCreated() + " connections created", httpClient.getNumCreated() < 5);
		
		httpClient.close();
		server.close();
	}


	
	@Test
	public void testPostAsyncChunked() throws Exception {
	    System.out.println("testPostAsyncChunked");
		
		HttpServer server = new HttpServer(new ItWorksRequetHandler());
		server.start();

		
		HttpClient httpClient = new HttpClient();
		ConnectionUtils.registerMBean(httpClient);

	
		for (int i = 0; i < 5; i++) {
			
			FutureResponseHandler respHdl = new FutureResponseHandler();
			BodyDataSink bodyDataSink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/index.html", "text/plain"), respHdl);
			bodyDataSink.write(QAUtil.generateByteArray(100));
			bodyDataSink.close();
			
			IHttpResponse response = respHdl.getResponse();
			Assert.assertEquals(200, response.getStatus());
			
			Assert.assertEquals("it works", response.getBody().readString());
			
			QAUtil.sleep(100);
		}

		// does the pooling really work?
		Assert.assertEquals(1, httpClient.getNumCreated());

		
		httpClient.close();
		server.close();
	}


	
	
	
	@Test
	public void testLiveGetFollowRedirect() throws Exception {
	    System.out.println("testLiveGetFollowRedirect");
	    
		HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);

		IHttpResponse response = httpClient.call(new GetRequest("http://www.web.de/index.html"));
		Assert.assertEquals(200, response.getStatus());
		httpClient.close();
	}

	@Test
	public void testLiveGetFollowRedirectSend() throws Exception {
	    System.out.println("testLiveGetFollowRedirectSend");
	    
		HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);

		MultiThreadedResponseHandler hdl = new MultiThreadedResponseHandler();
		httpClient.send(new GetRequest("http://www.web.de/index.html"), hdl);

		long start = System.currentTimeMillis();
		while (hdl.getResponse() == null) {
		    if (System.currentTimeMillis() > (start + 3000)) {
		        Assert.fail("timeout reached");
		    }
		    QAUtil.sleep(100);
		}

		
		Assert.assertEquals(200, hdl.getResponse().getStatus());

		httpClient.close();
	}



/*
    // requires Java 1.6
	@Test
	public void testLiveGetHttps() throws Exception {
		HttpClient httpClient = new HttpClient(SSLContext.getDefault()); // SSLContext.getDefault() -> Java 1.6!

		Response response = httpClient.callFollowRedirects(new GetRequest("https://www.web.de/"));
		Assert.assertEquals(200, response.getStatus());
	}
*/


	
	@Test
	public void testBlockingTransferTo() throws Exception {
	    System.out.println("testBlockingTransferTo");
		
		HttpServer server = new HttpServer(9886, new HeaderInfoServerHandler());
		server.start();

		
		HttpClient httpClient = new HttpClient();
 	
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		
		File file = QAUtil.createTempfile();
		System.out.println("write to file " + file.getAbsolutePath());
			
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		FileChannel fc = raf.getChannel();
		response.getBody().transferTo(fc);
		fc.close();
		raf.close();

		LineNumberReader lnr = new LineNumberReader(new FileReader(file));
		String line = lnr.readLine();
		
		Assert.assertEquals("method= GET", line);
		
		file.delete();
		httpClient.close();
		server.close();
	}
	
	
	@Test
	public void testCallTimeout() throws Exception {
	    System.out.println("testCallTimeout");
	    
		IServer server = new Server(new DevNullHandler());
		server.start();

		HttpClient httpClient = new HttpClient();
		httpClient.setResponseTimeoutMillis(1000);

		try {
			httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
			Assert.fail("timeout exepction shoud haven been thrown");
		} catch (SocketTimeoutException expeceted) { }

		QAUtil.sleep(100);

		Assert.assertEquals(1, httpClient.getNumCreated());
		Assert.assertEquals(1, httpClient.getNumDestroyed());
		Assert.assertEquals(0, httpClient.getIdleConnectionInfos().size());

		httpClient.close();
		server.close();
	}


	@Test
	public void testHeaders() throws Exception {
	    System.out.println("testHeaders");
	    
		HttpClient httpClient = new HttpClient();
		ConnectionUtils.registerMBean(httpClient);
		
		HttpServer server = new HttpServer(new HeaderInfoServerHandler());
		server.start();

		for (int i = 0; i < 5; i++) {
			GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
			request.addHeader("header1", "value1");
			request.addHeader("header2", "value2");

			IHttpResponse response = httpClient.call(request);
			String body = response.getBody().toString();


			Assert.assertTrue(body.indexOf("header1: value1") != -1);
			Assert.assertTrue(body.indexOf("header2: value2") != -1);
		}


		httpClient.close();
		server.close();
	}


	
	
	@Test
	public void testLiveRedirect() throws Exception {
	    System.out.println("testLiveRedirect");
	    
		HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
		ConnectionUtils.registerMBean(httpClient);

		IHttpResponse response = httpClient.call(new GetRequest("http://www.web.de:80/invalidpath"));
		String body = response.getBody().readString();
		
		Assert.assertTrue(body.indexOf("Die gew\u00FCnschte Seite wurde leider nicht gefunden") != -1);

		
		httpClient.close();
	}
	

	@Test
	public void testLiveSendRedirect() throws Exception {
	    System.out.println("testLiveSendRedirect");
	    
		HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
		ConnectionUtils.registerMBean(httpClient);

		FutureResponseHandler respHdl = new FutureResponseHandler();
		httpClient.send(new GetRequest("http://www.web.de:80/invalidpath"), respHdl);
		
		IHttpResponse response = respHdl.getResponse();
		String body = response.getBody().readString();
		
		Assert.assertTrue(body.indexOf("Die gew\u00FCnschte Seite wurde leider nicht gefunden") != -1);

		
		httpClient.close();
	}



	@Test
	public void testInvokeOnHeader() throws Exception {
	    System.out.println("testInvokeOnHeader");

		int delay = 500;
		HttpServer server = new HttpServer(9554, new DelayServerHandler(delay));
		server.start();

		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		
		Assert.assertFalse("body should not be complete received", response.getNonBlockingBody().isCompleteReceived());

		QAUtil.sleep(1000);
		
		Assert.assertTrue("body should be complete received", response.getNonBlockingBody().isCompleteReceived());
		

		
		httpClient.close();
		server.close();
	}
	
	



	@Test
	public void testNonThreadedAsynchGet() throws Exception {
	    System.out.println("testNonThreadedAsynchGet");

		IServer server = new HttpServer(new HeaderInfoServerHandler());
		server.start();
 

		IHttpClientEndpoint httpClient = new HttpClient();

		NonThreadedResponseHandler hdl = new NonThreadedResponseHandler();
		httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/"), hdl);

		while (hdl.getResponse() == null) {
		    QAUtil.sleep(100);
		}

		IHttpResponse response = hdl.getResponse();

		httpClient.close();
		server.close();

		Assert.assertTrue(hdl.getThreadname().startsWith("xDispatcher"));
		Assert.assertEquals(200, response.getStatus());
	}



	@Test
	public void testCallMaxRedirect() throws Exception {
	    System.out.println("testCallMaxRedirect");

		IServer server = new HttpServer(new RedirectLoopServerHandler());
		server.start();

		HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);

		try {
			httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
			Assert.fail("max redirect exception should have been thrown");
		} catch (IOException maxRedirect) {
			System.out.println(".");
		}

		httpClient.close();
		server.close();
	}
	
	
	
	
	
	
	@Test
	public void testCallRelativeRedirect() throws Exception {
	    System.out.println("testCallRelativeRedirect");

		IServer server = new HttpServer(new RelativeRedirectHandler ());
		server.start();

		HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);

		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
		Assert.assertEquals(200, response.getStatus());

		
		httpClient.close();
		server.close();
	}


	@Test
	public void testSendMaxRedirect() throws Exception {
	    System.out.println("testSendMaxRedirect");

		IServer server = new HttpServer(new RedirectLoopServerHandler());
		ConnectionUtils.start(server);


		HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);

		NonThreadedResponseHandler hdl = new NonThreadedResponseHandler();

		httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"), hdl);
		Assert.assertNull(hdl.getResponse());  // redirect loop should bee terminated -> response is null


		
		httpClient.close();
		server.close();
	}



	@Test
	public void testLiveNonThreadedAsynchGetRedirect() throws Exception {
	    System.out.println("testLiveNonThreadedAsynchGetRedirect");
	    
		HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);


		NonThreadedResponseHandler hdl = new NonThreadedResponseHandler();
		httpClient.send(new GetRequest("http://www.web.de:80/invalidpath"), hdl);

		long start = System.currentTimeMillis();
		while (hdl.getResponse() == null) {
		    if (System.currentTimeMillis() > (start + 3000)) {
		        Assert.fail("timeout reached");
		    }

		    QAUtil.sleep(100);
		}

		Assert.assertEquals(200, hdl.getResponse().getStatus());
		Assert.assertTrue(hdl.getThreadname().startsWith("xDispatcher"));
		Assert.assertTrue(hdl.getResponse().getBody().readString().indexOf("Die gew\u00FCnschte Seite wurde leider nicht gefunden") != -1);
		
		
		httpClient.close();
	}



	@Test
	public void testLiveMultiThreadedAsynchGetRedirect() throws Exception {
	    System.out.println("testLiveMultiThreadedAsynchGetRedirect");

		HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);

		MultiThreadedResponseHandler hdl = new MultiThreadedResponseHandler();
		httpClient.send(new GetRequest("http://www.web.de:80/invalidpath"), hdl);

		long start = System.currentTimeMillis();
		while(hdl.getResponse() == null) {
		    if (System.currentTimeMillis() > (start + 3000)) {
		        Assert.fail("timeout reached");
		    }

		    QAUtil.sleep(100);
		}

		IHttpResponse response = hdl.getResponse();

		Assert.assertEquals(200, response.getStatus());
		Assert.assertFalse(hdl.getThreadname().startsWith("xDispatcher"));
		Assert.assertTrue(hdl.getResponse().getBody().readString().indexOf("Die gew\u00FCnschte Seite wurde leider nicht gefunden") != -1);
		
		httpClient.close();
	}



	@Test
	public void testMultiThreadedAsynchGet() throws Exception {
	    System.out.println("testMultiThreadedAsynchGet");

		IServer server = new HttpServer(9154, new HeaderInfoServerHandler());
		server.start();


		HttpClient httpClient = new HttpClient();

		Assert.assertEquals(0, httpClient.getActiveConnectionInfos().size());
		Assert.assertEquals(0, httpClient.getIdleConnectionInfos().size());

		MultiThreadedResponseHandler hdl = new MultiThreadedResponseHandler();
		httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/"), hdl);

		QAUtil.sleep(1000);

		Assert.assertEquals(0, httpClient.getActiveConnectionInfos().size());
		Assert.assertEquals(1, httpClient.getIdleConnectionInfos().size());

		IHttpResponse response = hdl.getResponse();


		httpClient.close();
		server.close();

		Assert.assertFalse(hdl.getThreadname().startsWith("xDispatcher"));
		Assert.assertEquals(200, response.getStatus());

	}


	@Execution(Execution.NONTHREADED)
	private static final class NonThreadedResponseHandler implements IHttpResponseHandler {

		private final AtomicReference<String> threadnameRef = new AtomicReference<String>();
		private final AtomicReference<IHttpResponse> responseRef = new AtomicReference<IHttpResponse>();

		public void onResponse(IHttpResponse response) throws IOException {
			threadnameRef.set(Thread.currentThread().getName());
			responseRef.set(response);
		}
		
		public void onException(IOException ioe) {
			
		}


		public IHttpResponse getResponse() {
			return responseRef.get();
		}

		String getThreadname() {
			return threadnameRef.get();
		}
	}


	@Execution(Execution.MULTITHREADED)
	private static final class MultiThreadedResponseHandler implements IHttpResponseHandler {

		private final AtomicReference<String> threadnameRef = new AtomicReference<String>();
		private final AtomicReference<IHttpResponse> responseRef = new AtomicReference<IHttpResponse>();

		public void onResponse(IHttpResponse response) throws IOException {
			threadnameRef.set(Thread.currentThread().getName());
			responseRef.set(response);
		}
		
		public void onException(IOException ioe) {
		}


		public IHttpResponse getResponse() {
			return responseRef.get();
		}

		String getThreadname() {
			return threadnameRef.get();
		}
	}
	
	
	private static final class ItWorksRequetHandler implements IHttpRequestHandler {
		
		@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			exchange.send(new HttpResponse(200, "text/plain", "it works"));
		}
	}
	
	
	private static final class DelayServerHandler implements IHttpRequestHandler {
		
		private int delay = 0;
		
		public DelayServerHandler(int delay) {
			this.delay = delay;
		}
		
	
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			int size = 400;
			
			BodyDataSink bodyDataSink = exchange.send(new HttpResponseHeader(200, "text/plain;charset=ISO-8895-1"), size);
			bodyDataSink.setFlushmode(FlushMode.ASYNC);
			
			bodyDataSink.write((byte) 45);
			
			QAUtil.sleep(delay);
			
			byte[] bytes = new byte[size - 1];
			for (int i = 0; i < bytes.length; i++) {
				bytes[i] = 45;
			}
			bodyDataSink.write(bytes);
		}	
	}	
	
	
	private static final class RedirectLoopServerHandler implements IHttpRequestHandler {
	
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			URL targetURL = exchange.getRequest().getRequestUrl();
			
			HttpResponse response = new HttpResponse(302);
			response.setHeader("Location", targetURL.toString());
			exchange.send(response);
		}
	}

	
	private static final class RelativeRedirectHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			IHttpRequest request = exchange.getRequest();
			if (request.getRequestURI().equals("/test")) {
				HttpResponse response = new HttpResponse(302);
				response.setHeader("Location", "/redirected");
				exchange.send(response);
				
			} else if (request.getRequestURI().equals("/redirected")) {
				exchange.send(new HttpResponse(200, "text/plain", "OK"));
			 	
			} else {
				exchange.sendError(500);
			}
		}
	}
	
	
	
	public final class ParamsInfoServerHandler implements IHttpRequestHandler {


		@Execution(Execution.NONTHREADED)
		public void onRequest(IHttpExchange exchange) throws IOException {

			IHttpRequest request = exchange.getRequest();
			new HttpResponseHeader(200, "text/plain");

			StringBuilder sb = new StringBuilder();
			sb.append("method= " + request.getMethod() + "\r\n");

			for (String paramName : request.getHeaderNameSet()) {
				sb.append("[param] " + paramName + ": " + request.getParameter(paramName) + "\r\n");
			}
			
			exchange.send(new HttpResponse(200, "text/plain", sb.toString()));
		}
	}
	
	
	@Execution(Execution.NONTHREADED)
	public final class DevNullHandler implements IDataHandler {


		public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException {
			connection.readByteBufferByLength(connection.available());

			return true;
		}
	}
}