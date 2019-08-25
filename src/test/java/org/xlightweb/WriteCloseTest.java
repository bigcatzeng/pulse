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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IServer;





/**
*
* @author grro@xlightweb.org
*/
public final class WriteCloseTest  {


	@Test
	public void testClientInitiatedClose() throws Exception {
	    
	    
		HttpServer server = new HttpServer(new HeaderInfoServerHandler());
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());

		IHttpResponseHandler respHdl = new IHttpResponseHandler() {
			public void onResponse(IHttpResponse response) throws IOException {
			}
			
			public void onException(IOException ioe) {
			}
		};
		
		BodyDataSink outputBodyChannel = con.send(new HttpRequestHeader("GET", "/"), respHdl);
		
		BodyCloseListener cl = new BodyCloseListener();
		outputBodyChannel.addCloseListener(cl);
		
		outputBodyChannel.close();
		
		QAUtil.sleep(200);

		
		Assert.assertEquals(1, cl.getCountOnClosedCalled());

		con.close();
		server.close();
	}
	
	
	
	

	@Test
	public void testSingleThreadedClose() throws Exception {
		System.out.println("testSingleThreadedClose");
		
		Thread.currentThread().setName("test");
		
		IServer server = new HttpServer(new HeaderInfoServerHandler());
		ConnectionUtils.start(server);
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());

		IHttpResponseHandler respHdl = new IHttpResponseHandler() {
			public void onResponse(IHttpResponse response) throws IOException {
			}
			
			public void onException(IOException ioe) {
			}
		};
		
		BodyDataSink outputBodyChannel = con.send(new HttpRequestHeader("GET", "/"), respHdl);
		
		NonThreadedBodyCloseListener cl = new NonThreadedBodyCloseListener();
		outputBodyChannel.addCloseListener(cl);
		
		outputBodyChannel.close();

		
		QAUtil.sleep(500);
		
		if (cl.getCountOnClosedCalled() != 1) {
			System.out.println("onClosed hasn't been called");
			Assert.fail();
		}
		
		

		con.close();
		server.close();
	}
	
	
	
	private static final class BodyCloseListener implements IBodyCloseListener {
		
		private final AtomicInteger countOnClosedCalled = new AtomicInteger(0);
		private final AtomicReference<String> threadnameRef = new AtomicReference<String>();
		
		
		public void onClose() throws IOException {
			countOnClosedCalled.incrementAndGet();
			threadnameRef.set(Thread.currentThread().getName());
		}		
		
		int getCountOnClosedCalled() {
			return countOnClosedCalled.get();
		}
	}	
	
	
	private static final class NonThreadedBodyCloseListener implements IBodyCloseListener {
		
		private final AtomicInteger countOnClosedCalled = new AtomicInteger();
		
		@Execution(Execution.NONTHREADED)
		public void onClose() throws IOException {
			countOnClosedCalled.incrementAndGet();
		}		
		
		int getCountOnClosedCalled() {
			return countOnClosedCalled.get();
		}
	}	
}