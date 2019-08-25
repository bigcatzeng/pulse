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

import java.io.FileInputStream;
import java.io.IOException;

import junit.framework.Assert;




import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;




/**
*
* @author grro@xlightweb.org
*/
public final class FormParamRequestTest  {

	private static HttpServer server;
	private static String url;
	
	private static File file;
	private static byte[] data;
	
	
	
	@BeforeClass
	public static void setup() throws IOException {

		file = QAUtil.copyToTempfile("Testfile_1k.pdf");
		file.deleteOnExit();
		
		FileInputStream fis = new FileInputStream(file);
		data = new byte[(int) file.length()];
		fis.read(data);
		fis.close();
		
		
		
    	IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
    		@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {

				IHttpRequest request = exchange.getRequest();
				MultivalueMap body = new MultivalueMap(request.getBody());
				byte[] base64 = body.getParameter("file").getBytes();
				
				exchange.send(new HttpResponse(200, "application/pdf", HttpUtils.decodeBase64(base64)));
			}
		};

		server = new HttpServer(reqHdl);
		server.start();
		
		url = "http://localhost:" + server.getLocalPort() + "/test";
	}
	
	
	@AfterClass
	public static final void tearDown() throws IOException {
		server.close();
	}
	

	@Test
	public void testSimple() throws Exception {

		HttpClient httpClient = new HttpClient(); 

    	IHttpResponse response = httpClient.call(new PostRequest(url, new NameValuePair("file",  new String(HttpUtils.encodeBase64(data), "US-ASCII"))));
    	
    	Assert.assertEquals(200, response.getStatus());
    	Assert.assertTrue(QAUtil.isEquals(file, response.getBody().readByteBuffer()));
    	
    	httpClient.close();
	}			
	
	
	@Test
    public void testSimple2() throws Exception {

        HttpClient httpClient = new HttpClient(); 

        FormURLEncodedRequest request = new FormURLEncodedRequest(url);
        request.addParameter("file", new String(HttpUtils.encodeBase64(data), "US-ASCII"));
        request.addParameter("unused", "test");
        request.getParameter("file");
        
        IHttpResponse response = httpClient.call(request);
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(QAUtil.isEquals(file, response.getBody().readByteBuffer()));
        
        httpClient.close();
    }           	
	
	@Test
	public void testApacheClient() throws Exception {

		org.apache.commons.httpclient.HttpClient httpClient = new org.apache.commons.httpclient.HttpClient();
		
		PostMethod postMethod = new PostMethod(url);
		postMethod.addParameter(new org.apache.commons.httpclient.NameValuePair("file", new String(HttpUtils.encodeBase64(data), "US-ASCII")));

		httpClient.executeMethod(postMethod);
    	
    	Assert.assertEquals(200, postMethod.getStatusCode());
    	Assert.assertTrue(QAUtil.isEquals(file, postMethod.getResponseBody()));
    	
    	postMethod.releaseConnection();
	}			
}