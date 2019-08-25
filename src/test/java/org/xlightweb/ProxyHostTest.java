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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.GetRequest;
import org.xlightweb.IHttpResponse;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;



/**
*
* @author grro@xlightweb.org
*/
public final class ProxyHostTest  {

	
	public static void main(String[] args) throws Exception {

		for (int i = 0; i < 100; i++) {
			new ProxyHostTest().testHttpClientWithAuthMissingPassword();
		}
	}


	@Test
	public void testLiveURLConnectionWithAuth() throws Exception {
		
		System.out.println("testLiveURLConnectionWithAuth");
		
		HttpProxy2 proxy = new HttpProxy2(true);
		proxy.start();
		
		URL url = new URL("http://www.amazon.de/");
		System.setProperty("http.proxyHost", "localhost");
		System.setProperty("http.proxyPort", Integer.toString(proxy.getLocalPort()));

		URLConnection con =  url.openConnection();

	    
	    con.setRequestProperty("Proxy-Authorization", 
	    		               "Basic " + new String(Base64.encodeBase64(("test:test").getBytes())));

		
		
		StringBuilder sb = new StringBuilder();
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			sb.append(inputLine + "\r\n");
		}
		in.close();
		
		proxy.close();
		
		Assert.assertTrue(sb.toString().indexOf("amazon") != -1);
	}

	
	@Test
	public void testLiveSSLURLConnection() throws Exception {
	    
	    System.out.println("testLiveSSLURLConnection");
		
		HttpProxy2 proxy = new HttpProxy2(false);
		proxy.start();
		
		URL url = new URL("https://www.amazon.de/");
		System.setProperty("https.proxyHost", "localhost");
		System.setProperty("https.proxyPort", Integer.toString(proxy.getLocalPort()));

		URLConnection con =  url.openConnection();

		
		StringBuilder sb = new StringBuilder();
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			sb.append(inputLine + "\r\n");
		}
		in.close();
		
		proxy.close();
		
		Assert.assertTrue(sb.toString().indexOf("amazon") != -1);
	}


	@Test
	public void testLiveHttpClient() throws Exception {
		
		System.out.println("testLiveHttpClient");
		
		HttpProxy2 proxy = new HttpProxy2(false);
		proxy.start();
		
		
		HttpClient httpClient = new HttpClient(SSLTestContextFactory.getSSLContext());
		httpClient.setProxyHost("localhost");
		httpClient.setProxyPort(proxy.getLocalPort());

		GetRequest request = new GetRequest("http://www.amazon.de/");
		IHttpResponse response = httpClient.call(request);
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertTrue(response.getBody().readString().indexOf("amazon") != -1);
		
		
		httpClient.close();
		proxy.close();
	}	
	

	@Test
	public void testSSLHttpClient() throws Exception {
	
		System.out.println("testSSLHttpClient");
		
		HttpServer server = new HttpServer(0, new RequestHandler(), SSLTestContextFactory.getSSLContext(), true);
		server.start();

		HttpProxy2 proxy = new HttpProxy2(false);
		proxy.start();
		
		
		HttpClient httpClient = new HttpClient(SSLTestContextFactory.getSSLContext());
		httpClient.setProxyHost("localhost");
		httpClient.setProxyPort(proxy.getLocalPort());
		httpClient.setProxyUser("test");
		httpClient.setProxyPassword("test");

		IHttpResponse response = httpClient.call(new GetRequest("https://localhost:" + server.getLocalPort() + "/"));
		
		String body = response.getBody().readString();

		Assert.assertEquals(200, response.getStatus());
		Assert.assertTrue(body.indexOf("it works") != -1);
		
		
		httpClient.close();
		proxy.close();
		server.close();
	}	

	
    @Test
    public void testSSLHttpClient2() throws Exception {
    
        System.out.println("testSSLHttpClient");
        
        HttpServer server = new HttpServer(0, new RequestHandler(), SSLTestContextFactory.getSSLContext(), true);
        server.start();

        HttpProxy2 proxy = new HttpProxy2(false);
        proxy.start();
        
        
        HttpClient httpClient = new HttpClient(SSLTestContextFactory.getSSLContext());
        httpClient.setProxyHost("localhost");
        httpClient.setProxyPort(proxy.getLocalPort());
        httpClient.setProxyUser("test");
        httpClient.setProxyPassword("test");

        IHttpResponse response = httpClient.call(new GetRequest("https://localhost:" + server.getLocalPort() + "/"));
        
        String body = response.getBody().readString();

        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(body.indexOf("it works") != -1);
        
        
        httpClient.close();
        proxy.close();
        server.close();
    }   	
	

	@Test
	public void testLiveHttpClientWithAuth() throws Exception {
		
		System.out.println("testLiveHttpClientWithAuth");
		
		HttpProxy2 proxy = new HttpProxy2(true);
		proxy.start();
		
		
		HttpClient httpClient = new HttpClient();
		httpClient.setProxyHost("localhost");
		httpClient.setProxyPort(proxy.getLocalPort());
		httpClient.setProxyUser("test");
		httpClient.setProxyPassword("test");
		

		IHttpResponse response = httpClient.call(new GetRequest("http://www.amazon.de/"));
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertTrue(response.getBody().readString().indexOf("amazon") != -1);
		
		
		httpClient.close();
		proxy.close();
	}	
	
	
	@Test
	public void testHttpClientWithAuthMissingPassword() throws Exception {
		
		System.out.println("testHttpClientWithAuthMissingPassword");

		Logger LOG = Logger.getLogger("org.xlightweb.test");
		
		LOG.fine("start proxy");
		HttpProxy2 proxy = new HttpProxy2(true);
		proxy.start();
		
		
		HttpClient httpClient = new HttpClient();
		httpClient.setProxyHost("localhost");
		httpClient.setProxyPort(proxy.getLocalPort());
		httpClient.setProxyUser("test");
		

		try {
			LOG.fine("call");
			httpClient.call(new GetRequest("http://www.amazon.de/"));
			Assert.fail("IOException expected");
		} catch (IOException expected) {  }
		
		
		LOG.fine("returned");
		httpClient.close();
		proxy.close();
	}	
	
	
	
	private static final class RequestHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			exchange.send(new HttpResponse(200, "text/plain", "it works"));
		}
	}
	
}
