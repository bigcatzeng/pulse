/*
 *  Copyright (c) xlightweb.org, 2006 - 2009. All rights reserved.
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

import java.util.concurrent.atomic.AtomicInteger;


import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IBlockingConnection;


/**
*
* @author grro@xlightweb.org
*/
public final class HttpServerTest {

	
	@Test
	public void testSimple() throws Exception {
		Handler hdl = new Handler();  
		HttpServer server = new HttpServer(hdl);
		server.addConnectionHandler(hdl);
		server.setMaxConcurrentConnections(20000);
		server.start();

		ConnectionUtils.registerMBean(server);
		
		HttpClient httpClient = new HttpClient();

		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/s/"));
		Assert.assertEquals(200, response.getStatus());
	
		Assert.assertEquals(1, hdl.getCountConnected());
		Assert.assertEquals(0, hdl.getCountDisconnected());

		response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		Assert.assertEquals(400, response.getStatus());
		
		
		httpClient.close();
		QAUtil.sleep(1000);
		Assert.assertEquals(1, hdl.getCountConnected());
		Assert.assertEquals(1, hdl.getCountDisconnected());
		
		server.close();
	}

	
	@Test
	public void testHttp_09_Request() throws Exception {
		
		
		Handler hdl = new Handler();  
		HttpServer server = new HttpServer("localhost", 0, hdl);
		server.addConnectionHandler(hdl);
		server.setMaxConcurrentConnections(20000);
		server.start();


		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		
		con.write("GET /s/\r\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n");
		Assert.assertTrue(header.indexOf("400") != -1);

		con.close();
		QAUtil.sleep(1000);
		Assert.assertEquals(1, hdl.getCountConnected());
		Assert.assertEquals(1, hdl.getCountDisconnected());

		
		server.close();
	}

	
	@Test
	public void testBadRequest() throws Exception {
		
		System.setProperty("org.xlightweb.showDetailedError", "true");
		
		Handler hdl = new Handler();  
		HttpServer server = new HttpServer("localhost", 0, hdl);
		server.addConnectionHandler(hdl);
		server.setMaxConcurrentConnections(20000);
		server.start();


		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		
		con.write("GET /s/ ere6r" + "\r\n\r\n");

		String header = con.readStringByDelimiter("\r\n\r\n");
		Assert.assertTrue(header.indexOf("400") != -1);


		
		server.close();
	}

	
	

	@Test
	public void testHttp_1_0_Request() throws Exception {
		
		
		Handler hdl = new Handler();  
		HttpServer server = new HttpServer("localhost", 0, hdl);
		server.addConnectionHandler(hdl);
		server.setMaxConcurrentConnections(20000);
		server.start();


		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		
		String request = "GET /s/ HTTP/1.0\r\n\r\n";
		con.write(request);
		
		String header = con.readStringByDelimiter("\r\n\r\n");
		Assert.assertTrue(header.indexOf("200") != -1);

		QAUtil.sleep(1000);

		Assert.assertEquals(1, hdl.getCountConnected());
		Assert.assertEquals(1, hdl.getCountDisconnected());

		con = new BlockingConnection("localhost", server.getLocalPort());

		request = "GET / HTTP/1.0\r\n\r\n";
		con.write(request);
		
		header = con.readStringByDelimiter("\r\n\r\n");
		Assert.assertTrue(header.indexOf("400") != -1);
		
		con.close();
		QAUtil.sleep(1000);
		Assert.assertEquals(2, hdl.getCountConnected());
		Assert.assertEquals(2, hdl.getCountDisconnected());

		
		server.close();
	}

	
	public class Handler implements IHttpConnectHandler, IHttpRequestHandler, IHttpDisconnectHandler {

		private AtomicInteger countConnected = new AtomicInteger();
		private AtomicInteger countDisconnected = new AtomicInteger();
		
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			if (exchange.getRequest().getPathInfo().startsWith("/s/")) {
				exchange.send(new HttpResponse(200));
			} else {
				exchange.send(new HttpResponse(400));
			}
		}
		
		public boolean onConnect(IHttpConnection httpConnection) throws IOException {
			countConnected.incrementAndGet();
			return true;
		}
		
		public boolean onDisconnect(IHttpConnection httpConnection) throws IOException {
			countDisconnected.incrementAndGet();
			return true;
		}
		
		int getCountConnected() {
			return countConnected.get();
		}
		
		int getCountDisconnected() {
			return countDisconnected.get();
		}
	}
}
