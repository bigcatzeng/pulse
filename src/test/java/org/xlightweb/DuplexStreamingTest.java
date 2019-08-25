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


import javax.net.ssl.SSLContext;



import org.junit.Assert;
import org.junit.Test;


import org.xlightweb.BodyDataSink;
import org.xlightweb.HttpRequestHeader;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.NonBlockingConnection;





/**
*
* @author grro@xlightweb.org
*/
public final class DuplexStreamingTest  {


	
	@Test
	public void testSimple() throws Exception {
	    
		IServer server = new HttpServer(new ContentEchoHandler());
		server.start();

		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
	
		    
		FutureResponseHandler hdl = new FutureResponseHandler();
		
		BodyDataSink sink = con.send(new HttpRequestHeader("POST", "/", "text/plain; charset=iso-8859-1"), hdl);
		sink.flush(); 
		
		BodyDataSource source = hdl.getResponse().getBody();
		
		sink.write("test\r\n");
		Assert.assertEquals("test", source.readStringByDelimiter("\r\n"));

		sink.write("123\r\n");
		Assert.assertEquals("123", source.readStringByDelimiter("\r\n"));

        sink.write("456\r\n");
        Assert.assertEquals("456", source.readStringByDelimiter("\r\n"));

		
		con.close();
		server.close();
	}

	
	
	@Test
    public void testHTTPClient() throws Exception {
        IServer server = new HttpServer(new ContentEchoHandler());
        server.start();

        HttpClient httpClient = new HttpClient();
        
            
        FutureResponseHandler hdl = new FutureResponseHandler();
        
        BodyDataSink sink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() +  "/", "text/plain; charset=iso-8859-1"), hdl);
        sink.flush();
        
        BodyDataSource source = hdl.getResponse().getBody();
        
        sink.write("test\r\n");
        Assert.assertEquals("test", source.readStringByDelimiter("\r\n"));

        sink.write("123\r\n");
        Assert.assertEquals("123", source.readStringByDelimiter("\r\n"));

        sink.write("456\r\n");
        Assert.assertEquals("456", source.readStringByDelimiter("\r\n"));

        
        httpClient.close();
        server.close();
    }
	
	
	@Test
    public void testSSL() throws Exception {
	    
	    
	    SSLContext sslContext = SSLTestContextFactory.getSSLContext();
	    
        IServer server = new HttpServer(0, new ContentEchoHandler(), sslContext, true);
        server.start();

        INonBlockingConnection tcpCon = new NonBlockingConnection("localhost", server.getLocalPort(), sslContext, true);
        HttpClientConnection con = new HttpClientConnection(tcpCon);
    
            
        FutureResponseHandler hdl = new FutureResponseHandler();
        
        BodyDataSink sink = con.send(new HttpRequestHeader("POST", "/", "text/plain; charset=iso-8859-1"), hdl);
        sink.flush();
        
        BodyDataSource source = hdl.getResponse().getBody();
        
        sink.write("test\r\n");
        Assert.assertEquals("test", source.readStringByDelimiter("\r\n"));

        sink.write("123\r\n");
        Assert.assertEquals("123", source.readStringByDelimiter("\r\n"));

        sink.write("456\r\n");
        Assert.assertEquals("456", source.readStringByDelimiter("\r\n"));

        
        con.close();
        server.close();
    }

	
	@Test
    public void testHttpClientSSL() throws Exception {
        
        
        SSLContext sslContext = SSLTestContextFactory.getSSLContext();
        
        IServer server = new HttpServer(0, new ContentEchoHandler(), sslContext, true);
        server.start();

        
        HttpClient httpClient = new HttpClient(sslContext);
            
        FutureResponseHandler hdl = new FutureResponseHandler();
        
        BodyDataSink sink = httpClient.send(new HttpRequestHeader("POST", "https://localhost:" + server.getLocalPort() +  "/", "text/plain; charset=iso-8859-1"), hdl);
        sink.flush();
        
        BodyDataSource source = hdl.getResponse().getBody();
        
        sink.write("test\r\n");
        Assert.assertEquals("test", source.readStringByDelimiter("\r\n"));

        sink.write("123\r\n");
        Assert.assertEquals("123", source.readStringByDelimiter("\r\n"));

        sink.write("456\r\n");
        Assert.assertEquals("456", source.readStringByDelimiter("\r\n"));

        
        httpClient.close();
        server.close();
    }
	
	
	
	private static final class ContentEchoHandler implements IHttpRequestHandler {

		public void onRequest(IHttpExchange exchange) throws IOException {

			IHttpRequest request = exchange.getRequest();
			
			BodyDataSink dataSink = exchange.send(new HttpResponseHeader(200, request.getContentType()));
			dataSink.flush();
			
			request.getNonBlockingBody().forwardTo(dataSink);
		}
		
	}
}
