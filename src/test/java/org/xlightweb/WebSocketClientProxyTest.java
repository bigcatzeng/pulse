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

import java.net.URL;

import junit.framework.Assert;

import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;



/**  
*
* @author grro@xlightweb.org
*/
public final class WebSocketClientProxyTest  {
	
    
    @Test
    public void testWithProxy() throws Exception {
        
        HttpClient httpClient = new HttpClient();
        
        HttpServer server = new HttpServer(new WSHandler());
        server.start();
        
        
        HttpServer proxy = new HttpServer(new ProxyHandler(httpClient));
        proxy.start();
        
        httpClient.setProxyHost("localhost");
        httpClient.setProxyPort(proxy.getLocalPort());
        
        IWebSocketConnection wsCon = httpClient.openWebSocketConnection("ws://localhost:" + server.getLocalPort() + "/");
        
        wsCon.writeMessage(new TextMessage("0123456789"));
        Assert.assertEquals("0123456789", wsCon.readMessage().toString());
        
        
        httpClient.close();
        proxy.close();
        server.close();
    }   
    

    
    private class WSHandler implements IWebSocketHandler {
        
        public void onConnect(IWebSocketConnection con) throws IOException, UnsupportedProtocolException {
        }
        
        public void onDisconnect(IWebSocketConnection con) throws IOException {
            
        }
        
        public void onMessage(IWebSocketConnection con) throws IOException {
            WebSocketMessage msg = con.readMessage();
            con.writeMessage(msg);
        }
    }
    
    
    

    private static final class ProxyHandler implements IHttpRequestHandler {

        private final HttpClient httpClient;
        
        
        ProxyHandler(HttpClient httpClient) {
            this.httpClient = httpClient;
        }
        

        public void onRequest(final IHttpExchange exchange) throws IOException {

            IHttpRequest req = exchange.getRequest();

            // connect request?
            if (req.getMethod().equalsIgnoreCase("CONNECT")) {
                HttpUtils.establishTcpTunnel(exchange.getConnection(), req.getRequestURI());
                            
                IHttpResponse response = new HttpResponse(200);
                response.getResponseHeader().setReason("Connection established");
                response.setHeader("Proxy-agent", "myProxy");
                exchange.send(response);
                            
                
            // .. no
            } else {                
                String path = req.getRequestUrl().getFile();
                URL target = new URL(path);
                
                req.setRequestUrl(target);

                IHttpResponseHandler respHdl = new IHttpResponseHandler() {
                    
                    public void onResponse(IHttpResponse response) throws IOException {
                        if (response.getStatus() > 199) {
                            response.addHeader("Via", "myProxy");
                        }
                        
                        //  return the response 
                        exchange.send(response);
                    }
                    
                    public void onException(IOException ioe) throws IOException {
                        exchange.sendError(500, ioe.toString());
                    }
                };

                httpClient.send(req, respHdl);
            }
        }
    }
}