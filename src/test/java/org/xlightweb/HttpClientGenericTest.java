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


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.client.IHttpClientEndpoint;
import org.xlightweb.client.HttpClient.FollowsRedirectMode;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IServer;



/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientGenericTest  {
 
 
	@Test
	public void testLiveGet() throws Exception {
		IHttpClientEndpoint httpClient = new HttpClient();
		
		IHttpResponse response = httpClient.call(new GetRequest("http://www.web.de/index.html"));
		Assert.assertEquals(302, response.getStatus());
	}
	
	
	@Test
	public void testLiveGetFollowRedirect() throws Exception {
		HttpClient httpClient = new HttpClient();
		httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
		
		IHttpResponse response = httpClient.call(new GetRequest("http://www.web.de/index.html"));
		Assert.assertEquals(200, response.getStatus());
	}
 
	
	@Test
	public void testPostPlainBodyData() throws Exception {
		IHttpClientEndpoint httpClient = new HttpClient();
		
		IServer server = new HttpServer(new RequestHandler());
		server.start();
		

		IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort() + "/", "text/plain", "UTF-8", "Test"));
		String body = response.getBody().readString();
		
		server.close();
		httpClient.close();
		
		Assert.assertTrue(body.equals("Test"));
	} 
	
	
	

	

/*	@Test
	public void testGetLiveHttps() throws Exception {
		HttpClient httpClient = new HttpClient(SSLContext.getDefault());  // JSE 1.6 !

		HttpResponse response = httpClient.callFollowRedirects(new GetRequest("https://www.gmx.de/"));
			
		httpClient.close();
			
		Assert.assertEquals(200, response.getStatus());
	}*/
	
	
	




	
	
	private static final class RequestHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {

			IHttpRequest request = exchange.getRequest();
			exchange.send(new HttpResponse(200, request.getContentType(), request.getNonBlockingBody()));
		}
		
	}
}