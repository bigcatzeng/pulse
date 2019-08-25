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


import java.net.InetAddress;
import java.nio.BufferUnderflowException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.client.IHttpClientEndpoint;
import org.xlightweb.server.HttpServer;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IConnectExceptionHandler;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.NonBlockingConnection;

 


/**
*
* @author grro@xlightweb.org
*/
public final class AsyncConnectTest  {

	
	@Test
	public void testLifeAsyncConnect() throws Exception {
	     
	    ConnectHandler ch = new ConnectHandler();
	    new NonBlockingConnection(InetAddress.getByName("www.web.de"), 80, ch, false, 2000);
	    
	    QAUtil.sleep(1500);
	    
	    IHttpClientEndpoint httpEndpoint = ch.getHttpConnection();
	    IHttpResponse response = httpEndpoint.call(new GetRequest("/"));

	    Assert.assertTrue((response.getStatus() >= 200) && (response.getStatus() < 400));
	}

	
	@Test
	public void testSimple() throws Exception {
	         
	    IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                exchange.send(new HttpResponse(200, "text/plain", "OK"));
            }
        };
        HttpServer server = new HttpServer(hdl);
        server.start();

        
	    HttpClient httpClient = new HttpClient();

	    for (int i = 0; i < 10; i++) {
    	    HttpResponseHandler respHdl = new HttpResponseHandler();
    	    httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"), respHdl);
    	    
    	    while (respHdl.getResponse() == null) {
    	        QAUtil.sleep(50);
    	    }
	    
    	    Assert.assertEquals(200, respHdl.getResponse().getStatus());
	    }
	}

	
    @Test
    public void testSync() throws Exception {
             
        IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                exchange.send(new HttpResponse(200, "text/plain", "OK"));
            }
        };
        HttpServer server = new HttpServer(hdl);
        server.start();

        
        HttpClient httpClient = new HttpClient();

        for (int i = 0; i < 10; i++) {
            IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
            Assert.assertEquals(200, response.getStatus());
        }
    }
	
	
	private static final class HttpResponseHandler implements IHttpResponseHandler {

	    private final AtomicReference<IHttpResponse> respRef = new AtomicReference<IHttpResponse>();
	    
	    
	    public void onResponse(IHttpResponse response) throws IOException {
	        respRef.set(response);
	    }
	    
	    public void onException(IOException ioe) throws IOException {
	        // TODO Auto-generated method stub
	        
	    }
	    
	    
	    IHttpResponse getResponse() {
	        return respRef.get();
	    }
	}
	
	
	
	@Ignore
	@Test
	public void testAsyncConnectFailed() throws Exception {
	        
		int connectionTimeoutMillis = 1000;
		
	    ConnectHandler ch = new ConnectHandler();
	    new NonBlockingConnection(InetAddress.getByName("192.168.255.255"), 80, ch, false, connectionTimeoutMillis);
	        
	    QAUtil.sleep(3000);
	    
	    if (ch.getIOException() == null) {
	    	System.out.println("exception expected");
	    	Assert.fail("exception expected");
	    } else {
	    	System.out.println("expected exception: " + ch.getIOException().toString());
	    }	        
	}

	
	private static final class ConnectHandler implements IConnectHandler, IConnectExceptionHandler {
	    
	    private HttpClientConnection httpCon; 
	    private IOException ioe;
	    
	    public boolean onConnect(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
	        httpCon = new HttpClientConnection(connection);
	        return true;
	    }
	    
	  
	    public boolean onConnectException(INonBlockingConnection connection, IOException ioe) throws IOException {
	        this.ioe = ioe;
	        return true;
	    }
	    
	    
	    HttpClientConnection getHttpConnection() {
	        return httpCon;
	    }
	    
	    IOException getIOException( ) {
	        return ioe;
	    }
	}
}
