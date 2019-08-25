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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.BodyDataSink;
import org.xlightweb.HttpResponse;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.ConnectionUtils;



/**
*
* @author grro@xlightweb.org
*/
public final class CloseAfterWrittenTest  {

   

	@Test
	public void testMessage() throws Exception {
		
		
		IServer server = new HttpServer(new MessageResponder());
		server.start();

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("GET / HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: me\r\n" +
					  "\r\n");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
			
		con.readByteBufferByLength(contentLength);

		Assert.assertTrue(header.indexOf("Connection: close") != -1);
				
		QAUtil.sleep(400);
			
		try {
			con.readByte();
			Assert.fail("ClosedChannelException expected");
		} catch (ClosedChannelException expected) { }



		server.close();		
	}
	
	
	


	@Test
	public void testHeaderBody() throws Exception {
		
		
		IServer server = new HttpServer(new HeaderBodyResponder());
		ConnectionUtils.start(server);

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("GET / HTTP/1.1\r\n" +
					  "Host: localhost\r\n" +
					  "User-Agent: me\r\n" +
					  "\r\n");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
			
		con.readByteBufferByLength(contentLength);

		Assert.assertTrue(header.indexOf("Connection: close") != -1);
				
		QAUtil.sleep(400);
			
		try {
			con.readByte();
			Assert.fail("ClosedChannelException expected");
		} catch (ClosedChannelException expected) { }



		server.close();		
	}
	
	
	private static final class MessageResponder implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			HttpResponse response = new HttpResponse(200, "text/plain", "OK");
			response.setHeader("Connection", "close");
			
			exchange.send(response);
		}
	}
	
	private static final class HeaderBodyResponder implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			HttpResponseHeader header = new HttpResponseHeader(200, "text/plain", "OK");
			header.setHeader("Connection", "close");
			
			BodyDataSink bodyDataSink = exchange.send(header, 2);
			bodyDataSink.write("OK");
			bodyDataSink.close();
		}
	}
}