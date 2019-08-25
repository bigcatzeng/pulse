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
public final class HttpClientCookieTest  {

	@Test
	public void testMultipleCookies() throws Exception {
		
		HttpServer server = new HttpServer(new RequestHandler());
		server.start();
		
		HttpClient httpClient = new HttpClient();
		httpClient.setAutoHandleCookies(true);
		
		IHttpRequest req = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
		httpClient.call(req);
		
		req = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
		httpClient.call(req);
		
		Assert.assertEquals("name1=1; name2=2", req.getHeader("Cookie"));
		
		httpClient.close();
		server.close();
	}
	
	
	private static final class RequestHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			HttpResponse response = new HttpResponse("OK");
			response.addHeader("Set-Cookie", "name1=1;Path=/");
			response.addHeader("Set-Cookie", "name2=2;Path=/");
			
			exchange.send(response);
		}
		
	}
}