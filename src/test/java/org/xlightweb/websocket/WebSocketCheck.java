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




import org.xlightweb.BadMessageException;
import org.xlightweb.ContentType;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IWebSocketConnection;
import org.xlightweb.IWebSocketHandler;
import org.xlightweb.QAUtil;
import org.xlightweb.TextMessage;
import org.xlightweb.server.HttpServer;





/**  
 *
 */
public final class WebSocketCheck   {
    
    
    public static void main(String[] args) throws IOException {
        QAUtil.setLogLevel("org.xsocket", Level.FINE);
        
        HttpServer server = new HttpServer(8876, new WebSocketHandler());
        System.out.println("http://localhost:8876/websocket.html");
        
        server.run();
    }
    
    
    
    public static class WebSocketHandler implements IWebSocketHandler, IHttpRequestHandler {

        public void onConnect(IWebSocketConnection webSocketConnection) throws IOException {
            System.out.println("WebSockets.onConnect(IWebSocketConnection)");
        }

        public void onMessage(IWebSocketConnection webSocketConnection) throws IOException {
            String message = webSocketConnection.readTextMessage().toString();
            System.out.println("received ws message " + message);
            webSocketConnection.writeMessage(new TextMessage(message + " " + new Date().toString()));
        }

        public void onDisconnect(IWebSocketConnection webSocketConnection) throws IOException {
            System.out.println("WebSockets.onDisconnect(IWebSocketConnection)");
        }

        public void onRequest(IHttpExchange httpExchange) throws IOException, BadMessageException {
            System.out.println("WebSockets.onRequest(IHttpExchange)");
        
            IHttpRequest httpRequest = httpExchange.getRequest();
            if (httpRequest.getAccept().contains(new ContentType("text/html"))) {
                String requestURI = httpRequest.getRequestURI();
                
                if (requestURI.equals("/websocket.html")) {
                    String page =   " <html> \r\n" +
                                    "   <head> \r\n" +
                                    "     <script type='text/javascript'> \r\n" +
                                    "       var socket = new WebSocket('ws://localhost:8876/services');\r\n" +
                                    "       socket.onopen = function() {\r\n" +
                                    "          setInterval(function() { if (socket.bufferedAmount == 0)\r\n" +
                                    "          socket.send('1'); }, 1000);\r\n" +
                                    "       };\r\n" +
                                    "       socket.onmessage = function(message) { \r\n" +
                                    "          var messages = document.getElementById('messages');\r\n" + 
                                    "          messages.innerHTML += \"<br>[ws] \" + message.data;\r\n"+
                                    "       }; \r\n" +                    
                                    "     </script> \r\n" +
                                    "   </head> \r\n" +
                                    "   <body> \r\n" +
                                    "     <div id=\"messages\"></div> \r\n" +
                                    "   </body> \r\n" +
                                    " </html> \r\n";
        
                    HttpResponse httpResponse = new HttpResponse(200, "text/html", page);
                    httpExchange.send(httpResponse);
                } else {
                    httpExchange.sendError(404);
                }
            } else {
                httpExchange.sendError(404);
            }
        }
    }        
}