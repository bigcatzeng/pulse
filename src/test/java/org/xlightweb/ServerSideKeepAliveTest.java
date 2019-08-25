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

import java.nio.channels.ClosedChannelException;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.server.HttpServer;
import org.xlightweb.server.HttpServerConnection;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.ConnectionUtils;




/**
*
* @author grro@xlightweb.org
*/
public final class ServerSideKeepAliveTest  {



	@Test
	public void testMaxTransactions() throws Exception {
	    System.out.println("testMaxTransactions");
		
		IServer server = new HttpServer(new TransactionServerHandler());
		ConnectionUtils.start(server);

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		for (int i = 5; i > 0; --i) {
		
			con.write("GET / HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: me\r\n" +
					  "\r\n");
			
			String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			int contentLength = QAUtil.readContentLength(header);
			
			con.readByteBufferByLength(contentLength);

			if (i == 1) {
				Assert.assertTrue(header.indexOf("Connection: close") != -1);
				
				QAUtil.sleep(400);
				
				try {
					con.readByte();
					Assert.fail("ClosedChannelException expected");
				} catch (ClosedChannelException expected) { }

				
			} else {
				int start = header.indexOf("Keep-Alive: max=");
				int end = header.indexOf("\r\n", start + 1);
				int count = Integer.parseInt(header.substring(start + "Keep-Alive: max=".length(), end));
				Assert.assertEquals(i - 1, count);
				Assert.assertTrue(con.isOpen());
			}
		}

		con.close();
		server.close();
	}
	
	
	@Test
	public void testMaxTransactionsContainerManager() throws Exception {
	    System.out.println("testMaxTransactionsContainerManager");
	    
		HttpServer server = new HttpServer(new ServerHandler());
		server.setMaxTransactions(5);
		ConnectionUtils.start(server);
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		for (int i = 5; i > 0; --i) {
		
			con.write("GET / HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: me\r\n" +
					  "\r\n");
			
			String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			int contentLength = QAUtil.readContentLength(header);
			
			con.readByteBufferByLength(contentLength);

			if (i == 1) {
				Assert.assertTrue(header.indexOf("Connection: close") != -1);
				
				QAUtil.sleep(400);
				
				try {
					con.readByte();
					Assert.fail("ClosedChannelException expected");
				} catch (ClosedChannelException expected) { }

				
			} else {
				int start = header.indexOf("Keep-Alive: max=");
				int end = header.indexOf("\r\n", start + 1);
				int count = Integer.parseInt(header.substring(start + "Keep-Alive: max=".length(), end));
				Assert.assertEquals(i - 1, count);
				Assert.assertTrue(con.isOpen());
			}
		}

		con.close();
		server.close();
	}

	
	@Test
	public void testClientSideCloseHeader() throws Exception {
	    System.out.println("testClientSideCloseHeader");
		
		
		IServer server = new HttpServer(new TransactionServerHandler());
		ConnectionUtils.start(server);

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		
		con.write("GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "Connection: close\r\n" + 
				  "\r\n");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
			
		con.readByteBufferByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("Connection: close") != -1); 


		con.close();
		server.close();
	}
	
	

	@Test
	public void testRequestTimeout() throws Exception {
	    System.out.println("testRequestTimeout");
	    
		IServer server = new HttpServer(new RequestTimeoutServerHandler());
		server.start();
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		for (int i = 0; i < 3; i++) {
			QAUtil.sleep(200);
			con.write("GET / HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: me\r\n" +
					  "\r\n");
			
			String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			int contentLength = QAUtil.readContentLength(header);
			
			con.readByteBufferByLength(contentLength);

			Assert.assertTrue(header.indexOf("200") != -1);
		}
		
		QAUtil.sleep(1500);

		try {
			con.readByte();
			Assert.fail("ClosedChannelException expected");
		} catch (ClosedChannelException expected) { }

		server.close();	
	}
	 
	
	
	@Test
	public void testHttp1_0_Keep_Alive() throws Exception {
	    System.out.println("testHttp1_0_Keep_Alive");
	    
		IServer server = new HttpServer(new ServerHandler());
		ConnectionUtils.start(server);
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		QAUtil.sleep(200);
		con.write("GET / HTTP/1.0\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "Connection: Keep-Alive\r\n" +
				  "\r\n");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("HTTP/1.0") != -1);
		Assert.assertTrue(header.indexOf("Connection: close") != -1);  // xSocket will ignore keep-alive if HTTP/1.0

		Assert.assertTrue(header.indexOf("200") != -1);

		server.close();	
	}
		

	

	

	
	
	private static final class TransactionServerHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			((HttpServerConnection) exchange.getConnection()).setMaxTransactions(5);
			exchange.send(new HttpResponse(200, "text/plain", "OK"));
		}
	}

	private static final class RequestTimeoutServerHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			((HttpServerConnection) exchange.getConnection()).setRequestTimeoutMillis(1000);
			exchange.send(new HttpResponse(200, "text/plain", "OK"));
		}
	}


	private static final class ServerHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			exchange.send(new HttpResponse(200, "text/plain", "OK"));
		}
	}

}