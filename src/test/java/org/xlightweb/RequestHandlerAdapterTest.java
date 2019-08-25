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

import junit.framework.Assert;

import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;

/**
*
* @author grro@xlightweb.org
*/
public final class RequestHandlerAdapterTest {
	

	@Test 
	public void testSimple() throws Exception {
		
		HttpServer server = new HttpServer(new RequestHandler());
		server.start();

		HttpClient httpClient = new HttpClient();
		
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("GET called", response.getBody().readString());

		response = httpClient.call(new HttpRequest("HEAD", "http://localhost:" + server.getLocalPort() + "/"));
		Assert.assertEquals(200, response.getStatus());
		Assert.assertFalse(response.hasBody());

		
		response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort() + "/"));
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("POST called", response.getBody().readString());
		
		
		response = httpClient.call(new DeleteRequest("http://localhost:" + server.getLocalPort() + "/"));
		Assert.assertEquals(404, response.getStatus());
		
		httpClient.close();
		server.close();
	}
	
	

	private static final class RequestHandler extends HttpRequestHandler {
		
		@Override
		public void doGet(IHttpExchange exchange) throws IOException, BadMessageException {
			exchange.send(new HttpResponse(200, "text/plain", "GET called"));
		}
		
		@Override
		public void doPost(IHttpExchange exchange) throws IOException, BadMessageException {
			exchange.send(new HttpResponse(200, "text/plain", "POST called"));
		}
	}
}
