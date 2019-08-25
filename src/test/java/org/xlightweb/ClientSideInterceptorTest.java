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
import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.DataConverter;
import org.xsocket.Execution;





/**  
*
* @author grro@xlightweb.org
*/
public final class ClientSideInterceptorTest  {
	
    private static final Logger LOG = Logger.getLogger(ClientSideInterceptorTest.class.getName());
    
    
     
    public static void main(String[] args) throws Exception {
   
        
        for (int i = 0; i < 10000; i++) {
            System.out.println("loop " + i);
            new ClientSideInterceptorTest().testAuditRequestOnlyNonThreaded();
        }
        
        System.out.println("end");
    }
      

    
    

    
    @Test
    public void testAuditRequestOnly() throws Exception {
        
        HttpServer server = new HttpServer(new EchoHandler());
        server.start();
       
        HttpClient httpClient = new HttpClient();
        
        RequestAuditHandler auditInterceptor = new RequestAuditHandler();
        httpClient.addInterceptor(auditInterceptor);
        
        String s = "est12345678901234567890"; 
        // String s = "test"; 
        
        IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort()+ "/", "text/plain", s));
        
        
        Assert.assertEquals(200, response.getStatus());
        
        String body = response.getBody().readString();
        if (body.indexOf(s) == -1) {
            System.out.println("got wrong content " + body);
            Assert.fail();
        }
        
        System.out.println("finished");
        
        httpClient.close();
        server.close();
    }
    

 
    
    
    
    @Test
    public void testAuditRequestOnlyNonThreaded() throws Exception {
        
        HttpServer server = new HttpServer(new EchoHandler());
        server.start();
       
        HttpClient httpClient = new HttpClient();
        
        NonThreadedRequestAuditHandler auditInterceptor = new NonThreadedRequestAuditHandler();
        httpClient.addInterceptor(auditInterceptor);
        
        String s = "est12345678901234567890";
        IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort()+ "/", "text/plain", s));
        
        
        Assert.assertEquals(200, response.getStatus());
        
        String body = response.getBody().readString();
        if (body.indexOf(s) == -1) {
            System.out.println("got wrong content " + body);
            Assert.fail();
        }
        
        System.out.println("finished");
        
        httpClient.close();
        server.close();
    }

    
    
    @Test
    public void testAuditResponseOnly() throws Exception {
         
        HttpServer server = new HttpServer(new EchoHandler());
        server.start();
       
        HttpClient httpClient = new HttpClient();
        
        RequestAuditReverseHandler auditInterceptor = new RequestAuditReverseHandler();
        httpClient.addInterceptor(auditInterceptor);
        
        IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort()+ "/", "text/plain", "test12345678901234567890"));
        
        
        Assert.assertEquals(200, response.getStatus());
        
        String body = response.getBody().readString();
        if (body.indexOf("test1234") == -1) {
            System.out.println("got wrong content " + body);
            Assert.fail();
        }
        
        System.out.println("finished");
        
        httpClient.close();
        server.close();
    }    

    
    @Test
    public void testAuditResponseOnlyNonThreaded() throws Exception {
         
        HttpServer server = new HttpServer(new EchoHandler());
        server.start();
       
        HttpClient httpClient = new HttpClient();
        
        NonThreadedRequestAuditReverseHandler auditInterceptor = new NonThreadedRequestAuditReverseHandler();
        httpClient.addInterceptor(auditInterceptor);
        
        IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort()+ "/", "text/plain", "test12345678901234567890"));
        
        
        Assert.assertEquals(200, response.getStatus());
        
        String body = response.getBody().readString();
        if (body.indexOf("test1234") == -1) {
            System.out.println("got wrong content " + body);
            Assert.fail();
        }
        
        System.out.println("finished");
        
        httpClient.close();
        server.close();
    }    
    
    
  
  
    private static class RequestAuditReverseHandler implements IHttpRequestHandler {
        
        
        public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
            
            final StringBuilder requestString = new StringBuilder();
            
            
            IHttpResponseHandler hdl = new IHttpResponseHandler() {
                
                public void onResponse(final IHttpResponse response) throws IOException {
                    
                    requestString.append(response.getResponseHeader().toString());
                    
                    if (response.hasBody()) {
                        
                        NonBlockingBodyDataSource bodyDataSource = response.getNonBlockingBody();
                    
                        BodyDataSink bodyDataSink = exchange.send(response.getResponseHeader());
                        
                        BodyForwarder bodyForwarder = new BodyForwarder(bodyDataSource, bodyDataSink) {
                            
                            private final List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
                            
                            public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException ,IOException {
                                
                                ByteBuffer[] data = bodyDataSource.readByteBufferByLength(bodyDataSource.available());
                                for (ByteBuffer byteBuffer : data) {
                                    buffers.add(byteBuffer.duplicate());
                                }
                                
                                LOG.fine("forwarding " + HttpUtils.computeRemaining(data));
                                bodyDataSink.write(data);
                            };
                            
                            public void onComplete() {
                                try {
                                    requestString.append(DataConverter.toString(buffers, response.getCharacterEncoding()));
                                } catch (UnsupportedEncodingException use) {
                                    requestString.append("<error>");
                                }
                            };
                        };
                        
                        LOG.fine("setDataHandler");
                        response.getNonBlockingBody().setDataHandler(bodyForwarder);
                            
                    } else {
                        exchange.send(response);                 
                    }
                }
                    
                public void onException(IOException ioe) throws IOException {
                    exchange.sendError(ioe);
                }
            };
            
            exchange.forward(exchange.getRequest(), hdl);            
        }
    }
    
	
    @Execution(Execution.NONTHREADED)
    private static class NonThreadedRequestAuditReverseHandler extends RequestAuditReverseHandler {
        
    }
    
    
	   
	   
    private static class RequestAuditHandler implements IHttpRequestHandler {
        
        private StringBuilder requestString = new StringBuilder();

        
        public void onRequest(final IHttpExchange exchange) throws IOException {

            final IHttpRequest req = exchange.getRequest(); 

            // add header audit record
            requestString.append(req.getRequestHeader().toString());
            
            
            // does request contain a body? 
            if (req.hasBody()) {
                
                NonBlockingBodyDataSource bodyDataSource = req.getNonBlockingBody();
            
                BodyDataSink bodyDataSink = exchange.forward(req.getRequestHeader());
                
                BodyForwarder bodyForwarder = new BodyForwarder(bodyDataSource, bodyDataSink) {
                    
                    private final List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
                    
                    public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException ,IOException {
                        
                        ByteBuffer[] data = bodyDataSource.readByteBufferByLength(bodyDataSource.available());
                        for (ByteBuffer byteBuffer : data) {
                            buffers.add(byteBuffer.duplicate());
                        }
                        
                        QAUtil.sleep(1000);
                        bodyDataSink.write(data);
                    };
                    
                    public void onComplete() {
                        try {
                            requestString.append(DataConverter.toString(buffers, req.getCharacterEncoding()));
                        } catch (UnsupportedEncodingException use) {
                            requestString.append("<error>");
                        }
                    };
                };
                
                req.getNonBlockingBody().setDataHandler(bodyForwarder);
                    
            } else {
                // forward request
                exchange.forward(req);                 
            }
        }        
    }

    
    @Execution(Execution.NONTHREADED)
    private static final class NonThreadedRequestAuditHandler extends RequestAuditHandler {
        
    }
    
    
    
    private static final class EchoHandler implements IHttpRequestHandler {
        
        public void onRequest(IHttpExchange exchange) throws IOException {
            
            IHttpRequest request = exchange.getRequest();
            
            HttpResponse response = null;
            if (request.hasBody()) {
                response = new HttpResponse(200, "text/plain", exchange.getRequest().getNonBlockingBody());
            } else {
                response = new HttpResponse(200);
            }
                
            for (String headerName : exchange.getRequest().getHeaderNameSet()) {
                if (headerName.startsWith("X")) {
                    response.setHeader(headerName, exchange.getRequest().getHeader(headerName));
                }
            }
            
            exchange.send(response);
        }
    }
}