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
import java.util.concurrent.atomic.AtomicInteger;


import org.junit.Assert;

import org.junit.Test;
import org.xlightweb.GetRequest;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.RequestHandlerChain;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;



/**
*
* @author grro@xlightweb.org
*/
public final class SynchronizedOnTest {
  
	 
	@Test
	public void testSessionSynchronized() throws Exception {
		
		SessionSynchronizedRequestHandler reqHdl = new SessionSynchronizedRequestHandler();
		HttpServer server = new HttpServer(reqHdl);
		server.start();
		
		
		HttpClient httpClient = new HttpClient();
		httpClient.setAutoHandleCookies(true);
		
		httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		QAUtil.sleep(200);
		
		FutureResponseHandler respHdl1 = new FutureResponseHandler();
		httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?sleeptime=3000"), respHdl1);
		QAUtil.sleep(1000);
		
		Assert.assertEquals(1, reqHdl.getCount());
		

		FutureResponseHandler respHdl2 = new FutureResponseHandler();
		httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?sleeptime=2000"), respHdl2);
		
		QAUtil.sleep(1000);
		
		Assert.assertEquals(1, reqHdl.getCount());

		
		
		httpClient.close();
		server.close();
	}
	


	@Test
	public void testSessionSynchronizedRequestHandlerChain() throws Exception {
		
		SessionSynchronizedRequestHandler reqHdl = new SessionSynchronizedRequestHandler();
		RequestHandlerChain chain = new RequestHandlerChain();
		chain.addLast(reqHdl);
		
		HttpServer server = new HttpServer(chain);
		server.start();
		
		
		HttpClient httpClient = new HttpClient();
		httpClient.setAutoHandleCookies(true);
		
		httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		QAUtil.sleep(200);
		
		FutureResponseHandler respHdl1 = new FutureResponseHandler();
		httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?sleeptime=3000"), respHdl1);
		QAUtil.sleep(1000);
		
		Assert.assertEquals(1, reqHdl.getCount());
		

		FutureResponseHandler respHdl2 = new FutureResponseHandler();
		httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?sleeptime=2000"), respHdl2);
		
		QAUtil.sleep(1000);
		
		Assert.assertEquals(1, reqHdl.getCount());

		respHdl1.getResponse();
		respHdl2.getResponse();
		
		httpClient.close();
		server.close();
	}

	
	
	@Test
	public void testConnectionSynchronized() throws Exception {
		
		ConnectionSynchronizedRequestHandler reqHdl = new ConnectionSynchronizedRequestHandler();
		HttpServer server = new HttpServer(reqHdl);
		server.start();
		
		
		HttpClient httpClient = new HttpClient();
		
		httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		QAUtil.sleep(200);
		
		FutureResponseHandler respHdl1 = new FutureResponseHandler();
		httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?sleeptime=3000"), respHdl1);
		QAUtil.sleep(1000);
		
		Assert.assertEquals(1, reqHdl.getCount());
		

		FutureResponseHandler respHdl2 = new FutureResponseHandler();
		httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?sleeptime=2000"), respHdl2);
		
		QAUtil.sleep(1000);
		
		Assert.assertEquals(2, reqHdl.getCount());

	
		
		httpClient.close();
		server.close();
	}

	
	private static final class SessionSynchronizedRequestHandler implements IHttpRequestHandler {

		private AtomicInteger count = new AtomicInteger(0);
		
		
		@SynchronizedOn(SynchronizedOn.SESSION)
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
		
			count.incrementAndGet();
			
			IHttpSession session = exchange.getSession(true);
			session.getAttribute("myCounter");
		
			
			
			int sleeptime = exchange.getRequest().getIntParameter("sleeptime", 0);
			
			QAUtil.sleep(sleeptime);
			
			count.decrementAndGet();
			exchange.send(new HttpResponse("OK"));
		}
		
		
		int getCount() {
			return count.get();
		}
	}
	
	

	private static final class ConnectionSynchronizedRequestHandler implements IHttpRequestHandler {

		private AtomicInteger count = new AtomicInteger(0);
		
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
		
			count.incrementAndGet();
			
			exchange.getSession(true);
			
			int sleeptime = exchange.getRequest().getIntParameter("sleeptime", 0);
			
			QAUtil.sleep(sleeptime);

			count.decrementAndGet();
			exchange.send(new HttpResponse("OK"));			
		}
		
		
		int getCount() {
			return count.get();
		}
	}
}
