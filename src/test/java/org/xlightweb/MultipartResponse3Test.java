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
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;




/**
*
* @author grro@xlightweb.org
*/
public final class MultipartResponse3Test {
	


    
    
    @Test 
    public void testClientMultiFile() throws Exception {

        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
            }
        };
        HttpServer server = new HttpServer(reqHdl);
        server.start();
            
            
        HttpClient httpClient = new HttpClient();
        
        String body = "--36340eb4-950c-43ce-9da5-f8ccc93d2a82\r\n" +
                      "Content-Type: text/html\r\n" +
                      "\r\n" +
                      "<head>\r\n" +
                      "<title>xsocket download</title>\r\n" +
                      "</head>\r\n" +
                      "\r\n" +
                      "--36340eb4-950c-43ce-9da5-f8ccc93d2a82\r\n" +
                      "Content-Type: text/html\r\n" +
                      "\r\n" +
                      "<head>\r\n" +
                      "<title>xsocket download</title>\r\n" +
                      "<style type=\"text/css\">\r\n" +
                      "</style>\r\n" +
                      "</head>\r\n" +
                      "\r\n" +
                      "--36340eb4-950c-43ce-9da5-f8ccc93d2a82--\r\n";
        
        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/", "multipart/mixed; boundary=36340eb4-950c-43ce-9da5-f8ccc93d2a82", body);
        
            
        IHttpResponse resp = httpClient.call(request);
        Assert.assertEquals(200, resp.getStatus());
        
        IPart p1 = resp.getBody().readPart();
        String b = p1.getBody().readString();
        Assert.assertEquals(b, "<head>\r\n" +
                            "<title>xsocket download</title>\r\n" +
                            "</head>\r\n", b); 
        
        IPart p2 = resp.getBody().readPart();
        b = p2.getBody().readString();
        Assert.assertEquals("got '" + b + "'", "<head>\r\n" +
                            "<title>xsocket download</title>\r\n" +
                            "<style type=\"text/css\">\r\n" +
                            "</style>\r\n" +
                            "</head>\r\n", b); 
        
        httpClient.close();
        server.close();
    }    
    
    

    @Test 
    public void testClientMultiFileChunked() throws Exception {
        
        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
            }
        };
        HttpServer server = new HttpServer(reqHdl);
        server.start();
            
            
        HttpClient httpClient = new HttpClient();
        
        String body = "--36340eb4-950c-43ce-9da5-f8ccc93d2a82\r\n" +
                      "Content-Type: text/html\r\n" +
                      "\r\n" +
                      "<head>\r\n" +
                      "<title>xsocket download</title>\r\n" +
                      "</head>\r\n" +
                      "\r\n" +
                      "--36340eb4-950c-43ce-9da5-f8ccc93d2a82\r\n" +
                      "Content-Type: text/html\r\n" +
                      "\r\n" +
                      "<head>\r\n" +
                      "<title>xsocket download</title>\r\n" +
                      "<style type=\"text/css\">\r\n" +
                      "</style>\r\n" +
                      "</head>\r\n" +
                      "\r\n" +
                      "--36340eb4-950c-43ce-9da5-f8ccc93d2a82--\r\n";
        
        PostRequest request = new PostRequest("http://localhost:" + server.getLocalPort() + "/", "multipart/mixed; boundary=36340eb4-950c-43ce-9da5-f8ccc93d2a82", body);
        request.setHeader("Transfer-Encoding", "chunked");
        request.removeHeader("Content-Length");
            
        IHttpResponse resp = httpClient.call(request);
        Assert.assertEquals(200, resp.getStatus());
        
        IPart p1 = resp.getBody().readPart();
        String b = p1.getBody().readString();
        Assert.assertEquals(b, "<head>\r\n" +
                            "<title>xsocket download</title>\r\n" +
                            "</head>\r\n", b); 
        
        IPart p2 = resp.getBody().readPart();
        b = p2.getBody().readString();
        Assert.assertEquals("got '" + b + "'", "<head>\r\n" +
                            "<title>xsocket download</title>\r\n" +
                            "<style type=\"text/css\">\r\n" +
                            "</style>\r\n" +
                            "</head>\r\n", b); 
        
        httpClient.close();
        server.close();
    }

    
    @Test 
    public void testClientMultiFileRequestResponseBatch() throws Exception {
        for (int i = 0; i < 50; i++) {
            testClientMultiFileRequestResponse();
        }
    }

    
    
    @Test 
    public void testClientMultiFileRequestResponse() throws Exception {

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
        File file2 = QAUtil.createTestfile_80byte();
        
        Part part = new Part(new Header(), file1);
        part.setHeader("part", "1");
        req.addPart(part);
        
        Part part2 = new Part(new Header(), file2);
        part2.setHeader("part", "2");
        req.addPart(part2);
        
            
        IHttpResponse resp = httpClient.call(req);
        Assert.assertEquals(200, resp.getStatus());
        
        IPart p1 = resp.getBody().readPart();
        if (p1.getHeader("part").equals("1")) {
            Assert.assertTrue(QAUtil.isEquals(file1, p1.getBody().readBytes()));
        } else {
            Assert.assertTrue(QAUtil.isEquals(file2, p1.getBody().readBytes()));
        }

        IPart p2 = resp.getBody().readPart();
        if (p2.getHeader("part").equals("1")) {
            Assert.assertTrue(QAUtil.isEquals(file1, p2.getBody().readBytes()));
        } else {
            Assert.assertTrue(QAUtil.isEquals(file2, p2.getBody().readBytes()));
        }
        
        file1.delete();
        file2.delete();
        httpClient.close();
        server.close();
    }    
 
}
