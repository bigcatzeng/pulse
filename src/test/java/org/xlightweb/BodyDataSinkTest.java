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

import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import org.junit.Test;
import org.xlightweb.client.HttpClientConnection;



/**
*
* @author grro@xlightweb.org
*/
public final class BodyDataSinkTest  {
    


    @Test
    public void testBodyless() throws Exception {

        WebContainer container = new WebContainer(new MyServlet());
        container.start();
             
        HttpClientConnection con = new HttpClientConnection("localhost", container.getLocalPort());
        
        IHttpResponse response = con.call(new GetRequest("http://localhost:" + container.getLocalPort() + "/"));
 
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("OK", response.getBody().readString());
                
        con.close();
        container.stop();
    }    
    
    
    @Test
    public void testNonExpliciteBody() throws Exception {
        
        WebContainer container = new WebContainer(new MyServlet());
        container.start();
             
        HttpClientConnection con = new HttpClientConnection("localhost", container.getLocalPort());
        
        PostRequest request = new PostRequest("http://localhost:" + container.getLocalPort() + "/", "test/plain", "123456789012345");
        IHttpResponse response = con.call(request);
 
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("123456789012345", response.getBody().readString());
                
        con.close();
        container.stop();
    }    
    
    
    
    @Test
    public void testFullMessage() throws Exception {

        WebContainer container = new WebContainer(new MyServlet());
        container.start();
             
        HttpClientConnection con = new HttpClientConnection("localhost", container.getLocalPort());
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        BodyDataSink dataSink = con.send(new HttpRequestHeader("POST", "http://localhost:" + container.getLocalPort() + "/"), 15, respHdl);
        dataSink.write("1234567890");
        
        IHttpResponse response = respHdl.getResponse(); 
        Assert.assertEquals(200, response.getStatus());
        
        
        dataSink.write("12345");
        Assert.assertFalse(dataSink.isOpen());
        
        Assert.assertEquals("123456789012345", response.getBody().readString());
                
        con.close();
        container.stop();
    }    
    
    
    @Test
    public void testFullMessageChunked() throws Exception {

        WebContainer container = new WebContainer(new MyServlet());
        container.start();
             
        HttpClientConnection con = new HttpClientConnection("localhost", container.getLocalPort());
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        BodyDataSink dataSink = con.send(new HttpRequestHeader("POST", "http://localhost:" + container.getLocalPort() + "/"), respHdl);
        dataSink.write("1234567890");
        
        IHttpResponse response = respHdl.getResponse(); 
        Assert.assertEquals(200, response.getStatus());
        
        
        dataSink.write("12345");
        dataSink.close();
        
        Assert.assertEquals("123456789012345", response.getBody().readString());
                
        con.close();
        container.stop();
    }    
    

    
    
    @Test
    public void testInvalidCloseDataSink() throws Exception {

        WebContainer container = new WebContainer(new MyServlet());
        container.start();
             
        HttpClientConnection con = new HttpClientConnection("localhost", container.getLocalPort());
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        BodyDataSink dataSink = con.send(new HttpRequestHeader("POST", "http://localhost:" + container.getLocalPort() + "/"), 15, respHdl);
        dataSink.write("12345678");
        
        
        try  {
            dataSink.close();
            Assert.fail("ProtocolException expected");
        } catch (ProtocolException expected) { }
        
                
        con.close();
        container.stop();
    }    

    


    private static final class MyServlet extends HttpServlet {

        private static final long serialVersionUID = -6112517976734846433L;
        
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.getWriter().write("OK");
        }
        

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            InputStream is = req.getInputStream();
            OutputStream os = resp.getOutputStream();
                        
            byte[] data = new byte[4096];
            int read = 0;
            do {
                read = is.read(data);
                if (read > 0) {
                    os.write(data, 0, read);
                    os.flush();
                }
            } while (read >= 0);
            
            is.close();
            os.close();
        }        
    }
    	

}