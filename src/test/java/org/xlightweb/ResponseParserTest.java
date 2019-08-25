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
import java.nio.channels.ClosedChannelException;

import junit.framework.Assert;

import org.junit.Test;
import org.xlightweb.GetRequest;
import org.xlightweb.IHttpResponse;
import org.xlightweb.client.HttpClientConnection;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;


/**
*
* @author grro@xlightweb.org
*/
public final class ResponseParserTest {


	@Test
	public void testGodHttp11Response() throws Exception {

		IDataHandler hdl = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
				connection.readStringByDelimiter("\r\n\r\n");
				
				connection.write("HTTP/1.1 200 OK\r\n" +
						         "Server: me\r\n" +
						         "Content-Type: text/plain\r\n" +
						         "Content-Length: 5\r\n" +
						         "\r\n" + 
						         "12345");
				
				return true;
			}
		};
		
		IServer server = new Server(hdl);
		server.start();
		
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		IHttpResponse response = con.call(new GetRequest("/"));
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("12345", response.getBody().readString());
		Assert.assertEquals("me", response.getServer());
		
		con.close();
		server.close();
	}
	
	
	@Test
	public void testMissingHeaderValue() throws Exception {

		IDataHandler hdl = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
				connection.readStringByDelimiter("\r\n\r\n");
				
				connection.write("HTTP/1.1 200 OK\r\n" +
						         "ETag:\r\n" +
						         "Server: me\r\n" +
						         "Content-Type: text/plain\r\n" +
						         "Content-Length: 5\r\n" +
						         "\r\n" + 
						         "12345");
				
				return true;
			}
		};
		
		IServer server = new Server(hdl);
		server.start();
		
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		IHttpResponse response = con.call(new GetRequest("/"));
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("12345", response.getBody().readString());
		Assert.assertEquals("me", response.getServer());
		Assert.assertEquals("", response.getHeader("ETag"));
		
		con.close();
		server.close();
	}
	
	
	@Test
	public void testTwoLines() throws Exception {

		IDataHandler hdl = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
				connection.readStringByDelimiter("\r\n\r\n");
				
				connection.write("HTTP/1.1 200 OK\r\n" +
						         "Server: its\r\n" +
						         " me\r\n" +
						         "Content-Type: text/plain\r\n" +
						         "Content-Length: 5\r\n" +
						         "\r\n" + 
						         "12345");
				
				return true;
			}
		};
		
		IServer server = new Server(hdl);
		server.start();
		
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		IHttpResponse response = con.call(new GetRequest("/"));
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("12345", response.getBody().readString());
		Assert.assertEquals("its me", response.getServer());
		
		con.close();
		server.close();
	}
	
	
	

	@Test
	public void testTwoLines2() throws Exception {

		IDataHandler hdl = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
				connection.readStringByDelimiter("\r\n\r\n");
				
				connection.write("HTTP/1.1 200 OK\r\n" +
						         "Server: \r\n" +
						         " its me\r\n" +
						         "Content-Type: text/plain\r\n" +
						         "Content-Length: 5\r\n" +
						         "\r\n" + 
						         "12345");
				
				return true;
			}
		};
		
		IServer server = new Server(hdl);
		server.start();
		
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		IHttpResponse response = con.call(new GetRequest("/"));
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("12345", response.getBody().readString());
		Assert.assertEquals("its me", response.getServer());
		
		con.close();
		server.close();
	}


	@Test
	public void testMissingBlank() throws Exception {

		IDataHandler hdl = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
				connection.readStringByDelimiter("\r\n\r\n");
				
				connection.write("HTTP/1.1 302 Moved Temporarily\r\n" + 
						         "Location:http://www.myserver.com/\r\n" +
						         "Content-Length: 0\r\n" + 
						         "\r\n");
				
				return true;
			}
		};
		
		IServer server = new Server(hdl);
		server.start();
		
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		IHttpResponse response = con.call(new GetRequest("/"));
		
		Assert.assertEquals(302, response.getStatus());
		Assert.assertEquals("http://www.myserver.com/", response.getHeader("location"));
		
		con.close();
		server.close();
	}
	
	
	@Test
	public void testGodHttp11ChunkedResponseWithBlank() throws Exception {

		IDataHandler hdl = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
				connection.readStringByDelimiter("\r\n\r\n");
				
				connection.write("HTTP/1.1 200 OK\r\n" +
						         "Server: me\r\n" +
						         "Transfer-Encoding: chunked\r\n" +
						         "\r\n" + 
						         "5 \r\n" + 
						         "12345\r\n" +
						         "0  \r\n\r\n");
				
				return true;
			}
		};
		
		IServer server = new Server(hdl);
		server.start();
		
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		IHttpResponse response = con.call(new GetRequest("/"));
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("12345", response.getBody().readString());
		
		con.close();
		server.close();
	}
	
	
	@Test
    public void testGodHttp11ChunkedResponse() throws Exception {

        IDataHandler hdl = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                connection.readStringByDelimiter("\r\n\r\n");
                
                connection.write("HTTP/1.1 200 OK\r\n" +
                                 "Server: me\r\n" +
                                 "Transfer-Encoding: chunked\r\n" +
                                 "\r\n" + 
                                 "5\r\n" + 
                                 "12345\r\n" +
                                 "0\r\n\r\n");
                
                return true;
            }
        };
        
        IServer server = new Server(hdl);
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        IHttpResponse response = con.call(new GetRequest("/"));
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345", response.getBody().readString());
        
        con.close();
        server.close();
    }	
	
	@Test
	public void testInvalidResponse() throws Exception {

		IDataHandler hdl = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
				connection.readStringByDelimiter("\r\n\r\n");
				
				connection.write("HTTP/1.1 200 OK\r\n" +
						         "Server: me\r\n" +
						         "Transfer-Encoding: chunked\r\n" +
						         "\r\n" + 
						         "uz\r\n" + 
						         "12345\r\n" +
						         "0\r\n\r\n");
				
				return true;
			}
		};
		
		IServer server = new Server(hdl);
		server.start();
		
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		try {
			IHttpResponse response = con.call(new GetRequest("/"));
			response.getBody().readString();
			Assert.fail("IOException expected");
		} catch (IOException expected) {  }
		
		con.close();
		server.close();
	}
	
	

	@Test
	public void testAmbiguousResponse() throws Exception {

		IDataHandler hdl = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
				connection.readStringByDelimiter("\r\n\r\n");
				
				connection.write("HTTP/1.1 200 OK\r\n" +
						         "Server: me\r\n" +
						         "Connection: close\r\n" +
						         "\r\n" + 
						         "12345");
				connection.close();
				
				return true;
			}
		};
		
		IServer server = new Server(hdl);
		server.start();
		
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		IHttpResponse response = con.call(new GetRequest("/"));
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("12345", response.getBody().readString());
		
		con.close();
		server.close();
	}
	
	
	@Test
	public void testHttp11ResponseWithLeadingCRLFAndSpace() throws Exception {

		IDataHandler hdl = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
				connection.readStringByDelimiter("\r\n\r\n");
				
				connection.write("\r\n" +
						         "HTTP/1.1 200 OK\r\n" +
						         "Server: me\r\n" +
						         "Content-Type: text/plain\r\n" +
						         "Content-Length: 5\r\n" +
						         "\r\n" + 
						         "12345");
				
				return true;
			}
		};
		
		IServer server = new Server(hdl);
		server.start();
		
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		IHttpResponse response = con.call(new GetRequest("/"));
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("12345", response.getBody().readString());
		
		con.close();
		server.close();
	}
	
	
	
	@Test
	public void testHttp11ResponseWithLeadingCRLF() throws Exception {

		IDataHandler hdl = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
				connection.readStringByDelimiter("\r\n\r\n");
				
				connection.write("\r\n" +
						         "HTTP/1.1 200 OK\r\n" +
						         "Server: me\r\n" +
						         "Content-Type: text/plain\r\n" +
						         "Content-Length: 5\r\n" +
						         "\r\n" + 
						         "12345");
				
				return true;
			}
		};
		
		IServer server = new Server(hdl);
		server.start();
		
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		IHttpResponse response = con.call(new GetRequest("/"));
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("12345", response.getBody().readString());
		
		con.close();
		server.close();
	}
	
	@Test
	public void testHttp11ResponseWithServeralLeadingCRLF() throws Exception {

		IDataHandler hdl = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
				connection.readStringByDelimiter("\r\n\r\n");
				
				connection.write("  \r\n" +
						         "HTTP/1.1 200 OK\r\n" +
						         "Server: me\r\n" +
						         "Content-Type: text/plain\r\n" +
						         "Content-Length: 5\r\n" +
						         "\r\n" + 
						         "12345");
				
				return true;
			}
		};
		
		IServer server = new Server(hdl);
		server.start();
		
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		IHttpResponse response = con.call(new GetRequest("/"));
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("12345", response.getBody().readString());
		
		con.close();
		server.close();
	}
	
	
	
	@Test
	public void testSimpleResponse() throws Exception {

		IDataHandler hdl = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
				connection.readStringByDelimiter("\r\n\r\n");
				
				connection.write("<html> <body>this is a plain body </body></html>");
				connection.close();
				
				return true;
			}
		};
		
		IServer server = new Server(hdl);
		server.start();

		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		IHttpResponse response = con.call(new GetRequest("/"));
		
		String body = response.getBody().readString();
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertTrue(response.getContentType() == null);
		Assert.assertEquals("<html> <body>this is a plain body </body></html>", body);
		
		con.close();
		server.close();
	}
	
	
	@Test
    public void testChunkedSingleChars() throws Exception {
	    
	    IDataHandler hdl = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                connection.readStringByDelimiter("\r\n\r\n");
                
                connection.write("HTTP/1.1 200 OK\r\n" +
                                 "Server: me\r\n" +
                                 "Content-Type: text/plain; charset=UTF-8\r\n" +
                                 "Transfer-Encoding: chunked\r\n" +
                                 "\r\n");
                
                for (int i = 0; i < 2; i++) {
                    connection.write("3");
                    QAUtil.sleep(100);
                    
                    connection.write("\r");
                    QAUtil.sleep(100);
                    
                    connection.write("\n");
                    QAUtil.sleep(100);
                    
                    connection.write("1");
                    QAUtil.sleep(100);
                    
                    connection.write("2");
                    QAUtil.sleep(100);
                    
                    connection.write("3");
                    QAUtil.sleep(100);
                    
                    connection.write("\r");
                    QAUtil.sleep(100);
                    
                    connection.write("\n");
                    QAUtil.sleep(100);
                }

                connection.write("0");
                QAUtil.sleep(100);

                connection.write("\r");
                QAUtil.sleep(100);

                connection.write("\n");
                QAUtil.sleep(100);

                connection.write("\r");
                QAUtil.sleep(100);

                connection.write("\n");

                connection.close();
                
                return true;
            }
        };
        
        IServer server = new Server(hdl);
        server.start();

        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        IHttpResponse response = con.call(new GetRequest("/"));
        
        String body = response.getBody().readString();
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("123123", body);
        
        con.close();
        server.close();
    }	

	
    @Test
    public void testContentTypeEncoding() throws Exception {

        IDataHandler hdl = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                connection.readStringByDelimiter("\r\n\r\n");
                
                connection.write("HTTP/1.1 200 OK\r\n" +
                                 "Server: me\r\n" +
                                 "Content-Type: text/plain;charset=iso-8859-1\r\n" +
                                 "Content-Length: 5\r\n" +
                                 "\r\n" + 
                                 "12345");
                
                return true;
            }
        };
        
        IServer server = new Server(hdl);
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        IHttpResponse response = con.call(new GetRequest("/"));
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345", response.getBody().readString());
        Assert.assertEquals("me", response.getServer());
        
        con.close();
        server.close();
    }
    
    
    @Test
    public void testContentTypeQuotedEncoding() throws Exception {

        IDataHandler hdl = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                connection.readStringByDelimiter("\r\n\r\n");
                
                connection.write("HTTP/1.1 200 OK\r\n" +
                                 "Server: me\r\n" +
                                 "Content-Type: text/plain;charset=\"iso-8859-1\"\r\n" +
                                 "Content-Length: 5\r\n" +
                                 "\r\n" + 
                                 "12345");
                
                return true;
            }
        };
        
        IServer server = new Server(hdl);
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        IHttpResponse response = con.call(new GetRequest("/"));
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345", response.getBody().readString());
        Assert.assertEquals("me", response.getServer());
        
        con.close();
        server.close();
    }    
}
