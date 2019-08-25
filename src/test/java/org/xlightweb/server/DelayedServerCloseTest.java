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
package org.xlightweb.server;






import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

import org.junit.Test;
import org.xlightweb.AbstractHttpConnection;
import org.xlightweb.BadMessageException;
import org.xlightweb.BodyDataSink;
import org.xlightweb.FutureResponseHandler;
import org.xlightweb.HttpRequestHeader;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.QAUtil;
import org.xlightweb.server.HttpServer;
import org.xlightweb.server.HttpServerConnection;
import org.xlightweb.client.HttpClient;




/**
*
* @author grro@xlightweb.org
*/
public final class DelayedServerCloseTest  {


	@Test
	public void testCloseNow() throws Exception {
	    RequestHandler reqHdl = new RequestHandler();
	    HttpServer server = new HttpServer(reqHdl);
	    server.start();
	    
	    HttpClient httpClient = new HttpClient();
	    FutureResponseHandler respHdl = new FutureResponseHandler();
	    BodyDataSink ds = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/?isWaitForBody=true", "text/plain;charset=UTF-8"), respHdl);
	    ds.flush();
	    
	    QAUtil.sleep(1000);
	    ds.write("1234567");
	    ds.close();
	    
	    IHttpResponse response = respHdl.getResponse();
	    
	    Assert.assertEquals(200, response.getStatus());
	    
	    QAUtil.sleep(2000);
	    Assert.assertFalse(((HttpServerConnection) reqHdl.getLastCon()).isDelayedClosed());
	    
	    httpClient.close();
	    server.close();
	}

	@Test
    public void testCloseLater() throws Exception {
        
        RequestHandler reqHdl = new RequestHandler();
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        FutureResponseHandler respHdl = new FutureResponseHandler();
        BodyDataSink ds = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/", "text/plain;charset=UTF-8"), respHdl);
        ds.flush();
        
        QAUtil.sleep(1000);
        ds.write("1234567");
        ds.close();
        
        IHttpResponse response = respHdl.getResponse();
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(((HttpServerConnection) reqHdl.getLastCon()).isDelayedClosed());
        
        httpClient.close();
        server.close();
    }
	
	
    @Test
    public void testCloseLater2() throws Exception {
        
        RequestHandler reqHdl = new RequestHandler();
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        FutureResponseHandler respHdl = new FutureResponseHandler();
        BodyDataSink ds = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/", "text/plain;charset=UTF-8"), respHdl);
        ds.flush();
        
        QAUtil.sleep(1000);

        ds.close();
        
        IHttpResponse response = respHdl.getResponse();
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(((HttpServerConnection) reqHdl.getLastCon()).isDelayedClosed());
        
        httpClient.close();
        server.close();
    }	

	private static final class RequestHandler implements IHttpRequestHandler {
	    
	    private final AtomicReference<AbstractHttpConnection> lastConRef = new AtomicReference<AbstractHttpConnection>();
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        
	        lastConRef.set((AbstractHttpConnection) exchange.getConnection());
	        
	        IHttpRequest request = exchange.getRequest();
	        
	        if (request.getBooleanParameter("isWaitForBody", false)) { 
	            request.getBody().readString();
	        }	
	        
	        IHttpResponse response = new HttpResponse(200, "text/plain", "1234567890");
	        response.setHeader("Connection", "close");
            
	        exchange.send(response);
	    }
	    
	    
	    AbstractHttpConnection getLastCon() {
	        return lastConRef.get();
	    }
	}
	
	
}