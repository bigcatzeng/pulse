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
public final class FormURLEncodedTest  {


		
	@Test
	public void testSimple() throws Exception {
	    
	    WebContainer container = new WebContainer(new MyServlet());
	    container.start();
	    
		
		HttpClient httpClient = new HttpClient();

		String txt = "Test now return \n another at the end\n";
		PostRequest request = new PostRequest("http://localhost:" + container.getLocalPort() + "/");
		request.setParameter("\r\ntest ", txt);
		IHttpResponse response = httpClient.call(request);

		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals(txt, response.getBody().readString());
		
		httpClient.close();
		container.stop();
	}

	
    @Test
    public void testSimple2() throws Exception {
        
        WebContainer container = new WebContainer(new MyServlet());
        container.start();
        
        
        HttpClient httpClient = new HttpClient();

        String txt = "Test now return \n another at the end\n";
        IHttpRequest request = new FormURLEncodedRequest("http://localhost:" + container.getLocalPort() + "/");
        request.setParameter("\r\ntest ", txt);
        IHttpResponse response = httpClient.call(request);

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(txt, response.getBody().readString());
        
        httpClient.close();
        container.stop();
    }
	


	private static final class MyServlet extends HttpServlet {
	
        private static final long serialVersionUID = -4530220165449212942L;

        @Override
	    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	        response.setContentType("text/plain");
	        
	        String test = request.getParameter("\r\ntest ");
	        response.getWriter().write(test);
	        
	    }
	}	 
}