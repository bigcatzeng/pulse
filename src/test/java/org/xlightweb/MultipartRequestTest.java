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

import java.io.ByteArrayOutputStream;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.junit.Assert;
import org.junit.Test;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;



/**
*
* @author grro@xlightweb.org
*/
public final class MultipartRequestTest {
	



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
        

        MultipartRequest req = new MultipartRequest("POST", "http://localhost:" + server.getLocalPort()+ "/test");
        
        File file1 = QAUtil.createTestfile_50byte();
        File file2 = QAUtil.createTestfile_80byte();
        
        Part part = new Part(new Header(), file1);
        req.addPart(part);
        
        Part part2 = new Part(new Header(), file2);
        req.addPart(part2);
        
            
        IHttpResponse resp = httpClient.call(req);
        Assert.assertEquals(200, resp.getStatus());
        
        String body = resp.getBody().readString();
        Assert.assertTrue(body.contains("<title>xsocket download</title>"));
        
        file1.delete();
        file2.delete();
        httpClient.close();
        server.close();
    }    
     
	
   

    @Test 
    public void testClientMultiFile2() throws Exception {
        System.out.println("testClientMultiFile2");

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
        req.addPart(part);
        
        Part part2 = new Part(new Header(), file2);
        req.addPart(part2);
        
            
        IHttpResponse resp = httpClient.call(req);
        Assert.assertEquals(200, resp.getStatus());
        
        String body = resp.getBody().readString();
        Assert.assertTrue(body.contains("<title>xsocket download</title>"));
        
        file1.delete();
        file2.delete();
        httpClient.close();
        server.close();
    }    
 
    
    @Test 
    public void testClientMultiFile3() throws Exception {
        System.out.println("testClientMultiFile3");
        
        WebContainer server = new WebContainer(new TestServlet());
        server.start();            
            
        HttpClient httpClient = new HttpClient();
        

        MultipartRequest req = new MultipartRequest("POST", "http://localhost:" + server.getLocalPort()+ "/test");
        
        String name1 = "file4k";
        File file1 = QAUtil.createTestfile_50byte();
        String name2 = "file40k";
        File file2 = QAUtil.createTestfile_80byte();
        

        Part part = new Part(new Header(), file1);
        part.setHeader("Content-disposition", "form-data; name=\"pics\"; filename=\"file1.txt\"");
        req.addPart(part);
        
        Part part2 = new Part(new Header(), file2);
        part2.setHeader("Content-disposition", "form-data; name=\"pics2\"; filename=\"file2.txt\"");
        req.addPart(part2);
        
        
        IHttpResponse resp = httpClient.call(req);
        String body = resp.getBody().toString();
        Assert.assertTrue(body.contains("name=pics"));
        Assert.assertTrue(body.contains("name=pics2"));
        
        Assert.assertEquals(200, resp.getStatus());
        

        
        file1.delete();
        file2.delete();
        httpClient.close();
        server.stop();
    }    

  

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

        String body = resp.getBody().readString();
        Assert.assertTrue(body.contains("<title>xsocket download</title>"));
        
        file1.delete();
        httpClient.close();
        server.close();
    }    
    
    @Test 
    public void testClientMultiFileAndStringStreaming() throws Exception {

        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
            }
        };
        HttpServer server = new HttpServer(reqHdl);
        server.start();
            
            
        HttpClient httpClient = new HttpClient();
        
        File file = QAUtil.createTestfile_40k();
        FileChannel fc = new RandomAccessFile(file, "r").getChannel();
        
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        BodyDataSink dataSink = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/"), respHdl);
        
        BodyDataSink partDataSink = dataSink.writePart(new HttpMessageHeader("text/plain"));
        partDataSink.write("12345678790");
        partDataSink.close();
        
        BodyDataSink partDataSink2 = dataSink.writePart(new HttpMessageHeader("text/html")); 
        partDataSink2.transferFrom(fc);
        fc.close();
        partDataSink2.close();
        
        dataSink.close();

        IHttpResponse resp = respHdl.getResponse();
        
        Assert.assertEquals(200, resp.getStatus());
        
        List<IPart> parts = resp.getBody().readParts();
        Assert.assertEquals("12345678790", parts.get(0).getBody().readString());
        
        Assert.assertTrue(QAUtil.isEquals(file, parts.get(1).getBody().readBytes()));

        
        file.delete();
        httpClient.close();
        server.close();
    }        

    
   

    @Test 
    public void testClientMultiFileAndStringWithInterceptor() throws Exception {

        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
            }
        };
        HttpServer server = new HttpServer(reqHdl);
        server.start();
            
        
        HttpClient httpClient = new HttpClient();
        httpClient.addInterceptor(new Interceptor());
        

        MultipartRequest req = new MultipartRequest("POST", "http://localhost:" + server.getLocalPort()+ "/test");
        
        File file1 = QAUtil.createTestfile_50byte();
        
        Part part = new Part(file1);
        req.addPart(part);
        
        Part part2 = new Part("text/plain", "0123456789");
        req.addPart(part2);
        
            
        IHttpResponse resp = httpClient.call(req);
        Assert.assertEquals(200, resp.getStatus());

        String body = resp.getBody().readString();
        Assert.assertTrue(body.contains("<title>xsocket download</title>"));
        
        file1.delete();
        httpClient.close();
        server.close();
    }    

    private static final class Interceptor implements IHttpRequestHandler {
        
        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
            IHttpRequest request = exchange.getRequest();
            
            String body = request.getBody().readString();
            
            HttpRequest newRequest = new HttpRequest(request.getRequestHeader(), body);
            exchange.forward(newRequest);
        }
    };
    

    private static final class TestServlet extends HttpServlet {
        
    	private static final long serialVersionUID = -5985963633008722396L;
    	
    	private CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(); 
        
           @SuppressWarnings("unchecked")
           @Override
           protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            
               StringBuilder sb = new StringBuilder();
            
               MultipartHttpServletRequest multipartRequest = multipartResolver.resolveMultipart(req);
            
               Map filemap = multipartRequest.getFileMap();
            
               for (Object name : filemap.keySet()) {
                
                   MultipartFile mpf = (MultipartFile) filemap.get(name);
                   String orgFilename = mpf.getOriginalFilename();
                   String contentType = mpf.getContentType();
                   InputStream is = mpf.getInputStream();
    
                   Content content = new Content((String) name, orgFilename, contentType, is);
                   is.close();
                   sb.append(content + "\r\n");
               }
            
            
               resp.getWriter().write(sb.toString());
           }
    }



   private static final class Content {
    
       private String content;
    
       public Content(String name, String filename, String contentType, InputStream is) throws IOException {
           StringBuilder sb = new StringBuilder();
           sb.append("name=" + name +"\r\n");
           sb.append("filename=" + filename +"\r\n");
           sb.append("contentType=" + contentType.split(";")[0].trim() + "\r\n");
           sb.append("content=");
        
           ByteArrayOutputStream bos = new ByteArrayOutputStream();
           
           byte[] buf = new byte[1024];
           int numRead=0;
           while((numRead = is.read(buf)) != -1){
        	   bos.write(buf, 0, numRead);
           }
           bos.close();
           
           sb.append(bos.toString("ISO-8859-1"));
        
           content = sb.toString();
       }
    
    
       @Override
       public String toString() {
           return content;
       }
   }
}
