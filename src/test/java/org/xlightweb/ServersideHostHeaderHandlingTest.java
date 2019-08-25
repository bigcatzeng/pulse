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
import org.xsocket.connection.ConnectionUtils;




/**
*
* @author grro@xlightweb.org
*/
public final class ServersideHostHeaderHandlingTest {

	
	@Test 
	public void testWithoutPort() throws Exception {

		IServer server = new HttpServer(new ServerHandler());
		ConnectionUtils.start(server);
		
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("GET / HTTP/1.1\r\n" +
				  "Host: testserver\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		
		String body = con.readStringByLength(contentLength);
			
		Assert.assertEquals("false testserver:80 / ", body);
	}

	
	@Test 
	public void testWithPort() throws Exception {

		IServer server = new HttpServer(new ServerHandler());
		ConnectionUtils.start(server);
		
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("GET / HTTP/1.1\r\n" +
				  "Host: testserver:9955\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		
		String body = con.readStringByLength(contentLength);
			
		Assert.assertEquals("false testserver:9955 / ", body);
	}
	
	
	
	@Test 
	public void testWithQuery() throws Exception {

		IServer server = new HttpServer(new ServerHandler());
		ConnectionUtils.start(server);
		
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("GET /test?param1=value1&param2=value2 HTTP/1.1\r\n" +
				  "Host: testserver:9955\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		
		String body = con.readStringByLength(contentLength);
			
		Assert.assertEquals("false testserver:9955 /test param1=value1&param2=value2", body);
	}
	
	
	@Test 
	public void testSSL() throws Exception {

		IServer server = new HttpServer(0, new ServerHandler(), SSLTestContextFactory.getSSLContext(), true);
		ConnectionUtils.start(server);
		
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort(), SSLTestContextFactory.getSSLContext(), true);

		con.write("GET / HTTP/1.1\r\n" +
				  "Host: testserver\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		
		String body = con.readStringByLength(contentLength);
			
		Assert.assertEquals("true testserver:443 / ", body);
	}

	
	@Test 
	public void testSSLWithPort() throws Exception {

		IServer server = new HttpServer(0, new ServerHandler(), SSLTestContextFactory.getSSLContext(), true);
		ConnectionUtils.start(server);
		
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort(), SSLTestContextFactory.getSSLContext(), true);

		con.write("GET / HTTP/1.1\r\n" +
				  "Host: testserver:6644\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		
		String body = con.readStringByLength(contentLength);
			
		Assert.assertEquals("true testserver:6644 / ", body);
	}
	
	
	private static final class ServerHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			String server = exchange.getRequest().getServerName();
			int port = exchange.getRequest().getServerPort();
			String query = exchange.getRequest().getQueryString();
			if (query == null) {
				query = "";
			}
			boolean isSecured = exchange.getRequest().isSecure();
			String uri = exchange.getRequest().getRequestURI();
			exchange.send(new HttpResponse(200, "text/plain", isSecured + " " + server + ":" + port + " " + uri + " " + query));
		}
	}
}
