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




/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientSSLMaxConTest  {

    

	@Test
	public void testSimple() throws Exception {
		System.setProperty("org.xlightweb.showDetailedError", "true");

		IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
		  
		    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
		        exchange.send(new HttpResponse(200, "text/plain", "OK"));
		    }
		};
		
		HttpServer server = new HttpServer(0, reqHdl, SSLTestContextFactory.getSSLContext(), true);
		server.start();
		
		
		HttpClient httpClient = new HttpClient(SSLTestContextFactory.getSSLContext());
		httpClient.setMaxActive(1);
		
		for (int i = 0; i < 3; i++) {
		    IHttpResponse resp = httpClient.call(new GetRequest("https://localhost:" + server.getLocalPort() + "/"));
		    Assert.assertEquals(200, resp.getStatus());
		    Assert.assertEquals("OK", resp.getBody().readString());
		    QAUtil.sleep(1000);
		}
		
		Assert.assertEquals(1, httpClient.getNumCreated());
		
		httpClient.close();
		server.close();
	}
}