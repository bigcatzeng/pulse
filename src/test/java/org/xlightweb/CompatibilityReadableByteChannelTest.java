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


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.Server;




/**
*
* @author grro@xlightweb.org
*/
public final class CompatibilityReadableByteChannelTest {

    
    
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 1000; i++) {
            new CompatibilityReadableByteChannelTest().testBoundProtocolError();
        }
    }
    

	
	@Test 
	public void testBoundProtocolError() throws Exception {

	    System.out.println("testBoundProtocolError");
	    
		class DataHandler implements IConnectHandler {

			private final AtomicReference<INonBlockingConnection> nbcRef = new AtomicReference<INonBlockingConnection>(null);
			
			public boolean onConnect(INonBlockingConnection nbc) throws IOException {
				nbcRef.set(nbc);
				return true;
			}
			
			INonBlockingConnection getConnection() {
				return nbcRef.get();
			}
		}
		
		
		DataHandler dh = new DataHandler();
		IServer server = new Server(dh); 
		server.start();
		
		
		HttpClient httpClient = new HttpClient();
		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/"), respHdl);

		IBlockingConnection serverCon = null;
		long start = System.currentTimeMillis();
		do {
			INonBlockingConnection nbc = dh.getConnection();
			if (nbc != null) {
				serverCon = new BlockingConnection(nbc);
				break;
			}
			QAUtil.sleep(100);
 		} while (System.currentTimeMillis() < (start +  (30 * 1000)));
		
		
		Assert.assertNotNull("Timeout: could not fetch serverCon ", serverCon);
		
		serverCon.readStringByDelimiter("\r\n\r\n");
		serverCon.setAutoflush(false);
		serverCon.write("HTTP/1.1 200 OK\r\n");
		serverCon.write("SERVER: xSocket-http/2.0-alpha-5\r\n");
		serverCon.write("Content-Length: 200\r\n");
		serverCon.write("\r\n");
		serverCon.write("3454353");
		serverCon.flush();
		
		IHttpResponse response = respHdl.getResponse();
		NonBlockingBodyDataSource clientChannel = response.getNonBlockingBody();
		
		QAUtil.sleep(1000);
		ByteBuffer buf = ByteBuffer.allocate(1000);
		clientChannel.read(buf);
		
		if (buf.position() != 7) {
			System.out.println(buf.position() + " read instead of 7");
			Assert.fail(buf.position() + " read instead of 7");
		}
		

		serverCon.write("7878");
		serverCon.flush();
		serverCon.close();

		
		QAUtil.sleep(500);
		
		try {
		    clientChannel.available();
		    Assert.fail("ProtocolException expected");
		} catch (ProtocolException expected) { }
		
		
		clientChannel.close();
		httpClient.close();
		server.close();
	}

	
	
	@Test 
	public void testChunkedProtocolError() throws Exception {

	    
	    System.out.println("testChunkedProtocolError");
		
		IDataHandler dh = new IDataHandler() {

			public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {

				connection.readStringByDelimiter("\r\n\r\n");
				
				
				connection.setAutoflush(false);
				connection.write("HTTP/1.1 200 OK\r\n");
				connection.write("SERVER: xSocket-http/2.0-alpha-5\r\n");
				connection.write("Transfer-Encoding: chunked\r\n");
				connection.write("\r\n");
				connection.write("7\r\n");
				connection.write("3454353\r\n");
				connection.flush();
				QAUtil.sleep(500);
				
				connection.write("P\r\n");
				connection.flush();
				
				return true;				
			}
		};
		
		IServer server = new Server(dh); 
		ConnectionUtils.start(server);
		
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		QAUtil.sleep(200);
		
		NonBlockingBodyDataSource clientChannel = response.getNonBlockingBody();
		
		ByteBuffer buf = ByteBuffer.allocate(1000);
		clientChannel.read(buf);
		Assert.assertEquals(7, buf.position());
		
		QAUtil.sleep(500);
		
		try {
			buf = ByteBuffer.allocate(1000);
			clientChannel.read(buf);
			Assert.fail("IOException (protocol error) exepcted");
		} catch(IOException exepcted) {  }
		
		
		clientChannel.close();
		httpClient.close();
		server.close();
	}
	
	

	@Test 
	public void testNonBlockingReadEndOfStream() throws Exception {
	    
	    System.out.println("testNonBlockingReadEndOfStream");
		
		RequestHandler reqHdl = new RequestHandler();
		HttpServer server = new HttpServer(reqHdl);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		QAUtil.sleep(200);
		
		NonBlockingBodyDataSource clientChannel = response.getNonBlockingBody();
				
		BodyDataSink serverChannel = reqHdl.getDataSink();
		serverChannel.write(QAUtil.generateByteArray(100));
		QAUtil.sleep(200);
		
		// in buffer contains 100 bytes
		ByteBuffer buffer = ByteBuffer.allocate(60);
		int read = clientChannel.read(buffer);
		Assert.assertEquals(60, read);
		
		// in buffer contains 40 bytes
		buffer = ByteBuffer.allocate(60);
		read = clientChannel.read(buffer);
		Assert.assertEquals(40, read);
		
		// in buffer contains 0 bytes
		buffer = ByteBuffer.allocate(60);
		read = clientChannel.read(buffer);
		Assert.assertEquals(0, read);
	
		serverChannel.write(5);
		serverChannel.close();
		QAUtil.sleep(200);

		// in buffer contains 4 bytes
		buffer = ByteBuffer.allocate(60);
		read = clientChannel.read(buffer);
		Assert.assertEquals(4, read);
		
		Assert.assertFalse(clientChannel.isOpen());

		httpClient.close();
		server.close();
	}
	

	
	

	
	
	
	
	@Test 
	public void testBlockingReadEndOfStream() throws Exception {
	    
	    System.out.println("testBlockingReadEndOfStream");

	    RequestHandler reqHdl = new RequestHandler();
		IServer server = new HttpServer(reqHdl);
		ConnectionUtils.start(server);
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		QAUtil.sleep(200);
		
		BodyDataSource clientChannel = response.getBody();
		
		final BodyDataSink serverChannel = reqHdl.getDataSink();
		
		serverChannel.write(QAUtil.generateByteArray(100));
		QAUtil.sleep(1000);
		
		// in buffer contains 100 bytes
		ByteBuffer buffer = ByteBuffer.allocate(60);
		int read = clientChannel.read(buffer);
		Assert.assertEquals(60, read);
		
		// in buffer contains 40 bytes
		buffer = ByteBuffer.allocate(60);
		read = clientChannel.read(buffer);
		Assert.assertEquals(40, read);

		Thread t = new Thread() {
			@Override
			public void run() {
				QAUtil.sleep(500);
				try {
					serverChannel.write((byte) 6);
					serverChannel.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		};
		t.start();
		
		// in buffer contains 0 bytes (after 500 millis 1 bytes)
		buffer = ByteBuffer.allocate(60);
		read = clientChannel.read(buffer);
		Assert.assertEquals(1, read);
			
		buffer = ByteBuffer.allocate(60);
		read = clientChannel.read(buffer);
		Assert.assertEquals(-1, read);
		
		Assert.assertFalse(clientChannel.isOpen());
		
		
		httpClient.close();
		server.close();
	}
	

	@Test 
	public void testNonBlockingReadClientChannelClosed() throws Exception {
	    
	    System.out.println("testNonBlockingReadClientChannelClosed");
		
		RequestHandler reqHdl = new RequestHandler();
		IServer server = new HttpServer(reqHdl);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		QAUtil.sleep(200);
		
		NonBlockingBodyDataSource clientChannel = response.getNonBlockingBody();
		
		// retrieve server-side channel 
		BodyDataSink serverChannel = reqHdl.getDataSink();

		// and write data 
		serverChannel.write(QAUtil.generateByteArray(100));
		QAUtil.sleep(200);
		
		
		// in buffer contains 100 bytes
		ByteBuffer buffer = ByteBuffer.allocate(60);
		int read = clientChannel.read(buffer);
		Assert.assertEquals(60, read);

		clientChannel.close();
		
		try {
		    clientChannel.available();
		    Assert.fail("ProtocolException expected");
		} catch (ProtocolException expected) { }
		
		httpClient.close();
		server.close();
	}
	

	
	
	
	
	
	@Test 
	public void testBlockingReadClientChannelClosed() throws Exception {
	    
	    System.out.println("testBlockingReadClientChannelClosed");
		
		RequestHandler reqHdl = new RequestHandler();
		IServer server = new HttpServer(reqHdl);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		QAUtil.sleep(200);
		
		BodyDataSource clientChannel = response.getBody();
		
		BodyDataSink serverChannel = reqHdl.getDataSink();
		
		serverChannel.write(QAUtil.generateByteArray(100));
		QAUtil.sleep(1000);
		
		// in buffer contains 100 bytes
		ByteBuffer buffer = ByteBuffer.allocate(60);
		int read = clientChannel.read(buffer);
		Assert.assertEquals(60, read);

		
		clientChannel.close();
		
		buffer = ByteBuffer.allocate(60);
		try {
			read = clientChannel.read(buffer);
			Assert.fail("ProtocolException expected");
		} catch (ProtocolException expected) {  }  
		
		server.close();
	}
	
	


	@Test 
	public void testBlockingRead() throws Exception {
	    
	    System.out.println("testBlockingRead");
		
		RequestHandler reqHdl = new RequestHandler();
		IServer server = new HttpServer(reqHdl);
		ConnectionUtils.start(server);
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		QAUtil.sleep(200);
		
		BodyDataSource clientChannel = response.getBody();
		
		BodyDataSink serverChannel = reqHdl.getDataSink();
		
		serverChannel.write(QAUtil.generateByteArray(4));
		QAUtil.sleep(200);
	
		clientChannel.readInt();
		Assert.assertTrue(clientChannel.isOpen());
		
		serverChannel.write(QAUtil.generateByteArray(7));
		serverChannel.close();
		QAUtil.sleep(1000);
		
		clientChannel.readInt();
		Assert.assertTrue(clientChannel.isOpen());

		try {
			clientChannel.readInt();
			Assert.fail("ClosedChannelException excepted");
		} catch (ClosedChannelException excepted) { }
		
		Assert.assertFalse(clientChannel.isOpen());
		
		server.close();
	}
	
	
	@Test 
	public void testNonBlockingRead() throws Exception {
	    
	    System.out.println("testNonBlockingRead");
		
		RequestHandler reqHdl = new RequestHandler();
		IServer server = new HttpServer(reqHdl);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		QAUtil.sleep(200);
		
		NonBlockingBodyDataSource clientChannel = response.getNonBlockingBody();
		
		// retrieve server-side channel 
		BodyDataSink serverChannel = reqHdl.getDataSink();
		
		// and write 4 bytes 
		serverChannel.write(QAUtil.generateByteArray(4));
		QAUtil.sleep(200);
	
		// read it on the client-side
		Assert.assertEquals(4, clientChannel.available());
		clientChannel.readInt();
		Assert.assertEquals(0, clientChannel.available());
		Assert.assertTrue(clientChannel.isOpen());
		
		// write further 7 bytes 
		serverChannel.write(QAUtil.generateByteArray(7));
		serverChannel.close();
		QAUtil.sleep(500);
		
		// read 4 bytes of it (3 bytes remains)
		Assert.assertEquals(7, clientChannel.available());
		clientChannel.readInt();
		Assert.assertEquals(3, clientChannel.available());
		Assert.assertTrue(clientChannel.isOpen());

		try {
			clientChannel.readInt();
			Assert.fail("ClosedChannelException excepted");
		} catch (ClosedChannelException excepted) { }
		
		Assert.assertFalse(clientChannel.isOpen());
		Assert.assertEquals(3, clientChannel.available());
		
		server.close();
	}
	
	


    @Test 
    public void testReadLine() throws Exception {
        
        System.out.println("testReadLine");
        
        RequestHandler reqHdl = new RequestHandler();
        IServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        QAUtil.sleep(200);

        BodyDataSink srvBodyDataSink = reqHdl.getDataSink();
        File file = QAUtil.createTestfile_400k();
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel fc = raf.getChannel();
        srvBodyDataSink.transferFrom(fc);
        fc.close();
        raf.close();
        
        srvBodyDataSink.close();
        QAUtil.sleep(1000);
        
        
        BodyDataSource clientChannel = response.getBody();
        InputStream is = Channels.newInputStream(clientChannel);
        LineNumberReader lnr = new LineNumberReader(new InputStreamReader(is));
        
        StringBuilder sb = new StringBuilder();
        String line = null;
        do {
            line = lnr.readLine();
            if (line != null) {
                sb.append(line + "\r\n");
            }
        } while (line != null);


        
        InputStream is2 = new FileInputStream(file);
        LineNumberReader lnr2 = new LineNumberReader(new InputStreamReader(is2));
        
        StringBuilder sb2 = new StringBuilder();
        String line2 = null;
        do {
            line2 = lnr2.readLine();
            if (line2 != null) {
                sb2.append(line2 + "\r\n");
            }
        } while (line2 != null);
        
        Assert.assertEquals(sb2.toString(), sb.toString());
        
        
        file.delete();
        httpClient.close();
        server.close();        
    }
    
	
	
	
	private static final class RequestHandler implements IHttpRequestHandler {

		private BodyDataSink dataSink = null;
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			dataSink = exchange.send(new HttpResponseHeader(200));
			dataSink.flush();
		}
		

		BodyDataSink getDataSink() {
			return dataSink;
		}
	}
}
