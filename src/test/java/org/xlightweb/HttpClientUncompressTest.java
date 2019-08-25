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




import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;



/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientUncompressTest  {

    

    @Test
    public void testSimple() throws Exception {
        System.out.println("testSimple");

    	IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				
				exchange.send(new HttpResponse(200, "text/plain", "1234567890", true));
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();
    	
    	HttpClient httpClient = new HttpClient();
    	httpClient.setAutoUncompress(true);
    	
    	IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
    	
    	Assert.assertEquals("true (auto uncompress)", response.getHeader("X-XLightweb-Uncompressed"));
    	Assert.assertEquals(200, response.getStatus());
    	Assert.assertEquals("1234567890", response.getBody().readString());
    	Assert.assertEquals(-1, response.getContentLength());
    	
    	httpClient.close();
    	server.close();
	}
    

    @Test
    public void testChunked() throws Exception {
        System.out.println("testChunked");

    	IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				
				HttpResponseHeader header = new HttpResponseHeader(200, "text/plain");
				header.setHeader("Content-Encoding", "gzip");
				BodyDataSink sink = exchange.send(header);
				sink.flush();
				sink.write(HttpUtils.compress("1234567890".getBytes()));
				sink.close();
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();
    	
    	HttpClient httpClient = new HttpClient();
    	httpClient.setAutoUncompress(true);
    	
    	IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));

    	QAUtil.sleep(500);
    	Assert.assertEquals("true (auto uncompress)", response.getHeader("X-XLightweb-Uncompressed"));
    	Assert.assertEquals(200, response.getStatus());
    	Assert.assertEquals("1234567890", response.getBody().readString());
    	
    	httpClient.close();
    	server.close();
	}	
    

    @Test
    public void testFixed() throws Exception {
        System.out.println("testFixed");
    	
    	byte[] d = new byte[0];
    	for (int i = 0; i < 30; i++) {
    		d = HttpUtils.merge(d, QAUtil.generateRandomByteArray(5000));
    	}
    	
    	final byte[] data = d;
    	
    	
    	IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				
				byte[] compressed = HttpUtils.compress(data);
				
				HttpResponseHeader header = new HttpResponseHeader(200, "text/plain");
				header.setHeader("Content-Encoding", "gzip");
				
				BodyDataSink dataSink = exchange.send(header, compressed.length);
				
				dataSink.write(compressed, 0, 574);
				QAUtil.sleep(200);
				
				dataSink.write(compressed, 574, 3000);
				QAUtil.sleep(200);
				
				dataSink.write(compressed, 3574, 210);
				QAUtil.sleep(200);
				
				dataSink.write(compressed, 3784, (compressed.length - 3784));
				QAUtil.sleep(200);
			}
		};
		
		
		HttpServer server = new HttpServer(reqHdl);
		server.start();
    	
    	HttpClient httpClient = new HttpClient();
    	httpClient.setAutoUncompress(true);
    	
    	IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
    	
    	Assert.assertEquals(200, response.getStatus());
    	Assert.assertArrayEquals(data, response.getBody().readBytes());
    	
    	httpClient.close();
    	server.close();
	}
}