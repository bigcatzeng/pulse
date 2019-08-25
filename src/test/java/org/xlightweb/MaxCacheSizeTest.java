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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xlightweb.BadMessageException;
import org.xlightweb.GetRequest;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.ConnectionUtils;


/**
*
* @author grro@xlightweb.org
*/
public final class MaxCacheSizeTest  {

    private static HttpServer server;
    
    
    @BeforeClass
    public static void setup() throws Exception {
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {

                
                IHttpRequest request = exchange.getRequest();
                
                boolean ignoreValidation = request.getBooleanParameter("ignoreValidadtion", false);
                
                if (!ignoreValidation) {
                    String ifNoneMatch = request.getHeader("If-None-Match");
                    if ((ifNoneMatch != null) && (ifNoneMatch.equals("\"23\""))) {
                        exchange.send(new HttpResponse(304));
                        return;
                    }
                }
                    
                int size = request.getIntParameter("size", 1000);
                HttpResponse resp = new HttpResponse(200, "text/plain", new String(QAUtil.generateByteArray(size)));
                resp.setHeader("ETag", "\"23\"");
                exchange.send(resp);
            }
        };
        
        server = new HttpServer(reqHdl);
        server.start();
    }
    
    
    @AfterClass
    public static void teardown() {
        server.close();
    }
    

	@Test
	public void testSimple() throws Exception {

	    HttpClient httpClient = new HttpClient();
        httpClient.setCacheMaxSizeKB(50);
        
        ConnectionUtils.registerMBean(httpClient);
        
        for (int i = 0; i < 20; i++) {
            httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/" + i + "/?size=4400"));
            QAUtil.sleep(100);
            System.out.println("current=" + httpClient.getCacheSizeKB() + " bytes, max=" +httpClient.getCacheMaxSizeKB() + " Kbytes");
        }
        
        Assert.assertTrue(httpClient.getCacheSizeKB() < (20 * 4.440));
        
        httpClient.close();
        server.close();
    }   
	
	

   
    
}
