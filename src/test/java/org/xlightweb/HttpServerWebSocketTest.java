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




import java.io.File;





import java.io.FileInputStream;
import java.io.OutputStream;

import java.io.IOException;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.connection.BlockingConnection;





/**  
*
* @author grro@xlightweb.org
*/
public final class HttpServerWebSocketTest  {
	


    @Test
    public void testServerMixedHandler() throws Exception {
        HttpServer server = new HttpServer(new MixedRequestHandler());
        server.start();

        BlockingConnection bc = new BlockingConnection("localhost", server.getLocalPort());

        bc.write("GET / HTTP/1.1\r\n"+ 
                 "Host: localhost:" + server.getLocalPort() + "\r\n"+
                 "User-Agent: xLightweb/2.11\r\n"+ 
                 "Upgrade: WebSocket\r\n"+ 
                 "Connection: Upgrade\r\n"+ 
                 "Origin: http://localhost:5161/\r\n"+ 
                 "\r\n");

        bc.readStringByDelimiter("\r\n\r\n");

        
        byte[] msg1 = bc.readBytesByLength(11);
        Assert.assertArrayEquals(new byte[] { 0x00, 0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x20, 0x79, 0x6f, 0x75, (byte) 0xFF}, msg1);

     

        bc.close();
        server.close();
    }     
  
    
    @Test
    public void testServer() throws Exception {
   
        HttpServer server = new HttpServer(new WebHandler());
        server.start();

        BlockingConnection bc = new BlockingConnection("localhost", server.getLocalPort());

        bc.write("GET /0123456 HTTP/1.1\r\n"+ 
                 "Host: localhost:" + server.getLocalPort() + "\r\n"+
                 "User-Agent: xLightweb/2.11\r\n"+ 
                 "Upgrade: WebSocket\r\n"+ 
                 "Connection: Upgrade\r\n"+ 
                 "Origin: http://localhost:5161/\r\n"+ 
                 "\r\n");

        System.out.println(bc.readStringByDelimiter("\r\n\r\n"));
        
        
        byte[] msg1 = bc.readBytesByLength(11);
        Assert.assertArrayEquals(new byte[] { 0x00, 0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x20, 0x79, 0x6f, 0x75, (byte) 0xFF}, msg1);

        bc.close();
        server.close();
    }     
    

    @Test
    public void testClientAndServer() throws Exception {
        HttpServer server = new HttpServer(new MixedRequestHandler());
        server.start();
 
        HttpClient httpClient = new HttpClient();
        
        IWebSocketConnection webSocketConnection = httpClient.openWebSocketConnection("ws://localhost:" + server.getLocalPort());
        
        WebSocketMessage msg = webSocketConnection.readMessage();
        Assert.assertTrue(msg.isTextMessage());
        Assert.assertEquals("Hello you", msg.toString());
        
        webSocketConnection.writeMessage(new TextMessage("01234567890"));
        
        msg = webSocketConnection.readMessage();
        Assert.assertTrue(msg.isTextMessage());
        Assert.assertEquals("01234567890", msg.toString());
        
        httpClient.close();
        server.close();
    } 

    
    @Test
    public void testClientAndServerClose() throws Exception {
        HttpServer server = new HttpServer(new MixedRequestHandler());
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("OK", response.getBody().toString());
        
        
        IWebSocketConnection webSocketConnection = httpClient.openWebSocketConnection("ws://localhost:" + server.getLocalPort());
        
        WebSocketMessage msg = webSocketConnection.readMessage();
        Assert.assertTrue(msg.isTextMessage());
        Assert.assertEquals("Hello you", msg.toString());
        
        webSocketConnection.writeMessage(new TextMessage("01234567890"));
        
        msg = webSocketConnection.readMessage();
        Assert.assertEquals("01234567890", msg.toString("UTF-8"));
   
        
        webSocketConnection.close();
        Assert.assertFalse(webSocketConnection.isOpen());
        
        QAUtil.sleep(500);
        
        httpClient.close();
        server.close();
    } 
    

    @Test
    public void testWrongProtocol() throws Exception {
   
        IWebSocketHandler hdl = new IWebSocketHandler() {
            
            public void onConnect(IWebSocketConnection con) throws IOException, UnsupportedProtocolException {
                throw new UnsupportedProtocolException("protocol xxx expetected ");
            }
            
            public void onDisconnect(IWebSocketConnection con) throws IOException {
            }
            
            public void onMessage(IWebSocketConnection con) throws IOException {
                con.writeMessage(new TextMessage("should not be sent"));
            }
        };
        
        HttpServer server = new HttpServer(hdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        try {
            httpClient.openWebSocketConnection("ws://localhost:" + server.getLocalPort());
            Assert.fail("UnsupportedProtocolException expected");
        } catch (UnsupportedProtocolException expected) { }
                
        httpClient.close();
        server.close();
    } 


    
    
    
    @Test
    public void testClientAndServerSSL() throws Exception {
        HttpServer server = new HttpServer(0, new MixedRequestHandler(), SSLTestContextFactory.getSSLContext(), true);
        server.start();
        
        HttpClient httpClient = new HttpClient(SSLTestContextFactory.getSSLContext());
        
        IWebSocketConnection webSocketConnection = httpClient.openWebSocketConnection("wss://localhost:" + server.getLocalPort());
        
        WebSocketMessage msg = webSocketConnection.readMessage();
        Assert.assertTrue(msg.isTextMessage());
        Assert.assertEquals("Hello you", msg.toString());
        
        webSocketConnection.writeMessage(new TextMessage("01234567890"));
        
        msg = webSocketConnection.readMessage();
        Assert.assertTrue(msg.isTextMessage());
        Assert.assertEquals("01234567890", msg.toString());
        
        httpClient.close();
        server.close();
    } 
   


    private static final class MixedRequestHandler implements IHttpRequestHandler, IWebSocketHandler {
        
        @Execution(Execution.NONTHREADED)
        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
            IHttpRequest request = exchange.getRequest();
        
            if (request.getHeader("Upgrade") != null)  {
                System.out.println("!!send response for request " + request);
            }
            exchange.send(new HttpResponse(200, "text/plain", "OK"));
        }

        
        @Execution(Execution.NONTHREADED)
        public void onConnect(IWebSocketConnection con) throws IOException {
            String location = con.getWebSocketLocation();
       
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
     
    
    private static final class WebHandler implements IWebSocketHandler {
        
        @Execution(Execution.NONTHREADED)
        public void onConnect(IWebSocketConnection con) throws IOException {
            String loc = con.getWebSocketLocation();
            int idx = loc.lastIndexOf("/");
            String id = loc.substring(idx + 1, loc.length());
            
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
    
    
    public static final class EchoWebSocketServlet extends WebSocketServlet {
        
        private static final long serialVersionUID = 1744596653631618106L;
        
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            getServletContext().getNamedDispatcher("default").forward(request,response);
        }
        
        protected WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
            return new EchoWebSocket();
        }
        
        class EchoWebSocket implements WebSocket {
            Outbound _outbound;

            public void onConnect(Outbound outbound) {
                _outbound = outbound; 
                
                try {
                    outbound.sendMessage(SENTINEL_FRAME, "Hello you");
                } catch(IOException e) {
                     e.printStackTrace();
                }
            }
            
            public void onMessage(byte frame, byte[] data, int offset, int length) {
                try {
                    _outbound.sendMessage(frame, data, offset, length);
                } catch(IOException e) {
                    e.printStackTrace();
               }
            }

            public void onMessage(byte frame, String data) {
                
                try {
                    _outbound.sendMessage(frame, data);
                 } catch(IOException e) {
                     e.printStackTrace();
                }
            }

            public void onDisconnect() {
             
            }
        }
    }
    
    
    public static final class ChatServlet extends WebSocketServlet {
        
        private static final long serialVersionUID = 1744596653631618106L;
        
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            
            if (request.getRequestURI().startsWith("/ws.html")) {
                response.setContentType("text/html");

                OutputStream os = response.getOutputStream();
                FileInputStream fis = new FileInputStream("src" + File.separator + "test" + File.separator + "resources" + File.separator + "org" + File.separator + "xlightweb" + File.separator + "ws.html");
                IOUtils.copy(fis, os);
                os.close();
                fis.close();

                
            } else if (request.getRequestURI().startsWith("/ws.js")) {
                response.setContentType("text/javascript");
                
                OutputStream os = response.getOutputStream();
                FileInputStream fis = new FileInputStream("src" + File.separator + "test" + File.separator + "resources" + File.separator + "org" + File.separator + "xlightweb" + File.separator + "ws.js");
                IOUtils.copy(fis, os);
                os.close();
                fis.close();

            } else {
                response.sendError(404);
            }
        }
        
        protected WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
            return new EchoWebSocket();
        }
        
        class EchoWebSocket implements WebSocket {
            Outbound _outbound;

            public void onConnect(Outbound outbound) {
                _outbound = outbound; 
                
                try {
                    outbound.sendMessage(SENTINEL_FRAME, "Hello jetty");
                } catch(IOException e) {
                     e.printStackTrace();
                }
            }
            
            public void onMessage(byte frame, byte[] data, int offset, int length) {
                try {
                    _outbound.sendMessage(frame, data, offset, length);
                } catch(IOException e) {
                    e.printStackTrace();
               }
            }

            public void onMessage(byte frame, String data) {
                
                try {
                    _outbound.sendMessage(frame, data);
                 } catch(IOException e) {
                     e.printStackTrace();
                }
            }

            public void onDisconnect() {
             
            }
        }
    }   

}