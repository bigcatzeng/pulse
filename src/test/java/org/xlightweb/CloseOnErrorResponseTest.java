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
import java.nio.channels.ClosedChannelException;

import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.client.HttpClientConnection;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.Server;




/**
*
* @author grro@xlightweb.org
*/
public final class CloseOnErrorResponseTest {
	
	

	@Test 
	public void testClient4xx() throws Exception {

	    IDataHandler dh = new IDataHandler() {
	      
	        public boolean onData(INonBlockingConnection con) throws IOException {
	            con.readStringByDelimiter("\r\n\r\n");
	            
	            con.write("HTTP/1.1 400 Bad Request\r\n" + 
	                      "Server: me\r\n" +
	                      "Content-Length: 0\r\n" +
	                      "\r\n");
	            
	            return true;
	        }
	    };
	    
	    Server server = new Server(dh);
	    server.start();
		
		HttpClientConnection httpCon = new HttpClientConnection("localhost", server.getLocalPort());
		
		IHttpResponse response = httpCon.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		Assert.assertEquals(400, response.getStatus());

		response = httpCon.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		Assert.assertEquals(400, response.getStatus());
		
		httpCon.close();
		server.close();
	}
	
	

    @Test 
     public void testClient5xx() throws Exception {

         IDataHandler dh = new IDataHandler() {
           
             public boolean onData(INonBlockingConnection con) throws IOException {
                 con.readStringByDelimiter("\r\n\r\n");
                 
                 con.write("HTTP/1.1 500 Server error\r\n" + 
                           "Server: me\r\n" +
                           "Content-Length: 0\r\n" +
                           "\r\n");
                 
                 return true;
             }
         };
         
         Server server = new Server(dh);
         server.start();
         
         HttpClientConnection httpCon = new HttpClientConnection("localhost", server.getLocalPort());
         
         IHttpResponse response = httpCon.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
         Assert.assertEquals(500, response.getStatus());

         try {
             response = httpCon.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
             Assert.fail("ClosedChannelException expected");
         } catch (ClosedChannelException expected) { }
         
         server.close();
     }	
}
