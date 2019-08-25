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

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;
import org.xlightweb.QAUtil;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClient.FollowsRedirectMode;
import org.xlightweb.server.HttpServer;




/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientRetryTest {

    public static void main(String[] args) throws Exception {
        
        Logger LOG = Logger.getLogger(HttpClientRetryTest.class.getName());
        
        for (int i = 0; i < 10000; i++) {
            LOG.info("loop " + i);
            new HttpClientRetryTest().testSimpleGetRetry();
        }
    }
    
    
    @Test 
    public void testOrgException() throws Exception {
        IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            private int counter = 0; 
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                exchange.sendError(500 + (counter++));
            }
        };
        HttpServer server = new HttpServer(hdl);
        server.start();

        
        HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        
        GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/test");
        
        IHttpResponse response = httpClient.call(request);
        Assert.assertEquals(500, response.getStatus());
        
        httpClient.close();
        server.close();
    }

    
    @Test 
    public void testOrgException2() throws Exception {
        
        IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            private int counter = 0; 
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                if (counter == 0) {
                    exchange.sendError(500);
                } else {
                    exchange.destroy();
                }
                counter++;
            }
        };
        HttpServer server = new HttpServer(hdl);
        server.start();

        
        HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        
        GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/test");
        
        IHttpResponse response = httpClient.call(request);
        Assert.assertEquals(500, response.getStatus());
        
        httpClient.close();
        server.close();
    }

    
    @Test 
    public void testOrgException3() throws Exception {
        
        IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            private int counter = 0; 
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                if (counter != 0) {
                    exchange.sendError(500);
                } else {
                    exchange.destroy();
                }
                counter++;
            }
        };
        HttpServer server = new HttpServer(hdl);
        server.start();

        
        HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        
        GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/test");
        
        try {
            httpClient.call(request);
            Assert.fail("IOException expected");
        } catch (IOException expected) { }
        
        httpClient.close();
        server.close();
    }

    
    @Test 
    public void testOrgException4() throws Exception {
        
        IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            private int counter = 0; 
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                exchange.sendError(500 + (counter++));
            }
        };
        HttpServer server = new HttpServer(hdl);
        server.start();

        
        HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        
        PutRequest request = new PutRequest("http://localhost:" + server.getLocalPort() + "/test", "text/plain", "0123456789");
        
        IHttpResponse response = httpClient.call(request);
        Assert.assertEquals(500, response.getStatus());
        
        httpClient.close();
        server.close();
    }
    

    
    @Test 
    public void testOrgException5() throws Exception {
        
        IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            private int counter = 0; 
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                if (counter == 0) {
                    exchange.sendError(500);
                } else {
                    exchange.destroy();
                }
                counter++;
            }
        };
        HttpServer server = new HttpServer(hdl);
        server.start();

        
        HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        
        PutRequest request = new PutRequest("http://localhost:" + server.getLocalPort() + "/test", "text/plain", "0123456789");
        
        IHttpResponse response = httpClient.call(request);
        Assert.assertEquals(500, response.getStatus());
        
        httpClient.close();
        server.close();
    }

    
    @Test 
    public void testOrgException6() throws Exception {
        
        IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            private int counter = 0; 
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                if (counter != 0) {
                    exchange.sendError(500);
                } else {
                    exchange.destroy();
                }
                counter++;
            }
        };
        HttpServer server = new HttpServer(hdl);
        server.start();

        
        HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        
        PutRequest request = new PutRequest("http://localhost:" + server.getLocalPort() + "/test", "text/plain", "0123456789");
        
        try {
            httpClient.call(request);
            Assert.fail("IOException expected");
        } catch (IOException expected) { }
        
        httpClient.close();
        server.close();
    }


    
    @Test 
    public void testSimpleGetRetryBulk() throws Exception {
        System.out.println("testSimpleGetRetryBulk");
        for (int i = 0; i < 30; i++) {
            testSimpleGetRetry();
        }
    }
   
    
    @Test 
    public void testSimpleGetRetry() throws Exception {
        System.out.println("testSimpleGetRetry");
        
        IDataHandler dh = new IDataHandler() {

            private int counter = 0; 
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                String header = connection.readStringByDelimiter("\r\n\r\n");

                counter++;
                System.out.println(counter + " call\r\n" + header);

                if (counter < 3) {
                    connection.close();
                    
                } else { 
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n" + 
                                     "Content-length: 5\r\n" +
                                     "\r\n" +
                                     "12345"); 
                }    

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();

        
        HttpClient httpClient = new HttpClient();
        
        GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/test");
        IHttpResponse response = httpClient.call(request);
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345", response.getBody().readString());
        
        httpClient.close();
        server.close();
    }
    
    
    @Test 
    public void testSimpleGetRetry2() throws Exception {
        System.out.println("testSimpleGetRetry2");
        
        IDataHandler dh = new IDataHandler() {

            private int counter = 0; 
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                connection.readStringByDelimiter("\r\n");

                counter++;

                if (counter < 3) {
                    connection.close();
                    
                } else { 
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n" + 
                                     "Content-length: 5\r\n" +
                                     "\r\n" +
                                     "12345"); 
                }    

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();

        
        HttpClient httpClient = new HttpClient();
        
        GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/test");
        IHttpResponse response = httpClient.call(request);
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345", response.getBody().readString());
        
        httpClient.close();
        server.close();
    }
    
    
    @Test 
    public void testSimpleGetRetry3() throws Exception {
        System.out.println("testSimpleGetRetry3");
        
        IDataHandler dh = new IDataHandler() {

            private int counter = 0; 
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                
                counter++;
                System.out.println(counter + " call");

                if (counter < 3) {
                    connection.close();
                    
                } else {
                    connection.readStringByDelimiter("\r\n\r\n");

                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n" + 
                                     "Content-length: 5\r\n" +
                                     "\r\n" +
                                     "12345"); 
                }    

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();

        
        HttpClient httpClient = new HttpClient();
        
        GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/test");
        IHttpResponse response = httpClient.call(request);
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345", response.getBody().readString());
        
        httpClient.close();
        server.close();
    }
    
    
    
    @Test 
    public void testSimpleGetRetry4() throws Exception {
        System.out.println("testSimpleGetRetry4");
        
        IDataHandler dh = new IDataHandler() {

            private int counter = 0; 
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                connection.readStringByDelimiter("\r\n\r\n");

                counter++;

                if (counter < 3) {
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n"); 
                    connection.close();
                    
                } else { 
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n" + 
                                     "Content-length: 5\r\n" +
                                     "\r\n" +
                                     "12345"); 
                }    

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();

        
        HttpClient httpClient = new HttpClient();
        
        GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/test");
        ResponseHandler respHdl = new ResponseHandler();
        httpClient.send(request, respHdl);
        
        
        while (respHdl.getResponse() == null) {
            QAUtil.sleep(100);
        }
        
        IHttpResponse response = respHdl.getResponse();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345", response.getBody().readString());
        
        httpClient.close();
        server.close();
    }
    
    
    @Test 
    public void testSimpleGetRetry5() throws Exception {
        System.out.println("testSimpleGetRetry5");
        
        IDataHandler dh = new IDataHandler() {

            private int counter = 0; 
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                connection.readStringByDelimiter("\r\n\r\n");

                counter++;

                if (counter < 3) {
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n" +
                                     "Content-length: 5\r\n"); 
                    connection.close();
                    
                } else { 
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n" + 
                                     "Content-length: 5\r\n" +
                                     "\r\n" +
                                     "12345"); 
                }    

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();

        
        HttpClient httpClient = new HttpClient();
        
        GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/test");
        ResponseHandler respHdl = new ResponseHandler();
        httpClient.send(request, respHdl);
        
        
        while (respHdl.getResponse() == null) {
            QAUtil.sleep(100);
        }
        
        IHttpResponse response = respHdl.getResponse();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345", response.getBody().readString());
        
        httpClient.close();
        server.close();
    }    
    


    @Test 
    public void testSimpleGetRetryOnServerError() throws Exception {
        System.out.println("testSimpleGetRetryOnServerError");
        
        IDataHandler dh = new IDataHandler() {

            private int counter = 0; 
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                connection.readStringByDelimiter("\r\n\r\n");

                counter++;

                if (counter < 3) {
                    connection.write("HTTP/1.1 500 Error\r\n" +
                                     "Server: me\r\n" +
                                     "Content-length: 0\r\n\r\n"); 
                    connection.close();
                    
                } else { 
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n" + 
                                     "Content-length: 5\r\n" +
                                     "\r\n" +
                                     "12345"); 
                }    

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();

        
        HttpClient httpClient = new HttpClient();
        
        GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/test");
        ResponseHandler respHdl = new ResponseHandler();
        httpClient.send(request, respHdl);
        
        
        while (respHdl.getResponse() == null) {
            QAUtil.sleep(100);
        }
        
        IHttpResponse response = respHdl.getResponse();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345", response.getBody().readString());
        
        httpClient.close();
        server.close();
    }    
        
    
    @InvokeOn(InvokeOn.MESSAGE_RECEIVED)
    private static final class ResponseHandler implements IHttpResponseHandler {

        private final AtomicReference<IHttpResponse> responseRef = new AtomicReference<IHttpResponse>();
            
            
        public void onResponse(IHttpResponse response) throws IOException {
            responseRef.set(response);
        }
        
        public void onException(IOException ioe) throws IOException {
            
        }
        
        public IHttpResponse getResponse() {
            return responseRef.get();
        }
    }
    
    
    
    @Test 
    public void testSimpleGetMaxRetryExceeded() throws Exception {
        System.out.println("testSimpleGetMaxRetryExceeded");
        
        IDataHandler dh = new IDataHandler() {

            private int counter = 0; 
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                connection.readStringByDelimiter("\r\n\r\n");

                counter++;

                if (counter < 8) {
                    connection.close();
                    
                } else { 
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n" + 
                                     "Content-length: 5\r\n" +
                                     "\r\n" +
                                     "12345"); 
                }    

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();

        
        HttpClient httpClient = new HttpClient();
        httpClient.setMaxRetries(6);
        
        GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/test");
        
        try {
            httpClient.call(request);
            Assert.fail("IOException expected");
        } catch (IOException expected) {  }
        
        httpClient.close();
        server.close();
    }
    
    
    
    @Test 
    public void testNoRetry() throws Exception {
        System.out.println("testNoRetry");
        
        IDataHandler dh = new IDataHandler() {

            private int counter = 0; 
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                connection.readStringByDelimiter("\r\n\r\n");

                counter++;

                if (counter < 2) {
                    connection.close();
                    
                } else { 
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n" + 
                                     "Content-length: 5\r\n" +
                                     "\r\n" +
                                     "12345"); 
                }    

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();

        
        HttpClient httpClient = new HttpClient();
        httpClient.setMaxRetries(0);
        
        GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/test");
        
        try {
            httpClient.call(request);
            Assert.fail("IOException expected");
        } catch (IOException expected) {  }
        
        httpClient.close();
        server.close();
    }
    
    
    @Test 
    public void testSimpleDeleteRetry() throws Exception {
        System.out.println("testSimpleDeleteRetry");
        
        IDataHandler dh = new IDataHandler() {

            private int counter = 0; 
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                connection.readStringByDelimiter("\r\n\r\n");

                counter++;
                System.out.println(counter + " call");

                if (counter < 3) {
                    connection.close();
                    
                } else { 
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n" + 
                                     "Content-length: 5\r\n" +
                                     "\r\n" +
                                     "12345"); 
                }    

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();

        
        HttpClient httpClient = new HttpClient();
        
        DeleteRequest request = new DeleteRequest("http://localhost:" + server.getLocalPort() + "/test");
        IHttpResponse response = httpClient.call(request);
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345", response.getBody().readString());
        
        httpClient.close();
        server.close();
    }

 
    
    
    
    @Test 
    public void testPutRetry() throws Exception {
        System.out.println("testPutRetry");
        
        IDataHandler dh = new IDataHandler() {

            private int counter = 0; 
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                connection.readStringByDelimiter("\r\n\r\n");

                counter++;

                if (counter < 3) {
                    connection.close();
                    
                } else { 
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n" + 
                                     "Content-length: 5\r\n" +
                                     "\r\n" +
                                     "12345"); 
                }    

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        PutRequest request = new PutRequest("http://localhost:" + server.getLocalPort() + "/test", "text/plain", "12345");
        IHttpResponse response = httpClient.call(request);
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345", response.getBody().readString());
        
        httpClient.close();
        server.close();
    }
    
    
    @Test 
    public void testPutRetry2() throws Exception {
        System.out.println("testPutRetry2");
        
        IDataHandler dh = new IDataHandler() {

            private int counter = 0;

            private int state = 0;
            private int size = 0;
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                switch (state) {
                case 0:
                    String header = connection.readStringByDelimiter("\r\n\r\n");

                    counter++;

                    if (counter < 3) {
                        connection.close();
                        break;
                        
                    } else {
                        size = QAUtil.readContentLength(header);
                        state = 1;
                    }                        
                    
                default:
                    String body = connection.readStringByLength(size);
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n" + 
                                     "Content-length: " + size + "\r\n" +
                                     "\r\n" +
                                     body); 

                    break;
                }

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        PutRequest request = new PutRequest("http://localhost:" + server.getLocalPort() + "/test", "text/plain", "12345");
        IHttpResponse response = httpClient.call(request);
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345", response.getBody().readString());
        
        httpClient.close();
        server.close();
    }
    

    @Test 
    public void testPutRetryFileBatch() throws Exception {        
        for (int i = 0; i < 20; i++) {
            System.out.print("run " + i);
            testPutRetryFile();
        }
    }
    
    


    @Test 
    public void testPutRetryFile() throws Exception {
        IDataHandler dh = new IDataHandler() {

            private int counter = 0; 
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                String header = connection.readStringByDelimiter("\r\n\r\n");

                counter++;

                if (counter < 3) {
                    connection.close();
                    
                } else {
                    int length = QAUtil.readContentLength(header);
                    while (connection.available() < length) {
                        QAUtil.sleep(20);
                    } 
                  
                    byte[] bytes = connection.readBytesByLength(length);
                    
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n" + 
                                     "Content-length: " + length + "\r\n" +
                                     "\r\n");
                    connection.write(bytes);
                }    

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        File file = QAUtil.createTestfile_80byte();
        PutRequest request = new PutRequest("http://localhost:" + server.getLocalPort() + "/test", file);
        IHttpResponse response = httpClient.call(request);

        Assert.assertEquals(200, response.getStatus());
        
        byte[] bytes = response.getBody().readBytes();
        Assert.assertTrue(QAUtil.isEquals(file, bytes));

        file.delete();
        
        httpClient.close();
        server.close();
    }
    
    

    @Test 
    public void testPutRetryMutlipart() throws Exception {
        
        IDataHandler dh = new IDataHandler() {

            private int counter = 0; 
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                connection.readStringByDelimiter("\r\n\r\n");

                counter++;

                if (counter < 3) {
                    connection.close();
                    
                } else { 
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n" + 
                                     "Content-length: 5\r\n" +
                                     "\r\n" +
                                     "12345"); 
                }    

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        MultipartRequest req = new MultipartRequest("PUT", "http://localhost:" + server.getLocalPort()+ "/test");
        
        File file1 = QAUtil.createTestfile_50byte();
        
        Part part = new Part(file1);
        req.addPart(part);
        
        Part part2 = new Part("text/plain", "0123456789");
        req.addPart(part2);
        
        IHttpResponse response = httpClient.call(req);
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345", response.getBody().readString());
        
        httpClient.close();
        server.close();
    }
    
    
        
    
    @Test 
    public void testPutMaxBufferSizeExceeded() throws Exception {
        System.out.println("testPutMaxBufferSizeExceeded");
        
        final int size = 10000000;
        
        IDataHandler dh = new IDataHandler() {

            private int counter = 0; 
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                connection.resetToReadMark();
                connection.markReadPosition();
                
                connection.readStringByDelimiter("\r\n\r\n");
                connection.readByteBufferByLength(size);

                counter++;

                if (counter < 3) {
                    connection.close();
                    
                } else { 
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n" + 
                                     "Content-length: 5\r\n" +
                                     "\r\n" +
                                     "12345"); 
                }    

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        PutRequest request = new PutRequest("http://localhost:" + server.getLocalPort() + "/test", "text/plain", QAUtil.generateByteArray(size * 2));
        
        try {
            httpClient.call(request);
            Assert.fail("IOException expected");
        } catch (IOException expected) { }
        
        httpClient.close();
        server.close();
        
        System.gc();
    }    
}
