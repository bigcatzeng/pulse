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

import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Assert;

import org.junit.Test;
import org.xlightweb.BodyDataSink;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.Server;
import org.xsocket.connection.ConnectionUtils;



/**
*
* @author grro@xlightweb.org
*/
public final class CompareParametersTest {


	@Test
	public void testCompareParameters() throws Exception {

		// start jetty server
		WebContainer servletEngine = new WebContainer(new CompareParametersServlet());
		servletEngine.start();


		// start xSocket
		Server server = new HttpServer(0, new CompareParametersHandler());
		server.start();


		
		// compare calls
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "/test?print=&param2= ");
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "/test?test=12&test=zwei");
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "/over/there/index.dtb;type=animal?name=ferret#nose");
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "/test+2");
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "/test?print=&print=tr");
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "/test?print=tr&print=");
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "/test?print=");
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "/wiki/Comet_%28programming%29");
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "/wiki/Comet_(programming)");
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "/test/test2#fragment");
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "/test/tee?aa=33&bb=44&aa=55");
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "/test?" + URLEncoder.encode("Schl�ssel", "UTF-8") + "=" + URLEncoder.encode("Sch�n", "UTF-8"));
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "/test?key=" + URLEncoder.encode("important&confidential", "UTF-8"));
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "/test/tee?aa=33&bb=44");
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "/test/");


		// shutdown server
		servletEngine.stop();
		server.close();
	}


	
	@Test
	public void testCompareParametersWithServletPath() throws Exception {

		// start jetty server
		WebContainer servletEngine = new WebContainer(new CompareParametersServlet(), "/path");
		servletEngine.start();


		// start xSocket
		Server server = new HttpServer(new CompareParametersHandler());
		ConnectionUtils.start(server);




		// compare calls
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "/path/test/tee?aa=33&bb=44&aa=55");


		// shutdown server
		servletEngine.stop();
		server.close();
	}

	

	private void callAndCompare(int jettyPort, int xSocketPort, String path) throws IOException {

		HttpClient httpClient = new HttpClient();

		GetMethod jettyMethod = new GetMethod("http://localhost:" + jettyPort + path);
		int jettyStatusCode = httpClient.executeMethod(jettyMethod);
		String jettyResponse = jettyMethod.getResponseBodyAsString().trim();
		jettyMethod.releaseConnection();


		GetMethod xSocketMethod = new GetMethod("http://localhost:" + xSocketPort + path);
		int xSocketStatusCode = httpClient.executeMethod(xSocketMethod);
		String xSocketResponse = xSocketMethod.getResponseBodyAsString().trim();
		xSocketMethod.releaseConnection();

		Assert.assertEquals(jettyStatusCode, xSocketStatusCode);
		Assert.assertEquals(jettyResponse, xSocketResponse);
	}




	private static final class CompareParametersHandler implements IHttpRequestHandler {


		public void onRequest(IHttpExchange exchange) throws IOException {

			// send response
			HttpResponseHeader responseHeader = new HttpResponseHeader(200, "text/plain");

			IHttpRequest request = exchange.getRequest();
			
			BodyDataSink bodyDataSink = exchange.send(responseHeader);
			bodyDataSink.write("requestUri=" + request.getRequestURI() + "\r\n");
			bodyDataSink.write("queryString=" + request.getQueryString() + "\r\n");

			Set<String> paramNames = new TreeSet<String>(request.getParameterNameSet());
			for (String key : paramNames) {
				String[] values = request.getParameterValues(key);
				for (String value : values) {
					bodyDataSink.write("[param] " + key  + "=" + value + "\r\n");	
				}
			}


			bodyDataSink.close();
		}
	}



	private static final class CompareParametersServlet extends HttpServlet {


		private static final long serialVersionUID = 5414556868896021677L;

		@SuppressWarnings("unchecked")
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.setContentType("text/plain");
			PrintWriter writer = resp.getWriter();
			writer.write("requestUri=" + req.getRequestURI() + "\r\n");
			writer.write("queryString=" + req.getQueryString() + "\r\n");

			Set<String> paramNames = new TreeSet<String>(req.getParameterMap().keySet());

			for (String key : paramNames) {
				String[] values = req.getParameterValues(key);
				for (String value : values) {
					String txt = "[param] " + key  + "=" + value + "\r\n";
					writer.write(txt);	
				}
			}



			writer.close();
		}
	}
}
