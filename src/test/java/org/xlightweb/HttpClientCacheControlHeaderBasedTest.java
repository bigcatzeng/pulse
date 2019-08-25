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
import org.xlightweb.CacheHandler;
import org.xlightweb.GetRequest;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;



/**
 * 
 * @author grro@xlightweb.org
 */
public final class HttpClientCacheControlHeaderBasedTest  {


   
	@Test
	public void testSimple() throws Exception {
	    
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                HttpResponse resp = new HttpResponse(200, "text/plain", "test");
                resp.setHeader("Cache-Control", "public, max-age=1000");
                exchange.send(resp);
            }
        };
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setCacheMaxSizeKB(100);
        
        IHttpResponse resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, resp.getStatus());
        Assert.assertEquals("test", resp.getBody().readString());
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        
        QAUtil.sleep(1000);
        
        resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, resp.getStatus());
        Assert.assertTrue(resp.getHeader(CacheHandler.XHEADER_NAME).startsWith("HIT"));
        Assert.assertEquals("test", resp.getBody().readString());
        
        
        Assert.assertEquals(1, httpClient.getNumCacheHit());
        Assert.assertEquals(1, httpClient.getNumCacheMiss());
        
        httpClient.close();
        server.close();
	}   

	
	
	@Test
    public void testRevalidate() throws Exception {
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                HttpResponse resp = new HttpResponse(200, "text/plain", "test");
                resp.setHeader("Cache-Control", "public, max-age=1000, must-revalidate");
                exchange.send(resp);
            }
        };
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setCacheMaxSizeKB(100);
        
        IHttpResponse resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, resp.getStatus());
        Assert.assertEquals("test", resp.getBody().readString());
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        
        QAUtil.sleep(1000);
        
        resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, resp.getStatus());
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        Assert.assertEquals("test", resp.getBody().readString());
        
        
        Assert.assertEquals(0, httpClient.getNumCacheHit());
        Assert.assertEquals(2, httpClient.getNumCacheMiss());
        
        httpClient.close();
        server.close();
    }   
	
	
	
    @Test
    public void testMissingAge() throws Exception {
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                HttpResponse resp = new HttpResponse(200, "text/plain", "test");
                resp.setHeader("Cache-Control", "public");
                exchange.send(resp);
            }
        };
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setCacheMaxSizeKB(100);
        
        IHttpResponse resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, resp.getStatus());
        Assert.assertEquals("test", resp.getBody().readString());
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        
        QAUtil.sleep(1000);
        
        resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, resp.getStatus());
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        Assert.assertEquals("test", resp.getBody().readString());
        
        
        Assert.assertEquals(0, httpClient.getNumCacheHit());
        Assert.assertEquals(2, httpClient.getNumCacheMiss());
        
        httpClient.close();
        server.close();
    }   	
	
}
