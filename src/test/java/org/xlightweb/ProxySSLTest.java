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
import java.net.ConnectException;
import java.net.URL;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.xlightweb.BadMessageException;
import org.xlightweb.GetRequest;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xlightweb.server.IHttpServer;
import org.xsocket.ILifeCycle;



/**
*
* @author grro@xlightweb.org
*/
public final class ProxySSLTest  {

    private static IHttpServer proxyServer;
    private static IHttpServer proxySslServer;

    private static IHttpServer server;
    private static IHttpServer sslServer;

    
    
    @BeforeClass
    public static void setUp() throws Exception {
        IHttpRequestHandler hdl = new RequestHandler();
        
        proxyServer = new HttpServer(0, hdl);
        proxyServer.start();
        
        proxySslServer = new HttpServer(0, hdl, SSLTestContextFactory.getSSLContext(), true);
        proxySslServer.start();
        
        
        IHttpRequestHandler busiHdl = new BusinessRequestHandler();
        server = new HttpServer(0, busiHdl);
        server.start();
        
        sslServer = new HttpServer(0, busiHdl, SSLTestContextFactory.getSSLContext(), true);
        sslServer.start();

    }
	
    
    public static void tearDown() throws Exception {
        proxyServer.close();
        proxySslServer.close();
        
        server.close();
        sslServer.close();
    }
    
    
    @Test
    public void testSSL() throws Exception {
		HttpClient httpClient = new HttpClient(SSLTestContextFactory.getSSLContext());
		
		IHttpResponse response = httpClient.call(new GetRequest("https://localhost:" + proxySslServer.getLocalPort() + "/test"));
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("isSecured=true", response.getBody().readString());
		
		httpClient.close();
	}

    
    @Test
    public void testPlain() throws Exception {
        HttpClient httpClient = new HttpClient(SSLTestContextFactory.getSSLContext());
        
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + proxyServer.getLocalPort() + "/test"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("isSecured=false", response.getBody().readString());
        
        httpClient.close();
    }
	
	

	private static final class RequestHandler implements IHttpRequestHandler, ILifeCycle {
	    
	    private HttpClient httpClient;
	    
	    
	    public void onInit() {
	        httpClient = new HttpClient(SSLTestContextFactory.getSSLContext());
	        httpClient.setTreat302RedirectAs303(true);
	    }
	    
	    
	    public void onDestroy() throws IOException {
	        httpClient.close();
	    }
	    
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {

	        
	        IHttpRequest req = exchange.getRequest();
	        
	        int port;
	        if (req.isSecure()) {
	            port = sslServer.getLocalPort();
	        } else {
	            port = server.getLocalPort();
	        }
	        
	        URL url = req.getRequestUrl();
	        
	        req.setRequestUrl(new URL(url.getProtocol(), url.getHost(), port, url.getFile()));
	        
	        try {
	            httpClient.send(req, new ResponseHandler(exchange));
	        }catch (ConnectException ce) {
	            exchange.sendError(502, ce.getMessage());
	        }
	        
	        /*
	        >
	        > There's no easy way of telling whether the request is secure or not (this
	        > isn't the fault of xLightweb) and request.isSecure() always returns false,
	        > even for https URLs. I've tried to edit the code, removing the check,
	        > creating a HttpClient with an SSLContext directly and sending the request
	        > synchronously.
	        > Anyhow I do it, I don't get a response when I send the request, not even a
	        > exception. The browser just tells me:
	        >
	        > Proxy Server Refused Connection
	        > The connection was refused when attempting to contact the proxy server you
	        > have configured. Please check your proxy settings and try again.
	        >
	        > I've visited the mailing list. I saw a post relating to using HttpClient
	        > with SSL and you mentioned there was a bug you were working on. Is that
	        > bug also responsible for my own issue or am I doing something wrong?
	        >
	        > Thanks.
	        >
	        > Regards,*/
	        
	    }
	}
	
	
	private static final class ResponseHandler implements IHttpResponseHandler {
	    
	    private final IHttpExchange exchange;
	    
	    public ResponseHandler(IHttpExchange exchange) {
	        this.exchange = exchange;
        }
	    
	    public void onResponse(IHttpResponse response) throws IOException {
	        exchange.send(response);
	    }
	    
	    public void onException(IOException ioe) throws IOException {
	        exchange.sendError(ioe);
	    }
	}
	
	
	private static final class BusinessRequestHandler implements IHttpRequestHandler {
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        
	        boolean isSecured = exchange.getRequest().isSecure();
	        exchange.send(new HttpResponse(200, "text/plain", "isSecured=" + isSecured));
	    }
	}
	
}
