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
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClient.FollowsRedirectMode;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.Server;



/**
*
* @author grro@xlightweb.org
*/
public final class EncodingTest {
    
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 1000; i++) {
            new EncodingTest().testClient();
        }
    }


    @Test
    public void testClient() throws Exception {
        
        IDataHandler ds = new IDataHandler() {
          
            public boolean onData(INonBlockingConnection connection) throws IOException {
                connection.setAutoflush(false);
                
                connection.readStringByDelimiter("\r\n\r\n");
                
                connection.write("HTTP/1.1 200 OK\r\n" +
                                 "Content-Type: text/plain\r\n" +
                                 "Content-Length: 10\r\n" +
                                 "\r\n");
                                 
                connection.write((byte) 239);
                connection.write((byte) 187);
                connection.write((byte) 191);
                connection.write((byte) 70);
                connection.write((byte) 114);
                connection.write((byte) 195);
                connection.write((byte) 188);
                connection.write((byte) 104);
                connection.write((byte) 101);
                connection.write((byte) 114);
                connection.flush();
                
                return true;
            }
        };
        Server server = new Server(ds);
        server.start();
       
        
        HttpClient httpClient = new HttpClient();
        
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        
        Assert.assertEquals("UTF-8", response.getNonBlockingBody().getEncoding());
        
        System.out.println(response.getBody().readString());
        
        httpClient.close();
        server.close();
    }
    

    @Test
    public void testClient2() throws Exception {
        
        IDataHandler ds = new IDataHandler() {
          
            public boolean onData(INonBlockingConnection connection) throws IOException {
                connection.setAutoflush(false);
                
                connection.readStringByDelimiter("\r\n\r\n");
                
                connection.write("HTTP/1.1 200 OK\r\n" +
                                 "Content-Type: text/plain\r\n" +
                                 "Content-Length: 10\r\n" +
                                 "\r\n");
                                 
                connection.write((byte) 239);
                connection.write((byte) 187);
                connection.write((byte) 191);
                connection.write((byte) 70);
                connection.write((byte) 114);
                connection.write((byte) 195);
                connection.write((byte) 188);
                connection.write((byte) 104);
                connection.write((byte) 101);
                connection.write((byte) 114);
                connection.flush();
                
                return true;
            }
        };
        Server server = new Server(ds);
        server.start();
       
        
        HttpClient httpClient = new HttpClient();
        httpClient.setCacheMaxSizeKB(5000);

        // redirect and cookie handling
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        httpClient.setAutoHandleCookies(false);
        httpClient.setCallReturnOnMessage(true);
        httpClient.setMaxRetries(3);

        
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        
        Assert.assertEquals("UTF-8", response.getNonBlockingBody().getEncoding());
        
        System.out.println(response.getBody().readString());
        
        httpClient.close();
        server.close();
    }    
}
