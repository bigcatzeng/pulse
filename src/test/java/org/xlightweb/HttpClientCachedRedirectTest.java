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
import org.xlightweb.client.HttpClient.FollowsRedirectMode;
import org.xlightweb.server.HttpServer;



/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientCachedRedirectTest  {

 
   
	@Test
	public void testPersistentRedirect() throws Exception {
	    
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {

                try {
                    HttpResponse resp = new HttpResponse(301);
                    resp.setHeader("Location", exchange.getRequest().getRequestUrl().toURI() + "/redirected");
                    exchange.send(resp);
                } catch (Exception e) {
                    throw new IOException(e.toString());
                }
            }
        };
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setCacheMaxSizeKB(100);
        
        IHttpResponse resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
        Assert.assertEquals(301, resp.getStatus());
        Assert.assertNotNull(resp.getHeader("Location"));
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        
        QAUtil.sleep(1000);
        
        resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
        Assert.assertEquals(301, resp.getStatus());
        Assert.assertNotNull(resp.getHeader("Location"));
        Assert.assertTrue(resp.getHeader(CacheHandler.XHEADER_NAME).startsWith("HIT"));
        
        
        Assert.assertEquals(1, httpClient.getNumCacheHit());
        Assert.assertEquals(1, httpClient.getNumCacheMiss());
        
        httpClient.close();
        server.close();
	}   


    @Test
    public void testPersistentRedirectNoCache() throws Exception {
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {

                try {
                    HttpResponse resp = new HttpResponse(301);
                    resp.setHeader("Location", exchange.getRequest().getRequestUrl().toURI() + "/redirected");
                    resp.setHeader("Cache-Control", "no-cache");
                    exchange.send(resp);
                } catch (Exception e) {
                    throw new IOException(e.toString());
                }
            }
        };
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setCacheMaxSizeKB(100);
        
        IHttpResponse resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
        Assert.assertEquals(301, resp.getStatus());
        Assert.assertNotNull(resp.getHeader("Location"));
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        
        QAUtil.sleep(1000);
        
        resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
        Assert.assertEquals(301, resp.getStatus());
        Assert.assertNotNull(resp.getHeader("Location"));
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        
        
        Assert.assertEquals(0, httpClient.getNumCacheHit());
        Assert.assertEquals(2, httpClient.getNumCacheMiss());
        
        httpClient.close();
        server.close();
    }   


    
 
    @Test
    public void testNotFoundRedirectCached() throws Exception {
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {

                try {
                    HttpResponse resp = new HttpResponse(302);
                    resp.setHeader("Location", exchange.getRequest().getRequestUrl().toURI() + "/redirected");
                    resp.setHeader("Cache-Control", "public, max-age=3600");
                    exchange.send(resp);
                } catch (Exception e) {
                    throw new IOException(e.toString());
                }
            }
        };
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setCacheMaxSizeKB(100);
        
        IHttpResponse resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
        Assert.assertEquals(302, resp.getStatus());
        Assert.assertNotNull(resp.getHeader("Location"));
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        
        QAUtil.sleep(1000);
        
        resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
        Assert.assertEquals(302, resp.getStatus());
        Assert.assertNotNull(resp.getHeader("Location"));
        Assert.assertTrue(resp.getHeader(CacheHandler.XHEADER_NAME).startsWith("HIT"));
        
        
        Assert.assertEquals(1, httpClient.getNumCacheHit());
        Assert.assertEquals(1, httpClient.getNumCacheMiss());
        
        httpClient.close();
        server.close();
    }


    @Test
    public void testNotFoundRedirectCachedAutohandled() throws Exception {
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
    
                HttpResponse resp = null;
                
                if (exchange.getRequest().getRequestURI().endsWith("/redirected")) {
                    resp = new HttpResponse(200, "text/plain", "OK");
                    
                } else {
                    try {
                        resp = new HttpResponse(302);
                        resp.setHeader("Location", exchange.getRequest().getRequestUrl().toURI() + "/redirected");
                    } catch (Exception e) {
                        throw new IOException(e.toString());
                    }
                }
                
                resp.setHeader("Cache-Control", "public, max-age=3600");
                exchange.send(resp);

            }
        };
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        httpClient.setCacheMaxSizeKB(100);
        
        IHttpResponse resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
        Assert.assertEquals(200, resp.getStatus());
        
        QAUtil.sleep(1000);
        
        resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
        Assert.assertEquals(200, resp.getStatus());
        Assert.assertTrue(resp.getHeader(CacheHandler.XHEADER_NAME).startsWith("HIT"));
        
        Assert.assertEquals(2, httpClient.getNumCacheHit());
        Assert.assertEquals(2, httpClient.getNumCacheMiss());
        
        httpClient.close();
        server.close();
    }   

       

    
    @Test
    public void testSeeOtherTryCached() throws Exception {
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {

                try {
                    HttpResponse resp = new HttpResponse(303);
                    resp.setHeader("Location", exchange.getRequest().getRequestUrl().toURI() + "/redirected");
                    resp.setHeader("Cache-Control", "public, max-age=3600");
                    exchange.send(resp);
                } catch (Exception e) {
                    throw new IOException(e.toString());
                }
            }
        };
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setCacheMaxSizeKB(100);
        
        IHttpResponse resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
        Assert.assertEquals(303, resp.getStatus());
        Assert.assertNotNull(resp.getHeader("Location"));
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        
        QAUtil.sleep(1000);
        
        resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
        Assert.assertEquals(303, resp.getStatus());
        Assert.assertNotNull(resp.getHeader("Location"));
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        
        
        Assert.assertEquals(0, httpClient.getNumCacheHit());
        Assert.assertEquals(2, httpClient.getNumCacheMiss());
        
        httpClient.close();
        server.close();
    }


    @Test
    public void testNotFoundRedirect() throws Exception {
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
    
                try {
                    HttpResponse resp = new HttpResponse(302);
                    resp.setHeader("Location", exchange.getRequest().getRequestUrl().toURI() + "/redirected");
                    exchange.send(resp);
                } catch (Exception e) {
                    throw new IOException(e.toString());
                }
            }
        };
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setCacheMaxSizeKB(100);
        
        IHttpResponse resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
        Assert.assertEquals(302, resp.getStatus());
        Assert.assertNotNull(resp.getHeader("Location"));
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        
        QAUtil.sleep(1000);
        
        resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
        Assert.assertEquals(302, resp.getStatus());
        Assert.assertNotNull(resp.getHeader("Location"));
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        
        
        Assert.assertEquals(0, httpClient.getNumCacheHit());
        Assert.assertEquals(2, httpClient.getNumCacheMiss());
        
        httpClient.close();
        server.close();
    } 
}
