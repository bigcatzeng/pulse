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
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xsocket.Execution;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;




/**
*
* @author grro@xlightweb.org
*/
public final class ConnectErrorTest  {



	@Test
	public void testConnectError() throws Exception {
		HttpClient httpClient = new HttpClient();
		
		HttpRequestHandler interceptor = new HttpRequestHandler();
		httpClient.addInterceptor(interceptor);
		
		try {
		    IHttpResponse response = httpClient.call(new GetRequest("http://notexits:77663/test"));
		    Assert.fail("IOException expected");
		} catch (IOException expected) { }

		Assert.assertTrue(interceptor.isCalled());
		
	}
	
	
	@Execution(Execution.NONTHREADED)
	private static final class HttpRequestHandler implements IHttpRequestHandler {
	    
	    private final AtomicBoolean isCalled = new AtomicBoolean();
	    
	    public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
	     
	        IHttpResponseHandler respHdl = new IHttpResponseHandler() {
                
                public void onResponse(IHttpResponse response) throws IOException {
                    isCalled.set(true);
                    exchange.send(response);
                }
                
                public void onException(IOException ioe) throws IOException {
                    isCalled.set(true);
                    exchange.sendError(ioe);
                }
            }; 
            
            exchange.forward(exchange.getRequest(), respHdl);
	    }
	    
	    boolean isCalled() {
	        return isCalled.get();
	    }
	}
}