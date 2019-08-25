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

import java.nio.BufferUnderflowException;




import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.NonBlockingBodyDataSource;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;





/**
*
* @author grro@xlightweb.org
*/
public final class ServersideInterceptorTest  {
    
    
    public static void main(String[] args) throws Exception {
                
        for (int i = 0; i < 10000; i++) {
            System.out.println("loop " + i);
            new ServersideInterceptorTest().testNonThreadedByteOrientedReverseInterceptor();
        }
    }


	@Test
	public void testSimple() throws Exception {
	    System.out.println("testSimple");
	    
	    RequestHandler reqHdl = new RequestHandler();
	    HttpServer server = new HttpServer(reqHdl);
	    server.start();
	    
	    
	    HttpClient httpClient = new HttpClient(); 
	    
	    FutureResponseHandler hdl = new FutureResponseHandler();
	    BodyDataSink ds = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test"), 100, hdl);
	    for (int i = 0; i < 10; i++) {
            ds.write("0123456789");
        }
        ds.close();
        
        
        IHttpResponse response = hdl.get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(100, response.getBody().readBytes().length);
	    
	    httpClient.close();
		server.close();
	}

	
    @Test
    public void testMessageOrientedInterceptor() throws Exception {
        
        System.out.println("testMessageOrientedInterceptor");

        RequestHandlerChain chain = new RequestHandlerChain();
        chain.addLast(new MessageorientedInterceptor());
        chain.addLast(new RequestHandler());
        HttpServer server = new HttpServer(chain);
        server.start();
        
        
        HttpClient httpClient = new HttpClient(); 
        
        FutureResponseHandler hdl = new FutureResponseHandler();
        BodyDataSink ds = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test"), 100, hdl);
        for (int i = 0; i < 10; i++) {
            ds.write("0123456789");
        }
        ds.close();
        
        
        IHttpResponse response = hdl.get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("true", response.getHeader("X-Intercepted"));
        Assert.assertEquals(100, response.getBody().readBytes().length);
        
        httpClient.close();
        server.close();
    }
	

    
    
    @Test
    public void testByteOrientedInterceptor() throws Exception {
        
        System.out.println("testByteOrientedInterceptor");

        RequestHandlerChain chain = new RequestHandlerChain();
        chain.addLast(new ByteorientedForwardInterceptor());
        chain.addLast(new RequestHandler());
        HttpServer server = new HttpServer(chain);
        server.start();
        
        
        HttpClient httpClient = new HttpClient(); 
        
        FutureResponseHandler hdl = new FutureResponseHandler();
        BodyDataSink ds = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test"), 100, hdl);
        for (int i = 0; i < 10; i++) {
            ds.write("0123456789");
        }
        ds.close();
        
        
        IHttpResponse response = hdl.get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(100, response.getBody().readBytes().length);
        
        httpClient.close();
        server.close();
    }
    
    

    
    @Test
    public void testNonThreadedByteOrientedInterceptor() throws Exception {
        
        System.out.println("testNonThreadedByteOrientedInterceptor");

        RequestHandlerChain chain = new RequestHandlerChain();
        chain.addLast(new NonTheadedByteorientedForwardInterceptor());
        chain.addLast(new RequestHandler());
        HttpServer server = new HttpServer(chain);
        server.start();
        
        
        HttpClient httpClient = new HttpClient(); 
        
        FutureResponseHandler hdl = new FutureResponseHandler();
        BodyDataSink ds = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test"), 100, hdl);
        for (int i = 0; i < 10; i++) {
            ds.write("0123456789");
        }
        ds.close();
        
        
        IHttpResponse response = hdl.get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(100, response.getBody().readBytes().length);
        
        httpClient.close();
        server.close();
    }
        

    @Test
    public void testByteOrientedReverseInterceptor() throws Exception {
        
        System.out.println("testByteOrientedReverseInterceptor");
        
        RequestHandlerChain chain = new RequestHandlerChain();
        chain.addLast(new ByteorientedReverseInterceptor());
        chain.addLast(new RequestHandler());
        HttpServer server = new HttpServer(chain);
        server.start();
        
        
        HttpClient httpClient = new HttpClient(); 
        
        FutureResponseHandler hdl = new FutureResponseHandler();
        BodyDataSink ds = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test"), 100, hdl);
        for (int i = 0; i < 10; i++) {
            ds.write("0123456789");
        }
        ds.close();
        
        
        IHttpResponse response = hdl.get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(100, response.getBody().readBytes().length);
        
        httpClient.close();
        server.close();
    }
    
    

    @Test
    public void testNonThreadedByteOrientedReverseInterceptor() throws Exception {
        
        System.out.println("testNonThreadedByteOrientedReverseInterceptor");
        
        RequestHandlerChain chain = new RequestHandlerChain();
        chain.addLast(new NonThreadedByteorientedReverseInterceptor());
        chain.addLast(new RequestHandler());
        HttpServer server = new HttpServer(chain);
        server.start();
        
        
        HttpClient httpClient = new HttpClient(); 
        
        FutureResponseHandler hdl = new FutureResponseHandler();
        BodyDataSink ds = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test"), 100, hdl);
        for (int i = 0; i < 10; i++) {
            ds.write("0123456789");
        }
        ds.close();
        
        
        IHttpResponse response = hdl.get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(100, response.getBody().readBytes().length);
        
        httpClient.close();
        server.close();
    }    
    
	
	private static final class MessageorientedInterceptor implements IHttpRequestHandler {
	    
	    public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
	        
	        IHttpResponseHandler hdl = new IHttpResponseHandler() {
	          
	            public void onResponse(IHttpResponse response) throws IOException {
	                response.addHeader("X-Intercepted", "true");
	                exchange.send(response);
	            }
	            
	            public void onException(IOException ioe) throws IOException {
	                exchange.sendError(ioe);
	            }
	            
	        };
	        
	        exchange.forward(exchange.getRequest(), hdl);
	    }
	}


	
	
	
    private static class ByteorientedForwardInterceptor implements IHttpRequestHandler {
        
        public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
            
            BodyDataSink ds = exchange.forward(exchange.getRequest().getRequestHeader());
            
            BodyForwarder bf = new BodyForwarder(exchange.getRequest().getNonBlockingBody(), ds) {
                
                @Override
                public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException, IOException {
                    int available = bodyDataSource.available();
                    bodyDataSink.write(bodyDataSource.readByteBufferByLength(available));
                }
            };
            
            exchange.getRequest().getNonBlockingBody().setDataHandler(bf);
        }
    }
	

    @Execution(Execution.NONTHREADED)
    private static final class NonTheadedByteorientedForwardInterceptor extends ByteorientedForwardInterceptor {
    }
    
    
    
    
    private static class ByteorientedReverseInterceptor implements IHttpRequestHandler {
        
        public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {

            
            IHttpResponseHandler hdl = new IHttpResponseHandler() {
              
                public void onResponse(IHttpResponse response) throws IOException {
                    
                    BodyDataSink ds = exchange.send(response.getResponseHeader());
                    
                    NonBlockingBodyDataSource source = response.getNonBlockingBody();
                    BodyForwarder bf = new BodyForwarder(source, ds) {
                        
                        @Override
                        public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException, IOException {
                            int available = bodyDataSource.available();
                            bodyDataSink.write(bodyDataSource.readByteBufferByLength(available));
                        }
                    };
                    
                    response.getNonBlockingBody().setDataHandler(bf);
                }
                
                
                public void onException(IOException ioe) throws IOException {
                    exchange.sendError(ioe);
                }
            };
         
            
            exchange.forward(exchange.getRequest(), hdl);
        }
    }    


    @Execution(Execution.NONTHREADED)
    private static class NonThreadedByteorientedReverseInterceptor extends ByteorientedReverseInterceptor {
    }    

    
	private static final class RequestHandler implements IHttpRequestHandler {
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        
	        BodyDataSink ds = exchange.send(new HttpResponseHeader(200, "text/plain"), 100);
	        for (int i = 0; i < 10; i++) {
	            ds.write("0123456789");
	        }
	        ds.close();	        
	    }
	}

}