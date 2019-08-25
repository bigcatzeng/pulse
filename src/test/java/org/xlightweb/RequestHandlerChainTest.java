/*
 *  Copyright (c) xsocket.org, 2006 - 2009. All rights reserved.
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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;



import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.RequestHandlerChain;
import org.xlightweb.server.HttpServer;
import org.xlightweb.server.IHttpServer;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;




/**
*
* @author grro@xlightweb.org
*/
public final class RequestHandlerChainTest {

 
	@Test 
	public void testFirstHandler() throws Exception {
 		RequestHandlerChain chain = new RequestHandlerChain();
		
		RequestHandler h1 = new RequestHandler("/test");
		chain.addLast(h1);
		
		RequestHandler h2 = new RequestHandler("/bs");
		chain.addLast(h2);

		IHttpServer server = new HttpServer(chain);
		server.start();
		
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		
		con.write("GET /test HTTP/1.1\r\n" +
		          "Host: localhost:" + server.getLocalPort() + "\r\n" +
		          "User-Agent: me\r\n\r\n"); 
		
		String header = con.readStringByDelimiter("\r\n\r\n");
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertFalse(h1.isForwardedRef.get());
		
		h1.reset();
		h2.reset();
		con.close();
		
		con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET /bs HTTP/1.1\r\n" +
                "Host: localhost:" + server.getLocalPort() + "\r\n" +
                "User-Agent: me\r\n\r\n"); 
      
		header = con.readStringByDelimiter("\r\n\r\n");
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertTrue(h1.isForwardedRef.get());
	    Assert.assertFalse(h2.isForwardedRef.get());
	    
        con.close();
		con.close();
		server.close();
	}

	
	@Test 
	public void testSecondHandler() throws Exception {
        RequestHandlerChain chain = new RequestHandlerChain();
        
        RequestHandler h1 = new RequestHandler("/test");
        chain.addLast(h1);
        
        RequestHandler h2 = new RequestHandler("/bs");
        chain.addLast(h2);

        IHttpServer server = new HttpServer(chain);
        server.start();
        
        
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        
        
        con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("GET /bs HTTP/1.1\r\n" +
                "Host: localhost:" + server.getLocalPort() + "\r\n" +
                "User-Agent: me\r\n\r\n"); 
      
        String header = con.readStringByDelimiter("\r\n\r\n");
        Assert.assertTrue(header.indexOf("200") != -1);
        Assert.assertTrue(h1.isForwardedRef.get());
        Assert.assertFalse(h2.isForwardedRef.get());
        
        h1.reset();
        h2.reset();
        
        con.close();
        server.close();	 
    }

	
	
    @Test 
    public void testNoHandler() throws Exception {
        RequestHandlerChain chain = new RequestHandlerChain();
        
        RequestHandler h1 = new RequestHandler("/test");
        chain.addLast(h1);
        
        RequestHandler h2 = new RequestHandler("/bs");
        chain.addLast(h2);

        IHttpServer server = new HttpServer(chain);
        server.start();
        
        
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        
        con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("GET /tztz HTTP/1.1\r\n" +
                "Host: localhost:" + server.getLocalPort() + "\r\n" +
                "User-Agent: me\r\n\r\n"); 
      
        String header = con.readStringByDelimiter("\r\n\r\n");
        Assert.assertTrue(header.indexOf("404") != -1);
        Assert.assertTrue(h1.isForwardedRef.get());
        Assert.assertTrue(h2.isForwardedRef.get());
        
        con.close();
        server.close();
    }
	
	
	@Test 
    public void testStreamedResponse() throws Exception {
	    
        RequestHandlerChain chain = new RequestHandlerChain();

        
        IHttpRequestHandler rh = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                exchange.getRequest();
                
                BodyDataSink dataSink = exchange.send(new HttpResponseHeader(200));
                dataSink.write("test");
                
                QAUtil.sleep(200);
                dataSink.write("1234");
                dataSink.close();
            }
        };
        chain.addLast(rh);
        
        IHttpServer server = new HttpServer(chain);
        server.start();
        
        
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        
        con.write("GET /test HTTP/1.1\r\n" +
                  "Host: localhost:" + server.getLocalPort() + "\r\n" +
                  "User-Agent: me\r\n\r\n"); 
        
        String header = con.readStringByDelimiter("\r\n\r\n");
        Assert.assertTrue(header.indexOf("200") != -1);
        
        con.close();
        server.close();
    }

	
	
	
	   
    @Test 
    public void testStreamedForward() throws Exception {
        System.setProperty("org.xlightweb.showDetailedError", "true");
        
        RequestHandlerChain chain = new RequestHandlerChain();

        
        IHttpRequestHandler rh = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                IHttpRequest request = exchange.getRequest();
                
                BodyDataSink dataSink = exchange.forward(request.getRequestHeader());
                dataSink.write("addedLine\r\n");
                dataSink.write(request.getBody().readString());
                dataSink.close();
            }
        };
        chain.addLast(rh);
        
        
        IHttpRequestHandler rh2 = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                IHttpRequest request = exchange.getRequest();
                exchange.send(new HttpResponse(200, request.getBody().readString()));
            }
        };
        chain.addLast(rh2);

        
        IHttpServer server = new HttpServer(chain);
        server.start();
        
        
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        
        con.write("POST /test HTTP/1.1\r\n" +
                  "Host: localhost:" + server.getLocalPort() + "\r\n" +
                  "User-Agent: me\r\n" +
                  "Content-Length: 5\r\n" +
                  "\r\n" + 
                  "12345"); 
        
        String header = con.readStringByDelimiter("\r\n\r\n");
        Assert.assertTrue(header.indexOf("200") != -1);
        
        QAUtil.sleep(1000);
        Assert.assertEquals("addedLine\r\n12345", con.readStringByLength(16));
        
        con.close();
        server.close();
    }


	
	private static class RequestHandler implements IHttpRequestHandler {

	    final AtomicReference<String> threadnameRef = new AtomicReference<String>();
	    final AtomicReference<IHttpRequest> requestRef = new AtomicReference<IHttpRequest>();
	    final AtomicBoolean isForwardedRef = new AtomicBoolean(false);
	    
	    private final String path;
	    
	    public RequestHandler(String path) {
	        this.path = path;
        }
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        threadnameRef.set(Thread.currentThread().getName());
	        requestRef.set(exchange.getRequest());
	        
	        if (exchange.getRequest().getRequestURI().startsWith(path)) {
	            exchange.send(new HttpResponse(200, "OK"));
	            
	        } else {
	            isForwardedRef.set(true);
	            exchange.forward(exchange.getRequest());
	        }
	    }
	    
	    public void reset() {
	        threadnameRef.set(null);
	        requestRef.set(null);
	        isForwardedRef.set(false);
	    }
	}

}
