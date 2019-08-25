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


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IServer;




/**
*
* @author grro@xlightweb.org
*/
public final class CompatibilityFileChannelTest {
	
	
	public static void main(String[] args) throws Exception {
		
		for (int i = 0; i < 1000; i++) {
			new CompatibilityFileChannelTest().testTransferTo();
		}
	}

	@Test 
	public void testNonBlockingTransferFrom() throws Exception {
		
		System.out.println("testNonBlockingTransferFrom");
		
		RequestHandler reqHdl = new RequestHandler();
		HttpServer server = new HttpServer(reqHdl);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		QAUtil.sleep(200);
		
		NonBlockingBodyDataSource clientChannel = response.getNonBlockingBody();
		
		BodyDataSink serverChannel = reqHdl.dataSinkRef.get();
		
		
		File file = QAUtil.createTempfile();
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		FileChannel fc = raf.getChannel();
		
		String txt = "Hello my client\r\n";
		serverChannel.write(txt);
		serverChannel.flush();
		QAUtil.sleep(200);
		
		long transfered = fc.transferFrom(clientChannel, 0, 9000000);
		fc.close();
		raf.close();
		
		Assert.assertEquals(txt.length(), transfered);
		Assert.assertTrue(QAUtil.isEquals(file, txt));

		file.delete();
		clientChannel.close();
		server.close();
	}

	

	@Test 
	public void testBlockingTransferFromServerClosed() throws Exception {
		
		System.out.println("testBlockingTransferFromServerClosed");
	
		RequestHandler reqHdl = new RequestHandler();
		final IServer server = new HttpServer(reqHdl);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		QAUtil.sleep(200);
		
		BodyDataSource clientChannel = response.getBody();
		
		BodyDataSink serverChannel = reqHdl.dataSinkRef.get();
		
		File file = QAUtil.createTempfile();
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		FileChannel fc = raf.getChannel();
		
		String txt = "Hello my client\r\n";
		serverChannel.write(txt);
		serverChannel.flush();
		server.close();
		
		QAUtil.sleep(1000);

		try {
		    fc.transferFrom(clientChannel, 0, 17);
		    Assert.fail("IOException expected");
		} catch (IOException expected) { }

		file.delete();
		server.close();
	}

	
	
	@Test 
	public void testNonBlockingTransferFromServerClosed() throws Exception {
		
		System.out.println("testNonBlockingTransferFromServerClosed");
		
		RequestHandler reqHdl = new RequestHandler();
		IServer server = new HttpServer(reqHdl);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		QAUtil.sleep(200);
		
		NonBlockingBodyDataSource clientChannel = response.getNonBlockingBody();
		
        BodyDataSink serverChannel = reqHdl.dataSinkRef.get();
		
		File file = QAUtil.createTempfile();
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		FileChannel fc = raf.getChannel();
		
		String txt = "Hello my client\r\n";
		serverChannel.write(txt);
		serverChannel.flush();
		serverChannel.close();
		QAUtil.sleep(200);
		
		long transfered = fc.transferFrom(clientChannel, 0, 9000000);
		fc.close();
		raf.close();
		
		Assert.assertEquals(txt.length(), transfered);
		Assert.assertTrue(QAUtil.isEquals(file, txt));
		
		clientChannel.close();
		file.delete();
	}
	

	@Test 
	public void testNonBlockingTransferFromClientClosed() throws Exception {
		
		System.out.println("testNonBlockingTransferFromClientClosed");
		
		RequestHandler reqHdl = new RequestHandler();
		IServer server = new HttpServer(reqHdl);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		QAUtil.sleep(200);
		
		NonBlockingBodyDataSource clientChannel = response.getNonBlockingBody();
		
        BodyDataSink serverChannel = reqHdl.dataSinkRef.get();
	
		
		File file = QAUtil.createTempfile();
		System.out.println(file.getAbsolutePath());
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		FileChannel fc = raf.getChannel();
		
		String txt = "Hello my client\r\n";
		serverChannel.write(txt);
		serverChannel.flush();
		QAUtil.sleep(200);
		
		clientChannel.close();
		
		try {
			fc.transferFrom(clientChannel, 0, 9000000);
			Assert.fail("ClosedChannelException expected");
		} catch (ClosedChannelException expected) { }
		
		file.delete();
		server.close();
		clientChannel.close();
	}
	
	
	@Test 
	public void testBlockingTransferFromClientClosed() throws Exception {
		
		System.out.println("testBlockingTransferFromClientClosed");
	
		RequestHandler reqHdl = new RequestHandler();
		final IServer server = new HttpServer(reqHdl);
		ConnectionUtils.start(server);
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		QAUtil.sleep(200);
		
		BodyDataSource clientChannel = response.getBody();
		
        BodyDataSink serverChannel = reqHdl.dataSinkRef.get();
	
		
		File file = QAUtil.createTempfile();
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		FileChannel fc = raf.getChannel();
		
		String txt = "Hello my client\r\n";
		serverChannel.write(txt);
		serverChannel.flush();
		QAUtil.sleep(200);
		
		clientChannel.close();
		
		try {
			fc.transferFrom(clientChannel, 0, 9000000);
			Assert.fail("ClosedChannelException expected");
		} catch (ClosedChannelException expected) { }
		
		file.delete();
		server.close();
	}
	
	
	
	@Test 
	public void testTransferTo() throws Exception {
		
		System.out.println("testTransferTo");
		
		RequestHandler2 reqHdl = new RequestHandler2();
		IServer server = new HttpServer(reqHdl);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		
		BodyDataSink clientChannel = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test/resource"), respHdl);
		
		File file = QAUtil.createTestfile_40k();
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		FileChannel fc = raf.getChannel();
		
		fc.transferTo(0, fc.size(), clientChannel);
		
		clientChannel.close();
		fc.close();
		raf.close();
		
		Assert.assertEquals(200, respHdl.getResponse().getStatus());
		
		Assert.assertTrue(QAUtil.isEquals(file, new ByteBuffer[] { ByteBuffer.wrap(reqHdl.getDataSource().readBytes()) }));
		
		file.delete();
		server.close();
	}
	
	
	
	@Test 
	public void testTransferToSourceClosed() throws Exception {
		
		System.out.println("testTransferToSourceClosed");

		RequestHandler2 reqHdl = new RequestHandler2();
		IServer server = new HttpServer(reqHdl);
		ConnectionUtils.start(server);
		
		HttpClient httpClient = new HttpClient();
		
		
		FutureResponseHandler respHdl = new FutureResponseHandler();		
		BodyDataSink clientChannel = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/"), respHdl);
		
		File file = QAUtil.createTestfile_40k();
		file.deleteOnExit();
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		FileChannel fc = raf.getChannel();
		
		fc.transferTo(0, fc.size(), clientChannel);

		clientChannel.close();
		fc.close();
		raf.close();
		
		
		Assert.assertEquals(200, respHdl.getResponse().getStatus());

		Assert.assertTrue(QAUtil.isEquals(file, new ByteBuffer[] { ByteBuffer.wrap(reqHdl.getDataSource().readBytes())}));
		
		file.delete();
		server.close();
	}
	
	
	

	private static final class RequestHandler implements IHttpRequestHandler {

		private final AtomicReference<BodyDataSink> dataSinkRef = new AtomicReference<BodyDataSink>();
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			BodyDataSink dataSink = exchange.send(new HttpResponseHeader(200));
			dataSink.flush();
			
			dataSinkRef.set(dataSink);
		}
	}
	
	private static final class RequestHandler2 implements IHttpRequestHandler {

		private final AtomicReference<BodyDataSource> dataSourceRef = new AtomicReference<BodyDataSource>();
		
		
		@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
		public void onRequest(IHttpExchange exchange) throws IOException {
			dataSourceRef.set(exchange.getRequest().getBody());
			
			exchange.send(new HttpResponse(200));
		}
		

		BodyDataSource getDataSource() {
			return dataSourceRef.get();
		}
	}
}
