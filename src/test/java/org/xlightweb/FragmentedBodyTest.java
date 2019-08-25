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
import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IServer;
import org.xsocket.connection.IConnection.FlushMode;





/**
*
* @author grro@xlightweb.org
*/
public final class FragmentedBodyTest  {



	@Test
	public void testFragmented() throws Exception {
		MyServerHandler srvHdl = new MyServerHandler();
		IServer server = new HttpServer(srvHdl);
		server.start();


		ProxyHandler prxHdl = new ProxyHandler("localhost", server.getLocalPort());
		IServer proxy = new HttpServer(prxHdl);
		proxy.start();


		HttpClientConnection clientCon = new HttpClientConnection("localhost", proxy.getLocalPort());


		IHttpResponseHandler responseHandler = new IHttpResponseHandler() {

			public void onResponse(IHttpResponse response) throws IOException {
			}

			public void onException(IOException ioe) {
			}
		};


		BodyDataSink bodyDataSink = clientCon.send(new HttpRequestHeader("POST", "/", "text/plain; charset=ISO-8859-1"), responseHandler);
		bodyDataSink.setAutoflush(false);

		bodyDataSink.write("1");
		bodyDataSink.flush();
		QAUtil.sleep(200);

		Assert.assertTrue(prxHdl.isRequestReceived());
		Assert.assertFalse(srvHdl.isRequestReceived());


		bodyDataSink.write("2");
		bodyDataSink.close();
		QAUtil.sleep(200);

		Assert.assertTrue(srvHdl.isRequestReceived());


		QAUtil.sleep(300);

		clientCon.close();
		proxy.close();
		server.close();
	}





	private static final class ProxyHandler implements IHttpRequestHandler {

		private boolean requestReceived = false;

		private HttpClientConnection clientCon = null;



		public ProxyHandler(String host, int port) throws IOException {
			clientCon = new HttpClientConnection(host, port);
		}



		public void onRequest(final IHttpExchange exchange) throws IOException {

			requestReceived = true;
			IHttpResponseHandler responseHandler = new IHttpResponseHandler() {

				public void onResponse(IHttpResponse response) throws IOException {
					exchange.send(response);
				}
	
				public void onException(IOException ioe) {
				}
			};

			clientCon.send(exchange.getRequest(), responseHandler);
		}



		public boolean isRequestReceived() {
			return requestReceived;
		}
	}



	private static final class MyServerHandler implements IHttpRequestHandler {

		private boolean requestReceived = false;


		@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
		public void onRequest(IHttpExchange exchange) throws IOException {
			requestReceived = true;

			IHttpRequest request = exchange.getRequest();
			NonBlockingBodyDataSource bodyChannel = request.getNonBlockingBody();
			ByteBuffer[] data = bodyChannel.readByteBufferByLength(bodyChannel.available());
			HttpResponseHeader responseHeader = new HttpResponseHeader(200, request.getContentType());

			BodyDataSink bodyDataSink = exchange.send(responseHeader);
			bodyDataSink.setFlushmode(FlushMode.ASYNC);
			for (ByteBuffer buffer : data) {
				bodyDataSink.write(buffer);
				QAUtil.sleep(200);
			}
			bodyDataSink.close();
		}



		public boolean isRequestReceived() {
			return requestReceived;
		}
	}



}
