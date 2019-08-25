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
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClientConnection;
import org.xsocket.DataConverter;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IWriteCompletionHandler;
import org.xsocket.connection.Server;
import org.xsocket.connection.IConnection.FlushMode;



/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientConnectionTest  {



    
    public static void main(String[] args) throws Exception {
        
        for (int i = 0; i < 1000000; i++) {
            new HttpClientConnectionTest().testStreamingCompletionHandler();
        }
        
    }
    


	@Test
	public void testPersistent() throws Exception {
	    System.out.println("testPersistent");
	    

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
    public void testNonPersistent() throws Exception {
        System.out.println("testNonPersistent");
        
        IDataHandler dh = new IDataHandler() {
          
            public boolean onData(INonBlockingConnection con) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                con.readStringByDelimiter("\r\n\r\n");

                con.write("HTTP/1.1 200 OK\r\n" +
                          "Date: Fri, 20 Mar 2009 12:13:55 GMT\r\n" +
                          "Content-Length: 5\r\n" +
                          "Connection: close\r\n" +
                          "\r\n" +
                          "12345");
                return true;
            }
        };
        
        
        Server server = new Server(dh);
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        System.out.println("first call");
        IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("12345", response.getBody().readString());
        
        System.out.println("second call");
        try {
            response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
            Assert.fail("ClosedChannelException expected");
        } catch (ClosedChannelException expected) { }      
        
        
        server.close();
    }
    
    

    @Test
    public void testServerSideTermination() throws Exception {
        System.out.println("testServerSideTermination");

       IDataHandler dh = new IDataHandler() {
          
            public boolean onData(INonBlockingConnection con) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                con.readStringByDelimiter("\r\n\r\n");

                con.write("HTTP/1.1 200 OK\r\n" +
                          "Date: Fri, 20 Mar 2009 12:13:55 GMT\r\n" +
                          "Content-Length: 5\r\n" +
                          "\r\n" +
                          "12");
                con.close();
                return true;
            }
        };
        
        
        Server server = new Server(dh);
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        try {
            response.getBody().readString();
            Assert.fail("ProtocolException expected");
        } catch (ProtocolException expected) { }

        
        con.close();
        server.close();
    }

	

    @Test
    public void testOnHeaderReceived() throws Exception {
        System.out.println("testOnHeaderReceived");

      
        
        IDataHandler dh = new IDataHandler() {
          
            public boolean onData(INonBlockingConnection con) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                con.readStringByDelimiter("\r\n\r\n");

                con.write("HTTP/1.1 200 OK\r\n" +
                          "Date: Fri, 20 Mar 2009 12:13:55 GMT\r\n" +
                          "Content-Length: 5\r\n" +
                          "\r\n");
                
                QAUtil.sleep(2000);
                con.write("12345");
                return true;
            }
        };
        
        
        Server server = new Server(dh);
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());

        OnHeaderReceivedResponseHandler respHdl = new OnHeaderReceivedResponseHandler();
        con.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/"), respHdl);
        
        QAUtil.sleep(1000);
        Assert.assertEquals(200, respHdl.responseRef.get().getStatus());
        Assert.assertEquals(0, respHdl.responseRef.get().getNonBlockingBody().available());
        
        QAUtil.sleep(3000);
        Assert.assertEquals(5, respHdl.responseRef.get().getNonBlockingBody().available());
        
        con.close();
        server.close();
    }    
    
    
    


    @Test
    public void testOnMessageReceived() throws Exception {
        System.out.println("testOnMessageReceived");
     
        
        IDataHandler dh = new IDataHandler() {
          
            public boolean onData(INonBlockingConnection con) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                con.readStringByDelimiter("\r\n\r\n");

                con.write("HTTP/1.1 200 OK\r\n" +
                          "Date: Fri, 20 Mar 2009 12:13:55 GMT\r\n" +
                          "Content-Length: 5\r\n" +
                          "\r\n");
                
                QAUtil.sleep(2000);
                con.write("12345");
                return true;
            }
        };
        
        
        Server server = new Server(dh);
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());

        OnMessageReceivedResponseHandler respHdl = new OnMessageReceivedResponseHandler();
        con.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/"), respHdl);
        
        QAUtil.sleep(1000);
        Assert.assertNull(respHdl.responseRef.get());
        
        QAUtil.sleep(3000);
        Assert.assertEquals(200, respHdl.responseRef.get().getStatus());
        Assert.assertEquals(5, respHdl.responseRef.get().getNonBlockingBody().available());
        
        con.close();
        server.close();
    }   
    

    @Test
    public void testStreaming() throws Exception {
        System.out.println("testStreaming");

        WebContainer container = new WebContainer(new MyServlet());
        container.start();
             
        HttpClientConnection con = new HttpClientConnection("localhost", container.getLocalPort());
        con.setBodyDataReceiveTimeoutMillis(5000);
        
        
        for (int i = 0; i < 40; i++) {
            FutureResponseHandler respHdl = new FutureResponseHandler();
            BodyDataSink dataSink = con.send(new HttpRequestHeader("POST", "http://localhost:" + container.getLocalPort() + "/test"), respHdl);
            
            dataSink.write("test");
            
            IHttpResponse response = respHdl.getResponse();
            Assert.assertEquals(200, response.getStatus());
            
            BodyDataSource dataSource = response.getBody();
            Assert.assertEquals("test", dataSource.readStringByLength(4));
            
            dataSink.write("12345");
            Assert.assertEquals("12345", dataSource.readStringByLength(5));
            
            dataSink.write("789");
            dataSink.close();
            Assert.assertEquals("789", dataSource.readString());
        }
        
        con.close();
        container.stop();
    }    
    
    

    @Test
    public void testStreamingNonPersistent() throws Exception {
        System.out.println("testStreamingNonPersistent");

        WebContainer container = new WebContainer(new MyServlet());
        container.start();
             
        HttpClientConnection con = new HttpClientConnection("localhost", container.getLocalPort());

        FutureResponseHandler respHdl = new FutureResponseHandler();
        HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + container.getLocalPort() + "/test");
        header.setHeader("connection", "close");
        BodyDataSink dataSink = con.send(header, respHdl);
        
        dataSink.write("test");
        
        IHttpResponse response = respHdl.getResponse();
        Assert.assertEquals(200, response.getStatus());
        
        BodyDataSource dataSource = response.getBody();
        Assert.assertEquals("test", dataSource.readStringByLength(4));
        
        dataSink.write("12345");
        Assert.assertEquals("12345", dataSource.readStringByLength(5));
        
        dataSink.write("789");
        dataSink.close();
        Assert.assertEquals("789", dataSource.readString());        

                
        con.close();
        container.stop();
    }    
    
    @Test
    public void testStreamingCompletionHandler() throws Exception {
        System.out.println("testStreamingCompletionHandler");
        
        WebContainer container = new WebContainer(new MyServlet());
        container.start();
             
        HttpClientConnection con = new HttpClientConnection("localhost", container.getLocalPort());

        
        for (int i = 0; i < 20; i++) {
            final FutureResponseHandler respHdl = new FutureResponseHandler();
            final BodyDataSink dataSink = con.send(new HttpRequestHeader("POST", "http://localhost:" + container.getLocalPort() + "/test"), respHdl);
            dataSink.setFlushmode(FlushMode.ASYNC);
            
            
            IWriteCompletionHandler completionHandler1 = new IWriteCompletionHandler() {
                
                public void onWritten(int written) throws IOException {
                    try {
                        IHttpResponse response = respHdl.getResponse();
                        Assert.assertEquals(200, response.getStatus());
                        
                        final BodyDataSource dataSource = response.getBody();
                        Assert.assertEquals("test", dataSource.readStringByLength(4));
                        
    
                        IWriteCompletionHandler completionHandler2 = new IWriteCompletionHandler() {
                            
                            public void onWritten(int written) throws IOException {
                                Assert.assertEquals("12345", dataSource.readStringByLength(5));

                                IWriteCompletionHandler completionHandler3 = new IWriteCompletionHandler() {
                                    
                                    public void onWritten(int written) throws IOException {
                                        Assert.assertEquals("789", dataSource.readStringByLength(3));
                                        dataSink.close();
                                    }
                                    
                                    public void onException(IOException ioe) {
                                        ioe.printStackTrace();
                                    }
                                };
                                
                                // writing 3. junk and close
                                dataSink.write(new ByteBuffer[] { DataConverter.toByteBuffer("789", "US-ASCII") }, completionHandler3);
                            }
                            
                            public void onException(IOException ioe) {
                                ioe.printStackTrace();
                            }
                        };
                        
                        
                        // writing 2. junk
                        dataSink.write(new ByteBuffer[] { DataConverter.toByteBuffer("12345", "US-ASCII") }, completionHandler2);    
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                public void onException(IOException ioe) {
                    ioe.printStackTrace();
                }
            };
            
            // writing 1. junk
            dataSink.write(new ByteBuffer[] { DataConverter.toByteBuffer("test", "US-ASCII") }, completionHandler1);
            
                        
            IBodyCloseListener closeListener = new IBodyCloseListener() {
                public void onClose() throws IOException {
                    synchronized (dataSink) {
                        dataSink.notify();
                    }
                }
            };
            
            dataSink.addCloseListener(closeListener);
            
            try {
               synchronized (dataSink) {
                   if (dataSink.isOpen()) {
                       dataSink.wait();
                   }
               } 
            } catch (InterruptedException ignore) {  }
            
            System.out.print(".");
       }
        
        System.out.print("finished");
        con.close();
        container.stop();
    }    
    
    
    
    

    private static final class MyServlet extends HttpServlet {

        private static final long serialVersionUID = -6112517976734846433L;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            InputStream is = req.getInputStream();
            OutputStream os = resp.getOutputStream();
                        
            byte[] data = new byte[4096];
            int read = 0;
            do {
                read = is.read(data);
                if (read > 0) {
                    os.write(data, 0, read);
                    os.flush();
                }
            } while (read >= 0);
            
            is.close();
            os.close();
        }        
    }
    
    
    
    private static class ResponseHandler implements IHttpResponseHandler {

        AtomicReference<IHttpResponse> responseRef = new AtomicReference<IHttpResponse>();
        AtomicReference<IOException> exceptionRef = new AtomicReference<IOException>();
        
        public void onResponse(IHttpResponse response) throws IOException {
            responseRef.set(response);
        }
        
        public void onException(IOException ioe) throws IOException {
            exceptionRef.set(ioe);
        }
    }
    
    
    @InvokeOn(InvokeOn.MESSAGE_RECEIVED)
    private static final class OnMessageReceivedResponseHandler extends ResponseHandler {
        
    }
    
    @InvokeOn(InvokeOn.HEADER_RECEIVED)
    private static final class OnHeaderReceivedResponseHandler extends ResponseHandler {
        
    }
}