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
public final class HttpClientInstableServerTest {

	
	@Test
	public void testSimple() throws Exception {
	    
	    HttpServer server = new HttpServer(new InstableRequestHandler());
	    server.start();
	    
	    
	    HttpClient httpClient = new HttpClient();
	    httpClient.setMaxRetries(0);
	    
	    httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?pauseMillis=500"), null);
        httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?pauseMillis=500"), null);
        httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?pauseMillis=500"), null);

        QAUtil.sleep(2000);
        Assert.assertEquals(3, httpClient.getNumCreated());
        Assert.assertEquals(3, httpClient.getNumIdle());
        Assert.assertEquals(0, httpClient.getNumActive());


        httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?destroy=true"), null);

        QAUtil.sleep(1000);
        Assert.assertEquals(3, httpClient.getNumCreated());
        Assert.assertEquals(2, httpClient.getNumIdle());
        
	    
	    httpClient.close();
	    server.close();
	}
	
	
	
	@Test
	public void testSeverDestroy() throws Exception {
	    
	    HttpServer server = new HttpServer(new InstableRequestHandler());
	    server.start();
	    
	    HttpClient httpClient = new HttpClient();
	    httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/?destroy=true"), null);

        QAUtil.sleep(1000);
        Assert.assertEquals(0, httpClient.getNumActive());
        Assert.assertEquals(0, httpClient.getNumIdle());
        
	    
	    httpClient.close();
	    server.close();
	}
	
	
	
	private static final class InstableRequestHandler implements IHttpRequestHandler {
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        
	        int pause = exchange.getRequest().getIntParameter("pauseMillis", 0);
	        boolean destroy = exchange.getRequest().getBooleanParameter("destroy", false);
	        
	        QAUtil.sleep(pause);
	        
	        if (destroy) {
	        	System.out.println("destroy connection");
	            exchange.destroy();
	        } else {
                exchange.send(new HttpResponse(200));
            }
	    }
	}
}
