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

import org.xlightweb.server.HttpServer;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;




/**
*
* @author grro@xlightweb.org
*/
public final class AutoChosseResponseBodyTypeTest  {

	
	
	@Test
	public void testHttp1_1() throws Exception {
		System.out.println("testHttp1_1");
		
		HttpServer server = new HttpServer(new RequestHandler());
		server.start();

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET /test HTTP/1.1\r\n");
		con.write("Host: localhost:" +  server.getLocalPort() + "\r\n");
		con.write("User-Agent:me\r\n");
		con.write("\r\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n");
		Assert.assertTrue(header.indexOf("OK") != -1);
		Assert.assertTrue(header.indexOf("chunked") != -1);
		
		con.close();
		server.close();
	}
	
	@Test
	public void testHttp1_0() throws Exception {
		System.out.println("testHttp1_0");
				
		HttpServer server = new HttpServer(new RequestHandler());
		server.start();

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET /test HTTP/1.0\r\n");
		con.write("\r\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n");
		Assert.assertTrue(header.indexOf("OK") != -1);
		Assert.assertEquals("test123456", con.readStringByLength(10));
		
		try {
			con.readByte();
			Assert.fail("ClosedChannelException expected");
		} catch (ClosedChannelException expected) { }
		
		con.close();
		server.close();
	}
	
	
	
	private static final class RequestHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			
			BodyDataSink bodyDataSink = exchange.send(new HttpResponseHeader(200, "text/plain"));
			bodyDataSink.write("test");
			QAUtil.sleep(200);

			bodyDataSink.write("123");
			QAUtil.sleep(200);

			bodyDataSink.write("456");
			bodyDataSink.close();
		}	
	}
}