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


import java.util.logging.Logger;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.BodyDataSink;
import org.xlightweb.GetRequest;
import org.xlightweb.HttpResponse;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.HttpUtils;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IConnection.FlushMode;




/**
*
* @author grro@xlightweb.org
*/
public final class AutoCompressTest  {

	private static final Logger LOG = Logger.getLogger(AutoCompressTest.class.getName());
	
	
	public static void main(String[] args) throws Exception {
		
		for (int i = 0; i < 10000; i++) {
			LOG.fine("run " + i);
			new AutoCompressTest().testServerSide();
		}
	}
	

    @Test
    public void testServerSide() throws Exception {
		
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException {
                exchange.send(new HttpResponse(200, "text/plain", "1234567890"));
            }            
        };   
        
        HttpServer server = new HttpServer(reqHdl);
        server.setAutoCompressThresholdBytes(5);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setAutoUncompress(false);
        
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
        request.setHeader("Accept-Encoding", "gzip");
        
        IHttpResponse response = httpClient.call(request); 
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("gzip", response.getHeader("Content-Encoding"));
        
        byte[] data = response.getBody().readBytes();
        Assert.assertEquals("1234567890", new String(HttpUtils.decompress(data)));
        
        httpClient.close();
        server.close();
    }        
    
    
    @Test
    public void testServerSide2() throws Exception {
    	
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException {
            	
            	BodyDataSink ds = exchange.send(new HttpResponseHeader(200, "text/plain"), 10);
            	ds.flush();
            	ds.write("1234567890");
            	ds.close();
            }            
        };   
        
        HttpServer server = new HttpServer(reqHdl);
        server.setAutoCompressThresholdBytes(5);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setAutoUncompress(false);
        
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
        request.setHeader("Accept-Encoding", "gzip");
        
        IHttpResponse response = httpClient.call(request); 
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("gzip", response.getHeader("Content-Encoding"));
        Assert.assertEquals("1234567890", new String(HttpUtils.decompress(response.getBody().readBytes())));
        
        httpClient.close();
        server.close();
    }            
    
    
    @Test
    public void testServerSide3() throws Exception {
    	
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException {
            	
            	BodyDataSink ds = exchange.send(new HttpResponseHeader(200, "text/plain"));
            	ds.write("1234567890");
            	ds.close();
            }            
        };   
        
        HttpServer server = new HttpServer(reqHdl);
        server.setAutoCompressThresholdBytes(5);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setAutoUncompress(false);
        
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
        request.setHeader("Accept-Encoding", "gzip");
        
        IHttpResponse response = httpClient.call(request); 
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("gzip", response.getHeader("Content-Encoding"));
        Assert.assertEquals("1234567890", new String(HttpUtils.decompress(response.getBody().readBytes())));
        
        httpClient.close();
        server.close();
    }    

    
    @Test
    public void testServerSide4() throws Exception {
    	
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException {
            	
            	BodyDataSink ds = exchange.send(new HttpResponseHeader(200, "text/plain"));
            	
            	for (int i = 0; i < 100; i++) {
            		ds.write("1234567890");
            	}
            	
        		ds.close();
            }            
        };   
        
        HttpServer server = new HttpServer(reqHdl);
        server.setAutoCompressThresholdBytes(5);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setAutoUncompress(false);
        
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
        request.setHeader("Accept-Encoding", "gzip");
        
        IHttpResponse response = httpClient.call(request); 
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("gzip", response.getHeader("Content-Encoding"));
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
    		sb.append("1234567890");
    	}
        Assert.assertEquals(sb.toString(), new String(HttpUtils.decompress(response.getBody().readBytes())));
        
        httpClient.close();
        server.close();
    }    
    
    
    @Test
    public void testServerSide5() throws Exception {
    	
    	final byte[] data = QAUtil.generateByteArray(10000);
		
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException {
                exchange.send(new HttpResponse(200, "text/plain", data));
            }            
        };   
        
        HttpServer server = new HttpServer(reqHdl);
        server.setAutoCompressThresholdBytes(5);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setAutoUncompress(false);
        
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
        request.setHeader("Accept-Encoding", "gzip");
        
        IHttpResponse response = httpClient.call(request); 
        
        Assert.assertEquals("gzip", response.getHeader("Content-Encoding"));
        Assert.assertEquals(200, response.getStatus());
        
        Assert.assertArrayEquals(data, HttpUtils.decompress(response.getBody().readBytes()));
        
        httpClient.close();
        server.close();
    }        
    
    @Test
    public void testServerSide6() throws Exception {
    	
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException {
            	
            	BodyDataSink ds = exchange.send(new HttpResponseHeader(200, "text/plain"));
            	ds.flush();
            	ds.write("1234567890");
            	ds.close();
            }            
        };   
        
        HttpServer server = new HttpServer(reqHdl);
        server.setAutoCompressThresholdBytes(5);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setAutoUncompress(false);
        
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
        request.setHeader("Accept-Encoding", "gzip");
        
        IHttpResponse response = httpClient.call(request); 
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertNull(response.getHeader("Content-Encoding"));
        Assert.assertEquals("1234567890", response.getBody().readString());
        
        httpClient.close();
        server.close();
    }       
    
    
    
    @Test
    public void testServerSide7() throws Exception {
    	
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException {
            	
            	BodyDataSink ds = exchange.send(new HttpResponseHeader(200, "text/plain"));
            	ds.setFlushmode(FlushMode.ASYNC);
            	
            	for (int i = 0; i < 100; i++) {
            		ds.write("1234567890");
            	}
            	
        		ds.close();
            }            
        };   
        
        HttpServer server = new HttpServer(reqHdl);
        server.setAutoCompressThresholdBytes(5);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setAutoUncompress(false);
        
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
        request.setHeader("Accept-Encoding", "gzip");
        
        IHttpResponse response = httpClient.call(request); 
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("gzip", response.getHeader("Content-Encoding"));
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
    		sb.append("1234567890");
    	}
        Assert.assertEquals(sb.toString(), new String(HttpUtils.decompress(response.getBody().readBytes())));
        
        httpClient.close();
        server.close();
    }    
        
    
    @Test
    public void testServerSide8() throws Exception {
    	
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException {
            	
            	BodyDataSink ds = exchange.send(new HttpResponseHeader(200, "text/plain"));
            	ds.setFlushmode(FlushMode.ASYNC);
            	ds.setAutoflush(false);
            	
            	for (int i = 0; i < 100; i++) {
            		ds.write("1234567890");
            	}
        		ds.close();
            }            
        };   
        
        HttpServer server = new HttpServer(reqHdl);
        server.setAutoCompressThresholdBytes(5);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setAutoUncompress(false);
        
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
        request.setHeader("Accept-Encoding", "gzip");
        
        IHttpResponse response = httpClient.call(request); 
        
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("gzip", response.getHeader("Content-Encoding"));
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
    		sb.append("1234567890");
    	}
        Assert.assertEquals(sb.toString(), new String(HttpUtils.decompress(response.getBody().readBytes())));
        
        httpClient.close();
        server.close();
    }        
    
    
    @Test
    public void testServerSide9() throws Exception {
    	
    	final byte[] data = QAUtil.generateByteArray(10000);
		
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException {
                exchange.send(new HttpResponse(200, "text/plain", data));
            }            
        };   
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setAutoUncompress(false);
        
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
        request.setHeader("Accept-Encoding", "gzip");
        
        IHttpResponse response = httpClient.call(request); 
        
        Assert.assertEquals("gzip", response.getHeader("Content-Encoding"));
        Assert.assertEquals(200, response.getStatus());
        
        Assert.assertArrayEquals(data, HttpUtils.decompress(response.getBody().readBytes()));
        
        httpClient.close();
        server.close();
    }            
    
    @Test
    public void testServerSideUncompressed() throws Exception {
    	
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException {
                exchange.send(new HttpResponse(200, "text/plain", "1234567890"));
            }            
        };   
        
        HttpServer server = new HttpServer(reqHdl);
        server.setAutoCompressThresholdBytes(10);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setAutoUncompress(false);
        
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
        
        IHttpResponse response = httpClient.call(request); 
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("1234567890", response.getBody().readString());
        
        
        httpClient.close();
        server.close();
    }
    
    
    @Test
    public void testServerSideUncompressed2() throws Exception {
    	
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException {
            	HttpResponse response = new HttpResponse(200, "text/plain", "1234567890");
            	response.setHeader("Content-Encoding", "gzip");
                exchange.send(response);
            }            
        };   
        
        HttpServer server = new HttpServer(reqHdl);
        server.setAutoCompressThresholdBytes(10);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setAutoUncompress(false);
        
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/");
        
        IHttpResponse response = httpClient.call(request); 
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("1234567890", response.getBody().readString());
        
        
        httpClient.close();
        server.close();
    }    
}
