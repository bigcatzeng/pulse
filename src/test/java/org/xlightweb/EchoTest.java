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
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IServer;





/**
*
* @author grro@xlightweb.org
*/
public final class EchoTest  {
	
    	
    @Test
    public void testPlain() throws Exception {
        
        IServer server = new HttpServer(new EchoHandler());
        server.start();
        

        BlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("POST / HTTP/1.1\r\n" + 
                  "User-Agent: me\r\n" + 
                  "Host: localhost:13931\r\n" +
                  "Content-Type: text/plain\r\n" +
                  "Content-Length: 8\r\n" +
                  "\r\n" +
                  "123");
  
        QAUtil.sleep(1000);
        con.write("45678");
       
       
        con.readStringByDelimiter("\r\n\r\n");
        con.readStringByLength(8);
        
        con.close();
        server.close();
    }
    
    
    @Test
    public void testSimple() throws Exception {
        
        IServer server = new HttpServer(new EchoHandler());
        server.start();
        

        HttpClient httpClient = new HttpClient();
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        BodyDataSink ds = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort()+ "/", "text/plain"), 8, respHdl);
        ds.write("123");
        
        QAUtil.sleep(1000);
        ds.write("45678");
        
        IHttpResponse response = respHdl.get();
        Assert.assertEquals(200, response.getStatus());
        
        String body = response.getBody().readString();
        if (body.indexOf("12345678") == -1) {
            System.out.println("got wrong content " + body);
            Assert.fail();
        }
        
        httpClient.close();
        server.close();
    }    

    
    
	private static final class EchoHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			IHttpRequest request = exchange.getRequest();
			
			HttpResponse response = null;
			if (request.hasBody()) {
				response = new HttpResponse(200, "text/plain", exchange.getRequest().getNonBlockingBody());
			} else {
				response = new HttpResponse(200);
			}
				
			for (String headerName : exchange.getRequest().getHeaderNameSet()) {
				if (headerName.startsWith("X")) {
					response.setHeader(headerName, exchange.getRequest().getHeader(headerName));
				}
			}
			
			exchange.send(response);
		}
	}
}