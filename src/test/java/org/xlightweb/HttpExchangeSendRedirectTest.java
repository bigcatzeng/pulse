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
public final class HttpExchangeSendRedirectTest  {

	
	@Test
	public void testAbsolute() throws Exception {


	    IHttpRequestHandler hdl = new IHttpRequestHandler() {
	        
	        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	            
	            IHttpRequest req = exchange.getRequest();
	            exchange.sendRedirect("http://" + req.getHost() + "/redirected");
	        }
	    };
	    
		HttpServer server = new HttpServer(hdl);
		server.start();

		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort()+ "/aaa"));
		
		Assert.assertEquals(302, response.getStatus());
		Assert.assertEquals("http://localhost:" + server.getLocalPort() + "/redirected", response.getHeader("location"));
		
		httpClient.close();
		server.close();
	}

	
    @Test
    public void testRelative() throws Exception {

        IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                
                exchange.getRequest();
                exchange.sendRedirect("redirected");
            }
        };
        
        Context ctx = new Context("/test");
        ctx.addHandler("/*", hdl);
        
        HttpServer server = new HttpServer(ctx);
        server.start();

        HttpClient httpClient = new HttpClient();
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort()+ "/test/aaa?a=b"));
        
        Assert.assertEquals(302, response.getStatus());
        Assert.assertEquals("http://localhost:" + server.getLocalPort() + "/test/aaa/redirected", response.getHeader("location"));
        
        httpClient.close();
        server.close();
    }
    
    
    @Test
    public void testRelative2() throws Exception {

        IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                
                exchange.sendRedirect("/redirected");
            }
        };
        
        Context ctx = new Context("/test");
        ctx.addHandler("/*", hdl);
        
        HttpServer server = new HttpServer(ctx);
        server.start();

        HttpClient httpClient = new HttpClient();
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort()+ "/test/aaa?a=b"));
        
        Assert.assertEquals(302, response.getStatus());
        Assert.assertEquals("http://localhost:" + server.getLocalPort() + "/test/redirected", response.getHeader("location"));
        
        httpClient.close();
        server.close();
    }   

}
