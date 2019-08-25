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
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;


/**
*
* @author grro@xlightweb.org
*/
public final class BulkChunkedTest {


	@Test
	public void testSimple() throws Exception {
		
		HttpServer server = new HttpServer(new Handler()); 
		server.start();
		
		HttpClient httpClient = new HttpClient();
		
		for (int i = 0; i < 100; i++) {
			IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
			String body = response.getBody().readString();
			
			String expected = "<HTML>\r\n" + "<BODY>\r\n" + "<H1>test</H1>\r\n" + "</BODY>\r\n" + "</HTML>\r\n";
			if (!body.equals(expected)) {
				System.out.println("got " + body + " instead of " + expected);
				Assert.fail();
			}
		}
		
		Assert.assertTrue("pool has not been worked", httpClient.getNumCreated() < 100);

		
		
		httpClient.close();
		server.close();
	}

	
	
	private static final class Handler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {

			HttpResponseHeader response = new HttpResponseHeader(200, "text/plain");
			
			BodyDataSink channel = exchange.send(response);
			channel.write("<HTML>\r\n");
			channel.write("<BODY>\r\n");
			channel.write("<H1>test</H1>\r\n");
			channel.write("</BODY>\r\n");
			channel.write("</HTML>\r\n");
			channel.close();
		}
	}
}
