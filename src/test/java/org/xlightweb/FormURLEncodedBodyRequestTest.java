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
import java.io.IOException;


import org.junit.Test;
import org.junit.Assert;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;





/**
*
* @author grro@xlightweb.org
*/
public final class FormURLEncodedBodyRequestTest  {


	@Test
	public void testNoBoundary() throws Exception {

		IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();

        String url = "http://localhost:" + server.getLocalPort() + "/MyResource";
        
        
        
        HttpClient httpClient = new HttpClient();
  
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        
        HttpRequestHeader header = new HttpRequestHeader("POST", url, "multipart/mixed");
        BodyDataSink dataSink = httpClient.send(header, respHdl);

        QAUtil.sleep(500);
        
        BodyDataSink partDataSink = dataSink.writePart(new HttpMessageHeader("application/x-www-form-urlencoded"));
        MultivalueMap body = new MultivalueMap(new NameValuePair("test", "testvalue"), new NameValuePair("test2", "testvalue2"));
        partDataSink.write(body.toString());
        partDataSink.close();

        dataSink.close();


        IHttpResponse response = respHdl.getResponse();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertNotNull(HttpUtils.parseMediaTypeParameter(response.getContentType(), "boundary", true, null));
        
        
        httpClient.close();
        server.close();
    }	


	
	
	@Test
	public void testNoContentType() throws Exception {

		IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();

        String url = "http://localhost:" + server.getLocalPort() + "/MyResource";
        
        
        
        HttpClient httpClient = new HttpClient();
  
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        
        HttpRequestHeader header = new HttpRequestHeader("POST", url);
        BodyDataSink dataSink = httpClient.send(header, respHdl);

        QAUtil.sleep(500);
        
        BodyDataSink partDataSink = dataSink.writePart(new HttpMessageHeader("application/x-www-form-urlencoded"));
        MultivalueMap body = new MultivalueMap(new NameValuePair("test", "testvalue"), new NameValuePair("test2", "testvalue2"));
        partDataSink.write(body.toString());
        partDataSink.close();

        dataSink.close();


        IHttpResponse response = respHdl.getResponse();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(200, response.getStatus());
        Assert.assertNotNull(HttpUtils.parseMediaType(response.getContentType()).equals("multipart/mixed"));
        Assert.assertNotNull(HttpUtils.parseMediaTypeParameter(response.getContentType(), "boundary", true, null));
        
        
        httpClient.close();
        server.close();
    }	
	

	
	@Test
	public void testSimple() throws Exception {

		IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();

        String url = "http://localhost:" + server.getLocalPort() + "/MyResource";
        
        
        
        HttpClient httpClient = new HttpClient();
  
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        
        HttpRequestHeader header = new HttpRequestHeader("POST", url, "multipart/mixed; boundary=3453655444");
        BodyDataSink dataSink = httpClient.send(header, respHdl);

        QAUtil.sleep(500);
        
        BodyDataSink partDataSink = dataSink.writePart(new HttpMessageHeader("application/x-www-form-urlencoded"));
        MultivalueMap body = new MultivalueMap(new NameValuePair("test", "testvalue"), new NameValuePair("test2", "testvalue2"));
        partDataSink.write(body.toString());
        partDataSink.close();

        dataSink.close();


        IHttpResponse response = respHdl.getResponse();
        Assert.assertEquals("status is " + response.getStatus(), 200, response.getStatus());
        
        
        httpClient.close();
        server.close();
    }	


	@Test
	public void testSimple2() throws Exception {

		IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();

        String url = "http://localhost:" + server.getLocalPort() + "/MyResource";
        
        
        
        HttpClient httpClient = new HttpClient();
  
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        
        HttpRequestHeader header = new HttpRequestHeader("POST", url, "multipart/mixed; boundary=67676");
        BodyDataSink dataSink = httpClient.send(header, respHdl);

        QAUtil.sleep(500);
        
        
        BodyDataSink partDataSink = dataSink.writePart(new HttpMessageHeader("application/x-www-form-urlencoded"));
        MultivalueMap body = new MultivalueMap(new NameValuePair("test", "testvalue"), new NameValuePair("test2", "testvalue2"));
        partDataSink.write(body.toString());
        partDataSink.close();

        
        BodyDataSink partDataSink2 = dataSink.writePart(new HttpMessageHeader("tex/plain"));
        dataSink.write("Hello");
        partDataSink2.close();
        
        
        dataSink.close();


        IHttpResponse response = respHdl.getResponse();
        Assert.assertEquals(200, response.getStatus());
        
   
        
        httpClient.close();
        server.close();
    }	
	
	

	@Test
	public void testCompletePart() throws Exception {

		IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();

        String url = "http://localhost:" + server.getLocalPort() + "/MyResource";
        
        
        
        HttpClient httpClient = new HttpClient();
  
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        
        HttpRequestHeader header = new HttpRequestHeader("POST", url, "multipart/mixed; boundary=7766");
        BodyDataSink dataSink = httpClient.send(header, respHdl);

        QAUtil.sleep(500);
        
        
        BodyDataSink partDataSink = dataSink.writePart(new HttpMessageHeader("application/x-www-form-urlencoded"));
        MultivalueMap body = new MultivalueMap(new NameValuePair("test", "testvalue"), new NameValuePair("test2", "testvalue2"));
        partDataSink.write(body.toString());
        partDataSink.close();

        dataSink.writePart(new Part(new Header("text/plain"), "0123456789"));

        dataSink.close();


        IHttpResponse response = respHdl.getResponse();
        Assert.assertEquals(200, response.getStatus());
   
        
        
        httpClient.close();
        server.close();
    }	
	
	

	@Test
	public void testFile() throws Exception {

		IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();

        String url = "http://localhost:" + server.getLocalPort() + "/MyResource";
        
        
        
        HttpClient httpClient = new HttpClient();
  
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        
        HttpRequestHeader header = new HttpRequestHeader("POST", url, "multipart/form-data; boundary=7766");
        BodyDataSink dataSink = httpClient.send(header, respHdl);

	    File file = QAUtil.createTestfile_80byte();
	    Part part = new Part(new Header(), file);
	    part.setHeader("Content-disposition", "attachment; filename=\"file2.gif\"");
        dataSink.writePart(part);
        
        dataSink.close();


        IHttpResponse response = respHdl.getResponse();
        Assert.assertEquals(200, response.getStatus());
   
        
        
        httpClient.close();
        server.close();
    }		
	
	
	@Test
	public void testCompletePartBuffered() throws Exception {

		IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();

        String url = "http://localhost:" + server.getLocalPort() + "/MyResource";
        
        
        
        HttpClient httpClient = new HttpClient();
  
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        
        HttpRequestHeader header = new HttpRequestHeader("POST", url, "multipart/mixed; boundary=7766");
        BodyDataSink dataSink = httpClient.send(header, respHdl);

        QAUtil.sleep(500);
        
        
        BodyDataSink partDataSink = dataSink.writePart(new HttpMessageHeader("application/x-www-form-urlencoded"));
        MultivalueMap body = new MultivalueMap(new NameValuePair("test", "testvalue"), new NameValuePair("test2", "testvalue2"));

        dataSink.writePart(new Part(new Header("text/plain"), "0123456789"));

        partDataSink.write(body.toString());
        partDataSink.close();
        
        dataSink.close();


        IHttpResponse response = respHdl.getResponse();
        Assert.assertEquals(200, response.getStatus());
        
   
        
        httpClient.close();
        server.close();
    }		
}