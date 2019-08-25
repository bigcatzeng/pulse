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
import org.xlightweb.GetRequest;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;




/**
*
* @author grro@xlightweb.org
*/
public final class ClientConnectionOnMessageTest  {

 
    
    
    @Test
    public void testReturnOnHeader() throws Exception {

        RequestHandler hdl = new RequestHandler();
        HttpServer server = new HttpServer(hdl);
        server.start();

        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        
        long start = System.currentTimeMillis();
        
        
        ResponseHandler respHdl = new ResponseHandler();
        con.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/test?delay=2000"), respHdl);
        
        while (respHdl.getResponse() == null) {
            QAUtil.sleep(100);
        }
        
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("elapsed " + elapsed);

        Assert.assertTrue(elapsed < 2000);

        IHttpResponse response = respHdl.getResponse();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertFalse(response.getNonBlockingBody().isCompleteReceived());
        
        con.close();
        server.close();
    }
        
    
    @Test
    public void testConReturnOnMessage() throws Exception {

        RequestHandler hdl = new RequestHandler();
        HttpServer server = new HttpServer(hdl);
        server.start();

        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        
        long start = System.currentTimeMillis();
        
        
        OnMesageResponseHandler respHdl = new OnMesageResponseHandler();
        con.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/test?delay=2000"), respHdl);
        
        while (respHdl.getResponse() == null) {
            QAUtil.sleep(100);
        }
        
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("elapsed " + elapsed);

        Assert.assertTrue(elapsed >= 2000);

        IHttpResponse response = respHdl.getResponse();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.getNonBlockingBody().isCompleteReceived());
        
        con.close();
        server.close();
    }
	

    @Test
    public void testReturnOnMessage() throws Exception {

        RequestHandler hdl = new RequestHandler();
        HttpServer server = new HttpServer(hdl);
        server.start();

        HttpClient httpClient = new HttpClient();
        
        long start = System.currentTimeMillis();
        
        
        OnMesageResponseHandler respHdl = new OnMesageResponseHandler();
        httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/test?delay=2000"), respHdl);
        
        while (respHdl.getResponse() == null) {
            QAUtil.sleep(100);
        }
        
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("elapsed " + elapsed);

        Assert.assertTrue(elapsed >= 2000);

        IHttpResponse response = respHdl.getResponse();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.getNonBlockingBody().isCompleteReceived());
        
        httpClient.close();
        server.close();
    }
    

    
    @Test
    public void testConReturnOnMessageInterrupted() throws Exception {
        
        IDataHandler dh = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                connection.readStringByDelimiter("\r\n\r\n");

                connection.write("HTTP/1.1 200 OK\r\n" +
                                 "Server: me\r\n" + 
                                 "Content-length: 5\r\n" +
                                 "\r\n" +
                                 "123"); 
                
                QAUtil.sleep(200);
                connection.close();

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();


        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        OnMesageResponseHandler respHdl = new OnMesageResponseHandler();
        con.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/test?delay=2000"), respHdl);
        
        while (respHdl.getException() == null) {
            QAUtil.sleep(100);
        }
        
        con.close();
        server.close();
    }
        
    
    
    @Test
    public void testReturnOnMessageInterrupted() throws Exception {
        
        IDataHandler dh = new IDataHandler() {

            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                connection.readStringByDelimiter("\r\n\r\n");

                connection.write("HTTP/1.1 200 OK\r\n" +
                                 "Server: me\r\n" + 
                                 "Content-length: 5\r\n" +
                                 "\r\n" +
                                 "123"); 
                
                QAUtil.sleep(200);
                connection.close();

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();


        HttpClient httpClient = new HttpClient();
        
        OnMesageResponseHandler respHdl = new OnMesageResponseHandler();
        httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/test?delay=2000"), respHdl);
        
        while (respHdl.getException() == null) {
            QAUtil.sleep(100);
        }
        
        httpClient.close();
        server.close();
    }


    
    @Test
    public void testReturnOnMessageInterrupted2() throws Exception {
        
        IDataHandler dh = new IDataHandler() {

            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                connection.readStringByDelimiter("\r\n\r\n");

                connection.write("HTTP/1.1 200 OK\r\n" +
                                 "Server: me\r\n" + 
                                 "Content-length: 5\r\n" +
                                 "\r\n" +
                                 "123"); 
                
                QAUtil.sleep(200);
                connection.close();

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();


        HttpClient httpClient = new HttpClient();
        httpClient.setAutoHandleCookies(false);
        httpClient.setMaxRetries(0);
        
        OnMesageResponseHandler respHdl = new OnMesageResponseHandler();
        httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/test?delay=2000"), respHdl);
        
        while (respHdl.getException() == null) {
            QAUtil.sleep(100);
        }
        
        httpClient.close();
        server.close();
    }
    
    @Test
    public void testReturnOnHeaderInterrupted() throws Exception {
        
        
        IDataHandler dh = new IDataHandler() {

            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                connection.readStringByDelimiter("\r\n\r\n");

                connection.write("HTTP/1.1 200 OK\r\n" +
                                 "Server: me\r\n" + 
                                 "Content-"); 
                
                QAUtil.sleep(200);
                connection.close();

                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();


        HttpClient httpClient = new HttpClient();
        
        OnMesageResponseHandler respHdl = new OnMesageResponseHandler();
        httpClient.send(new GetRequest("http://localhost:" + server.getLocalPort() + "/test?delay=2000"), respHdl);
        
        while (respHdl.getException() == null) {
            QAUtil.sleep(100);
        }
        
        httpClient.close();
        server.close();
    }
            
    
    
    
    @InvokeOn(InvokeOn.HEADER_RECEIVED)
    @Execution(Execution.NONTHREADED)
    private static class ResponseHandler implements IHttpResponseHandler {
        
        private final AtomicReference<IHttpResponse> responseRef = new AtomicReference<IHttpResponse>(); 
        private final AtomicReference<IOException> exceptionRef = new AtomicReference<IOException>();
        
        public void onResponse(IHttpResponse response) throws IOException {
            responseRef.set(response);
        }
        
        
        public void onException(IOException ioe) throws IOException {
            exceptionRef.set(ioe);
        }
        
        
        public IHttpResponse getResponse() {
            return responseRef.get();
        }
        
        public IOException getException() {
            return exceptionRef.get();
        }
    }
	
    
    
    @InvokeOn(InvokeOn.MESSAGE_RECEIVED)
    private static final class OnMesageResponseHandler extends ResponseHandler {
        
    }        
    

    private static final class RequestHandler implements IHttpRequestHandler {
        
        
        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
            int delay = exchange.getRequest().getIntParameter("delay", 0);
            
            BodyDataSink ds = exchange.send(new HttpResponseHeader(200, "text/plain; charset=ISO-8859-1"), 2);
            ds.flush();
            QAUtil.sleep(delay);
            
            ds.write("OK");
            ds.close();
        }
    }	
}