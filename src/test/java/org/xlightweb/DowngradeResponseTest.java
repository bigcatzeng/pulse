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
import java.nio.channels.ClosedChannelException;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.BodyDataSink;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.Server;




/**
*
* @author grro@xlightweb.org
*/
public final class DowngradeResponseTest  {
    
    
    public static void main(String[] args) throws Exception {
        
        for (int i = 0; i < 1000; i++) {
            new DowngradeResponseTest().testSimpleHttp_0_9_Response();
        }
    }

	
	@Test
	public void testSimpleHttp_0_9_Response() throws Exception {
	    
	    IDataHandler dh = new IDataHandler() {
	        
	        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
	            connection.readStringByDelimiter("\r\n");
	            connection.write("<html>this ...");
	            connection.close();
	            return true;
	        }
	    };
	    final IServer oldServer = new Server(dh);
	    oldServer.start();
	    
	    
	    IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
	      
	        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	            HttpClientConnection con = new HttpClientConnection("localhost", oldServer.getLocalPort());
	            IHttpResponse response = con.call(new GetRequest("http://localhost:" + oldServer.getLocalPort() + "/"));
	            
	            exchange.send(response);
	        }
	    };
	    
		IServer server = new HttpServer(reqHdl);
		server.start();
	
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
			
		con.write("GET /test HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: test\r\n" +
				  "\r\n"); 
			
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertTrue(header.indexOf("Connection: close") != -1);
		Assert.assertEquals("<html>this ...", con.readStringByLength(14));
		
		try {
			con.readByte();
			Assert.fail("ClosedChannelException expected");
		} catch (ClosedChannelException expected) { }
		
		
		oldServer.close();
		server.close();
	}


	
	@Test
	public void testHttp_1_0_Response() throws Exception {
		
		IServer server = new HttpServer(new ResponseServerHandler());
		ConnectionUtils.start(server);
	
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
			
		con.write("GET /test?chunked=false HTTP/1.0\r\n" +
				  "\r\n"); 
			
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertTrue(header.indexOf("HTTP/1.0") != -1);
		Assert.assertTrue(header.indexOf("Connection: close") != -1);
		con.close();
	
		server.close();
	}

	
	@Test
	public void testHttp_1_0_ResponseKeepAlive() throws Exception {
		
		IServer server = new HttpServer(new ResponseServerHandler());
		ConnectionUtils.start(server);
	
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
			
		con.write("GET /test?chunked=false HTTP/1.0\r\n" +
				  "Connection: keep-alive\r\n" +
				  "\r\n"); 
			
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertTrue(header.indexOf("HTTP/1.0") != -1);
		Assert.assertTrue(header.indexOf("Connection: close") != -1);
		
        con.readByteBufferByDelimiter("\r\n");

		con.close();
	
		server.close();
	}
	
	
	@Test
	public void testHttp_1_0_ResponseChunked() throws Exception {
		
		IServer server = new HttpServer(new ResponseServerHandler());
		server.start();
	
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
			
		con.write("GET /test?chunked=true HTTP/1.0\r\n" +
				  "\r\n"); 
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertTrue(header.indexOf("HTTP/1.0") != -1);
		Assert.assertTrue(header.indexOf("Connection: close") != -1);
		
        con.readByteBufferByDelimiter("\r\n");
		
		con.close();
		server.close();
	}
	
	
	

    @Test
    public void testHttp_1_0_ResponseChunkedBulk() throws Exception {
        
        IServer server = new HttpServer(new ResponseServerHandler());
        server.start();
    
        
        for (int i = 0; i < 1000; i++) {
            IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
                
            con.write("GET /test?chunked=true HTTP/1.0\r\n" +
                      "\r\n"); 
                
            String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
                
            Assert.assertTrue(header.indexOf("200") != -1);
            Assert.assertTrue(header.indexOf("HTTP/1.0") != -1);
            Assert.assertTrue(header.indexOf("Connection: close") != -1);
            
            con.readByteBufferByDelimiter("\r\n");
            
            con.close();
        }
        
        
        server.close();
    }

	
	
	@Test
    public void testHttp_1_0_Proxy() throws Exception {
	    
	    IDataHandler dh = new IDataHandler() {
	        
	        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
	            connection.readStringByDelimiter("\r\n\r\n");
	            
	            connection.write("HTTP/1.0 200 OK\r\n" + 
	                             "Content-Length: 8\r\n" + 
	                             "Content-Type: text/html\r\n" + 
	                             "Date: Fri, 10 Apr 2009 09:09:59 GMT\r\n" + 
	                             "Last-Modified: Wed, 14 May 2008 15:58:25 GMT\r\n" + 
	                             "Mime-Version: 1.0\r\n" + 
	                             "\r\n" + 
	                             "12345678");	            
	            return true;
	        }
	    };
	    
	    final IServer server = new Server(dh);
	    server.start();
	    
	    IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
	        
	        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	            
	            IHttpRequest request = exchange.getRequest();
	            URL url = request.getRequestUrl();
	            URL newURL = new URL(url.getProtocol(), "localhost", server.getLocalPort(), url.getFile()); 
	            request.setRequestUrl(newURL);
	            
	            HttpClient httpClient = new HttpClient();
	            IHttpResponse response = httpClient.call(request);
	            exchange.send(response);
	            
	            
	        }
	    };
	    HttpServer proxy = new HttpServer(reqHdl);
	    proxy.start();
	    
	    HttpClient httpClient = new HttpClient();
	    IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + proxy.getLocalPort() + "/"));
	    Assert.assertEquals(200, response.getStatus());
	    Assert.assertEquals("12345678", response.getBody().readString());
	    
	    httpClient.close();
	    proxy.close();
	    server.close();
	}
    

	
	
	
	private static final class ResponseServerHandler implements IHttpRequestHandler {

		
		 public void onRequest(IHttpExchange exchange) throws IOException {

			 boolean chunkedTest = exchange.getRequest().getBooleanParameter("chunked");
			 
			 if (chunkedTest) {
			     HttpResponseHeader header = new HttpResponseHeader(200, "text/plain");
			     header.setHeader("connection", "close");
				 BodyDataSink bodyDataSink = exchange.send(header);
				 bodyDataSink.write("test123");
				 bodyDataSink.write("456\r\n");
				 bodyDataSink.close();
				 
			 } else {
				 BodyDataSink bodyDataSink = exchange.send(new HttpResponseHeader(200, "text/plain"), 10);
				 bodyDataSink.write("test123\r\n");
				 bodyDataSink.write("456");
				 bodyDataSink.close();
			 }
		}
	}
}
