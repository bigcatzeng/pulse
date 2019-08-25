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

import org.junit.Ignore;
import org.junit.Test;
import org.xlightweb.BadMessageException;
import org.xlightweb.CacheHandler;
import org.xlightweb.GetRequest;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.PostRequest;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.ConnectionUtils;

 

/**
 * 
 * @author grro@xlightweb.org
 */
public final class HttpClientExpiredBasedCachingTest  {


   
	@Test
	public void testGetWithExpireHeader() throws Exception {
	    
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                HttpResponse resp = new HttpResponse(200, "text/plain", "test");
                resp.setHeader("Expires", "Fri, 30 Oct 2011 14:19:41 GMT");
                exchange.send(resp);
            }
        };
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        
        HttpClient httpClient = new HttpClient();
        httpClient.setCacheMaxSizeKB(100);
        ConnectionUtils.registerMBean(httpClient);
        
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

	
	@Ignore
    @Test
    public void testGetWithExpireHeaderAndPOST() throws Exception {
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                HttpResponse resp = new HttpResponse(200, "text/plain", "test");
                resp.setHeader("Expires", "Fri, 30 Oct 2011 14:19:41 GMT");
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
        
        
        QAUtil.sleep(1000);
        
        resp = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, resp.getStatus());
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        Assert.assertEquals("test", resp.getBody().readString());

        
        QAUtil.sleep(1000);

        
        resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, resp.getStatus());
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        Assert.assertEquals("test", resp.getBody().readString());
        

        
        Assert.assertEquals(1, httpClient.getNumCacheHit());
        Assert.assertEquals(2, httpClient.getNumCacheMiss());
        
        httpClient.close();
        server.close();
    }   	
	

    @Test
    public void testGetWithExpireAndCacheControlHeader() throws Exception {
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                HttpResponse resp = new HttpResponse(200, "text/plain", "test");
                resp.setHeader("Expires", "Fri, 30 Oct 2011 14:19:41 GMT");
                resp.setHeader("Cache-Control", "public, max-age=21600");
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
        
        QAUtil.sleep(1000);
        
        Assert.assertEquals(1, httpClient.getNumCacheHit());
        Assert.assertEquals(1, httpClient.getNumCacheMiss());
        
        httpClient.close();
        server.close();
    }   
    
    
    @Test
    public void testNegativeExpired() throws Exception {
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                HttpResponse resp = new HttpResponse(200, "text/plain", "test");
                resp.setHeader("Expires", "-1");
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
        
        QAUtil.sleep(2000);
        
        resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, resp.getStatus());
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        Assert.assertEquals("test", resp.getBody().readString());
        
        QAUtil.sleep(1000);
        
        Assert.assertEquals(0, httpClient.getNumCacheHit());
        Assert.assertEquals(2, httpClient.getNumCacheMiss());
        
        httpClient.close();
        server.close();
    }   
	
    
       
    
 

    @Test
    public void testGetAlreadyExpired() throws Exception {
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                HttpResponse resp = new HttpResponse(200, "text/plain", "test");
                resp.setHeader("Expires", "Fri, 30 Oct 2011 14:19:41 GMT");
                resp.setHeader("Cache-Control", "public, max-age=1");
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
        
        QAUtil.sleep(2000);
        
        resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, resp.getStatus());
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        Assert.assertEquals("test", resp.getBody().readString());
        
        QAUtil.sleep(1000);
        
        Assert.assertEquals(0, httpClient.getNumCacheHit());
        Assert.assertEquals(2, httpClient.getNumCacheMiss());
        
        httpClient.close();
        server.close();
    }   
	
}
