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
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IServer;





/**
*
* @author grro@xlightweb.org
*/
public final class UpgradeSSLTest  {
	
	
	
	
	@Test
	public void testClientInitiated() throws Exception {
		IServer server = new HttpServer(0, new RequestHandler(), SSLTestContextFactory.getSSLContext(), false);
		server.start();
		
		
		GetRequest request = new GetRequest("/");
		request.setHeader("Host", "localhost");
		request.setHeader("User-Agent", "me");
		request.setHeader("Upgrade", "TLS/1.0");
		request.setHeader("Connection", "Upgrade");
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort(), SSLTestContextFactory.getSSLContext(), false);
		con.write(request.toString());
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			
		Assert.assertTrue(header.indexOf("101") != -1);
		Assert.assertTrue(header.indexOf("Upgrade: TLS/1.0, HTTP/1.1") != -1);
		
		System.out.println("activating secured mode");
		con.activateSecuredMode();
		
		QAUtil.sleep(500);
		Assert.assertTrue(con.isSecure());

		System.out.println("send 2.te request");
		request = new GetRequest("/");
		request.setHeader("Host", "localhost");
		request.setHeader("User-Agent", "me");

		con.write(request.toString());
			
		header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		
		System.out.println("got 2.te response header");
		int contentLength = QAUtil.readContentLength(header);
			
		String body = con.readStringByLength(contentLength);
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("isSecured=true", body);
		
		con.close();
		server.close();
	}
	
	
	
	@Test
	public void testClientInitiatedServerNoSSL() throws Exception {
		
		IServer server = new HttpServer(0, new RequestHandler());
		ConnectionUtils.start(server);
		
		
		GetRequest request = new GetRequest("/");
		request.setHeader("Host", "localhost");
		request.setHeader("User-Agent", "me");
		request.setHeader("Upgrade", "TLS/1.0");
		request.setHeader("Connection", "Upgrade");
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort(), SSLTestContextFactory.getSSLContext(), false);
		con.write(request.toString());
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		
		String msg = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("400") != -1);


		
				
		con.close();
		server.close();
	}
	
	
	
	public static final class RequestHandler implements IHttpRequestHandler {
			
		public void onRequest(IHttpExchange exchange) throws IOException {
			exchange.send(new HttpResponse(200, "text/plain" , "isSecured=" + exchange.getRequest().isSecure()));
		}
	}
}