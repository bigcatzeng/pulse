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
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;


import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.ILifeCycle;
import org.xsocket.connection.ConnectionUtils;


/**
*
* @author grro@xlightweb.org
*/
public final class HttpProxy extends HttpServer {



	public HttpProxy(int listenport, String forwardHost, int forwardPort, boolean isPersistent, int responseTimeoutServiceSec, int responseTimeoutBackendcallSec) throws Exception {
		super(listenport, new ForwardHandler(forwardHost, forwardPort, isPersistent, responseTimeoutBackendcallSec));
		setCloseOnSendingError(true);

		setRequestTimeoutMillis(responseTimeoutServiceSec * 1000L);
		
		ConnectionUtils.registerMBean(this, "test.httpproxy");
		System.out.println("proxy forwarding to " + forwardHost + ":" + forwardPort);
	}





	public static void main(String... args) throws Exception {
		
		if (args.length < 5) {
			System.out.println("usage java org.xsocket.connection.http.HttpProxy <listenport> <forwardhost> <forwardport> <isPersistent> <responsetimeoutMillis>");
			System.exit(-1);
		}

		new HttpProxy(Integer.parseInt(args[0]), args[1], Integer.parseInt(args[2]), Boolean.parseBoolean(args[3]), Integer.parseInt(args[4]) + 1000, Integer.parseInt(args[4])).run();
	}





	public static class ForwardHandler implements IHttpRequestHandler, ILifeCycle {



		private String host = null;
		private int port = 0;
		private boolean isPersistent = true;
		private int responseTimeoutSec = 0;

		private HttpClient httpClient = null;


		// statistics
		private int countRequest = 0;
		private long timestampRequest = System.currentTimeMillis();



		public ForwardHandler(String host, int port, boolean isPersistent, int responseTimeoutSec) {
			this.host = host;
			this.port = port;
			this.isPersistent = isPersistent;
			this.responseTimeoutSec = responseTimeoutSec;
		}



		public void onInit() {
			httpClient = new HttpClient();
			httpClient.setResponseTimeoutMillis(responseTimeoutSec * 1000L);
			httpClient.setBodyDataReceiveTimeoutMillis(responseTimeoutSec * 1000L);
		}


		public void onDestroy() throws IOException {
			httpClient.close();
		}



		@Execution(Execution.NONTHREADED)
		public void onRequest(IHttpExchange exchange) throws IOException {
			countRequest++;
			
			
			IHttpRequest request = exchange.getRequest();
			
			URL url = request.getRequestUrl();
			URL newUrl = new URL(url.getProtocol(), host, port, url.getFile());
			
			
			// reset address (Host header will be update automatically)
			request.setRequestUrl(newUrl);

			
			request.removeHopByHopHeaders();
	
			try {
				// .. and forward the request
				httpClient.send(request, new ReverseHandler(exchange, isPersistent));
			} catch (ConnectException ce) {
				exchange.sendError(502, ce.toString());
			}
		}



		int getCountRequestPerSec() {
			long elapsed = System.currentTimeMillis() - timestampRequest;

			if (elapsed > 0) {
				int requests = countRequest;

				countRequest = 0;
				timestampRequest = System.currentTimeMillis();

				int rate = (int) ((requests * 1000) / elapsed);
				return rate;


			} else {
				return -1;
			}
		}


	}


	public static final class ReverseHandler implements IHttpResponseHandler, IHttpSocketTimeoutHandler {

		private IHttpExchange exchange = null;
		private boolean isPersistent = false;

		public ReverseHandler(IHttpExchange exchange, boolean isPersistent) {
			this.exchange = exchange;
			this.isPersistent = isPersistent;
		}

		
		@Execution(Execution.NONTHREADED)
		public void onResponse(IHttpResponse response) throws IOException {

			// remove the Server header (new header will be set by sending automatically)
			response.removeHeader("Server");
			response.removeHopByHopHeaders();

			if (!isPersistent) {
				// set the connection close header (http client will close the connection)
				response.setHeader("Connection", "close");
			}

			exchange.send(response);
		}
		
		
		public void onException(IOException ioe) {
			exchange.sendError(ioe);
		}
		
		
		public void onException(SocketTimeoutException stoe) {
			exchange.sendError(504, stoe.getMessage());
		}
	}
}
