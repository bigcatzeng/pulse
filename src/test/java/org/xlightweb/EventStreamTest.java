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
public final class EventStreamTest {

    
    @Test
    public void testSimple() throws Exception {
        
        IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {

                BodyDataSink sink = exchange.send(new HttpResponseHeader(200, "text/event-stream"));
                sink.write("0123456789");
                sink.close();
            }
        };
        
        
        HttpServer server = new HttpServer(hdl);
        server.start();
        
        
        HttpClient httpClient = new  HttpClient();
        IHttpResponse resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        
        Assert.assertEquals(200, resp.getStatus());
        Assert.assertEquals(-1, resp.getContentLength());
        Assert.assertNull(resp.getHeader("Transfer-encoding"));
        
        Assert.assertEquals("SimpleMessageBodyDataSource", resp.getNonBlockingBody().getClass().getSimpleName());
        Assert.assertEquals("0123456789", resp.getBody().readString());
        
        httpClient.close();
        server.close();
    }    
    
    
    @Test
    public void testWithProxy() throws Exception {
        
        IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {

                BodyDataSink sink = exchange.send(new HttpResponseHeader(200, "text/event-stream"));
                sink.write("0123456789");
                sink.close();
            }
        };
        
        
        HttpServer server = new HttpServer(hdl);
        server.start();
        
        HttpProxy proxy =  new HttpProxy(0, "localhost", server.getLocalPort(), false, 60, 30);
        proxy.start();


        HttpClient httpClient = new  HttpClient();
        IHttpResponse resp = httpClient.call(new GetRequest("http://localhost:" + proxy.getLocalPort() + "/"));
        
        Assert.assertEquals(200, resp.getStatus());
        Assert.assertEquals(-1, resp.getContentLength());
        Assert.assertNull(resp.getHeader("Transfer-encoding"));
        
        Assert.assertEquals("SimpleMessageBodyDataSource", resp.getNonBlockingBody().getClass().getSimpleName());
        Assert.assertEquals("0123456789", resp.getBody().readString());
        
        httpClient.close();
        server.close();
    }        
}
