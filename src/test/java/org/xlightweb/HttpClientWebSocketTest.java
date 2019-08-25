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
import org.junit.Ignore;
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
public final class HttpClientWebSocketTest  {
	
    
    public static void main(String[] args) throws Exception {
        
        WebSocketConnection webSocketConnection = new WebSocketConnection("ws://localhost:9988");

        
        webSocketConnection.writeMessage(new TextMessage("Hello you"));
        
        TextMessage response = webSocketConnection.readTextMessage();
        System.out.println(response);

    } 
  

    
    @Test
    public void testClientHandler() throws Exception {
        WebContainer webContainer = new WebContainer(new EchoWebSocketServlet());
        webContainer.start();
        
        HttpClient httpClient = new HttpClient();
        
        
        class MyWebSocketHandler implements IWebSocketHandler {
            
            public void onConnect(IWebSocketConnection con) throws IOException, UnsupportedProtocolException {
                String webSocketProtocol = con.getProtocol();
                IHttpRequestHeader header = con.getUpgradeRequestHeader();
            }
            
            public void onMessage(IWebSocketConnection con) throws IOException {
                WebSocketMessage msg = con.readMessage();
            }
            
            public void onDisconnect(IWebSocketConnection con) throws IOException {
                
            }
        }
        
        IWebSocketConnection webSocketConnection = httpClient.openWebSocketConnection("ws://localhost:" +  webContainer.getLocalPort(), new MyWebSocketHandler());

        webSocketConnection.writeMessage(new TextMessage("Hello jetty"));

        
        httpClient.close();
        webContainer.stop();
    } 
    
   

    @Ignore
    @Test
    public void testClientLive() throws Exception {
        WebSocketConnection webSocketConnection = new WebSocketConnection("ws://websockets.org:8787");
        
        String txt = new String(QAUtil.generateByteArray(1000));
        webSocketConnection.writeMessage(new TextMessage(txt));
        
        TextMessage msg = webSocketConnection.readTextMessage();
        Assert.assertEquals("echo: " +txt, msg.toString());
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
    public void testProtocolError() throws Exception {
        
        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                String header = connection.readStringByDelimiter("\r\n\r\n");
                
                connection.write("HTTP/1.1 101 Web Socket Protocol Handshake\r\n" +
                                 "Upgrade: WebSocket\r\n" + 
                                 "Connection: Upgrade\r\n" + 
                                 "WebSocket-Origin: http://localhost:25767/\r\n" + 
                                 "WebSocket-Location: ws://localhost:25767/\r\n" + 
                                 "\r\n");
                
                connection.write( new byte[] { 0x00, 0x43, 0x33 } );
                connection.close();
                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        try {
            IWebSocketConnection ws = httpClient.openWebSocketConnection("ws://localhost:" + server.getLocalPort());

            ws.readMessage();
            Assert.fail("IOException expected");
        }catch (IOException expected) {  }
        
        httpClient.close();
        server.close();
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