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
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IServer;





/**
*
* @author grro@xlightweb.org
*/
public final class LoadBalancerTest  {



	@Test
	public void testSimple() throws Exception {
		IServer server1 = new HttpServer(new RequestHandler("srv1"));
		server1.start();
		
		IServer server2= new HttpServer(new RequestHandler("srv2"));
		server2.start();
		
		
		HttpClient httpClient = new HttpClient();
		
		LoadBalancerRequestInterceptor interceptor = new LoadBalancerRequestInterceptor();
		interceptor.addVirtualServer("http://service", 
				                     "localhost:" + server1.getLocalPort(), 
				                     "localhost:" + server2.getLocalPort());
		httpClient.addInterceptor(interceptor);
		
		
		for (int i = 0; i < 10; i++) {
			IHttpResponse response = httpClient.call(new GetRequest("http://service/test/path?id=12"));
			Assert.assertEquals("srv2", response.getBody().readString());
			
			response = httpClient.call(new GetRequest("http://service/test/path?id=13"));
			Assert.assertEquals("srv1", response.getBody().readString());
		}
		
		httpClient.close();
		server1.close();
		server2.close();
	}


	private static final class RequestHandler implements IHttpRequestHandler {
		
		private String name;
		
		public RequestHandler(String name) {
			this.name = name;
		}
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			exchange.send(new HttpResponse(200, "text/plain", name));
		};
	}

}