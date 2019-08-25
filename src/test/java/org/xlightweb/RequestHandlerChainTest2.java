/*
 *  Copyright (c) xsocket.org, 2006 - 2009. All rights reserved.
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
import java.util.concurrent.atomic.AtomicReference;



import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpConnectHandler;
import org.xlightweb.IHttpConnection;
import org.xlightweb.IHttpDisconnectHandler;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.InvokeOn;
import org.xlightweb.RequestHandlerChain;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.ILifeCycle;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.ConnectionUtils;




/**
*
* @author grro@xlightweb.org
*/
public final class RequestHandlerChainTest2 {

 
	@Test 
	public void testLifeCycle() throws Exception {

		RequestHandlerChain root = new RequestHandlerChain();
		
		RequestHandler h1 = new RequestHandler();
		root.addLast(h1);
		
		RequestHandler h2 = new RequestHandler();
		root.addLast(h2);
		
		IServer server = new HttpServer(root);
		ConnectionUtils.start(server);
		
		Assert.assertEquals(1, h1.getCountOnInitCalled());
		Assert.assertEquals(1, h2.getCountOnInitCalled());
		
		server.close();
		
		Assert.assertEquals(1, h1.getCountOnDestroyCalled());
		Assert.assertEquals(1, h2.getCountOnDestroyCalled());
	}

	
	@Test 
	public void testRequestHandler() throws Exception {

		RequestHandlerChain root = new RequestHandlerChain();
		
		RequestFilter h1 = new RequestFilter();
		root.addLast(h1);
		
		RequestHandler h2 = new RequestHandler();
		root.addLast(h2);
		
		IServer server = new HttpServer(root);
		server.start();
		
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
			
		con.readByteBufferByLength(contentLength);

		
		QAUtil.sleep(400);
		
		if (h1.countOnRequestCalled() != 1) {
			String msg = "RequestFilter should haven been called once not " + h1.countOnRequestCalled();
			System.out.println(msg);
			Assert.fail(msg);	
		}
		
		
		if (h2.countOnRequestCalled() != 1) {
			String msg = "RequestHandler should haven been called once not " + h2.countOnRequestCalled();
			System.out.println(msg);
			Assert.fail(msg);	
		}

		
		if (!h2.getOnRequestThreadname().startsWith("xWorker")) {
			String msg = "RequestHandler should be executed by xWorker not by " + h2.getOnRequestThreadname();
			System.out.println(msg);
			Assert.fail(msg);
		}
		
		con.close();
		server.close();
	}

	
	@Test 
	public void testRequestHandlerNonThreaded() throws Exception {

		RequestHandlerChain root = new RequestHandlerChain();
		
		RequestFilter h1 = new RequestFilter();
		root.addLast(h1);
		
		NonThreadedRequestHandler h2 = new NonThreadedRequestHandler();
		root.addLast(h2);
		
		IServer server = new HttpServer(root);
		server.start();
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
			
		con.readByteBufferByLength(contentLength);
		
		
		QAUtil.sleep(200);
		
		Assert.assertEquals(1, h1.countOnRequestCalled());
		Assert.assertFalse(h1.onRequestThreadname.startsWith("xWorker"));
		Assert.assertEquals(1, h2.countOnRequestCalled());
		Assert.assertFalse(h2.onRequestThreadname.startsWith("xWorker"));
		
		con.close();
		server.close();
	}
	
	
	@Test 
	public void testRequestHandlerMessage() throws Exception {
	    
		RequestHandlerChain root = new RequestHandlerChain();
		
		MessageRequestFilter h1 = new MessageRequestFilter();
		root.addLast(h1);
		
		MessageRequestHandler h2 = new MessageRequestHandler();
		root.addLast(h2);
		
		IServer server = new HttpServer(root);
		server.start();
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("POST / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "Content-Length: 4\r\n" +
				  "\r\n");
			
		
		QAUtil.sleep(500);

		Assert.assertEquals(0, h1.countOnRequestCalled());
		Assert.assertEquals(0, h2.countOnRequestCalled());

		con.write("1234");

		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
			
		con.readByteBufferByLength(contentLength);
		

		
		QAUtil.sleep(200);
		
		Assert.assertEquals(1, h1.countOnRequestCalled());
		Assert.assertEquals(1, h2.countOnRequestCalled());
		Assert.assertTrue(h2.getOnRequestThreadname().startsWith("xWorker"));
		
		con.close();
		server.close();
	}


	
	
	@Test 
	public void testChainInChain() throws Exception {

		RequestHandlerChain root = new RequestHandlerChain();
		
		
		RequestHandlerChain c1 = new RequestHandlerChain();
		root.addLast(c1);
		
		RequestFilter h1 = new RequestFilter();
		c1.addLast(h1);
		
		RequestHandler h2 = new RequestHandler();
		c1.addLast(h2);
		
		IServer server = new HttpServer(root);
		ConnectionUtils.start(server);
		
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
			
		con.readByteBufferByLength(contentLength);

		
		QAUtil.sleep(400);
		
		if (h1.countOnRequestCalled() != 1) {
			String msg = "RequestFilter should haven been called once not " + h1.countOnRequestCalled();
			System.out.println(msg);
			Assert.fail(msg);	
		}
		
		
		if (h2.countOnRequestCalled() != 1) {
			String msg = "RequestHandler should haven been called once not " + h2.countOnRequestCalled();
			System.out.println(msg);
			Assert.fail(msg);	
		}

		
		if (!h2.getOnRequestThreadname().startsWith("xWorker")) {
			String msg = "RequestHandler should be executed by xWorker not by " + h2.getOnRequestThreadname();
			System.out.println(msg);
			Assert.fail(msg);
		}
		
		con.close();
		server.close();
	}
	


	@Test 
	public void testChainInChain2() throws Exception {

		RequestHandlerChain root = new RequestHandlerChain();

		RequestFilter h1 = new RequestFilter();
		root.addLast(h1);

		
		RequestHandlerChain c1 = new RequestHandlerChain();
		root.addLast(c1);
		
		
		RequestHandler h2 = new RequestHandler();
		c1.addLast(h2);
		
		IServer server = new HttpServer(root);
		ConnectionUtils.start(server);
		
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
			
		con.readByteBufferByLength(contentLength);

		
		QAUtil.sleep(400);
		
		if (h1.countOnRequestCalled() != 1) {
			String msg = "RequestFilter should haven been called once not " + h1.countOnRequestCalled();
			System.out.println(msg);
			Assert.fail(msg);	
		}
		
		
		if (h2.countOnRequestCalled() != 1) {
			String msg = "RequestHandler should haven been called once not " + h2.countOnRequestCalled();
			System.out.println(msg);
			Assert.fail(msg);	
		}

		
		if (!h2.getOnRequestThreadname().startsWith("xWorker")) {
			String msg = "RequestHandler should be executed by xWorker not by " + h2.getOnRequestThreadname();
			System.out.println(msg);
			Assert.fail(msg);
		}
		
		con.close();
		server.close();
	}
	

	


	
	

	@Test 
	public void testRequestTimeout() throws Exception {

		RequestHandlerChain root = new RequestHandlerChain();
		
		RequestHandler h1 = new RequestHandler();
		h1.setOnConnectResponse(false);
		h1.setOnRequestTimeoutResponse(false);
		root.addLast(h1);
		
		RequestHandler h2 = new RequestHandler();
		h1.setOnRequestTimeoutResponse(false);
		root.addLast(h2);
		
		HttpServer server = new HttpServer(root);
		server.setRequestTimeoutMillis(1000);
		ConnectionUtils.start(server);
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

			
		
		QAUtil.sleep(1500);
		
		Assert.assertEquals(1, h1.getCountOnRequestTimeoutCalled());
		Assert.assertTrue(h1.onRequestTimeoutThreadname.startsWith("xWorker"));
		Assert.assertEquals(1, h2.getCountOnRequestTimeoutCalled());
		Assert.assertTrue(h2.onRequestTimeoutThreadname.startsWith("xWorker"));
		
		
		con.close();
		server.close();
	}
	
	
	@Test 
	public void testRequestTimeoutNonThreaded() throws Exception {

		RequestHandlerChain root = new RequestHandlerChain();
		
		NonThreadedRequestHandler h1 = new NonThreadedRequestHandler();
		h1.setOnConnectResponse(false);
		h1.setOnRequestTimeoutResponse(false);
		root.addLast(h1);
		
		NonThreadedRequestHandler h2 = new NonThreadedRequestHandler();
		h1.setOnRequestTimeoutResponse(false);
		root.addLast(h2);
		
		HttpServer server = new HttpServer(root);
		server.setRequestTimeoutMillis(1000);
		server.start();
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		QAUtil.sleep(1500);
		
		Assert.assertEquals(1, h1.getCountOnRequestTimeoutCalled());
		Assert.assertTrue(h1.onRequestTimeoutThreadname.startsWith("xHttpTimer"));
		Assert.assertEquals(1, h2.getCountOnRequestTimeoutCalled());
		Assert.assertTrue(h2.onRequestTimeoutThreadname.startsWith("xHttpTimer"));
		

		con.close();
		server.close();
	}
	
	
	
	@Test 
	public void testMixedExecutionMode() throws Exception {

		RequestHandlerChain root = new RequestHandlerChain();

		Filter filter = new Filter();
		root.addLast(filter);
		
		NonThreadedRequestHandler rh = new NonThreadedRequestHandler();
		root.addLast(rh);
		
		HttpServer server = new HttpServer(root);
		server.setRequestTimeoutMillis(1000);
		ConnectionUtils.start(server);
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("POST / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "Content-Length: 4\r\n" +
				  "\r\n" +
				  "1234");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		
		con.readByteBufferByLength(contentLength);
		Assert.assertTrue(header.indexOf("200") != -1);
		
		Assert.assertNotNull(filter.getRequest());
		Assert.assertTrue(filter.getThreadName().startsWith("xWorker"));
		Assert.assertTrue(rh.onRequestThreadname.startsWith("xWorker"));
		
		con.close();
		server.close();
	}
	
	
	@Test 
	public void testMixedInvokeOnMode() throws Exception {
		

		RequestHandlerChain root = new RequestHandlerChain();

		OnMessageFilter filter = new OnMessageFilter();
		root.addLast(filter);
		
		NonThreadedRequestHandler rh = new NonThreadedRequestHandler();
		root.addLast(rh);
		
		HttpServer server = new HttpServer(root);
		server.setRequestTimeoutMillis(1000);
		ConnectionUtils.start(server);
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("POST / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "Content-Length: 4\r\n" +
				  "\r\n" +
				  "1234");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		
		con.readByteBufferByLength(contentLength);
		Assert.assertTrue(header.indexOf("200") != -1);
				
		Assert.assertNotNull(filter.getRequest());
		Assert.assertTrue(filter.getThreadName().startsWith("xWorker"));
		Assert.assertTrue(rh.onRequestThreadname.startsWith("xWorker"));
		
		
		server.close();
	}
	
	
	
	@Test 
	public void testMixedThreaded() throws Exception {
		
		RequestHandlerChain root = new RequestHandlerChain();

		OnMessageFilter filter = new OnMessageFilter();
		root.addLast(filter);
		
		NonThreadedRequestHandler rh = new NonThreadedRequestHandler();
		root.addLast(rh);
		
		HttpServer server = new HttpServer(root);
		server.setRequestTimeoutMillis(1000);
		server.start();
		ConnectionUtils.registerMBean(server);
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("POST / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "Content-Length: 4\r\n" +
				  "\r\n" +
				  "1234");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		
		con.readByteBufferByLength(contentLength);
		Assert.assertTrue(header.indexOf("200") != -1);
		
		
		Assert.assertNotNull(filter.getRequest());
		Assert.assertTrue(filter.getThreadName().startsWith("xWorker"));
		Assert.assertTrue(rh.onRequestThreadname.startsWith("xWorker"));
		
		
		server.close();
	}
	

	@Test
	public void testReadingBlockingBodyWithinServerHandler() throws Exception {
		
		RequestHandlerChain chain = new RequestHandlerChain();
		chain.addLast(new RequestFilter());
		chain.addLast(new BlockingReadHandler());
		
		
		final IServer server = new HttpServer(chain);
		ConnectionUtils.start(server);

		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("POST / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "Content-Length: 4\r\n" +
				  "\r\n");
			
		con.write("12");

		QAUtil.sleep(400);
		con.write("34");
		
		QAUtil.sleep(500);
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		
		String body = con.readStringByLength(contentLength);
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("1234", body);
		


		server.close();
	}
	
	
	
	
	
	
	@Test 
	public void testEmptyChain() throws Exception {
	
		RequestHandlerChain root = new RequestHandlerChain();
		
		IServer server = new HttpServer(root);
		ConnectionUtils.start(server);
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
	
		con.write("POST / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "Content-Length: 4\r\n" +
				  "\r\n" +
				  "1234");
			
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("404") != -1);
	}



	@Test 
	public void testUnhandledRequest() throws Exception {
	
		RequestHandlerChain root = new RequestHandlerChain();
		DoNothingFilter doNothingFilter = new DoNothingFilter();
		root.addLast(doNothingFilter);
		
		IServer server = new HttpServer(root);
		ConnectionUtils.start(server);
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
	
		con.write("POST /test/path?param1=value1 HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "Content-Length: 4\r\n" +
				  "\r\n" +
				  "1234");
			
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("404") != -1);
		Assert.assertTrue(doNothingFilter.isOnResponseCalled());
	}


	
	


	@Execution(Execution.NONTHREADED)
	private static final class RequestFilter implements IHttpRequestHandler {
		
		private int countOnRequestCalled = 0;
		private String onRequestThreadname = null;

		
		
		public void onRequest(final IHttpExchange exchange) throws IOException {
			countOnRequestCalled++;
			onRequestThreadname = Thread.currentThread().getName();	
			
			IHttpResponseHandler respHdl = new IHttpResponseHandler() {
				
				public void onResponse(IHttpResponse response) throws IOException {
					exchange.send(response);
				}
				
				public void onException(IOException ioe) {
				}
			};
			
			exchange.forward(exchange.getRequest(), respHdl);
		}
		
		
		int countOnRequestCalled() {
			return countOnRequestCalled;
		}
		
		String getOnRequestThreadname() {
			return onRequestThreadname;
		}
	}

	
	
	@Execution(Execution.NONTHREADED)
	@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
	private static final class MessageRequestFilter implements IHttpRequestHandler {
		
		private int countOnRequestCalled = 0;
		private AtomicReference<String> onRequestThreadname = new AtomicReference<String>();

		
		
		public void onRequest(final IHttpExchange exchange) throws IOException {
			countOnRequestCalled++;
			onRequestThreadname.set(Thread.currentThread().getName());		
			
			IHttpResponseHandler respHdl = new IHttpResponseHandler() {
				
				public void onResponse(IHttpResponse response) throws IOException {
					exchange.send(response);
				}
				
				public void onException(IOException ioe) {
				}
			};
			
			exchange.forward(exchange.getRequest(), respHdl);
		}
		
		
		int countOnRequestCalled() {
			return countOnRequestCalled;
		}
		
		String getOnRequestThreadname() {
			return onRequestThreadname.get();
		}
	}

	
	
	private static final class Filter implements IHttpRequestHandler {
		
		private IHttpRequest request = null;
		private String onRequestThreadname = null;
		
		public void onRequest(final IHttpExchange exchange) throws IOException {
			this.request = exchange.getRequest();
			this.onRequestThreadname = Thread.currentThread().getName();
			
			IHttpResponseHandler respHdl = new IHttpResponseHandler() {
				
				public void onResponse(IHttpResponse response) throws IOException {
					exchange.send(response);
				}
				
				public void onException(IOException ioe) {
				}
			};
			
			exchange.forward(exchange.getRequest(), respHdl);
		}
		
		String getThreadName() {
			return onRequestThreadname;
		}
		
		IHttpRequest getRequest() {
			return request;
		}
	}
	
	private static final class OnMessageFilter implements IHttpRequestHandler {
		
		private IHttpRequest request = null;
		private String onRequestThreadname = null;
		
		@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
		public void onRequest(final IHttpExchange exchange) throws IOException {
			this.request = exchange.getRequest();
			this.onRequestThreadname = Thread.currentThread().getName();
			
			IHttpResponseHandler respHdl = new IHttpResponseHandler() {
				
				public void onResponse(IHttpResponse response) throws IOException {
					exchange.send(response);
				}
				
				public void onException(IOException ioe) {
				}
			};
			
			exchange.forward(exchange.getRequest(), respHdl);
		}
		
		String getThreadName() {
			return onRequestThreadname;
		}
		
		IHttpRequest getRequest() {
			return request;
		}
	}

	
	
	private static final class DoNothingFilter implements IHttpRequestHandler {
		
		private boolean onResponseCalled = false;
		
		public void onRequest(final IHttpExchange exchange) throws IOException {
			
			
			IHttpResponseHandler respHdl = new IHttpResponseHandler() {
				
				public void onResponse(IHttpResponse response) throws IOException {
					onResponseCalled = true;
					exchange.send(response);
				}
				
				public void onException(IOException ioe) {
				}
			};
			
			exchange.forward(exchange.getRequest(), respHdl);
		}	
		
		public boolean isOnResponseCalled() {
			return onResponseCalled;
		}
	}

	
	private static final class RequestHandler implements IHttpRequestHandler, IHttpRequestTimeoutHandler, IHttpConnectHandler, IHttpDisconnectHandler, ILifeCycle {

		private int countOnInitCalled = 0;
		private int countOnDestroyCalled = 0;

		private int countOnConnectCalled = 0;
		private String onConnectThreadname = null;
		private boolean onConnectResponse = true;
		
		private int countOnRequestCalled = 0;
		private String onRequestThreadname = null;

		private int countOnRequestTimeoutCalled = 0;
		private String onRequestTimeoutThreadname = null;
		private boolean onRequestTimeoutResponse = true;
	
		private int countOnDisconnectCalled = 0;
		private String onDisconnectThreadname = null;
		private boolean onDisconnectResponse = true;
		
		public void onInit() {
			countOnInitCalled++;
		}
		
		public void onDestroy() throws IOException {
			countOnDestroyCalled++;
		}
		
		
		public boolean onConnect(IHttpConnection httpConnection) throws IOException {
			countOnConnectCalled++;
			onConnectThreadname = Thread.currentThread().getName();
			
			return onConnectResponse;
		}
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			countOnRequestCalled++;
			onRequestThreadname = Thread.currentThread().getName();
			
			exchange.send(new HttpResponse(200, "text/plain", "OK"));
		}
		
		public boolean onRequestTimeout(IHttpConnection connection) throws IOException {
			countOnRequestTimeoutCalled++;
			onRequestTimeoutThreadname = Thread.currentThread().getName();
			
			return onRequestTimeoutResponse;
		}
		
		public boolean onDisconnect(IHttpConnection httpConnection) throws IOException {
			countOnDisconnectCalled++;
			onDisconnectThreadname = Thread.currentThread().getName();
			
			return onDisconnectResponse;
		}
		
		int getCountOnInitCalled() {
			return countOnInitCalled;
		}
		
		int getCountOnDestroyCalled() {
			return countOnDestroyCalled;
		}
		
		int countOnRequestCalled() {
			return countOnRequestCalled;
		}
		
		String getOnRequestThreadname() {
			return onRequestThreadname;
		}

		public boolean isOnConnectResponse() {
			return onConnectResponse;
		}

		public void setOnConnectResponse(boolean onConnectResponse) {
			this.onConnectResponse = onConnectResponse;
		}

		public int getCountOnConnectCalled() {
			return countOnConnectCalled;
		}

		public String getOnConnectThreadname() {
			return onConnectThreadname;
		}

		public int getCountOnRequestCalled() {
			return countOnRequestCalled;
		}
		
		public void setOnDisconnectResponse(boolean onDisconnectResponse) {
			this.onDisconnectResponse = onDisconnectResponse;
		}
		
		public int getCountOnDisconnectCalled() {
			return countOnDisconnectCalled;
		}

		public String getOnDisconnectThreadname() {
			return onDisconnectThreadname;
		}
		
		
		public void setOnRequestTimeoutResponse(boolean onRequestTimeoutResponse) {
			this.onRequestTimeoutResponse = onRequestTimeoutResponse;
		}
		
		public int getCountOnRequestTimeoutCalled() {
			return countOnRequestTimeoutCalled;
		}

		public String getOnRequestTimeoutThreadname() {
			return onRequestTimeoutThreadname;
		}
	}

	
	@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
	private static final class MessageRequestHandler implements IHttpRequestHandler, ILifeCycle {

		
		private int countOnInitCalled = 0;
		private int countOnDestroyCalled = 0;

		private int countOnRequestCalled = 0;
		private AtomicReference<String> onRequestThreadname = new AtomicReference<String>();

		
		public void onInit() {
			countOnInitCalled++;
		}
		
		public void onDestroy() throws IOException {
			countOnDestroyCalled++;
		}
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			countOnRequestCalled++;
			onRequestThreadname.set(Thread.currentThread().getName());
			
			exchange.send(new HttpResponse(200, "text/plain", "OK"));
		}
		
		int getCountOnInitCalled() {
			return countOnInitCalled;
		}
		
		int getCountOnDestroyCalled() {
			return countOnDestroyCalled;
		}
		
		int countOnRequestCalled() {
			return countOnRequestCalled;
		}
		
		String getOnRequestThreadname() {
			return onRequestThreadname.get();
		}
	}

	
	@Execution(Execution.NONTHREADED)
	private static final class NonThreadedRequestHandler implements IHttpRequestHandler, IHttpRequestTimeoutHandler, IHttpConnectHandler, IHttpDisconnectHandler, ILifeCycle {

		private int countOnInitCalled = 0;
		private int countOnDestroyCalled = 0;

		private int countOnRequestCalled = 0;
		private String onRequestThreadname = null;
		
		private int countOnConnectCalled = 0;
		private String onConnectThreadname = null;
		private boolean onConnectResponse = true;

		private int countOnRequestTimeoutCalled = 0;
		private String onRequestTimeoutThreadname = null;
		private boolean onRequestTimeoutResponse = true;

		private int countOnDisconnectCalled = 0;
		private String onDisconnectThreadname = null;
		private boolean onDisconnectResponse = true;
		
		
		
		public void onInit() {
			countOnInitCalled++;
		}
		
		public void onDestroy() throws IOException {
			countOnDestroyCalled++;
		}
		
		
		
		public boolean onConnect(IHttpConnection httpConnection) throws IOException {
			countOnConnectCalled++;
			onConnectThreadname = Thread.currentThread().getName();
			
			return onConnectResponse;
		}
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			countOnRequestCalled++;
			onRequestThreadname = Thread.currentThread().getName();
			
			exchange.send(new HttpResponse(200, "text/plain", "OK"));
		}
		
		public boolean onRequestTimeout(IHttpConnection connection) throws IOException {
			countOnRequestTimeoutCalled++;
			onRequestTimeoutThreadname = Thread.currentThread().getName();
			
			return onRequestTimeoutResponse;
		}
		
		public boolean onDisconnect(IHttpConnection httpConnection) throws IOException {
			countOnDisconnectCalled++;
			onDisconnectThreadname = Thread.currentThread().getName();
			
			return onDisconnectResponse;
		}
		
		int getCountOnInitCalled() {
			return countOnInitCalled;
		}
		
		int getCountOnDestroyCalled() {
			return countOnDestroyCalled;
		}
		
		int countOnRequestCalled() {
			return countOnRequestCalled;
		}
		
		String onRequestThreadname() {
			return onRequestThreadname;
		}
		
		public void setOnConnectResponse(boolean onConnectResponse) {
			this.onConnectResponse = onConnectResponse;
		}

		
		public int getCountOnConnectCalled() {
			return countOnConnectCalled;
		}

		public String getOnConnectThreadname() {
			return onConnectThreadname;
		}

		public int getCountOnRequestCalled() {
			return countOnRequestCalled;
		}
		
		
		public void setOnDisconnectResponse(boolean onDisconnectResponse) {
			this.onDisconnectResponse = onDisconnectResponse;
		}
		
		public int getCountOnDisconnectCalled() {
			return countOnDisconnectCalled;
		}

		public String getOnDisconnectThreadname() {
			return onDisconnectThreadname;
		}
		
		public void setOnRequestTimeoutResponse(boolean onRequestTimeoutResponse) {
			this.onRequestTimeoutResponse = onRequestTimeoutResponse;
		}
		
		public int getCountOnRequestTimeoutCalled() {
			return countOnRequestTimeoutCalled;
		}

		public String getOnRequestTimeoutThreadname() {
			return onRequestTimeoutThreadname;
		}
	}
	
	
	
	
	private static final class BlockingReadHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {

			String body = exchange.getRequest().getBody().readString();
			exchange.send(new HttpResponse(200, "text/plain", body));
			
		}
	}
}
