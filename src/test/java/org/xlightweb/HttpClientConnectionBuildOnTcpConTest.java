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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;

import org.junit.Test;
import org.xlightweb.client.HttpClientConnection;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.NonBlockingConnectionPool;


/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientConnectionBuildOnTcpConTest {

	
	@Test
	public void testLive() throws Exception {
		
		NonBlockingConnectionPool pool = new NonBlockingConnectionPool();
		
		for (int i = 0; i < 10; i++) {
			ConnectHandler ch = new ConnectHandler();
			
			long start = System.nanoTime();
			
			INonBlockingConnection nbc = pool.getNonBlockingConnection("www.gmx.com", 80, ch);
			// perform ch.isConnected() later. It will be called multithreaded 
			
			HttpClientConnection con = new HttpClientConnection(nbc);
			
			IHttpRequest request = new GetRequest("http://www.gmx.com/doesNotExist.html");
			
			ResponseHandler respHdl = new ResponseHandler();
			con.send(request, respHdl);
	
			while (respHdl.getResponse() == null) {
				QAUtil.sleep(100);
			}
			
			respHdl.getResponse().getBody().readString();
			nbc.close();
	
			double elapsedMillis = ((double) (respHdl.getTime() - start)) / 1000000;
			System.out.println(elapsedMillis + " millis");
			
			Assert.assertTrue(elapsedMillis < 2000);
		}
		
		System.out.println(pool);
	}
	
	
	private static final class ResponseHandler implements IHttpResponseHandler {
		
		private final AtomicLong time = new AtomicLong();
		private final AtomicReference<IHttpResponse> responseRef = new AtomicReference<IHttpResponse>();
		
		
		public void onResponse(IHttpResponse response) throws IOException {
			time.set(System.nanoTime());
			responseRef.set(response);
		}
		
		public void onException(IOException ioe) throws IOException {
			
		}
		
		public IHttpResponse getResponse() {
			return responseRef.get();
		}
		
		public long getTime() {
			return time.get();
		}
	}
	
	
	private static final class ConnectHandler implements IConnectHandler {
		
		private final AtomicBoolean isConnected = new AtomicBoolean(false);
		
		public boolean onConnect(INonBlockingConnection connection) throws IOException {
			isConnected.set(true);
			return true;
		}
		
		
		public boolean isConnected() {
			return isConnected.get();
		}
	}
	
	
}
