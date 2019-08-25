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
import java.nio.ByteBuffer;

import junit.framework.Assert;


import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.NonBlockingConnection;



/**
*
* @author grro@xlightweb.org
*/
public final class SimpleDualPortServerTest {



    @Test
    public void testSimple() throws Exception {
        SimpleDualPortServer dualPortServer = new SimpleDualPortServer(0, 0);
        new Thread(dualPortServer).start();
        
        QAUtil.sleep(1000);
     
        INonBlockingConnection nbc1 = new NonBlockingConnection("localhost", dualPortServer.getTcpServerListenPort(), new EchoDataHandler());
        nbc1.write((int) 55);
        
        INonBlockingConnection nbc2 = new NonBlockingConnection("localhost", dualPortServer.getTcpServerListenPort(), new EchoDataHandler());
        nbc2.write((int) 55);
        
        INonBlockingConnection nbc3 = new NonBlockingConnection("localhost", dualPortServer.getTcpServerListenPort(), new EchoDataHandler());
        nbc3.write((int) 66);
        
        QAUtil.sleep(1000);
        
        HttpClient httpClient = new HttpClient();

        IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + dualPortServer.getHttpServerListenPort() + "/66", "text/plain", "hello you"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("hello you", response.getBody().readString());
        

        response = httpClient.call(new PostRequest("http://localhost:" + dualPortServer.getHttpServerListenPort() + "/66", "text/plain", "hello you2"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("hello you2", response.getBody().readString());


        response = httpClient.call(new PostRequest("http://localhost:" + dualPortServer.getHttpServerListenPort() + "/55", "text/plain", "hello you3"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("hello you3", response.getBody().readString());

        
        httpClient.close();
        nbc1.close();
        nbc2.close();
        nbc3.close();
        dualPortServer.close();
    }

    
    private static final class EchoDataHandler implements IDataHandler {
        
        public boolean onData(INonBlockingConnection connection) throws IOException {
            
            connection.resetToReadMark();
            connection.markReadPosition();
            int length = connection.readInt();
            ByteBuffer[] data = connection.readByteBufferByLength(length);
            connection.removeReadMark();
            
            connection.write(length);
            connection.write(data);
            return true;
        }
    }
}
