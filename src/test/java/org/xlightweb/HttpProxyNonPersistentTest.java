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
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.Server;

 

/**
*
* @author grro@xlightweb.org
*/
public final class HttpProxyNonPersistentTest  {
  

    @Test
    public void testSimpleBatch() throws Exception {
        
        for (int i = 0; i < 10; i++) {
            testSimple();
        }
    }

    
    
	@Test
	public void testSimple() throws Exception {
		
	    IDataHandler dh = new IDataHandler() {
	      
	        public boolean onData(INonBlockingConnection connection) throws IOException {
	            connection.readStringByDelimiter("\r\n\r\n");
	            
	            connection.write("HTTP/1.0 200 FOUND\r\n" +
                                 "Server: me\r\n" +
                                 "Content-Type: application/x-javascript\r\n" +
                                 "\r\n" +
                                 "var =erererr");
	            connection.close();
	            
	            return true;
	        }
	    };
	    
		Server server = new Server(dh);
		server.start();
		
		HttpProxy2 proxy =  new HttpProxy2(0, false);
		proxy.start();

		
		HttpClient httpClient = new HttpClient();
		httpClient.setProxyHost("localhost");
		httpClient.setProxyPort(proxy.getLocalPort());
		
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() +"/"));
		System.out.println(response);
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("HTTP/1.0", response.getProtocol());
		Assert.assertEquals("close", response.getHeader("Connection"));
		
		response.getBody().readString();
		
		
		httpClient.close();
		proxy.close();
		server.close();
	}
	
	

    @Test
    public void testIllegal10ResponseTimeout() throws Exception {
        
        IDataHandler dh = new IDataHandler() {
          
            public boolean onData(INonBlockingConnection connection) throws IOException {
                connection.readStringByDelimiter("\r\n\r\n");
                
                connection.write("HTTP/1.0 200 FOUND\r\n" +
                                 "Server: me\r\n" +
                                 "Content-Type: application/x-javascript\r\n" +
                                 "\r\n" +
                                 "var =erererr");
                
                return true;
            }
        };
        
        Server server = new Server(dh);
        server.start();
        
        HttpProxy2 proxy =  new HttpProxy2(0, false);
        proxy.start();

        
        HttpClient httpClient = new HttpClient();
        httpClient.setProxyHost("localhost");
        httpClient.setProxyPort(proxy.getLocalPort());
        
        httpClient.setBodyDataReceiveTimeoutMillis(1000);
        
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() +"/"));
        System.out.println(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("HTTP/1.0", response.getProtocol());
        Assert.assertEquals("close", response.getHeader("Connection"));
        
        try {
            response.getBody().readString();
            Assert.fail("ReceiveTimeoutException expected");
        } catch (ReceiveTimeoutException expected) { }

        
        
        httpClient.close();
        proxy.close();
        server.close();
    }	
}

