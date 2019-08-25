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


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;



/**
*
* @author grro@xlightweb.org
*/
public final class RequestHandlerBlockingBodyTest  {




	@Test
	public void testSimple() throws Exception {
	    
	    RequestHandler reqHdl = new RequestHandler();
	    HttpServer server = new HttpServer(reqHdl);
	    server.start();
	    	    
		HttpClient httpClient = new HttpClient();
		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		BodyDataSink dataSink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/", "text/plain"), respHdl);
		dataSink.write("line one \r\n");
		QAUtil.sleep(300);
		
		dataSink.write("line two \r\n");
		QAUtil.sleep(600);
		
		dataSink.write("line three \r\n");
		QAUtil.sleep(900);
		dataSink.close();
		
		IHttpResponse resp = respHdl.getResponse();

		String body = resp.getBody().readString();
		Assert.assertEquals("line one \r\nline two \r\nline three \r\n", body);

		httpClient.close();
		server.close();
	}

	
	private static final class RequestHandler implements IHttpRequestHandler {
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        
	        IHttpRequest req = exchange.getRequest();
	        BodyDataSource bodyDataSource = req.getBody();
	        String body = bodyDataSource.readString();
	        
	        exchange.send(new HttpResponse(200, "plain/text", body));
	    }
	}
}