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
import java.net.URLEncoder;


import org.junit.Assert;

import org.junit.Test;

import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;




/**
*
* @author grro@xlightweb.org
*/
public final class RequestLineEncodingTest {


	@Test
	public void testModifiedRequestHeader() throws Exception {

		Handler hdl = new Handler();
		HttpServer server = new HttpServer(0, hdl);
		server.start();

		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		con.send(new GetRequest("/test?belong=" + URLEncoder.encode("geh\u00F6ren", "UTF-8")), new ResponseHandler());

		QAUtil.sleep(300);

		Assert.assertNull("error occured " + hdl.error, hdl.error);
		con.close();
		server.close();
	}

	
	

	

	private static final class ResponseHandler implements IHttpResponseHandler {

		public void onResponse(IHttpResponse response) throws IOException {
		}
		
		public void onException(IOException ioe) {
		}
	}


	private static final class Handler implements IHttpRequestHandler {

		private String error = null;


		public void onRequest(IHttpExchange exchange) throws IOException {
			
			IHttpRequest request = exchange.getRequest();
			String value = request.getParameter("belong");
			if (!value.equals("geh\u00F6ren")) {
				error = "wrong encoding. got " + value + " instead of geh\u00F6ren";
			}

			request.setParameter("beautiful", "sch\u00F6n");
			request.toString();

			if (request.toString().indexOf("geh%C3%B6ren") == -1) {
				error = "wrong encoding for geh\u00F6ren " + request.toString();
			}

			if (request.toString().indexOf("sch%C3%B6n") == -1) {
				error = "wrong encoding for sch\u00F6n " + request.toString();
			}
		}
	}
}
