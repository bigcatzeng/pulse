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
import java.net.URL;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClient.FollowsRedirectMode;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IServer;




/**
*
* @author grro@xlightweb.org
*/
public final class ModifedFromURLEncodedParamTest  {


	@Test
	public void testSimple() throws Exception {
	    
	    IServer server = new HttpServer(new ServerHandler());
        server.start();
        
        IServer proxy = new HttpServer(new ProxyHandler("http://localhost:" + server.getLocalPort() + "/login"));
        proxy.start();
        
        
        HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);

        PostRequest request = new PostRequest("http://localhost:" + proxy.getLocalPort() + "/login", new NameValuePair("username", "berta.breit"), new NameValuePair("password", "I dont tell you"));
        
        
        IHttpResponse response = httpClient.call(request);
        
        String body = response.getBody().toString();
        
        Assert.assertTrue(body.indexOf("username") == -1);
        Assert.assertTrue(body.indexOf("dont") != -1);
        Assert.assertTrue(body.indexOf("11") != -1);
        
        httpClient.close();
        proxy.close();
        server.close();
	}
	
	
	private static final class ProxyHandler implements IHttpRequestHandler {
	    
	    private final HttpClient httpClient = new HttpClient();
	    private final String forwardUrl;
	    
	    public ProxyHandler(String forwardUrl) {
	        this.forwardUrl = forwardUrl;
        }
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {

	        IHttpRequest request = exchange.getRequest();
	        
	        request.setParameter("addedParam", "11");
	        request.removeParameter("username");
	        
	        request.setRequestUrl(new URL(forwardUrl));
	        
	        IHttpResponse response = httpClient.call(request);

	        exchange.send(response);
	    }
	}

	
	private static final class ServerHandler implements IHttpRequestHandler {
	        
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        exchange.send(new HttpResponse(200, "text/plain", exchange.getRequest().toString()));
	    }
	}
}