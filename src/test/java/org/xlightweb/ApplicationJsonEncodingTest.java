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
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.Server;


/**
*
* @author grro@xlightweb.org
*/
public final class ApplicationJsonEncodingTest {


	@Test
	public void testSimple() throws Exception {
		
		HttpServer server = new HttpServer(new Handler()); 
		server.start();
		
		HttpClient httpClient = new HttpClient();
		
		IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort() + "/", "application/json", "erwwr"));
		
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("OK", response.getBody().readString());
		Assert.assertEquals("utf-8", response.getCharacterEncoding());
		
		httpClient.close();
		server.close();
	}

	
    @Test
    public void testSimple2() throws Exception {
        
        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection con) throws IOException {
                con.readStringByDelimiter("\r\n\r\n");
                con.write("HTTP/1.1 200 ok\r\n" + 
                          "Server: me\r\n" +
                          "Content-Type: application/json\r\n" +
                          "Content-Length: 3\r\n" +
                          "\r\n" +
                          "123");
                return false;
            }
        };
        
        Server server = new Server(dh); 
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort() + "/", "application/json", "erwwr"));
        
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("123", response.getBody().readString());
        Assert.assertEquals("utf-8", response.getCharacterEncoding());
        
        httpClient.close();
        server.close();
    }


	
	private static final class Handler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {

		    if (exchange.getRequest().getContentType().equalsIgnoreCase("application/json; charset=utf-8")) {
		        exchange.send(new HttpResponse(200, "application/json", "OK"));
		    } else {
		        exchange.sendError(400);
		    }
		}
	}
	
	
	
}
