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
package org.xlightweb.websocket;





import java.io.IOException;




import java.util.Date;
import java.util.logging.Level;


import org.xlightweb.GetRequest;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IWebSocketConnection;
import org.xlightweb.IWebSocketHandler;
import org.xlightweb.TextMessage;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;





/**  
*
* @author grro@xlightweb.org
*/
public final class SimpleWebSocketsServerExample2WithCookies   {
	
    public static void main(String[] args) throws IOException {
        
        class ServerHandler implements IHttpRequestHandler, IWebSocketHandler {

            public void onRequest(IHttpExchange exchange) throws IOException {
                String requestURI = exchange.getRequest().getRequestURI();
                
                if (requestURI.equals("/WebSocketsExample")) {
                    exchange.getSession(true);
                    sendWebSocketPage(exchange, requestURI);
                    
                } else {
                    exchange.sendError(404);
                }
            }
            
            
            private void sendWebSocketPage(IHttpExchange exchange, String uri) throws IOException {
                String page = "<html>\r\n " +
                              "  <head>\r\n" + 
                              "     <script type='text/javascript'>\r\n" + 
                              "        ws = new WebSocket('ws://" + exchange.getRequest().getHost() + "/Channel', 'mySubprotocol');\r\n" +
                              "        ws.onmessage = function (message) {\r\n" +
                              "          messages = document.getElementById('messages');\r\n" + 
                              "          messages.innerHTML += \"<br>[in] \" + message.data;\r\n"+
                              "        };\r\n" +
                              "        \r\n" +
                              "        sendmsg = function() {\r\n" +
                              "          message = document.getElementById('message_to_send').value\r\n" +
                              "          document.getElementById('message_to_send').value = ''\r\n" +
                              "          ws.send(message);\r\n" + 
                              "          messages = document.getElementById('messages');\r\n" +
                              "          messages.innerHTML += \"<br>[out] \" + message;\r\n"+
                              "        };\r\n" +
                              "     </script>\r\n" +
                              "  </head>\r\n" +
                              "  <body>\r\n" +
                              "     <form>\r\n" +
                              "       <input type=\"text\" id=\"message_to_send\" name=\"msg\"/>\r\n" +
                              "       <input type=\"button\" name=\"btn\" id=\"sendMsg\" value=\"Send\" onclick=\"javascript:sendmsg();\">\r\n" +
                              "       <div id=\"messages\"></div>\r\n" + 
                              "     </form>\r\n" +
                              "  </body>\r\n" + 
                              "</html>\r\n ";
                
                exchange.send(new HttpResponse(200, "text/html", page));
            }

            
            
            public void onConnect(IWebSocketConnection webStream) throws IOException {
                
            }

            
            public void onMessage(IWebSocketConnection webStream) throws IOException {
                TextMessage msg = webStream.readTextMessage();
                if (msg.toString().equalsIgnoreCase("GetDate")) {
                    webStream.writeMessage(new TextMessage(new Date().toString()));
                } else {
                    webStream.writeMessage(new TextMessage("unknown command (supported: GetDate)"));
                }
            }
            
            
            public void onDisconnect(IWebSocketConnection webStream) throws IOException {
                
            }
        }

        
        HttpServer server = new HttpServer(8876, new ServerHandler());
        server.start();
        
        
        HttpClient httpClient = new HttpClient();

        httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/WebSocketsExample"));
        
        IWebSocketConnection wsCon = httpClient.openWebSocketConnection("ws://localhost:" + server.getLocalPort() + "/WebSocketsExample", "mySubprotocol");
        
        wsCon.writeMessage(new TextMessage("GetDate"));
        TextMessage msg = wsCon.readTextMessage(); 
        
        
        
        httpClient.close();
        
    }        
}