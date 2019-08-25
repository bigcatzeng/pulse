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
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;




/**
*
* @author grro@xlightweb.org
*/
public final class CloseOnSendingErrorTest {
	

	@Test 
	public void testSending4xxError() throws Exception {
		
		RequestHandler reqHdl = new RequestHandler(true, 400);
		HttpServer server = new HttpServer(reqHdl);
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		
		Assert.assertEquals(400, response.getStatus());
		Assert.assertNull(response.getHeader("Connection"));
		Assert.assertTrue(con.isPersistent());
		
		con.close();
		server.close();
	}
	
	
    @Test 
    public void testSending4xxError2() throws Exception {
        
        RequestHandler reqHdl = new RequestHandler(true, 400);
        HttpServer server = new HttpServer(reqHdl);
        server.setCloseOnSendingError(true);
        server.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        
        Assert.assertEquals(400, response.getStatus());
        Assert.assertEquals("close", response.getHeader("Connection"));
        
        QAUtil.sleep(500);
        Assert.assertFalse(con.isOpen());
    
        server.close();
    }	
	
	@Test 
    public void testSending5xxError() throws Exception {
        
        RequestHandler reqHdl = new RequestHandler(true, 500);
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
        
        Assert.assertEquals(500, response.getStatus());
        Assert.assertEquals("close", response.getHeader("Connection"));
        Assert.assertFalse(con.isPersistent());
        
        QAUtil.sleep(500);
        Assert.assertFalse(con.isOpen());
    
        server.close();
    }	

	@Test 
	public void testSendingPositive() throws Exception {
		
		RequestHandler reqHdl = new RequestHandler(false, 200);
		HttpServer server = new HttpServer(reqHdl);
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertTrue(con.isPersistent());
		
		QAUtil.sleep(500);
		Assert.assertTrue(con.isOpen());
	
		server.close();
	}

	
	private static final class RequestHandler implements IHttpRequestHandler {
		
		private boolean isSendError = false;
		private final int errorNum;
		
		
		public RequestHandler(boolean isSendError, int errorNum) {
			this.isSendError = isSendError;
			this.errorNum = errorNum;
		}
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			if (isSendError) {
				exchange.sendError(errorNum);
			} else { 
				exchange.send(new HttpResponse("OK"));
			}
		}
	}
}
