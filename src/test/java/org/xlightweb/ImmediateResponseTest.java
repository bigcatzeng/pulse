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
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;

import org.junit.Test;
import org.xlightweb.BodyDataSink;
import org.xlightweb.client.HttpClient;



/**
*
* @author grro@xlightweb.org
*/
public final class ImmediateResponseTest {


	@Test
	public void testSimple() throws Exception {

	    
		// start jetty server
		WebContainer servletEngine = new WebContainer(new MyServlet());
		servletEngine.start();
		
		
		HttpClient httpClient = new HttpClient();
		

		// send the request header and one chunk
		FutureResponseHandler hdl = new FutureResponseHandler();
		BodyDataSink bodySink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + servletEngine.getLocalPort() + "/"), hdl);
		
		bodySink.write("test");
		bodySink.flush(); 
		
		QAUtil.sleep(1000);

		// get the response
		IHttpResponse response = hdl.getResponse();
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("OK", response.getBody().readString());
		
		
		bodySink.write("test2"); // will be ignored 
		
		
		httpClient.close();
		servletEngine.stop();
	}
	
	
	

	private static final class MyServlet extends HttpServlet {


		private static final long serialVersionUID = 5414556868896021677L;

		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		    
		    PrintWriter writer = resp.getWriter();
		    writer.write("OK");
		    writer.close();
		}
	}
}
