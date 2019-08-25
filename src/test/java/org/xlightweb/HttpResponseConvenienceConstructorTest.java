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
import java.io.FileOutputStream;
import java.io.IOException;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IServer;



/**
*
* @author grro@xlightweb.org
*/
public final class HttpResponseConvenienceConstructorTest  {

	 
	@Test
	public void testSimple() throws Exception {

		IServer server = new HttpServer(new RequestHandler());
		server.start();
		
		HttpClient httpClient = new HttpClient(); 
		
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		Assert.assertEquals(200, response.getStatus());
		Assert.assertNull(response.getContentType());
		
		httpClient.close();
		server.close();
	}
	

	
	@Test
	public void testFile() throws Exception {

		File file = QAUtil.createTempfile(".txt");
		
		FileOutputStream fos = new FileOutputStream(file);
		fos.write("test1234".getBytes());
		fos.close();
		
		IServer server = new HttpServer(new FileRequestHandler(file));
		server.start();
		
		HttpClient httpClient = new HttpClient(); 
		
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("text/plain", response.getContentType());
		Assert.assertEquals("test1234", response.getBody().readString());
	
		file.delete();		
		httpClient.close();
		server.close();
	}
	
	
	private static final class RequestHandler implements IHttpRequestHandler  {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			exchange.send(new HttpResponse("OK"));
		}		
	}
	

	private static final class FileRequestHandler implements IHttpRequestHandler  {
		
		private File file = null;
		
		FileRequestHandler(File file) {
			this.file = file;
			
		}
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			exchange.send(new HttpResponse(file));
		}		
	}
}