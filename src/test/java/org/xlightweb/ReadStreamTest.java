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
public final class ReadStreamTest  {

	 
	@Test
	public void testSimple() throws Exception {
	    
	    HttpServer server = new HttpServer(new RequestHandler());
	    server.start();
	    
	    HttpClient client = new HttpClient();
	    
	    IHttpResponse resp = client.call(new GetRequest("Http://localhost:" + server.getLocalPort() + "/"));
	    
	    BodyDataSource bodyDataSource = resp.getBody();

	    for (int i = 0; i < 10000; i++) {
	        String value = bodyDataSource.readStringByDelimiter("\r\n\r\n");
	        Assert.assertEquals(Integer.toString(i), value);
	    }
	    
	    client.close();
	    server.close();
	}
	

	private static final class RequestHandler implements IHttpRequestHandler {
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        
	        BodyDataSink ds = exchange.send(new HttpResponseHeader(200, "text/event-stream"));
	        
	        for (int i = 0; i < 10000; i++) {
	            if ((i % 200) == 50) {
	                QAUtil.sleep(100);
	            }
	            ds.write(i + "\r\n\r\n");
	        }
	    }
	}
}