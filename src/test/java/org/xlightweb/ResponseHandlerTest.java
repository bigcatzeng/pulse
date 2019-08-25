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


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.connection.IServer;
import org.xsocket.connection.ConnectionUtils;


/**
*
* @author grro@xlightweb.org
*/
public final class ResponseHandlerTest  {

	public static void main(String[] args) throws Exception {
		
		for (int i = 0; i < 10000; i++) {
			new ResponseHandlerTest().testNonThreaded();
			System.out.println(".");
		}
	}


	@Test
	public void testMultiThreaded() throws Exception {
		IServer server = new HttpServer(new HeaderInfoServerHandler());
		ConnectionUtils.start(server);

		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		ThreadedResponseHandler tHandler = new ThreadedResponseHandler();
		con.send(new GetRequest("/"), tHandler);
		
		QAUtil.sleep(1000);
		Assert.assertTrue(tHandler.getResponse().getStatus() == 200);
		Assert.assertFalse(tHandler.getThreadname().startsWith("xDispatcher"));
		
		
		con.close();
		server.close();
	}

	
	@Test
	public void testNonThreaded() throws Exception {
		IServer server = new HttpServer(new HeaderInfoServerHandler());
		ConnectionUtils.start(server);

		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		NonThreadedResponseHandler tHandler = new NonThreadedResponseHandler();
		con.send(new GetRequest("/"), tHandler);
		
		QAUtil.sleep(1000);
		Assert.assertTrue(tHandler.getResponse().getStatus() == 200);
		Assert.assertTrue(tHandler.getThreadname().startsWith("xDispatcher"));
		
		
		con.close();
		server.close();
	}

	
	
	private static final class ThreadedResponseHandler implements IHttpResponseHandler {
		
		private String threadname = null;
		private IHttpResponse response = null;
		
		
		public void onResponse(IHttpResponse response) throws IOException {
			threadname = Thread.currentThread().getName();
			this.response = response;
		}
		
		public void onException(IOException ioe) {
		}		
		
		String getThreadname() {
			return threadname;
		}
		
		IHttpResponse getResponse() {
			return response;
		}
	}

	

	@Execution(Execution.NONTHREADED)
	private static final class NonThreadedResponseHandler implements IHttpResponseHandler {
		
		private String threadname = null;
		private IHttpResponse response = null;
		
		
		public void onResponse(IHttpResponse response) throws IOException {
			threadname = Thread.currentThread().getName();
			this.response = response;
		}
		
		public void onException(IOException ioe) {
		}

		String getThreadname() {
			return threadname;
		}
		
		IHttpResponse getResponse() {
			return response;
		}
	}
}