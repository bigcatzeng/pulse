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



import java.io.Reader;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;

import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;

import org.xlightweb.server.HttpServer;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClient.FollowsRedirectMode;


 


/**
*
* @author grro@xlightweb.org
*/
public final class HttpClient100ContinueTest  {

    
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 100000; i++) {
            new HttpClient100ContinueTest().testHttpClientManualHandledContinueHeader();
        }
    }
    
    @Test
    public void testAuto100Continue() throws Exception {
        System.out.println("testAuto100Continue");
        
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            @Supports100Continue
            public void onRequest(IHttpExchange exchange) throws IOException {
                
                IHttpRequest request = exchange.getRequest();
                
                if (HttpUtils.isContainsExpect100ContinueHeader(request)) {
                    exchange.sendContinueIfRequested();
                }
                
                exchange.send(new HttpResponse(200, request.getContentType(), request.getNonBlockingBody()));
            }            
        };   
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        byte[] data = QAUtil.generateByteArray(400); 
        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/test", "text/plain", data);
        request.setHeader("Expect", "100-Continue");
        
        
        IHttpResponse response = httpClient.call(request);
        
        Assert.assertEquals(true, response.getAttribute("org.xlightweb.HttpClientConnection.100-continueReceived"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertArrayEquals(data, response.getBody().readBytes());
        
        httpClient.close();
        server.close();
    }
    
    
    
    @Test
    public void testAuto100Continue2() throws Exception {
        System.out.println("testAuto100Continue2");
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            @Supports100Continue
            public void onRequest(IHttpExchange exchange) throws IOException {
                
                IHttpRequest request = exchange.getRequest();
                
                if (HttpUtils.isContainsExpect100ContinueHeader(request)) {
                    exchange.sendContinueIfRequested();
                }
                
                byte[] data = request.getBody().readBytes();
                exchange.send(new HttpResponse(200, request.getContentType(), data));
            }            
        };   
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        byte[] data = QAUtil.generateByteArray(400); 
        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/test", "text/plain", data);
        request.setHeader("Expect", "100-Continue");
        
        
        IHttpResponse response = httpClient.call(request);
        
        Assert.assertEquals(true, response.getAttribute("org.xlightweb.HttpClientConnection.100-continueReceived"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertArrayEquals(data, response.getBody().readBytes());
        
        httpClient.close();
        server.close();
    }    
    
        
    @Test
    public void testAuto100ContinueLargeFile() throws Exception {
        System.out.println("testAuto100ContinueLargeFile");
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            @Supports100Continue
            public void onRequest(IHttpExchange exchange) throws IOException {
                
                IHttpRequest request = exchange.getRequest();
                
                if (HttpUtils.isContainsExpect100ContinueHeader(request)) {
                    exchange.sendContinueIfRequested();
                }
                
                exchange.send(new HttpResponse(200, request.getContentType(), request.getNonBlockingBody()));
            }            
        };   
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        byte[] data = QAUtil.generateByteArray(400000); 
        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/test", "text/plain", data);
        request.setHeader("Expect", "100-Continue");
        
        
        IHttpResponse response = httpClient.call(request);
        
        Assert.assertEquals(true, response.getAttribute("org.xlightweb.HttpClientConnection.100-continueReceived"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertArrayEquals(data, response.getBody().readBytes());
        
        httpClient.close();
        server.close();
    }
        
    
    
    
	@Test
	public void testUnexpectedContinue() throws Exception {
	    System.out.println("testUnexpectedContinue");

		IDataHandler dataHandler = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {

				connection.readStringByDelimiter("\r\n\r\n");
				
				connection.write("HTTP/1.1 100 Continue\r\n" +
						         "\r\n" +
						         "HTTP/1.1 200 OK\r\n" +
								 "Server: xLightweb/2.0-beta-2\r\n" +
								 "Content-Type: text/plain; charset=UTF-8\r\n" +
								 "Content-Length: 2\r\n" +
								 "\r\n" +
								 "OK"); 
				
				return true;
			}
		};
		
		IServer server = new Server(dataHandler);
		server.start();

		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort()+ "/"));
		
		Assert.assertEquals(200, response.getStatus());
		
		httpClient.close();
		server.close();
	}
	
	



	@Test
	public void testHttpClientManualHandledContinueHeader() throws Exception {
	    System.out.println("testHttpClientManualHandledContinueHeader");
	    
        HttpServer server = new HttpServer(new ContinueHandler());
        server.start();

        
        HttpClient httpClient = new HttpClient(); 
        httpClient.setResponseTimeoutMillis(2000);
        HttpRequestHeader requestHeader = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test", "text/plain; charset=iso-8859-1");
        requestHeader.setHeader("Expect", "100-Continue");
        
     
        String data = "test1234567890";
        ResponseHandler respHdl = new ResponseHandler(data);
        BodyDataSink dataSink = httpClient.send(requestHeader, respHdl);
        respHdl.setBodyDataSink(dataSink);
        dataSink.flush();

        
        while ((respHdl.getLastContinue() == null)) {
            QAUtil.sleep(100);
        }
        
        
        QAUtil.sleep(300);
        
        IHttpResponse response = respHdl.getLastResponse();
        
        Assert.assertEquals(100, respHdl.getLastContinue().getStatus());
        Assert.assertEquals("Continue", respHdl.getLastContinue().getReason());
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(data, response.getBody().readString());
        
	    httpClient.close();
	    server.close();
	}

	
    @Test
    public void testHttpClientAutoHandledContinueHeader() throws Exception {
        System.out.println("testHttpClientAutoHandledContinueHeader");
        
        HttpServer server = new HttpServer(new ContinueHandler());
        server.start();

        
        HttpClient httpClient = new HttpClient();
        
        HttpRequestHeader requestHeader = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test", "text/plain; charset=iso-8859-1");
        requestHeader.setHeader("Expect", "100-Continue");
        
     
        ResponseHandlerContinueNotSupported respHdl = new ResponseHandlerContinueNotSupported();
        BodyDataSink dataSink = httpClient.send(requestHeader, respHdl);
        dataSink.write("1234567890");
        dataSink.flush();

        
        while ((respHdl.getLastResponse() == null)) {
            QAUtil.sleep(100);
        }
        
        
        IHttpResponse response = respHdl.getLastResponse();
        Assert.assertEquals(200, response.getStatus());
        
        
        httpClient.close();
        server.close();
    }   
    	


    @Test
    public void testHttpClientAutoHandledContinueHeader2() throws Exception {
        System.out.println("testHttpClientAutoHandledContinueHeader2");
        
        HttpServer server = new HttpServer(new ContinueHandler());
        server.start();

        
        HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        
        HttpRequestHeader requestHeader = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test", "text/plain; charset=iso-8859-1");
        requestHeader.setHeader("Expect", "100-Continue");
        
     
        ResponseHandlerContinueNotSupported respHdl = new ResponseHandlerContinueNotSupported();
        BodyDataSink dataSink = httpClient.send(requestHeader, respHdl);
        dataSink.write("1234567890");
        dataSink.close();

        
        while ((respHdl.getLastResponse() == null)) {
            QAUtil.sleep(100);
        }
        
        Assert.assertEquals("got " + respHdl.getLastResponse(), 200, respHdl.getLastResponse().getStatus());
        
        
        httpClient.close();
        server.close();
    }   
	
    
    
   
	
	@Supports100Continue
    private static final class ContinueHandler implements IHttpRequestHandler {
        
        
        public void onRequest(IHttpExchange exchange) throws IOException {
            
            IHttpRequest request = exchange.getRequest();
            
            if ((request.getHeader("Expect") != null) && (request.getHeader("Expect").equalsIgnoreCase("100-Continue"))) {
                
                boolean isCheckEarlyData = request.getBooleanParameter("checkEarlyData", true);
                int waittime = request.getIntParameter("waittime", 0); 
                QAUtil.sleep(waittime);
                
                if (isCheckEarlyData && (request.getNonBlockingBody().available() > 0)) {
                    exchange.sendError(400, "body data already recevied");
                    
                } else {
                    exchange.sendContinueIfRequested();
                    
                    HttpResponse resp = new HttpResponse(200, request.getContentType(), request.getNonBlockingBody());
                    resp.setHeader("X-Handler", "ContinueHandler");
                    exchange.send(resp);
                }
                
            } else {
                HttpResponse resp = new HttpResponse(200, request.getContentType(), request.getNonBlockingBody());
                exchange.send(resp);
            }
        }
        
        
    }	
	
	@Supports100Continue
    private static final class ResponseHandler implements IHttpResponseHandler {
        
        private final AtomicReference<IHttpResponse> lastContinueRef = new AtomicReference<IHttpResponse>();
        private final AtomicReference<IOException> lastExceptionRef = new AtomicReference<IOException>();
        private final AtomicReference<IHttpResponse> lastResponseRef = new AtomicReference<IHttpResponse>();
        private final AtomicReference<BodyDataSink> bodyDataSinkRef = new AtomicReference<BodyDataSink>();
        
        private final String data;
        
        public ResponseHandler(String data) {
            this.data = data; 
        }
         
        
        public void setBodyDataSink(BodyDataSink dataSink) throws IOException {
            bodyDataSinkRef.set(dataSink);
        }
        
        
        @Supports100Continue
        public void onResponse(IHttpResponse response) throws IOException {
            
            if (response.getStatus() == 100) {
                lastContinueRef.set(response);
                
                bodyDataSinkRef.get().write(data);
                bodyDataSinkRef.get().close();
            } else {
                lastResponseRef.set(response);
            }
        }

        
        public void onException(IOException ioe) throws IOException {
            lastExceptionRef.set(ioe);
        }
        
        public IHttpResponse getLastResponse() {
            return lastResponseRef.get();
        }
        
        public IOException getlastException() {
            return lastExceptionRef.get();
        }
        
        public IHttpResponse getLastContinue() {
            return lastContinueRef.get();
        }
    }
    
    
    private static final class ResponseHandlerContinueNotSupported implements IHttpResponseHandler {
        
        private final AtomicReference<IHttpResponse> lastResponseRef = new AtomicReference<IHttpResponse>();
        
        
        public void onResponse(IHttpResponse response) throws IOException {
            lastResponseRef.set(response);           
        }
        
        
        public void onException(IOException ioe) throws IOException {
            
        }
        
        public IHttpResponse getLastResponse() {
            return lastResponseRef.get();
        }
    }	
    
    
    
    private static final class MyServlet extends HttpServlet {
        
        private static final long serialVersionUID = 1L;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            
            Reader reader = req.getReader();
            reader.read();
        }
    }
}
