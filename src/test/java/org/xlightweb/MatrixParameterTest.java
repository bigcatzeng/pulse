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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Ignore;

import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;



/**
*
* @author grro@xlightweb.org
*/
public final class MatrixParameterTest {


    @Ignore
	@Test
	public void testJetty() throws Exception {

		WebContainer servletEngine = new WebContainer(new MyServlet());
		servletEngine.start();

		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + servletEngine.getLocalPort() + "/test;param=1"));
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("requestURI=/test;param=1", response.getBody().readString());
		
		httpClient.close();
		servletEngine.stop();
	}
	
	
    @Ignore
	@Test
    public void testLightweb() throws Exception {

	    
	    HttpServer server = new HttpServer(new RequestHandler());
	    server.start();

        HttpClient httpClient = new HttpClient();
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test;param=1"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("requestURI=/test;param=1%queryString=null%Mparam=1%", response.getBody().readString());

        
        response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test;param=1;param=2"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("requestURI=/test;param=1;param=2%queryString=null%Mparam=1%Mparam=2%", response.getBody().readString());

        
        response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test;param=1;param=2;param2=8"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("requestURI=/test;param=1;param=2;param2=8%queryString=null%Mparam2=8%Mparam=1%Mparam=2%", response.getBody().readString());
        
        
        response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test;param=1;param=2;param2=8?param=7777"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("requestURI=/test;param=1;param=2;param2=8%queryString=param=7777%Pparam=7777%Mparam2=8%Mparam=1%Mparam=2%", response.getBody().readString());
        
        
        response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test;param=1;param=2;param2=8;removeMe=77"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("requestURI=/test;param=1;param=2;param2=8%queryString=null%Mparam2=8%Mparam=1%Mparam=2%", response.getBody().readString());

        
        response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test;param=1;param=2;param2=8;removeMe=77;param3=9?test=1"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("requestURI=/test;param=1;param=2;param2=8;param3=9%queryString=test=1%Ptest=1%Mparam2=8%Mparam=1%Mparam=2%Mparam3=9%", response.getBody().readString());
        
 
        
        
        httpClient.close();
        server.close();
    }

	
	private static final class RequestHandler implements IHttpRequestHandler {
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        
	        IHttpRequest request = exchange.getRequest();
	        
	        request.removeMatrixParameter("removeMe");
	        
	        StringBuilder sb = new StringBuilder();
	        sb.append("requestURI=" + request.getRequestURI() + "%");
	        sb.append("queryString=" + request.getQueryString() + "%");
	        
	        
	        for (String name : request.getParameterNameSet()) {
                for (String value : request.getParameterValues(name)) {
                    sb.append("P" + name + "=" + value + "%");
                }
            }
	        
	        
	        for (String name : request.getMatrixParameterNameSet()) {
	            for (String value : request.getMatrixParameterValues(name)) {
	                sb.append("M" + name + "=" + value + "%");
	            }
	        }
	        
	        
	        exchange.send(new HttpResponse(200, "text/plain", sb.toString()));
	    }
	}


	private static final class MyServlet extends HttpServlet {

		private static final long serialVersionUID = 8183044648040068422L;

				@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.setContentType("text/plain");
			resp.getWriter().write("requestURI=" + req.getRequestURI());
		}
	}
	
}
