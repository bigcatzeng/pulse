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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.BodyDataSink;
import org.xlightweb.BodyForwarder;
import org.xlightweb.GetRequest;
import org.xlightweb.HttpRequestHeader;
import org.xlightweb.HttpResponse;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.NonBlockingBodyDataSource;
import org.xlightweb.PostRequest;
import org.xlightweb.RequestHandlerChain;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.DataConverter;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IServer;





/**
*
* @author grro@xlightweb.org
*/
public final class ServerSideFilterTest  {

    
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 1000; i++) {
            new ServerSideFilterTest().testAuditAndCaesarCipher();
        }
    }
	
	
	@Test
	public void testReplaceRequest() throws Exception {
	    System.out.println("testReplaceRequest");
	
		RequestHandlerChain chain = new RequestHandlerChain();
		
		IHttpRequestHandler filter = new IHttpRequestHandler() {

			public void onRequest(final IHttpExchange exchange) throws IOException {

				PostRequest newRequest = new PostRequest(exchange.getRequest().getRequestUrl().toString(), "text/plain", "Hello");
				
				IHttpResponseHandler respHdl = new IHttpResponseHandler() {
				
					public void onResponse(IHttpResponse response) throws IOException {
						exchange.send(response);
					}
					
					public void onException(IOException ioe) {
						
					}
					
				};
				
				
				exchange.forward(newRequest, respHdl);
			}
		};
		
		
		chain.addLast(filter);
		chain.addLast(new EchoHandler());
		
		IServer server = new HttpServer(0, chain);
		ConnectionUtils.start(server);
		
		
		PostRequest req = new PostRequest("http://localhost:" + server.getLocalPort() + "/", "text/plain", "test123456");
		req.setHeader("Host", "localhost");
		req.setHeader("User-Agent", "me");

		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write(req.toString());
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
			
		String body = con.readStringByLength(contentLength);

		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("Hello", body);
	
		
		con.close();
		server.close();
	}



	
	@Test
	public void testReplaceStreamedRequest() throws Exception {
	    System.out.println("testReplaceStreamedRequest");
	
	    System.setProperty("org.xlightweb.showDetailedError", "true");

	    
		RequestHandlerChain chain = new RequestHandlerChain();
		
		IHttpRequestHandler filter = new IHttpRequestHandler() {

			public void onRequest(final IHttpExchange exchange) throws IOException {
				
				IHttpResponseHandler respHdl = new IHttpResponseHandler() {
				
					public void onResponse(IHttpResponse response) throws IOException {
						exchange.send(response);
					}
					
					public void onException(IOException ioe) {
						
					}
					
				};
				
				
				
				BodyDataSink bodyDataSink = exchange.forward(new HttpRequestHeader("POST", exchange.getRequest().getRequestUrl().toString()), respHdl);
				bodyDataSink.write("Hel");
				QAUtil.sleep(200);
				
				bodyDataSink.write("lo");
				bodyDataSink.close();
			}
		};
		
		
		chain.addLast(filter);
		chain.addLast(new EchoHandler());
		
		IServer server = new HttpServer(chain);
		server.start();
		
		
		
		org.apache.commons.httpclient.HttpClient httpClient = new org.apache.commons.httpclient.HttpClient();
		PostMethod meth = new PostMethod("http://localhost:" + server.getLocalPort() + "/");
		meth.setRequestBody("test123456");

		httpClient.executeMethod(meth);
		
		String body = meth.getResponseBodyAsString();
		Assert.assertEquals("Hello", body);
	
		meth.releaseConnection();
		server.close();
	}


	
	@Test
	public void testReplaceResponse() throws Exception {
	    System.out.println("testReplaceResponse");
	
		RequestHandlerChain chain = new RequestHandlerChain();
		
		IHttpRequestHandler filter = new IHttpRequestHandler() {

			public void onRequest(final IHttpExchange exchange) throws IOException {

				IHttpResponseHandler respHdl = new IHttpResponseHandler() {
				
					public void onResponse(IHttpResponse response) throws IOException {
					
						HttpResponse newResponse = new HttpResponse(200, "text/plain", "Hello You");
						exchange.send(newResponse);
					}
					
					public void onException(IOException ioe) {
						
					}
					
				};
				
				
				exchange.forward(exchange.getRequest(), respHdl);
			}
		};
		
		
		chain.addLast(filter);
		chain.addLast(new EchoHandler());
		
		IServer server = new HttpServer(0, chain);
		ConnectionUtils.start(server);
		
		
		PostRequest req = new PostRequest("http://localhost:" + server.getLocalPort() + "/", "text/plain", "test123456");
		req.setHeader("Host", "localhost");
		req.setHeader("User-Agent", "me");

		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write(req.toString());
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
			
		String body = con.readStringByLength(contentLength);

		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("Hello You", body);
	
		
		con.close();
		server.close();
	}



	@Test
	public void testReplaceResponseChunked() throws Exception {
	    System.out.println("testReplaceResponseChunked");
	
		RequestHandlerChain chain = new RequestHandlerChain();
		
		IHttpRequestHandler filter = new IHttpRequestHandler() {

			public void onRequest(final IHttpExchange exchange) throws IOException {

				IHttpResponseHandler respHdl = new IHttpResponseHandler() {
				
					public void onResponse(IHttpResponse response) throws IOException {
						
						BodyDataSink bodyDataSink = exchange.send(new HttpResponseHeader(200, "text/plain"));
						bodyDataSink.write("Hello");
						QAUtil.sleep(200);
						bodyDataSink.write(" You");
						bodyDataSink.close();					
					}
					
					public void onException(IOException ioe) {
						
					}
					
				};
				
				
				exchange.forward(exchange.getRequest(), respHdl);
			}
		};
		
		
		chain.addLast(filter);
		chain.addLast(new EchoHandler());
		
		IServer server = new HttpServer(0, chain);
		ConnectionUtils.start(server);
		
		
		PostRequest req = new PostRequest("http://localhost:" + server.getLocalPort() + "/", "text/plain", "test123456");
		req.setHeader("Host", "localhost");
		req.setHeader("User-Agent", "me");


		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write(req.toString());
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		Assert.assertTrue(header.indexOf("Transfer-Encoding: chunked") != -1);
		
		int firstChunkLength =  Integer.parseInt(con.readStringByDelimiter("\r\n"), 16);
		String body = con.readStringByLength(firstChunkLength);
		con.readStringByLength(2); // skip CRLF
		
		int secondChunkLength =  Integer.parseInt(con.readStringByDelimiter("\r\n"), 16);
		body += con.readStringByLength(secondChunkLength);
		con.readStringByLength(2); // skip CRLF
		

		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("Hello You", body);

		
	
		
		con.close();
		server.close();
	}


	
	@Test
	public void testReplaceResponseBound() throws Exception {
	    System.out.println("testReplaceResponseBound");
	
		RequestHandlerChain chain = new RequestHandlerChain();
		
		IHttpRequestHandler filter = new IHttpRequestHandler() {

			public void onRequest(final IHttpExchange exchange) throws IOException {

				IHttpResponseHandler respHdl = new IHttpResponseHandler() {
				
					public void onResponse(IHttpResponse response) throws IOException {
						
						BodyDataSink bodyDataSink = exchange.send(new HttpResponseHeader(200, "text/plain"), 9);
						bodyDataSink.write("Hello");
						QAUtil.sleep(200);
						bodyDataSink.write(" You");
						bodyDataSink.close();					
					}
					
					public void onException(IOException ioe) {
						
					}
					
				};
				
				
				exchange.forward(exchange.getRequest(), respHdl);
			}
		};
		
		
		chain.addLast(filter);
		chain.addLast(new EchoHandler());
		
		IServer server = new HttpServer(0, chain);
		ConnectionUtils.start(server);
		
	
		PostRequest req = new PostRequest("http://localhost:" + server.getLocalPort() + "/", "text/plain", "test123456");
		req.setHeader("Host", "localhost");
		req.setHeader("User-Agent", "me");

		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write(req.toString());
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
			
		String body = con.readStringByLength(contentLength);

		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("Hello You", body);
	
		
		con.close();
		server.close();
	}

	
	@Test
	public void testTimestamp() throws Exception {
	    System.out.println("testTimestamp");
	
		RequestHandlerChain chain = new RequestHandlerChain();
		
		chain.addLast(new TimestampHandler());
		chain.addLast(new EchoHandler());
		
		IServer server = new HttpServer(chain);
		ConnectionUtils.start(server);
	
		
		PostRequest req = new PostRequest("http://localhost:" + server.getLocalPort() + "/", "text/plain", "test123456");
		req.setHeader("Host", "localhost");
		req.setHeader("User-Agent", "me");

		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write(req.toString());
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
			
		String body = con.readStringByLength(contentLength);

		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("test123456", body);
	
		Assert.assertTrue(header.indexOf("X-Received") != -1);
		Assert.assertTrue(header.indexOf("X-Sent") != -1);
		
		con.close();
		server.close();
	}
	
	



	@Test
	public void testAudit() throws Exception {
	    System.out.println("testAudit");
	
		RequestHandlerChain chain = new RequestHandlerChain();
		
		AuditFilter auditFilter = new AuditFilter();
		chain.addLast(auditFilter);
		chain.addLast(new EchoHandler());
		
		IServer server = new HttpServer(chain);
		ConnectionUtils.start(server);
		
		
		PostRequest req = new PostRequest("http://localhost:" + server.getLocalPort() + "/", "text/plain", "test123456");
		req.setHeader("Host", "localhost");
		req.setHeader("User-Agent", "me");

		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write(req.toString());
			
		con.readStringByDelimiter("\r\n\r\n");
		
		int firstChunkLength =  Integer.parseInt(con.readStringByDelimiter("\r\n"), 16);
		String body = con.readStringByLength(firstChunkLength);
		con.readStringByLength(2); // skip CRLF
		
		Assert.assertEquals("test123456", body);


		Assert.assertTrue(auditFilter.getRequest().indexOf("POST /") != -1);
		Assert.assertTrue(auditFilter.getRequest().indexOf("test123") != -1);
		
		con.close();
		server.close();
	}

	
	

	@Test
	public void testAuditAndCaesarCipher() throws Exception {
	    System.out.println("testAuditAndCaesarCipher");
		
		RequestHandlerChain chain = new RequestHandlerChain();
		
		CaesarCipherHandler caesarCipherHandler = new CaesarCipherHandler(-1);
		chain.addLast(caesarCipherHandler);
		
		AuditFilter auditHandler = new AuditFilter();
		chain.addLast(auditHandler);
		
		chain.addLast(new EchoHandler());
		
		HttpServer server = new HttpServer(chain);
		server.start();
	
		
		HttpClient httpClient = new HttpClient();
		
		PostRequest req = new PostRequest("http://localhost:" + server.getLocalPort() + "/", "text/plain", "uftu234567");
		req.setHeader("Host", "localhost");
		req.setHeader("User-Agent", "me");


		IHttpResponse response = httpClient.call(req);

		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("uftu234567", response.getBody().toString());
	
		Assert.assertTrue(auditHandler.getRequest().indexOf("POST /") != -1);
		Assert.assertTrue(auditHandler.getRequest().indexOf("test123") != -1);

		httpClient.close();
		server.close();
	}

	
	
	
	@Test
	public void testAuth() throws Exception {
	    System.out.println("testAuth");
		
		IHttpRequestHandler auditFilter = new IHttpRequestHandler() {
			
			private Authenticator authenticator = new Authenticator();
			
			public void onRequest(final IHttpExchange exchange) throws IOException {
				
				IHttpRequest request = exchange.getRequest();
				String authorization = request.getHeader("Authorization");
				if (authorization != null) {
					String[] s = authorization.split(" ");
					if (!s[0].equalsIgnoreCase("BASIC")) {
						exchange.sendError(401);
					}
					
					String decoded = new String(Base64.decodeBase64(s[1].getBytes()));
					String[] userPasswordPair = decoded.split(":");
					
					String authtoken = authenticator.login(userPasswordPair[0], userPasswordPair[1]);

					request.removeHeader("Authorization");
					request.setHeader("X-Authentication", authtoken);
					
					
					IHttpResponseHandler respHdl = new IHttpResponseHandler() {

						public void onResponse(IHttpResponse response) throws IOException {
							exchange.send(response);
							
						}
						
						public void onException(IOException ioe) {
						}
					};
					
					exchange.forward(exchange.getRequest(), respHdl);
	 				return;
				}
				
						
				String authentication = request.getHeader("Authentication");
				if (authentication == null) {
					
					HttpResponse resp = new HttpResponse(401);
					resp.setHeader("WWW-Authenticate", "basic");
					exchange.send(resp);
				}				
			}
		};
		
		
		RequestHandlerChain chain = new RequestHandlerChain();
		chain.addLast(auditFilter);
		chain.addLast(new HeaderInfoServerHandler());
		
		HttpServer server = new HttpServer(0, chain);
		server.setCloseOnSendingError(false);
		ConnectionUtils.start(server);
	
		
		GetRequest req = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
		req.setHeader("Host", "localhost");
		req.setHeader("User-Agent", "me");

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write(req.toString());
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			
		Assert.assertTrue(header.indexOf("401") != -1);
		Assert.assertTrue(header.indexOf("WWW-Authenticate") != -1);
		
		
		req = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
		req.setHeader("Authorization", "Basic YXNkYXNkOmFzZGFzZA==");

		con.write(req.toString());
		
		header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
			
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);

		
		Assert.assertTrue(body.indexOf("X-Authentication") != -1);
		
		con.close();
		server.close();
	}
	
	
	

	
	public static final class AuditFilter implements IHttpRequestHandler {
		
		private StringBuilder requestString = new StringBuilder();
		
		
		public void onRequest(final IHttpExchange exchange) throws IOException {
			
			IHttpRequest req = exchange.getRequest(); 

			requestString.append(req.getRequestHeader().toString());
			
			
			IHttpResponseHandler respHdl = new IHttpResponseHandler() {
				
				public void onResponse(IHttpResponse response) throws IOException {

					// does request contain a body? 
					if (response.hasBody()) {
						
						// get the body 
						NonBlockingBodyDataSource orgDataSource = response.getNonBlockingBody();
						
						// ... and replace it  
						final BodyDataSink inBodyChannel = exchange.send(response.getResponseHeader());
						
						//... by a body forward handler
						BodyForwarder bodyForwardHandler = new BodyForwarder(orgDataSource, inBodyChannel) {
							
							@Override
							public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException, IOException {
								ByteBuffer[] bufs = bodyDataSource.readByteBufferByLength(bodyDataSource.available());
										
								for (ByteBuffer byteBuffer : bufs) {
									// WARNING DataConverter call could fail!
									requestString.append(DataConverter.toString(byteBuffer.duplicate()));
								}
										
								bodyDataSink.write(bufs);
								bodyDataSink.flush();
							}
						};
						orgDataSource.setDataHandler(bodyForwardHandler);
						
					} else {
						exchange.send(response);
					}
				}
				
				public void onException(IOException ioe) {
					
				}
			};
			
			
			// does request contain a body? 
			if (req.hasBody()) {
				
				// get the body 
				NonBlockingBodyDataSource orgDataSource = req.getNonBlockingBody();
				
				// ... and replace it  
				final BodyDataSink inBodyChannel = exchange.forward(req.getRequestHeader(), respHdl);
				
				//... by a body forward handler
				BodyForwarder bodyForwardHandler = new BodyForwarder(orgDataSource, inBodyChannel) {
					
					@Override
					public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException, IOException {
						ByteBuffer[] bufs = bodyDataSource.readByteBufferByLength(bodyDataSource.available());
								
						for (ByteBuffer byteBuffer : bufs) {
							// WARNING DataConverter call could fail!
							requestString.append(DataConverter.toString(byteBuffer.duplicate()));
						}
								
						bodyDataSink.write(bufs);
						bodyDataSink.flush();
					}
				};
				orgDataSource.setDataHandler(bodyForwardHandler);
				
			} else {
				exchange.forward(req, respHdl);
			}
		}
		
		
		String getRequest() {
			return requestString.toString();
		}
	}
	

	public static final class TimestampHandler implements IHttpRequestHandler {
		
		
		public void onRequest(final IHttpExchange exchange) throws IOException {
			
			IHttpRequest req = exchange.getRequest();
			req.addHeader("X-Received", new Date().toString());
			
			
			IHttpResponseHandler responseInterceptor = new IHttpResponseHandler() {

				public void onResponse(IHttpResponse response) throws IOException {
					response.addHeader("X-Sent", new Date().toString());
					exchange.send(response);
				}
				
				public void onException(IOException ioe) {
					
				}
				
			};

			
			exchange.forward(exchange.getRequest(), responseInterceptor);
		}
	}
	
	
	public static final class CaesarCipherHandler implements IHttpRequestHandler {

		private int shift = 0;
		
		public CaesarCipherHandler(int shift) {
			this.shift = shift;
		}
		
		public void onRequest(final IHttpExchange exchange) throws IOException {
	
			IHttpResponseHandler responseInterceptor = new IHttpResponseHandler() {
				
				public void onResponse(IHttpResponse response) throws IOException {

					if (response.hasBody()) {

						// get the body 
						NonBlockingBodyDataSource orgDataSource = response.getNonBlockingBody();

						// ... and replace it  
						final BodyDataSink inBodyChannel = exchange.send(response.getResponseHeader());
						
						//... by a body forward handler
						BodyForwarder bodyForwardHandler = new BodyForwarder(orgDataSource, inBodyChannel) {
							
							@Override
							public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException, IOException {
								byte[] data = bodyDataSource.readBytesByLength(bodyDataSource.available());
										
								for (int i = 0; i < data.length; i++) {
									data[i] = (byte) ((int) data[i] - shift);
								}
																				
								inBodyChannel.write(data);
								inBodyChannel.flush();
							}
						};
						orgDataSource.setDataHandler(bodyForwardHandler);
						
					} else {
						exchange.send(response);
					}
				};
				
				public void onException(IOException ioe) {
					
				}; 
			};
			
			// does request contain a body? 
			if (exchange.getRequest().hasBody()) {
				
				// get the body 
				NonBlockingBodyDataSource orgDataSource = exchange.getRequest().getNonBlockingBody();
				
				// ... and replace it ... 
				final BodyDataSink inBodyChannel = exchange.forward(exchange.getRequest().getRequestHeader(), responseInterceptor);
				
				//... by a body forward handler
				BodyForwarder bodyForwardHandler = new BodyForwarder(orgDataSource, inBodyChannel) {
					
					@Override
					public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException, IOException {
						byte[] data = bodyDataSource.readBytesByLength(bodyDataSource.available());
								
						for (int i = 0; i < data.length; i++) {
							data[i] = (byte) ((int) data[i] + shift);
						}
								
								
						inBodyChannel.write(data);
						inBodyChannel.flush();
					}
				};
				orgDataSource.setDataHandler(bodyForwardHandler);
				
			} else {
				exchange.forward(exchange.getRequest(), responseInterceptor);
			}
		}
	}

	
	
	private static final class EchoHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			HttpResponse response = new HttpResponse(200, "text/plain", exchange.getRequest().getNonBlockingBody());
			
			for (String headerName : exchange.getRequest().getHeaderNameSet()) {
				if (headerName.startsWith("X")) {
					response.setHeader(headerName, exchange.getRequest().getHeader(headerName));
				}
			}
			
			exchange.send(response);
		}
	}
	
	
	public static final class AuthHandler implements IHttpRequestHandler {
		
		private Authenticator authenticator = new Authenticator();
		
		
		public void onRequest(final IHttpExchange exchange) throws IOException {
			
			
			IHttpRequest request = exchange.getRequest();
			String authorization = request.getHeader("Authorization");
			if (authorization != null) {
				String[] s = authorization.split(" ");
				if (!s[0].equalsIgnoreCase("BASIC")) {
					exchange.sendError(401);
				}
				
				String decoded = new String(Base64.decodeBase64(s[1].getBytes()));
				String[] userPasswordPair = decoded.split(":");
				
				String authtoken = authenticator.login(userPasswordPair[0], userPasswordPair[1]);

				request.removeHeader("Authorization");
				request.setHeader("X-Authentication", authtoken);
				
				IHttpResponseHandler respHdl = new IHttpResponseHandler() {

					public void onResponse(IHttpResponse response) throws IOException {
						exchange.send(response);
						
					}
					
					public void onException(IOException ioe) {
					}
				};
				
				exchange.forward(exchange.getRequest(), respHdl);
			}
			
					
			String authentication = request.getHeader("Authentication");
			if (authentication == null) {
				
				HttpResponse resp = new HttpResponse(401);
				resp.setHeader("WWW-Authenticate", "basic");
				exchange.send(resp);
			}
		}
	}
	
	
	private static final class Authenticator {
		
		String login(String username,String password) {
			return "223232323";
		}
		
		boolean authenticate(String authtoken) {
			return "223232323".equals(authtoken);
		}
		
	}
}