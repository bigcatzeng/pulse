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

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;



import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.ConnectionUtils;

 

/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientAsyncSendTest  {
  
	
	@Test
	public void testSimple() throws Exception {
	    
	    IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                exchange.send(new HttpResponse(200));
            }
        };
		
		HttpServer server = new HttpServer(reqHdl);
		server.start();
		
		
		HttpClient httpClient = new HttpClient();
		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test", "text/plain;charset=UTF-8");
		BodyDataSink bodyDataSink = httpClient.send(header, respHdl);
		bodyDataSink.write(10);

		Assert.assertEquals(200, respHdl.getResponse().getStatus());

		
		httpClient.close();
		server.close();
	}
}
