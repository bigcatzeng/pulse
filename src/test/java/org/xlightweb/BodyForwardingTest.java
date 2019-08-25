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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.util.logging.Level;

import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.connection.IServer;
import org.xsocket.connection.IConnection.FlushMode;




/**
*
* @author grro@xlightweb.org
*/
public final class BodyForwardingTest {
	
	
	
	public static void main(String[] args) throws Exception {
	    
		for (int i = 0; i < 10000; i++) {
			new BodyForwardingTest().testChunked2();
		}
	}
	
	

    @Test 
    public void testSimple() throws Exception {
        
        IHttpRequestHandler rh = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                
                BodyDataSink dataSink = exchange.send(new HttpResponseHeader(200, "text/plain"));
                dataSink.write("hello ");
                
                QAUtil.sleep(300);
                dataSink.write("you");
                dataSink.close();
            }
        };
        
        
        IServer server = new HttpServer(rh);
        server.start();
        
        org.apache.commons.httpclient.HttpClient httpClient = new org.apache.commons.httpclient.HttpClient();
        GetMethod meth = new GetMethod("http://localhost:" + server.getLocalPort() + "/test");
        httpClient.executeMethod(meth);

        Assert.assertEquals("hello you", meth.getResponseBodyAsString());

        meth.releaseConnection();
        server.close();
    }
    
 


    @Test 
    public void testChain() throws Exception {
        
        IHttpRequestHandler rh = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                
                BodyDataSink dataSink = exchange.send(new HttpResponseHeader(200, "text/plain"));
                dataSink.write("hello ");
                
                QAUtil.sleep(300);
                dataSink.write("you");
                dataSink.close();
            }
        };
        
        RequestHandlerChain chain = new RequestHandlerChain();
        chain.addFirst(rh);
        
        IServer server = new HttpServer(chain);
        server.start();
        
        org.apache.commons.httpclient.HttpClient httpClient = new org.apache.commons.httpclient.HttpClient();
        GetMethod meth = new GetMethod("http://localhost:" + server.getLocalPort() + "/test");
        httpClient.executeMethod(meth);

        Assert.assertEquals("hello you", meth.getResponseBodyAsString());

        meth.releaseConnection();
        server.close();
    }
	
    

    @Test 
    public void testChainWithInterceptor() throws Exception {
        

        IHttpRequestHandler rh = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                
                BodyDataSink dataSink = exchange.send(new HttpResponseHeader(200, "text/plain"));
                dataSink.write("hello ");
                
                QAUtil.sleep(300);
                dataSink.write("you");
                dataSink.close();
            }
        };
        
        
        IHttpRequestHandler interceptor = new IHttpRequestHandler() {
            
            public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
                
                IHttpResponseHandler rh = new IHttpResponseHandler() {
                    
                    public void onResponse(IHttpResponse response) throws IOException {
                        BodyDataSink dsink = exchange.send(response.getResponseHeader());
                        dsink.write("you ");
                        
                        QAUtil.sleep(300);
                        dsink.write("hello");
                        dsink.close();
                    }
                    
                    public void onException(IOException ioe) throws IOException {
                        exchange.sendError(ioe);
                    }
                };
                
                exchange.forward(exchange.getRequest(), rh);
            }
        };
        
        RequestHandlerChain chain = new RequestHandlerChain();
        chain.addLast(interceptor);
        chain.addLast(rh);
        
        IServer server = new HttpServer(chain);
        server.start();
        
        org.apache.commons.httpclient.HttpClient httpClient = new org.apache.commons.httpclient.HttpClient();
        GetMethod meth = new GetMethod("http://localhost:" + server.getLocalPort() + "/test");
        httpClient.executeMethod(meth);

        Assert.assertEquals("you hello", meth.getResponseBodyAsString());

        meth.releaseConnection();
        server.close();
    }
    
    
    
    
    
    

	@Test 
	public void testChunked() throws Exception {
	    
		IServer service = new HttpServer(new ServiceHandler());
		service.start();
		
		IServer proxy = new HttpServer(new ForwardHandler(service.getLocalPort()));
		proxy.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", proxy.getLocalPort());

		
		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		BodyDataSink dataSink = con.send(new HttpRequestHeader("POST", "/"), respHdl);
		
		dataSink.write("test");

		IHttpResponse response = respHdl.getResponse();
		NonBlockingBodyDataSource dataSource = response.getNonBlockingBody();
		
		QAUtil.sleep(1000);
		Assert.assertEquals("test", dataSource.readStringByLength(4));
		
		Assert.assertEquals(0, dataSource.available());
		
		dataSink.close();
		QAUtil.sleep(500);
		Assert.assertEquals(-1, dataSource.available());
		
		proxy.close();
		service.close();
	}
	
	
	@Test 
	public void testChunked2() throws Exception {	    
		IServer server = new HttpServer(new ServiceHandler());
		server.start();
				
		
		HttpClient httpClient = new HttpClient();
	
		FutureResponseHandler respHdl = new FutureResponseHandler();
		BodyDataSink outBodyChannel = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort()+ "/"), respHdl);
		outBodyChannel.flush();
		
		IHttpResponse resp = respHdl.getResponse();
		BodyDataSource inBodyChannel = resp.getBody();

		InputStream is = Channels.newInputStream(inBodyChannel);
		OutputStream os = Channels.newOutputStream(outBodyChannel);

		os.write("test".getBytes());
		os.flush();
		
		byte[] data = new byte[4];
		is.read(data);
		
		Assert.assertEquals("test", new String(data));

		httpClient.close();
		server.close();
	}
	

	@Test 
	public void testProxyBound() throws Exception {
		
		IServer service = new HttpServer(new ServiceHandler());
		service.start();
		
		IServer proxy = new HttpServer(new ForwardHandler(service.getLocalPort()));
		proxy.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", proxy.getLocalPort());

		
		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		BodyDataSink dataSink = con.send(new HttpRequestHeader("POST", "/"), 4, respHdl);
		
		dataSink.write("test");
		
		IHttpResponse response = respHdl.getResponse();
		QAUtil.sleep(1000);
		
		NonBlockingBodyDataSource dataSource = response.getNonBlockingBody();
		Assert.assertEquals("test", dataSource.readStringByLength(4));
		
		Assert.assertEquals(-1, dataSource.available());
		Assert.assertFalse(dataSource.isOpen());

		
		proxy.close();
		service.close();
	}

	
	
	@Test 
	public void testProxyChunkedConnectionDelete() throws Exception {
		
		System.out.println("testChunkedConnectionDelete()");
		
		IServer service = new HttpServer(new ServiceHandler());
		service.start();
		
		IServer proxy = new HttpServer(new ForwardHandler(service.getLocalPort()));
		proxy.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", proxy.getLocalPort());

		
		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		BodyDataSink dataSink = con.send(new HttpRequestHeader("POST", "/"), respHdl);
		
		dataSink.write("test");

		IHttpResponse response = respHdl.getResponse();
		NonBlockingBodyDataSource dataSource = response.getNonBlockingBody();
		
		QAUtil.sleep(1000);
		
		System.out.println("reading 4 bytes");
		String res = dataSource.readStringByLength(4);
		if (!res.equals("test")) {
			String msg = "got " + res + " instead of test";
			System.out.println(msg);	
			Assert.fail(msg);
		}
		
		System.out.println("check availablity");
		int available = dataSource.available();
		if (available != 0) {
			String msg = "available is " + available+ " expected is 0";
			System.out.println(msg);	
			Assert.fail(msg);
		}
		
		
		System.out.println("closing connection");
		con.close();
		QAUtil.sleep(1000);

		System.out.println("try to write");
		try {
			dataSink.write("should not work");
			Assert.fail("ClosedChannelException expected");
		} catch (ClosedChannelException expected) { }
		
		
		proxy.close();
		service.close();
	}
	
	
	@Test 
	public void testProxyBoundConnectionDelete() throws Exception {
	
		IServer service = new HttpServer(new ServiceHandler());
		service.start();
		
		IServer proxy = new HttpServer(new ForwardHandler(service.getLocalPort()));
		proxy.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", proxy.getLocalPort());

		
		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		BodyDataSink dataSink = con.send(new HttpRequestHeader("POST", "/"), 4, respHdl);
		
		
		dataSink.write("test");
			
		IHttpResponse response = respHdl.getResponse();
		QAUtil.sleep(1000);
		
		NonBlockingBodyDataSource dataSource = response.getNonBlockingBody();
		Assert.assertEquals("test", dataSource.readStringByLength(4));
		
		Assert.assertEquals(-1, dataSource.available());
		Assert.assertFalse(dataSource.isOpen());
		
		proxy.close();
		service.close();
	}
	
	

	
	
	
	
	
	private static final class ForwardHandler implements IHttpRequestHandler {
		
		private int port = 0;
		
		public ForwardHandler(int port) {
			this.port = port;
		}

		public void onRequest(final IHttpExchange exchange) throws IOException {

			
			
			IHttpResponseHandler respHdl = new IHttpResponseHandler() {
				
				@Execution(Execution.NONTHREADED)
				public void onResponse(IHttpResponse response) throws IOException {
					
					IHttpResponseHeader resHdr = response.getResponseHeader();					
					final BodyDataSink dataSink = exchange.send(resHdr);
					dataSink.setFlushmode(FlushMode.ASYNC);
					
					NonBlockingBodyDataSource bodyDataSource = response.getNonBlockingBody();
					BodyForwarder bodyForwarder = new BodyForwarder(bodyDataSource, dataSink) {
						
						@Execution(Execution.NONTHREADED)
						public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException ,IOException {
							ByteBuffer[] data = bodyDataSource.readByteBufferByLength(bodyDataSource.available());
							dataSink.write(data);
						}
					};
				
					bodyDataSource.setDataHandler(bodyForwarder);					
				}

				public void onException(IOException ioe) {
				}
			};

			
			
			
			IHttpRequestHeader reqHdr = exchange.getRequest().getRequestHeader();
			
			URL url = reqHdr.getRequestUrl();
			URL newUrl = new URL(url.getProtocol(), "localhost", port, url.getFile());
			reqHdr.setRequestUrl(newUrl);
			
			HttpClientConnection con = new HttpClientConnection("localhost", port);

			final BodyDataSink dataSink = con.send(reqHdr, respHdl);
			dataSink.setFlushmode(FlushMode.ASYNC);

			NonBlockingBodyDataSource bodyDataSource = exchange.getRequest().getNonBlockingBody();
			BodyForwarder bodyForwarder = new BodyForwarder(bodyDataSource, dataSink) {
				
				@Execution(Execution.MULTITHREADED)
				public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException ,IOException {
					ByteBuffer[] data = bodyDataSource.readByteBufferByLength(bodyDataSource.available());
					dataSink.write(data);
				}
			};
			exchange.getRequest().getNonBlockingBody().setDataHandler(bodyForwarder);
			
		}
	}
	

	
	

	
	
	private static final class ServiceHandler implements IHttpRequestHandler {

		public void onRequest(IHttpExchange exchange) throws IOException {
			
	
			final BodyDataSink dataSink = exchange.send(new HttpResponseHeader(200));
			dataSink.setFlushmode(FlushMode.ASYNC);
			dataSink.flush();
			
			NonBlockingBodyDataSource bodyDataSource = exchange.getRequest().getNonBlockingBody();
			BodyForwarder bodyForwarder = new BodyForwarder(bodyDataSource, dataSink) {
				
				@Execution(Execution.NONTHREADED)
				public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException ,IOException {
					ByteBuffer[] data = bodyDataSource.readByteBufferByLength(bodyDataSource.available());
					dataSink.write(data);
				}
			};
			
			exchange.getRequest().getNonBlockingBody().setDataHandler(bodyForwarder);			
		}
	}
}
