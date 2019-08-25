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
import java.io.FileWriter;
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



/**
*
* @author grro@xlightweb.org
*/
public final class MultipartFormDataRequestTest {
	

    
    @Test 
    public void testClientMultiFile() throws Exception {
        
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
    public void testClientMultiFile2() throws Exception {

        WebContainer server = new WebContainer(new TestServlet());
        server.start();
            
        HttpClient httpClient = new HttpClient();
        

        MultipartFormDataRequest req = new MultipartFormDataRequest("http://localhost:" + server.getLocalPort()+ "/");
        
        String name1 = "file4k";
        File file1 = File.createTempFile("test", "tmp");
        FileWriter fw = new FileWriter(file1);
        fw.write("0123456789");
        fw.close();
        
        
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
