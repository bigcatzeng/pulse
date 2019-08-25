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
import java.net.URL;
import java.nio.BufferUnderflowException;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClient.FollowsRedirectMode;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.NonBlockingConnection;



 


/**
*
* @author grro@xlightweb.org
*/
public final class Server100ContinueTest  {

    
   
	
	@Test
	public void testAutoContinue() throws Exception {

        RequestHandler reqHdl = new RequestHandler();        
		HttpServer server = new HttpServer(reqHdl);
		server.start();
		
		INonBlockingConnection con = new NonBlockingConnection("localhost", server.getLocalPort());
		con.write("POST / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" + 
				  "Expect: 100-Continue\r\n" +
				  "Content-Length: 2000\r\n" +
				  "\r\n");

		
		
        while (reqHdl.getLastExchange() == null) {
            QAUtil.sleep(50);
        }
        
        reqHdl.getLastExchange().getRequest().getContentLength();
        
        
        QAUtil.sleep(500);
        try {
            con.readStringByDelimiter("\r\n\r\n");
            Assert.fail("BufferUnderflowException expected");
        } catch (BufferUnderflowException expected) { }

        reqHdl.getLastExchange().getRequest().getNonBlockingBody().available();

        QAUtil.sleep(500);
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("100") != -1);

		con.close();
		server.close();
	}
	
	

    
    @Test
     public void testAutoContinueBodyAccess() throws Exception {
         RequestHandler reqHdl = new RequestHandler();        
         HttpServer server = new HttpServer(reqHdl);
         server.start();
         
         INonBlockingConnection con = new NonBlockingConnection("localhost", server.getLocalPort());
         con.write("POST / HTTP/1.1\r\n" +
                   "Host: localhost\r\n" +
                   "User-Agent: me\r\n" + 
                   "Expect: 100-Continue\r\n" +
                   "Content-Length: 2000\r\n" +
                   "\r\n");
         
         while (reqHdl.getLastExchange() == null) {
             QAUtil.sleep(50);
         }
         
         IHttpExchange serverExchange = reqHdl.getLastExchange();
         IHttpRequest srvReq = serverExchange.getRequest();
         srvReq.getHeader("Content-Length");
         
         QAUtil.sleep(500);
         try {
             con.readStringByDelimiter("\r\n\r\n");
             Assert.fail("BufferUnderflowException expected");
         } catch (BufferUnderflowException expected) { }
         
         srvReq.getBody().isOpen();
         
         QAUtil.sleep(500);
         Assert.assertTrue(con.readStringByDelimiter("\r\n\r\n").indexOf("100") != -1);
         
         con.close();
         server.close();
     }
    
    
    
    @Test
     public void testAutoContinueUrlEncodedBody() throws Exception {
         RequestHandler reqHdl = new RequestHandler();        
         HttpServer server = new HttpServer(reqHdl);
         server.start();
         
         INonBlockingConnection con = new NonBlockingConnection("localhost", server.getLocalPort());
         con.write("POST / HTTP/1.1\r\n" +
                   "Host: localhost\r\n" +
                   "User-Agent: me\r\n" + 
                   "Expect: 100-Continue\r\n" +
                   "Content-Type: application/x-www-form-urlencoded\r\n" +
                   "Content-Length: 2000\r\n" +
                   "\r\n");
         
         
         QAUtil.sleep(500);
         Assert.assertTrue(con.readStringByDelimiter("\r\n\r\n").indexOf("100") != -1);
         
         
         con.close();
         server.close();
     }    

	
    @Test
    public void testManualContinue() throws Exception {

        ContinueHandler hdl = new ContinueHandler();
        HttpServer server = new HttpServer(hdl);
        server.start();
        
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("POST / HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" + 
                  "Expect: 100-Continue\r\n" +
                  "Content-Length: 2000\r\n" +
                  "\r\n");
        
        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("100") != -1);
        
        QAUtil.sleep(300);
        
        Assert.assertTrue(hdl.is100ContinueSent());
        
        con.write(QAUtil.generateByteArray(2000));

        header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("200") != -1);
        int length = QAUtil.readContentLength(header);
        String txt = con.readStringByLength(length);
        Assert.assertEquals("OK", txt);
        
        
        
        con.close();
        server.close();
    }


    @Test
    public void testManualContinue2() throws Exception {

        ContinueHandler2 hdl = new ContinueHandler2();
        HttpServer server = new HttpServer(hdl);
        server.start();
        
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("POST / HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" + 
                  "Expect: 100-Continue\r\n" +
                  "Content-Length: 2000\r\n" +
                  "\r\n");
        
        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("100") != -1);
        QAUtil.sleep(300);
        
        Assert.assertTrue(hdl.is100ContinueSent());
        
        con.write(QAUtil.generateByteArray(2000));

        header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("200") != -1);
        int length = QAUtil.readContentLength(header);
        String txt = con.readStringByLength(length);
        Assert.assertEquals("OK", txt);
        
        
        
        con.close();
        server.close();
    }
    
    @Test
    public void testManualContinue3() throws Exception {
        
        
        IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            @Supports100Continue
            public void onRequest(IHttpExchange exchange) throws IOException {
                
                exchange.send(new HttpResponse(100));  // send continue
                
                exchange.send(new HttpResponse(200, "text/plain", "OK"));
            }
        };

        HttpServer server = new HttpServer(hdl);
        server.start();
        
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("POST / HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" + 
                  "Expect: 100-Continue\r\n" +
                  "Content-Length: 2000\r\n" +
                  "\r\n");
        
        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("100") != -1);
        
        QAUtil.sleep(300);
        
        con.write(QAUtil.generateByteArray(2000));

        header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("200") != -1);
        int length = QAUtil.readContentLength(header);
        String txt = con.readStringByLength(length);
        Assert.assertEquals("OK", txt);
        
        
        
        con.close();
        server.close();
    }
    

    @Test
    public void testManualContinueTwice() throws Exception {

        IHttpRequestHandler hdl = new IHttpRequestHandler() {

            @Supports100Continue
            public void onRequest(IHttpExchange exchange) throws IOException {
                exchange.sendContinueIfRequested();
                exchange.sendContinueIfRequested();
                
                exchange.send(new HttpResponse(200, "text/plain", "OK"));
            }
        };
        
        
        HttpServer server = new HttpServer(hdl);
        server.start();
        
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("POST / HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" + 
                  "Expect: 100-Continue\r\n" +
                  "Content-Length: 2000\r\n" +
                  "\r\n");
        
        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("100") != -1);
        
        QAUtil.sleep(300);
        
        con.write(QAUtil.generateByteArray(2000));

        header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("200") != -1);
        int length = QAUtil.readContentLength(header);
        String txt = con.readStringByLength(length);
        Assert.assertEquals("OK", txt);
        
        
        
        con.close();
        server.close();
    }
    
    
    
    @Test
    public void testApacheClient() throws Exception {
        
      
        ContinueHandler hdl = new ContinueHandler();
        HttpServer server = new HttpServer(hdl);
        server.start();


        org.apache.commons.httpclient.HttpClient httpClient = new org.apache.commons.httpclient.HttpClient();
        httpClient.getParams().setParameter("http.protocol.expect-continue", true);

        PostMethod meth = new PostMethod("http://localhost:" + server.getLocalPort() + "/test");
        meth.setRequestBody("OK");

        httpClient.executeMethod(meth);
        
        Assert.assertEquals(200, meth.getStatusCode());
        Assert.assertEquals("OK", meth.getResponseBodyAsString());
        
        meth.releaseConnection();
        
        server.close();
    }

    
    @Test
    public void testManualContinueWithMissingAnnotation() throws Exception {

        MissingAnnotationContinueHandler hdl = new MissingAnnotationContinueHandler();
        HttpServer server = new HttpServer(hdl);
        server.start();
        
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("POST / HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" + 
                  "Expect: 100-Continue\r\n" +
                  "Content-Length: 2000\r\n" +
                  "\r\n");
        
        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("100") != -1);
        
        con.write(QAUtil.generateByteArray(2000));

        header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("200") != -1);
        int length = QAUtil.readContentLength(header);
        String txt = con.readStringByLength(length);
        Assert.assertEquals("OK", txt);
        
        
        
        con.close();
        server.close();
    }

    
    
    @Test
    public void testAutoContinueWithInterceptor() throws Exception {

        IHttpRequestHandler interceptor = new IHttpRequestHandler() {
            
            public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
                IHttpRequest request = exchange.getRequest();
                request.addHeader("X-Intercepted", "true");
                
                IHttpResponseHandler hdl = new IHttpResponseHandler() {
                    
                    public void onResponse(IHttpResponse response) throws IOException {
                        response.addHeader("X-Intercepted", "true");
                        exchange.send(response);
                    }
                    
                    public void onException(IOException ioe) throws IOException {
                        exchange.sendError(ioe);
                    }
                };
                
                exchange.forward(request, hdl);
                
            }
        };
        

        RequestHandler reqHdl = new RequestHandler();        
        
        RequestHandlerChain chain = new RequestHandlerChain();
        chain.addLast(interceptor);
        chain.addLast(reqHdl);
        
        HttpServer server = new HttpServer(chain);
        server.start();
        
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("POST / HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" + 
                  "Expect: 100-Continue\r\n" +
                  "Content-Length: 2000\r\n" +
                  "\r\n");
        
        
        while (reqHdl.getLastExchange() == null) {
            QAUtil.sleep(50);
        }
        
        reqHdl.getLastExchange().getRequest().getNonBlockingBody().setDataHandler(null);
        
        reqHdl.getLastExchange().send(new HttpResponse(200, "text/plain", "OK"));

        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("100") != -1);
        
        con.write(QAUtil.generateByteArray(2000));

        header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("200") != -1);
        Assert.assertTrue(header.indexOf("X-Intercepted") != -1);
        int length = QAUtil.readContentLength(header);
        String txt = con.readStringByLength(length);
        Assert.assertEquals("OK", txt);
        
        
        
        con.close();
        server.close();
    }


    
    @Test
    public void testAutoContinueWithNestedInterceptor() throws Exception {
        
        IHttpRequestHandler interceptor = new IHttpRequestHandler() {
            
            public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
                IHttpRequest request = exchange.getRequest();
                request.addHeader("X-Intercepted1", "true");
                
                IHttpResponseHandler hdl = new IHttpResponseHandler() {
                    
                    public void onResponse(IHttpResponse response) throws IOException {
                        response.addHeader("X-Intercepted1", "true");
                        exchange.send(response);
                    }
                    
                    public void onException(IOException ioe) throws IOException {
                        exchange.sendError(ioe);
                    }
                };
                
                exchange.forward(request, hdl);
                
            }
        };
        

        IHttpRequestHandler interceptor2 = new IHttpRequestHandler() {
            
            public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
                IHttpRequest request = exchange.getRequest();
                request.addHeader("X-Intercepted2", "true");
                
                IHttpResponseHandler hdl = new IHttpResponseHandler() {
                    
                    public void onResponse(IHttpResponse response) throws IOException {
                        response.addHeader("X-Intercepted2", "true");
                        exchange.send(response);
                    }
                    
                    public void onException(IOException ioe) throws IOException {
                        exchange.sendError(ioe);
                    }
                };
                
                exchange.forward(request, hdl);
            }
        };

        
        RequestHandler reqHdl = new RequestHandler();        

        RequestHandlerChain chain = new RequestHandlerChain();
        chain.addLast(interceptor);
        chain.addLast(reqHdl);
        
        RequestHandlerChain outerChain = new RequestHandlerChain();
        outerChain.addLast(interceptor2);
        outerChain.addLast(chain);
        
        
        HttpServer server = new HttpServer(outerChain);
        server.start();
        
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("POST / HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" + 
                  "Expect: 100-Continue\r\n" +
                  "Content-Length: 2000\r\n" +
                  "\r\n");
        
        while (reqHdl.getLastExchange() == null) {
            QAUtil.sleep(50);
        }
        
        reqHdl.getLastExchange().getRequest().getNonBlockingBody().getBodyDataReceiveTimeoutMillis();
        reqHdl.getLastExchange().send(new HttpResponse(200, "text/plain", "OK"));

        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("100") != -1);
        
        con.write(QAUtil.generateByteArray(2000));

        header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("200") != -1);
        Assert.assertTrue(header.indexOf("X-Intercepted1") != -1);
        Assert.assertTrue(header.indexOf("X-Intercepted2") != -1);
        int length = QAUtil.readContentLength(header);
        String txt = con.readStringByLength(length);
        Assert.assertEquals("OK", txt);
        
        
        
        con.close();
        server.close();
    }



    @Test
    public void testAutoContinueWithNestedInterceptor2() throws Exception {

        IHttpRequestHandler interceptor = new IHttpRequestHandler() {

            @Supports100Continue
            public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
                IHttpRequest request = exchange.getRequest();
                request.addHeader("X-Intercepted1", "true");
                
                IHttpResponseHandler hdl = new IHttpResponseHandler() {
                    
                    public void onResponse(IHttpResponse response) throws IOException {
                        response.addHeader("X-Intercepted1", "true");
                        exchange.send(response);
                    }
                    
                    public void onException(IOException ioe) throws IOException {
                        exchange.sendError(ioe);
                    }
                };
                
                exchange.forward(request, hdl);
                
            }
        };
        

        IHttpRequestHandler interceptor2 = new IHttpRequestHandler() {
            
            public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
                IHttpRequest request = exchange.getRequest();
                request.addHeader("X-Intercepted2", "true");
                
                IHttpResponseHandler hdl = new IHttpResponseHandler() {
                    
                    public void onResponse(IHttpResponse response) throws IOException {
                        response.addHeader("X-Intercepted2", "true");
                        exchange.send(response);
                    }
                    
                    public void onException(IOException ioe) throws IOException {
                        exchange.sendError(ioe);
                    }
                };
                
                exchange.forward(request, hdl);
                
            }
        };

        RequestHandler reqHdl = new RequestHandler();        

        RequestHandlerChain chain = new RequestHandlerChain();
        chain.addLast(interceptor);
        chain.addLast(reqHdl);
        
        RequestHandlerChain outerChain = new RequestHandlerChain();
        outerChain.addLast(interceptor2);
        outerChain.addLast(chain);
        
        
        HttpServer server = new HttpServer(outerChain);
        server.start();
        
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("POST / HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" + 
                  "Expect: 100-Continue\r\n" +
                  "Content-Length: 2000\r\n" +
                  "\r\n");
        
        while (reqHdl.getLastExchange() == null) {
            QAUtil.sleep(50);
        }
        
        reqHdl.getLastExchange().getRequest().getBody().markReadPosition();
        reqHdl.getLastExchange().send(new HttpResponse(200, "text/plain", "OK"));
        
        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("100") != -1);
        
        con.write(QAUtil.generateByteArray(2000));

        header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("200") != -1);
        Assert.assertTrue(header.indexOf("X-Intercepted1") != -1);
        Assert.assertTrue(header.indexOf("X-Intercepted2") != -1);
        int length = QAUtil.readContentLength(header);
        String txt = con.readStringByLength(length);
        Assert.assertEquals("OK", txt);
        
        
        
        con.close();
        server.close();
    }
    
    
    
    @Test
    public void testManualContinueWithNestedInterceptor() throws Exception {
        
        IHttpRequestHandler interceptor = new IHttpRequestHandler() {

            @Supports100Continue
            public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
                IHttpRequest request = exchange.getRequest();
                request.addHeader("X-Intercepted1", "true");
                
                IHttpResponseHandler hdl = new IHttpResponseHandler() {
                    
                    public void onResponse(IHttpResponse response) throws IOException {
                        response.addHeader("X-Intercepted1", "true");
                        exchange.send(response);
                    }
                    
                    public void onException(IOException ioe) throws IOException {
                        exchange.sendError(ioe);
                    }
                };
                
                exchange.forward(request, hdl);
                
            }
        };
        

        IHttpRequestHandler interceptor2 = new IHttpRequestHandler() {

            @Supports100Continue
            public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
                IHttpRequest request = exchange.getRequest();
                request.addHeader("X-Intercepted2", "true");
                
                IHttpResponseHandler hdl = new IHttpResponseHandler() {
                    
                    public void onResponse(IHttpResponse response) throws IOException {
                        response.addHeader("X-Intercepted2", "true");
                        exchange.send(response);
                    }
                    
                    public void onException(IOException ioe) throws IOException {
                        exchange.sendError(ioe);
                    }
                };
                
                exchange.forward(request, hdl);
                
            }
        };

        
        RequestHandlerChain chain = new RequestHandlerChain();
        chain.addLast(interceptor);
        ContinueHandler hdl = new ContinueHandler();
        chain.addLast(hdl);
        
        RequestHandlerChain outerChain = new RequestHandlerChain();
        outerChain.addLast(interceptor2);
        outerChain.addLast(chain);
        
        
        HttpServer server = new HttpServer(outerChain);
        server.start();
        
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("POST / HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" + 
                  "Expect: 100-Continue\r\n" +
                  "Content-Length: 2000\r\n" +
                  "\r\n");
        
        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("100") != -1);
        Assert.assertTrue(hdl.is100ContinueSent());
        
        con.write(QAUtil.generateByteArray(2000));

        header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("200") != -1);
        Assert.assertTrue(header.indexOf("X-Intercepted1") != -1);
        Assert.assertTrue(header.indexOf("X-Intercepted2") != -1);
        int length = QAUtil.readContentLength(header);
        String txt = con.readStringByLength(length);
        Assert.assertEquals("OK", txt);
        
        
        
        con.close();
        server.close();
    }
    


    
    
    @Test
    public void testManualContinueWithNonAnnotatedInterceptor() throws Exception {

        IHttpRequestHandler interceptor = new IHttpRequestHandler() {
            
            public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
                IHttpRequest request = exchange.getRequest();
                request.addHeader("X-Intercepted", "true");
                
                IHttpResponseHandler hdl = new IHttpResponseHandler() {
                    
                    public void onResponse(IHttpResponse response) throws IOException {
                        response.addHeader("X-Intercepted", "true");
                        exchange.send(response);
                    }
                    
                    public void onException(IOException ioe) throws IOException {
                        exchange.sendError(ioe);
                    }
                };
                
                exchange.forward(request, hdl);
                
            }
        };
        
        
        RequestHandlerChain chain = new RequestHandlerChain();
        chain.addLast(interceptor);
        ContinueHandler hdl = new ContinueHandler();
        chain.addLast(hdl);
        
        HttpServer server = new HttpServer(chain);
        server.start();
        
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("POST / HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" + 
                  "Expect: 100-Continue\r\n" +
                  "Content-Length: 2000\r\n" +
                  "\r\n");
        
        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("100") != -1);
        
        con.write(QAUtil.generateByteArray(2000));

        header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        if (header.indexOf("200") == -1) {
            Assert.fail();
        }

        Assert.assertTrue(header.indexOf("X-Intercepted") != -1);
        int length = QAUtil.readContentLength(header);
        String txt = con.readStringByLength(length);
        Assert.assertEquals("OK", txt);
        
        
        
        con.close();
        server.close();
    }


    
    @Test
    public void testManualContinueWithAnnotatedInterceptor() throws Exception {

        IHttpRequestHandler interceptor = new IHttpRequestHandler() {
            
            @Supports100Continue
            public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
                IHttpRequest request = exchange.getRequest();
                request.addHeader("X-Intercepted", "true");
                
                IHttpResponseHandler hdl = new IHttpResponseHandler() {
                    
                    public void onResponse(IHttpResponse response) throws IOException {
                        response.addHeader("X-Intercepted", "true");
                        exchange.send(response);
                    }
                    
                    public void onException(IOException ioe) throws IOException {
                        exchange.sendError(ioe);
                    }
                };
                
                exchange.forward(request, hdl);
                
            }
        };
        
        
        RequestHandlerChain chain = new RequestHandlerChain();
        chain.addLast(interceptor);
        ContinueHandler hdl = new ContinueHandler();
        chain.addLast(hdl);
        
        HttpServer server = new HttpServer(chain);
        server.start();
        
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("POST / HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" + 
                  "Expect: 100-Continue\r\n" +
                  "Content-Length: 2000\r\n" +
                  "\r\n");
        
        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("100") != -1);
        Assert.assertTrue(hdl.is100ContinueSent());
        Assert.assertTrue(header.indexOf("X-Intercepted: true") != -1);
        
        con.write(QAUtil.generateByteArray(2000));

        header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("200") != -1);
        Assert.assertTrue(header.indexOf("X-Intercepted") != -1);
        int length = QAUtil.readContentLength(header);
        String txt = con.readStringByLength(length);
        Assert.assertEquals("OK", txt);
        
        
        
        con.close();
        server.close();
    }

    
    @Test
    public void testAutoContinueProxy() throws Exception {
        RequestHandler reqHdl = new RequestHandler();        
        final HttpServer server = new HttpServer(reqHdl);
        server.start();

        
        IHttpRequestHandler proxyHdl =  new IHttpRequestHandler() {
            
            private HttpClient httpClient = new HttpClient();
            
            public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
                
                IHttpResponseHandler respHdl = new IHttpResponseHandler() {
                    
                    public void onResponse(IHttpResponse response) throws IOException {
                        exchange.send(response);
                    }
                    
                    public void onException(IOException ioe) throws IOException {
                        exchange.sendError(ioe);
                    }
                };
                
                IHttpRequest request = exchange.getRequest();

                URL url = request.getRequestUrl();
                URL newUrl = new URL(url.getProtocol(), "localhost", server.getLocalPort(), url.getFile());
                request.setRequestUrl(newUrl);
                
                httpClient.send(request, respHdl);
            }
        };
        
        HttpServer proxy = new HttpServer(proxyHdl);
        proxy.start();
        
        
        
        INonBlockingConnection con = new NonBlockingConnection("localhost", proxy.getLocalPort());
        con.write("POST / HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" + 
                  "Expect: 100-Continue\r\n" +
                  "Content-Length: 2000\r\n" +
                  "\r\n");

        
        
        QAUtil.sleep(500);
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("100") != -1);


        con.close();
        proxy.close();
        server.close();
    }
    
    
    @Test
    public void testAutoContinueContinueProxy() throws Exception {

        RequestHandler reqHdl = new RequestHandler();        
        final HttpServer server = new HttpServer(reqHdl);
        server.start();

        final HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        httpClient.setCacheMaxSizeKB(1000);
        
        
        IHttpRequestHandler proxyHdl =  new IHttpRequestHandler() {
            
            public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
                
                IHttpResponseHandler respHdl = new IHttpResponseHandler() {
                    
                    @Supports100Continue
                    public void onResponse(IHttpResponse response) throws IOException {
                        exchange.send(response);
                    }
                    
                    public void onException(IOException ioe) throws IOException {
                        exchange.sendError(ioe);
                    }
                };
                
                IHttpRequest request = exchange.getRequest();

                URL url = request.getRequestUrl();
                URL newUrl = new URL(url.getProtocol(), "localhost", server.getLocalPort(), url.getFile());
                request.setRequestUrl(newUrl);
                
                httpClient.send(request, respHdl);
            }
        };
        
        HttpServer proxy = new HttpServer(proxyHdl);
        proxy.start();
        
        
        
        INonBlockingConnection con = new NonBlockingConnection("localhost", proxy.getLocalPort());
        con.write("PUT / HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" + 
                  "Expect: 100-Continue\r\n" +
                  "Content-Length: 2000\r\n" +
                  "\r\n");

        
        
        while (reqHdl.getLastExchange() == null) {
            QAUtil.sleep(50);
        }
        
        reqHdl.getLastExchange().getRequest().getContentLength();
        
        
        QAUtil.sleep(500);
        try {
            String header = con.readStringByDelimiter("\r\n\r\n");
            Assert.fail("BufferUnderflowException expected");
        } catch (BufferUnderflowException expected) { }

        reqHdl.getLastExchange().getRequest().getNonBlockingBody().available();

        QAUtil.sleep(500);
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        Assert.assertTrue(header.indexOf("100") != -1);

        httpClient.close();
        con.close();
        proxy.close();
        server.close();
    }
        

    private static final class RequestHandler implements IHttpRequestHandler {

        private final AtomicReference<IHttpExchange> lastExchangeRef = new AtomicReference<IHttpExchange>();
        
        public void onRequest(IHttpExchange exchange) throws IOException {
            lastExchangeRef.set(exchange);
        }
        
        public IHttpExchange getLastExchange() {
            return lastExchangeRef.get();
        }
    }

	
	@Supports100Continue
	private static final class ContinueHandler implements IHttpRequestHandler {
	    
	    private AtomicBoolean is100ContinueSent = new AtomicBoolean(false);  


	    public void onRequest(IHttpExchange exchange) throws IOException {
	        is100ContinueSent.set(exchange.sendContinueIfRequested());
	        exchange.send(new HttpResponse(200, "text/plain", "OK"));
	    }
	    
	    public boolean is100ContinueSent() {
	        return is100ContinueSent.get();
	    } 
	}
	
	
	private static final class ContinueHandler2 implements IHttpRequestHandler {

	    private AtomicBoolean is100ContinueSent = new AtomicBoolean(false);  
	    
	    @Supports100Continue
	    public void onRequest(IHttpExchange exchange) throws IOException {
	            
	        is100ContinueSent.set(exchange.sendContinueIfRequested());
	        exchange.send(new HttpResponse(200, "text/plain", "OK"));
	    }
	    
	    public boolean is100ContinueSent() {
	        return is100ContinueSent.get();
	    }
	}

	
	private static final class MissingAnnotationContinueHandler implements IHttpRequestHandler {

	    private AtomicBoolean is100ContinueSent = new AtomicBoolean(false);  
	    
        public void onRequest(IHttpExchange exchange) throws IOException {
                
            is100ContinueSent.set(exchange.sendContinueIfRequested());
            exchange.send(new HttpResponse(200, "text/plain", "OK"));
        }
        
        public boolean is100ContinueSent() {
            return is100ContinueSent.get();
        }
    }
}
