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




import org.junit.Test;
import org.junit.Assert;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.WorkerPool;




/**
*
* @author grro@xlightweb.org
*/
public final class HttpServerWorkerPoolSizeTest  {


	@Test
	public void testCorePoolSize0() throws Exception {

		IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			    exchange.send(new HttpResponse(200));
			}
		};
		HttpServer server = new HttpServer(0, reqHdl, 0, 10);
		server.start();

		
		HttpClient httpClient = new HttpClient();

		for (int i = 0; i < 10; i++) {
    		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
    		Assert.assertEquals(200, response.getStatus());
    		
    		QAUtil.sleep(200);
	    }
		
		Assert.assertEquals(1, ((WorkerPool) server.getWorkerpool()).getLargestPoolSize());
		
		
		httpClient.close();
        server.close();
    }
	
	
    @Test
    public void testCorePoolSize3() throws Exception {

        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                exchange.send(new HttpResponse(200));
            }
        };
        HttpServer server = new HttpServer(0, reqHdl, 3, 10);
        server.start();

        
        HttpClient httpClient = new HttpClient();

        for (int i = 0; i < 10; i++) {
            IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
            Assert.assertEquals(200, response.getStatus());
            
            QAUtil.sleep(200);
        }
        
        Assert.assertEquals(3, ((WorkerPool) server.getWorkerpool()).getLargestPoolSize());
        
        
        httpClient.close();
        server.close();
    }   
	
}