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




import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;


import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClient.FollowsRedirectMode;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IServer;



/**
 *
 * @author grro@xlightweb.org
 */
public class HttpClientPoolingTest {
	

	
	@Test
	public void testLocal() throws Exception {
		
		System.out.println("testing local ...");
			
		IServer server = new HttpServer(new RequestHandler());
		server.start();

		call(10, "http://localhost:" + server.getLocalPort() + "/test", 200, false);
		
		server.close();
	}
	

	
	@Test
	public void testLive() throws Exception {
		System.setProperty("org.xlightweb.showDetailedError", "true");
		
		System.out.println("testing live ...");

		call(1, "http://www.google.com:81", 200, true);
	//	call(5, "http://www.netscape.com/", 200, false);
		call(5, "http://www.asdasdadasdasdasdasd.com", 200, true);
		call(5, "http://www.google.com/", 200, false);
		call(5, "http://www.apache.org/", 200, false);
		call(5, "http://www.yahoo.com/", 200, false);
	}

	
	@Test
	public void testLiveSSL() throws Exception {
	    
		System.out.println("testing live ssl ...");
		
//		call(3, "https://www.netscape.com/", 200, true);
		call(3, "https://www.verisign.com/", 200, true);
		call(3, "https://www.yahoo.com/", 200, true);
	}

	
	
	
	private void call(int loops, String url, int expectedStatus, boolean ignoreException) throws Exception {
		
		System.out.println("calling " + url + " " + loops +  " times");
		
		for (int i = 0; i < loops; i++) {
			System.out.print(".");
			HttpClient httpClient = new HttpClient(createSSLContext());
			httpClient.setConnectTimeoutMillis(1000);
	        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);

			try {
				IHttpResponse response = httpClient.call(new GetRequest(url));
				if (response.getStatus() != expectedStatus) {
					String txt = "error occured by calling " + url + " got " + response.getStatus() + " instead of " + expectedStatus;
					System.out.println(txt);
					Assert.fail(txt);
				}
			} catch (Exception e) {
			    
			    QAUtil.sleep(300);
			    
			    int numActive = httpClient.getNumActive();
			    if (numActive != 0) {
			        System.out.println(url + " active con should be 0 not " + numActive);
			        Assert.fail(url + " active con should be 0 not " + numActive);
			    }
			    
				if (!ignoreException) {
					throw e;
				}
			}
	        
			httpClient.close();
			QAUtil.sleep(300);
	    }
	}
	
	

	
	
	private SSLContext createSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
		
		TrustManager[] trustAllCerts = new TrustManager[] {
											new X509TrustManager() {
												public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
												
												public void checkClientTrusted(final X509Certificate[] certs, final String authType) { /* NOP */ }
												
												public void checkServerTrusted(final X509Certificate[] certs, final String authType) { /* NOP */ }
											}
										};
	                                        
		SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, trustAllCerts, null);
		return sslContext;
	}
	
	
	
	
	private static final class RequestHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			exchange.send(new HttpResponse(200, "text/plain", "it works"));
		}		
	}
}	
	
	
