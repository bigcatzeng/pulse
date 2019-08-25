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
import org.junit.Test;
import org.xlightweb.server.HttpServer;
import org.xsocket.WorkerPool;
import org.xsocket.connection.BlockingConnection;





/**  
*
* @author grro@xlightweb.org
*/
public final class WorkerThreadpolsizeTest  {
	


  
    
    @Test
    public void testCorePoolSize0() throws Exception {
   
        HttpServer server = new HttpServer(0, new WebHandler(), 0, 40);
        server.start();
        
        WorkerPool pool = ((WorkerPool) server.getWorkerpool());
        

        BlockingConnection bc = new BlockingConnection("localhost", server.getLocalPort());

        bc.write("GET /0123456 HTTP/1.1\r\n"+ 
                 "Host: localhost:" + server.getLocalPort() + "\r\n"+
                 "User-Agent: xLightweb/2.11\r\n"+ 
                 "Upgrade: WebSocket\r\n"+ 
                 "Connection: Upgrade\r\n"+ 
                 "Origin: http://localhost:5161/\r\n"+ 
                 "\r\n");

        System.out.println(bc.readStringByDelimiter("\r\n\r\n"));
        
        for (int i = 0; i < 10; i++) {
            bc.write(new byte[] { 0x00, 0x48, 0x65, (byte) 0xFF});

            byte[] b = bc.readBytesByLength(4);
            Assert.assertArrayEquals(new byte[] { 0x00, 0x48, 0x65, (byte) 0xFF}, b);
            
            QAUtil.sleep(250);
        }

        
        System.out.println(pool.getLargestPoolSize());
        
        bc.close();
        server.close();
    }     
    

    
    private static final class WebHandler implements IWebSocketHandler {
        
        public void onConnect(IWebSocketConnection con) throws IOException {
        }
        
        public void onDisconnect(IWebSocketConnection con) throws IOException {
        }
        
        
        public void onMessage(IWebSocketConnection con) throws IOException {
            WebSocketMessage msg = con.readMessage();
            con.writeMessage(msg);
        }
    }
}