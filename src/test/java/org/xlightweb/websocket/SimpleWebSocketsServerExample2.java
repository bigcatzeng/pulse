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

import junit.framework.Assert;


import org.xlightweb.BadMessageException;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpRequestHeader;
import org.xlightweb.IWebSocketConnection;
import org.xlightweb.IWebSocketHandler;
import org.xlightweb.TextMessage;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;





/**  
*
* @author grro@xlightweb.org
*/
public final class SimpleWebSocketsServerExample2   {
	
    public static void main(String[] args) throws IOException {
                
        class ServerHandler implements IHttpRequestHandler, IWebSocketHandler {
            
            
            // IHttpRequestHandler method
            public void onRequest(IHttpExchange exchange) throws IOException {
                String requestURI = exchange.getRequest().getRequestURI();
                
                if (requestURI.equals("/WebSocketsExample")) {
                    sendWebSocketPage(exchange, requestURI);
                    
                } else {
                    exchange.sendError(404);
                }
            }
            
            
            private void sendWebSocketPage(IHttpExchange exchange, String uri) throws IOException {
                String page = "<html>\r\n " +
                              "  <head>\r\n" + 
                              "     <script type='text/javascript'>\r\n" + 
                              "        var ws = new WebSocket('ws://" + exchange.getRequest().getHost() + "/Channel', 'mySubprotocol.example.org');\r\n" +
                              "        ws.onmessage = function (message) {\r\n" +
                              "          var messages = document.getElementById('messages');\r\n" + 
                              "          messages.innerHTML += \"<br>[in] \" + message.data;\r\n"+
                              "        };\r\n" +
                              "        \r\n" +
                              "        sendmsg = function() {\r\n" +
                              "          var message = document.getElementById('message_to_send').value\r\n" +
                              "          document.getElementById('message_to_send').value = ''\r\n" +
                              "          ws.send(message);\r\n" + 
                              "          var messages = document.getElementById('messages');\r\n" +
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

            
            
            // IWebSocketHandler method
            public void onConnect(IWebSocketConnection webStream) throws IOException, BadMessageException {
                IHttpRequestHeader header = webStream.getUpgradeRequestHeader();

                // check origin header
                String origin = header.getHeader("Origin");
                if (!isAllowed(origin)) {
                    throw new BadMessageException(403);
                }
                
                // check the subprotocol  
                String subprotocol = header.getHeader("WebSocket-Protocol", "");
                if (!subprotocol.equalsIgnoreCase("mySubprotocol.example.org")) {
                    throw new BadMessageException(501);
                }
            }

            private boolean isAllowed(String origin) {
                // check the origin
                // ...
                return true;
            }
            
            
            // IWebSocketHandler
            public void onMessage(IWebSocketConnection webStream) throws IOException {
                TextMessage msg = webStream.readTextMessage();
                if (msg.toString().equalsIgnoreCase("GetDate")) {
                   webStream.writeMessage(new TextMessage(new Date().toString()));
                } else {
                    webStream.writeMessage(new TextMessage("unknown command (supported: GetDate)"));
                }
            }
            
            // IWebSocketHandler
            public void onDisconnect(IWebSocketConnection webStream) throws IOException {  }
        }
        
        HttpServer server = new HttpServer(8876, new ServerHandler());
        server.start();
        

        
    }        
}