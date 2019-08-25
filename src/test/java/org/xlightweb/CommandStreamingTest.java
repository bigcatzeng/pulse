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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileChannel;

import org.junit.Test;

import org.xlightweb.BodyDataSink;
import org.xlightweb.FutureResponseHandler;
import org.xlightweb.HttpRequestHeader;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;




/**
*
* @author grro@xlightweb.org
*/
public final class CommandStreamingTest  {

 
	@Test
	public void testStreaming() throws Exception {
		
		HttpServer server = new HttpServer(new RequestHandler());
		server.start();
	
		HttpClient httpClient = new HttpClient();
		
		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		BodyDataSink outChannel = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/conversation"), respHdl);
		outChannel.flush();
		
		BodyDataSource inChannel = respHdl.getResponse().getBody();
		inChannel.readStringByDelimiter("\r\n"); // read greeting
		
		
		// send first request 
		File file = QAUtil.createTestfile_40k();
		outChannel.write("GET FILE: " + file.getAbsolutePath() + "\r\n");
		int length = inChannel.readInt();
		
		String s = inChannel.readStringByLength(length);
		QAUtil.isEquals(file, s);

		
		// send second request
		File file2 = QAUtil.createTestfile_400k();
		outChannel.write("GET FILE: " + file2.getAbsolutePath() + "\r\n");
		length = inChannel.readInt();
		
		s = inChannel.readStringByLength(length);
		QAUtil.isEquals(file2, s);


		file.delete();
		file2.delete();
		
		httpClient.close();
		server.close();
	}

	
	

	
	private static final class RequestHandler implements IHttpRequestHandler {

		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {

			IHttpRequest request = exchange.getRequest(); 
			
			if (!request.getRequestURI().equalsIgnoreCase("/conversation")) {
				exchange.sendError(400);
				return;
			}	

			

			// write response header and get out channel
			BodyDataSink outChannel = exchange.send(new HttpResponseHeader(200));

			// get set handler for the input channel
			request.getNonBlockingBody().setDataHandler(new CommandHandler(outChannel));
			
			outChannel.write("ready\r\n");
		}
	}	
	
	
	
	private static final class CommandHandler implements IBodyDataHandler {
		
		private BodyDataSink outChannel;
		
		
		public CommandHandler(BodyDataSink outChannel) {
			this.outChannel = outChannel;
		}
		
	
		public boolean onData(NonBlockingBodyDataSource bodyDataSource) throws BufferUnderflowException {
			try {
				
				String s = bodyDataSource.readStringByDelimiter("\r\n");
				if (s.startsWith("GET FILE:")) {
					String filename = s.substring("GET FILE:".length(), s.length()).trim();
					RandomAccessFile file = new RandomAccessFile(filename, "r");
					
					outChannel.write((int) file.length());
					
					FileChannel fc = file.getChannel();
					outChannel.transferFrom(fc);
					fc.close();
					file.close();
					
					
					
				} else {
					//...
				}
			
			} catch (IOException ioe) {
				outChannel.destroy();
			}
			
			return true;
		}	
	}
	
	
}