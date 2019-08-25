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
import java.io.IOException;
import java.io.RandomAccessFile;

import org.junit.Assert;
import org.junit.Ignore;

import org.junit.Test;
import org.xlightweb.client.HttpClientConnection;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.Server;



/**
*
* @author grro@xlightweb.org
*/
public final class ChunkedBodyTest {

    
    @Test
    public void testSimple1() throws Exception {
        
        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                connection.readStringByDelimiter("\r\n\r\n");
                
                connection.write("HTTP/1.1 200 OK\r\n" +
                                 "Server: me\r\n" +
                                 "Content-Type: text/plain; charset=UTF-8\r\n" +
                                 "Transfer-Encoding: chunked\r\n" +
                                 "\r\n" + 
                                 "5\r\n" + 
                                 "12345\r\n");
                QAUtil.sleep(200);
                
                connection.write("3\r\n" + 
                                 "6");
                QAUtil.sleep(200);
                
                connection.write("78\r\n");
                QAUtil.sleep(200);

                connection.write("4\r");
                QAUtil.sleep(200);

                connection.write("\n" + 
                                 "9012");
                QAUtil.sleep(200);

                connection.write("\r\n");
                QAUtil.sleep(200);
                
                connection.write("3\n" + 
                                 "345\n");
                QAUtil.sleep(200);
                
                connection.write("0\r\n\r\n");
                
                return true;
            }
        };
        
        Server server = new Server(dh);
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        IHttpResponse response = con.call(new GetRequest("/"));
        
        Assert.assertEquals(200, response.getStatus());
        
        String body = response.getBody().readString();
        Assert.assertEquals("123456789012345", body);
        
        con.close();
        server.close();
    }    

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    @Ignore 
	@Test
	public void testSimple() throws Exception {
	    
	    IDataHandler dh = new IDataHandler() {
	        
	        public boolean onData(INonBlockingConnection connection) throws IOException {
	            connection.readStringByDelimiter("\r\n\r\n");
	            
	            String path = QAUtil.getFilepath("org" + File.separator + "xlightweb" + File.separator + "ChunkedResponse.txt");
	            RandomAccessFile raf = new RandomAccessFile(path, "r");
	            connection.transferFrom(raf.getChannel());
	            raf.close();
	            
	            return true;
	        }
	    };
	    
	    Server server = new Server(dh);
	    server.start();
	    
	    
	    HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
	    IHttpResponse response = con.call(new GetRequest("/"));
	    System.out.println(response);
	    
	    response.getBody().readString();
	    server.close();	    
	}
}
