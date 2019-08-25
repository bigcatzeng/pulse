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


import org.junit.Assert;
import org.xlightweb.client.HttpClient;





/**  
*
* @author grro@xlightweb.org
*/
public final class SimpleWebSocketClientExample   {
	
    
    public static void main(String[] args) throws IOException {
        
        int port = Integer.parseInt(args[0]);
    
        HttpClient httpClient = new HttpClient();
    
     
        /////////////////////////
        // with handler
        IWebSocketHandler handler = new IWebSocketHandler() {
            
            public void onMessage(IWebSocketConnection con) throws IOException {
                WebSocketMessage msg = con.readMessage();
                System.out.println(msg);
            }
            
            public void onDisconnect(IWebSocketConnection con) throws IOException { }
            
            public void onConnect(IWebSocketConnection con) throws IOException, UnsupportedProtocolException {  }
        };
        IWebSocketConnection webSocketConnection = httpClient.openWebSocketConnection("ws://localhost:" + port, "com.example.echo", handler);
        
        webSocketConnection.writeMessage(new TextMessage("0123456789"));

        
        
        ////////////////////////////////////////////
        // without handler  
        IWebSocketConnection webSocketConnection2 = httpClient.openWebSocketConnection("ws://localhost:" + port, "com.example.echo");
    
        webSocketConnection2.writeMessage(new TextMessage("0123456789"));
        WebSocketMessage msg = webSocketConnection2.readMessage();
        Assert.assertEquals("0123456789", msg.toString());

        webSocketConnection2.writeMessage(new TextMessage("0123456789"));
        msg = webSocketConnection2.readMessage();
        Assert.assertEquals("0123456789", msg.toString());

        webSocketConnection.close();
        webSocketConnection2.close();
    } 
}