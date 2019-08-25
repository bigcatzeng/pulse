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
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpProtocolAdapter;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;
import org.xsocket.connection.multiplexed.MultiplexedProtocolAdapter;



/**
*
* @author grro@xlightweb.org
*/
public final class MultiplexedTest  {

	private static final Logger LOG = Logger.getLogger(MultiplexedTest.class.getName());


	private int running = 0;

 

	@Test
	public void testSimple() throws Exception {

		// start multiplexed http server
		IServer mutliplexedHttpServer = new Server(0, new MultiplexedProtocolAdapter(new HttpProtocolAdapter(new ServerHandler(true))));
		mutliplexedHttpServer.setIdleTimeoutMillis(80 * 1000);
		mutliplexedHttpServer.start();

		// start tcp concentrator
		IServer tcpConcentrator = new TcpConcentratorServer(0, "localhost", mutliplexedHttpServer.getLocalPort());
		tcpConcentrator.setIdleTimeoutMillis(60 * 1000);
		tcpConcentrator.start();


		HttpClient httpClient = new HttpClient();
		for (int j = 0; j < 50; j++) {
			LOG.fine("client: sending request");
			IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + tcpConcentrator.getLocalPort() + "/test/loopid_" + j));

			Assert.assertEquals(200, response.getStatus());
		}

		httpClient.close();
		tcpConcentrator.close();
		mutliplexedHttpServer.close();
	}



	@Test
	public void testNonPersistent() throws Exception {

		// start multiplexed http server
		IServer mutliplexedHttpServer = new Server(0, new MultiplexedProtocolAdapter(new HttpProtocolAdapter(new ServerHandler(false))));
		mutliplexedHttpServer.setIdleTimeoutMillis(60 * 60 * 1000);
		mutliplexedHttpServer.start();

		// start tcp concentrator
		IServer tcpConcentrator = new TcpConcentratorServer(0, "localhost", mutliplexedHttpServer.getLocalPort());
		tcpConcentrator.setIdleTimeoutMillis(60 * 60 * 1000);
		tcpConcentrator.start();


		HttpClient httpClient = new HttpClient();
		for (int j = 0; j < 10; j++) {
			LOG.fine("client: sending request");
			IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + tcpConcentrator.getLocalPort() + "/test/loopid_" + j));

			Assert.assertEquals(200, response.getStatus());
		}

		httpClient.close();
		tcpConcentrator.close();
		mutliplexedHttpServer.close();
	}


	@Test
	public void testConcurrent() throws Exception {
		
		// start multiplexed http server
		IServer mutliplexedHttpServer = new Server(0, new MultiplexedProtocolAdapter(new HttpProtocolAdapter(new ServerHandler(true))));
		mutliplexedHttpServer.setIdleTimeoutMillis(60 * 60 * 1000);
		mutliplexedHttpServer.start();

		// start tcp concentrator
		final IServer tcpConcentrator = new TcpConcentratorServer(0, "localhost", mutliplexedHttpServer.getLocalPort());
		tcpConcentrator.setIdleTimeoutMillis(60 * 60 * 1000);
		tcpConcentrator.start();



		for (int i = 0; i < 5; i++) {

			final int threadId = i;

			Thread t = new Thread() {
				@Override
				public void run() {
					running++;

					try {

						HttpClient httpClient = new HttpClient();
						for (int j = 0; j < 20; j++) {
							LOG.fine("client: sending request");
							IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + tcpConcentrator.getLocalPort() + "/test/treadid_" + threadId + "/loopid_" + j));

							Assert.assertEquals(200, response.getStatus());
							System.out.print(".");
						}
						httpClient.close();

					} catch (Exception e) {
						e.printStackTrace();
					}

					running--;
				}

			};
			t.start();
		}


		do {
			QAUtil.sleep(300);
		} while (running > 0);


		tcpConcentrator.close();
		mutliplexedHttpServer.close();
	}


	private static final class ServerHandler implements IHttpRequestHandler {


		private boolean isPersistent = true;

		public ServerHandler( boolean isPersistent) {
			this.isPersistent = isPersistent;
		}


		public void onRequest(IHttpExchange exchange) throws IOException {

			IHttpRequest request = exchange.getRequest();
			HttpResponse response = new HttpResponse("text/plain", request.getRequestURI());
			if (!isPersistent) {
				response.setHeader("Connection", "close");
			}

			exchange.send(response);

		}
	}
}
