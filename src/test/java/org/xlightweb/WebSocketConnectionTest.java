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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.client.HttpClient;



/**  
*
* @author grro@xlightweb.org
*/
public final class WebSocketConnectionTest {
	

    public static void main(String[] args) throws Exception {
    
        for (int i = 0; i < 1000; i++) {
            new WebSocketConnectionTest().testClientConnection();
        }
    }


    @Test
    public void testClient() throws Exception {
                
        WebContainer webContainer = new WebContainer(new EchoWebSocketServlet());
        webContainer.start();
        
        
        HttpClient httpClient = new HttpClient();
        
        IWebSocketConnection webSocketConnection = httpClient.openWebSocketConnection("ws://localhost:" +  webContainer.getLocalPort());
        
        webSocketConnection.writeMessage(new TextMessage("Hello you"));
        
        WebSocketMessage msg = webSocketConnection.readMessage();
        Assert.assertTrue(msg.isTextMessage());
        Assert.assertEquals("Hello you", msg.toString());
        
        String txt = "0123456789";
        webSocketConnection.writeMessage(new TextMessage(txt));
        msg = webSocketConnection.readMessage();
        Assert.assertTrue(msg.isTextMessage());
        Assert.assertEquals(txt, msg.toString());
        
        
        httpClient.close();
        webContainer.stop();
    } 
    
   
   

    
    
    

    @Test
    public void testClientConnection() throws Exception {        
        WebContainer webContainer = new WebContainer(new EchoWebSocketServlet());
        webContainer.start();
        
        
        HttpClient httpClient = new HttpClient();
        
        IWebSocketConnection webSocketConnection = new WebSocketConnection("ws://localhost:" +  webContainer.getLocalPort(), "net.example.myprotocol");
        
        webSocketConnection.writeMessage(new TextMessage("Hello you"));
        
        WebSocketMessage msg = webSocketConnection.readMessage();
        Assert.assertTrue(msg.isTextMessage());
        Assert.assertEquals("Hello you", msg.toString());
        
     
        String txt = "0123456789";
        webSocketConnection.writeMessage(new TextMessage(txt));
        msg = webSocketConnection.readMessage();
        Assert.assertTrue(msg.isTextMessage());
        Assert.assertEquals(txt, msg.toString());
        
        
        webSocketConnection.close();
        webContainer.stop();
    }     


    @Test
    public void testSendAsync() throws Exception {
        
        WebContainer webContainer = new WebContainer(new EchoWebSocketServlet());
        webContainer.start();
        
        
        HttpClient httpClient = new HttpClient();
        
        IWebSocketConnection webSocketConnection = httpClient.openWebSocketConnection("ws://localhost:" +  webContainer.getLocalPort());
        
        webSocketConnection.writeMessage(new TextMessage("Hello you"));
        
        int available = webSocketConnection.availableMessages();
        WebSocketMessage msg = webSocketConnection.readMessage();
        Assert.assertTrue(msg.isTextMessage());
        Assert.assertEquals("Hello you", msg.toString());
        
     
        class MyWriteCompleteHandler implements IWriteCompleteHandler {
            
            public void onWritten(int written) {
                // ..
            }
            
            public void onException(IOException ioe) {
                //..
            }
        }
        
        
        String txt = "0123456789";
        webSocketConnection.writeMessage(new TextMessage(txt));
        msg = webSocketConnection.readMessage();
        Assert.assertTrue(msg.isTextMessage());
        Assert.assertEquals(txt, msg.toString());
        
        
        httpClient.close();
        webContainer.stop();
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