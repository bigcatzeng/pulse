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


import org.xlightweb.BodyDataSink;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;





/**
*
* @author grro@xlightweb.org
*/
public final class LazyHeaderSendTest  {


	@Test
	public void testSimple() throws Exception {
		RequestHandler reqHdl = new RequestHandler();
		HttpServer server = new HttpServer(reqHdl);
		server.start();
		
		HttpClient httpClient =new HttpClient();
		
		ResponseHandler respHdl = new ResponseHandler();
		httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/"), respHdl);
		
		do {
			QAUtil.sleep(100);
		} while (reqHdl.getExchange() == null);
		
		IHttpExchange exchange = reqHdl.getExchange();
		BodyDataSink dataSink = exchange.send(new HttpResponseHeader(200, "text/plain"));
		
		QAUtil.sleep(1000);
		Assert.assertNull(respHdl.getResponse());
		
		dataSink.write("test");
		QAUtil.sleep(1000);
		
		Assert.assertNotNull(respHdl.getResponse());
		
		httpClient.close();
		server.close();
	}
	
	
	private static final class ResponseHandler implements IHttpResponseHandler {
		
		private IHttpResponse response;
		
		public void onResponse(IHttpResponse response) throws IOException {
			this.response = response;
		}
		
		public void onException(IOException ioe) throws IOException {
			ioe.printStackTrace();
		}
		
		IHttpResponse getResponse() {
			return response;
		}
	}
	
	
	private static final class RequestHandler implements IHttpRequestHandler {
		
		private IHttpExchange exchange;
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			this.exchange = exchange;
		}
		
		
		IHttpExchange getExchange() {
			return exchange;
		}
	}

}

