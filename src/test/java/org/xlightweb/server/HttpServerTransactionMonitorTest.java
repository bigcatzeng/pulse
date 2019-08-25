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
package org.xlightweb.server;


import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;


import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.BadMessageException;
import org.xlightweb.GetRequest;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpConnectHandler;
import org.xlightweb.IHttpConnection;
import org.xlightweb.IHttpDisconnectHandler;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.NameValuePair;
import org.xlightweb.PostRequest;
import org.xlightweb.QAUtil;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.ConnectionUtils;



/**
*
* @author grro@xlightweb.org
*/
public final class HttpServerTransactionMonitorTest {

	
	@Test
	public void testGET() throws Exception {
		
		Handler hdl = new Handler();  
		HttpServer server = new HttpServer(hdl);
		server.addConnectionHandler(hdl);
		server.setMaxConcurrentConnections(20000);
		server.setTransactionLogMaxSize(1000);
		server.start();

		ConnectionUtils.registerMBean(server);
		
		HttpClient httpClient = new HttpClient();

		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/s/"));
		Assert.assertEquals(200, response.getStatus());

		QAUtil.sleep(1000);

		
        Assert.assertEquals(0, (int) server.getTransactionsPending());
		Assert.assertEquals(1, server.getTransactionInfos().size());
		for (String info : server.getTransactionInfos()) {
		    System.out.println(info);
		    Assert.assertTrue(info.indexOf("(NO BODY) -> 200 OK ") != -1);
		}

	
		
		httpClient.close();
		server.close();
	}

	
	   @Test
	    public void testPOST() throws Exception {
	        
	        Handler hdl = new Handler();  
	        HttpServer server = new HttpServer(hdl);
	        server.addConnectionHandler(hdl);
	        server.setMaxConcurrentConnections(20000);
	        server.setTransactionLogMaxSize(1000);
	        server.start();

	        ConnectionUtils.registerMBean(server);
	        
	        HttpClient httpClient = new HttpClient();

	        IHttpResponse response = httpClient.call(new PostRequest("http://localhost:" + server.getLocalPort() + "/s/", new NameValuePair("param", "value"), new NameValuePair("param2", "value2")));
	        Assert.assertEquals(200, response.getStatus());

	        QAUtil.sleep(1000);

	        
	        Assert.assertEquals(0, (int) server.getTransactionsPending());
	        Assert.assertEquals(1, server.getTransactionInfos().size());
	        for (String info : server.getTransactionInfos()) {
	            System.out.println(info);
	            Assert.assertTrue(info.indexOf("-> 200 OK ") != -1);
	        }

	    
	        
	        httpClient.close();
	        server.close();
	    }


	
		
	public class Handler implements IHttpConnectHandler, IHttpRequestHandler, IHttpDisconnectHandler {

		private AtomicInteger countConnected = new AtomicInteger();
		private AtomicInteger countDisconnected = new AtomicInteger();
		
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			if (exchange.getRequest().getPathInfo().startsWith("/s/")) {
				exchange.send(new HttpResponse(200, "text/plain", "test"));
			} else {
				exchange.send(new HttpResponse(400));
			}
		}
		
		public boolean onConnect(IHttpConnection httpConnection) throws IOException {
			countConnected.incrementAndGet();
			return true;
		}
		
		public boolean onDisconnect(IHttpConnection httpConnection) throws IOException {
			countDisconnected.incrementAndGet();
			return true;
		}
		
		int getCountConnected() {
			return countConnected.get();
		}
		
		int getCountDisconnected() {
			return countDisconnected.get();
		}
	}
}
