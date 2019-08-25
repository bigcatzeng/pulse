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

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IServer;
import org.xsocket.connection.ConnectionUtils;



/**
*
* @author grro@xlightweb.org
*/
public final class CompatibilityWriteableByteChannelTest {
	

	@Test 
	public void testNonBlockingWriteClientClose() throws Exception {
		
		RequestHandler reqHdl = new RequestHandler();
		HttpServer server = new HttpServer(reqHdl);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		
		IHttpResponseHandler respHdl = new IHttpResponseHandler() {
			public void onResponse(IHttpResponse response) throws IOException {
			}
			
			public void onException(IOException ioe) {
			}
		};
		
		BodyDataSink clientChannel = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/"), respHdl);
		clientChannel.flush();
		
		BodyCloseListener closeListener = new BodyCloseListener();
		clientChannel.addCloseListener(closeListener);
		QAUtil.sleep(1000);
		
		NonBlockingBodyDataSource serverChannel = reqHdl.getDataSource();
		

		ByteBuffer buffer = QAUtil.generateByteBuffer(40);
		int written = clientChannel.write(buffer);
		Assert.assertEquals(40, written);
		
		QAUtil.sleep(1000);
		Assert.assertEquals(40, serverChannel.available());
		
		clientChannel.close();
		QAUtil.sleep(1000);
		Assert.assertEquals(1, closeListener.getCountOnCloseCalled());

		buffer = QAUtil.generateByteBuffer(40);
		try {
			written = clientChannel.write(buffer);
			Assert.fail("ClosedChannelException expected");
		} catch (ClosedChannelException expected) { }
		
		server.close();
	}
	

	@Test 
	public void testNonBlockingWriteServerConnectionClose() throws Exception {
		
		
		RequestHandler reqHdl = new RequestHandler();
		IServer server = new HttpServer(reqHdl);
		ConnectionUtils.start(server);
		
		HttpClient httpClient = new HttpClient();
		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		BodyDataSink clientChannel = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/"), respHdl);
		clientChannel.flush();
		
		BodyCloseListener closeListener = new BodyCloseListener();
		clientChannel.addCloseListener(closeListener);
		QAUtil.sleep(1000);
		
		NonBlockingBodyDataSource serverChannel = reqHdl.getDataSource();
		

		ByteBuffer buffer = QAUtil.generateByteBuffer(40);
		int written = clientChannel.write(buffer);
		Assert.assertEquals(40, written);
		
		QAUtil.sleep(1000);
		Assert.assertEquals(40, serverChannel.available());
		
		server.close();
		
		QAUtil.sleep(1000);
		
		buffer = QAUtil.generateByteBuffer(40);
		try {
			written = clientChannel.write(buffer);
			Assert.fail("ClosedChannelException expected");
		} catch (ClosedChannelException expected) { }
		
		server.close();
	}
	

	private static final class BodyCloseListener implements IBodyCloseListener {

		private int countOnCloseCalled = 0;
		
		public void onClose() throws IOException {
			countOnCloseCalled++;
		}
		
		int getCountOnCloseCalled() {
			return countOnCloseCalled;
		}
	}
	
		
	
	private static final class RequestHandler implements IHttpRequestHandler {

		private NonBlockingBodyDataSource dataSource = null;
		
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			dataSource = exchange.getRequest().getNonBlockingBody();
		}
		

		NonBlockingBodyDataSource getDataSource() {
			return dataSource;
		}
	}
}
