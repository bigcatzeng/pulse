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
package org.xlightweb.server;



import java.io.IOException;


import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xlightweb.GetRequest;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.QAUtil;
import org.xlightweb.SSLTestContextFactory;
import org.xlightweb.SimpleProxy;


/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientProxyReuseTest  {

    private static HttpServer server;
    private static HttpServer secServer;
    private static SimpleProxy proxy;
    
    
    
    @BeforeClass
    public static void setUp() throws IOException {
        server = new HttpServer(0, new RequestHandler());
        server.start();
        
        secServer = new HttpServer(0, new RequestHandler(), SSLTestContextFactory.getSSLContext(), true);
        secServer.start();
        
        proxy = new SimpleProxy(0);
        proxy.start();        
    }

    
    @AfterClass
    public static void tearDown() throws IOException {
        proxy.close();
        server.close();
        secServer.close();
    }

    
    
	@Test
	public void testPlain() throws Exception {

	    HttpClient httpClient = new HttpClient();
		httpClient.setProxyHost("localhost");
		httpClient.setProxyPort(proxy.getLocalPort());
 
		
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("OK", response.getBody().readString());
		
		QAUtil.sleep(500);
		
		response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("OK", response.getBody().readString());
        
        QAUtil.sleep(500);
        Assert.assertEquals(1, server.getNumHandledConnections());  // connection should have been reused
		
		httpClient.close();
	}
	
	
	
    @Test
    public void testSSL() throws Exception {

        HttpClient httpClient = new HttpClient(SSLTestContextFactory.getSSLContext());
        httpClient.setProxyHost("localhost");
        httpClient.setProxyPort(proxy.getLocalPort());
 
        
        IHttpResponse response = httpClient.call(new GetRequest("https://localhost:" + secServer.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("OK", response.getBody().readString());
        
        QAUtil.sleep(500);
        
        response = httpClient.call(new GetRequest("https://localhost:" + secServer.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("OK", response.getBody().readString());
        
        QAUtil.sleep(500);
        Assert.assertEquals(1, secServer.getNumHandledConnections());  // connection should have been reused
        
        httpClient.close();
    }
    

   
    
    
    
    private static final class RequestHandler implements IHttpRequestHandler {
        
        public void onRequest(IHttpExchange exchange) throws IOException {
            exchange.send(new HttpResponse(200, "text/plain", "OK"));
        }
    }
	
}