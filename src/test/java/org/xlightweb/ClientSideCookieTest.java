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

import junit.framework.Assert;

import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;


/**
*
* @author grro@xlightweb.org
*/
public final class ClientSideCookieTest {
  
	
	@Test
	public void testRewriteCookie() throws Exception {
	    HttpServer server = new HttpServer(new RequestHandler());
	    server.start();
	    
	    HttpClient httpClient = new HttpClient();
	    
	    GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/test");
	    IHttpResponse response = httpClient.call(request);
	    Assert.assertEquals(200, response.getStatus());
	    
	    request = new GetRequest("http://localhost:" + server.getLocalPort() + "/test");
	    response = httpClient.call(request);
	    Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("JSESSIONID=1", request.getHeader("Cookie"));
	    
        request = new GetRequest("http://localhost:" + server.getLocalPort() + "/test");
        response = httpClient.call(request);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("JSESSIONID=2", request.getHeader("Cookie"));

        request = new GetRequest("http://localhost:" + server.getLocalPort() + "/test");
        response = httpClient.call(request);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("JSESSIONID=3", request.getHeader("Cookie"));

        
	    httpClient.close();
	    server.close();
	}

	
	private static final class RequestHandler implements IHttpRequestHandler {
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        IHttpRequest request = exchange.getRequest();
	        
	        IHttpResponse response = new HttpResponse(200, "text/plain", "OK");
	        
	        String cookie = request.getHeader("Cookie");
	        if (cookie == null) {
	            response.setHeader("Set-Cookie", "JSESSIONID=1");
	        } else {
	            int value = Integer.parseInt(cookie.substring(cookie.length() - 1, cookie.length()));
	            value++;
	            response.setHeader("Set-Cookie", "JSESSIONID=" + value);
	        }
	        
	        exchange.send(response);
	    }	    
	}
}
