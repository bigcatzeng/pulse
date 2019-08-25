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
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;



/**
*
* @author grro@xlightweb.org
*/
public final class SendHeaderTest {
    

    @Test
    public void testSimple() throws Exception {
        
        RequestHandler hdl = new RequestHandler();
        HttpServer server = new HttpServer(hdl);
        server.start();
       
        
        HttpClient httpClient = new HttpClient();
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        BodyDataSink ds = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/"), respHdl);

        QAUtil.sleep(1000);
        Assert.assertNull(hdl.getExchange());
        
        ds.flush();
        
        QAUtil.sleep(500);
        Assert.assertNotNull(hdl.getExchange());
        
        
        httpClient.close();
        server.close();
    }

    
    private static final class RequestHandler implements IHttpRequestHandler {
        
        
        private final AtomicReference<IHttpExchange> exchangeRef = new AtomicReference<IHttpExchange>();
        
        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
            exchangeRef.set(exchange);
        }
        
        
        IHttpExchange getExchange() {
            return exchangeRef.get();
        }
    }
}
