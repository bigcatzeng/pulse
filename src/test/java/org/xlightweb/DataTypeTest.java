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
public final class DataTypeTest  {
    
    public static void main(String[] args) throws Exception {
        
        for (int i = 0; i < 1000; i++) {
            new DataTypeTest().testSimple();
        }
    }


	@Test
	public void testSimple() throws Exception {
	    HttpServer server = new HttpServer(new RequestHandler());
	    server.start();
	    
	    HttpClient httpClient = new HttpClient();
	    
	    HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/", "application/octet-stream");
	    
	    FutureResponseHandler respHdl = new FutureResponseHandler();
	    BodyDataSink dataSink = httpClient.send(header, respHdl);
	    
	    dataSink.write((int) 6);
	    dataSink.write((long) 9);
	    dataSink.write((double) 5.5);
	    dataSink.close();
	    
	    IHttpResponse resp = respHdl.getResponse();
	    BodyDataSource body = resp.getBody();

	    Assert.assertEquals(6, body.readInt());
	    Assert.assertEquals(9, body.readLong());
	    Assert.assertEquals(5.5, body.readDouble(), 0);
	    
	    
	    httpClient.close();
	    server.close();
	}


	
	private static final class RequestHandler implements IHttpRequestHandler {
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	    	IHttpRequest req = exchange.getRequest();
	    	NonBlockingBodyDataSource body = req.getNonBlockingBody();
	        
	        exchange.send(new HttpResponse(200, req.getContentType(), body));
	    }
	}
}