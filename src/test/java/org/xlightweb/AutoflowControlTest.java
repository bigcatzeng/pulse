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


import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;





/**
*
* @author grro@xlightweb.org
*/
public final class AutoflowControlTest  {

    private final AtomicBoolean isErrorOccured = new AtomicBoolean(false);
	
    

    @Test
    public void testSimple() throws Exception {
        System.out.println("testSimple");
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            
            @Supports100Continue
            public void onRequest(IHttpExchange exchange) throws IOException {
                
                IHttpRequest request = exchange.getRequest();
                exchange.sendContinueIfRequested();
                
                exchange.send(new HttpResponse(200, request.getContentType(), request.getNonBlockingBody()));
            }            
        };   
        
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        byte[] data = QAUtil.generateByteArray(400000); 
        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/test", "text/plain", data);
        
        IHttpResponse response = con.call(request);
        
        Assert.assertNull(response.getAttribute("org.xlightweb.HttpClientConnection.100-continueReceived"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertArrayEquals(data, response.getBody().readBytes());
        
        con.close();
        server.close();
    }    
    

    @Ignore
	@Test
	public void testProxy() throws Exception {
        System.out.println("testProxy");
	    isErrorOccured.set(false);

	    
	    final long startTime = System.currentTimeMillis();
	    
	    RequestHandler reqHdl = new RequestHandler();
		HttpServer server = new HttpServer(reqHdl);
		server.start();

        
        HttpClient httpClientProxy = new HttpClient();
        httpClientProxy.setAutoHandleCookies(false);

		HttpServer proxy = new HttpServer(new ProxyHandler(server.getLocalPort(), httpClientProxy));
		proxy.start();

		
		HttpClient httpClient = new HttpClient();
		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		BodyDataSink dataSink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + proxy.getLocalPort() + "/", "text/plain"), respHdl);
		dataSink.setSendTimeoutMillis(60 * 60 * 1000);
		
		dataSink.write(QAUtil.generateByteArray(40000));
		
		while (reqHdl.getLastExchange() == null) {
		    QAUtil.sleep(100);
		} 
		
		final AtomicBoolean isAllDataSend = new AtomicBoolean(); 
		final IHttpExchange serverExchange = reqHdl.getLastExchange();
		
		System.out.println("[" + (System.currentTimeMillis() - startTime) + "] server: suspend receiving");
		serverExchange.getConnection().suspendReceiving();
		Runnable resumeTask = new Runnable() {
		    
		    public void run() {
		        
		        QAUtil.sleep(1500);
		        if (isAllDataSend.get()) {
		            isErrorOccured.set(true);
		        }
		        try {
		            System.out.println("[" + (System.currentTimeMillis() - startTime) + "]  server: resume receving");
		            serverExchange.getConnection().resumeReceiving();
		        } catch(IOException ioe) {
		            throw new RuntimeException(ioe);
		        }
		    }
		};
		new Thread(resumeTask).start();
		
		System.out.println("[" + (System.currentTimeMillis() - startTime) + "]  client: start sending...");
		for (int i = 0; i < 100; i++) {
		    dataSink.write(QAUtil.generateByteArray(1000));
		}
		isAllDataSend.set(true);
		
		System.out.println("[" + (System.currentTimeMillis() - startTime) + "]  client: finished sending");
		
		QAUtil.sleep(2500);
		
		Assert.assertFalse(isErrorOccured.get());
		
		httpClient.close();
		proxy.close();
		server.close();
	}


    @Ignore
    @Test
    public void testProxy2() throws Exception {
        System.out.println("testProxy2");

        isErrorOccured.set(false);
        
        final long startTime = System.currentTimeMillis();
        
        RequestHandler reqHdl = new RequestHandler();
        HttpServer server = new HttpServer(reqHdl);
        server.start();

        HttpClient httpClientProxy = new HttpClient();
        HttpServer proxy = new HttpServer(new ProxyHandler(server.getLocalPort(), httpClientProxy));
        proxy.start();

        
        HttpClient httpClient = new HttpClient();
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        BodyDataSink dataSink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + proxy.getLocalPort() + "/", "text/plain"), respHdl);
        dataSink.setSendTimeoutMillis(60 * 60 * 1000);
        
        dataSink.write(QAUtil.generateByteArray(40000));
        
        while (reqHdl.getLastExchange() == null) {
            QAUtil.sleep(100);
        } 
        
        final AtomicBoolean isAllDataSend = new AtomicBoolean(); 
        final IHttpExchange serverExchange = reqHdl.getLastExchange();
        
        System.out.println("[" + (System.currentTimeMillis() - startTime) + "] server: suspend receiving");
        serverExchange.getConnection().suspendReceiving();
        Runnable resumeTask = new Runnable() {
            
            public void run() {
                
                QAUtil.sleep(1500);
                if (isAllDataSend.get()) {
                    isErrorOccured.set(true);
                }
                try {
                    System.out.println("[" + (System.currentTimeMillis() - startTime) + "]  server: resume receving");
                    serverExchange.getConnection().resumeReceiving();
                } catch(IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        };
        new Thread(resumeTask).start();
        
        System.out.println("[" + (System.currentTimeMillis() - startTime) + "]  client: start sending...");
        for (int i = 0; i < 100; i++) {
            dataSink.write(QAUtil.generateByteArray(1000));
        }
        isAllDataSend.set(true);
        
        System.out.println("[" + (System.currentTimeMillis() - startTime) + "]  client: finished sending");
        
        QAUtil.sleep(2500);
        
        Assert.assertFalse(isErrorOccured.get());
        
        httpClient.close();
        proxy.close();
        server.close();        
    }

    
    
       

  	
    private static final class ProxyHandler implements IHttpRequestHandler {
        
        private final HttpClient httpClient;
        
        private final int port;
        
        public ProxyHandler(int port, HttpClient httpClient) {
            this.port = port;
            this.httpClient = httpClient;
        }
        
        public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
            
            IHttpResponseHandler respHdl = new IHttpResponseHandler() {
                
                public void onResponse(IHttpResponse response) throws IOException {
                    exchange.send(response);
                }
                
                public void onException(IOException ioe) throws IOException {
                    exchange.sendError(ioe);
                }
            };
            
            IHttpRequest request = exchange.getRequest();
            URL url = request.getRequestUrl();
            request.setRequestUrl(new URL(url.getProtocol(), url.getHost(), port, url.getFile()));
            
            httpClient.send(request, respHdl);
        }
    }   	
  
    
    
	
	private static final class RequestHandler implements IHttpRequestHandler {
	 
	    private final AtomicReference<IHttpExchange> exchangeRef = new AtomicReference<IHttpExchange>(); 
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        exchangeRef.set(exchange);
	    }
	    
	    public IHttpExchange getLastExchange() {
	        return exchangeRef.get();
	    }
	}	
}
