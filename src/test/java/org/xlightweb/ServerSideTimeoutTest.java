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
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;




import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.HttpResponse;
import org.xlightweb.IBodyDataHandler;
import org.xlightweb.IHttpConnectHandler;
import org.xlightweb.IHttpConnection;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.NonBlockingBodyDataSource;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IBlockingConnection;





/**
*
* @author grro@xlightweb.org
*/
public final class ServerSideTimeoutTest  {


	@Test
	public void testIdleTimeout() throws Exception {

		ServerHandler srvHdl = new ServerHandler(true);
		HttpServer server = new HttpServer(srvHdl);
		server.addConnectionHandler(srvHdl);
		
		server.setIdleTimeoutMillis(500);
		server.start();

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		QAUtil.sleep(200);
	
		Assert.assertTrue("connection should be open", srvHdl.getConnection().isOpen());
		QAUtil.sleep(1500);
		
		Assert.assertFalse("connection should be closed", srvHdl.getConnection().isOpen());
	
		
		con.close();
		server.close();
	}
	
	
	@Test
	public void testRequestTimeout() throws Exception {

		ServerHandler srvHdl = new ServerHandler(true);
		HttpServer server = new HttpServer(srvHdl);
		server.addConnectionHandler(srvHdl);
		server.setRequestTimeoutMillis(1000);
		server.start();
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		QAUtil.sleep(200);
	
		Assert.assertTrue(srvHdl.getConnection().isOpen());
		QAUtil.sleep(1300);
		
		Assert.assertEquals(1, srvHdl.getCountRequestTimeout());
		Assert.assertFalse(srvHdl.getConnection().isOpen());
	
		
		con.close();
		server.close();
	}
	
	
	@Test
	public void testRequestTimeout2() throws Exception {

		ServerHandler srvHdl = new ServerHandler(false);
		HttpServer server = new HttpServer(srvHdl);
		server.addConnectionHandler(srvHdl);
		server.setRequestTimeoutMillis(1000);
		server.start();
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		QAUtil.sleep(200);
	
		Assert.assertTrue(srvHdl.getConnection().isOpen());
		QAUtil.sleep(1300);
		
		Assert.assertEquals(1, srvHdl.getCountRequestTimeout());
		Assert.assertTrue(srvHdl.getConnection().isOpen());
	
		
		con.close();
		server.close();
	}
	
	

	
	@Test
	public void testConnectionTimeout() throws Exception {
	
		ServerHandler srvHdl = new ServerHandler(true);
		HttpServer server = new HttpServer(srvHdl);
		server.setConnectionTimeoutMillis(1000);
		server.addConnectionHandler(srvHdl);
		server.start();
		
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
	
		QAUtil.sleep(1500);
		
		try {
			con.write("POST / HTTP/1.0\r\n" +
					  "Content-Length: 10\r\n" +
					  "\r\n" +
					  "12345767890");
			
			Assert.fail("ClosedChannelException expected");
		} catch (ClosedChannelException expected) { }
		
	}
	
	
	@Test
	public void testBodyDataReceiveTimeout() throws Exception {
	
		ServerHandler srvHdl = new ServerHandler(true);
		HttpServer server = new HttpServer(srvHdl);
		server.setBodyDataReceiveTimeoutMillis(1000);
		ConnectionUtils.start(server);
		
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("POST / HTTP/1.0\r\n" +
				  "Content-Length: 1000\r\n" +
				  "\r\n");
		
		con.write("12345");
		QAUtil.sleep(500);
		Assert.assertNull(srvHdl.getException());
		
		con.write("67890");
		QAUtil.sleep(1500);
		
		Assert.assertFalse(srvHdl.isMessageCompeleteReceived());
		Assert.assertNotNull(srvHdl.getException());
		
		server.close();
	}





	@Execution(Execution.NONTHREADED)
	private static final class ServerHandler implements IHttpConnectHandler, IHttpRequestHandler, IHttpRequestTimeoutHandler {
		
		private IHttpConnection connection = null;
		private int countRequestTimeout = 0;
		
		private boolean isCloseOnTimeout = false;
		
		private IOException ioe = null;
		private boolean messageCompleteReceived = false;
		
		public ServerHandler(boolean isCloseOnTimeout) {
			this.isCloseOnTimeout = isCloseOnTimeout;
		}
		
		public boolean onConnect(IHttpConnection httpConnection) throws IOException {
			this.connection = httpConnection;
			return true;
		}
		
		public void onRequest(final IHttpExchange exchange) throws IOException {
			
			IBodyDataHandler bodyDataHandler = new IBodyDataHandler() {
				
				public boolean onData(NonBlockingBodyDataSource bodyDataSource) throws BufferUnderflowException {

					try {
						int available = bodyDataSource.available();
						if (available > 0) {
							bodyDataSource.readByteBufferByLength(available);
						}
						
						if (available == -1) {
							messageCompleteReceived = true;
							exchange.send(new HttpResponse(200, "text/plain", "OK"));
						}
					} catch (IOException e) {
						ioe = e;
					}
					
					return true;
				}
			};

			exchange.getRequest().getNonBlockingBody().setDataHandler(bodyDataHandler);
		}
		
		public boolean onRequestTimeout(IHttpConnection connection) throws IOException {
			countRequestTimeout++;
			return !isCloseOnTimeout;
		}
		
		IHttpConnection getConnection() {
			return connection;
		}
		
		int getCountRequestTimeout() {
			return countRequestTimeout;
		}
		
		boolean isMessageCompeleteReceived() {
			return messageCompleteReceived;
		}
		
		IOException getException() {
			return ioe;
		}
	}
}