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
import java.net.SocketTimeoutException;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicReference;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xsocket.Execution;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;



/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientResponseThreadingTest  {



	@Test
	public void testMultithreaded() throws Exception {

	    IDataHandler dh = new IDataHandler() {
	        
	        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
	            connection.readStringByDelimiter("\r\n\r\n");
	            connection.write("HTTP/1.1 200 OK\r\n" + 
	                             "Content-Length: 5\r\n" +
	                             "\r\n" +
	                             "12345");
	            return false;
	        }
	    };
	    IServer server = new Server(dh);
	    server.start();
	    
	    
		HttpClient httpClient = new HttpClient();
		
		ResponseHandler rh = new ResponseHandler();
		httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/"), rh);
		
		QAUtil.sleep(1000);
		
		Assert.assertTrue(rh.threadnameRef.get().indexOf("ool-") != -1);
		Assert.assertNotNull(rh.responseRef.get());
		Assert.assertNull(rh.exceptionRef.get());
		
		
		httpClient.close();
	}
	
	
	
	@Test
    public void testMultithreadedException() throws Exception {
        
        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                connection.readStringByDelimiter("\r\n\r\n");
                connection.write("HTTP/1.1 200 OK\r\n" + 
                                 "Conte");
                connection.close();
                return false;
            }
        };
        IServer server = new Server(dh);
        server.start();
        
        
        HttpClient httpClient = new HttpClient();
        
        ResponseHandler rh = new ResponseHandler();
        httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/"), rh);
        
        QAUtil.sleep(1000);
        
        Assert.assertTrue(rh.threadnameRef.get().indexOf("Pool-") != -1);
        Assert.assertNull(rh.responseRef.get());
        Assert.assertNotNull(rh.exceptionRef.get());
        
        httpClient.close();
    }
    
	
	@Test
    public void testMultithreadedTimoutException() throws Exception {
        
        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                // do nothing
                return false;
            }
        };
        IServer server = new Server(dh);
        server.start();
        
        
        HttpClient httpClient = new HttpClient();
        httpClient.setResponseTimeoutMillis(500);
        
        ResponseHandler rh = new ResponseHandler();
        httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/"), rh);
        
        QAUtil.sleep(1500);
        
        Assert.assertTrue(rh.threadnameRef.get().indexOf("ool-") != -1);
        Assert.assertNull(rh.responseRef.get());
        Assert.assertNotNull(rh.timeoutExceptionRef.get());
        
        httpClient.close();
    }
    
	

    @Test
    public void testNonthreaded() throws Exception {

        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                connection.readStringByDelimiter("\r\n\r\n");
                connection.write("HTTP/1.1 200 OK\r\n" + 
                                 "Content-Length: 5\r\n" +
                                 "\r\n" +
                                 "12345");
                return false;
            }
        };
        IServer server = new Server(dh);
        server.start();
        
        
        HttpClient httpClient = new HttpClient();
        
        NonThreadedResponseHandler rh = new NonThreadedResponseHandler();
        httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/"), rh);
        
        QAUtil.sleep(1000);
        
        Assert.assertTrue(rh.threadnameRef.get().startsWith("xDispatcher"));
        Assert.assertNotNull(rh.responseRef.get());
        Assert.assertNull(rh.exceptionRef.get());
        
        httpClient.close();
    }	
	
	
    
    @Test
    public void testNonthreadedException() throws Exception {
        
        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                connection.readStringByDelimiter("\r\n\r\n");
                connection.write("HTTP/1.1 200 OK\r\n" + 
                                 "Conte");
                connection.close();
                return false;
            }
        };
        IServer server = new Server(dh);
        server.start();
        
        
        HttpClient httpClient = new HttpClient();
        
        NonThreadedResponseHandler rh = new NonThreadedResponseHandler();
        httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/"), rh);
        
        QAUtil.sleep(1000);
        
        Assert.assertTrue(rh.threadnameRef.get().startsWith("xDispatcher"));
        Assert.assertNull(rh.responseRef.get());
        Assert.assertNotNull(rh.exceptionRef.get());
        
        httpClient.close();
    }    
    
    
    
    
    @Test
    public void testNonthreadedTimoutException() throws Exception {
        
        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                // do nothing
                return false;
            }
        };
        IServer server = new Server(dh);
        server.start();
        
        
        HttpClient httpClient = new HttpClient();
        httpClient.setResponseTimeoutMillis(500);
        
        NonThreadedResponseHandler rh = new NonThreadedResponseHandler();
        httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/"), rh);
        
        QAUtil.sleep(1500);
        
        Assert.assertTrue(rh.threadnameRef.get().startsWith("xHttpTimer"));
        Assert.assertNull(rh.responseRef.get());
        Assert.assertNotNull(rh.timeoutExceptionRef.get());
        
        httpClient.close();
    }
    
    
	
	private static class ResponseHandler implements IHttpResponseHandler, IHttpSocketTimeoutHandler {
	    
	    
	    final AtomicReference<String> threadnameRef = new AtomicReference<String>();
	    final AtomicReference<IHttpResponse> responseRef = new AtomicReference<IHttpResponse>();
	    final AtomicReference<IOException> exceptionRef = new AtomicReference<IOException>();
	    final AtomicReference<SocketTimeoutException> timeoutExceptionRef = new AtomicReference<SocketTimeoutException>();
	    
	    
	    public void onResponse(IHttpResponse response) throws IOException {
	        threadnameRef.set(Thread.currentThread().getName());
	        responseRef.set(response);
	    }
	    
	    
	    public void onException(IOException ioe) throws IOException {
            threadnameRef.set(Thread.currentThread().getName());
            exceptionRef.set(ioe);
	    }
	    
	    public void onException(SocketTimeoutException stoe) {
	        threadnameRef.set(Thread.currentThread().getName());
            timeoutExceptionRef.set(stoe);	        
	    }
	}
	
	
	@Execution(Execution.NONTHREADED)
	private static class NonThreadedResponseHandler extends ResponseHandler {
	    
	}
}