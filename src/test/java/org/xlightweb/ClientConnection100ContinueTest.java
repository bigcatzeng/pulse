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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.Server;

import org.xlightweb.server.HttpServer;
import org.xlightweb.client.HttpClientConnection;


 


/**
*
* @author grro@xlightweb.org
*/
public final class ClientConnection100ContinueTest  {

    
    
    public static void main(String[] args) throws Exception {
    
        for (int i = 0; i < 1000; i++) {
            new ClientConnection100ContinueTest().testNon100Continue();
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
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        byte[] data = QAUtil.generateByteArray(400); 
        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/test", "text/plain", data);
        request.setHeader("Expect", "100-Continue");
        
        
        IHttpResponse response = con.call(request);
        
        Assert.assertEquals(true, response.getAttribute("org.xlightweb.HttpClientConnection.100-continueReceived"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertArrayEquals(data, response.getBody().readBytes());
        
        con.close();
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
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        byte[] data = QAUtil.generateByteArray(400); 
        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/test", "text/plain", data);
        request.setHeader("Expect", "100-Continue");
        
        
        IHttpResponse response = con.call(request);
        
        Assert.assertEquals(true, response.getAttribute("org.xlightweb.HttpClientConnection.100-continueReceived"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertArrayEquals(data, response.getBody().readBytes());
        
        con.close();
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
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        byte[] data = QAUtil.generateByteArray(400000); 
        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/test", "text/plain", data);
        request.setHeader("Expect", "100-Continue");
        
        
        IHttpResponse response = con.call(request);
        
        Assert.assertEquals(true, response.getAttribute("org.xlightweb.HttpClientConnection.100-continueReceived"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertArrayEquals(data, response.getBody().readBytes());
        
        con.close();
        server.close();
    }
    
    
    @Test
    public void testNon100Continue() throws Exception {
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            @Supports100Continue
            public void onRequest(IHttpExchange exchange) throws IOException {
                
                IHttpRequest request = exchange.getRequest();
                exchange.sendContinueIfRequested();
                
                exchange.send(new HttpResponse(200, request.getContentType(), request.getNonBlockingBody()));
            }            
        };   
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        byte[] data = QAUtil.generateByteArray(400000); 
        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/test", "text/plain", data);
        
        IHttpResponse response = con.call(request);
        
        Assert.assertNull(response.getAttribute("org.xlightweb.HttpClientConnection.100-continueReceived"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertArrayEquals(data, response.getBody().readBytes());
        
        con.close();
        server.close();
    }
    
    
    @Test
    public void testFileNon100Continue() throws Exception {
        
        System.out.println("testFileNon100Continue");
        
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
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        File file = QAUtil.createTestfile_400k();
        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/test", file);
        IHttpResponse response = con.call(request);
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertNull(response.getAttribute("org.xlightweb.HttpClientConnection.100-continueReceived"));
        Assert.assertTrue(QAUtil.isEquals(file, response.getBody().readByteBuffer()));
        
        con.close();
        server.close();
    }
    

    @Test
    public void testFileAuto100Continue() throws Exception {
        System.out.println("testFileAuto100Continue");
        
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
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        File file = QAUtil.createTestfile_400k();
        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/test", file);
        request.setHeader("Expect", "100-Continue");
        
        IHttpResponse response = con.call(request);
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(true, response.getAttribute("org.xlightweb.HttpClientConnection.100-continueReceived"));
        Assert.assertTrue(QAUtil.isEquals(file, response.getBody().readByteBuffer()));
        
        con.close();
        server.close();
    }
    
        
    
    @Test
    public void testContinueHandlerIgnore() throws Exception {
        
        System.out.println("testContinueHandlerIgnore");
        
        HttpServer server = new HttpServer(new ContinueHandler());
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        HttpRequestHeader requestHeader = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test", "text/plain; charset=iso-8859-1");
        requestHeader.setHeader("Expect", "100-Continue");
        
     
        ResponseHandlerContinueNotSupported respHdl = new ResponseHandlerContinueNotSupported();
        BodyDataSink dataSink = con.send(requestHeader, respHdl);
        dataSink.write("1234567890");
        dataSink.flush();
        
        while ((respHdl.getLastResponse() == null)) {
            QAUtil.sleep(100);
        }

        QAUtil.sleep(300);
        
        IHttpResponse response = respHdl.getLastResponse();

        Assert.assertEquals(200, response.getStatus());
        
        
        con.close();
        server.close();
    }
    
    
    
    
    
    @Test
    public void testManualConnectionContinueHeader() throws Exception {
        System.out.println("testManualConnectionContinueHeader");
        
        HttpServer server = new HttpServer(new ContinueHandler());
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        HttpRequestHeader requestHeader = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test", "text/plain; charset=iso-8859-1");
        requestHeader.setHeader("Expect", "100-Continue");
        
     
        String data = "test1234567890";
        ResponseHandler respHdl = new ResponseHandler(data);
        BodyDataSink dataSink = con.send(requestHeader, respHdl);
        respHdl.setBodyDataSink(dataSink);
        dataSink.flush();

        
        while ((respHdl.getLastResponse() == null) || (respHdl.getLastResponse().getStatus() == 100)) {
            QAUtil.sleep(100);
        }
        
        
        IHttpResponse response = respHdl.getLastResponse();
        
        Assert.assertEquals(100, respHdl.getLastContinue().getStatus());
        Assert.assertEquals("Continue", respHdl.getLastContinue().getReason());
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(data, response.getBody().readString());
        
        con.close();
        server.close();
    }

    
    @Test
    public void testManualConnectionContinueResponseTimeout() throws Exception {
        
        System.out.println("testManualConnectionContinueResponseTimeout");

        IDataHandler dh = new IDataHandler() {
          
            public boolean onData(INonBlockingConnection con) throws IOException {
                con.readStringByDelimiter("\r\n\r\n");
                
                QAUtil.sleep(5000);
                
                con.write("HTTP/1.1 200 OK\r\n" + 
                          "Server: me\r\n" + 
                          "Content-Type: text/plain\r\n" + 
                          "Content-Length: 6\r\n" + 
                          "\r\n" + 
                          "123456"); 
                
                return true;
            }
        };
        Server server = new Server(dh);
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        HttpRequestHeader requestHeader = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test", "text/plain; charset=iso-8859-1");
        requestHeader.setHeader("Expect", "100-Continue");
        
     
        String data = "test1234567890";
        ResponseHandler respHdl = new ResponseHandler(data);
        BodyDataSink dataSink = con.send(requestHeader, respHdl);
        respHdl.setBodyDataSink(dataSink);
        dataSink.flush();

        
        while ((respHdl.getLastResponse() == null)) {
            QAUtil.sleep(100);
        }
        
        
        Assert.assertEquals(100, respHdl.getLastContinue().getStatus());
        Assert.assertEquals("Continue (100-continue response timeout)", respHdl.getLastContinue().getReason());
        Assert.assertEquals(200, respHdl.getLastResponse().getStatus());
        Assert.assertEquals("123456", respHdl.getLastResponse().getBody().readString());
        
        con.close();
        server.close();
    }
    
    
    @Test
    public void testManualConnectionContinueHeaderNonContinueHandler() throws Exception {
        
        System.out.println("testManualConnectionContinueHeaderNonContinueHandler");
        
        HttpServer server = new HttpServer(new ContinueHandler());
        server.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        HttpRequestHeader requestHeader = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test", "text/plain; charset=iso-8859-1");
        requestHeader.setHeader("Expect", "100-Continue");
        
     
        ResponseHandlerContinueNotSupported respHdl = new ResponseHandlerContinueNotSupported();
        BodyDataSink dataSink = con.send(requestHeader, respHdl);
        dataSink.write("1234567890");
        dataSink.close();

        
        while (respHdl.getLastResponse() == null) {
            QAUtil.sleep(100);
        }
        
        IHttpResponse response = respHdl.getLastResponse();
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("1234567890", response.getBody().readString());
        
        con.close();
        server.close();
    }
    
    
    @Test
    public void testUnsupportedAutoConnectionContinueHeader() throws Exception {
        
        System.out.println("testUnsupportedAutoConnectionContinueHeader");
        
        
        HttpServer server = new HttpServer(new ContinueHandler());
        server.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        HttpRequestHeader requestHeader = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test", "text/plain; charset=iso-8859-1");
        requestHeader.setHeader("Expect", "100-Continue");
        
     
        ResponseHandlerContinueNotSupported respHdl = new ResponseHandlerContinueNotSupported();
        BodyDataSink dataSink = con.send(requestHeader, respHdl);
        dataSink.write("1234567890");
        dataSink.close();
        
        while ((respHdl.getLastResponse() == null)) {
            QAUtil.sleep(100);
        }
        
        
        IHttpResponse response = respHdl.getLastResponse();
        Assert.assertEquals(200, response.getStatus());
        
        con.close();
        server.close();
    }
    
    
    @Test
    public void testManualConnectionContinueHeaderUnexpectedResponse() throws Exception {
        
        System.out.println("testManualConnectionContinueHeaderUnexpectedResponse");
        
        IDataHandler dh = new IDataHandler() {
            
            private int state = 0;
            private int size = 0;
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                if (state == 0) {
                    String header = connection.readStringByDelimiter("\r\n\r\n");
                    size = QAUtil.readContentLength(header);
                    state = 1;
                    connection.write("HTTP/1.1 100 Continue\r\n\r\n");
                    
                } if (state == 1) {
                    String data = connection.readStringByLength(size);
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n"+
                                     "Content-Length: " + data.length() + "\r\n" +
                                     "\r\n" +
                                     data);
                    state = 0;
                }
                
                return true;
            }
        };
        
        Server server3 = new Server(dh);
        server3.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server3.getLocalPort());
        System.out.println("conn connected to " + con.getRemotePort());
        
        HttpRequestHeader requestHeader = new HttpRequestHeader("POST", "http://localhost:" + server3.getLocalPort() + "/test", "text/plain; charset=iso-8859-1");
        
        String data = "test1234567890";
        FutureResponseHandler respHdl = new FutureResponseHandler();
        BodyDataSink dataSink = con.send(requestHeader, data.length(), respHdl);
        dataSink.write("test1234567890");
        
        IHttpResponse response = respHdl.getResponse();
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(data, response.getBody().readString());
        
        
        con.close();
        server3.close();
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
}
