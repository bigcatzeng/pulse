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



import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import junit.framework.Assert;



import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;





/**  
*
* @author grro@xlightweb.org
*/
public final class ServerSentEventsTest   {
    
    
    public static void main(String[] args) throws Exception {
        QAUtil.setLogLevel(Level.FINE);
        
        for (int i = 0; i < 1000; i++) {
            new ServerSentEventsTest().testReconnect();
        }
    }
	
    
    @Test
    public void testSimple() throws Exception {
        
        IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                
                BodyDataSink ds = exchange.send(new HttpResponseHeader(200, "text/event-stream"));
                
                ds.write("data: first line \ndata: second line \n\n");
                
                ds.write(": test stream \n\n");
                
                ds.write(" : test stream \n\n");

                ds.write("data: very first event \n\n");
                
                ds.write("  data: very second event\n\n");
                
                ds.write("data  :very third event\n\n");
                
                ds.write("event: test event\n data: first line\ndata: second line \n\n");

            }
        };
        
        HttpServer server = new HttpServer(hdl);
        server.start();
        
        
        HttpClient client = new HttpClient();
        
        GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/Events");
        request.setHeader("Accept", "text/event-stream");
        
        IHttpResponse response = client.call(request);
        BodyDataSource body = response.getBody(); 
        
        Assert.assertEquals(200, response.getStatus());
        
        Event event = Event.parse(body.readStringByDelimiter("\n\n"));
        Assert.assertEquals("first line second line ", event.getData());

        event = Event.parse(body.readStringByDelimiter("\n\n"));
        Assert.assertEquals("test stream ", event.getComment());

        event = Event.parse(body.readStringByDelimiter("\n\n"));
        
        event = Event.parse(body.readStringByDelimiter("\n\n"));
        Assert.assertEquals("very first event ", event.getData());
        
        event = Event.parse(body.readStringByDelimiter("\n\n"));
        Assert.assertEquals("very second event", event.getData());
        
        event = Event.parse(body.readStringByDelimiter("\n\n"));
        Assert.assertEquals("very third event", event.getData());
        
        event = Event.parse(body.readStringByDelimiter("\n\n"));
        Assert.assertEquals("test event", event.getEventname());
        Assert.assertEquals("first linesecond line ", event.getData());
    }
    
    
 
    @Test
    public void testReconnect() throws Exception {
        
        IHttpRequestHandler hdl = new IHttpRequestHandler() {
            
            private AtomicInteger numRequests = new AtomicInteger(0);
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                
                BodyDataSink ds = exchange.send(new HttpResponseHeader(200, "text/event-stream"));
                
                ds.write("data: " + numRequests.incrementAndGet() + "\n");
                ds.write("id: 4\n");
                
                if (numRequests.get() == 1) {
                    ds.destroy();
                } else {
                    ds.write("\n");
                }
            }
        };
        
        HttpServer server = new HttpServer(hdl);
        server.start();
        
        
        HttpClient client = new HttpClient();
        
        IEventDataSource ds = client.openEventDataSource("http://localhost:" + server.getLocalPort() + "/Events");
        ds.setReconnectionTimeMillis(1000);
        
        Event ev = ds.readMessage();
        Assert.assertEquals("2", ev.getData());
        
        Assert.assertEquals(1, ds.getNumReconnects());
        
        client.close();
        server.close();
    }
}