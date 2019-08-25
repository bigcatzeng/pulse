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

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IConnection;
import org.xsocket.connection.IConnection.FlushMode;



/**
*
* @author grro@xlightweb.org
*/
public final class MaxBuffersizeForwardingTest  {


    @Ignore
	@Test
	public void testSimple() throws Exception {
	   
	    RequestHandler reqHdl = new RequestHandler();
	    HttpServer server = new HttpServer(reqHdl);
	    server.start();
	    
	    
	    HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
	    con.setOption(IConnection.SO_RCVBUF, 64);
	    
	    FutureResponseHandler rh = new FutureResponseHandler();
	    BodyDataSink clientDataSink = con.send(new HttpRequestHeader("POST", "text/plain"), rh);
	    clientDataSink.setFlushmode(FlushMode.ASYNC);
	    
	    clientDataSink.write(QAUtil.generateByteArray(300));
	    QAUtil.sleep(500);
	    
	    clientDataSink.write(QAUtil.generateByteArray(400));
	    
	    NonBlockingBodyDataSource serverDataSource = rh.getResponse().getNonBlockingBody();

	    con.suspendReceiving();
	    
	    for (int i = 0; i < 10; i++) {
	        clientDataSink.write(QAUtil.generateByteArray(40000));
	        QAUtil.sleep(100);
	        
	        System.out.println("datSink pending data: " + clientDataSink.getPendingWriteDataSize());
	    }

	    
	    Assert.assertTrue(clientDataSink.getPendingWriteDataSize() > 100000);
	    Assert.assertTrue(serverDataSource.available() < 400000);

	    con.resumeReceiving();
	    QAUtil.sleep(3000);
	    
	    Assert.assertTrue(serverDataSource.available() > 400000);

	    con.close();
	    server.close();
	}
	
	
	private static final class RequestHandler implements IHttpRequestHandler {
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        
	        IHttpRequest request = exchange.getRequest();
	        exchange.send(new HttpResponse(200, request.getContentType(), request.getNonBlockingBody()));
	    }
	}
}

