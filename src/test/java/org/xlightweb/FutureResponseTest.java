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
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;




/**
*
* @author grro@xlightweb.org
*/
public final class FutureResponseTest  {


	@Test
	public void testHttpConnection() throws Exception {
	    HttpServer server = new HttpServer(new RequestHandler());
	    server.start();
	    
	    
	    HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
	    IFutureResponse futureResponse = con.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?pauseMillis=10"));

	    IHttpResponse response = futureResponse.getResponse();
	    
	    Assert.assertTrue(futureResponse.isDone());
	    Assert.assertFalse(futureResponse.isCancelled());
	    
	    Assert.assertEquals(200, response.getStatus());
	    
	    Assert.assertFalse(futureResponse.cancel(true));
	    Assert.assertFalse(futureResponse.isCancelled());
	    
	    con.close();
	    server.close();
	}



    @Test
    public void testHttpClient() throws Exception {
        HttpServer server = new HttpServer(new RequestHandler());
        server.start();
        
        
        HttpClient client = new HttpClient();
        IFutureResponse futureResponse = client.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?pauseMillis=10"));

        IHttpResponse response = futureResponse.getResponse();
        
        Assert.assertTrue(futureResponse.isDone());
        Assert.assertFalse(futureResponse.isCancelled());
        
        Assert.assertEquals(200, response.getStatus());
        
        Assert.assertFalse(futureResponse.cancel(true));
        Assert.assertFalse(futureResponse.isCancelled());
        
        client.close();
        server.close();
    }

	

    @Test
    public void testHttpConnectionTimeout() throws Exception {
        HttpServer server = new HttpServer(new RequestHandler());
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        IFutureResponse futureResponse = con.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?pauseMillis=500"));

        try {
            futureResponse.getResponse(100, TimeUnit.MILLISECONDS);
            Assert.fail("SocketTimeoutException expected");
        } catch (SocketTimeoutException expected) { }
        
        Assert.assertFalse(futureResponse.isCancelled());
        Assert.assertTrue(futureResponse.isDone());
        
        con.close();
        server.close();
    }


    @Test
    public void testHttpClientTimeout() throws Exception {
        HttpServer server = new HttpServer(new RequestHandler());
        server.start();
        
        
        HttpClient client = new HttpClient();
        IFutureResponse futureResponse = client.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?pauseMillis=50000"));

        try {
            futureResponse.getResponse(100, TimeUnit.MILLISECONDS);
            Assert.fail("SocketTimeoutException expected");
        } catch (SocketTimeoutException expected) { }
        
        Assert.assertFalse(futureResponse.isCancelled());
        Assert.assertTrue(futureResponse.isDone());
        
        client.close();
        server.close();
    }    
    
    
    @Test
    public void testHttpConnectionCancel() throws Exception {
        HttpServer server = new HttpServer(new RequestHandler());
        server.start();
        
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        IFutureResponse futureResponse = con.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?pauseMillis=500"));

        futureResponse.cancel(true);
        
        Assert.assertTrue(futureResponse.isCancelled());
        Assert.assertTrue(futureResponse.isDone());
        
        con.close();
        server.close();
    }
    

    @Test
    public void testHttpClientCancel() throws Exception {
        HttpServer server = new HttpServer(new RequestHandler());
        server.start();
        
        
        HttpClient client = new HttpClient();
        IFutureResponse futureResponse = client.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?pauseMillis=500"));

        futureResponse.cancel(true);
        
        Assert.assertTrue(futureResponse.isCancelled());
        Assert.assertTrue(futureResponse.isDone());
        
        client.close();
        server.close();
    }
    
	
	private static final class RequestHandler implements IHttpRequestHandler {
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        
	        int pause = exchange.getRequest().getIntParameter("pauseMillis", 0);
	        
	        QAUtil.sleep(pause);
	        
	        exchange.send(new HttpResponse(200));
	    }
	}
}