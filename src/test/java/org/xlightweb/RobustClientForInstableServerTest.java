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



import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;

import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;






/**
*
* @author grro@xlightweb.org
*/
public final class RobustClientForInstableServerTest  {



	@Test
	public void testHealthyPersistent() throws Exception {
	    
	    IDataHandler dh = new IDataHandler() {
	        
	        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
	        
	            connection.readStringByDelimiter("\r\n\r\n");
	            
	            connection.write("HTTP/1.1 200 OK\r\n" +
	                             "Server: myServer\r\n" + 
	                             "Content-Type: text/plain; charset=UTF-8\r\n" +
	                             "Content-Length: 30\r\n" +
	                             "\r\n" + 
	                             "123456789012345678901234567890"); 
	            return true;
	        }
	    };
	    
	    IServer server = new Server(dh);
	    server.start();
	    
	    HttpClient httpClient = new HttpClient();
	            
        for (int i = 0; i < 100; i++) {
            try {
                IHttpResponse resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        
                InputStream in = Channels.newInputStream(resp.getBody());
                in.close();
                
            } catch (IOException ioe) {
                System.out.println("Failure " + ioe.toString());
            }
        } 
	}


	@Test
    public void testHealthyNonPersistent() throws Exception {
        
        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
            
                connection.readStringByDelimiter("\r\n\r\n");
                
                connection.write("HTTP/1.1 200 OK\r\n" +
                                 "Server: myServer\r\n" + 
                                 "Content-Type: text/plain; charset=UTF-8\r\n" +
                                 "Connection: close\r\n" +
                                 "Content-Length: 30\r\n" +
                                 "\r\n" + 
                                 "123456789012345678901234567890"); 
                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();
        
        HttpClient httpClient = new HttpClient();
                
        for (int i = 0; i < 100; i++) {
            try {
                IHttpResponse resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        
                InputStream in = Channels.newInputStream(resp.getBody());
                in.close();
                
            } catch (IOException ioe) {
                System.out.println("Failure " + ioe.toString());
            }
        } 
    }

	
    @Test
    public void testInterruptedHeader() throws Exception {
        
        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
            
                connection.readStringByDelimiter("\r\n\r\n");
                
                connection.write("HTTP/1.1 200 OK\r\n" +
                                 "Server: myServer\r\n" + 
                                 "Content-Type: text/p");
                connection.close();
                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();
        
        HttpClient httpClient = new HttpClient();
                
        try {
            httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
            Assert.fail("ProtocolException expected");
        } catch (ProtocolException expected) { }
        
        server.close();
    }



	
  
	
    @Test
    public void testInterruptedBody() throws Exception {
       
       IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
            
                connection.readStringByDelimiter("\r\n\r\n");
                
                connection.write("HTTP/1.1 200 OK\r\n" +
                                 "Server: myServer\r\n" + 
                                 "Content-Type: text/plain; charset=UTF-8\r\n" +
                                 "Content-Length: 30\r\n" +
                                 "\r\n" + 
                                 "12345678901234567890123456");
                connection.close();
                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();
        
        HttpClient httpClient = new HttpClient();
                
        IHttpResponse resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        
        InputStream in = Channels.newInputStream(resp.getBody());
        QAUtil.sleep(500);
        in.close();
               
        
        Assert.assertEquals(0, httpClient.getNumIdle());
        Assert.assertEquals(1, httpClient.getNumDestroyed());
    }
    
    


    @Test
    public void testClientSideCloseWhileReceiving() throws Exception {
        
       IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
            
                connection.readStringByDelimiter("\r\n\r\n");
                
                connection.write("HTTP/1.1 200 OK\r\n" +
                                 "Server: myServer\r\n" + 
                                 "Content-Type: text/plain; charset=UTF-8\r\n" +
                                 "Content-Length: 30\r\n" +
                                 "\r\n");
                
                connection.write("0123456789");
                QAUtil.sleep(300);
                
                connection.write("0123456789");
                QAUtil.sleep(300);
                
                connection.write("0123456789");
                
                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();
        
        HttpClient httpClient = new HttpClient();
                
        IHttpResponse resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        
        InputStream in = Channels.newInputStream(resp.getBody());
        QAUtil.sleep(200);
        in.close();
               
        QAUtil.sleep(200);
        
        Assert.assertEquals(0, httpClient.getNumIdle());
        
        httpClient.close();
        server.close();
    }
}
