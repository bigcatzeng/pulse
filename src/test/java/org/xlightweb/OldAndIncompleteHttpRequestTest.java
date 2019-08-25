/*
 *  Copyright (c) xsocket.org, 2006 - 2009. All rights reserved.
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
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IServer;




/**
*
* @author grro@xlightweb.org
*/
public final class OldAndIncompleteHttpRequestTest {

 
	@Test 
	public void testIncompleteHttp1_1Request() throws Exception {
		System.out.println("testIncompleteHttp1_1Request");
		
		IServer server = new HttpServer(new RequestHandler());
		server.start();

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET / HTTP/1.1\r\n" +
		          "\r\n");
		
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		System.out.println("header received");
		
		int contentLength = QAUtil.readContentLength(header);
			
		String body = con.readStringByLength(contentLength);

		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);
		
		
		server.close();
	}
	
	
	@Test 
	public void testHttp1_0Request() throws Exception {
		System.out.println("testHttp1_0Request");
		
		IServer server = new HttpServer(new RequestHandler());
		server.start();

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET / HTTP/1.0\r\n" +
				  "\r\n");
		
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		System.out.println("header received");
		int contentLength = QAUtil.readContentLength(header);
			
		String body = con.readStringByLength(contentLength);

		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);
		
		
		server.close();
	}
	


	
	private static final class RequestHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			exchange.send(new HttpResponse(200, "text/plain", "OK"));
		}
		
	}
}
