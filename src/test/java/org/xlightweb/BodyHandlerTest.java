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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.GetRequest;
import org.xlightweb.IBodyDataHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.NonBlockingBodyDataSource;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
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
public final class BodyHandlerTest  {

    
    public static void main(String[] args) throws Exception {
        
        for (int i = 0; i < 1000; i++) {
            new BodyHandlerTest().testClientReadError();
        }
    }
    

	@Test
	public void testClientMutlithreaded() throws Exception {

		Thread.currentThread().setName("testThread");
		
		HttpServer server = new HttpServer(new HeaderInfoServerHandler());
		server.start();

		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		IHttpResponse response = con.call(new GetRequest("/"));

		MultithreadedBodyHandler bh = new MultithreadedBodyHandler();
		response.getNonBlockingBody().setDataHandler(bh);
		
		while (bh.getCountCalled() <= 0) {
		    QAUtil.sleep(100);
		}
		
		Assert.assertFalse(bh.getThreadname().startsWith("xDispatcher") || bh.getThreadname().startsWith("testThread"));
		

		con.close();
		server.close();
	}

	
	
	@Test
	public void testClientSinglethreaded() throws Exception {
		
		Thread.currentThread().setName("testThread");
		
		HttpServer server = new HttpServer(new HeaderInfoServerHandler());
		server.start();

		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		IHttpResponse response = con.call(new GetRequest("/"));

		NonthreadedBodyHandler bh = new NonthreadedBodyHandler();
		response.getNonBlockingBody().setDataHandler(bh);
		
		while (bh.getCountCalled() <= 0) {
		    QAUtil.sleep(100);
		}
		
		if (!bh.getThreadname().startsWith("testThread")) {
			System.out.println("thread name should be testThread not " + bh.getThreadname());
			Assert.fail();
		}

		con.close();
		server.close();
	}
	
	
	
	
	@Test
	public void testClientReadError() throws Exception {

		IDataHandler errorneousServer = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {

				connection.setAutoflush(false);
				
				// read request
				connection.readStringByDelimiter("\r\n\r\n");
				
				// write response
				connection.write("HTTP/1.1 200 OK\r\n");
				connection.write("Transfer-Encoding: chunked\r\n");
				connection.write("\r\n");
				connection.write("2\r\n");
				connection.write("12\r\n");
				connection.write("P\r\n");  // write invalid char
				connection.flush();
				
				return true;
			}
		};
		
		IServer server = new Server(errorneousServer);
		server.start();

		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		
		try {
			IHttpResponse response = con.call(new GetRequest("/"));
	
			BodyHandler bh = new BodyHandler();
			response.getNonBlockingBody().setDataHandler(bh);
		
			QAUtil.sleep(1000);
		
			bh.getNonBlockingBody().available();  
			Assert.fail("IOException expected");
		} catch (IOException expected) { }
		

		con.close();
		server.close();
	}

	
	private static final class BodyHandler implements IBodyDataHandler {
		
		private final AtomicReference<NonBlockingBodyDataSource> bodyDataSourceRef = new AtomicReference<NonBlockingBodyDataSource>();
		
		public boolean onData(NonBlockingBodyDataSource bodyDataSource) {
			bodyDataSourceRef.set(bodyDataSource);
			return true;
		}
		
		
		public NonBlockingBodyDataSource getNonBlockingBody() {
			return bodyDataSourceRef.get();
		}
	}
	
	
	
	
	private static final class MultithreadedBodyHandler implements IBodyDataHandler {

        private final AtomicInteger countCalledRef = new AtomicInteger(0);
        private final AtomicReference<String> threadNameRef = new AtomicReference<String>(null);
        
		
		public boolean onData(NonBlockingBodyDataSource bodyDataSource) {
			threadNameRef.set(Thread.currentThread().getName());
			countCalledRef.incrementAndGet();
			return true;
		}
		
		
		public int getCountCalled() {
			return countCalledRef.get();
		}
		
		public String getThreadname() {
			return threadNameRef.get(); 
		}
	}

	
	private static final class NonthreadedBodyHandler implements IBodyDataHandler {

		private final AtomicInteger countCalledRef = new AtomicInteger(0);
		private final AtomicReference<String> threadNameRef = new AtomicReference<String>(null);
		
		
		@Execution(Execution.NONTHREADED)
		public boolean onData(NonBlockingBodyDataSource bodyDataSource) {
			threadNameRef.set(Thread.currentThread().getName());
			countCalledRef.incrementAndGet();
			return true;
		}
		
		
		public int getCountCalled() {
			return countCalledRef.get();
		}
		
		public String getThreadname() {
			return threadNameRef.get(); 
		}
	}

 
}