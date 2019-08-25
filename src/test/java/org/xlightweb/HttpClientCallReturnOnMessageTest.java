/*
 *  Copyright (c) xlightweb.org, 2006 - 2009. All rights reserved.
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
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;



/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientCallReturnOnMessageTest  {


	
    @Test
    public void testReturnOnHeader() throws Exception {

        RequestHandler hdl = new RequestHandler();
        HttpServer server = new HttpServer(hdl);
        server.start();

        HttpClient httpClient = new HttpClient();
        
        long start = System.currentTimeMillis();
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test?delay=2000"));
        
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("elapsed " + elapsed);
        
        Assert.assertTrue(elapsed < 2000);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertFalse(response.getNonBlockingBody().isCompleteReceived());
        
        httpClient.close();
        server.close();
    }
	

    
    @Test
    public void testReturnOnMessage() throws Exception {

        RequestHandler hdl = new RequestHandler();
        HttpServer server = new HttpServer(hdl);
        server.start();

        HttpClient httpClient = new HttpClient();
        httpClient.setCallReturnOnMessage(true);
        
        long start = System.currentTimeMillis();
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test?delay=2000"));
        
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("elapsed " + elapsed);
        
        Assert.assertTrue(elapsed >= 2000);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.getNonBlockingBody().isCompleteReceived());
        
        httpClient.close();
        server.close();
    }

    
    @Test
    public void testReturnOnMessageRetry() throws Exception {
        
        
        IDataHandler dh = new IDataHandler() {

            private int counter = 0; 
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                connection.readStringByDelimiter("\r\n\r\n");

                counter++;
                System.out.println(counter + " call");

                if (counter < 3) {
                    connection.write("HTTP/1.1 200 OK\r\n" +
                            "Server: me\r\n" + 
                            "Content-length: 5\r\n" +
                            "\r\n" +
                            "123"); 
                    connection.close();
                    
                } else { 
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n" + 
                                     "Content-length: 5\r\n" +
                                     "\r\n" +
                                     "12345"); 
                }    

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();


        HttpClient httpClient = new HttpClient();
        httpClient.setCallReturnOnMessage(true);
        
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.getNonBlockingBody().isCompleteReceived());
        
        httpClient.close();
        server.close();
    }
    
    @Test
    public void testReturnOnMessageRetry2() throws Exception {
        
        
        IDataHandler dh = new IDataHandler() {

            private int counter = 0; 
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                connection.readStringByDelimiter("\r\n\r\n");

                counter++;
                System.out.println(counter + " call");

                if (counter < 3) {
                    connection.write("HTTP/1.1 200 OK\r\n" +
                            "Server: me\r\n" + 
                            "Content"); 
                    connection.close();
                    
                } else { 
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n" + 
                                     "Content-length: 5\r\n" +
                                     "\r\n" +
                                     "12345"); 
                }    

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();


        HttpClient httpClient = new HttpClient();
        httpClient.setCallReturnOnMessage(true);
        
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.getNonBlockingBody().isCompleteReceived());
        
        httpClient.close();
        server.close();
    }    
    
    
    
    @Test
    public void testReturnOnMessageRetryMaxEceeded() throws Exception {
        
        IDataHandler dh = new IDataHandler() {

            private int counter = 0; 
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                connection.readStringByDelimiter("\r\n\r\n");

                counter++;
                System.out.println(counter + " call");

                if (counter < 6) {
                    connection.write("HTTP/1.1 200 OK\r\n" +
                            "Server: me\r\n" + 
                            "Content-length: 5\r\n" +
                            "\r\n" +
                            "123"); 
                    connection.close();
                    
                } else { 
                    connection.write("HTTP/1.1 200 OK\r\n" +
                                     "Server: me\r\n" + 
                                     "Content-length: 5\r\n" +
                                     "\r\n" +
                                     "12345"); 
                }    

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();


        HttpClient httpClient = new HttpClient();
        httpClient.setMaxRetries(4);
        httpClient.setCallReturnOnMessage(true);
        
        try {
            httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
            Assert.fail("IOException expected");
        } catch (IOException expected) { }
        
        
        httpClient.close();
        server.close();
    }
        
    
	
	private static final class RequestHandler implements IHttpRequestHandler {
	    
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        int delay = exchange.getRequest().getIntParameter("delay", 0);
	        
	        BodyDataSink ds = exchange.send(new HttpResponseHeader(200, "text/plain; charset=iso-8859-1"), 2);
	        ds.flush();
	        QAUtil.sleep(delay);
	        
	        ds.write("OK");
	        ds.close();
	    }
	}
	
	
}