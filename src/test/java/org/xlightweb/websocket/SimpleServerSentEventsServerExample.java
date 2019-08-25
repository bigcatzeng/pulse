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





import java.io.IOException
;


import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.xlightweb.BodyDataSink;
import org.xlightweb.Event;
import org.xlightweb.HttpResponse;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.server.HttpServer;






/**  
*
* @author grro@xlightweb.org
*/
public final class SimpleServerSentEventsServerExample   {
	
    public static void main(String[] args) throws IOException {
        
        class ServerHandler implements IHttpRequestHandler {

            private final Timer timer = new Timer(false);
            
            public void onRequest(IHttpExchange exchange) throws IOException {
                String requestURI = exchange.getRequest().getRequestURI();
                
                if (requestURI.equals("/Events")) {
                    sendEventStream(exchange);
                    
                } else {
                    sendStaticContent(exchange, requestURI);
                }
            }
            
            
            private void sendStaticContent(IHttpExchange exchange, String uri) throws IOException {
                String page = "<html>\r\n " +
                              "  <head>\r\n" + 
                              "     <script type='text/javascript'>\r\n" + 
                              "        var source = new EventSource('Events');\r\n" +
                              "        source.onmessage = function (event) {\r\n" +
                              "          var ev = document.getElementById('events');\r\n" + 
                              "          ev.innerHTML += \"data: \" + event.data;\r\n"+
                              "        };\r\n" +
                              "     </script>\r\n" +
                              "  </head>\r\n" +
                              "  <body>\r\n" +
                              "    Events:\r\n" + 
                              "    <div id=\"events\"></div>\r\n" + 
                              "  </body>\r\n" + 
                              "</html>\r\n ";
                
                exchange.send(new HttpResponse(200, "text/html", page));
            }
            
            
            private void sendEventStream(final IHttpExchange exchange) throws IOException {

                // sending the response header
                final BodyDataSink sink = exchange.send(new HttpResponseHeader(200, "text/event-stream"));

                
                TimerTask tt = new TimerTask() {

                    private int id = Integer.parseInt(exchange.getRequest().getHeader("Last-Event-Id", "0"));
                    
                    public void run() {
                        try {
                            Event event = new Event(new Date().toString(), ++id);
                            sink.write(event.toString());
                        } catch (IOException ioe) {
                            cancel();
                            sink.destroy();
                        }
                    };
                };
                
                Event event = new Event();
                event.setRetryMillis(5 * 1000);
                sink.write(event.toString());
                
                timer.schedule(tt, 3000, 3000);
            }
        }

        
        
        HttpServer server = new HttpServer(8875, new ServerHandler());
        server.start();
    }        
}