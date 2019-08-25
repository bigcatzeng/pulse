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
import java.util.concurrent.atomic.AtomicReference;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.server.HttpServer;
import org.xlightweb.server.IHttpServer;
import org.xsocket.Execution;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;



/**
*
* @author grro@xlightweb.org
*/
public final class HttpServerConnectionTest  {


    @Test
    public void testNonThreaded() throws Exception {

        NonThreadedRequestHandler requestHdl = new NonThreadedRequestHandler();
        IHttpServer server = new HttpServer(requestHdl);
        server.start();
      
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("GET /test HTTP/1.1\r\n" +
                  "User-Agent: me\r\n" +
                  "Host: localhost:" + server.getLocalPort() + "\r\n\r\n");
        
        String header = con.readStringByDelimiter("\r\n\r\n");
        Assert.assertTrue(header.indexOf("200") != -1);
        
        Assert.assertFalse(requestHdl.threadnameRef.get().startsWith("xWorker"));
                
        con.close();
        server.close();
    }    
    
    
    @Test
    public void testMultiThreaded() throws Exception {

        MultiThreadedRequestHandler requestHdl = new MultiThreadedRequestHandler();
        IHttpServer server = new HttpServer(requestHdl);
        server.start();
      
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("GET /test HTTP/1.1\r\n" +
                  "User-Agent: me\r\n" +
                  "Host: localhost:" + server.getLocalPort() + "\r\n\r\n");
        
        String header = con.readStringByDelimiter("\r\n\r\n");
        Assert.assertTrue(header.indexOf("200") != -1);
              
        Assert.assertTrue(requestHdl.threadnameRef.get().startsWith("xWorker"));
        
        con.close();
        server.close();
    }    
    
    
    

    @Test
    public void testPiplining() throws Exception {

        IHttpServer server = new HttpServer(new SlowRequestHandler());
        server.start();

        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        
        int sleepTime = 1000;
        for (int i = 0; i < 10; i++) {
            con.write("GET /test?num=" + i + "&wait=" + sleepTime + " HTTP/1.1\r\n" +
                      "User-Agent: me\r\n" +
                      "Host: localhost:" + server.getLocalPort() + "\r\n\r\n");
            
            sleepTime = sleepTime / 2;
        }

        
        for (int i = 0; i < 10; i++) {
            String header = con.readStringByDelimiter("\r\n\r\n");
            Assert.assertTrue(header.indexOf("200") != -1);
            // content length is 1
            int num = Integer.parseInt(con.readStringByLength(1));
            Assert.assertEquals(i, num);
        }
        
        con.close();
        server.close();
    }    
    
    
    
    private static class RequestHandler implements IHttpRequestHandler { 

        AtomicReference<IHttpRequest> requestRef = new AtomicReference<IHttpRequest>();
        AtomicReference<String> threadnameRef = new AtomicReference<String>();
        
        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
            requestRef.set(exchange.getRequest());
            threadnameRef.set(Thread.currentThread().getName());
            
            exchange.send(new HttpResponse(200));
        }
    }
    
    
    @Execution(Execution.NONTHREADED)
    private static final class NonThreadedRequestHandler extends RequestHandler {
        
    }
    
    @Execution(Execution.MULTITHREADED)
    private static final class MultiThreadedRequestHandler extends RequestHandler {
        
    }

 
    @Execution(Execution.MULTITHREADED)
    private static final class SlowRequestHandler implements IHttpRequestHandler {
     
        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
            int sleeptime = exchange.getRequest().getIntParameter("wait");
            QAUtil.sleep(sleeptime);
            
            exchange.send(new HttpResponse(200, exchange.getRequest().getParameter("num")));
        }
    }
}