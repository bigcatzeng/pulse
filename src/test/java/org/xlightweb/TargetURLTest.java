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

import org.xlightweb.GetRequest;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
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
public final class TargetURLTest  {




	@Test
	public void testSimple() throws Exception {
		IServer server = new HttpServer(new ServerHandler());
		ConnectionUtils.start(server);
		
		GetRequest req = new GetRequest("http://localhost:" + server.getLocalPort() + "/test");
		req.setHeader("Host", "localhost");
		req.setHeader("User-Agent", "me");

		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write(req.toString());
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
			
		String body = con.readStringByLength(contentLength);

		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertTrue(body.indexOf("requestURI: /test") != -1);
		Assert.assertTrue(body.indexOf("queryString: null") != -1);
		
	
		con.close();
		server.close();
	}


	@Test
	public void testWithQueryString() throws Exception {
		
		IServer server = new HttpServer(new ServerHandler());
		ConnectionUtils.start(server);
		
		GetRequest req = new GetRequest("http://localhost:" + server.getLocalPort() + "/test?param=value");
		req.setHeader("Host", "localhost");
		req.setHeader("User-Agent", "me");

		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write(req.toString());
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
			
		String body = con.readStringByLength(contentLength);

		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertTrue(body.indexOf("requestURI: /test") != -1);
		Assert.assertTrue(body.indexOf("queryString: param=value") != -1);
		
	
		con.close();
		server.close();
	}

	
	
	private static final class ServerHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			StringBuilder sb = new StringBuilder();
			
			IHttpRequest request = exchange.getRequest();
			sb.append("requestURI: " + request.getRequestURI() + "\r\n");
			sb.append("queryString: " + request.getQueryString() + "\r\n");

			exchange.send(new HttpResponse(200, "text/plain", sb.toString()));
		}
	}

}