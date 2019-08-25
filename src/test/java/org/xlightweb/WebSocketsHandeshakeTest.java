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
import java.util.logging.Level;

import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;



/**
*
* @author grro@xlightweb.org
*/
public final class WebSocketsHandeshakeTest  {

	 
	@Test
	public void testSimple() throws Exception {

	    IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                
                if (exchange.getRequest().getBody().readString().equals("^n:ds[4U") && (exchange.getRequest().getHeader("Content-Length") == null)) {
                    IHttpResponse resp = new HttpResponse(101, "8jKS'y:G*Co,Wxa-");
                    resp.removeHeader("Content-Length");
                    
                    resp.setHeader("Upgrade", "WebSocket");
                    resp.setHeader("Connection", "Upgrade");
                    resp.setHeader("Sec-WebSocket-Origin", "http://example.com");
                    resp.setHeader("Sec-WebSocket-Location", "ws://example.com/demo");
                    resp.setHeader("Sec-WebSocket-Protocol", "sample");
                    
                    exchange.send(resp);
                    
                } else {
                    exchange.sendError(400);
                }
            }
        };

        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
	    
	    GetRequest req = new GetRequest("http://localhost:" + server.getLocalPort() + "/", "^n:ds[4U".getBytes());
	    req.removeHeader("Content-Length");
	    
	    req.setHeader("Connection", "Upgrade");
	    req.setHeader("Sec-WebSocket-Key2", "12998 5 Y3 1  .P00");
	    req.setHeader("Sec-WebSocket-Protocol", "sample");
	    req.setHeader("Upgrade", "WebSocket");
	    req.setHeader("Sec-WebSocket-Key1", "4 @1  46546xW%0l 1 5");
	    req.setHeader("Origin", "http://example.com");
	    
	    IHttpResponse resp = httpClient.call(req);
	    Assert.assertEquals(101, resp.getStatus());
	    Assert.assertNull(resp.getHeader("Content-Length"));
	    Assert.assertEquals("8jKS'y:G*Co,Wxa-", resp.getBody().readString());
	    
	    httpClient.close();
	    server.close();
	}
	

}