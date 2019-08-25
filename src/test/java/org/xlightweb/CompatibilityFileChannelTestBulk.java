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
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IServer;




/**
*
* @author grro@xlightweb.org
*/
public final class CompatibilityFileChannelTestBulk {
	
	
	public static void main(String[] args) throws Exception {
		
		for (int i = 0; i < 1000; i++) {
			new CompatibilityFileChannelTestBulk().testTransferTo();
		}
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
	public void testTransferToBulk() throws Exception {
		
		for (int i = 0; i < 60; i++) {
			testTransferTo();
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
