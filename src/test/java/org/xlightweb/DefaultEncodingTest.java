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
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.Server;





/**
*
* @author grro@xlightweb.org
*/
public final class DefaultEncodingTest  {
	
     
    @Test
    public void testSimple() throws Exception {

        
        IDataHandler hdl = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection con) throws IOException {
                con.readStringByDelimiter("\r\n\r\n");
                
                byte[] data = "Herzlichen Gl\u00FCckwunsch, Sie haben sich zur Reinigung des Aufzugs entschlossen.".getBytes("UTF-8");  
                
                con.write("HTTP/1.1 200 OK\r\n" + 
                          "Server: me \r\n" + 
                          "Content-Type: text/plain\r\n" + 
                          "Content-Length: " + data.length + "\r\n" +
                          "\r\n");
                con.write(data);
                return true;
            }
        };
        Server server = new Server(hdl);
        server.start();

        HttpClient httpClient = new HttpClient();
        
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort()+ "/"));
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("iso-8859-1", response.getCharacterEncoding());
        Assert.assertFalse("Herzlichen Gl\u00FCckwunsch, Sie haben sich zur Reinigung des Aufzugs entschlossen.".equals(response.getBody().readString()));
        
        httpClient.close();
        server.close();
    }
    
    
    
    @Test
    public void testUTF8() throws Exception {

        
        IDataHandler hdl = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection con) throws IOException {
                con.readStringByDelimiter("\r\n\r\n");
                
                byte[] data = "Herzlichen Gl\u00FCckwunsch, Sie haben sich zur Reinigung des Aufzugs entschlossen.".getBytes("UTF-8");  
                
                con.write("HTTP/1.1 200 OK\r\n" + 
                          "Server: me \r\n" + 
                          "Content-Type: text/plain\r\n" + 
                          "Content-Length: " + data.length + "\r\n" +
                          "\r\n");
                con.write(data);
                return true;
            }
        };
        Server server = new Server(hdl);
        server.start();

        HttpClient httpClient = new HttpClient();
        httpClient.setResponseBodyDefaultEncoding("utf-8");
        
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort()+ "/"));
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("utf-8", response.getCharacterEncoding());
        Assert.assertEquals("Herzlichen Gl\u00FCckwunsch, Sie haben sich zur Reinigung des Aufzugs entschlossen.", response.getBody().readString());
        
        httpClient.close();
        server.close();
    }
    
    
    @Test
    public void testExplicite() throws Exception {

        
        IDataHandler hdl = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection con) throws IOException {
                con.readStringByDelimiter("\r\n\r\n");
                
                byte[] data = "Herzlichen Gl\u00FCckwunsch, Sie haben sich zur Reinigung des Aufzugs entschlossen.".getBytes("UTF-8");  
                
                con.write("HTTP/1.1 200 OK\r\n" + 
                          "Server: me \r\n" + 
                          "Content-Type: text/plain; charset=iso-8859-1\r\n" + 
                          "Content-Length: " + data.length + "\r\n" +
                          "\r\n");
                con.write(data);
                return true;
            }
        };
        Server server = new Server(hdl);
        server.start();

        HttpClient httpClient = new HttpClient();
        httpClient.setResponseBodyDefaultEncoding("utf-8");
        
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort()+ "/"));
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("iso-8859-1", response.getCharacterEncoding());
        Assert.assertFalse("Herzlichen Gl\u00FCckwunsch, Sie haben sich zur Reinigung des Aufzugs entschlossen.".equals(response.getBody().readString()));
        
        httpClient.close();
        server.close();
    }      
    

    @Test
    public void testPostRequest() throws Exception {

        PostRequest req = new PostRequest("http://xlightweb.org", "text/plain", "Hello");
        
        Assert.assertEquals(req.getCharacterEncoding(), "iso-8859-1");
        Assert.assertEquals("text/plain; charset=iso-8859-1", req.getContentType());
    }

    @Test
    public void testPutRequest() throws Exception {

        PutRequest req = new PutRequest("http://xlightweb.org", "text/plain", "Hello");
        
        Assert.assertEquals(req.getCharacterEncoding(), "iso-8859-1");
        Assert.assertEquals("text/plain; charset=iso-8859-1", req.getContentType());
    }

    
    @Test
    public void testResponse() throws Exception {

        HttpResponse res = new HttpResponse(200, "text/plain", "Hello");
        
        Assert.assertEquals(res.getCharacterEncoding(), "utf-8");
        Assert.assertEquals("text/plain; charset=utf-8", res.getContentType());
    }
}