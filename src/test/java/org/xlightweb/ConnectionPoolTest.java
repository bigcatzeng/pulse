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




import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;


/**
*
* @author grro@xlightweb.org
*/
public final class ConnectionPoolTest {
	

	@Test 
	public void testPersistent() throws Exception {
		
		System.setProperty("org.xlightweb.showDetailedError", "true");

		
		System.out.println("testPersistent");

		HttpServer server = new HttpServer(new HeaderInfoServerHandler());
		server.start();
		
		HttpClient httpClient = new HttpClient(); 
		for (int i = 0; i < 100; i++) {
			IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
			
			Assert.assertEquals(200, response.getStatus());
		}
		
		httpClient.close();		
		server.close();	
	}

	

	@Test 
	public void testLiveNonPersistent() throws Exception {
		System.out.println("testLiveNonPersistent");
		
		HttpClient httpClient = new HttpClient(); 
		for (int i = 0; i < 10; i++) {
			IHttpResponse response = httpClient.call(new GetRequest("http://www.web.de/"));
			Assert.assertTrue((response.getStatus() >= 200) && (response.getStatus() < 400));
		}
			
		httpClient.close();
	}


/*	@Test 
	public void testPersistentKeepAlive() throws Exception {


		HttpClient httpClient = new HttpClient(); 
		for (int i = 0; i < 110; i++) {
			HttpRequestHeader requestHeader = new HttpRequestHeader("GET", "http://www1.1und1.de/index.php");
			requestHeader.setHeader("Connection", "Keep-Alive");
			HttpResponse response = httpClient.call(new HttpRequest(requestHeader, null));
		
			Assert.assertEquals(200, response.getStatus());
			System.out.print(".");
		}
			
		httpClient.close();
	}*/

	
}
