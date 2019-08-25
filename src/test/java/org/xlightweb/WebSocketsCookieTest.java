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
public final class WebSocketsCookieTest  {
	
    @Test
    public void testSimple() throws Exception {
             
        Context context = new Context("");
        context.addHandler("/srv/*", new MyRequestHandler());
        context.addHandler("/ws/*", new MyWebSocketHandler());

        HttpServer server = new HttpServer(context);
        server.start();
        
        
        HttpClient httpClient = new HttpClient();
        
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/srv/test"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("OK", response.getBody().readString());

        
        response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/ws/test"));
        Assert.assertEquals(404, response.getStatus());

        
        IWebSocketConnection wsCon = httpClient.openWebSocketConnection("http://localhost:" + server.getLocalPort() + "/ws/test");
        
        wsCon.writeMessage(new TextMessage("Hello"));
        Assert.assertEquals("Hello", wsCon.readMessage().toString());
        
        wsCon.close();
        
        httpClient.close();
        server.close();
    }
    
    
    private static class MyRequestHandler implements IHttpRequestHandler {
        
        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
            IHttpSession s = exchange.getSession(true);
            s.setAttribute("accountid", 43534533);
            
            exchange.send(new HttpResponse(200, "text/plain", "OK"));
        }
    }

    private static final class MyWebSocketHandler implements IWebSocketHandler {
        
        public void onConnect(IWebSocketConnection con) throws IOException, UnsupportedProtocolException {
            if (!con.getUpgradeRequestHeader().getHeader("Cookie").startsWith("JSESSIONID=")) {
                throw new IOException("cookie is not set");
            }
        }
        
        public void onMessage(IWebSocketConnection con) throws IOException {
            con.writeMessage(con.readMessage());
        }       
        
        public void onDisconnect(IWebSocketConnection con) throws IOException {
            
        }
    }
 
}