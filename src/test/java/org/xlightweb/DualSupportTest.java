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
public final class DualSupportTest  {
	
    
    @Test
    public void testWebSockets() throws Exception {
        
        HttpServer server = new HttpServer(new DualHandler());
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        IWebSocketConnection webSocketConnection = httpClient.openWebSocketConnection("ws://localhost:" +  server.getLocalPort());
        
        webSocketConnection.writeMessage(new TextMessage("GetData"));
        WebSocketMessage msg = webSocketConnection.readMessage();
        Assert.assertEquals("id: 556\r\ndata: 566;555\r\n\r\n", msg.toString());
        System.out.println(msg);

        webSocketConnection.writeMessage(new TextMessage("GetData"));
        msg = webSocketConnection.readMessage();
        Assert.assertEquals("id: 557\r\ndata: 567;556\r\n\r\n", msg.toString());
        System.out.println(msg);
        
        webSocketConnection.close();
        httpClient.close();
        server.close();
    } 
    
   

    @Test
    public void testSSE() throws Exception {
        
        HttpServer server = new HttpServer(new DualHandler());
        server.start();
        
        
        HttpClient httpClient = new HttpClient();
        
        IEventDataSource eventSource = httpClient.openEventDataSource("http://localhost:" +  server.getLocalPort() + "/", false);
        
        Event event = eventSource.readMessage();
        Assert.assertEquals(": keep-alive\r\n\r\n", event.toString());
        System.out.println(event);
        
        event = eventSource.readMessage();
        Assert.assertEquals("id: 556\r\ndata: 566;555\r\n\r\n", event.toString());
        System.out.println(event);
        
        eventSource.close();
        
        httpClient.close();
        
        server.close();
    } 
  

    private static final class DualHandler implements IHttpRequestHandler, IWebSocketHandler {
        
        
        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
            
            BodyDataSink sink = exchange.send(new HttpResponseHeader(200, "text/event-stream"));
            
            Event event = new Event();
            event.setComment("keep-alive");
            sink.write(event.toString());
            
            QAUtil.sleep(100);
            
            event = new Event(566 + ";" + 555, 556);
            sink.write(event.toString());
        }


        public void onMessage(IWebSocketConnection webStream) throws IOException {
            
            Event event = new Event(566 + ";" + 555, 556);
            webStream.writeMessage(new TextMessage(event.toString()));

            QAUtil.sleep(100);

            event = new Event(567 + ";" + 556, 557);
            webStream.writeMessage(new TextMessage(event.toString()));           
        }
        
        
        public void onConnect(IWebSocketConnection webStream) throws IOException {
        }
        
        public void onDisconnect(IWebSocketConnection webStream) throws IOException {
        }
    }
    
}