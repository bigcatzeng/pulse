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
public final class HttpClientWebSocketCookiesTest  {
	
    
    @Test
    public void testSimple() throws Exception {
        
        HttpServer server = new HttpServer(new MixedRequestHandler());
        server.start();
        HttpClient httpClient = new HttpClient();
        httpClient.setAutoHandleCookies(true);
        
        
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        
        IWebSocketConnection webSocketConnection = httpClient.openWebSocketConnection("ws://localhost:" +  server.getLocalPort());
        
        
        httpClient.close();
        server.close();
    } 
    
    private static final class MixedRequestHandler implements IHttpRequestHandler, IWebSocketHandler {
        
        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
            IHttpRequest request = exchange.getRequest();
            
            IHttpSession session = exchange.getSession(true);
            session.setAttribute("sid", "3332");
           
            exchange.send(new HttpResponse(200, "text/plain", "OK"));
        }

        
        public void onConnect(IWebSocketConnection con) throws IOException {
            System.out.println(con.getUpgradeRequestHeader());
            con.writeMessage(new TextMessage("Hello you"));
        }
        
        public void onDisconnect(IWebSocketConnection con) throws IOException {
            System.out.println("on disconnect");            
        }
        
        
        public void onMessage(IWebSocketConnection con) throws IOException {
            WebSocketMessage msg = con.readMessage();
            con.writeMessage(msg);
        }
    }     
}