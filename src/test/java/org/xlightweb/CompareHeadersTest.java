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
import java.util.Enumeration;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;

import org.junit.Test;
import org.xlightweb.BodyDataSink;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.Server;
import org.xsocket.connection.ConnectionUtils;



/**
*
* @author grro@xlightweb.org
*/
public final class CompareHeadersTest {


	@Test
	public void testHeadersParameters() throws Exception {

		
		// start jetty server
		WebContainer servletEngine = new WebContainer(new CompareHeadersServlet());
		servletEngine.start();


		// start xSocket
		Server server = new HttpServer(0, new CompareHeadersHandler());
		ConnectionUtils.start(server);



		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5",
														 "Accept-Language: de-de,de;q=0.8,en-us;q=0.5,en;q=0.3" ,
														 "Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7",
														 "Keep-Alive: 300");
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "HeaderName1: test1", "HeaderName2: test2");
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "Key: test, test3");
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort());
		callAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "HeaderName: test", "HeaderName: test2", "HeaderName: test3");


		// shutdown server
		servletEngine.stop();
		server.close();
	}



	@Test
	public void testHeadersFolding() throws Exception {


		// start jetty server
		WebContainer servletEngine = new WebContainer(new CompareHeadersServlet());
		servletEngine.start();


		// start xSocket
		Server server = new HttpServer(0, new CompareHeadersHandler());
		ConnectionUtils.start(server);

		callFoldedAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "Key", "partOne", " \t \tpartTwo");
		callFoldedAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "Key", "partOne", "\tpartTwo");
		callFoldedAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "Key", "partOne", " partTwo");
		callFoldedAndCompare(servletEngine.getLocalPort(), server.getLocalPort(), "Key", "partOne", "    partTwo");


		// shutdown server
		servletEngine.stop();
		server.close();
	}



	private void callAndCompare(int jettyPort, int xSocketPort, String... headers) throws IOException {

		String jettyResponse = call(jettyPort, headers);
		String xSocketResponse = call(xSocketPort, headers);

		Assert.assertEquals(jettyResponse, xSocketResponse);
	}



	private String call(int port, String... headers) throws IOException {

		IBlockingConnection con = new BlockingConnection("localhost", port);
		con.setAutoflush(false);

		con.write("GET / HTTP/1.1\r\n");
		con.write("Host: localhost:" + port + "\r\n");
		for (String hds : headers) {
			con.write(hds + "\r\n");
		}
		con.write("\r\n");
		con.flush();


		String responseHeaders = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int length = 0;
		for (String header : responseHeaders.split("\r\n")) {
			if (header.toUpperCase().startsWith("CONTENT-LENGTH")) {
				length = Integer.parseInt(header.substring("Content-Length:".length(), header.length()).trim());
			}
		}

		return con.readStringByLength(length);
	}


	private void callFoldedAndCompare(int jettyPort, int xSocketPort, String headerName, String headerValuePart1, String headerValuePart2) throws IOException {

		String jettyResponse = callFolded(jettyPort, headerName, headerValuePart1, headerValuePart2);
		String xSocketResponse = callFolded(xSocketPort, headerName, headerValuePart1, headerValuePart2);

		Assert.assertEquals(jettyResponse, xSocketResponse);
	}


	private String callFolded(int port, String headerName, String headerValuePart1, String headerValuePart2) throws IOException {

		IBlockingConnection con = new BlockingConnection("localhost", port);
		con.setAutoflush(false);

		con.write("GET / HTTP/1.1\r\n");
		con.write("Host: localhost:" + port + "\r\n");
		con.write(headerName + ": " + headerValuePart1 + "\r\n");
		con.write(headerValuePart2 + "\r\n");
		con.write("\r\n");
		con.flush();


		String responseHeaders = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int length = 0;
		for (String header : responseHeaders.split("\r\n")) {
			if (header.toUpperCase().startsWith("CONTENT-LENGTH")) {
				length = Integer.parseInt(header.substring("Content-Length:".length(), header.length()).trim());
			}
		}

		return con.readStringByLength(length);
	}


	
	@Test
	public void contentTypeTest() throws Exception {
	    	
		// start jetty server
		WebContainer servletEngine = new WebContainer(new ContentTypeServlet ());
		servletEngine.start();


		// start xSocket
		Server server = new HttpServer(0, new ContentTypeRequestHandler());
		server.start();

		
		HttpClient httpClient = new HttpClient();
		
		
		PostRequest request = new PostRequest("http://localhost:" + servletEngine.getLocalPort() + "/test", new NameValuePair("key1", "value1"), new NameValuePair("key2", "value2"));
		IHttpResponse response = httpClient.call(request);
		Assert.assertTrue(response.getBody().readString().startsWith("application/x-www-form-urlencoded"));
		
		request = new PostRequest("http://localhost:" + server.getLocalPort() + "/test", new NameValuePair("key", "value1"), new NameValuePair("key2", "value2"));
		response = httpClient.call(request);
		Assert.assertTrue(response.getBody().readString().startsWith("application/x-www-form-urlencoded"));

		
		
		httpClient.close();
		servletEngine.stop();
		server.close();
	}



	private static final class CompareHeadersHandler implements IHttpRequestHandler {


		@SuppressWarnings("unchecked")
		public void onRequest(IHttpExchange exchange) throws IOException {

			// send response
			HttpResponseHeader responseHeader = new HttpResponseHeader(200, "text/plain");


			IHttpRequest request = exchange.getRequest();
			
			HashSet<String> headerSet = new HashSet<String>();
			for (Enumeration en = request.getHeaderNames(); en.hasMoreElements(); ) {
				String headerName = (String) en.nextElement();
				if (!headerName.equalsIgnoreCase("Host")) {
					for (Enumeration en2 = request.getHeaders(headerName); en2.hasMoreElements(); ) {
						String headerValue = (String) en2.nextElement();
						String txt = "[header] " + headerName  + "=" + headerValue + "\r\n";
						headerSet.add(txt);
					}
				}
			}

			StringBuilder sb = new StringBuilder();
			for (String txt : headerSet) {
				sb.append(txt);
			}


			String value = request.getHeader("headerNAME");
			if (value != null) {
				sb.append("HeadernameFound=true\r\n");
			}

			String value2 = request.getHeader("Accept-Language");
			if (value2 != null) {
				sb.append("Accept-Language -> " + value2 + "\r\n");
			}




			byte[] data = sb.toString().getBytes("ISO-8859-1");


			BodyDataSink bodyDataSink = exchange.send(responseHeader, data.length);
			bodyDataSink.write(data);
			bodyDataSink.close();
		}
	}



	private static final class CompareHeadersServlet extends HttpServlet {

		private static final long serialVersionUID = 8183044648040068422L;

		@SuppressWarnings("unchecked")
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.setContentType("text/plain");

			PrintWriter writer = resp.getWriter();

			HashSet<String> headerSet = new HashSet<String>();
			for (Enumeration en = req.getHeaderNames(); en.hasMoreElements(); ) {
				String headerName = (String) en.nextElement();
				if (!headerName.equalsIgnoreCase("Host")) {
					for (Enumeration en2 = req.getHeaders(headerName); en2.hasMoreElements(); ) {
						String headerValue = (String) en2.nextElement();
						String txt = "[header] " + headerName  + "=" + headerValue + "\r\n";
						headerSet.add(txt);
					}
				}
			}

			for (String txt : headerSet) {
				writer.write(txt);
			}


			String value = req.getHeader("headerNAME");
			if (value != null) {
				writer.write("HeadernameFound=true\r\n");
			}

			String value2 = req.getHeader("Accept-Language");
			if (value2 != null) {
				writer.write("Accept-Language -> " + value2 + "\r\n");
			}

			writer.close();
		}
	}
	
	
	private static final class ContentTypeServlet extends HttpServlet {
		
		private static final long serialVersionUID = 2303026419004518170L;

		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.getWriter().write(req.getContentType());
		}
	}
	
	
	private static final class ContentTypeRequestHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			exchange.send(new HttpResponse(200, exchange.getRequest().getContentType()));
		}
	}
}
