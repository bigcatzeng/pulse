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
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;

import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;





/**
*
* @author grro@xlightweb.org
*/
public final class MultipartRangeTest  {

	


	@Test
	public void testSimple() throws Exception {
		
		
		IDataHandler hdl = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection con) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
				con.readStringByDelimiter("\r\n\r\n");
				con.write("HTTP/1.1 206 Partial content\r\n" +
						  "Content-type: multipart/byteranges; boundary=THIS_STRING_SEPARATES\r\n" +
						  "Last-Modified: Tue, 14 Feb 2006 04:45:31 GMT\r\n" +
						  "Date: Wed, 24 Sep 2008 07:32:00 GMT\r\n" +
						  "Server: Server\r\n" +
						  "Connection: keep-alive\r\n" +
						  "\r\n" +
						  "--THIS_STRING_SEPARATES\r\n" +
						  "Content-Type: image/gif\r\n" +
						  "Content-Range: bytes 4000-4010/6532\r\n" +
						  "\r\n" +
						  "�P?H`���\r\n" +
						  "--THIS_STRING_SEPARATES\r\n" +
						  "Content-Type: image/gif\r\n" +
						  "Content-Range: bytes 4300-4400/6532\r\n" +
						  "\r\n" +
						  "��?��?��?:-6�p���?m@?P+�?+?-�/`ztP��z��i�+J?� ??�+t?+:?j?�?��6`?���B��7+?<�?�\r\n" +
						  "?wE+-7�G��5�\r\n" +
						  "\r\n" +
						  "--THIS_STRING_SEPARATES--\r\n");
				
				return true;
			}
		};
		
		IServer server = new Server(hdl);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		
		GetRequest req = new GetRequest("http://localhost:" + server.getLocalPort() +"/");
		req.setHeader("Range", "bytes=4000-4010, 4040-4050");
		
		IHttpResponse response = httpClient.call(req);
		
		String contentType = response.getContentType();
		String boundaryEntry = contentType.substring("multipart/byteranges;".length(), contentType.length()).trim();
		String boundary = boundaryEntry.substring("boundary=".length(), boundaryEntry.length()).trim();
		

		String body = response.getBody().readString();
		Assert.assertTrue(body.endsWith("--" + boundary + "--\r\n"));
		
		httpClient.close();
		server.close();
	}
	




    @Test
    public void testServerSideClose() throws Exception {
        
        
        IDataHandler hdl = new IDataHandler() {
            
            public boolean onData(INonBlockingConnection con) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                con.readStringByDelimiter("\r\n\r\n");
                con.write("HTTP/1.1 206 Partial content\r\n" +
                          "Content-type: multipart/byteranges; boundary=THIS_STRING_SEPARATES\r\n" +
                          "Last-Modified: Tue, 14 Feb 2006 04:45:31 GMT\r\n" +
                          "Date: Wed, 24 Sep 2008 07:32:00 GMT\r\n" +
                          "Connection: close\r\n" +
                          "Server: Server\r\n" +
                          "\r\n" +
                          "--THIS_STRING_SEPARATES\r\n" +
                          "Content-Type: image/gif\r\n" +
                          "Content-Range: bytes 4000-4010/6532\r\n" +
                          "\r\n" +
                          "�P?H`���\r\n" +
                          "--THIS_STRING_SEPARATES\r\n" +
                          "Content-Type: image/gif\r\n" +
                          "Content-Range: bytes 4300-4400/6532\r\n" +
                          "\r\n" +
                          "��?��?��?:-6�p���?m@?P+�?+?-�/`ztP��z��i�+J?� ??�+t?+:?j?�?��6`?���B��7+?<�?�\r\n" +
                          "?wE+-7�G��5�\r\n" +
                          "\r\n");
                con.close();
                
                return true;
            }
        };
        
        IServer server = new Server(hdl);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        GetRequest req = new GetRequest("http://localhost:" + server.getLocalPort() +"/");
        req.setHeader("Range", "bytes=4000-4010, 4040-4050");
        
        IHttpResponse response = httpClient.call(req);
        
        String contentType = response.getContentType();
        String boundaryEntry = contentType.substring("multipart/byteranges;".length(), contentType.length()).trim();
        boundaryEntry.substring("boundary=".length(), boundaryEntry.length()).trim();
        

        response.getBody().readString();
        
        httpClient.close();
        server.close();
    }
    
    

	
	
	@Test
	public void testChunked() throws Exception {
		
		
		IDataHandler hdl = new IDataHandler() {
			
			public boolean onData(INonBlockingConnection con) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
				con.readStringByDelimiter("\r\n\r\n");
				con.write("HTTP/1.1 206 Partial content\r\n" +
						  "Content-type: multipart/byteranges; boundary=THIS_STRING_SEPARATES\r\n" +
						  "Last-Modified: Tue, 14 Feb 2006 04:45:31 GMT\r\n" +
						  "Date: Wed, 24 Sep 2008 07:32:00 GMT\r\n" +
						  "Server: Server\r\n" +
						  "Connection: keep-alive\r\n" +
						  "\r\n" +
						  "--THIS_STRING_SEPARATES\r\n" +
						  "Content-Type: image/gif\r\n" +
						  "Content-Range: bytes 4000-4010/6532\r\n" +
						  "\r\n" +
						  "�P?H`���\r\n" +
						  "--THIS_STRING_SEPARATES\r\n" +
						  "Content-Type: image/gif\r\n" +
						  "Content-Range: bytes 4300-4400/6532\r\n" +
						  "\r\n" +
						  "��?��?��?:-6�p���?m@?P+�?+?-");
				
				QAUtil.sleep(500);
				
				con.write("�/`ztP��z��i�+J?� ??�+t?+:?j?�?��6`?���B��7+?<�?�\r\n" +
						  "?wE+-7�G��5�\r\n" +
						  "\r\n" +
						  "--THIS_STRING_SEPARATES--\r\n");
				
				return true;
			}
		};
		
		IServer server = new Server(hdl);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		
		GetRequest req = new GetRequest("http://localhost:" + server.getLocalPort() +"/");
		req.setHeader("Range", "bytes=4000-4010, 4040-4050");
		
		IHttpResponse response = httpClient.call(req);
		
		String contentType = response.getContentType();
		String boundaryEntry = contentType.substring("multipart/byteranges;".length(), contentType.length()).trim();
		String boundary = boundaryEntry.substring("boundary=".length(), boundaryEntry.length()).trim();
		

		String body = response.getBody().readString();
		Assert.assertTrue(body.endsWith("--" + boundary + "--\r\n"));
		
		httpClient.close();
		server.close();
	}
	
	
	
	
}
