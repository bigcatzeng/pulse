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

import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.ConnectionUtils;



/**
*
* @author grro@xlightweb.org
*/
public final class ExecutionModeTest {


	@Test
	public void testNonThreaded() throws Exception {

		NonThreadedServerHandler hdl = new NonThreadedServerHandler();
		IServer server = new HttpServer(hdl);
		ConnectionUtils.start(server);


		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		
		con.write("GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
			
		con.readByteBufferByLength(contentLength);
		con.close();
		
		server.close();

		Assert.assertTrue(hdl.getThreadName().startsWith("xDispatcher"));
	}



	@Test
	public void testMultithreaded() throws Exception {

		MultiThreadedServerHandler hdl = new MultiThreadedServerHandler();
		IServer server = new HttpServer(hdl);
		ConnectionUtils.start(server);

		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
	
		con.write("GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		
		con.readByteBufferByLength(contentLength);
		con.close();
		
		server.close();

		Assert.assertFalse(hdl.getThreadName().startsWith("xDispatcher"));
	}




	private static final class NonThreadedServerHandler implements IHttpRequestHandler {

		private String theadname = null;

		@Execution(Execution.NONTHREADED)
		public void onRequest(IHttpExchange exchange) throws IOException {
			theadname = Thread.currentThread().getName();

			exchange.send(new HttpResponse(200, "text/plain", Integer.toString(this.hashCode())));
		}

		public String getThreadName() {
			return theadname;
		}
	}


	private static final class MultiThreadedServerHandler implements IHttpRequestHandler {

		private String theadname = null;

		public void onRequest(IHttpExchange exchange) throws IOException {
			theadname = Thread.currentThread().getName();

			exchange.send(new HttpResponse(200, "text/plain", Integer.toString(this.hashCode())));
		}

		public String getThreadName() {
			return theadname;
		}
	}
}
