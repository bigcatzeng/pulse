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







import java.io.File;


import java.io.IOException;
import java.net.URLEncoder;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;





/**
*
* @author grro@xlightweb.org
*/
public final class SimpleDocumentStoreTest  {

	private static String basepath; 
	
	
	
	@BeforeClass
	public static void setUp() throws IOException {
		basepath = new File(new File("").getAbsoluteFile() + File.separator + "store").toString(); 
	}
	
	
	public static void tearDown() {
		for (File file : new File(basepath).listFiles()) {
			file.delete();
		}
		new File(basepath).delete();
	}

	
	@Test
	public void testExampleUseCase() throws Exception {
		
		// create a test file
		File largeTestFile = QAUtil.createTestfile_4000k();
		largeTestFile.deleteOnExit();
		
		File mediumFile = QAUtil.createTestfile_4k();
		mediumFile.deleteOnExit();
		
		File smallTestFile = QAUtil.createTestfile_650byte();
		smallTestFile.deleteOnExit();
		
		// start the server
		HttpServer server = new SimpleDocumentStore(0, basepath, 40 * 1000);
		server.start();

		// create the client
		HttpClient httpClient = new HttpClient();
		httpClient.setCacheMaxSizeKB(1000);

		
		
		
		
		
		System.out.println("try to upload a new document which is too large");
		IHttpRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/?cacheExpiresSec=2", largeTestFile);
		request.setHeader("Expect", "100-continue");  // client will wait for 100-continue response before sending request body   
		
		IHttpResponse response = httpClient.call(request);
		Assert.assertEquals(413, response.getStatus());
		
		
		
		System.out.println("upload a smaller one");
		request = new PostRequest("http://localhost:" + server.getLocalPort() + "/?cacheExpiresSec=2", smallTestFile);
		request.setHeader("Expect", "100-continue");
		
		response = httpClient.call(request);
		String docUriOrg = response.getHeader("Location");

		
		
		System.out.println("copying it (create a new document based on a URI");
		request = new PostRequest("http://localhost:" + server.getLocalPort() + "/?cacheExpiresSec=2&sourceURI=" + URLEncoder.encode(docUriOrg, "UTF-8"));
		response = httpClient.call(request);
		String docUri = response.getHeader("Location");

		
		System.out.println("retrieve the document");
		request = new GetRequest(docUri);
		response = httpClient.call(request);
		String doc = response.getBody().readString();
		
		
		System.out.println("retrieve the document zipped");
		request = new GetRequest(docUri);
		request.setHeader("Accept-Encoding", "gzip");
		
		response = httpClient.call(request);
		doc = response.getBody().readString();
		
				
		System.out.println("retrieve partial");
		request = new GetRequest(docUri);
		request.setHeader("Range", "bytes=516-");
		
		response = httpClient.call(request);
		doc = response.getBody().readString();

		
		System.out.println("update the document");
		request = new PutRequest(docUri + "/?cacheExpiresSec=2", mediumFile);
		request.setHeader("Expect", "100-continue");    
try {		
		response = httpClient.call(request);
} catch (IOException ioe) {
    ioe.printStackTrace();
}
		
		System.out.println("retrieve the updated document (uri stay the same) after the cache time!");
		QAUtil.sleep(3000);
		
		request = new GetRequest(docUri);
		response = httpClient.call(request);
		doc = response.getBody().readString();
		
		httpClient.close();
		server.close();
	}	
	
	

    @Test
	public void testLastModified() throws Exception {
		
		HttpServer server = new SimpleDocumentStore(0, basepath);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		
		
		// create a new document 
		IHttpRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/", "text/plain", "0123456789");
		IHttpResponse response = httpClient.call(request);
		
		Assert.assertEquals(201, response.getStatus());
		String docUri = response.getHeader("Location");
		
		
		// retrieve it
		request = new GetRequest(docUri);
		response = httpClient.call(request);
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("0123456789", response.getBody().readString());
		
		String lastModified = response.getHeader("Last-Modified"); 

		// retrieve it again, if modified
		request = new GetRequest(docUri);
		request.setHeader("If-Modified-Since", lastModified);
		response = httpClient.call(request);
		
		Assert.assertEquals(304, response.getStatus());

		
		httpClient.close();
		server.close();
	}	
	
	
	@Test
	public void testContinue() throws Exception {

		HttpServer server = new SimpleDocumentStore(0, basepath);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		
		
		// create a new document 
		IHttpRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/", "text/plain", "0123456789");
		request.setHeader("Expect", "100-continue");
		
		IHttpResponse response = httpClient.call(request);
		
		Assert.assertEquals(201, response.getStatus());
		String docUri = response.getHeader("Location");
		
		
		// retrieve it
		request = new GetRequest(docUri);
		
		response = httpClient.call(request);
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("0123456789", response.getBody().readString());


		
		httpClient.close();
		server.close();
	}	


	
	@Test
	public void testCaching() throws Exception {
		
		HttpServer server = new SimpleDocumentStore(0, basepath);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		httpClient.setCacheMaxSizeKB(1000);
		
		
		// create a new document 
		IHttpRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/?cacheExpiresSec=3600", "text/plain", "0123456789");
		
		IHttpResponse response = httpClient.call(request);
		
		Assert.assertEquals(201, response.getStatus());
		String docUri = response.getHeader("Location");
		
		
		// retrieve it
		request = new GetRequest(docUri);
		
		response = httpClient.call(request);
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("0123456789", response.getBody().readString());
		Assert.assertNotNull(response.getHeader("Expires"));


		// retrieve it again (should be served by httpClient cache)
		request = new GetRequest(docUri);
		
		response = httpClient.call(request);
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("0123456789", response.getBody().readString());
		Assert.assertNotNull(response.getHeader("X-Cache"));


		
		httpClient.close();
		server.close();
	}
	
	
	@Test
	public void testRange() throws Exception {
		
		HttpServer server = new SimpleDocumentStore(0, basepath);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		
		
		// create a new document 
		IHttpRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/", "text/plain", "0123456789");
		
		IHttpResponse response = httpClient.call(request);
		
		Assert.assertEquals(201, response.getStatus());
		String docUri = response.getHeader("Location");
		
		
		// retrieve it
		request = new GetRequest(docUri);
		request.setHeader("Range", "bytes=6-");
		
		response = httpClient.call(request);
		
		Assert.assertEquals(206, response.getStatus());
		Assert.assertEquals("6789", response.getBody().readString());

		httpClient.close();
		server.close();
	}
	
	
	
	@Test
	public void testBrokenUpload() throws Exception {
		HttpServer server = new SimpleDocumentStore(0, basepath);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		
		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		BodyDataSink dataSink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/", "text/plain"), 100, respHdl);

		dataSink.write("1234");
		
		QAUtil.sleep(500);
		dataSink.destroy();
		
		
		httpClient.close();
		server.close();
	}


}