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
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IServer;





/**
*
* @author grro@xlightweb.org
*/
public final class UrlFormEncodedParameterTest  {


	@Test
	public void testSimple() throws Exception {

		IServer server = new HttpServer(new ServerHandler());
		server.start();

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		
		con.write("POST /?message=%5B%7B%22version%22%3A%221.0%22%2C%22minimumVersion%22%3A%220.9%22%2C%22channel%22%3A%22%2Fmeta%2Fhandshake%22%2C%22id%22%3A%220%22%2C%22supportedConnectionTypes%22%3A%5B%22long-polling%22%2C%22callback-polling%22%5D%7D%5D HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "Content-Type: application/x-www-form-urlencoded\r\n" +
				  "Content-Length: 5\r\n" +
				  "\r\n" +
				  "12345");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
			
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(body.indexOf("message=") != -1);
		
		con.close();
		server.close();
	}
	
	
	@Test
	public void testSimpleWithEncoding() throws Exception {

		IServer server = new HttpServer(new ServerHandler());
		server.start();

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		
		con.write("POST /?message=%5B%7B%22version%22%3A%221.0%22%2C%22minimumVersion%22%3A%220.9%22%2C%22channel%22%3A%22%2Fmeta%2Fhandshake%22%2C%22id%22%3A%220%22%2C%22supportedConnectionTypes%22%3A%5B%22long-polling%22%2C%22callback-polling%22%5D%7D%5D HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "Content-Type: application/x-www-form-urlencoded; charset=UTF-8\r\n" +
				  "Content-Length: 5\r\n" +
				  "\r\n" +
				  "12345");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
			
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(body.indexOf("message=") != -1);
		
		con.close();
		
		server.close();
	}



	@Execution(Execution.NONTHREADED)
	private static final class ServerHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			IHttpRequest request = exchange.getRequest();
			
			StringBuilder sb = new StringBuilder();
			for (String paramName : request.getParameterNameSet()) {
				sb.append(paramName + "=" + request.getParameter(paramName) + "\r\n");
			}
			
			exchange.send(new HttpResponse(200, "text/plain", sb.toString()));
		}
	}

}