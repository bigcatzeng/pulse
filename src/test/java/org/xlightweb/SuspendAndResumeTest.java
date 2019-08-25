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
import java.util.concurrent.atomic.AtomicReference;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.BodyDataSink;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IConnection.FlushMode;






/**
*
* @author grro@xlightweb.org
*/
public final class SuspendAndResumeTest  {
	
	
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 1000; i++) {
            new SuspendAndResumeTest().testSimple();
        }
    }
    
	
	@Test
	public void testSimple() throws Exception {

	    RequestHandler srvHdl = new RequestHandler();
	    HttpServer server = new HttpServer(srvHdl); 
	    server.start();
	    
	    
	    HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
	    
	    FutureResponseHandler respHdl = new FutureResponseHandler();
	    BodyDataSink dataSink = con.send(new HttpRequestHeader("POST", "text/plain"), respHdl);
	    dataSink.setFlushmode(FlushMode.ASYNC);
	    dataSink.flush();
	    
	    IHttpExchange srvExchange = null;
	    do {
	        QAUtil.sleep(100);
	        srvExchange = srvHdl.getLastExchange();
	    } while(srvExchange == null);
	    
	    
	    IHttpRequest srvSideRequest = srvExchange.getRequest();
	    NonBlockingBodyDataSource srvSideDataSource = srvSideRequest.getNonBlockingBody();

	    Assert.assertEquals(0, srvSideDataSource.available());
	    
	    srvExchange.getConnection().suspendReceiving();
	    
	    StringBuilder sent = new StringBuilder();
	    for (int i = 0; i < 10; i++) {
	        String s = new String(QAUtil.generateByteArray(50000 + i));
	        sent.append(s);
	        
	        dataSink.write(s);
	        QAUtil.sleep(100);
	        
//	        System.out.println("Client: pending writeData: " + dataSink.getPendingWriteDataSize());
//	        System.out.println("Server: received data: " + srvSideDataSource.available());
	    }
	    
	    
//	    Assert.assertTrue(dataSink.getPendingWriteDataSize() > 10000);
//	    Assert.assertEquals(0, srvSideDataSource.available());
	    
	    srvExchange.getConnection().resumeReceiving();
	    dataSink.close();
	    
	    System.out.println("waiting for complete");
	    do {
	        QAUtil.sleep(100);
	    } while (!srvSideDataSource.isComplete());
	    
	    System.out.println("read received server data");
	    String reveived = srvSideDataSource.readStringByLength(srvSideDataSource.available());
	    
	    Assert.assertEquals(sent.toString(), reveived);

	    srvExchange.send(new HttpResponse(200));
	    
	    Assert.assertEquals(200, respHdl.getResponse().getStatus());
	    
	    con.close();
	    server.close();
	}
	
	
	private static final class RequestHandler implements IHttpRequestHandler {
	    
	    private final AtomicReference<IHttpExchange> lastExchangeRef = new AtomicReference<IHttpExchange>(null); 
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        lastExchangeRef.set(exchange);
	    }

	    
	    IHttpExchange getLastExchange() {
	        return lastExchangeRef.get();
	    }
	}
}
