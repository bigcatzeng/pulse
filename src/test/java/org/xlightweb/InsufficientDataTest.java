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

import junit.framework.Assert;

import org.junit.Test;
import org.xlightweb.BadMessageException;
import org.xlightweb.GetRequest;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.ConnectionUtils;


/**
*
* @author grro@xlightweb.org
*/
public final class InsufficientDataTest {

    
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 1000; i++) {
            new InsufficientDataTest().testBulk();
        }
    }
    
    
    
    @Test
    public void testBulk() throws Exception {
        for (int i = 0; i < 30; i++) {
            testSimple();
        }
    }   


	@Test
	public void testSimple() throws Exception {
	    
	    IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                HttpResponse resp = new HttpResponse(200, "text/plain", "test");
                resp.setHeader("Expires", "Fri, 30 Oct 2011 14:19:41 GMT");
                resp.setHeader("Cache-Control", "public, max-age=1000");
                exchange.send(resp);
            }
        };
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setCacheMaxSizeKB(100);
        
        ConnectionUtils.registerMBean(httpClient);
        
        
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/test/12345");
        IHttpResponse resp = httpClient.call(request);
        Assert.assertEquals(200, resp.getStatus());
        Assert.assertEquals("test", resp.getBody().readString());
        
        httpClient.close();
        server.close();	    
    }   
}
