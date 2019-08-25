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

import java.nio.ByteBuffer;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.DataConverter;
import org.xsocket.connection.IWriteCompletionHandler;



/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientEarlyResponseTest  {


      
    public static void main(String[] args) throws Exception {
        
        
        for (int i = 0; i < 10000; i++) {
            new HttpClientEarlyResponseTest().testCompletionHandler();
            System.out.print(".");
        }
    }



    @Test
    public void testCon() throws Exception {
        
        HttpServer server = new HttpServer(new RequestHandler());
        server.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());

        FutureResponseHandler hdl = new FutureResponseHandler();
        BodyDataSink ds = con.send(new HttpRequestHeader("POST", "http://localhost:"  +server.getLocalPort() + "/", "text/plain"), 200, hdl);
        for (int i = 0; i < 20; i++) {
            ds.write("0123456789");
            QAUtil.sleep(50);
        }
        ds.close();
            
        IHttpResponse response = hdl.get();
        Assert.assertEquals(200, response.getStatus());
        
        con.close();
        server.close();
    }
    
        
    

	@Test
	public void testSimple() throws Exception {
	   
	    HttpServer server = new HttpServer(new RequestHandler());
	    server.start();
	    
		HttpClient httpClient = new HttpClient();

		FutureResponseHandler hdl = new FutureResponseHandler();
		BodyDataSink ds = httpClient.send(new HttpRequestHeader("POST", "http://localhost:"  +server.getLocalPort() + "/", "text/plain"), 200, hdl);
		for (int i = 0; i < 20; i++) {
		    ds.write("0123456789");
		    QAUtil.sleep(50);
		}
		ds.close();
    		
		IHttpResponse response = hdl.get();
		Assert.assertEquals(200, response.getStatus());
		
		httpClient.close();
		server.close();
	}
	
	

    @Test
    public void testCompletionHandler() throws Exception {
        
        HttpServer server = new HttpServer(new RequestHandler());
        server.start();
        
        HttpClient httpClient = new HttpClient();

        FutureResponseHandler hdl = new FutureResponseHandler();
        BodyDataSink ds = httpClient.send(new HttpRequestHeader("POST", "http://localhost:"  +server.getLocalPort() + "/", "text/plain"), 200, hdl);
       
        
        try {
            for (int i = 0; i < 20; i++) {
                
                IWriteCompletionHandler ch = new IWriteCompletionHandler() {
                    public void onWritten(int written) throws IOException {
                        System.out.println("written " + written);
                    }
                    
                    public void onException(IOException ioe) {
                        ioe.printStackTrace();
                    }
                };
                
                ds.write(new ByteBuffer[] { DataConverter.toByteBuffer("0123456789", "UTF-8") }, ch);
                QAUtil.sleep(50);
            }
            ds.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
            
        IHttpResponse response = hdl.get();
        Assert.assertEquals(200, response.getStatus());
        
        httpClient.close();
        server.close();
    }	
	
	
	
	
	private static final class RequestHandler implements IHttpRequestHandler {
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {

	        HttpResponse response = new HttpResponse(200, "text/plain", "OK");
	        if (exchange.getRequest().getBooleanParameter("close", false)) {
	            response.setHeader("Connection", "close");
	        }
	        
	        exchange.send(response);
	    }
	}

}