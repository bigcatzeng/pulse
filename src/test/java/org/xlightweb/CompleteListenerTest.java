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
public final class CompleteListenerTest  {


	@Test
	public void testClientMutlithreaded() throws Exception {

		Thread.currentThread().setName("testThread");
		
		HttpServer server = new HttpServer(new HeaderInfoServerHandler());
		server.start();

		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		IHttpResponse response = con.call(new GetRequest("/"));

		MultithreadedCompleteListener cl = new MultithreadedCompleteListener();
		response.getNonBlockingBody().addCompleteListener(cl);
		
		QAUtil.sleep(300);
		
		Assert.assertEquals(1, cl.getCountCalled());
		Assert.assertFalse(cl.getThreadname().startsWith("xDispatcher") || cl.getThreadname().startsWith("testThread"));
		

		con.close();
		server.close();
	}

	
	@Test
	public void testClientNonthreaded() throws Exception {

		Thread.currentThread().setName("testThread");
		
		IServer server = new HttpServer(new HeaderInfoServerHandler());
		ConnectionUtils.start(server);

		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		IHttpResponse response = con.call(new GetRequest("/"));

		NonthreadedCompleteListener cl = new NonthreadedCompleteListener();
		response.getNonBlockingBody().addCompleteListener(cl);
		
		QAUtil.sleep(2000);
		
		Assert.assertEquals(1, cl.getCountCalled());
		Assert.assertTrue(cl.getThreadname().startsWith("xDispatcher") || cl.getThreadname().startsWith("testThread"));
		

		con.close();
		server.close();
	}
	
	
	
	private static final class MultithreadedCompleteListener implements IBodyCompleteListener {
		
		private int countCalled = 0;
		private String threadname = null;
		
		public void onComplete() throws IOException {
			countCalled++;
			threadname = Thread.currentThread().getName();
		}
		
		int getCountCalled() {
			return countCalled;
		}
		
		String getThreadname() {
			return threadname;
		}
	}
	
	
	@Execution(Execution.NONTHREADED)
	private static final class NonthreadedCompleteListener implements IBodyCompleteListener {
		
		private int countCalled = 0;
		private String threadname = null;
		
		
		@Execution(Execution.NONTHREADED)
		public void onComplete() throws IOException {
			countCalled++;
			threadname = Thread.currentThread().getName();			
		}
		
		int getCountCalled() {
			return countCalled;
		}
		
		String getThreadname() {
			return threadname;
		}
	}
}