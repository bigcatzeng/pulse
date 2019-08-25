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
package org.xlightweb.server;



import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;
import org.xlightweb.BadMessageException;
import org.xlightweb.GetRequest;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.QAUtil;
import org.xlightweb.SSLTestContextFactory;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;



/**
*
* @author grro@xlightweb.org
*/
public final class SSLHttpServerReuseConnectionTest {


	@Test
	public void testPlain() throws Exception {
	    HttpServer server = new HttpServer(0, new RequestHandler());

        server.start();

        HttpClient httpClient = new HttpClient(SSLTestContextFactory.getSSLContext());
        
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("OK", response.getBody().readString());

        QAUtil.sleep(500);
        
        response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("OK", response.getBody().readString());

        QAUtil.sleep(500);
        Assert.assertEquals(1, server.getNumHandledConnections());  // connection has been reused
        
        httpClient.close(); 
        server.close();
	}


	
    @Test
    public void testSSL() throws Exception {
        HttpServer server = new HttpServer(0, new RequestHandler(), SSLTestContextFactory.getSSLContext(), true);

        server.start();

        HttpClient httpClient = new HttpClient(SSLTestContextFactory.getSSLContext());
        
        IHttpResponse response = httpClient.call(new GetRequest("https://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("OK", response.getBody().readString());

        QAUtil.sleep(500);
        
        response = httpClient.call(new GetRequest("https://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("OK", response.getBody().readString());

        
        QAUtil.sleep(500);
        Assert.assertEquals(1, server.getNumHandledConnections());  // connection has been reused
        
        httpClient.close(); 
        server.close();
    }

    

    private static final class RequestHandler implements IHttpRequestHandler {
        
        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
            exchange.send(new HttpResponse(200, "text/plain", "OK"));
        }
    }
    
}
