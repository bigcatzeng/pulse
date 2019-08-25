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


import junit.framework.Assert;

import org.junit.Test;
import org.xlightweb.GetRequest;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpResponse;
import org.xlightweb.client.HttpClient;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.Server;


/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientCloseResponseTest  {
  
    

	@Test
	public void testNoCache() throws Exception {

	    IDataHandler dh = new IDataHandler() {
	      
	        public boolean onData(INonBlockingConnection connection) throws IOException {
	            connection.readStringByDelimiter("\r\n\r\n");
	            connection.write("HTTP/1.1 302 Found\r\n" +
	                             "Date: Wed, 23 Sep 2009 18:57:37 GMT\r\n" +
	                             "Server: Apache/2.0.52 (Unix) mod_ssl/2.0.52 OpenSSL/0.9.7e\r\n" +
	                             "Location: http://magazine.web.de/de/themen/unterhaltung/index,cc=000005479500089676784i8nZM.html\r\n" +
	                             "Content-Length: 6\r\n" +
	                             "Connection: close\r\n" +
	                             "Content-Type: text/html; charset=iso-8859-1\r\n" +
	                             "\r\n" +
	                             "123456");
	            connection.close();
	            return true;
	        }
	    };
	    
        Server server = new Server(dh);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/test/12345");
        IHttpResponse resp = httpClient.call(request);
        Assert.assertEquals(302, resp.getStatus());
        Assert.assertEquals("123456", resp.getBody().readString());
        
        
        httpClient.close();
        server.close();	    
    }
	
	
    @Test
    public void testCache() throws Exception {

        IDataHandler dh = new IDataHandler() {
          
            public boolean onData(INonBlockingConnection connection) throws IOException {
                connection.readStringByDelimiter("\r\n\r\n");
                connection.write("HTTP/1.1 302 Found\r\n" +
                                 "Date: Wed, 23 Sep 2009 18:57:37 GMT\r\n" +
                                 "Server: Apache/2.0.52 (Unix) mod_ssl/2.0.52 OpenSSL/0.9.7e\r\n" +
                                 "Location: http://magazine.web.de/de/themen/unterhaltung/index,cc=000005479500089676784i8nZM.html\r\n" +
                                 "Content-Length: 6\r\n" +
                                 "Connection: close\r\n" +
                                 "Content-Type: text/html; charset=iso-8859-1\r\n" +
                                 "\r\n" +
                                 "123456");
                connection.close();
                return true;
            }
        };
        
        Server server = new Server(dh);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setCacheMaxSizeKB(100);
        
        
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/test/12345");
        IHttpResponse resp = httpClient.call(request);
        Assert.assertEquals(302, resp.getStatus());
        Assert.assertEquals("123456", resp.getBody().readString());
        
        
        httpClient.close();
        server.close();     
    }       
	
}
