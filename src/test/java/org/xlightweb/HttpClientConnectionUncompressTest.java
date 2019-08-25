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




import org.junit.Test;

import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;



/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientConnectionUncompressTest  {


	@Test
	public void testDeactivated() throws Exception {

    	IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				
				exchange.send(new HttpResponse(200, "text/plain", "1234567890", true));
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();
    	
    	HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
    	
    	IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
    	
    	Assert.assertEquals(200, response.getStatus());
    	Assert.assertEquals("1234567890", HttpUtils.decompress(response.getBody()).readString());
    	
    	con.close();
    	server.close();		
	
	}		
	

    @Test
    public void testSimple() throws Exception {

    	IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				
				exchange.send(new HttpResponse(200, "text/plain", "1234567890", true));
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();
    	
    	HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
    	con.setAutoUncompress(true);
    	
    	IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
    	
    	Assert.assertEquals("true (auto uncompress)", response.getHeader("X-XLightweb-Uncompressed"));
    	Assert.assertEquals(200, response.getStatus());
    	Assert.assertEquals("1234567890", response.getBody().readString());
    	
    	con.close();
    	server.close();
	}
    
    

    @Test
    public void testStrongCompressed() throws Exception {

    	IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				
				File file = QAUtil.copyToTempfile("ZippedFile.gz");
				file.deleteOnExit();
				
				byte[] data = new byte[(int) file.length()];
				FileInputStream fis = new FileInputStream(file);
				fis.read(data);
				fis.close();
				
				HttpResponseHeader header = new HttpResponseHeader(200, "text/plain");
				header.setHeader("Content-Encoding", "gzip");
				
				BodyDataSink dataSink = exchange.send(header);
				dataSink.write(data);
				dataSink.close();
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();
    	
    	HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
    	con.setAutoUncompress(true);
    	
    	IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
    	
    	Assert.assertEquals("true (auto uncompress)", response.getHeader("X-XLightweb-Uncompressed"));
    	Assert.assertEquals(200, response.getStatus());
    	Assert.assertTrue(response.getBody().readString().contains("Request for Comments: 1951"));
    	
    	con.close();
    	server.close();
	}
    

    @Test
    public void testPartialData() throws Exception {

    	IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				
				
				HttpResponseHeader header = new HttpResponseHeader(200, "text/plain");
				header.setHeader("Content-Encoding", "gzip");
				
				BodyDataSink dataSink = exchange.send(header);
				dataSink.flush();
				
				byte[] data = new byte[] { 31, -117, 8, 0, 0, 0, 0, 0, 0, 0, 51, 52, 50, 54, 49, 53, 51, -73, -80, 52, 0, 0, -27, -82, 29, 38, 10, 0, 0, 0 };
				dataSink.write(data);
				dataSink.close();
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();
    	
    	HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
    	con.setAutoUncompress(true);
    	
    	IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
    	
    	Assert.assertEquals(200, response.getStatus());
    	Assert.assertEquals("1234567890", response.getBody().readString());
    	
    	con.close();
    	server.close();
	}	    
    
    @Test
    public void testPartialData2() throws Exception {

    	IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				
				
				HttpResponseHeader header = new HttpResponseHeader(200, "text/plain");
				header.setHeader("Content-Encoding", "gzip");
				
				BodyDataSink dataSink = exchange.send(header);
				dataSink.flush();
				
				byte[] data = new byte[] { 31, -117 };
				dataSink.write(data);
				
				QAUtil.sleep(200);
				data = new byte[] { 8, 0, 0, 0, 0, 0, 0, 0, 51, 52, 50, 54, 49, 53, 51, -73, -80, 52, 0, 0, -27, -82, 29, 38, 10, 0, 0, 0 };
				dataSink.write(data);
				dataSink.close();
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();
    	
    	HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
    	con.setAutoUncompress(true);
    	
    	IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
    	
    	Assert.assertEquals(200, response.getStatus());
    	Assert.assertEquals("1234567890", response.getBody().readString());
    	
    	con.close();
    	server.close();
	}	        
    
    
    @Test
    public void testPartialData3() throws Exception {

    	IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				
				
				HttpResponseHeader header = new HttpResponseHeader(200, "text/plain");
				header.setHeader("Content-Encoding", "gzip");
				
				BodyDataSink dataSink = exchange.send(header);
				dataSink.flush();
				
				byte[] data = new byte[] { 31, -117, 8, 0 };
				dataSink.write(data);
				
				QAUtil.sleep(200);
				data = new byte[] { 0, 0, 0, 0, 0, 0, 51, 52, 50, 54, 49, 53, 51, -73, -80, 52, 0, 0, -27, -82, 29, 38, 10, 0, 0, 0 };
				dataSink.write(data);
				dataSink.close();
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();
    	
    	HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
    	con.setAutoUncompress(true);
    	
    	IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
    	
    	Assert.assertEquals(200, response.getStatus());
    	Assert.assertEquals("1234567890", response.getBody().readString());
    	
    	con.close();
    	server.close();
	}	      
    
    
    @Test
    public void testPartialData4() throws Exception {

    	IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				
				
				HttpResponseHeader header = new HttpResponseHeader(200, "text/plain");
				header.setHeader("Content-Encoding", "gzip");
				
				BodyDataSink dataSink = exchange.send(header);
				dataSink.flush();
				
				byte[] data = new byte[] { 31, -117, 8, 0, 0, 0, 0, 0, 0, 0 };
				dataSink.write(data);
				
				QAUtil.sleep(200);
				data = new byte[] { 51, 52, 50, 54, 49, 53, 51, -73, -80, 52, 0, 0, -27, -82, 29, 38, 10, 0, 0, 0 };
				dataSink.write(data);
				dataSink.close();
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();
    	
    	HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
    	con.setAutoUncompress(true);
    	
    	IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
    	
    	Assert.assertEquals(200, response.getStatus());
    	Assert.assertEquals("1234567890", response.getBody().readString());
    	
    	con.close();
    	server.close();
	}	   
    
    
    @Test
    public void testPartialData5() throws Exception {
    	IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				
				
				HttpResponseHeader header = new HttpResponseHeader(200, "text/plain");
				header.setHeader("Content-Encoding", "gzip");
				
				BodyDataSink dataSink = exchange.send(header);
				dataSink.flush();
				
				byte[] data = new byte[] { 31, -117, 8, 0, 0, 0, 0, 0, 0, 0, 51, 52 };
				dataSink.write(data);
				
				QAUtil.sleep(200);
				data = new byte[] { 50, 54, 49, 53, 51, -73, -80, 52, 0, 0, -27, -82, 29, 38, 10, 0, 0, 0 };
				dataSink.write(data);
				dataSink.close();
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();
    	
    	HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
    	con.setAutoUncompress(true);
    	
    	IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
    	
    	Assert.assertEquals(200, response.getStatus());
    	Assert.assertEquals("1234567890", response.getBody().readString());
    	
    	con.close();
    	server.close();
	}	 
    
    @Test
    public void testPartialData6() throws Exception {

    	IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				
				
				HttpResponseHeader header = new HttpResponseHeader(200, "text/plain");
				header.setHeader("Content-Encoding", "gzip");
				
				BodyDataSink dataSink = exchange.send(header);
				dataSink.flush();
				
				byte[] data = new byte[] { 31, -117, 8, 0, 0, 0, 0, 0, 0, 0, 51, 52, 50};
				dataSink.write(data);
				
				QAUtil.sleep(200);
				data = new byte[] { 54, 49, 53, 51, -73, -80, 52, 0, 0, -27, -82, 29, 38, 10, 0 };
				dataSink.write(data);
				
				QAUtil.sleep(200);
				data = new byte[] { 0, 0 };
				dataSink.write(data);
				dataSink.close();
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();
    	
    	HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
    	con.setAutoUncompress(true);
    	
    	IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
    	
    	Assert.assertEquals(200, response.getStatus());
    	Assert.assertEquals("1234567890", response.getBody().readString());
    	
    	con.close();
    	server.close();
	}
    
    
    @Test
    public void testPartialData7() throws Exception {

    	IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				
				
				HttpResponseHeader header = new HttpResponseHeader(200, "text/plain");
				header.setHeader("Content-Encoding", "gzip");
				
				BodyDataSink dataSink = exchange.send(header);
				dataSink.flush();
				
				byte[] data = new byte[] { 31, -117, 8, 0, 0, 0, 0, 0, 0, 0, 51, 52,50, 54, 49, 53, 51, -73, -80, 52, 0, 0, -27, -82, 29, 38 };
				dataSink.write(data);
				
				QAUtil.sleep(200);
				data = new byte[] { 10, 0, 0, 0 };
				dataSink.write(data);
				dataSink.close();
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();
    	
    	HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
    	con.setAutoUncompress(true);
    	
    	IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test"));
    	
    	Assert.assertEquals(200, response.getStatus());
    	Assert.assertEquals("1234567890", response.getBody().readString());
    	
    	con.close();
    	server.close();
	}	     
}