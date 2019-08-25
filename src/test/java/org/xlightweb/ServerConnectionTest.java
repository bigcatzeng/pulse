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
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.BodyDataSink;
import org.xlightweb.HttpResponse;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.InvokeOn;
import org.xlightweb.server.HttpServer;
import org.xsocket.DataConverter;
import org.xsocket.Execution;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IConnection.FlushMode;





/**
*
* @author grro@xlightweb.org
*/
public final class ServerConnectionTest  {

	
	private AtomicInteger running = new AtomicInteger(0);
	private List<String> errors = new ArrayList<String>();
	
	

	
	public static void main(String[] args) throws IOException {
		IServer server = new HttpServer(8877, new EchoHandler());
		server.run();
	}
	
	
	
	@Test
	public void testSimple() throws Exception {
		
		System.out.println("testSimple");
		
		IServer server = new HttpServer(new RequestHandler());
		ConnectionUtils.start(server);
	
		
		for (int i = 0; i < 10; i++) {
			IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
			
			con.write("GET /test?dataLength=300 HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: test\r\n" +
					  "\r\n"); 
			
			
			String response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			con.readBytesByLength(300);
			
			Assert.assertTrue(response.indexOf("200") != -1);
			con.close();
		}
	
		server.close();
	}
	

	
	@Test
	public void testSimple2() throws Exception {
		
		System.out.println("testSimple2");
			
		IServer server = new HttpServer(new RequestHandler2());
		ConnectionUtils.start(server);
	
		
		for (int i = 0; i < 10; i++) {
			IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
			
			con.write("GET /test?dataLength=300 HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: test\r\n" +
					  "\r\n"); 
			
			
			String response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			con.readBytesByLength(300);
			
			Assert.assertTrue(response.indexOf("200") != -1);
			con.close();
		}
	
		server.close();
	}
	
	
	
	
	@Test
	public void testLessOutData() throws Exception {
		
		System.out.println("testLessOutData");
			
		LessWriterHandler hdl = new LessWriterHandler();
		IServer server = new HttpServer(hdl);
		server.start();
	
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
			
		con.write("GET /test HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: test\r\n" +
				  "\r\n"); 
						
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		System.out.println("header read");
		int contentLength = QAUtil.readContentLength(header);
					
		Assert.assertTrue(header.indexOf("200") != -1);
		
		try {
			con.readStringByLength(contentLength);
			Assert.fail("ClosedChannelException");
		} catch(ClosedChannelException expected) { }
	
		QAUtil.sleep(1000);
		
		Assert.assertNotNull(hdl.getException());

		con.close();
		server.close();
	}

	
	
	@Test
	public void testFragmented() throws Exception {
		
		System.out.println("testFragmented");
	
		errors.clear();

		
		final IServer server = new HttpServer(new RequestHandler());
		ConnectionUtils.start(server);
	
	

		for (int i = 0; i < 10; i++) {
			
			Thread t = new Thread() {
				
				@Override
				public void run() {
					
					running.incrementAndGet();
					
					try {
					
						IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

						for (int i = 0; i < 10; i++) {
							con.write("GET /?dataLength=5");
							QAUtil.sleep(150);
							
							con.write(" HTTP/1.1\r\n" +
									  "Host: localhost\r\n");
							QAUtil.sleep(150);
							
							con.write("User-Agent: test\r\n" + 
									  "\r\n");
							
							
							
							String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
							if (header.indexOf("200") == -1) {
								errors.add("got wrong response " + header);
							}
							
							con.readBytesByLength(5);
							
							System.out.print(".");
						}
						
						
						con.close();

						
					} catch (Exception e) {
						errors.add(e.toString());
					} finally {
						running.decrementAndGet();
					}
				}
			}; 
			t.start();
			
		}

		
		do {
			QAUtil.sleep(100);
		} while (running.get() > 0);
		
		server.close();
		
		
		if (!errors.isEmpty()) {
			for (String error : errors) {
				System.out.println(error);
			}
			Assert.fail("errors occured");
		}
	}
	
	

	
	@Test
	public void testPostBound() throws Exception {
		
		System.out.println("testPostBound");
			
		IServer server = new HttpServer(new EchoHandler());
		ConnectionUtils.start(server);
	
		for (int i = 0; i < 10; i++) {
			IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
			
			con.write("POST /test HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: test\r\n" +
					  "Content-Length: 10\r\n" +
					  "\r\n" +
					  "1234567890"); 
			
			
			String response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			String body = con.readStringByLength(10, "US-ASCII");
			Assert.assertEquals("1234567890", body);
			
			Assert.assertTrue(response.indexOf("200") != -1);
			con.close();
		}
	
		server.close();
	}

	
	
	@Test
	public void testPostBoundFragmented() throws Exception {
		
		System.out.println("testPostBoundFragmented");
			
		IServer server = new HttpServer(new EchoHandler());
		ConnectionUtils.start(server);
	
		
		for (int i = 0; i < 10; i++) {
			IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
			
			con.write("POST /test HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: test\r\n" +
					  "Content-Length: 10\r\n" +
					  "\r\n" +
					  "12345"); 
			

			con.write("67890"); 

			
			String response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			String body = con.readStringByLength(10, "US-ASCII");
			Assert.assertEquals("1234567890", body);
			
			Assert.assertTrue(response.indexOf("200") != -1);
			con.close();
		}
	
		server.close();
	}




	@Test
	public void testPostChunkedPipelining() throws Exception {
		
		System.out.println("testPostChunkedPipelining");
			
		IServer server = new HttpServer(new EchoHandler());
		ConnectionUtils.start(server);

		for (int i = 0; i < 10; i++) {
			IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
			
			con.write("POST /test HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: test\r\n" +
					  "Transfer-Encoding: Chunked\r\n" +
					  "\r\n" +
					  "5\r\n" +
					  "12345\r\n" +
					  "0\r\n\r\n" +
					  "POST /test HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: test\r\n" +
					  "Transfer-Encoding: Chunked\r\n" +
					  "\r\n" +
					  "5\r\n" +
					  "12345\r\n" +
					  "0\r\n\r\n"); 
			

			
			String response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			String body = con.readStringByLength(5, "US-ASCII");
			Assert.assertEquals("12345", body);
			
			Assert.assertTrue(response.indexOf("200") != -1);
			
			
			response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			body = con.readStringByLength(5, "US-ASCII");
			Assert.assertEquals("12345", body);
			
			Assert.assertTrue(response.indexOf("200") != -1);
			
			con.close();
		}
	
		server.close();
	}




	@Test
	public void testMixedPipelining() throws Exception {
		
		System.out.println("testMixedPipelining");
		
		System.setProperty("org.xsocket.connection.http.server.showDetailedError", "true");
			
		IServer server = new HttpServer(new EchoHandler());
		ConnectionUtils.start(server);
	
	
		
		for (int i = 0; i < 10; i++) {
			IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
			
			con.write("POST /test HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: test\r\n" +
					  "Content-Length: 10\r\n" +
					  "\r\n" +
					  "1234567890" +
					  "GET /test?dataLength=300 HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: test\r\n" +
					  "\r\n" +
					  "POST /test2 HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: test\r\n" +
					  "Content-Length: 8\r\n" +
					  "\r\n" +
					  "12345678" +
					  "GET /test?dataLength=300 HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: test\r\n" +
					  "\r\n"); 
			
			
			String response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			String body = con.readStringByLength(10, "US-ASCII");
			Assert.assertEquals("1234567890", body);
			
			Assert.assertTrue(response.indexOf("200") != -1);
			
			
			
			response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			con.readBytesByLength(300);
			
			Assert.assertTrue(response.indexOf("200") != -1);
			
			
			
			
			response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			body = con.readStringByLength(8, "US-ASCII");
			Assert.assertEquals("12345678", body);
			
			Assert.assertTrue(response.indexOf("200") != -1);
	
			
			
			
			response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			con.readBytesByLength(300);
			
			Assert.assertTrue(response.indexOf("200") != -1);
			
			
			
			con.close();
		}
	
		server.close();
	}




	@Test
	public void testPostBoundPipelining() throws Exception {
		
		System.out.println("testPostBoundPipelining");
			
		IServer server = new HttpServer(new EchoHandler());
		ConnectionUtils.start(server);
	
		
		for (int i = 0; i < 10; i++) {
			IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
			
			con.write("POST /test HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: test\r\n" +
					  "Content-Length: 10\r\n" +
					  "\r\n" +
					  "1234567890" +
					  "POST /test2 HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: test\r\n" +
					  "Content-Length: 8\r\n" +
					  "\r\n" +
					  "12345678"); 
			
			
			String response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			String body = con.readStringByLength(10, "US-ASCII");
			Assert.assertEquals("1234567890", body);
			
			Assert.assertTrue(response.indexOf("200") != -1);
			
			
			response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			body = con.readStringByLength(8, "US-ASCII");
			Assert.assertEquals("12345678", body);
			
			Assert.assertTrue(response.indexOf("200") != -1);
	
			con.close();
		}
	
		server.close();
	}




	@Test
	public void testPostChunkedFragmented() throws Exception {
		
		System.out.println("testPostChunkedFragmented");
			
		IServer server = new HttpServer(new EchoHandler());
		ConnectionUtils.start(server);
	
	
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
	
		for (int i = 0; i < 10; i++) {
			
			con.write("POST /test HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: test\r\n" +
					  "Transfer-Encoding: Chunked\r\n" +
					  "\r\n" +
					  "5\r\n" +
					  "123"); 
			
			con.write("45\r\n" +
					  "0\r\n\r\n");
			
			
			String response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			String body = con.readStringByLength(5, "US-ASCII");
			Assert.assertEquals("12345", body);
			
			Assert.assertTrue(response.indexOf("200") != -1);
			System.out.print(".");
		}
	
		con.close();

		server.close();
	}
	
	
	@Test
	public void testPostChunkedFragmented2() throws Exception {
		
		System.out.println("testPostChunkedFragmented2");
			
		IServer server = new HttpServer(new EchoHandler());
		ConnectionUtils.start(server);
	
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
	
		for (int i = 0; i < 10; i++) {
			
			con.write("POST /test HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: test\r\n" +
					  "Transfer-Encoding: Chunked\r\n" +
					  "\r\n" +
					  "5\r\n" +
					  "12345\r\n" +
					  "0"); 
			
			con.write("\r\n\r\n");
			
			
			String response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			String body = con.readStringByLength(5, "US-ASCII");
			Assert.assertEquals("12345", body);
			
			Assert.assertTrue(response.indexOf("200") != -1);
			System.out.print(".");
		}
	
		con.close();
	
		server.close();
	}
	
	

	@Test
	public void testPostChunkedFragmented3() throws Exception {
		
		System.out.println("testPostChunkedFragmented3");
			
		IServer server = new HttpServer(new EchoHandler());
		ConnectionUtils.start(server);
	
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
	
		for (int i = 0; i < 10; i++) {
			
			con.write("POST /test HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: test\r\n" +
					  "Transfer-Encoding: Chunked\r\n" +
					  "\r\n" +
					  "5\r\n" +
					  "12345\r\n" +
					  "0\r\n\r"); 
			
			con.write("\n");
			
			
			String response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			String body = con.readStringByLength(5, "US-ASCII");
			Assert.assertEquals("12345", body);
			
			Assert.assertTrue(response.indexOf("200") != -1);
			System.out.print(".");
		}
	
		con.close();
		server.close();
	}




	@Test
	public void testPostChunkedWithTrailer() throws Exception {
		
		System.out.println("testPostChunkedWithTrailer");
			
		IServer server = new HttpServer(new EchoHandler());
		ConnectionUtils.start(server);
	
	
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
	
		for (int i = 0; i < 10; i++) {
			
			con.write("POST /test HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: test\r\n" +
					  "Transfer-Encoding: Chunked\r\n" +
					  "\r\n" +
					  "5\r\n" +
					  "12345\r\n" +
					  "0\r\n" +
					  "X-Test: testvalue\r\n\r\n"); 
			
			
			String response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			String body = con.readStringByLength(5, "US-ASCII");
			Assert.assertEquals("12345", body);
			
			Assert.assertTrue(response.indexOf("200") != -1);
			System.out.print(".");
		}
	
		con.close();
	
		server.close();
	}




	@Test
	public void testPostChunked() throws Exception {
		
		System.out.println("testPostChunked");
			
		IServer server = new HttpServer(new EchoHandler());
		ConnectionUtils.start(server);
	
	
	
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
	
		for (int i = 0; i < 10; i++) {
			
			con.write("POST /test HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: test\r\n" +
					  "Transfer-Encoding: Chunked\r\n" +
					  "\r\n" +
					  "5\r\n" +
					  "12345\r\n" +
					  "0\r\n\r\n"); 
			
			
			String response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			String body = con.readStringByLength(5, "US-ASCII");
			Assert.assertEquals("12345", body);
			
			Assert.assertTrue(response.indexOf("200") != -1);
			System.out.print(".");
		}
	
		con.close();
		server.close();
	}

	
	@Test
	public void testAmbiguousServer() throws Exception {
		
		System.out.println("testAmbiguousServer");
		
		
		IServer server = new HttpServer(new AmbiguousHandler());
		server.start();
	
	
	
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
	
		con.write("GET /test HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: test\r\n" +
				  "\r\n"); 
			
			
		String response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		String body = con.readStringByLength(5, "US-ASCII");
		Assert.assertEquals("12345", body);
			
		Assert.assertTrue(response.indexOf("200") != -1);
		System.out.print(".");
	
		con.close();
		server.close();
	}




	@Test
	public void testPostChunkedFragmented4() throws Exception {
		
		System.out.println("testPostChunkedFragmented4");
				
		IServer server = new HttpServer(new EchoHandler());
		ConnectionUtils.start(server);
	
	
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
	
		for (int i = 0; i < 10; i++) {
			
			con.write("POST /test HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: test\r\n" +
					  "Transfer-Encoding: Chunk"); 
			
			con.write("ed\r\n" +
					  "\r\n" +
					  "5\r\n" +
					  "123"); 

			con.write("45\r\n" +
					  "0\r\n\r\n");
			
			
			String response = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			String body = con.readStringByLength(5, "US-ASCII");
			Assert.assertEquals("12345", body);
			
			Assert.assertTrue(response.indexOf("200") != -1);
			System.out.print(".");
		}
	
		con.close();
	
		server.close();
	}




	@Execution(Execution.NONTHREADED)
	private static final class RequestHandler implements IHttpRequestHandler {
		
		private static final int OFFSET = 48;
		
		private static int count = 0;
		private int cachedSize = 100; 
		private byte[] cached = generateByteArray(cachedSize);

		
		
		public void onRequest(IHttpExchange exchange) throws IOException {

			IHttpRequest request = exchange.getRequest();

			int size = request.getIntParameter("dataLength");
			
			if (size > 0) {
				if (size != cachedSize) {
					cachedSize = size;
					cached = generateByteArray(size);
				} 
			}

			try {
				BodyDataSink bodyDataSink = exchange.send(new HttpResponseHeader(200, "text/plain"), size);
				bodyDataSink.setFlushmode(FlushMode.ASYNC);
				bodyDataSink.write(cached);
				bodyDataSink.close();
				
				count++;
			} catch (Exception e) {
				e.printStackTrace();
//				onRequestHandlingException(e);
			}
		}
		

		
		

		public static byte[] generateByteArray(int length) {
			
			byte[] bytes = new byte[length];
			
			int item = OFFSET;
			
			for (int i = 0; i < length; i++) {
				bytes[i] = (byte) item;
				
				item++;
				if (item > (OFFSET + 9)) {
					item = OFFSET;
				}
			}
			
			return bytes;
		}
	}
	
	
	

	@Execution(Execution.NONTHREADED)
	private static final class RequestHandler2 implements IHttpRequestHandler {
		
		private static final int OFFSET = 48;
		
		public void onRequest(IHttpExchange exchange) throws IOException {

			IHttpRequest request = exchange.getRequest();

			int size = request.getIntParameter("dataLength");
			
			exchange.send(new HttpResponse(200, "text/plain", generateByteArray(size)));
		}
		



		public static byte[] generateByteArray(int length) {
			
			byte[] bytes = new byte[length];
			
			int item = OFFSET;
			
			for (int i = 0; i < length; i++) {
				bytes[i] = (byte) item;
				
				item++;
				if (item > (OFFSET + 9)) {
					item = OFFSET;
				}
			}
			
			return bytes;
		}
	}
	
	
	
	private static final class AmbiguousHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {

			IHttpResponseHeader header = new HttpResponseHeader(200, "text/plain");
		    NonBlockingBodyDataSource dataSource = new InMemoryBodyDataSource(header, new ByteBuffer[] { DataConverter.toByteBuffer("12345", IHttpMessageHeader.DEFAULT_ENCODING)} );
			HttpResponse response = new HttpResponse(header, dataSource);
			response.setHeader("Server", "me");
			response.setHeader("Connection", "close");
			
			exchange.send(response);
		}
	}
	
	

	@Execution(Execution.NONTHREADED)
	private static final class EchoHandler implements IHttpRequestHandler {
		
		@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
		public void onRequest(IHttpExchange exchange) throws IOException {

			IHttpRequest request = exchange.getRequest();
			
			if (request.hasBody()) {
				String body = request.getBody().readString();
				exchange.send(new HttpResponse(200, "text/plain", body));
				
			} else {
				int dataLength = request.getIntParameter("dataLength"); 
				exchange.send(new HttpResponse(200, "text/plain", QAUtil.generateByteArray(dataLength)));
			}
		}		
	}
	
	
	@Execution(Execution.NONTHREADED)
	private static final class LessWriterHandler implements IHttpRequestHandler {
		
		public static final int LENGTH = 1000;
		
		private Exception e = null; 
		
		@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
		public void onRequest(IHttpExchange exchange) throws IOException {

			try {
				BodyDataSink bodyDataSink = exchange.send(new HttpResponseHeader(200, "text/plain"), LENGTH);
				bodyDataSink.setFlushmode(FlushMode.ASYNC);
				bodyDataSink.write(QAUtil.generateByteArray(LENGTH / 2));
				bodyDataSink.close();
			} catch (IOException ioe) {
				this.e = ioe;
			}
		}		
		
		
		Exception getException() {
			return e;
		}
	}
}
