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
import java.util.Date;

import junit.framework.Assert;

import org.junit.Test;
import org.xlightweb.BadMessageException;
import org.xlightweb.CacheHandler;
import org.xlightweb.GetRequest;
import org.xlightweb.HttpResponse;
import org.xlightweb.HttpUtils;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;


/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientUserValidationBasedCachingTest  {
 

	@Test
	public void testETag() throws Exception {
	    
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                
                IHttpRequest request = exchange.getRequest();
                String ifNoneMatch = request.getHeader("If-None-Match");
                if ((ifNoneMatch != null) && (ifNoneMatch.equals("\"23\""))) {
                    exchange.send(new HttpResponse(304));
                    return;
                }
                
                HttpResponse resp = new HttpResponse(200, "text/plain", "test");
                resp.setHeader("ETag", "\"23\"");
                exchange.send(resp);
            }
        };
        
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setCacheMaxSizeKB(100);
        
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
        request.setHeader("If-None-Match", "\"23\"");
        IHttpResponse resp = httpClient.call(request);
        Assert.assertEquals(304, resp.getStatus());
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        
        QAUtil.sleep(1000);
        
        Assert.assertEquals(0, httpClient.getNumCacheHit());
        Assert.assertEquals(0, httpClient.getNumCacheMiss());
        
        httpClient.close();
        server.close();
    }   
    
    @Test
    public void testLastModification() throws Exception {
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {

            private final String date = HttpUtils.toRFC1123DateString(new Date());
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                
                IHttpRequest request = exchange.getRequest();
                String ifModifiedSince = request.getHeader("If-Modified-Since");
                if ((ifModifiedSince != null) && (ifModifiedSince.equals(date))) {
                    exchange.send(new HttpResponse(304));
                    return;
                }
                
                HttpResponse resp = new HttpResponse(200, "text/plain", "test");
                resp.setHeader("Last-Modified", date);
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
        String lastModified = resp.getHeader("Last-Modified"); 
        
        QAUtil.sleep(1000);
        
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
        request.setHeader("If-Modified-Since", lastModified);
        resp = httpClient.call(request);
        Assert.assertEquals(304, resp.getStatus());
        Assert.assertNull(resp.getHeader(CacheHandler.XHEADER_NAME));
        
        QAUtil.sleep(1000);
        
        Assert.assertEquals(0, httpClient.getNumCacheHit());
        Assert.assertEquals(1, httpClient.getNumCacheMiss());
        
        httpClient.close();
        server.close();
    }   
}
