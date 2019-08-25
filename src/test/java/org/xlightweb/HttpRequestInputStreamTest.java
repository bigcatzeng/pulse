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




import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;



import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;



/**
*
* @author grro@xlightweb.org
*/
public final class HttpRequestInputStreamTest  {

 
	
	@Test
	public void testGET() throws Exception {
		
	    HttpServer server = new HttpServer(new RequestParamsRequestHandler());
	    server.start();
	    
		HttpClient httpClient = new HttpClient();

		ByteArrayInputStream bis = new ByteArrayInputStream(new byte[0]);
		
		IHttpResponse response = httpClient.call(new HttpRequest(new HttpRequestHeader("GET", "http://localhost:" + server.getLocalPort() + "/"), bis));		
		Assert.assertEquals(200, response.getStatus());
		
		httpClient.close();
		server.close();
	}

	

	   
    @Test
    public void testPOSTform() throws Exception {
        
        HttpServer server = new HttpServer(new RequestParamsRequestHandler());
        server.start();
        
        HttpClient httpClient = new HttpClient();

        String s = "param1=value1&param2=value2";
        ByteArrayInputStream bis = new ByteArrayInputStream(s.getBytes());
        
        IHttpResponse response = httpClient.call(new HttpRequest(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/", "application/x-www-form-urlencoded"), bis));
        
        Assert.assertEquals(200, response.getStatus());
        
        String body = response.getBody().toString();
        Assert.assertTrue(body.indexOf("param2=value2") != -1);
        Assert.assertTrue(body.indexOf("param1=value1") != -1);
        
        httpClient.close();
        server.close();
    }

    
    @Test
    public void testPOST() throws Exception {
        
        HttpServer server = new HttpServer(new EchoHandler());
        server.start();
        
        HttpClient httpClient = new HttpClient();

        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream(pos);

        pos.write("Hello ".getBytes());
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        httpClient.send(new HttpRequest(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/", "text/plain"), pis), respHdl);
        
        IHttpResponse response = respHdl.getResponse();
        
        QAUtil.sleep(300);
        pos.write("how ".getBytes());
        
        QAUtil.sleep(300);
        pos.write("are ".getBytes());

        QAUtil.sleep(300);
        pos.write("you".getBytes());
        pos.close();
        
        QAUtil.sleep(1000);
        
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("Hello how are you", response.getBody().toString());

        
        
        httpClient.close();
        server.close();
    }
	
	
	
	@Test
    public void testPOSTEmptyBody() throws Exception {
        
        HttpServer server = new HttpServer(new RequestParamsRequestHandler());
        server.start();
        
        HttpClient httpClient = new HttpClient();

        ByteArrayInputStream bis = new ByteArrayInputStream(new byte[0]);
        
        IHttpResponse response = httpClient.call(new HttpRequest(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/"), bis));        
        Assert.assertEquals(200, response.getStatus());
        
        httpClient.close();
        server.close();
    }

	
	
	public final class RequestParamsRequestHandler implements IHttpRequestHandler {

	    public void onRequest(IHttpExchange exchange) throws IOException {

	        IHttpRequest request = exchange.getRequest();
	        StringBuilder sb = new StringBuilder();
	        
	        for (String paramName : request.getParameterNameSet()) {
	           sb.append(paramName + "=" + request.getParameter(paramName) + "\r\n"); 
	        }
	        
	        exchange.send(new HttpResponse(200, "text/plain", sb.toString()));
	    }
	}
	

    public final class EchoHandler implements IHttpRequestHandler {

        public void onRequest(IHttpExchange exchange) throws IOException {
            exchange.send(new HttpResponse(200, "text/plain", exchange.getRequest().getNonBlockingBody()));
        }
    }
}