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

import org.junit.Assert;

import org.junit.Test;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IServer;


/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientConnectionDuplexStreaming {


	@Test
	public void testLengthDefinedChunked() throws Exception {

        IServer server = new HttpServer(new EchoHandler());
        server.start();

        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());

        FutureResponseHandler hdl = new FutureResponseHandler();
        HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/");
        header.setContentType("text/plain; charset=UTF-8");


        int chunkSize = 100;
        int loops = 10;
        
        BodyDataSink bodyDataSink = con.send(header, chunkSize * loops, hdl);

        for (int i = 0; i < loops; i++) {
            bodyDataSink.write(QAUtil.generateByteBuffer(chunkSize));
        }
        
        bodyDataSink.close();
        
        IHttpResponse response = hdl.getResponse();
        
        byte[] result = response.getBody().readBytes();
        Assert.assertEquals(chunkSize * loops, result.length);
        
        server.close();
        con.close(); 
	}
	
	
    @Test
    public void testLengthDefinedChunkedBulk()  throws Exception {
        for (int i = 0; i < 100; i++) {
            testLengthDefinedChunked(); 
        }
    }
    
    

	
	

    @Test
    public void testChunkedChunked() throws Exception {

        IServer server = new HttpServer(new EchoHandler());
        server.start();

        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());

        FutureResponseHandler hdl = new FutureResponseHandler();
        HttpRequestHeader header = new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/");
        header.setContentType("text/plain; charset=UTF-8");


        int chunkSize = 100;
        int loops = 10;
        
        BodyDataSink bodyDataSink = con.send(header, hdl);

        for (int i = 0; i < loops; i++) {
            bodyDataSink.write(QAUtil.generateByteBuffer(chunkSize));
        }
        
        bodyDataSink.close();
        
        IHttpResponse response = hdl.getResponse();
        
        byte[] result = response.getBody().readBytes();
        Assert.assertEquals(chunkSize * loops, result.length);
        
        server.close();
        con.close(); 
    }
    
    

    @Test
    public void testtestChunkedChunkedBulk()  throws Exception {
        for (int i = 0; i < 100; i++) {
            testChunkedChunked(); 
        }
    }
    
    

    
	
	
	private static final class EchoHandler implements IHttpRequestHandler {
        
        public void onRequest(IHttpExchange exchange) throws IOException {

            IHttpRequest request = exchange.getRequest();
            NonBlockingBodyDataSource dSource = request.getNonBlockingBody();
            
            final BodyDataSink dSink = exchange.send(new HttpResponseHeader(200, request.getContentType()));
            dSink.flush();
            
            IBodyDataHandler dh = new IBodyDataHandler() {
                
                public boolean onData(NonBlockingBodyDataSource bodyDataSource)  {
                    try {
                        int available = bodyDataSource.available();
                        
                        if (available == -1) {
                            dSink.close();
                            
                        } else if (available > 0) {
                            dSink.write(bodyDataSource.readByteBufferByLength(available));
                        }
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                        dSink.destroy();
                        bodyDataSource.destroy();
                    }
                    return true;
                }
            };
            dSource.setDataHandler(dh);
            
        }
	}
}
