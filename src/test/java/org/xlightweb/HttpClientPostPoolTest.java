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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;



import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IServer;




/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientPostPoolTest  {


	private AtomicInteger running = new AtomicInteger(0);
	private AtomicInteger openResponses = new AtomicInteger(0);

	private List<String> errors = new ArrayList<String>(); 
	
		

	
	
	@Before
	public void setup() {
		running.set(0);
		openResponses.set(0);
		errors.clear();
	}
	



	@Test
	public void testPoolCall() throws Exception {
		System.setProperty("org.xlightweb.showDetailedError", "true");
	
		final HttpClient httpClient = new HttpClient();

		final IServer server = new HttpServer(new EchoHandler());
		server.start();

		for (int i = 0; i < 3; i++) {
			Thread t = new Thread() {

				@Override
				public void run() {

					running.incrementAndGet();
					try {
						for (int j = 0; j < 50; j++) {
							IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort() + "/", "text/plain", "test"));
							Assert.assertEquals(200, response.getStatus());
							Assert.assertEquals("test", response.getBody().readString());
						}
					} catch (Exception e) {
						errors.add(e.toString());
						
					} finally {
						running.decrementAndGet();
					}
				}
			};
			t.start();
		}

		do {
			QAUtil.sleep(200);
		} while (running.get() > 0);

		for (String error : errors) {
			System.out.println(error);
		}

		Assert.assertTrue(errors.isEmpty());
		Assert.assertTrue(httpClient.getNumCreated() <= 10);
		Assert.assertEquals(0, httpClient.getNumDestroyed());

		httpClient.close();
		server.close();
	}
	
	
	



	@Test
	public void testPoolSend() throws Exception {
		System.out.println("testPoolSend");
		System.setProperty("org.xlightweb.showDetailedError", "true");

		
		final HttpClient httpClient = new HttpClient();

		final IServer server = new HttpServer(new EchoHandler());
		ConnectionUtils.start(server);


		for (int i = 0; i < 5; i++) {
			Thread t = new Thread() {

				@Override
				public void run() {

					running.incrementAndGet();
					try {
						for (int j = 0; j < 30; j++) {
							IHttpResponseHandler hdl = new IHttpResponseHandler() {
								public void onResponse(IHttpResponse response) throws IOException {
									openResponses.incrementAndGet();
								}
								
								public void onException(IOException ioe) {
								}
							};
							
							openResponses.decrementAndGet();
							httpClient.send(new PostRequest("http://localhost:" + server.getLocalPort() + "/", "text/plian", "test"), hdl);
							QAUtil.sleep(10);
						}
					} catch (Exception e) {
						errors.add(e.toString());
					}

					running.decrementAndGet();
				}
			};
			t.start();
		}

		do {
			QAUtil.sleep(200);
		} while ((running.get() > 0) || (openResponses.get() > 0));

		
		for (String error : errors) {
			System.out.println(error);
		}

		Assert.assertTrue(errors.isEmpty());

		Assert.assertTrue(httpClient.getNumDestroyed() == 0);


		server.close();
		httpClient.close();
	}






	private static final class EchoHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {

			IHttpRequest request = exchange.getRequest();
						
			HttpResponse response = new HttpResponse(request.getContentType(), request.getNonBlockingBody());
			exchange.send(response);
		}
		
	}
}