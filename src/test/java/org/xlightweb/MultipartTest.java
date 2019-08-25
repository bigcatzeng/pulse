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


import java.io.File;

import java.io.IOException;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;



/**
*
* @author grro@xlightweb.org
*/
public final class MultipartTest {
	

    @Test 
    public void testClientMultiFileAndString() throws Exception {
        System.out.println("testClientMultiFileAndString");

        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
            }
        };
        HttpServer server = new HttpServer(reqHdl);
        server.start();
            
            
        HttpClient httpClient = new HttpClient();
        

        MultipartRequest req = new MultipartRequest("POST", "http://localhost:" + server.getLocalPort()+ "/test");
        
        File file1 = QAUtil.createTestfile_50byte();
        
        Part part = new Part(file1);
        req.addPart(part);
        
        Part part2 = new Part("text/plain", "0123456789");
        req.addPart(part2);
        
        
            
        IHttpResponse resp = httpClient.call(req);
        Assert.assertEquals(200, resp.getStatus());
        
        IPart p1 = resp.getBody().readPart();
        Assert.assertTrue(QAUtil.isEquals(file1, p1.getBody().readBytes()));
        
        IPart p2 = resp.getBody().readPart();
        Assert.assertEquals("0123456789", p2.getBody().readString());
        
        file1.delete();
        httpClient.close();
        server.close();
    }
    
    

    @Ignore
    @Test 
    public void testClientMultiFileAndString2() throws Exception {
        System.out.println("testClientMultiFileAndString2");
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
            }
        };
        HttpServer server = new HttpServer(reqHdl);
        server.start();
            
            
        HttpClient httpClient = new HttpClient();
        

        MultipartRequest req = new MultipartRequest("POST", "http://localhost:" + server.getLocalPort()+ "/test");
        
        File file1 = QAUtil.createTestfile_50byte();
        
        Part part = new Part(file1);
        req.addPart(part);
        
        Part part2 = new Part("text/plain", "0123456789");
        req.addPart(part2);
        
            
        IHttpResponse resp = httpClient.call(req);
        Assert.assertEquals(200, resp.getStatus());
        
        IPart p1 = resp.getBody().readPart();
        Assert.assertTrue(QAUtil.isEquals(file1, p1.getBody().readBytes()));
        
        IPart p2 = resp.getBody().readPart();
        Assert.assertEquals("0123456789", p2.getBody().readString());
        
        file1.delete();
        httpClient.close();
        server.close();
    }
        
    
    @Test 
    public void testClientMultiString() throws Exception {
        System.out.println("testClientMultiString");
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
            }
        };
        HttpServer server = new HttpServer(reqHdl);
        server.start();
            
            
        HttpClient httpClient = new HttpClient();
        

        MultipartRequest req = new MultipartRequest("POST", "http://localhost:" + server.getLocalPort()+ "/test");
        
        Part part = new Part("text/html", "<html><html>");
        req.addPart(part);
        
        Part part2 = new Part("text/plain", "0123456789");
        req.addPart(part2);
        
            
        IHttpResponse resp = httpClient.call(req);
        Assert.assertEquals(200, resp.getStatus());
        
        IPart p1 = resp.getBody().readPart();
        Assert.assertEquals("<html><html>", p1.getBody().readString());
        
        IPart p2 = resp.getBody().readPart();
        Assert.assertEquals("0123456789", p2.getBody().readString());
        
        httpClient.close();
        server.close();
    }        
    
    
    @Ignore
    @Test 
    public void testClientMultiString2() throws Exception {
        System.out.println("testClientMultiString2");

        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
            }
        };
        HttpServer server = new HttpServer(reqHdl);
        server.start();
            
            
        HttpClient httpClient = new HttpClient();
        

        MultipartRequest req = new MultipartRequest("POST", "http://localhost:" + server.getLocalPort()+ "/test");
        
        File file1 = QAUtil.createTestfile_50byte();
        
        Part part = new Part("text/html", "<html><html>");
        req.addPart(part);
        
        Part part2 = new Part("text/plain", "0123456789");
        req.addPart(part2);
        
            
        IHttpResponse resp = httpClient.call(req);
        Assert.assertEquals(200, resp.getStatus());
        
        IPart p1 = resp.getBody().readPart();
        Assert.assertTrue(QAUtil.isEquals(file1, p1.getBody().readBytes()));
        
        IPart p2 = resp.getBody().readPart();
        Assert.assertEquals("0123456789", p2.getBody().readString());
        
        file1.delete();
        httpClient.close();
        server.close();
    }            
}
