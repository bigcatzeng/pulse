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

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;



import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.ConnectionUtils;

 

/**
*
* @author grro@xlightweb.org
*/
public final class HttpProxyLargeDataTest  {
  
    
    public static void main(String[] args) throws Exception {
        System.out.println(ConnectionUtils.getImplementationDate());
        
        for (int i = 0; i < 1000; i++) {
            new HttpProxyLargeDataTest().testSimple();
        }
    }
	
	@Test
	public void testNonProxy() throws Exception {
		
	    RequestHandler hdl = new RequestHandler();
		HttpServer server = new HttpServer(hdl);
		server.start();
		
		
		HttpClient httpClient = new HttpClient();
		
		FutureResponseHandler respHdl = new FutureResponseHandler();
		HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/test", "text/plain;charset=UTF-8");
		BodyDataSink bodyDataSink = httpClient.send(header, respHdl);
        bodyDataSink.write(QAUtil.generateByteArray(1000));
		
		IHttpExchange exchange = null;
		do {
		    QAUtil.sleep(100);
		    exchange = hdl.getLastExchange();
		} while (exchange == null);
		

		exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
		
		IHttpResponse response = respHdl.getResponse();
		Assert.assertEquals(200, response.getStatus());
		
		for (int i = 0; i < 999; i++) {
		    bodyDataSink.write(QAUtil.generateByteArray(1000));
		}

	//	QAUtil.sleep(4000);
	//	Assert.assertTrue(response.getNonBlockingBody().available() + " available", response.getNonBlockingBody().available() == (1000 * 1000));
        
		System.out.println("passed");

		
		httpClient.close();
		server.close();
	}


    @Test
    public void testSimple() throws Exception {
        
        RequestHandler hdl = new RequestHandler();
        HttpServer server = new HttpServer(hdl);
        server.start();
        
        HttpProxy proxy =  new HttpProxy(0, "localhost", server.getLocalPort(), true, Integer.MAX_VALUE, Integer.MAX_VALUE);
        proxy.start();

        
        HttpClient httpClient = new HttpClient();
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + proxy.getLocalPort() + "/test", "text/plain;charset=UTF-8");
        BodyDataSink bodyDataSink = httpClient.send(header, respHdl);
        bodyDataSink.write(QAUtil.generateByteArray(1000));
        
        IHttpExchange exchange = null;
        do {
            QAUtil.sleep(100);
            exchange = hdl.getLastExchange();
        } while (exchange == null);
        

        exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
        
        IHttpResponse response = respHdl.getResponse();
        Assert.assertEquals(200, response.getStatus());
        
        for (int i = 0; i < 999; i++) {
            bodyDataSink.write(QAUtil.generateByteArray(1000));
        }

    //  QAUtil.sleep(4000);
    //  Assert.assertTrue(response.getNonBlockingBody().available() + " available", response.getNonBlockingBody().available() == (1000 * 1000));
        
        System.out.println("passed");

        
        httpClient.close();
        proxy.close();
        server.close();
    }
	
	
	
	private static final class RequestHandler implements IHttpRequestHandler {
	    
	    private final AtomicReference<IHttpExchange> lastExchangeRef = new AtomicReference<IHttpExchange>();
	    
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        lastExchangeRef.set(exchange);
	    }
	    
	    public IHttpExchange getLastExchange() {
	        return lastExchangeRef.get();
	    }
	}
	
}
