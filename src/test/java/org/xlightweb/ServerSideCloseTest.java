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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;






/**
*
* @author grro@xlightweb.org
*/
public final class ServerSideCloseTest  {
	

	private final AtomicInteger running = new AtomicInteger(0);
	private final List<String> errors  = new ArrayList<String>();
	

	
	public static void main(String[] args) throws Exception {
        
	    for (int i = 0; i < 10000; i++) {
	        new ServerSideCloseTest().testPooledJettySimpleMessageWithRetry();
	    }
	    
    }
	
	
	@Before
	public void setup() {
		running.set(0);
		errors.clear();
	}
	

	
	@Ignore
	@Test
	public void testApacheClientPooledWebServer() throws Exception {
		
		System.out.println("testApacheClientPooledWebServer");
		
		final HttpServer server = new HttpServer(new RequestHandler());
		server.start();
		
	
		for (int i =0; i < 3; i++) {
			new Thread() {
				@Override
				public void run() {

					running.incrementAndGet();
					try {
						org.apache.commons.httpclient.HttpClient httpClient = new org.apache.commons.httpclient.HttpClient();
						httpClient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false)); 
						for (int j = 0; j< 100; j++) {
							
							GetMethod getMeth = new GetMethod("http://localhost:" + server.getLocalPort() + "/");
							httpClient.executeMethod(getMeth);
							
							Assert.assertEquals(200, getMeth.getStatusCode());
							Assert.assertEquals("OK", getMeth.getResponseBodyAsString());
							
							getMeth.releaseConnection();
						}

						
					} catch (Exception e) {
						e.printStackTrace();
						errors.add(e.toString());
						
					} finally {
						running.decrementAndGet();
					}
					
				}
			}.start();
		}

		do {
			QAUtil.sleep(200);
		} while (running.get() > 0);
		
		for (String error : errors) {
			System.out.println(error);
		}
		
		Assert.assertTrue(errors.isEmpty());
		
		server.close();
	}
	
	
	
	

	
	

	
	

	

	@Ignore
	@Test
	public void testPooledJettySimpleMessage() throws Exception {

		System.out.println("testPooledJetty");

		final WebContainer webContainer = new WebContainer(new MyServlet());
		webContainer.start();
		
	
		for (int i =0; i < 3; i++) {
			new Thread() {
				@Override
				public void run() {

					running.incrementAndGet();
					try {
						HttpClient httpClient = new HttpClient();
						httpClient.setMaxRetries(0);

						for (int j = 0; j< 1000; j++) {
							GetRequest request = new GetRequest("http://localhost:" + webContainer.getLocalPort() + "/");
							IHttpResponse response = httpClient.call(request);
							
							if (response.getStatus() != 200) {
								System.out.println("status 200 expected. Got " + response);
								errors.add("status 200 expected. Got " + response.getStatus());
							}
							
							String body = response.getBody().readString();
							if (!body.equals("OK")) {
								System.out.println("content OK expected. Got " + body);
							}
						}
						
						httpClient.close();

						
					} catch (Exception e) {
						e.printStackTrace();
						errors.add(e.toString());
						
					} finally {
						running.decrementAndGet();
					}
					
				}
			}.start();
		}

		do {
			QAUtil.sleep(200);
		} while (running.get() > 0);
		
		for (String error : errors) {
			System.out.println(error);
		}
		
		Assert.assertTrue(errors.isEmpty());
		
		webContainer.stop();
	}
	
	

	@Test
    public void testPooledJettySimpleMessageWithRetry() throws Exception {

        System.out.println("testPooledJettyWithRetry");

        final WebContainer webContainer = new WebContainer(new MyServlet());
        webContainer.start();
        
    
        System.out.println("running test...");
        for (int i =0; i < 3; i++) {
            new Thread() {
                @Override
                public void run() {

                    running.incrementAndGet();
                    try {
                        HttpClient httpClient = new HttpClient();
                        httpClient.setBodyDataReceiveTimeoutMillis(5000);

                        for (int j = 0; j< 1000; j++) {
                            GetRequest request = new GetRequest("http://localhost:" + webContainer.getLocalPort() + "/");
                            IHttpResponse response = httpClient.call(request);
                            
                            if (response.getStatus() != 200) {
                                System.out.println("status 200 expected. Got " + response);
                                errors.add("status 200 expected. Got " + response.getStatus());
                            }
                            
                            String body = response.getBody().readString();
                            if (!body.equals("OK")) {
                                System.out.println("content OK expected. Got " + body);
                            }
                        }
                        
                        httpClient.close();

                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        errors.add(e.toString());
                        
                    } finally {
                        running.decrementAndGet();
                    }
                    
                }
            }.start();
        }

        do {
            QAUtil.sleep(200);
        } while (running.get() > 0);
        
        for (String error : errors) {
            System.out.println(error);
        }
        
        Assert.assertTrue(errors.isEmpty());
        
        webContainer.stop();
    }
	
	@Ignore
	@Test
    public void testPooledJettyFullMessage() throws Exception {

        System.out.println("testPooledJettyFullMessage");

        final WebContainer webContainer = new WebContainer(new MyServlet2());
        webContainer.start();
        
    
        for (int i =0; i < 3; i++) {
            new Thread() {
                @Override
                public void run() {

                    running.incrementAndGet();
                    try {
                        HttpClient httpClient = new HttpClient();
                        httpClient.setMaxRetries(0);

                        for (int j = 0; j< 1000; j++) {
                            GetRequest request = new GetRequest("http://localhost:" + webContainer.getLocalPort() + "/");
                            IHttpResponse response = httpClient.call(request);
                            
                            if (response.getStatus() != 200) {
                                System.out.println("status 200 expected. Got " + response);
                                errors.add("status 200 expected. Got " + response.getStatus());
                            }
                            
                            String body = response.getBody().readString();
                            if (!body.equals("OK")) {
                                System.out.println("content OK expected. Got " + body);
                            }
                        }
                        
                        httpClient.close();

                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        errors.add(e.toString());
                        
                    } finally {
                        running.decrementAndGet();
                    }
                    
                }
            }.start();
        }

        do {
            QAUtil.sleep(200);
        } while (running.get() > 0);
        
        for (String error : errors) {
            System.out.println(error);
        }
        
        Assert.assertTrue(errors.isEmpty());
        
        webContainer.stop();
    }
    
	
	
	private static final class MyServlet extends HttpServlet {
		
		private static final long serialVersionUID = -2014260619889214833L;

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.setContentType("text/plain");
			resp.setHeader("Connection", "close");
			resp.getWriter().write("OK");
		}
	}
	
	
	   
    private static final class MyServlet2 extends HttpServlet {
        
        private static final long serialVersionUID = -2014260619889214833L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain");
            resp.setHeader("Connection", "close");
            resp.setHeader("Content-Length", "2");
            resp.getWriter().write("OK");
        }
    }
    
    
    
    

	
    @Ignore
	@Test
	public void testPooledWebServer() throws Exception {
		
		final HttpServer server = new HttpServer(new RequestHandler());
		server.start();
		
	
		for (int i =0; i < 3; i++) {
			new Thread() {
				@Override
				public void run() {

					running.incrementAndGet();
					try {
						HttpClient httpClient = new HttpClient();

						for (int j = 0; j< 1000; j++) {
							IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
							
							if (response.getStatus() != 200) {
								System.out.println("status 200 expected. Got " + response);
								errors.add("status 200 expected. Got " + response.getStatus());
							}
							
							String body = response.getBody().readString();
							if (!body.equals("OK")) {
								System.out.println("content OK expected. Got " + body);
							}
						}
						
						httpClient.close();

						
					} catch (Exception e) {
						e.printStackTrace();
						errors.add(e.toString());
						
					} finally {
						running.decrementAndGet();
					}
					
				}
			}.start();
		}

		do {
			QAUtil.sleep(200);
		} while (running.get() > 0);
		
		for (String error : errors) {
			System.out.println(error);
		}
		
		Assert.assertTrue(errors.isEmpty());
		
		server.close();
	}
	
	
	

	
    @Ignore
	@Test
	public void testPooledNativeServer() throws Exception {

		
		IDataHandler dh = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection connection) throws IOException {
				connection.readStringByDelimiter("\r\n\r\n");
				connection.write("HTTP/1.1 200 OK\r\n" + 
						         "Server: xLightweb/2.5-SNAPSHOT\r\n" +
						         "Content-Length: 2\r\n" +
						         "Connection: close\r\n" +
						         "Content-Type: text/plain; charset=UTF-8\r\n" +
						         "\r\n" +
						         "OK");
				connection.close();
				return true;
			}
		};
		
		final IServer server = new Server(dh);
		server.start();

		
	
		for (int i =0; i < 3; i++) {
			new Thread() {
				@Override
				public void run() {

					running.incrementAndGet();
					try {
						HttpClient httpClient = new HttpClient();

						for (int j = 0; j< 1000; j++) {
							GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
							IHttpResponse response = httpClient.call(request);
							
							if (response.getStatus() != 200) {
								System.out.println("status 200 expected. Got " + response.getStatus());
								Assert.fail("status 200 expected. Got " + response.getStatus());
							}
							
							String body = response.getBody().readString();
							if (!body.equals("OK")) {
								System.out.println("content OK expected. Got " + body);
							}
						}
						
						httpClient.close();

						
					} catch (Exception e) {
						e.printStackTrace();
						errors.add(e.toString());
						
					} finally {
						running.decrementAndGet();
					}
					
				}
			}.start();
		}

		do {
			QAUtil.sleep(200);
		} while (running.get() > 0);
		
		for (String error : errors) {
			System.out.println(error);
		}
		
		Assert.assertTrue(errors.isEmpty());
		
		server.close();
	}
	
	

	
	@Test
	public void testProtocolError() throws Exception {

	        
	    IDataHandler dh = new IDataHandler() {
	        
            public boolean onData(INonBlockingConnection connection) throws IOException {
                connection.readStringByDelimiter("\r\n\r\n");
                connection.write("HTTP/1.1 200 OK\r\n" + 
                                 "Server: xLightweb/2.5-SNAPSHOT\r\n" +
                                 "Content-Length: 5\r\n" +
                                 "Connection: close\r\n" +
                                 "Content-Type: text/plain; charset=UTF-8\r\n" +
                                 "\r\n" +
                                 "OK");
                connection.close();
                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();

        HttpClient httpClient = new HttpClient();

        GetRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
        IHttpResponse response = httpClient.call(request);
            
        if (response.getStatus() != 200) {
            System.out.println("status 200 expected. Got " + response.getStatus());
            Assert.fail("status 200 expected. Got " + response.getStatus());
        }
            
        try {
            response.getBody().readString();
            Assert.fail("ProtocolException expected");
        } catch (ProtocolException expected) { }
        
        httpClient.close();
        server.close();
    }
    	    
	    

	
	
	
	private static final class RequestHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			HttpResponse response = new HttpResponse(200, "text/plain", "OK");
			response.setHeader("Connection", "close");
			
			exchange.send(response);
		}
	}
}