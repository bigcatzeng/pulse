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


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xlightweb.server.SessionManager;
import org.xsocket.connection.ConnectionUtils;


/**
*
* @author grro@xlightweb.org
*/
public final class HttpSessionTest {
	

    @Ignore
	@Test 
	public void testClientSide() throws Exception {

		// start jetty server
		WebContainer servletEngine = new WebContainer(new MyServlet(), "/path");
		servletEngine.start();
		
		HttpClient httpClient = new HttpClient();
		httpClient.setAutoHandleCookies(true);

		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + servletEngine.getLocalPort()+ "/path/test"));
		Assert.assertEquals("1", response.getBody().readString());
		Assert.assertNotNull(response.getHeader("Set-Cookie"));
		
		response = httpClient.call(new GetRequest("http://localhost:" + servletEngine.getLocalPort()+ "/path/test"));
		Assert.assertEquals("2", response.getBody().readString());
		Assert.assertNull(response.getHeader("Set-Cookie"));
		
		response = httpClient.call(new GetRequest("http://localhost:" + servletEngine.getLocalPort()+ "/path/test"));
		Assert.assertEquals("3", response.getBody().readString());
		Assert.assertNull(response.getHeader("Set-Cookie"));
		
		httpClient.close();
		servletEngine.stop();
	}
	
	

	@Test 
	public void testClientSide2() throws Exception {

		HttpServer server = new HttpServer(new RequestHandler());
		server.start();
		
		HttpClient httpClient = new HttpClient();
		httpClient.setAutoHandleCookies(true);
		
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() +  "/path/test"));
	
		Assert.assertEquals(2, response.getHeaderList("Set-Cookie").size());
		
		
		httpClient.close();
		server.close();
	}
	
	
	

	@Test 
	public void testSeverSide() throws Exception {

		HttpServer server = new HttpServer(new RequestHandler2());
		server.start();
		ConnectionUtils.registerMBean(server);
		
		HttpClient httpClient = new HttpClient();
		httpClient.setAutoHandleCookies(true);
		
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/path/test"));
		Assert.assertEquals("1", response.getBody().readString());
		Assert.assertNotNull(response.getHeader("Set-Cookie"));
		
		response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/path/test"));
		Assert.assertEquals("2", response.getBody().readString());
		Assert.assertNull(response.getHeader("Set-Cookie"));
		
		response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/path/test"));
		Assert.assertEquals("3", response.getBody().readString());
		Assert.assertNull(response.getHeader("Set-Cookie"));
		
		httpClient.close();
		server.close();
	}
	
	
	@Test 
    public void testInactiveTimeout() throws Exception {

        HttpServer server = new HttpServer(new RequestHandler2());
        server.setSessionMaxInactiveIntervalSec(1);
        server.start();

        SessionManager sessionManager = (SessionManager) server.getSessionManager();
        
        ConnectionUtils.registerMBean(server);
        
        HttpClient httpClient = new HttpClient();
        
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());

        response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        Assert.assertEquals(200, response.getStatus());
        
        QAUtil.sleep(500);
        Assert.assertEquals(0, sessionManager.getNumExpiredSessions());
        
        QAUtil.sleep(6000);   // cleaner runs each 5 sec!
        Assert.assertEquals(1, sessionManager.getNumExpiredSessions());
        
        httpClient.close();
        server.close();
    }
    
	

	@Test 
	public void testSecuredCookie() throws Exception {

		HttpServer server = new HttpServer(0, new RequestHandler2());
		server.start();

		
		HttpServer securedServer = new HttpServer(0, new RequestHandler2(), SSLTestContextFactory.getSSLContext(), true);
		securedServer.start();
		
		HttpClient httpClient = new HttpClient(SSLTestContextFactory.getSSLContext());

		IHttpResponse response = httpClient.call(new GetRequest("https://localhost:" + securedServer.getLocalPort() + "/path/test"));
		Assert.assertEquals("1", response.getBody().readString());
		Assert.assertTrue(response.getHeader("Set-Cookie").indexOf("secure") != -1);
		
		response = httpClient.call(new GetRequest("https://localhost:" + securedServer.getLocalPort() + "/path/test"));
		Assert.assertEquals("2", response.getBody().readString());
		Assert.assertNull(response.getHeader("Set-Cookie"));
		
		GetRequest request = new GetRequest("https://localhost:" + securedServer.getLocalPort() + "/path/test");
		response = httpClient.call(request);
		Assert.assertNotNull(request.getHeader("Cookie"));
		
		Assert.assertEquals("3", response.getBody().readString());
		Assert.assertNull(response.getHeader("Set-Cookie"));

		
		request = new GetRequest("http://localhost:" + server.getLocalPort() + "/path/test");
		response = httpClient.call(request);
		
		Assert.assertNull(request.getHeader("Cookie"));
		
		Assert.assertEquals("1", response.getBody().readString());

		
		httpClient.close();
		server.close();
		securedServer.close();
	}
	
	
	

	@Ignore
	@Test 
	public void testInvalidCookieJetty() throws Exception {

		// start jetty server
		WebContainer servletEngine = new WebContainer(new MyServlet(), "/path");
		servletEngine.start();
		
		HttpClient httpClient = new HttpClient();
		httpClient.setAutoHandleCookies(true);

		GetRequest request = new GetRequest("http://localhost:" + servletEngine.getLocalPort()+ "/path/test");
		request.setHeader("Cookie",  "JSESSIONID=c39bb635-528c-4763-b4ae-0a880553c339");
		IHttpResponse response = httpClient.call(request);
		
		Assert.assertEquals(1, response.getHeaderList("Set-Cookie").size());
		
		httpClient.close();
		servletEngine.stop();
	}
	

	@Test 
	public void testServerInvalidatesCookie() throws Exception {


		HttpServer server = new HttpServer(0, new RequestHandler4());
		server.start();

		
		HttpClient httpClient = new HttpClient();
		httpClient.setAutoHandleCookies(true);

		GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/path/test");
		IHttpResponse response = httpClient.call(request);
		Assert.assertEquals(1, response.getHeaderList("Set-Cookie").size());

		request = new GetRequest("http://localhost:" + server.getLocalPort() + "/path/test");
		response = httpClient.call(request);
		Assert.assertNull(response.getHeader("Set-Cookie"));

		request = new GetRequest("http://localhost:" + server.getLocalPort() + "/path/test");
		response = httpClient.call(request);
		Assert.assertNull(response.getHeader("Set-Cookie"));

		request = new GetRequest("http://localhost:" + server.getLocalPort() + "/path/test");
		response = httpClient.call(request);
		Assert.assertNull(response.getHeader("Set-Cookie"));


		
		httpClient.close();
		server.close();
	}
	

	
	@Test 
	public void testInvalidCookieLightweb() throws Exception {

		HttpServer server = new HttpServer(new RequestHandler2());
		server.start();

		
		HttpClient httpClient = new HttpClient();
		httpClient.setAutoHandleCookies(true);

		GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/path/test");
		request.setHeader("Cookie",  "JSESSIONID=c39bb635-528c-4763-b4ae-0a880553c339");
		IHttpResponse response = httpClient.call(request);
		
		Assert.assertEquals(1, response.getHeaderList("Set-Cookie").size());
		
		httpClient.close();
		server.close();
	}

	@Ignore
	@Test 
	public void testCookieTimeoutJetty() throws Exception {
	
		// start jetty server
		WebContainer servletEngine = new WebContainer(new MyServlet2(), "/path");
		servletEngine.start();
		
		HttpClient httpClient = new HttpClient();
		httpClient.setAutoHandleCookies(true);
	
		GetRequest request = new GetRequest("http://localhost:" + servletEngine.getLocalPort()+ "/path/test");
		IHttpResponse response = httpClient.call(request);
		
		Assert.assertEquals(1, response.getHeaderList("Set-Cookie").size());
		
		httpClient.close();
		servletEngine.stop();
	}


	
	


	
	@Test 
	public void testSessionWithoutCookies() throws Exception {
	
		HttpServer server = new HttpServer(new RequestHandler8());
		server.setUsingCookies(false);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		httpClient.setAutoHandleCookies(true);
	
		GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/path/test");
		IHttpResponse response = httpClient.call(request);
		String body = response.getBody().readString(); 
		
		Assert.assertTrue(body.indexOf("counter=1") != -1);
		int start = body.indexOf("<a href=\"") + "<a href=\"".length();
		int end = body.indexOf("\"", start);
		String url = body.substring(start, end);
		
		
		request = new GetRequest("http://localhost:" + server.getLocalPort() + "/path/test" + url);
		response = httpClient.call(request);
		body = response.getBody().readString(); 

		Assert.assertTrue(body.indexOf("counter=2") != -1);
		
		httpClient.close();
		server.close();
	}

	
	@Test 
	public void testHttpSessionRequestHandlerContextScope() throws Exception {

	
		
		Context root = new Context("");
		
		
		Context ctx1 = new Context(root, "/ctx1");
		RequestHandler7 reqHdl1 = new RequestHandler7();
		ctx1.addHandler("/*", reqHdl1);
		
		Context ctx2 = new Context(root, "/ctx2");
		RequestHandler7 reqHdl2 = new RequestHandler7();
		ctx2.addHandler("/*", reqHdl2);
		
		HttpServer server = new HttpServer(root);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		httpClient.setAutoHandleCookies(true);
	
		GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/ctx1/test");
		IHttpResponse response = httpClient.call(request);
		Assert.assertTrue("session=null".equals(response.getBody().readString()));
		
		request = new GetRequest("http://localhost:" + server.getLocalPort() + "/ctx2/test");
		response = httpClient.call(request);
		Assert.assertTrue("session=null".equals(response.getBody().readString()));

		
		request = new GetRequest("http://localhost:" + server.getLocalPort() + "/ctx1/test?createSession=true");
		response = httpClient.call(request);
		Assert.assertFalse("session=null".equals(response.getBody().readString()));

		request = new GetRequest("http://localhost:" + server.getLocalPort() + "/ctx1/test");
		response = httpClient.call(request);
		Assert.assertFalse("session=null".equals(response.getBody().readString()));

		
		
		request = new GetRequest("http://localhost:" + server.getLocalPort() + "/ctx2/test");
		response = httpClient.call(request);
		Assert.assertTrue("session=null".equals(response.getBody().readString()));

		
		
		httpClient.close();
		server.close();
	}


	
	
	@Test 
	public void testCookieWithoutValue() throws Exception {
	
		HttpServer server = new HttpServer(new RequestHandler6());
		server.start();

		
		HttpClient httpClient = new HttpClient();
		httpClient.setAutoHandleCookies(true);
	
		GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/path/test");
		IHttpResponse response = httpClient.call(request);
		
		Assert.assertEquals(1, response.getHeaderList("Set-Cookie").size());
		
		request = new GetRequest("http://localhost:" + server.getLocalPort() + "/path/test");
		response = httpClient.call(request);
		
		Assert.assertEquals("test=", request.getHeader("Cookie"));
		
		httpClient.close();
		server.close();
	}

	
	
	
	
	@Test 
	public void testMaxSessionInterval() throws Exception {
	
		HttpServer server = new HttpServer(new RequestHandler5());
		server.start();
		
		HttpClient httpClient = new HttpClient();
		httpClient.setAutoHandleCookies(true);
		
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() +  "/path/test"));
	
		Assert.assertEquals(1, response.getHeaderList("Set-Cookie").size());
		
		
		GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() +  "/path/test");
		response = httpClient.call(request);
		Assert.assertNotNull(request.getHeader("Cookie"));
		
		QAUtil.sleep(7000);
		request = new GetRequest("http://localhost:" + server.getLocalPort() +  "/path/test");
		response = httpClient.call(request);
		Assert.assertEquals(1, response.getHeaderList("Set-Cookie").size());

		
		httpClient.close();
		server.close();
	}


	private static final class MyServlet extends HttpServlet {

		private static final long serialVersionUID = 6775691244546606841L;

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			HttpSession session = req.getSession(true);
			
			Integer counter = (Integer) session.getAttribute("counter");
			if (counter == null) {
				counter = 0;
			}
			
			session.setAttribute("counter", ++counter);
			
			resp.setContentType("text/plain");
			resp.getWriter().write(Integer.toString(counter));
		}
	}
	

	private static final class MyServlet2 extends HttpServlet {

		private static final long serialVersionUID = 6775691244546606841L;

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			HttpSession session = req.getSession(true);
			session.setMaxInactiveInterval(10);
		
			
			Integer counter = (Integer) session.getAttribute("counter");
			if (counter == null) {
				counter = 0;
			}
			
			session.setAttribute("counter", ++counter);
			
			resp.setContentType("text/plain");
			resp.getWriter().write(Integer.toString(counter));
		}
	}

	

	private static final class MyServlet3 extends HttpServlet {

		private static final long serialVersionUID = 6775691244546606841L;

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			HttpSession session = req.getSession(true);
		
			System.out.println(req.getRequestURI());
			
			Integer counter = (Integer) session.getAttribute("counter");
			if (counter == null) {
				counter = 0;
			}
			
			session.setAttribute("counter", ++counter);
			
			resp.setContentType("text/html");
			resp.getWriter().write("<html><body> <a href=\"" + resp.encodeURL("/me?param1=1") + "\">link</a> counter=" + counter + "</body></html>");
		}
	}
	
	
	
	private static final class RequestHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			
			IHttpResponse response = new HttpResponse(200, "text/plain", "OK");
			response.addHeader("Set-Cookie", "param1=10qhhk6ntkxcc;Path=/");
			response.addHeader("Set-Cookie", "param2=20qhhk6ntkxcc;Path=/");

			exchange.send(response);
		}		
	}
	
	
	private static final class RequestHandler2 implements IHttpRequestHandler {
	    
	    private int numCalls = 0;
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			
			IHttpSession session = exchange.getSession(true);
			
			Integer counter = (Integer) session.getAttribute("counter");
			if (counter == null) {
				counter = 0;
			}
			
			session.setAttribute("counter", ++counter);
			
			exchange.send(new HttpResponse(200, "text/plain", Integer.toString(counter)));
		}
		
		
		int getNumCalls() {
		    return numCalls;
		}
	}
	
	
	private static final class RequestHandler4 implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			
			IHttpSession session = exchange.getSession(true);
			
			Integer counter = (Integer) session.getAttribute("counter");
			if (counter == null) {
				counter = 0;
			}
		
			if (counter >= 3) {
				session.invalidate();
			} else {
				session.setAttribute("counter", ++counter);
			}
			
			exchange.send(new HttpResponse(200, "text/plain", Integer.toString(counter)));
		}		
	}
	
	

	private static final class RequestHandler5 implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			
			IHttpSession session = exchange.getSession(true);
			session.setMaxInactiveInterval(1);
			
			Integer counter = (Integer) session.getAttribute("counter");
			if (counter == null) {
				counter = 0;
			}
			
			session.setAttribute("counter", ++counter);
			
			exchange.send(new HttpResponse(200, "text/plain", Integer.toString(counter)));
		}		
	}
	
	

	private static final class RequestHandler6 implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			
			HttpResponse response = new HttpResponse(200, "text/plain", "test");
			response.setHeader("Set-Cookie", "test=");
			exchange.send(response);
		}		
	}
	
	
	private static final class RequestHandler7 implements IHttpRequestHandler {
		
		private IHttpSession session = null;
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			
			IHttpRequest request = exchange.getRequest();
			
			boolean createSessionIfNotExists = request.getBooleanParameter("createSession", false);
			session = exchange.getSession(createSessionIfNotExists);
			
			if (session != null) {
				exchange.send(new HttpResponse("session=" + session.hashCode()));
			} else {
				exchange.send(new HttpResponse("session=null"));
			}
		}
		
		IHttpSession getSession() {
			return session;
		}
	}
	
	
	private static final class RequestHandler8 implements IHttpRequestHandler {
		
		private IHttpSession session = null;
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			
			exchange.getRequest();  // retrieve request
			IHttpSession session = exchange.getSession(true);
			
			Integer counter = (Integer) session.getAttribute("counter");
			if (counter == null) {
				counter = 0;
			}
			
			session.setAttribute("counter", ++counter);
			
			IHttpResponse response = new HttpResponse("<html><body> <a href=\"" + exchange.encodeURL("/me?param1=1") + "\">link</a> counter=" + counter + "</body></html>"); 
			exchange.send(response);
		}
		
		IHttpSession getSession() {
			return session;
		}
	}
	
	
	

}

