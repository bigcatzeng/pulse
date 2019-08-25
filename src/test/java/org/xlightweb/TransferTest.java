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


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;



/**
*
* @author grro@xlightweb.org
*/
public final class TransferTest  {

	
	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 100; i++) {
			new TransferTest().testSimple();
		}
	}


    @Test
    public void testSimple() throws Exception {
        WebContainer container = new WebContainer(new MyServlet());
        container.start();
             
        HttpClient httpClient = new HttpClient();

        for (int i = 0; i < 100; i++) {
        	try {
	        	System.out.print(".");
	            FutureResponseHandler respHdl = new FutureResponseHandler();
	            
	            BodyDataSink dataSink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + container.getLocalPort() + "/test"), respHdl);

	            dataSink.write("test");
	            
	            IHttpResponse response = respHdl.getResponse();
	            Assert.assertEquals(200, response.getStatus());
	            
	            BodyDataSource dataSource = response.getBody();

	            String txt = dataSource.readStringByLength(4); 
	            if (!txt.equals("test")) {
	            	System.out.println("got " + txt + " instead of test");
	            	Assert.fail();
	            }
	            
	            
	            dataSink.write("12345");
	            txt = dataSource.readStringByLength(5); 
	            if (!txt.equals("12345")) {
	            	System.out.println("got " + txt + " instead of 12345");
	            	Assert.fail();
	            }
	            
	            dataSink.write("789");
	            txt = dataSource.readStringByLength(3); 
	            if (!txt.equals("789")) {
	            	System.out.println("got " + txt + " instead of 789");
	            	Assert.fail();
	            }
	            dataSink.close();

	            
        	} catch (IOException ioe) {
        		ioe.printStackTrace();
        		throw ioe;
        	}
        }
        
        httpClient.close();
        container.stop();
    }    
    
    
    

    private static final class MyServlet extends HttpServlet {

        private static final long serialVersionUID = -6112517976734846433L;

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