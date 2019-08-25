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

import junit.framework.Assert;

import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.ConnectionUtils;

/**
*
* @author grro@xlightweb.org
*/
public final class TimeMeasureTest {
	

	@Test 
	public void testSimple() throws Exception {
		
		RequestHandlerChain chain = new RequestHandlerChain();
		LogHandler serverLogHandler = new LogHandler("[server]");
        chain.addLast(serverLogHandler);
		chain.addLast(new RequestHandler());

		HttpServer server = new HttpServer(chain);
		server.start();
	
		

		HttpClient httpClient = new HttpClient();
		ConnectionUtils.registerMBean(httpClient);
		
		LogHandler clientLogHandler = new LogHandler("[client]");
		httpClient.addInterceptor(clientLogHandler);
		
		IHttpResponse resp = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		Assert.assertEquals(200, resp.getStatus());
		Assert.assertTrue(clientLogHandler.getElapsedNanoLastCall() > serverLogHandler.getElapsedNanoLastCall());
		
		httpClient.close();
		server.close();
	}


	
	private static final class LogHandler implements IHttpRequestHandler {
	    
	    private final String prefix;
	    private long elapsedNanoLastCall;
	    
	    public LogHandler(String prefix) {
	        this.prefix = prefix;
        }
	    
	    public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
	    
	        final long start = System.nanoTime();

	        IHttpRequest req = exchange.getRequest();
	        final String txt = req.getMethod() + " " + req.getRequestURI();
	        
	        
	        IHttpResponseHandler respHdl = new IHttpResponseHandler() {
	          
	            public void onResponse(IHttpResponse response) throws IOException {
	                elapsedNanoLastCall = System.nanoTime() - start;
	                
	                System.out.println(prefix + " " + txt + " -> " + response.getStatus() + "  elpased nano=" + elapsedNanoLastCall);
	                exchange.send(response);
	            }
	            
	            public void onException(IOException ioe) throws IOException {
	                elapsedNanoLastCall = System.nanoTime() - start;

	                System.out.println(prefix + " " + txt + " -> error  elpased namo=" + elapsedNanoLastCall);
                    exchange.sendError(ioe);
	            }
	            
	        };
	        
	        exchange.forward(req, respHdl);
	    }	    
	    
	    
	    long getElapsedNanoLastCall() {
	        return elapsedNanoLastCall;
	    }
	}
	

	

	private static final class RequestHandler extends HttpRequestHandler {
		
		@Override 
		public void doGet(IHttpExchange exchange) throws IOException, BadMessageException {
			exchange.send(new HttpResponse(200, "text/plain", "GET called"));
		}
	}
}
