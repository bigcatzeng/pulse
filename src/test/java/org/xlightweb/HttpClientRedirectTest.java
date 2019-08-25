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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;


import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.GetRequest;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.QAUtil;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClient.FollowsRedirectMode;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.Server;




/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientRedirectTest  {
	
	
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 1000; i++) {
            new HttpClientRedirectTest().testContinueAndRedirect();
        }
    }
    

	@Test
	public void testGet() throws Exception {
	    System.out.println("testGet");
	    
		HttpServer server = new HttpServer(new ServerHandler());
		server.start();
		
		HttpClient httpClient = new HttpClient(); 
		httpClient.setFollowsRedirectMode(FollowsRedirectMode.RFC);
		
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test?countRedirects=3"));
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertTrue(response.getBody().readString().indexOf("works") != -1);
		
		httpClient.close();
		server.close();
	}
	
	
	
    @Test
    public void testGetHeader() throws Exception {

        
        IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            @InvokeOn(InvokeOn.MESSAGE_RECEIVED)
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                
                if (exchange.getRequest().getRequestURI().equals("/redirectme")) {
                    exchange.sendRedirect("/redirected");
                    
                } else {
                    exchange.send(new HttpResponse(200, "text/plain", exchange.getRequest().toString()));
                }
            }
        };
        
        
        HttpServer server = new HttpServer(hdl);
        server.start();
        
        HttpClient httpClient = new HttpClient(); 
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        
        
        HttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/redirectme");
        request.setHeader("Accept", "application/json");
        request.setHeader("X-Test", "test");
        
        IHttpResponse response = httpClient.call(request);
        String body = response.getBody().readString();
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(body.indexOf("Accept: application/json") != -1);
        Assert.assertTrue(body.indexOf("X-Test: test") != -1);
        
        httpClient.close();
        server.close();
    }
    
    
    @Test
    public void testPostHeader() throws Exception {

        
        IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            @InvokeOn(InvokeOn.MESSAGE_RECEIVED)
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                
                if (exchange.getRequest().getMethod().equals("POST")) {
                   HttpResponse response = new HttpResponse(303);
                   response.setHeader("location", "/redirected");
                   exchange.send(response);
                    
                } else {
                    exchange.send(new HttpResponse(200, "text/plain", exchange.getRequest().toString()));
                }
            }
        };
        
        
        HttpServer server = new HttpServer(hdl);
        server.start();
        
        HttpClient httpClient = new HttpClient(); 
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        
        
        HttpRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/redirectme", "text/plain", "0123456789");
        request.setHeader("Accept", "application/json");
        request.setHeader("X-Test", "test");
        
        IHttpResponse response = httpClient.call(request);
        String body = response.getBody().readString();
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(body.indexOf("Accept: application/json") != -1);
        Assert.assertTrue(body.indexOf("X-Test: test") != -1);
        
        httpClient.close();
        server.close();
    }
        
    

	@Test
	public void testPostRfc() throws Exception {
	    System.out.println("testPost");
	    
		HttpServer server = new HttpServer(new ServerHandler());
		server.start();
		
		HttpClient httpClient = new HttpClient(); 
		httpClient.setFollowsRedirectMode(FollowsRedirectMode.RFC);
		
		PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/test?countRedirects=3", "text/plain", "1234567890");
		IHttpResponse response = httpClient.call(request);
		
		Assert.assertEquals(302, response.getStatus());
		
		httpClient.close();
		server.close();
	}

    @Test
    public void testPostAll() throws Exception {
        System.out.println("testPost");
        
        HttpServer server = new HttpServer(new ServerHandler());
        server.start();
        
        HttpClient httpClient = new HttpClient(); 
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        
        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/test?countRedirects=3", "text/plain", "1234567890");
        request.setHeader("X-Transaction", UUID.randomUUID().toString());
        IHttpResponse response = httpClient.call(request);
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.getBody().readString().indexOf("1234567890") != -1);
        
        httpClient.close();
        server.close();
    }
	
	
	
	@Test
    public void test303Redirect() throws Exception {

	    
	    IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                IHttpRequest request = exchange.getRequest();
                
                HttpResponse response;
                if (request.getMethod().equals("POST")) {
                    response = new HttpResponse(303, "text/plain", request.getMethod());
                    response.setHeader("Location", request.getRequestUrl().toString());
                    
                } else {
                    response = new HttpResponse(200, "text/plain", request.getMethod());
                }
                
                exchange.send(response);
            }
        };
	    
        HttpServer server = new HttpServer(hdl);
        server.start();
        
        HttpClient httpClient = new HttpClient(); 
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        
        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/test?countRedirects=3", "text/plain", "1234567890");
        IHttpResponse response = httpClient.call(request);
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("GET", response.getBody().readString());
        
        httpClient.close();
        server.close();
    }
	
	
	
	@Test
	public void testStreaming() throws Exception {
	    System.out.println("testStreaming");
		ServerHandler srvHdl = new ServerHandler();
		HttpServer server = new HttpServer(srvHdl);
		server.start();
		
		HttpClient httpClient = new HttpClient(); 
		httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
		
		ResponseHandler respHdl = new ResponseHandler();
		BodyDataSink bodyDatasink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test?countRedirects=3"), respHdl);
		bodyDatasink.write("test");
		
		QAUtil.sleep(300);
		
		IHttpResponse response = respHdl.getResponse();
		Assert.assertEquals(200, response.getStatus());
		
		bodyDatasink.write("12345");
		bodyDatasink.close();
		
		QAUtil.sleep(300);
		
		Assert.assertEquals("test12345", response.getBody().readString());
		
		httpClient.close();
		server.close();
	}

	

    @Test
    public void testStreaming2() throws Exception {
        System.out.println("testStreaming2");

        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException {
                
                IHttpRequest request = exchange.getRequest();
                
                if (request.getRequestURI().startsWith("/redirectMe")) {
                    BodyDataSource body = request.getBody();
                    body.readStringByLength(4);
                    exchange.sendRedirect("/redirected");
                    
                } else {
                    String data = request.getBody().readString();
                    exchange.send(new HttpResponse(200, "text/plain", data));
                }
            }       
        };
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient(); 
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        
        ResponseHandler respHdl = new ResponseHandler();
        BodyDataSink bodyDatasink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test?countRedirects=3"), respHdl);
        bodyDatasink.write("test");
        
        QAUtil.sleep(300);
        
        bodyDatasink.write("12345");
        bodyDatasink.close();
        
        while (respHdl.getResponse() == null) {
            QAUtil.sleep(100);
        }
        
        IHttpResponse response = respHdl.getResponse();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("test12345", response.getBody().readString());
        
        httpClient.close();
        server.close();
    }	
	
    @Test
    public void testStreamingDestroy() throws Exception {
        System.out.println("testStreamingDestroy");
        ServerHandler srvHdl = new ServerHandler();
        HttpServer server = new HttpServer(srvHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient(); 
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        
        ResponseHandler respHdl = new ResponseHandler();
        BodyDataSink bodyDatasink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test?countRedirects=3"), respHdl);
        bodyDatasink.write("test");
        
        QAUtil.sleep(500);
        
        IHttpResponse response = respHdl.getResponse();
        Assert.assertEquals(200, response.getStatus());
        
        bodyDatasink.write("12345");
        bodyDatasink.destroy();
        
        QAUtil.sleep(500);
        
        try {
            response.getBody().readString();
            Assert.fail("ProtocolException expected");
        } catch (ProtocolException expected) { }
        
        httpClient.close();
        server.close();
    }
	

    
    @Test
    public void testStreamingDestroy2() throws Exception {
        System.out.println("testStreamingDestroy2");
        ServerHandler srvHdl = new ServerHandler();
        HttpServer server = new HttpServer(srvHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient(); 
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        
        ResponseHandler respHdl = new ResponseHandler();
        BodyDataSink bodyDatasink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test?countRedirects=3"), respHdl);
        bodyDatasink.write("test");
        bodyDatasink.destroy();
        
        QAUtil.sleep(1000);
        
        IOException ioe = respHdl.getException();
        Assert.assertNotNull(ioe);
                
        httpClient.close();
        server.close();
    }
    
    
	@Test
	public void testGetMaxRedirects() throws Exception {
	    System.out.println("testGetMaxRedirects");
		HttpServer server = new HttpServer(new ServerHandler());
		server.start();
		
		HttpClient httpClient = new HttpClient(); 
		httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
		
		try {
			httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test?countRedirects=9"));
			System.out.println("IOException expected");
			Assert.fail("IOException expected");
		} catch (IOException expected) { }
			
		httpClient.close();
		server.close();
	}
	
	
	@Test
	public void testGetMaxRedirectsHandler() throws Exception {
	    System.out.println("testGetMaxRedirectsHandler");
		HttpServer server = new HttpServer(new ServerHandler());
		server.start();
		 
		HttpClient httpClient = new HttpClient(); 
		httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
		
		ResponseHandler respHdl = new ResponseHandler();
		httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/test?countRedirects=9"), respHdl);

		QAUtil.sleep(500);
		Assert.assertNull(respHdl.getResponse());
		Assert.assertNotNull(respHdl.getException());
		
		httpClient.close();
		server.close();
	}

	
	
	@Test
    public void testContinue() throws Exception {
	    IDataHandler dh = new IDataHandler() {
	        
	        public boolean onData(INonBlockingConnection connection) throws IOException {
	            connection.readStringByDelimiter("\r\n\r\n");

	            connection.write("HTTP/1.1 100 Continue\r\n\r\n");
	            
	            QAUtil.sleep(1000);
	            
	            connection.write("HTTP/1.1 200 OK\r\n" +
	                             "Server: xLightweb/2.9.2-SNAPSHOT\r\n" +
	                             "Content-Length: 10\r\n" +
	                             "Content-Type: text/plain\r\n" +
	                             "\r\n" +
	                             "1234567890");
	            
	            
	            return true;
	        }
	    };
    	Server server = new Server(dh);
    	server.start();
    	
    	HttpClient httpClient = new HttpClient(); 
    	httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
    	
    	HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test", "text/plain");
    	header.setHeader("Expect", "100-Continue");
    	
    	ContinueResponseHandler respHdl = new ContinueResponseHandler();
    	BodyDataSink sink = httpClient.send(header, respHdl);
    	sink.flush();
    	
    	while (!respHdl.isContinueReceived()) {
            QAUtil.sleep(100);
        }
    	
    	QAUtil.sleep(500);
    	sink.write("1234567890");
    	sink.close();
    	
    	
        while (respHdl.getResponse() == null) {
            QAUtil.sleep(100);
        }
    	
    	
    	IHttpResponse response = respHdl.getResponse();
    	
    	Assert.assertEquals(200, response.getStatus());
    	Assert.assertTrue(response.getBody().readString().indexOf("1234567890") != -1);
    	
    	httpClient.close();
    	server.close();
    }
	
	
    @Test
    public void testServerIgnoresContinue() throws Exception {
        
        IHttpRequestHandler resHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                exchange.send(new HttpResponse(200, "text/plain", "1234567890"));
            }
        };
        final HttpServer server = new HttpServer(resHdl);
        server.start();
        
        
        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                connection.resetToReadMark();
                connection.markReadPosition();
                
                String header = connection.readStringByDelimiter("\r\n\r\n");
                int length = QAUtil.readContentLength(header);
                
                connection.readStringByLength(length);
                
                connection.removeReadMark();
                
                connection.write("HTTP/1.1 302 See other\r\n" +
                                 "Content-Length: 0\r\n" + 
                                 "Location: http://localhost:" + server.getLocalPort()+ "/\r\n" + 
                                 "\r\n");
                return true;
            }
        };
        Server server2 = new Server(dh);
        server2.start();
        
        HttpClient httpClient = new HttpClient(); 
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        
        HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + server2.getLocalPort() + "/test", "text/plain");
        header.setHeader("Expect", "100-Continue");
        
        ContinueResponseHandler respHdl = new ContinueResponseHandler();
        BodyDataSink sink = httpClient.send(header, 10, respHdl);
        sink.flush();
        
        while (!respHdl.isContinueReceived()) {
            QAUtil.sleep(100);
        }
        
        QAUtil.sleep(500);
        sink.write("1234567890");
        sink.close();
        
        
        while (respHdl.getResponse() == null) {
            QAUtil.sleep(100);
        }
        
        
        IHttpResponse response = respHdl.getResponse();
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.getBody().readString().indexOf("1234567890") != -1);
        
        httpClient.close();
        server.close();
    }

	

	
	@Supports100Continue
	private static final class ContinueResponseHandler implements IHttpResponseHandler {

	    private final AtomicReference<IHttpResponse> responseRef = new AtomicReference<IHttpResponse>();
	    private final AtomicReference<IOException> exceptionRef = new AtomicReference<IOException>();
	    private final AtomicBoolean isContinueReceived = new AtomicBoolean(false);
	    
	    public void onResponse(IHttpResponse response) throws IOException {
	        if (response.getStatus() == 100) {
	            isContinueReceived.set(true);
	        } else {
	            responseRef.set(response);
	        }
	    }
	    
	    public void onException(IOException ioe) throws IOException {
	        exceptionRef.set(ioe);
	    }
	    
	    boolean isContinueReceived() {
	        return isContinueReceived.get();
	    }
	    
	    IOException getException() {
	        return exceptionRef.get();
	    }
	    
	    IHttpResponse getResponse() {
	        return responseRef.get();
	    }
	}
	
	

    @Test
    public void testContinueAndRedirect() throws Exception {
        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                connection.readStringByDelimiter("\r\n\r\n");

                connection.write("HTTP/1.1 100 Continue\r\n\r\n");
               
                QAUtil.sleep(1000);
                
                connection.write("HTTP/1.1 303 See other\r\n" +
                                 "Server: xLightweb/2.9.2-SNAPSHOT\r\n" +
                                 "Content-Length: 0\r\n" +
                                 "Location: /test\r\n" +
                                 "\r\n");
                
                
                return true;
            }
        };
        Server server = new Server(dh);
        server.start();
        
        HttpClient httpClient = new HttpClient(); 
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.RFC);
        
        
        HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test", "text/plain");
        header.setHeader("Expect", "100-Continue");
        
        ContinueResponseHandler respHdl = new ContinueResponseHandler();
        BodyDataSink sink = httpClient.send(header, respHdl);
        sink.flush();
        
        while (!respHdl.isContinueReceived()) {
            QAUtil.sleep(100);
        }
        
        QAUtil.sleep(500);
        sink.write("1234567890");
        sink.close();
        
        
        while (respHdl.getException() == null) {
            QAUtil.sleep(100);
        }
        
        Assert.assertNotNull(respHdl.getException());
        
        
        httpClient.close();
        server.close();
    }
	

    private static final class ResponseHandler implements IHttpResponseHandler {
		
		private IHttpResponse response = null;
		private IOException exception = null;
		
		public void onResponse(IHttpResponse response) throws IOException {
			this.response = response;
		}
		
		public void onException(IOException ioe) {
			exception = ioe;
		}
		
		
		public IOException getException() {
			return exception;
		}
		
		public IHttpResponse getResponse() {
			return response;
		}
	}
	
	
	
	private static final class ServerHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			int countRedirects = exchange.getRequest().getIntParameter("countRedirects", 0);
		
			
            IHttpResponse response;
			
			if (countRedirects > 0) {
				response = new HttpResponse(302, "text/plain", "not found");
				response.setHeader("Location", exchange.getRequest().getRequestURI() + "?countRedirects=" + (--countRedirects));
				response.setHeader("Connection", "close");
				
			} else {
				if (exchange.getRequest().hasBody()) {
					response = new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody());
				} else {
				    response = new HttpResponse(200, "text/plain", "it works");
				}
			}
			
            if (exchange.getRequest().getHeader("X-Transaction") != null) {
                response.setHeader("X-Transaction", exchange.getRequest().getHeader("X-Transaction"));
            }
            exchange.send(response);
		}		
	}
}