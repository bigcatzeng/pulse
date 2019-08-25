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
package org.xlightweb.client;



import java.io.IOException;

import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.BadMessageException;
import org.xlightweb.GetRequest;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpConnection;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;





 
/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientFetchConnectionTest  {

  

	@Test
	public void testNoInterceptors() throws Exception {
	    
	    IDataHandler dh = new IDataHandler() {
	        
	        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
	        
	            connection.readStringByDelimiter("\r\n\r\n");
	            
	            connection.write("HTTP/1.1 101 Web Socket Protocol Handshake\r\n" + 
	                             "Upgrade: WebSocket\r\n" +
	                             "Connection: Upgrade\r\n" +
	                             "WebSocket-Origin: http://websockets.org:8787\r\n" +
	                             "WebSocket-Location: ws://websockets.org:8787/\r\n\r\n"); 
	            return true;
	        }
	    };
	    
	    IServer server = new Server(dh);
	    server.start();
	  
	    HttpClient client = new HttpClient();
	    client.setAutoHandleCookies(false);
	    client.setMaxRetries(0);
	    
	    GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
	    request.setHeader("Upgrade", "WebSocket");
	    request.setHeader("Connection", "upgrade");
	    request.setHeader("Origin", "http://websockets.org:" + server.getLocalPort());

	    IHttpResponse response = client.call(request);
	    
	    IHttpConnection con = (IHttpConnection) response.getAttribute("org.xlightweb.client.connection");
	    Assert.assertTrue(con.isOpen());
	    	    
	    client.close();
	    server.close();
	}
	
	
	
    @Test
    public void testSimple() throws Exception {
        
        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
            
                connection.readStringByDelimiter("\r\n\r\n");
                
                connection.write("HTTP/1.1 101 Web Socket Protocol Handshake\r\n" + 
                                 "Upgrade: WebSocket\r\n" +
                                 "Connection: Upgrade\r\n" +
                                 "WebSocket-Origin: http://websockets.org:8787\r\n" +
                                 "WebSocket-Location: ws://websockets.org:8787/\r\n\r\n"); 
                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();
      
        HttpClient client = new HttpClient();
        
        GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
        request.setHeader("Upgrade", "WebSocket");
        request.setHeader("Connection", "upgrade");
        request.setHeader("Origin", "http://websockets.org:" + server.getLocalPort());

        IHttpResponse response = client.call(request);
        
        IHttpConnection con = (IHttpConnection) response.getAttribute("org.xlightweb.client.connection");
        Assert.assertTrue(con.isOpen());
                
        client.close();
        server.close();
    }
	
    
    @Test
    public void testCustomInterceptor() throws Exception {
        
        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
            
                connection.readStringByDelimiter("\r\n\r\n");
                
                connection.write("HTTP/1.1 101 Web Socket Protocol Handshake\r\n" + 
                                 "Upgrade: WebSocket\r\n" +
                                 "Connection: Upgrade\r\n" +
                                 "WebSocket-Origin: http://websockets.org:8787\r\n" +
                                 "WebSocket-Location: ws://websockets.org:8787/\r\n\r\n"); 
                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();
      
        HttpClient client = new HttpClient();
        
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
        
                
                IHttpResponseHandler respHdl = new IHttpResponseHandler() {
                    
                    public void onResponse(IHttpResponse response) throws IOException {
                        
                        IHttpResponse newResponse = new HttpResponse(response.getStatus(), response.getContentType(), response.getBody());
                        exchange.send(newResponse);
                    }
                    
                    public void onException(IOException ioe) throws IOException {
                        exchange.sendError(ioe);
                    }
                };        

                exchange.forward(exchange.getRequest(), respHdl);
            }
        };
        
        client.addInterceptor(reqHdl);
        
        
        GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
        request.setHeader("Upgrade", "WebSocket");
        request.setHeader("Connection", "upgrade");
        request.setHeader("Origin", "http://websockets.org:" + server.getLocalPort());

        IHttpResponse response = client.call(request);
        
        IHttpConnection con = (IHttpConnection) response.getAttribute("org.xlightweb.client.connection");
        Assert.assertTrue(con.isOpen());
                
        client.close();
        server.close();
    }
}
