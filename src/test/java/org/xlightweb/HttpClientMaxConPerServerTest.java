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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClient.FollowsRedirectMode;
import org.xlightweb.server.HttpServer;
import org.xsocket.ILifeCycle;
import org.xsocket.connection.MaxConnectionsExceededException;






/**  
*
* @author grro@xlightweb.org
*/
public final class HttpClientMaxConPerServerTest  {
	
    
    private HttpServer httpServer;
    private ServerHandler serverHandler;
    private HttpClient httpClient;
    private ExecutorService executorService;

    

    @Before
    public void setUp() throws IOException {
        serverHandler = new ServerHandler();
        httpServer = new HttpServer(serverHandler); 
        httpServer.start();
        
        httpClient = new HttpClient();
        httpClient.setMaxActivePerServer(2);
        httpClient.setCallReturnOnMessage(true);
        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
        
        executorService = Executors.newCachedThreadPool();
    }
    
    
    @After
    public void tearDown() throws IOException {
        httpClient.close();
        executorService.shutdownNow();
        httpServer.close();
    }
    
  

    
    @Test
    public void testSlowResponseLoad() throws Exception {
                
        ServiceCall call1 = new ServiceCall(httpClient, "http://localhost:" + httpServer.getLocalPort() + "/?delay=1000");
        Future<Data> futureCallResponse1 = executorService.submit(call1);
        QAUtil.sleep(50);

        ServiceCall call2 = new ServiceCall(httpClient, "http://localhost:" + httpServer.getLocalPort() + "/?delay=1000");
        Future<Data> futureCallResponse2 = executorService.submit(call2);
        QAUtil.sleep(50);
        
        ServiceCall call3 = new ServiceCall(httpClient, "http://localhost:" + httpServer.getLocalPort() + "/?delay=1000");
        Future<Data> futureCallResponse3 = executorService.submit(call3);
        QAUtil.sleep(50);
        
        ServiceCall call4 = new ServiceCall(httpClient, "http://localhost:" + httpServer.getLocalPort() + "/?delay=1000");
        executorService.submit(call4);
        

        Assert.assertEquals("OK", futureCallResponse1.get(3000, TimeUnit.MILLISECONDS).getData());
        Assert.assertEquals("OK", futureCallResponse2.get(3000, TimeUnit.MILLISECONDS).getData());
        
        try {
            futureCallResponse3.get(3000, TimeUnit.MILLISECONDS).getData();
            Assert.fail("MaxConnectionsExceededException expected");
        } catch (ExecutionException expected) { 
            Assert.assertTrue(expected.getCause() instanceof MaxConnectionsExceededException);
        }

        
        Assert.assertEquals(2, serverHandler.getReceivedRequests());        
    }

    
    
    
    private static final class Data {
        
        private String s;
        
        public Data(String s) {
            this.s = s;
        }
        
        public String getData() {
            return s;
        }
    }
    
    
    
    
    
    
    private static final class ServiceCall implements Callable<Data> {   
        
        private final HttpClient httpCaller; 
        private final String uri;
        
        public ServiceCall(HttpClient httpCaller, String uri) {
            this.httpCaller = httpCaller;
            this.uri = uri;
        }
        
        public Data call() throws IOException {
            IHttpResponse response = httpCaller.call(new GetRequest(uri));
            if (response.getStatus() != 200) {
                throw new IOException("got " + response.getStatus() + " " + response.getReason());
            }
                
            return new Data(response.getBody().readString());
        }    
    }
    
    
    
    private static final class ServerHandler implements IHttpRequestHandler, ILifeCycle {

        private final AtomicInteger receivedRequests = new AtomicInteger(0);
        private Timer timer;

        public void onInit() {
            timer = new Timer();
        }
        
        public void onDestroy() throws IOException {
            timer.cancel();
        }
 

        public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
            receivedRequests.incrementAndGet();
            
            int delay = exchange.getRequest().getIntParameter("delay", 0);
                    
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    try {
                        exchange.send(new HttpResponse(200, "text/plain", "OK"));
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                    
                }
            };
            
            timer.schedule(task, delay);
        }
        
        public int getReceivedRequests() {
            return receivedRequests.get();
            
        }
    }
    
    
}