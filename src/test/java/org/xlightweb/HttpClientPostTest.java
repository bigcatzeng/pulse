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




import java.io.File;



import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.client.IHttpClientEndpoint;
import org.xlightweb.client.HttpClient.FollowsRedirectMode;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IServer;
import org.xsocket.connection.IConnection.FlushMode;




/**  
*
* @author grro@xlightweb.org
*/
public final class HttpClientPostTest  {

    private static final Logger LOG = Logger.getLogger(HttpClientPostTest.class.getName());
    

	private AtomicInteger running = new AtomicInteger(0);
	private AtomicInteger openResponses = new AtomicInteger(0);

	private List<String> errors = new ArrayList<String>(); 
	
		
	
	public static void main(String[] args) throws Exception {
	    for (int i = 0; i < 100000; i++) {
	        new HttpClientPostTest().testFlushedChunkedBodyData();
        }
	}
	
	
	
	@Before
	public void setup() {
		running.set(0);
		openResponses.set(0);
		errors.clear();
	}
	
	
	@Test
	public void testLive() throws Exception {
		System.out.println("testLive");
		
		HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
		httpClient.setTreat302RedirectAs303(true);

		IHttpResponse response = httpClient.call(new PostRequest("http://www.web.de/index.html"));
		Assert.assertEquals(200, response.getStatus());
	}


	@Test
	public void testBulk() throws Exception {
		
		System.out.println("testBulk");
			
		IServer server = new HttpServer(new HeaderInfoServerHandler());
		server.start();
		
		HttpClient httpClient = new HttpClient();
		

		for (int i = 0; i < 1000; i++) {
			PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/", "text/plain", "test");
			
			IHttpResponse response = httpClient.call(request);
			Assert.assertEquals(200, response.getStatus());
		}
		
		if (httpClient.getNumCreated() > 50) {
			System.out.println("num created " + httpClient.getNumCreated() + " is larger than 50");
			Assert.fail();
		}

		httpClient.close();
		server.close();
	}

	

/*	@Test
	public void testLiveHttps() throws Exception {
		HttpClient httpClient = new HttpClient(SSLContext.getDefault()); 	// SSLContext.getDefault() -> Java 1.6!
		httpClient.setTreat302RedirectAs303(true);


		HttpResponse response = httpClient.callFollowRedirects(new PostRequest("https://www.web.de/index.html"));
		
		httpClient.close();
		
		Assert.assertEquals(200, response.getStatus());
		
	}*/



	@Test
	public void testForm() throws Exception {
		
		System.out.println("testForm");
		
		IServer server = new HttpServer(new EchoHandler());
		server.start();
		
		HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);

		PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/login", new NameValuePair("username", "berta.breit"), new NameValuePair("password", "I dont tell you"));
		IHttpResponse response = httpClient.call(request);

		BodyDataSource bodyDataSource = response.getBody();

		MultivalueMap fue = new MultivalueMap(bodyDataSource);
		Assert.assertEquals("berta.breit", fue.getParameter("username"));
		Assert.assertEquals("I dont tell you", fue.getParameter("password"));
	
		httpClient.close();
		server.close();
	}
	
	
	
	@Test
    public void testModifiedFormParams() throws Exception {

        IServer server = new HttpServer(new EchoHandler());
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);


        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/login", new NameValuePair("username", "berta.breit"), new NameValuePair("password" ,"I dont tell you"));
        request.setParameter("addedParam", "11");
        request.removeParameter("username");
        
        IHttpResponse response = httpClient.call(request);

        BodyDataSource bodyDataSource = response.getBody();

        MultivalueMap fue = new MultivalueMap(bodyDataSource);
        Assert.assertNull(fue.getParameter("username"));
        Assert.assertEquals("I dont tell you", fue.getParameter("password"));
        Assert.assertEquals("11", fue.getParameter("addedParam"));
    
        httpClient.close();
        server.close();
    }

	
	

    @Test
    public void testEmptyForm() throws Exception {
        
        System.out.println("testForm");
        
        IServer server = new HttpServer(new EchoHandler());
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);

        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/login");
        IHttpResponse response = httpClient.call(request);

        Assert.assertEquals(200, response.getStatus());

        httpClient.close();
        server.close();
    }


	@Test
	public void testFileUpload() throws Exception {
	    
	    System.out.println("testFileUpload");
		
		IServer server = new HttpServer(new EchoHandler());
		server.start();
		
		HttpClient httpClient = new HttpClient();
		
		File file = QAUtil.createTempfile(".txt");
		FileOutputStream fos = new FileOutputStream(file);
		fos.write("test 123467678".getBytes());
		fos.close();

		PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/", file);
		IHttpResponse response = httpClient.call(request);

		Assert.assertEquals("text/plain", request.getContentType());
		BodyDataSource bodyDataSource = response.getBody();
		String body = bodyDataSource.readString();

		Assert.assertEquals("test 123467678", body);
	
		file.delete();
		httpClient.close();
		server.close();
	}

	
	
    @Test
    public void testFileUploadWithInterceptor() throws Exception {
        
        System.out.println("testFileUploadWithInterceptor");
        
        IServer server = new HttpServer(new EchoHandler());
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        IHttpRequestHandler interceptor = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                IHttpRequest request = exchange.getRequest();
                
                byte[] data = request.getBody().readBytes();
                exchange.forward(new HttpRequest(request.getRequestHeader(), data));
            }
        };
        httpClient.addInterceptor(interceptor);

        File file = QAUtil.createTestfile_400k();

        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/", file);
        IHttpResponse response = httpClient.call(request);

        Assert.assertEquals("text/html", request.getContentType());
        Assert.assertTrue(QAUtil.isEquals(file, response.getBody().readBytes()));
    
        file.delete();
        httpClient.close();
        server.close();
    }	
	
	
	

	/*	@Test
		public void testLiveForm() throws Exception {
			HttpClient httpClient = new HttpClient(SSLContext.getDefault()); // SSLContext.getDefault() -> Java 1.6!

			httpClient.setMaxIdlePooled(3);
			httpClient.setTreat302RedirectAs303(true);
			ConnectionUtils.registerMBean(httpClient);

			String[] formParams = new String[] {  "username=berta.breit"
					                            , "password=I dont tell you"
					                            , "service=freemail"
					                            , "server=https://freemail.web.de"
					                            , "onerror=https://freemail.web.de/msg/temporaer.htm"
					                            , "onfail=https://freemail.web.de/msg/logonfailed.htm"};


			PostRequest request = new PostRequest("https://login.web.de/intern/login/", formParams);
			HttpResponse response = httpClient.callFollowRedirects(request);

			BodyDataSource bodyDataSource = response.getBody();
			String body = bodyDataSource.readString();

			Assert.assertEquals(200, response.getStatus());
			Assert.assertTrue(body.indexOf("logonfailed") != -1);
		}*/


	@Test
	public void testLiveRequest() throws Exception {
		System.out.println("testLiveRequest");
		
		HttpClient httpClient = new HttpClient();
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
		httpClient.setTreat302RedirectAs303(true);

		PostRequest request = new PostRequest("http://www.web.de/index.html");
		IHttpResponse response = httpClient.call(request);
		Assert.assertEquals(200, response.getStatus());
	}


	
	@Test
	public void testSimple() throws Exception {
		
		System.out.println("testSimple");
	
		IServer server = new HttpServer(new EchoHandler());
		server.start();
		
		HttpClient httpClient = new HttpClient();
		
		
		PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/", "application/octet-stream", new byte[] { 65, 87, 78});
		IHttpResponse response = httpClient.call(request);
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("application/octet-stream", response.getContentType());
		Assert.assertArrayEquals(new byte[] { 65, 87, 78}, response.getBody().readBytes());
	}




	@Test
	public void testStringBodyData() throws Exception {
		System.out.println("testStringBodyData");
		
		IHttpClientEndpoint httpClient = new HttpClient();

		IServer server = new HttpServer(new EchoHandler());
		server.start();



		IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort() + "/", "text/plain", "Herzlichen Gl\u00FCckwunsch, Sie haben sich zur Reinigung des Aufzugs entschlossen."));
		String body = response.getBody().readString();

		server.close();
		httpClient.close();

		Assert.assertEquals("Herzlichen Gl\u00FCckwunsch, Sie haben sich zur Reinigung des Aufzugs entschlossen.", body);
	}



	@Test
	public void testPlainTransferEncoding() throws Exception {
		System.out.println("testPlainTransferEncoding");
		
		IHttpClientEndpoint httpClient = new HttpClient();

		IServer server = new HttpServer(new EchoHandler());
		server.start();



		IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort() + "/", "text/plain", "Herzlichen Gl\u00FCckwunsch, Sie haben sich zur Reinigung des Aufzugs entschlossen."));
		String body = response.getBody().readString();

		server.close();
		httpClient.close();

		Assert.assertEquals("Herzlichen Gl\u00FCckwunsch, Sie haben sich zur Reinigung des Aufzugs entschlossen.", body);
	}


	@Test
	public void testChunkedTransferEncoding() throws Exception {
		System.out.println("testChunkedTransferEncoding");
		
		IHttpClientEndpoint httpClient = new HttpClient();

		IServer server = new HttpServer(new EchoHandler());
		server.start();



		PostRequest postRequest = new PostRequest("http://localhost:" + server.getLocalPort() + "/", "text/plain", "Herzlichen Gl\u00FCckwunsch, Sie haben sich zur Reinigung des Aufzugs entschlossen.");
		postRequest.setTransferEncoding("chunked");

		IHttpResponse response = httpClient.call(postRequest);
		String body = response.getBody().readString();

		server.close();
		httpClient.close();
		
		Assert.assertEquals("Herzlichen Gl\u00FCckwunsch, Sie haben sich zur Reinigung des Aufzugs entschlossen.", body);
	}



	@Test
	public void testPlainBodyData() throws Exception {
		System.out.println("testPlainBodyData");
		IHttpClientEndpoint httpClient = new HttpClient();
		
		IServer server = new HttpServer(new EchoHandler());
		ConnectionUtils.start(server);



		File file = QAUtil.createTestfile_40k();
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		FileChannel fc = raf.getChannel();
		
		System.out.println("call");
		FutureResponseHandler respHdl = new FutureResponseHandler();
		BodyDataSink bodyDataSink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/", "text/plain"), (int) fc.size(), respHdl);
		bodyDataSink.transferFrom(fc);
		bodyDataSink.close();
		fc.close();
		raf.close();
		
		IHttpResponse response = respHdl.getResponse();
		
		BodyDataSource bodyChannel = response.getBody();
		String body = bodyChannel.readString();

		System.out.println("closing erver & httpClient");
		server.close();
		httpClient.close();

		if (body.indexOf("Architecture of a Highly Scalable NIO-Based Server") == -1) {
			System.out.println("error got:\r\n" + body);
			Assert.fail();
		}
		
		file.delete();
	}




	@Test
	public void testPlainBodyData2() throws Exception {
		System.out.println("testPlainBodyData2");

		IServer server = new HttpServer(new EchoHandler());
		server.start();

		IHttpClientEndpoint httpClient = new HttpClient();

		FutureResponseHandler hdl = new FutureResponseHandler();

		byte[] data = "hello".getBytes("UTF-8");

		HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" +  server.getLocalPort() + "/");
		header.setContentType("text/plain; charset=UTF-8");

		BodyDataSink bodyDataSink = httpClient.send(header, data.length, hdl);
		bodyDataSink.write(data);
		bodyDataSink.close();

		IHttpResponse response = hdl.getResponse();
		String body = response.getBody().readString();


		server.close();
		httpClient.close();


		Assert.assertEquals("hello", body);
	}


	@Test
	public void testBulkPlainBodyData() throws Exception {
		System.out.println("testBulkPlainBodyData");
		
		IHttpClientEndpoint httpClient = new HttpClient();

		IServer server = new HttpServer(new EchoHandler());
		ConnectionUtils.start(server);


		IHttpResponseHandler hdl = new IHttpResponseHandler() {
			public void onResponse(IHttpResponse response) throws IOException {
			}
			
			public void onException(IOException ioe) {
			}
		};
		
		BodyDataSink bodyDataSink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/"), hdl);
		bodyDataSink.write(QAUtil.generateByteArray(10000));
		bodyDataSink.flush();
		bodyDataSink.write(QAUtil.generateByteArray(10000));

		server.close();
		httpClient.close();
	}




	@Test
	public void testFlushedPlainBodyData() throws Exception {
		System.out.println("running testFlushedPlainBodyData");
		
		IServer server = new HttpServer(new EchoHandler());
		server.start();

		IHttpClientEndpoint httpClient = new HttpClient();

		FutureResponseHandler hdl = new FutureResponseHandler();
		HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/");
		header.setContentType("text/plain; charset=UTF-8");


		int chunkSize = 100;
		int loops = 10;
		
		LOG.fine("send header");
		BodyDataSink bodyDataSink = httpClient.send(header, chunkSize * loops, hdl);

		for (int i = 0; i < loops; i++) {
		    LOG.fine("write body data");
			bodyDataSink.write(QAUtil.generateByteBuffer(chunkSize));
		}
		
		LOG.fine("close body");
		bodyDataSink.close();
		
		LOG.fine("retrieve response handle");
		IHttpResponse response = hdl.getResponse();
		
		LOG.fine("read complete body as string");
		byte[] result = response.getBody().readBytes();
		LOG.fine("got it");
		
		Assert.assertEquals(chunkSize * loops, result.length);
		
		server.close();
		httpClient.close();	
	}

	
	@Test
    public void testFlushedPlainBodyDataJetty() throws Exception {
        System.out.println("running testFlushedPlainBodyData");
        
        WebContainer server = new WebContainer(new EchoServlet());
        server.start();

        IHttpClientEndpoint httpClient = new HttpClient();

        FutureResponseHandler hdl = new FutureResponseHandler();
        HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/");
        header.setContentType("text/plain; charset=UTF-8");


        int chunkSize = 100;
        int loops = 10;
        
        BodyDataSink bodyDataSink = httpClient.send(header, chunkSize * loops, hdl);

        for (int i = 0; i < loops; i++) {
            bodyDataSink.write(QAUtil.generateByteBuffer(chunkSize));
        }
        
        bodyDataSink.close();
        
        IHttpResponse response = hdl.getResponse();
        
        byte[] result = response.getBody().readBytes();
        Assert.assertEquals(chunkSize * loops, result.length);
        
        server.stop();
        httpClient.close(); 
    }
	
	@Test
    public void testFlushedPlainBodyDataHttpConnectionBulk() throws Exception {
        for (int i = 0; i < 10; i++) {
            testFlushedPlainBodyDataHttpConnection();
        }
	    
	}
	
	@Test
    public void testFlushedPlainBodyDataHttpConnection() throws Exception {
        System.out.println("running testFlushedPlainBodyDataHttpConnection");
        
        
        IServer server = new HttpServer(new EchoHandler());
        server.start();

        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());

        FutureResponseHandler hdl = new FutureResponseHandler();
        HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/");
        header.setContentType("text/plain; charset=UTF-8");


        int chunkSize = 100;
        int loops = 10;
        
        BodyDataSink bodyDataSink = con.send(header, chunkSize * loops, hdl);

        for (int i = 0; i < loops; i++) {
            bodyDataSink.write(QAUtil.generateByteBuffer(chunkSize));
        }
        
        bodyDataSink.close();
        
        IHttpResponse response = hdl.getResponse();
        
        byte[] result = response.getBody().readBytes();
        Assert.assertEquals(chunkSize * loops, result.length);
        
        server.close();
        con.close(); 
    }
	
	
	
	@Test
    public void testFlushedPlainBodyDataHttpConnection2() throws Exception {
        System.out.println("running testFlushedPlainBodyDataHttpConnection");
        
        WebContainer server = new WebContainer(new EchoServlet());
        server.start();

        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());

        FutureResponseHandler hdl = new FutureResponseHandler();
        HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/");
        header.setContentType("text/plain; charset=UTF-8");


        int chunkSize = 100;
        int loops = 10;
        
        BodyDataSink bodyDataSink = con.send(header, chunkSize * loops, hdl);

        for (int i = 0; i < loops; i++) {
            bodyDataSink.write(QAUtil.generateByteBuffer(chunkSize));
        }
        
        bodyDataSink.close();
        
        IHttpResponse response = hdl.getResponse();
        
        byte[] result = response.getBody().readBytes();
        Assert.assertEquals(chunkSize * loops, result.length);
        
        server.stop();
        con.close(); 
    }
	
	
	@Test
	public void testFlushedPlainBodyDataBulk() throws Exception {
		for (int i = 0; i < 10; i++) {
		    System.out.println("testFlushedPlainBodyDataBulk " + i + " loop");
			testFlushedPlainBodyData();
		}
	}
	
	

	


	@Test
	public void testFlushedChunkedBodyData() throws Exception {
		System.out.println("testFlushedChunkedBodyData");
		
		final IServer server = new HttpServer(new EchoHandler());
		server.start();


		IHttpClientEndpoint httpClient = new HttpClient();

		FutureResponseHandler hdl = new FutureResponseHandler();
		HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/");
		header.setContentType("text/plain; charset=UTF-8");

		BodyDataSink bodyDataSink = httpClient.send(header, hdl);
		Assert.assertTrue("flushmode is not sync", bodyDataSink.getFlushmode() == FlushMode.SYNC);

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < 10; i++) {
			byte[] data = QAUtil.generateByteArray(4500 + i);
			sb.append(new String(data));

	        LOG.fine("write body data");
			bodyDataSink.write(data);
			bodyDataSink.flush();

			QAUtil.sleep(50);
		} 

		bodyDataSink.close();
		
		IHttpResponse response = hdl.getResponse();
		if (!response.hasBody()) {
			System.out.println("response should have a body");
			Assert.fail("response should have a body");
		}
		
		String body = response.getBody().readString();

		if (!sb.toString().equals(body)) {
			System.out.println("got wrong body");
			Assert.fail("got wrong body");
		}
	}

	
	
	@Test
    public void testSetParams() throws Exception {
		
		System.out.println("testSetParams");
    	
    	final IServer server = new HttpServer(new RequestParamsRequestHandler());
    	server.start();

    	
    	HttpClient httpClient = new HttpClient();
    	
    	IHttpRequest req = new PostRequest("http://localhost:" + server.getLocalPort() + "/test", "text/plain");
    	req.setParameter("param1", "value1");
    	req.setParameter("param2", "value2");
    	
    	
    	IHttpResponse resp = httpClient.call(req);
    	String body = resp.getBody().toString();
    	
    	Assert.assertTrue(body.indexOf("param2=value2") != -1);
    	Assert.assertTrue(body.indexOf("param1=value1") != -1);
    	
    	httpClient.close();
    	server.close();
    }

	
	   
    @Test
    public void testSetParams2() throws Exception {
    	System.out.println("testSetParams2");
        
        final IServer server = new HttpServer(new RequestParamsRequestHandler());
        server.start();

        
        HttpClient httpClient = new HttpClient();
        
        IHttpRequest req = new PostRequest("http://localhost:" + server.getLocalPort() + "/test", new NameValuePair("param1", "H\u00F6rt"), new NameValuePair("param2", "value2"));
        
        IHttpResponse resp = httpClient.call(req);
        String body = resp.getBody().toString();
        
        Assert.assertTrue(body.indexOf("param2=value2") != -1);
        Assert.assertTrue(body.indexOf("param1=H\u00F6rt") != -1);
        
        httpClient.close();
        server.close();
    }

    
    
    @Test
    public void testSetParams3() throws Exception {
    	
    	System.out.println("testSetParams3");
        
        IServer server = new HttpServer(new RequestParamsRequestHandler());
        server.start();

        
        HttpClient httpClient = new HttpClient();
        
        IHttpRequest req = new PostRequest("http://localhost:" + server.getLocalPort() + "/test?param3=value3&param4=value4", "text/plain");
        req.setParameter("param1", "value1");
        req.setParameter("param2", "value2");
        
        
        IHttpResponse resp = httpClient.call(req);
        String body = resp.getBody().toString();
        
        Assert.assertTrue(body.indexOf("param4=value4") != -1);
        Assert.assertTrue(body.indexOf("param3=value3") != -1);
        Assert.assertTrue(body.indexOf("param2=value2") != -1);
        Assert.assertTrue(body.indexOf("param1=value1") != -1);
        
        httpClient.close();
        server.close();
    }

    @Test
	public void testFlushedChunkedBodyDataBulk() throws Exception {
		
		System.out.println("testFlushedChunkedBodyDataBulk");
		
		final IServer server = new HttpServer(new EchoHandler());
		server.start();

		
		
		for (int j = 0; j < 5; j++) {
			
			Thread t = new Thread() {
				
				@Override
				public void run() {
					
					try {
						running.incrementAndGet();
					
						IHttpClientEndpoint httpClient = new HttpClient();

						for (int i = 0; i < 50; i++) {
							FutureResponseHandler hdl = new FutureResponseHandler();
							HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/");
							header.setContentType("text/plain; charset=UTF-8");
							
							BodyDataSink bodyDataSink = httpClient.send(header, hdl);
							if (bodyDataSink.getFlushmode() != FlushMode.SYNC) {
								System.out.println("flushmode should be sync");
								Assert.fail();
							}
							
							for (int k = 0; k < 10; k++) {
								try {
									byte[] data = QAUtil.generateByteArray(128);
									bodyDataSink.write(data);
									bodyDataSink.flush();
								} catch (Exception e) {
									System.out.println("error occured by writing chunk " + e.toString());
									Assert.fail();
								}
							} 
					
							bodyDataSink.close();
							
							IHttpResponse response = hdl.getResponse();
							if (!response.hasBody()) {
								System.out.println("response should have a body");
								Assert.fail();
							}
							
							response.getBody().readString();
						}

						httpClient.close();
						
						
					} catch (Exception e) {
						e.printStackTrace();
						errors.add(e.toString());
						
					} finally {
					    running.decrementAndGet();
					}
				}
			};
			t.start();
		}
		
		
		do {
			QAUtil.sleep(100);
		} while (running.get() > 0);

		server.close();
		
		for (String error : errors) {
			System.out.println("ERROR: " + error);
		}
		
		Assert.assertTrue(errors.isEmpty());
	}


    @Test
    public void testFlushedChunkedBodyDataBulk2() throws Exception {
        System.out.println("testFlushedChunkedBodyDataBulk2");
        
        System.out.println("start server");
        final IServer server = new HttpServer(new ClosingEchoHandler());
        server.start();

        System.out.println("create client");
        IHttpClientEndpoint httpClient = new HttpClient();
        
        System.out.println("run tests");
        for (int i = 0; i < 500; i++) {
            System.out.print(".");
            FutureResponseHandler hdl = new FutureResponseHandler();
            HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/");
            header.setContentType("text/plain; charset=UTF-8");
            
            BodyDataSink bodyDataSink = httpClient.send(header, hdl);
            if (bodyDataSink.getFlushmode() != FlushMode.SYNC) {
                System.out.println("flushmode should be sync");
                Assert.fail();
            }
            
            for (int k = 0; k < 10; k++) {
                try {
                    byte[] data = QAUtil.generateByteArray(128);
                    bodyDataSink.write(data);
                    bodyDataSink.flush();
                } catch (Exception e) {
                    System.out.println("error occured by writing chunk " + e.toString());
                    Assert.fail();
                }
            } 

            bodyDataSink.write("round " + i);
            bodyDataSink.close();
            
            IHttpResponse response = hdl.getResponse();
            if (!response.hasBody()) {
                System.out.println("response should have a body");
                Assert.fail();
            }
            
            response.getBody().readString();
        }

        httpClient.close();
    }        
    
	

	@Test
	public void testChunkedBodyData() throws Exception {
		System.out.println("testChunkedBodyData");
		
		final IServer server = new HttpServer(new EchoHandler());
		server.start();


		HttpClient httpClient = new HttpClient();

		FutureResponseHandler hdl = new FutureResponseHandler();
		BodyDataSink bodyDataSink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/"), hdl);
		bodyDataSink.setFlushmode(FlushMode.ASYNC);
		bodyDataSink.write("hello");
		bodyDataSink.close();

		IHttpResponse response = hdl.getResponse();
		String body = response.getBody().readString();

		server.close();
		httpClient.close();
		
		Assert.assertEquals("hello", body);
	}







	private static final class EchoHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {

			IHttpRequest request = exchange.getRequest();
						
			HttpResponse response = new HttpResponse(request.getContentType(), request.getNonBlockingBody());
			exchange.send(response);
		}
		
	}
	
	

    private static final class ClosingEchoHandler implements IHttpRequestHandler {
        
        public void onRequest(IHttpExchange exchange) throws IOException {

            IHttpRequest request = exchange.getRequest();
                        
            HttpResponse response = new HttpResponse(request.getContentType(), request.getNonBlockingBody());
            response.setHeader("Connection", "close");
            exchange.send(response);
        }
        
    }
    
	
	private static final class EchoServlet extends HttpServlet {
	    
        private static final long serialVersionUID = 6979873802567595918L;

        @Override
	    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	        
	        InputStream is = req.getInputStream();
	        OutputStream os = resp.getOutputStream();
	        
	        byte[] data = new byte[4096];
	        int read = 0;
	        do {
	            read = is.read(data);
	            if (read > 0) {
	                os.write(data, 0, read);
	                os.flush();
	            }
	        } while(read != -1);
	        
	        is.close();
	        os.close();
	    }
	}
	


	public final class RequestParamsRequestHandler implements IHttpRequestHandler {

	    @Execution(Execution.NONTHREADED)
	    public void onRequest(IHttpExchange exchange) throws IOException {

	        IHttpRequest request = exchange.getRequest();
	        StringBuilder sb = new StringBuilder();
	        
	        for (String paramName : request.getParameterNameSet()) {
	           sb.append(paramName + "=" + request.getParameter(paramName) + "\r\n"); 
	        }
	        
	                
	        exchange.send(new HttpResponse(200, "text/plain", sb.toString()));
	    }
	}
}