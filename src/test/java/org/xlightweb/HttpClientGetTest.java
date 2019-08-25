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

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicReference;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.client.IHttpClientEndpoint;
import org.xlightweb.client.HttpClient.FollowsRedirectMode;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;
import org.xsocket.connection.ConnectionUtils;



/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientGetTest  {


      
    public static void main(String[] args) throws Exception {
        
        
        for (int i = 0; i < 10000; i++) {
            new HttpClientGetTest().testHttpClientDestroy();
            System.out.print(".");
        }
    }



	@Test
	public void testLiveGet() throws Exception {
		
		HttpClient httpClient = new HttpClient();
		httpClient.setAutoHandleCookies(false);
	
		IHttpResponse response = httpClient.call(new GetRequest("http://www.web.de/index.html"));
		Assert.assertEquals(302, response.getStatus());
	}
	
	@Test
	public void testLiveGet2() throws Exception {
		IHttpClientEndpoint httpClient = new HttpClient();
	
		IHttpResponse response = httpClient.call(new GetRequest("http://xlightweb.org"));
		Assert.assertEquals(301, response.getStatus());
	}
	


	
	@Test
	public void testLiveGetFollowRedirect() throws Exception {
		HttpClient httpClient = new HttpClient();
		httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);

		IHttpResponse response = httpClient.call(new GetRequest("http://www.web.de/index.html"));
		Assert.assertEquals(200, response.getStatus());
	}

	@Test
	public void testLiveGetFollowRedirectSend() throws Exception {
	    
	    long startTime = System.currentTimeMillis();
	    
		HttpClient httpClient = new HttpClient();
	      httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);

		MultiThreadedResponseHandler hdl = new MultiThreadedResponseHandler();
		httpClient.send(new GetRequest("http://www.web.de/index.html"), hdl);

		while (hdl.getResponse() == null) {
		    if (System.currentTimeMillis() > (startTime + (15 * 1000))) {
		        Assert.fail("timout");
		    }

		    QAUtil.sleep(100);

		}
		
		Assert.assertEquals(200, hdl.getResponse().getStatus());
	}




	@Ignore
	@Test
    public void testLiveGetHttps() throws Exception {
	    
	    //if this test runs only on Java 1.6 or higher -> the default SSLContext will be loaded automatically 
        HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);

        IHttpResponse response = httpClient.call(new GetRequest("https://www.web.de/"));
        Assert.assertEquals(200, response.getStatus());
    }

    
    @Test
    public void testQueryParameters() throws Exception {
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
        
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                
                IHttpRequest request = exchange.getRequest();
                
                MultivalueMap body = new MultivalueMap("UTF-8");
                
                for (String name : request.getParameterNameSet()) {
                    for (String value : request.getParameterValues(name)) {
                        body.addParameter(name, value);
                    }
                }
                
                
                exchange.send(new HttpResponse(200, "application/x-www-form-urlencoded", body.toString()));
            }
        };
        
        IServer server = new HttpServer(reqHdl);
        server.start();

        
        HttpClient httpClient = new HttpClient();
        
        GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/", new NameValuePair("test1", "value1"), new NameValuePair("test1", "value2"), new NameValuePair("test2", "value5"));
        IHttpResponse response = httpClient.call(request);

        Assert.assertEquals(200, response.getStatus());
        MultivalueMap body = new MultivalueMap(response.getBody());
        Assert.assertEquals("value5", body.getParameter("test2"));
        Assert.assertEquals(2, body.getParameterValues("test1").length);
        
        httpClient.close();
        server.close();
    }
    

	
	
	
	@Test
	public void testBlockingTransferTo() throws Exception {
		
		IServer server = new HttpServer(new HeaderInfoServerHandler());
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
		server.close();
	}
	
	
	@Test
	public void testCallTimeout() throws Exception {
		IServer server = new Server(new DevNullHandler());
		ConnectionUtils.start(server);

		HttpClient httpClient = new HttpClient();
		httpClient.setMaxRetries(0);
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
	public void testHttpClientClose() throws Exception {
		
		HttpServer server1 = new HttpServer(new ParamsInfoServerHandler());
		server1.start();
		
		HttpServer server2 = new HttpServer(new ParamsInfoServerHandler());
		server2.start();
		
		HttpServer server3 = new HttpServer(new ParamsInfoServerHandler());
		server3.start();

		
		HttpClient httpClient = new HttpClient();

		for (int i = 0; i < 10; i++) {
			IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server1.getLocalPort() + "/"));
			Assert.assertEquals(200, response.getStatus());
			
			response = httpClient.call(new GetRequest("http://localhost:" + server2.getLocalPort() + "/"));
			Assert.assertEquals(200, response.getStatus());
			
			response = httpClient.call(new GetRequest("http://localhost:" + server3.getLocalPort() + "/"));
			Assert.assertEquals(200, response.getStatus());
		}

		httpClient.close();
		QAUtil.sleep(1000);
		
		Assert.assertEquals(0, httpClient.getNumIdle());
		
		QAUtil.sleep(5000);
		Assert.assertEquals(0, server1.getOpenConnections().size());
		Assert.assertEquals(0, server2.getOpenConnections().size());
		Assert.assertEquals(0, server3.getOpenConnections().size());
			
		server1.close();
		server2.close();
		server3.close();
	}
	
	
	@Test
	public void testHttpClientDestroy() throws Exception {
	    
		HttpServer server1 = new HttpServer(new ParamsInfoServerHandler());
		server1.start();
		
		HttpServer server2 = new HttpServer(new ParamsInfoServerHandler());
		server2.start();
		
	
		HttpClient httpClient = new HttpClient();
		httpClient.setMaxRetries(0);

		for (int i = 0; i < 3; i++) {
			IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server1.getLocalPort() + "/"));
			Assert.assertEquals(200, response.getStatus());
			
			response = httpClient.call(new GetRequest("http://localhost:" + server2.getLocalPort() + "/"));
			Assert.assertEquals(200, response.getStatus());
		}

		httpClient.close();
		QAUtil.sleep(2000);
		
		System.out.println("idle " + httpClient.getNumIdle());
		Assert.assertEquals("idle should be 0", 0, httpClient.getNumIdle());
		
		System.out.println("destroyed " + httpClient.getNumDestroyed());
		Assert.assertTrue(httpClient.getNumDestroyed() > 0);
		
		QAUtil.sleep(3000);
		System.out.println("open cons server1: " + server1.getOpenConnections().size());
		Assert.assertEquals("open cons (server 1) should be 0", 0, server1.getOpenConnections().size());
		
		System.out.println("open cons server2: " + server2.getOpenConnections().size());
		Assert.assertEquals("open cons (server 1) should be 0", 0, server2.getOpenConnections().size());
		
		server1.close();
		server2.close();
	}
	
	@Test
	public void testHttpClientDestroy2() throws Exception {
        
        HttpServer server1 = new HttpServer(new ParamsInfoServerHandler());
        server1.start();
        
        HttpServer server2 = new HttpServer(new ParamsInfoServerHandler());
        server2.start();
        
    
        HttpClient httpClient = new HttpClient();

        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server1.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
            
        response = httpClient.call(new GetRequest("http://localhost:" + server2.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());

        httpClient.close();
        QAUtil.sleep(1000);
        
        Assert.assertEquals(0, httpClient.getNumIdle());
        Assert.assertEquals(2, httpClient.getNumDestroyed());
        
        QAUtil.sleep(3000);
        Assert.assertEquals(0, server1.getOpenConnections().size());
        Assert.assertEquals(0, server2.getOpenConnections().size());
        
        server1.close();
        server2.close();
    }
	

    @Test
    public void testParams() throws Exception {

        IServer server = new HttpServer(new RequestParamsRequestHandler());
        server.start();

        HttpClient httpClient = new HttpClient();
        
        IHttpRequest req = new GetRequest("http://localhost:" + server.getLocalPort() + "/test");
        req.setParameter("param1", "value1");
        req.setParameter("param2", "value2a");
        req.addParameter("param2", "value2b");
                             
        IHttpResponse resp = httpClient.call(req);
        
        String body = resp.getBody().toString();
        
        Assert.assertTrue(body.indexOf("param2=value2a") != -1);
        Assert.assertTrue(body.indexOf("param2=value2b") != -1);
        Assert.assertTrue(body.indexOf("param1=value1") != -1);

                

        httpClient.close();
        server.close();
    }
	
	
	


	@Test
	public void testHeaders() throws Exception {
		
		HttpClient httpClient = new HttpClient();
		
		IServer server = new HttpServer(new HeaderInfoServerHandler());
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
		HttpClient httpClient = new HttpClient();
		ConnectionUtils.registerMBean(httpClient);
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);

		IHttpResponse response = httpClient.call(new GetRequest("http://www.web.de:80/invalidpath"));
		String body = response.getBody().readString();
		
		Assert.assertTrue(body.indexOf("Die gew\u00FCnschte Seite wurde leider nicht gefunden") != -1);
	}



	@Test
	public void testInvokeOnHeader() throws Exception {
	
		ServerHandler srvHdl = new ServerHandler();
		IServer server = new HttpServer(srvHdl);
		server.start();
		
		
		HttpClient httpClient = new HttpClient();
		FutureResponseHandler respHdl = new FutureResponseHandler();
		httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/"), respHdl);
		
		do {
			QAUtil.sleep(100);
		} while (srvHdl.getExchange() == null);
		
		IHttpExchange exchange = srvHdl.getExchange();
		BodyDataSink bodyDataSink = exchange.send(new HttpResponseHeader(200));
		bodyDataSink.write("test");
	
		IHttpResponse response = respHdl.getResponse();
		Assert.assertFalse(response.getNonBlockingBody().isCompleteReceived());
	
		bodyDataSink.write("123");
		bodyDataSink.close();
		
		QAUtil.sleep(1000);
		Assert.assertTrue(response.getNonBlockingBody().isCompleteReceived());
		Assert.assertTrue(response.getNonBlockingBody().readStringByLength(7).equals("test123"));
		
		httpClient.close();
		server.close();
	}

	@Test
	public void testNonThreadedAsynchGet() throws Exception {

		IServer server = new HttpServer(new HeaderInfoServerHandler());
		ConnectionUtils.start(server);


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


		server.close();
	}
	
	
	@Test
	public void testCallRelativeRedirect() throws Exception {
		
		IServer server = new HttpServer(new RelativeRedirectHandler ());
		server.start();

		HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);

		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
		Assert.assertEquals(200, response.getStatus());

		server.close();
	}


	@Test
	public void testSendMaxRedirect() throws Exception {

		IServer server = new HttpServer(new RedirectLoopServerHandler());
		server.start();


		HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);

		NonThreadedResponseHandler hdl = new NonThreadedResponseHandler();

		httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"), hdl);
		Assert.assertNull(hdl.getResponse());  // redirect loop should bee terminated -> response is null


		server.close();
	}




	@Test
	public void testLiveMultiThreadedAsynchGetRedirect() throws Exception {

		HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);

		MultiThreadedResponseHandler hdl = new MultiThreadedResponseHandler();
		httpClient.send(new GetRequest("http://www.web.de:80/invalidpath"), hdl);

		long startTime = System.currentTimeMillis();
		while (hdl.getResponse() == null) {
		    if (System.currentTimeMillis() > (startTime + (3000))) {
		        Assert.fail("timout");
		    }

		    QAUtil.sleep(100);
		}

		IHttpResponse response = hdl.getResponse();

		Assert.assertFalse(hdl.getThreadname().startsWith("xDispatcher"));
		Assert.assertTrue(hdl.getResponse().getBody().readString().indexOf("Die gew\u00FCnschte Seite wurde leider nicht gefunden") != -1);
		Assert.assertEquals(200, response.getStatus());
	}



	@Test
	public void testMultiThreadedAsynchGet() throws Exception {

		IServer server = new HttpServer(new HeaderInfoServerHandler());
		server.start();


		HttpClient httpClient = new HttpClient();

		Assert.assertEquals(0, httpClient.getActiveConnectionInfos().size());
		Assert.assertEquals(0, httpClient.getIdleConnectionInfos().size());

		MultiThreadedResponseHandler hdl = new MultiThreadedResponseHandler();
		httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/"), hdl);

		long startTime = System.currentTimeMillis(); 
		while (hdl.getResponse() == null) {
		    if (System.currentTimeMillis() > (startTime + (3000))) {
		        Assert.fail("timout");
		    }

		    QAUtil.sleep(100);
		}

		Assert.assertEquals(0, httpClient.getActiveConnectionInfos().size());
		Assert.assertEquals(1, httpClient.getIdleConnectionInfos().size());


		httpClient.close();
		server.close();

		Assert.assertFalse(hdl.getThreadname().startsWith("xDispatcher"));
		Assert.assertEquals(200, hdl.getResponse().getStatus());
	}


	@Execution(Execution.NONTHREADED)
	private static final class NonThreadedResponseHandler implements IHttpResponseHandler {

	    private final AtomicReference<String> threadnameRef = new AtomicReference<String>(null);
        private final AtomicReference<IHttpResponse> responseRef = new AtomicReference<IHttpResponse>(null);

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

		private final AtomicReference<String> threadnameRef = new AtomicReference<String>(null);
		private final AtomicReference<IHttpResponse> responseRef = new AtomicReference<IHttpResponse>(null);

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
	
	

	private static final class ServerHandler implements IHttpRequestHandler {
		
		private IHttpExchange exchange;
	
		public void onRequest(IHttpExchange exchange) throws IOException {
			this.exchange = exchange; 
		}
		
		IHttpExchange getExchange() {
			return exchange;
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

			StringBuilder sb = new StringBuilder();
			sb.append("method= " + request.getMethod() + "\r\n");

			for (String paramName : request.getHeaderNameSet()) {
				sb.append("[param] " + paramName + ": " + request.getParameter(paramName) + "\r\n");
			}
			
			exchange.send(new HttpResponse(200, "text/plain", sb.toString()));
		}
	}
	
	

    public final class RequestParamsRequestHandler implements IHttpRequestHandler {

        @Execution(Execution.NONTHREADED)
        public void onRequest(IHttpExchange exchange) throws IOException {

            IHttpRequest request = exchange.getRequest();
            StringBuilder sb = new StringBuilder();
            
            for (String paramName : request.getParameterNameSet()) {
                for (String paramValue : request.getParameterValues(paramName)) {
                    sb.append(paramName + "=" + paramValue + "\r\n");
                }
            }
            
                    
            exchange.send(new HttpResponse(200, "text/plain", sb.toString()));
        }
    }

}