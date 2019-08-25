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
import java.io.PrintWriter;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicReference;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClientConnection;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.Server;



/**
*
* @author grro@xlightweb.org
*/
public final class BodyDataSourceTest  {
    
    public static void main(String[] args) throws Exception {
        
        for (int i = 0; i < 10000; i++) {
            new BodyDataSourceTest().testFullMessageInsufficientData();
        }
    }


	@Test
	public void testFullMessage() throws Exception {

	    IDataHandler dh = new IDataHandler() {
	      
	        public boolean onData(INonBlockingConnection con) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
	            con.readStringByDelimiter("\r\n\r\n");

	            con.write("HTTP/1.1 200 OK\r\n" +
	                      "Date: Fri, 20 Mar 2009 12:13:55 GMT\r\n" +
	                      "Content-Length: 5\r\n" +
	                      "\r\n" +
	                      "12345");
	            return true;
	        }
	    };
	    
	    
	    Server server = new Server(dh);
	    server.start();
	    
	    
	    HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
	    
	    IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
	    Assert.assertEquals(200, response.getStatus());
	    Assert.assertEquals("12345", response.getBody().readString());
	    
        response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345", response.getBody().readString());

	    
	    con.close();
	    server.close();
	}



    @Test
    public void testFullMessageCloseDataSource() throws Exception {

        IDataHandler dh = new IDataHandler() {
          
            public boolean onData(INonBlockingConnection con) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                con.readStringByDelimiter("\r\n\r\n");

                con.write("HTTP/1.1 200 OK\r\n" +
                          "Date: Fri, 20 Mar 2009 12:13:55 GMT\r\n" +
                          "Content-Length: 5\r\n" +
                          "\r\n" +
                          "123");
                return true;
            }
        };
        
        
        Server server = new Server(dh);
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        
        QAUtil.sleep(1000);
        NonBlockingBodyDataSource dataSource = response.getNonBlockingBody();
        
        BodyDataHandler dataHandler = new BodyDataHandler();
        dataSource.setDataHandler(dataHandler);
        
        QAUtil.sleep(500);
        Assert.assertNull(dataHandler.exceptionRef.get());
        Assert.assertEquals("123", dataHandler.dataRef.get());
                
        dataSource.close();
        QAUtil.sleep(500);
        Assert.assertFalse(con.isOpen());
        
        QAUtil.sleep(500);
        Assert.assertNotNull(dataHandler.exceptionRef.get());
        
        server.close();
    }

    
    @Test
    public void testFullMessageDestroyDataSource() throws Exception {

        IDataHandler dh = new IDataHandler() {
          
            public boolean onData(INonBlockingConnection con) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                con.readStringByDelimiter("\r\n\r\n");

                con.write("HTTP/1.1 200 OK\r\n" +
                          "Date: Fri, 20 Mar 2009 12:13:55 GMT\r\n" +
                          "Content-Length: 5\r\n" +
                          "\r\n" +
                          "123");
                return true;
            }
        };
        
        
        Server server = new Server(dh);
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        
        QAUtil.sleep(1000);
        NonBlockingBodyDataSource dataSource = response.getNonBlockingBody();
        
        BodyDataHandler dataHandler = new BodyDataHandler();
        dataSource.setDataHandler(dataHandler);
        
        QAUtil.sleep(1000);
        Assert.assertNull(dataHandler.exceptionRef.get());
        Assert.assertEquals("123", dataHandler.dataRef.get());
                
        dataSource.destroy();
        Assert.assertFalse(con.isOpen());
        
        QAUtil.sleep(1000);
        Assert.assertNotNull(dataHandler.exceptionRef.get());
        
        server.close();
    }

    

    @Test
    public void testFullChunkedMessageSingleChunk() throws Exception {

        HttpServlet servlet = new HttpServlet() {
       
            private static final long serialVersionUID = 7839408058146864492L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                PrintWriter pw = resp.getWriter();
                pw.write("12345");
                pw.flush();
                
                pw.close();
            }
        };
        
        WebContainer server = new WebContainer(servlet);
        server.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345", response.getBody().readString());
        
        response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345", response.getBody().readString());

        
        con.close();
        server.stop();
    }

    
    
    @Test
    public void testFullChunkedMessageMultipleChunks() throws Exception {
             
        final StringBuilder sb = new StringBuilder();
        
        HttpServlet servlet = new HttpServlet() {
        
            private static final long serialVersionUID = 8466220692667772651L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                PrintWriter pw = resp.getWriter();
                
                for (int i = 0; i < 100; i++) {
                    String data = new String(QAUtil.generateByteArray(20000 + i), "US-ASCII");
                    sb.append(data);
                    pw.write(data);
                    pw.flush();
                }
                
                pw.close();
            }
        };
        
        WebContainer server = new WebContainer(servlet);
        server.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));

        Assert.assertEquals(200, response.getStatus());
        String body = response.getBody().readString();
        
        Assert.assertEquals(sb.toString().length(), body.length());
        Assert.assertEquals(sb.toString(), body);
        
        con.close();
        server.stop();
    }


    @Test
    public void testFullChunkedMessageServersideClose() throws Exception {

        IDataHandler dh = new IDataHandler() {
          
            public boolean onData(INonBlockingConnection con) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                con.readStringByDelimiter("\r\n\r\n");

                con.write("HTTP/1.1 200 OK\r\n" +
                          "Date: Fri, 20 Mar 2009 12:13:55 GMT\r\n" +
                          "Connection: close\r\n" + 
                          "Transfer-Encoding: chunked\r\n" +
                          "\r\n" +
                          "5\r\n" +
                          "12345\r\n" + 
                          "0\r\n\r\n");
                con.close();
                return true;
            }
        };
        
        
        Server server = new Server(dh);
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345", response.getBody().readString());
        
        
        con.close();
        server.close();
    }
    
    
    
    private static final class BodyDataHandler implements IBodyDataHandler {
        
        private final AtomicReference<String> dataRef = new AtomicReference<String>("");
        private final AtomicReference<IOException> exceptionRef = new AtomicReference<IOException>(null);
        
        public boolean onData(NonBlockingBodyDataSource bodyDataSource) throws BufferUnderflowException {

            try {
                int available = bodyDataSource.available();
                String txt = bodyDataSource.readStringByLength(available);
                dataRef.set(dataRef.get() + txt);
                
            } catch (IOException ioe) {
                exceptionRef.set(ioe);
            }
            return true;
        }
    }

	
	@Test
	public void testFullMessageInsufficientData() throws Exception {
	    
	    System.out.println("testFullMessageInsufficientData");

	    IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection con) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                con.readStringByDelimiter("\r\n\r\n");

                con.write("HTTP/1.1 200 OK\r\n" +
                          "Date: Fri, 20 Mar 2009 12:13:55 GMT\r\n" +
                          "Content-Length: 5\r\n" +
                          "\r\n" +
                          "125");
                con.close();
                return true;
            }
        };
        
        
        Server server = new Server(dh);
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        try {
            IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
            Assert.assertEquals(200, response.getStatus());

            response.getBody().readString();
            Assert.fail("ProtocolException expected");
        } catch (ProtocolException expected) {  }
        
        con.close();
        server.close();
	}
	

	@Test
    public void testHeaderInsufficientData() throws Exception {

        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection con) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                con.readStringByDelimiter("\r\n\r\n");

                con.write("HTTP/1.1 2");
                con.close();
                return true;
            }
        };
        
        
        Server server = new Server(dh);
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        try {
            con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
            Assert.fail("ProtocolException expected");
        } catch (ProtocolException expected) { }
        
        server.close();
    }
	

    @Test
    public void testHeaderInsufficientData2() throws Exception {

        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection con) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                con.readStringByDelimiter("\r\n\r\n");

                con.write("HTTP/1.1 200 OK\r\n" +
                          "Date: Fri, 20 Mar 2009 12:13:55 GMT\r\n" +
                          "Content-Leng");
                con.close();
                return true;
            }
        };
        
        
        Server server = new Server(dh);
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        try {
            con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        } catch (IOException expected) { }
        
        server.close();
    }


    @Test
    public void testHttp_0_9_response() throws Exception {

        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection con) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                con.readStringByDelimiter("\r\n\r\n");

                con.write("<ht");
                con.close();
                return true;
            }
        };
        
        
        Server server = new Server(dh);
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<ht", response.getBody().readString());
        
        server.close();
    }
}