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


import java.io.File;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;



import org.junit.Assert;
import org.junit.Test;


import org.xlightweb.BodyDataSink;
import org.xlightweb.GetRequest;
import org.xlightweb.HttpRequestHeader;
import org.xlightweb.HttpResponse;
import org.xlightweb.IBodyDataHandler;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.NonBlockingBodyDataSource;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IServer;
import org.xsocket.connection.IConnection.FlushMode;





/**
*
* @author grro@xlightweb.org
*/
public final class ChannelStreamingTest  {


	
	@Test
	public void testSendStreaming() throws Exception {
	    		
		IServer server = new HttpServer(new ContentEchoHandler());
		ConnectionUtils.start(server);
	
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
	
	

		File file = QAUtil.createTestfile_40k();
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
	    FileChannel fc = raf.getChannel();
	
	    int size = (int) fc.size();
	    
		FutureResponseHandler hdl = new FutureResponseHandler();
		BodyDataSink bodyDataSink = con.send(new HttpRequestHeader("POST", "/", "text/plain"), size, hdl);
		
		if (bodyDataSink.getFlushmode() != FlushMode.SYNC) {
			System.out.println("flush mode is not sync");
			Assert.fail("flush mode is not sync"); 
		}
		fc.transferTo(0, size, bodyDataSink);
		if (bodyDataSink.getFlushmode() != FlushMode.SYNC) {
			System.out.println("flush mode after copy is not sync");
			Assert.fail("flush mode after copy is not sync"); 
		}
		bodyDataSink.close();
		fc.close();
		raf.close();

		IHttpResponse response = hdl.getResponse();
		Assert.assertEquals(200, response.getStatus());
		
		
	
		file.delete();
		con.close();
		server.close();
	}



	
	
	
	
	
	@Test
	public void testReceiveStreaming() throws Exception {
		
		IServer server = new HttpServer(new HeaderInfoServerHandler());
		ConnectionUtils.start(server);
	
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
	
		File file = QAUtil.createTempfile();
		System.out.println("writing into " + file.getAbsolutePath());
	
		StreamingResponseHandler cltHdl = new StreamingResponseHandler(file);
		con.send(new GetRequest("/"), cltHdl);
	
		QAUtil.sleep(1000);
		
		LineNumberReader lnr = new LineNumberReader(new FileReader(file));
	
		Assert.assertEquals("method= GET", lnr.readLine());
		
		file.delete();
		con.close();
		server.close();
	}

	
	
	
	
	private static final class StreamingResponseHandler implements IHttpResponseHandler {
		
		private File file = null;
		
		public StreamingResponseHandler(File file) {
			this.file = file;
		}
		
		public void onResponse(IHttpResponse response) throws IOException {
			response.getNonBlockingBody().setDataHandler(new BodyHandler(file));
			
			QAUtil.sleep(200);
		}
		
		public void onException(IOException ioe) {
		}
	}

	
	private static final class BodyHandler implements IBodyDataHandler {

		   private final RandomAccessFile raf;
		   private final FileChannel fc;
		   private final File file;

		   BodyHandler(File file) throws IOException {
			   this.file = file;
			   raf = new RandomAccessFile(file, "rw");
			   fc = raf.getChannel();
		   }

		   public boolean onData(NonBlockingBodyDataSource bodyDataSource) {
			   try {
				   int available = bodyDataSource.available();
				   
				   if (available > 0) { 
					   bodyDataSource.transferTo(fc, available);
					   
				   } else if (available == -1) {
					   fc.close();
					   raf.close();
				   }
			   } catch (IOException ioe) {
				   file.delete();
			   }
		      return true;
		   }
	} 
	


	


	@Test
	public void testLiveFileStreaming() throws Exception {
		

		HttpClientConnection con = new HttpClientConnection("xlightweb.sourceforge.net", 80);
	 	
		IHttpResponse response = con.call(new GetRequest("http://xlightweb.sourceforge.net/testfiles/Testfile_40k.html"));
			
		
		File file = QAUtil.createTempfile();
		System.out.println("write to file " + file.getAbsolutePath());
			
		FileChannel fc = new RandomAccessFile(file, "rw").getChannel();

		Assert.assertEquals(200, response.getStatus());
		ReadableByteChannel bodyChannel= response.getBody();
			
		ByteBuffer transferBuffer = ByteBuffer.allocateDirect(8192);
				    
		while (bodyChannel.read(transferBuffer) != -1) {
			transferBuffer.flip();
			
			while (transferBuffer.hasRemaining()) {
				fc.write(transferBuffer);
				}
			transferBuffer.clear();
		}
	
		
		LineNumberReader lnr = new LineNumberReader(new FileReader(file));
		int countLines = 0;
		String lastLine = null;
		String line = null;
		
		do {
			line = lnr.readLine();
			if (line != null) {
				lastLine = line;
				countLines++;
			}
		} while(line != null); 

		Assert.assertTrue(countLines == 939);
		Assert.assertEquals("<!--  End SiteCatalyst code  --></div></body></html>", lastLine);
		
		file.delete();
		con.close();
	}
	
	@Test
	public void testLiveFileStreaming2() throws Exception {
	
		HttpClientConnection con = new HttpClientConnection("xlightweb.sourceforge.net", 80);
	 	
		IHttpResponse response = con.call(new GetRequest("http://xlightweb.sourceforge.net/testfiles/Testfile_40k.html"));
			
		
		File file = QAUtil.createTempfile();
		System.out.println("write to file " + file.getAbsolutePath());
			
		FileChannel fc = new RandomAccessFile(file, "rw").getChannel();

		Assert.assertEquals(200, response.getStatus());
		int length = response.getContentLength();
		response.getBody().transferTo(fc, length);
		fc.close();
		
	
		
		LineNumberReader lnr = new LineNumberReader(new FileReader(file));
		int countLines = 0;
		String lastLine = null;
		String line = null;
		
		do {
			line = lnr.readLine();
			if (line != null) {
				lastLine = line;
				countLines++;
			}
		} while(line != null); 

		Assert.assertEquals("<!--  End SiteCatalyst code  --></div></body></html>", lastLine);
		
		file.delete();
		con.close();
	}
	
	
	private static final class ContentEchoHandler implements IHttpRequestHandler {

		public void onRequest(IHttpExchange exchange) throws IOException {

			IHttpRequest request = exchange.getRequest();
			NonBlockingBodyDataSource bodyChannel = request.getNonBlockingBody();
			exchange.send(new HttpResponse(200, request.getContentType(), bodyChannel));			
		}
		
	}
}
