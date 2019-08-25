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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


import junit.framework.Assert;


import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;





/**  
*
* @author grro@xlightweb.org
*/
public final class ServerSentEventHandlerTest   {
	
    
    @Test
    public void testSync() throws Exception {
        
        class MyHttpRequestHandler implements IHttpRequestHandler {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                
                IHttpRequest request = exchange.getRequest();
                
                if (request.getAccept().contains(new ContentType("text/event-stream"))) {
                    BodyDataSink ds = exchange.send(new HttpResponseHeader(200, "text/event-stream"));
                    
                    Event event = new Event();
                    event.setComment("test stream");
                    ds.write(event.toString());
                    ds.write(new Event("first event", "1").toString());
                    ds.write(new Event("second event", "2").toString());
                    
                    ds.close();
                    
                } else {
                    exchange.forward(request);
                }
                
            }
        };
        
        HttpServer server = new HttpServer(new MyHttpRequestHandler());
        server.start();
        
        
        HttpClient client = new HttpClient();
        IEventDataSource eventSource = client.openEventDataSource("http://localhost:" + server.getLocalPort() + "/Events", false);

        Assert.assertEquals("test stream", eventSource.readMessage().getComment());
        Assert.assertEquals("first event", eventSource.readMessage().getData());
        Assert.assertEquals("second event", eventSource.readMessage().getData());
        
        eventSource.close();
        
        client.close();
        server.close();
    }
    
    @Test
    public void testAsync() throws Exception {
        
        IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                
                BodyDataSink ds = exchange.send(new HttpResponseHeader(200, "text/event-stream"));
                
                Event event = new Event();
                event.setComment("test stream");
                ds.write(event.toString());
                ds.write(new Event("first event", "1").toString());
                ds.write(new Event("second event", "1").toString());
            }
        };
        
        HttpServer server = new HttpServer(hdl);
        server.start();
        
        
        HttpClient client = new HttpClient();
        
        EventHandler eventHandler = new EventHandler();
        client.openEventDataSource("http://localhost:" + server.getLocalPort() + "/Events", false, eventHandler);
        
        QAUtil.sleep(1000);
        
        
        List<Event> webEvents = eventHandler.getWebEvents();
        Assert.assertEquals("test stream", webEvents.get(0).getComment());
        Assert.assertEquals("first event", webEvents.get(1).getData());
        Assert.assertEquals("second event", webEvents.get(2).getData());

        
        client.close();
        server.close();
    }    
    
    
   
    private static final class EventHandler implements IEventHandler {
        
        private final AtomicBoolean isDisconnected = new AtomicBoolean(false);
        private final List<Event> webEvents = Collections.synchronizedList(new ArrayList<Event>());

        public void onConnect(IEventDataSource webEventDataSource) throws IOException {
            
        }
        
        public void onMessage(IEventDataSource webEventDataSource) throws IOException {
            Event event = webEventDataSource.readMessage();
            webEvents.add(event);
        }
        
        public void onDisconnect(IEventDataSource webEventDataSource) throws IOException {
            isDisconnected.set(true);
        }
        
        boolean isDisconnected() {
            return isDisconnected.get();
        }
        
        List<Event> getWebEvents() {
            return webEvents;
        }
    }
}