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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.connection.IServer;




/**
*
* @author grro@xlightweb.org
*/
public final class HttpPipeliningTest  {



	@Test
	public void testPipelining() throws Exception {
		final List<String> results = new ArrayList<String>();

		final IServer server = new HttpServer(new ServerHandler());
		server.start();

		IHttpResponseHandler responseHandler = new IHttpResponseHandler() {

			@Execution(Execution.NONTHREADED)
			@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
			public void onResponse(IHttpResponse response) throws IOException {

				NonBlockingBodyDataSource bodyDataSource = response.getNonBlockingBody();
				String id = bodyDataSource.readStringByLength(bodyDataSource.available());
				results.add(id);
			}
			
			public void onException(IOException ioe) {
			}
		};


		HttpClientConnection httpCon = new HttpClientConnection("localhost", server.getLocalPort());

		for (int i = 0; i < 10; i++) {
			httpCon.send(new GetRequest("/tztz?id=" + i), responseHandler);
		}


		do {
			QAUtil.sleep(300);
		} while (results.size() < 10);


		for (int i = 0; i < 10; i++) {
			if (!results.get(i).equals(Integer.toString(i))) {
				Assert.fail("wrong response order ");
			}
		}


		httpCon.close();
		server.close();

	}



	@Test
	public void testPipeliningWithJetty() throws Exception {
    
		List<String> results = new ArrayList<String>();

		WebContainer servletEngine = new WebContainer(new Servlet());
		servletEngine.start();




		HttpClientConnection httpCon = new HttpClientConnection("localhost", servletEngine.getLocalPort());

		for (int i = 0; i < 10; i++) {
			httpCon.send(new GetRequest("/tztz?id=" + i), new HttpResponseHandler(results));
		}


		do {
			QAUtil.sleep(300);
		} while (results.size() < 10);


		for (int i = 0; i < 10; i++) {
			if (!results.get(i).equals(Integer.toString(i))) {
				Assert.fail("wrong response order ");
			}
		}


		httpCon.close();
		servletEngine.stop();

	}

	

    private class HttpResponseHandler implements IHttpResponseHandler {
        
        private final List<String> results;
        
        public HttpResponseHandler(List<String> results) {
            this.results = results;
        }

        @Execution(Execution.NONTHREADED)
        @InvokeOn(InvokeOn.MESSAGE_RECEIVED)
        public void onResponse(IHttpResponse response) throws IOException {

            NonBlockingBodyDataSource bodyDataSource = response.getNonBlockingBody();
            String id = bodyDataSource.readStringByLength(bodyDataSource.available());
            results.add(id);
        }
        
        public void onException(IOException ioe) {
        }
    };


	private static final class ServerHandler implements IHttpRequestHandler {

		public void onRequest(IHttpExchange exchange) throws IOException {

			QAUtil.sleep(200);

			String id = exchange.getRequest().getParameter("id");


			HttpResponse response = new HttpResponse("text/plain", id);
			exchange.send(response);
		}
	}



	private static final class Servlet extends HttpServlet {

		private static final long serialVersionUID = -2947373502789496225L;

		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			QAUtil.sleep(200);

			String id = request.getParameter("id");

			response.setContentType("text/plain");
			response.getWriter().write(id);
		}
	}
}