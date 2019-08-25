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
public final class MultipartUploadTest {
	

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 1000; i++) {
		    System.out.print(".");
			new MultipartUploadTest().testClientMultiFile();
		}
	}
	
	
	
	@Test 
	public void testClientSingleFile() throws Exception {
		System.out.println("testClientSingleFile");

	    WebContainer server = new WebContainer(new TestServlet());
	    server.start();
	        
	    HttpClient httpClient = new HttpClient();
	        
	    
	    FutureResponseHandler respHdl = new FutureResponseHandler();
        HttpRequestHeader header = new HttpRequestHeader("POST",  "http://localhost:" + server.getLocalPort()+ "/", "multipart/form-data; boundary=7766");
        BodyDataSink dataSink = httpClient.send(header, respHdl);

	    File file = QAUtil.createTestfile_80byte();
	    Part part = new Part(new Header(), file);
	    part.setHeader("Content-disposition", "form-data; name=\"pics\"; filename=\"file1.txt\"");
        dataSink.writePart(part);
        
        dataSink.close();
        
        
	    IHttpResponse resp = respHdl.getResponse();
	    String body = resp.getBody().toString();
	    Assert.assertTrue(body.contains("name=pics"));
	    
	    Assert.assertEquals(200, resp.getStatus());
	       
	    file.delete();
	    httpClient.close();
	    server.stop();
	}
	
    
    @Test 
    public void testClientStringAndFile() throws Exception {
    	System.out.println("testClientStringAndFile");

    	IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();
            
        HttpClient httpClient = new HttpClient();

        
        
	    FutureResponseHandler respHdl = new FutureResponseHandler();
        HttpRequestHeader header = new HttpRequestHeader("POST",  "http://localhost:" + server.getLocalPort()+ "/", "multipart/form-data; boundary=7766");
        BodyDataSink dataSink = httpClient.send(header, respHdl);

        
        
        Header partHeader = new Header();
        partHeader.setHeader("Content-disposition", "form-data; name=\"text1\"");
        BodyDataSink partSink = dataSink.writePart(partHeader);
        
	    File file = QAUtil.createTestfile_80byte();
	    Part part2 = new Part(new Header(), file);
	    part2.setHeader("Content-disposition", "form-data; name=\"pics\"; filename=\"file1.txt\"");
        dataSink.writePart(part2);
    
        
        
        Part part3 = new Part(new Header(), "0123456789");
	    part3.setHeader("Content-disposition", "form-data; name=\"text2\"");
        dataSink.writePart(part3);
        
        
        partSink.write("ABCDEFG");
        partSink.close();

        
        dataSink.close();
        
        
	    IHttpResponse resp = respHdl.getResponse();
	    String body = resp.getBody().toString();
	    Assert.assertTrue(body.contains("Content-disposition: form-data; name=\"pics\""));
                   
        file.delete();
        httpClient.close();
        server.close();
    }
    
    
    @Test 
    public void testClientTwoStrings() throws Exception {
        System.out.println("testClientTwoStrings");

        IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                exchange.send(new HttpResponse(200, exchange.getRequest().getContentType(), exchange.getRequest().getNonBlockingBody()));
            }
        };
        HttpServer server = new HttpServer(reqHdl);
        server.start();
            
        HttpClient httpClient = new HttpClient();

        
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        HttpRequestHeader header = new HttpRequestHeader("POST",  "http://localhost:" + server.getLocalPort()+ "/", "multipart/form-data; boundary=7766");
        BodyDataSink dataSink = httpClient.send(header, respHdl);

        Header partHeader = new Header("text/plain");
        BodyDataSink partSink = dataSink.writePart(partHeader);
        partSink.write("ABCDEFG");
        partSink.close();
        
        Header partHeader2 = new Header("text/plain");
        BodyDataSink partSink2 = dataSink.writePart(partHeader2);
        partSink2.write("012345");
        partSink2.close();
        
        
        dataSink.close();
        
        
        IHttpResponse resp = respHdl.getResponse();
        String body = resp.getBody().toString();
                   
        httpClient.close();
        server.close();
    }    
        
    
    @Test 
    public void testClientMultiFileBatch() throws Exception {
        
        for (int i = 0; i < 10; i++) {
            System.out.println("run " + i);
            testClientMultiFile();
        }
    }
    
    
    @Test 
    public void testClientMultiFile() throws Exception {
    	System.out.println("testClientMultiFile");

        WebContainer server = new WebContainer(new TestServlet());
        server.start();
            
        HttpClient httpClient = new HttpClient();
        
        
	    FutureResponseHandler respHdl = new FutureResponseHandler();
        HttpRequestHeader header = new HttpRequestHeader("POST",  "http://localhost:" + server.getLocalPort()+ "/", "multipart/form-data; boundary=7766");
        BodyDataSink dataSink = httpClient.send(header, respHdl);

        
        File file1 = QAUtil.createTestfile_4k();
        File file2 = QAUtil.createTestfile_40k();
        
	    Part part = new Part(new Header(), file1);
	    part.setHeader("Content-disposition", "form-data; name=\"pics\"; filename=\"file1.txt\"");
        dataSink.writePart(part);
        
        Part part2 = new Part(new Header(), file2);
	    part2.setHeader("Content-disposition", "form-data; name=\"pics2\"; filename=\"file2.txt\"");
        dataSink.writePart(part2);
        
        dataSink.close();
        
        
	    IHttpResponse resp = respHdl.getResponse();
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
    public void testClientMultiFile4() throws Exception {
        System.out.println("testClientMultiFile4");
        
        WebContainer server = new WebContainer(new TestServlet());
        server.start();
            
        HttpClient httpClient = new HttpClient();
        

        MultipartFormDataRequest req = new MultipartFormDataRequest("http://localhost:" + server.getLocalPort()+ "/");
        
        String name1 = "file4k";
        File file1 = QAUtil.createTestfile_4k();
        String name2 = "file40k";
        File file2 = QAUtil.createTestfile_40k();
        
        req.addPart(name1, file1);
        req.addPart(name2, file2);
            
        IHttpResponse resp = httpClient.call(req);
        
        String body = resp.getBody().toString();
        Assert.assertTrue(body.contains("name=file4k"));
        Assert.assertTrue(body.contains("name=file40k"));
        
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
        
System.out.println("got repsonse reading part");        
        IPart p1 = resp.getBody().readPart();
        Assert.assertTrue(QAUtil.isEquals(file1, p1.getBody().readBytes()));
        
        IPart p2 = resp.getBody().readPart();
        Assert.assertEquals("0123456789", p2.getBody().readString());
        
        file1.delete();
        httpClient.close();
        server.close();
    }    

   

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
