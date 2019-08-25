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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.DataConverter;
import org.xsocket.Execution;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;





/**
*
* @author grro@xlightweb.org
*/
public final class ClientSideFilterTest  {
	
     
    private static final Logger LOG = Logger.getLogger(ClientSideFilterTest.class.getName());
    
	
	public static void main(String[] args) throws Exception {
		
	 
		for (int i = 0; i < 1000; i++) {
			new ClientSideFilterTest().testAuditResponseOnly();
		}
	}
	
	
    @Test
    public void testAuditResponseOnly() throws Exception {
        System.out.println("testAuditResponseOnly");
        
        IServer server = new HttpServer(new EchoHandler());
        server.start();
        

        HttpClient httpClient = new HttpClient();
        
        ResponseAuditHandler auditInterceptor = new ResponseAuditHandler();
        httpClient.addInterceptor(auditInterceptor);
        
        IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort()+ "/", "text/plain", "fest123456"));
        
        
        Assert.assertEquals(200, response.getStatus());
        
        String body = response.getBody().readString();
        if (body.indexOf("fest1234") == -1) {
            System.out.println("got wrong content " + body);
            Assert.fail();
        }
        
        QAUtil.sleep(1000);
        if (auditInterceptor.getResponseString().indexOf("fest123") == -1) {
            System.out.println("got wrong interceptor response " + auditInterceptor.getResponseString());
            Assert.fail();
        }
        
        httpClient.close();
        server.close();
    }

    
	
    @Test
    public void testAuditResponseOnlyDestroy() throws Exception {
        System.out.println("testAuditResponseOnlyDestroy");
        
        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException,ClosedChannelException, MaxReadSizeExceededException {
                connection.readStringByDelimiter("\r\n\r\n");
                
                connection.write("HTTP/1.1 200 OK\r\n" +
                                 "Server: xLightweb/2.5.1-SNAPSHOT\r\n" +
                                 "Content-Length: 10\r\n" +
                                 "Content-Type: text/plain; charset=iso-8859-1\r\n" +
                                 "\r\n" +
                                 "fest123");
                connection.close();
                return true;
            }
        };
        IServer server = new Server(dh);
        server.start();
        

        HttpClient httpClient = new HttpClient();
        
        ResponseAuditHandler auditInterceptor = new ResponseAuditHandler();
        httpClient.addInterceptor(auditInterceptor);
        
        IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort()+ "/", "text/plain", "fest123456"));
        
        
        Assert.assertEquals(200, response.getStatus());
        
        try {
            response.getBody().readString();
            Assert.fail("IOException expected");
        } catch (IOException expected) { }
        
        httpClient.close();
        server.close();
    }



    @Test
    public void testAuditRequestOnly() throws Exception {
        System.out.println("testAuditRequestOnly");
        
        IServer server = new HttpServer(new EchoHandler());
        server.start();
        

        HttpClient httpClient = new HttpClient();
        
        RequestAuditHandler auditInterceptor = new RequestAuditHandler();
        httpClient.addInterceptor(auditInterceptor);
        
        IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort()+ "/", "text/plain", "fest123456"));
        
        
        Assert.assertEquals(200, response.getStatus());
        
        String body = response.getBody().readString();
        if (body.indexOf("fest1234") == -1) {
            System.out.println("got wrong content " + body);
            Assert.fail();
        }
        
        QAUtil.sleep(1000);
        if (auditInterceptor.getRequestString().indexOf("fest123") == -1) {
            System.out.println("got wrong interceptor request " + auditInterceptor.getRequestString());
            Assert.fail();
        }
        
        httpClient.close();
        server.close();
    }

    

    @Test
    public void testAuditRequestOnlyDestroy() throws Exception {
        System.out.println("testAuditRequestOnlyDestroy");
        
        IDataHandler dh = new IDataHandler() {
          
            public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException,ClosedChannelException, MaxReadSizeExceededException {
                connection.readStringByDelimiter("\r\n\r\n");
                
                connection.write("HTTP/1.1 200 OK\r\n" +
                                 "Server: xLightweb/2.5.1-SNAPSHOT\r\n" +
                                 "Content-Length: 10\r\n" +
                                 "Content-Type: text/plain; charset=iso-8859-1\r\n" +
                                 "\r\n" +
                                 "fest123");
                connection.close();
                return true;
            }
        };
        IServer server = new Server(dh);
        server.start();
        

        HttpClient httpClient = new HttpClient();
        
        RequestAuditHandler auditInterceptor = new RequestAuditHandler();
        httpClient.addInterceptor(auditInterceptor);
        
        IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort()+ "/", "text/plain", "fest123456"));
        
        
        Assert.assertEquals(200, response.getStatus());
        
        try {
            response.getBody().readString();
            Assert.fail("ProtocolException expected");
        } catch (ProtocolException expected) { }
        
        httpClient.close();
        server.close();
    }
    


	@Test
	public void testAudit() throws Exception {
	    
	    System.out.println("testAudit");
	    
		IServer server = new HttpServer(new EchoHandler());
		server.start();
		

		HttpClient httpClient = new HttpClient();
		
		AuditHandler auditInterceptor = new AuditHandler();
		httpClient.addInterceptor(auditInterceptor);
		
		IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort()+ "/", "text/plain", "fest123456"));
		
		
		Assert.assertEquals(200, response.getStatus());
		
		String body = response.getBody().readString();
		if (body.indexOf("fest1234") == -1) {
			System.out.println("got wrong content " + body);
			Assert.fail();
		}
		
		QAUtil.sleep(1000);
		
		if (auditInterceptor.getRequestString().indexOf("fest123") == -1) {
			System.out.println("got wrong interceptor request " + auditInterceptor.getRequestString());
			Assert.fail();
		}
		
		if (auditInterceptor.getResponseString().indexOf("fest123") == -1) {
			System.out.println("got wrong interceptor response " + auditInterceptor.getResponseString());
			Assert.fail();
		}
		
		httpClient.close();
		server.close();
	}

	
	
	@Test
	public void testAudit2() throws Exception {
	    
	    System.out.println("testAudit2");
		
		IServer server = new HttpServer(new EchoHandler());
		server.start();
		

		HttpClient httpClient = new HttpClient();
		
		AuditHandler auditInterceptor = new AuditHandler();
		httpClient.addInterceptor(auditInterceptor);

		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		BodyDataSink outChannel = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort()+ "/", "text/plain; charset=iso-8859-1"), respHdl);
		outChannel.write("fest1234567");
		outChannel.close();
		
		IHttpResponse response = respHdl.getResponse(); 
		
		QAUtil.sleep(1000);

		Assert.assertEquals(200, response.getStatus());
		
		Assert.assertTrue(response.getBody().readString().indexOf("fest1234") != -1);
		Assert.assertTrue(auditInterceptor.getRequestString().indexOf("fest123") != -1);
		Assert.assertTrue(auditInterceptor.getResponseString().indexOf("fest123") != -1);


		httpClient.close();
		server.close();
	}



	@Test
	public void testReplace() throws Exception {
	    System.out.println("testReplace");
		
		IServer server = new HttpServer(new EchoHandler());
		server.start();
		

		HttpClient httpClient = new HttpClient();

		ReplaceHandler replaceHandler = new ReplaceHandler();
		httpClient.addInterceptor(replaceHandler);

		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		BodyDataSink outChannel = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort()+ "/", "text/plain"), respHdl);
		outChannel.write("fest1234567");
		outChannel.close();
		
		Assert.assertEquals(200, respHdl.getResponse().getStatus());
		Assert.assertEquals("you and me", respHdl.getResponse().getBody().readString());
		
		httpClient.close();
		server.close();
	}

	

	@Ignore
	@Test
	public void testNonThreadedRequestAndResponseHandler() throws Exception {
	    System.out.println("testNonThreadedRequestAndResponseHandler");
		
		Thread.currentThread().setName("testthread");
		
		IServer server = new HttpServer(new EchoHandler());
		server.start();
		
		HttpClient httpClient = new HttpClient();
		
		NonThreadedRequestHandler interceptor = new NonThreadedRequestHandler();
		httpClient.addInterceptor(interceptor);

		
		IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort()+ "/", "text/plain", "fest123456"));
		
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("fest123456", response.getBody().readString());
		Assert.assertTrue(interceptor.getThreadnameRequest().startsWith("testthread"));
		Assert.assertTrue(interceptor.getThreadnameResponse().startsWith("xDispatcher"));
		
		httpClient.close();
		server.close();
	}



	
	@Test
	public void testMultiThreadedRequestAndResponseHandler() throws Exception {
	    System.out.println("testMultiThreadedRequestAndResponseHandler");
		
		Thread.currentThread().setName("testthread");
		
		IServer server = new HttpServer(new EchoHandler());
		server.start();
		
		HttpClient httpClient = new HttpClient();
		
		MultiThreadedRequestHandler interceptor = new MultiThreadedRequestHandler();
		httpClient.addInterceptor(interceptor);

		
		IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort()+ "/", "text/plain", "fest123456"));
		
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("fest123456", response.getBody().readString());
		
		Assert.assertTrue(interceptor.getThreadnameRequest().indexOf("ool-") != -1);
		Assert.assertTrue(interceptor.getThreadnameResponse().startsWith("xNbcPool"));
		
		httpClient.close();
		server.close();
	}


	@Test
	public void testInvokeOnMessageRequestAndResponseHandler() throws Exception {
	    System.out.println("testInvokeOnMessageRequestAndResponseHandler");
		
		Thread.currentThread().setName("testthread");
		
		IServer server = new HttpServer(new EchoHandler());
		server.start();
		
		HttpClient httpClient = new HttpClient();
	
		InvokeOnMessageRequestHandler interceptor = new InvokeOnMessageRequestHandler();
		httpClient.addInterceptor(interceptor);

		
		ResponseHandler respHdl = new ResponseHandler();
		BodyDataSink outChannel = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort()+ "/", "text/plain"), respHdl);
		outChannel.write("fest1234567");

		Assert.assertNull(interceptor.getRequest());
		Assert.assertNull(interceptor.getResponse());
		
		
		outChannel.close();
		
		QAUtil.sleep(1000);
		
		Assert.assertEquals(200, respHdl.getResponse().getStatus());
		Assert.assertEquals("fest1234567", respHdl.getResponse().getBody().readString());
		Assert.assertNotNull(interceptor.getRequest());
		Assert.assertNotNull(interceptor.getResponse());
		
		httpClient.close();
		server.close();
	}


	
	@Test
	public void testRequestHandlerChain() throws Exception {
	    System.out.println("testRequestHandlerChain");

		IServer server = new HttpServer(new EchoHandler());
		server.start();
		
		HttpClient httpClient = new HttpClient();

		RequestHandlerChain chain = new RequestHandlerChain();
		chain.addLast(new HeaderEnhancer());
		chain.addLast(new AuditHandler());
		httpClient.addInterceptor(chain);

		IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort()+ "/", "text/plain", "fest123456"));
		
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("passed", response.getHeader("X-Request"));
		Assert.assertEquals("passed", response.getHeader("X-Response"));
		Assert.assertEquals("fest123456", response.getBody().readString());
		

		
		httpClient.close();
		server.close();
	}

	

	@SuppressWarnings("deprecation")
	@Test
	public void testRedirectCall() throws Exception {
	    System.out.println("testRedirectCall");

		Context ctx = new Context("");
		ctx.addHandler("/redirect/*", new RedirectHandler());
		ctx.addHandler("/*", new EchoHandler());
		
		IServer server = new HttpServer(ctx);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		httpClient.setFollowsRedirect(true);
		
		AuditHandler interceptor = new AuditHandler();
		httpClient.addInterceptor(interceptor);

		
		String url = URLEncoder.encode("http://localhost:" + server.getLocalPort() + "/test"); 
		httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort()+ "/redirect/?target=" + url));

		Assert.assertNotNull(interceptor.getRequestString());
		Assert.assertNotNull(interceptor.getResponseString());
		
		httpClient.close();
		server.close();
	}

	
		
	
	
	@Execution(Execution.NONTHREADED)
	private static final class NonThreadedRequestHandler implements IHttpRequestHandler {
		
		private String threadnameRequest = null;
		private String threadnameResponse = null;
		
		public void onRequest(final IHttpExchange exchange) throws IOException {
			
			
			IHttpResponseHandler respHdl = new IHttpResponseHandler() {

				@Execution(Execution.NONTHREADED)
				public void onResponse(IHttpResponse response) throws IOException {
					threadnameResponse = Thread.currentThread().getName();
					
					exchange.send(response);
				}
				
				public void onException(IOException ioe) {
					
				}
			};
			
			
			threadnameRequest = Thread.currentThread().getName();
			exchange.forward(exchange.getRequest(), respHdl);
		}
		
		
		String getThreadnameRequest() {
			return threadnameRequest;
		}
		
		String getThreadnameResponse() {
			return threadnameResponse;
		}
	}
	
	
	@Execution(Execution.MULTITHREADED)
	private static final class MultiThreadedRequestHandler implements IHttpRequestHandler {
		
		private String threadnameRequest = null;
		private String threadnameResponse = null;
		
		public void onRequest(final IHttpExchange exchange) throws IOException {
			
			
			IHttpResponseHandler respHdl = new IHttpResponseHandler() {

				@Execution(Execution.MULTITHREADED)
				public void onResponse(IHttpResponse response) throws IOException {
					threadnameResponse = Thread.currentThread().getName();
					
					exchange.send(response);
				}
				
				public void onException(IOException ioe) {
					
				}
			};
			
			
			threadnameRequest = Thread.currentThread().getName();
			exchange.forward(exchange.getRequest(), respHdl);
		}
		
		
		String getThreadnameRequest() {
			return threadnameRequest;
		}
		
		String getThreadnameResponse() {
			return threadnameResponse;
		}
	}
	

	
	
	@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
	private static final class InvokeOnMessageRequestHandler implements IHttpRequestHandler {
		
		private IHttpRequest req = null;
		private IHttpResponse res = null;
		
		public void onRequest(final IHttpExchange exchange) throws IOException {
			
			
			IHttpResponseHandler respHdl = new IHttpResponseHandler() {

				@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
				public void onResponse(IHttpResponse response) throws IOException {
					res = response;
					exchange.send(response);
				}
				
				public void onException(IOException ioe) {
					
				}
			};
			
			req = exchange.getRequest();
			exchange.forward(exchange.getRequest(), respHdl);
		}
		
		
		IHttpRequest getRequest() {
			return req;
		}
		
		IHttpResponse getResponse() {
			return res;
		}
	}
	
	

	
	
	private static final class ResponseHandler implements IHttpResponseHandler {
		
		private IHttpResponse response = null;
		
		public void onResponse(IHttpResponse response) throws IOException {
			this.response = response;
		}
		
		
		public void onException(IOException ioe) {
			
		}
		
		IHttpResponse getResponse() {
			return response;
		}
	}
	
	
	
	   private static final class ResponseAuditHandler implements IHttpRequestHandler {
	        
	        private StringBuilder responseString = new StringBuilder();
	        
	        public void onRequest(final IHttpExchange exchange) throws IOException {

	            final IHttpRequest req = exchange.getRequest(); 

	            
	            // response handler definition
	            IHttpResponseHandler responseHandler = new IHttpResponseHandler() {
	                
	                public void onResponse(IHttpResponse response) throws IOException {
	                    
	                    // add header audit record
	                    responseString.append(response.getResponseHeader().toString());
	                    
	                    
	                    // does response contain a body? 
	                    if (response.hasBody()) {
	                        
	                        NonBlockingBodyDataSource bodyDataSource = response.getNonBlockingBody();
	                    
	                        BodyDataSink bodyDataSink = exchange.send(response.getResponseHeader());
	                        
	                        BodyForwarder bodyForwarder = new BodyForwarder(bodyDataSource, bodyDataSink) {
	                            
	                            private final List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
	                            
	                            public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException ,IOException {
	                                
	                                ByteBuffer[] data = bodyDataSource.readByteBufferByLength(bodyDataSource.available());
	                                for (ByteBuffer byteBuffer : data) {
	                                    ByteBuffer buf = byteBuffer.duplicate();
	                                    buffers.add(buf);
	                                }
	                                
	                                LOG.fine("forwarding " + DataConverter.toString(HttpUtils.copy(data)));
	                                bodyDataSink.write(data);
	                            };
	                            
	                            public void onComplete() {
	                                try {
	                                    String s = DataConverter.toString(buffers, req.getCharacterEncoding());
	                                    responseString.append(s);
	                                    LOG.fine("onComplete added " + s);
	                                } catch (UnsupportedEncodingException use) {
	                                    responseString.append("<error>");
	                                }
	                            };
	                        };
	                        
	                        bodyDataSource.setDataHandler(bodyForwarder);
	                                            
	                    } else {
	                        // forward response
	                        exchange.send(response);                    
	                    }
	                }
	                
	                public void onException(IOException ioe) {
	                    
	                }
	            };
	    
	            // forward request
	            exchange.forward(req, responseHandler);                 
	        }
	        
	        String getResponseString() {
	            return responseString.toString();
	        }
	   }
	   
	   
	   
	   private static final class RequestAuditHandler implements IHttpRequestHandler {
	        
	        private StringBuilder requestString = new StringBuilder();

	        
	        public void onRequest(final IHttpExchange exchange) throws IOException {

	            final IHttpRequest req = exchange.getRequest(); 

	            // add header audit record
	            requestString.append(req.getRequestHeader().toString());
	            
	            
	            // does request contain a body? 
	            if (req.hasBody()) {
	                
	                NonBlockingBodyDataSource bodyDataSource = req.getNonBlockingBody();
	            
	                BodyDataSink bodyDataSink = exchange.forward(req.getRequestHeader());
	                
	                BodyForwarder bodyForwarder = new BodyForwarder(bodyDataSource, bodyDataSink) {
	                    
	                    private final List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
	                    
	                    public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException ,IOException {
	                        
	                        ByteBuffer[] data = bodyDataSource.readByteBufferByLength(bodyDataSource.available());
	                        for (ByteBuffer byteBuffer : data) {
	                            buffers.add(byteBuffer.duplicate());
	                        }
	                        
	                        bodyDataSink.write(data);
	                    };
	                    
	                    public void onComplete() {
	                        try {
	                            requestString.append(DataConverter.toString(buffers, req.getCharacterEncoding()));
	                        } catch (UnsupportedEncodingException use) {
	                            requestString.append("<error>");
	                        }
	                    };
	                };
	                
	                req.getNonBlockingBody().setDataHandler(bodyForwarder);
	                    
	            } else {
	                // forward request
	                exchange.forward(req);                 
	            }
	        }
	        
	        String getRequestString() {
	            return requestString.toString();
	        }
	    }

	   
	private static final class AuditHandler implements IHttpRequestHandler {
		
		private StringBuilder requestString = new StringBuilder();
		private StringBuilder responseString = new StringBuilder();
		
		

		
		public void onRequest(final IHttpExchange exchange) throws IOException {

			final IHttpRequest req = exchange.getRequest(); 

			
			// response handler definition
			IHttpResponseHandler responseHandler = new IHttpResponseHandler() {
				
				public void onResponse(IHttpResponse response) throws IOException {
					
					// add header audit record
					responseString.append(response.getResponseHeader().toString());
					
					
					// does response contain a body? 
					if (response.hasBody()) {
						
						NonBlockingBodyDataSource bodyDataSource = response.getNonBlockingBody();
					
						BodyDataSink bodyDataSink = exchange.send(response.getResponseHeader());
						
						BodyForwarder bodyForwarder = new BodyForwarder(bodyDataSource, bodyDataSink) {
							
							private final List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
							
							public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException ,IOException {
								
								ByteBuffer[] data = bodyDataSource.readByteBufferByLength(bodyDataSource.available());
								for (ByteBuffer byteBuffer : data) {
								    ByteBuffer buf = byteBuffer.duplicate();
									buffers.add(buf);
								}
								
								bodyDataSink.write(data);
							};
							
							public void onComplete() {
								try {
									responseString.append(DataConverter.toString(buffers, req.getCharacterEncoding()));
								} catch (UnsupportedEncodingException use) {
									requestString.append("<error>");
								}
							};
						};
						
						bodyDataSource.setDataHandler(bodyForwarder);
											
					} else {
						// forward response
						exchange.send(response);					
					}
				}
				
				public void onException(IOException ioe) {
					
				}
			};
			
			
			
			// add header audit record
			requestString.append(req.getRequestHeader().toString());
			
			
			// does request contain a body? 
			if (req.hasBody()) {
				
				NonBlockingBodyDataSource bodyDataSource = req.getNonBlockingBody();
			
				BodyDataSink bodyDataSink = exchange.forward(req.getRequestHeader(), responseHandler);
				
				BodyForwarder bodyForwarder = new BodyForwarder(bodyDataSource, bodyDataSink) {
					
					private final List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
					
					public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException ,IOException {
						
						ByteBuffer[] data = bodyDataSource.readByteBufferByLength(bodyDataSource.available());
						for (ByteBuffer byteBuffer : data) {
							buffers.add(byteBuffer.duplicate());
						}
						
						bodyDataSink.write(data);
					};
					
					public void onComplete() {
						try {
							requestString.append(DataConverter.toString(buffers, req.getCharacterEncoding()));
						} catch (UnsupportedEncodingException use) {
							requestString.append("<error>");
						}
					};
				};
				
				req.getNonBlockingBody().setDataHandler(bodyForwarder);
					
			} else {
				// forward request
				exchange.forward(req, responseHandler);					
			}
		}
		
		String getRequestString() {
			return requestString.toString();
		}
		
		String getResponseString() {
			return responseString.toString();
		}
	}
	
	
	private static final class HeaderEnhancer implements IHttpRequestHandler {
		
		public void onRequest(final IHttpExchange exchange) throws IOException {

			IHttpRequest req = exchange.getRequest();
			req.addHeader("X-Request", "passed");
			
			// response handler definition
			IHttpResponseHandler responseHandler = new IHttpResponseHandler() {
				
				public void onResponse(IHttpResponse response) throws IOException {

					response.addHeader("X-Response", "passed");
					exchange.send(response);					
				}
				
				public void onException(IOException ioe) {
					
				}
			};
			
			exchange.forward(req, responseHandler);					
		}
	}
	
	
	
	private static final class ReplaceHandler implements IHttpRequestHandler {
		
		@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
		public void onRequest(final IHttpExchange exchange) throws IOException {
			
			IHttpResponseHandler respHdl = new IHttpResponseHandler() {
				
				@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
				public void onResponse(IHttpResponse response) throws IOException {
					HttpResponse newResponse = new HttpResponse(200, "text/plain", response.getBody().readString() + " and me");
					exchange.send(newResponse);
				}
				
				public void onException(IOException ioe) {
				}
			}; 
			
			exchange.forward(new PostRequest(exchange.getRequest().getRequestUrl().toString(), "text/plain", "you"), respHdl);
		}
	}
	
	private static final class EchoHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			IHttpRequest request = exchange.getRequest();
			
			HttpResponse response = null;
			if (request.hasBody()) {
				response = new HttpResponse(200, request.getContentType(), exchange.getRequest().getNonBlockingBody());
			} else {
				response = new HttpResponse(200);
			}
				
			for (String headerName : exchange.getRequest().getHeaderNameSet()) {
				if (headerName.startsWith("X")) {
					response.setHeader(headerName, exchange.getRequest().getHeader(headerName));
				}
			}
			
			exchange.send(response);
		}
	}
	
	
	private static final class RedirectHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			String target = exchange.getRequest().getParameter("target");
			
			IHttpResponse response = new HttpResponse(303, "text/plain", "not found");
			response.setHeader("Location", target);
			exchange.send(response);
		}
	}
}