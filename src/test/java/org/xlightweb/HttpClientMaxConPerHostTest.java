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


import java.util.concurrent.atomic.AtomicInteger;


import junit.framework.Assert;

import org.junit.Test;
import org.xlightweb.BadMessageException;
import org.xlightweb.GetRequest;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.MaxConnectionsExceededException;



/**
 * 
 * @author grro@xlightweb.org
 */
public final class HttpClientMaxConPerHostTest  {

     
    
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 1000; i++) {
            System.out.println("run " + i);
            new HttpClientMaxConPerHostTest().testSimple();
        }
    }

    
	@Test
	public void testSimple() throws Exception {
	    
	    RequestHandler reqHdl = new RequestHandler();
        final HttpServer server = new HttpServer(0, reqHdl, 4, 10);
        server.start();
        
        final HttpClient httpClient = new HttpClient();
        httpClient.setMaxActivePerServer(2);


        System.out.println(System.currentTimeMillis() + " start first thread");
        
        httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?delay=3000"), new ResponseHandler());
        QAUtil.sleep(300);

        
        System.out.println(System.currentTimeMillis() + " start second thread");
        httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?delay=3000"), new ResponseHandler());

        
        System.out.println("waiting util started");
        while(reqHdl.getRunning() < 2) {
            QAUtil.sleep(100);
        }

        
        System.out.println("perform third call (should fail)");
        try {
            httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort()));
            Assert.fail("MaxConnectionsExceededException expected");
        } catch (MaxConnectionsExceededException expected) { }
        
        
        
        System.out.println("waiting until less than 2 threads are running");
        while(reqHdl.getRunning() >= 2) {
            QAUtil.sleep(100);
        }
        QAUtil.sleep(1000);
        
        
        System.out.println("try again calling (should be successful)");
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort()));
        Assert.assertEquals(200, response.getStatus());
        
        httpClient.close();
        server.close();
	}
	
	
    @Test
    public void testSimpleWithCache() throws Exception {
        
        
        RequestHandler reqHdl = new RequestHandler();
        final HttpServer server = new HttpServer(0, reqHdl, 4, 10);
        server.start();
        
        final HttpClient httpClient = new HttpClient();
        httpClient.setCacheMaxSizeKB(100);
        httpClient.setMaxActivePerServer(2);

        System.out.println(System.currentTimeMillis() + " start first thread");
        httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?delay=3000"), new ResponseHandler());
        QAUtil.sleep(300);

        
        System.out.println(System.currentTimeMillis() + " start second thread");
        httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?delay=3000"));

        
        while(reqHdl.getRunning() < 2) {
            QAUtil.sleep(100);
        }
        
        try {
            httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/?delay=2000"));
            Assert.fail("MaxConnectionsExceededException expected");
        } catch (MaxConnectionsExceededException expected) { }
        
        
        while(reqHdl.getRunning() >= 2) {
            QAUtil.sleep(100);
        }
        QAUtil.sleep(1000);

        
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort()));
        Assert.assertEquals(200, response.getStatus());
        
        httpClient.close();
        server.close();
    }
    
    
    @Test
    public void testNorExceeded() throws Exception {
        
        RequestHandler reqHdl = new RequestHandler();
        final HttpServer server = new HttpServer(0, reqHdl, 4, 10);
        server.start();
        
        final HttpClient httpClient = new HttpClient();
        httpClient.setMaxActivePerServer(100);


        for (int i = 0; i < 50; i++) {
            httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?delay=3000"), new ResponseHandler());
        }
        
        QAUtil.sleep(1000);
        
        httpClient.close();
        server.close();
    }    
    
    private static final class ResponseHandler implements IHttpResponseHandler {
        
        public void onException(IOException ioe) throws IOException {
            ioe.printStackTrace();
        }
        
        public void onResponse(IHttpResponse response) throws IOException {
            
        }
    }
    
    
    
    private static final class RequestHandler implements IHttpRequestHandler {
        
        private final AtomicInteger running = new AtomicInteger(0);
        
        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
            running.incrementAndGet();
            
            int delay = exchange.getRequest().getIntParameter("delay", 0);
            System.out.println("[" + Thread.currentThread().getName() + "] " + System.currentTimeMillis() + " start handle request delay=" + delay + " running=" + running.get());


            QAUtil.sleep(delay);
            
            HttpResponse resp = new HttpResponse(200, "text/plain", "test");
            exchange.send(resp);
            
            running.decrementAndGet();
            System.out.println(System.currentTimeMillis() + " end handle request delay=" + delay + " running=" + running.get());
        }
        
        
        public int getRunning() {
            return running.get();
        }
    };     
}
