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
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;



 


/**
*
* @author grro@xlightweb.org
*/
public final class NotModifiedTest  {

	
	@Test
	public void testNotModified() throws Exception {

		HttpServer server = new HttpServer(new ServerHandler());
		server.start();
		
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		GetRequest request = new GetRequest("/test.html");
		request.setHeader("If-Modified-Since", "Fri, 11 Jul 2008 18:05:46 GMT");

		IHttpResponse response = con.call(request);
		
		Assert.assertEquals(304, response.getStatus());
		
		
		
		con.close();
		server.close();
	}

	
	
	
	private static final class ServerHandler implements IHttpRequestHandler {

		public void onRequest(IHttpExchange exchange) throws IOException {
			
			System.out.println(exchange.getRequest().getCharacterEncoding());
			
			HttpResponse response = new HttpResponse(304);
			response.setHeader("Last-Modified", "Fri, 11 Jul 2008 18:05:46 GMT");
			
			exchange.send(response);
		}
	}
}
