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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.xlightweb.BodyDataSink;
import org.xlightweb.GetRequest;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.IBodyCompleteListener;
import org.xlightweb.IBodyDataHandler;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.InvokeOn;
import org.xlightweb.NonBlockingBodyDataSource;
import org.xlightweb.QAUtil;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.ConnectionUtils;




/**
*
* @author grro@xlightweb.org
*/
public final class DisconnectTest  {

	private AtomicInteger running = new AtomicInteger(0);
	private List<String> errors = new ArrayList<String>();
	
	
	public static void main(String[] args) throws Exception {
        
	    for (int i = 0; i < 1000; i++) {
	        DisconnectTest test = new DisconnectTest();
	        test.setup();
	        
	        test.testServerInitiatedDisconnectHeaderLevel();
	    }
	    
    }
	
	
	
	@Before
	public void setup() {
		running.set(0);
		errors.clear();
	}
	


	@Test
	public void testClientInitiatedDisconnectChunked() throws Exception {
			
		StreamHandler hdl = new StreamHandler();
		IServer server = new HttpServer(hdl);
		ConnectionUtils.start(server);
	
	

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
			
		con.write("POST /test HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: test\r\n" +
				  "Transfer-Encoding: Chunked\r\n" +
				  "\r\n" +
				  "5\r\n" +
				  "123"); 
		
		QAUtil.sleep(200);
		con.close();
		
		QAUtil.sleep(1000);
		
		Assert.assertNotNull(hdl.getException());
		Assert.assertFalse(hdl.isClosed());
		Assert.assertFalse(hdl.isCompleteListenerCalled());

		server.close();
	}

	
	
	@Test
	public void testClientInitiatedDisconnect() throws Exception {
			
		StreamHandler hdl = new StreamHandler();
		IServer server = new HttpServer(hdl);
		ConnectionUtils.start(server);
	
	

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
			
		con.write("POST /test HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: test\r\n" +
				  "Content-Length: 10\r\n" +
				  "\r\n" +
				  "1234567"); 
		
		QAUtil.sleep(100);
		con.close();
		
		QAUtil.sleep(1000);
		
		Assert.assertFalse(hdl.isClosed());
		Assert.assertFalse(hdl.isCompleteListenerCalled());
		Assert.assertNotNull(hdl.getException());

		server.close();
	}


	@Test
	public void testServerGodCase() throws Exception {
			
		ServerHandler hdl = new ServerHandler(false);
		IServer server = new HttpServer(hdl);
		server.start();
	
	

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
			
		con.write("GET /test HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: test\r\n" +
				  "\r\n"); 

	
		
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
			
		con.readStringByLength(contentLength);
		QAUtil.sleep(1000);

		Assert.assertTrue(header.indexOf("200") != -1);
		
		Assert.assertTrue(hdl.getDataWriter().isComplete());
		Assert.assertNull(hdl.getDataWriter().getException());
		
		server.close();
	}

	
	@Test
	public void testServerClose() throws Exception {
			
		ServerHandler hdl = new ServerHandler(false);
		IServer server = new HttpServer(hdl);
		ConnectionUtils.start(server);
	
	

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
			
		con.write("GET /test HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: test\r\n" +
				  "\r\n"); 


		QAUtil.sleep(200);
		server.close();
		
		QAUtil.sleep(1000);
		
		Assert.assertFalse(hdl.getDataWriter().isComplete());
		Assert.assertTrue(hdl.getDataWriter().getException() != null);

		con.close();
		
	}

		
	
	
	@Test
	public void testServerInitiatedDisconnectMessageLevel() throws Exception {
		
		IServer server = new HttpServer(new BadServerHandler());
		server.start();
		
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		ResponseHandler respHdl = new ResponseHandler();
		con.send(new GetRequest("/"), respHdl);
		
		QAUtil.sleep(1000);
		
		Assert.assertNotNull(respHdl.getException());
		
		con.close();
		server.close();
	}
	
	
	@Test
	public void testServerInitiatedDisconnectHeaderLevel() throws Exception {
		
		IServer server = new HttpServer(new BadServerHandler());
		server.start();
		
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		ResponseHandler2 respHdl = new ResponseHandler2();
		con.send(new GetRequest("/"), respHdl);
		
		QAUtil.sleep(1000);
		
		Assert.assertNotNull(respHdl.ioeRef.get());
		Assert.assertTrue(respHdl.ioeRef.get() instanceof ProtocolException);
		
		con.close();
		server.close();
	}
	
	


	private static final class ResponseHandler implements IHttpResponseHandler {
		
		
		private IOException ioe = null;
		
		@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
		public void onResponse(IHttpResponse response) throws IOException {
			System.out.println(response);
		}
		
		public void onException(IOException ioe) {
			this.ioe = ioe;
		}
		
		
		IOException getException() {
			return ioe;
		}
	}

	
	
	private static final class ResponseHandler2 implements IHttpResponseHandler {
		
	
		private final AtomicReference<IOException> ioeRef = new AtomicReference<IOException>();
		
		
		public void onResponse(IHttpResponse response) throws IOException {

			IBodyDataHandler dataHandler = new IBodyDataHandler() {
				
				public boolean onData(NonBlockingBodyDataSource bodyDataSource) throws BufferUnderflowException {
					try {
						int available = bodyDataSource.available();
						bodyDataSource.readByteBufferByLength(available);
					} catch (IOException e) {
						ioeRef.set(e);
					}
					return true;
				}
			};
			
			response.getNonBlockingBody().setDataHandler(dataHandler);

		}
		
		public void onException(IOException ioe) {
		    ioeRef.set(ioe);
            System.out.println("exception " + ioe.toString());
		}
	}
		
	
		
	



	@Execution(Execution.NONTHREADED)
	private static final class StreamHandler implements IHttpRequestHandler {

		private final AtomicReference<Exception> exceptionRef = new AtomicReference<Exception>();
		private final AtomicBoolean isCompleteListnerCalled = new AtomicBoolean(false);
		private final AtomicBoolean isClosed = new AtomicBoolean(false);

		public void onRequest(IHttpExchange exchange) throws IOException {

			IHttpRequest request = exchange.getRequest();
			
			IBodyCompleteListener cl = new IBodyCompleteListener() {
				public void onComplete() throws IOException {
					isCompleteListnerCalled.set(true);
				}
			};
			request.getNonBlockingBody().addCompleteListener(cl);
			
			
			IBodyDataHandler dh = new IBodyDataHandler() {
				
				public boolean onData(NonBlockingBodyDataSource bodyDataSource) throws BufferUnderflowException {

					try {
						int available = bodyDataSource.available();
						if (available == -1) {
							isClosed.set(true);
						} else {
							bodyDataSource.readByteBufferByLength(available);
						}
					} catch (IOException e) { 
						exceptionRef.set(e);
					}
					
					return true;
				}
			};
			request.getNonBlockingBody().setDataHandler(dh);
		}		
		
		
		boolean isClosed() {
			return isClosed.get();
		}
		
		boolean isCompleteListenerCalled() {
			return isCompleteListnerCalled.get();
		}
		
		Exception getException() {
			return exceptionRef.get();
		}
	}
	
	
	
	
	@Execution(Execution.NONTHREADED)
	private static final class ServerHandler implements IHttpRequestHandler {
		
		private DataWriter dataWriter = null;

		private int length = 10000;
		private boolean isChunkedMode = false;
		
		public ServerHandler(boolean isChunkedMode) {
			this.isChunkedMode = isChunkedMode;
		}
		
		public void onRequest(IHttpExchange exchange) throws IOException {
	
			BodyDataSink bodyDataSink = null;
			
			if (isChunkedMode) {
				bodyDataSink = exchange.send(new HttpResponseHeader(200, "text/plain"));
			} else {
				bodyDataSink = exchange.send(new HttpResponseHeader(200, "text/plain"), length);
			}
			
			dataWriter = new DataWriter(bodyDataSink, length / 10, 10);
			new Thread(dataWriter).start();
		}
		
		
		DataWriter getDataWriter() {
			return dataWriter;
		}
	}
	
	
	private static final class DataWriter implements Runnable {
		
		private BodyDataSink bodyDataSink = null;
		private int length = 0;
		private int chunks = 0;
		
		private boolean isComplete = false;
		private Exception e = null;
		
		public DataWriter(BodyDataSink bodyDataSink, int length, int chunks) {
			this.bodyDataSink = bodyDataSink;
			this.length = length;
			this.chunks = chunks;
		}
		
		
		public void run() {
		
			try {
				for (int i = 0; i < chunks; i++) {
					bodyDataSink.write(QAUtil.generateByteArray(length));
					QAUtil.sleep(100);
				}
	
				isComplete = true;
			} catch (Exception e) {
				this.e = e;
			}
		}
		
		
		boolean isComplete() {
			return isComplete;
		}
		
		
		Exception getException() {
			return e;
		}
	}
	
	
	
	
	private static final class BadServerHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			BodyDataSink bodyDataSink = exchange.send(new HttpResponseHeader(200, "text/plain"), 1000);
			byte[] data = QAUtil.generateByteArray(100);
			bodyDataSink.write(data);
			
			bodyDataSink.close();
		}	
	}
}
