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



import java.io.File;



import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.RequestHandlerChain;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.DataConverter;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IConnection;
import org.xsocket.connection.IServer;



/**
*
* @author grro@xlightweb.org
*/
public final class FileServiceTest  {

	 
	@Test
	public void testRange() throws Exception {
    	File file = QAUtil.createTestfile_75byte();
    	String basepath = file.getParentFile().getAbsolutePath();
		
		IServer server = new HttpServer(new FileServiceRequestHandler(basepath));
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		

		IHttpRequest request = new GetRequest("/" + file.getName());
		request.setHeader("Range", "bytes=70-");
		
		IHttpResponse response = con.call(request);
		
		Assert.assertEquals(206, response.getStatus());
		Assert.assertEquals("head>", response.getBody().readString());
		
		file.delete();
		con.close();
		server.close();
	}
	
	@Test
	public void testRange2() throws Exception {
    	File file = QAUtil.createTestfile_75byte();
    	String basepath = file.getParentFile().getAbsolutePath();
		
		IServer server = new HttpServer(new FileServiceRequestHandler(basepath));
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		

		IHttpRequest request = new GetRequest("/" + file.getName());
		request.setHeader("Range", "bytes=74-74");
		
		IHttpResponse response = con.call(request);
		
		Assert.assertEquals(206, response.getStatus());
		Assert.assertEquals(">", response.getBody().readString());
		
		file.delete();
		con.close();
		server.close();
	}
	
	@Test
	public void testRange3() throws Exception {
    	File file = QAUtil.createTestfile_75byte();
    	String basepath = file.getParentFile().getAbsolutePath();
		
		IServer server = new HttpServer(new FileServiceRequestHandler(basepath));
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		

		IHttpRequest request = new GetRequest("/" + file.getName());
		request.setHeader("Range", "bytes=0-0");
		
		IHttpResponse response = con.call(request);
		
		Assert.assertEquals(206, response.getStatus());
		Assert.assertEquals("<", response.getBody().readString());
		
		file.delete();
		con.close();
		server.close();
	}

	
	@Test
	public void testRange4() throws Exception {
    	File file = QAUtil.createTestfile_75byte();
    	String basepath = file.getParentFile().getAbsolutePath();
		
		IServer server = new HttpServer(new FileServiceRequestHandler(basepath));
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		

		IHttpRequest request = new GetRequest("/" + file.getName());
		request.setHeader("Range", "bytes=-10");
		
		IHttpResponse response = con.call(request);
		
		Assert.assertEquals(206, response.getStatus());
		Assert.assertEquals("le></head>", response.getBody().readString());
		
		file.delete();
		con.close();
		server.close();
	}	

	
	@Test
	public void testRange5() throws Exception {
    	File file = QAUtil.createTestfile_75byte();
    	String basepath = file.getParentFile().getAbsolutePath();
		
		IServer server = new HttpServer(new FileServiceRequestHandler(basepath));
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		

		IHttpRequest request = new GetRequest("/" + file.getName());
		request.setHeader("Range", "bytes=-1");
		
		IHttpResponse response = con.call(request);
		
		Assert.assertEquals(206, response.getStatus());
		Assert.assertEquals(">", response.getBody().readString());
		
		file.delete();
		con.close();
		server.close();
	}	
	
	
	@Test
	public void testIllegalRange() throws Exception {
    	File file = QAUtil.createTestfile_75byte();
    	String basepath = file.getParentFile().getAbsolutePath();
		
		IServer server = new HttpServer(new FileServiceRequestHandler(basepath));
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		

		IHttpRequest request = new GetRequest("/" + file.getName());
		request.setHeader("Range", "bytes=170-");
		
		IHttpResponse response = con.call(request);
		
		Assert.assertEquals(400, response.getStatus());
		
		file.delete();
		con.close();
		server.close();
	}

	
	@Test
	public void testMultiRange() throws Exception {
		
    	File file = QAUtil.createTestfile_75byte();
    	String basepath = file.getParentFile().getAbsolutePath();
		
		IServer server = new HttpServer(new FileServiceRequestHandler(basepath));
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		

		IHttpRequest request = new GetRequest("/" + file.getName());
		request.setHeader("Range", "bytes=0-0,-1");
		
		IHttpResponse response = con.call(request);
				
		Assert.assertEquals(206, response.getStatus());
		
		file.delete();
		con.close();
		server.close();
	}	
	
	
	@Test
	public void testMultiRange2() throws Exception {
		
    	File file = QAUtil.createTestfile_75byte();
    	String basepath = file.getParentFile().getAbsolutePath();
		
		IServer server = new HttpServer(new FileServiceRequestHandler(basepath));
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		

		IHttpRequest request = new GetRequest("/" + file.getName());
		request.setHeader("Range", "bytes=0-0,37-67,-1");
		
		IHttpResponse response = con.call(request);
		
		QAUtil.sleep(1000);
		System.out.println(response);
		
		Assert.assertEquals(206, response.getStatus());
		
		file.delete();
		con.close();
		server.close();
	}		
	
	
    @Test
    public void testFoundCompress() throws Exception {
        System.out.println("testFoundCompress");

        File file = QAUtil.createTestfile_4k();
        String basepath = file.getParentFile().getAbsolutePath();
        
        IServer server = new HttpServer(new FileServiceRequestHandler(basepath, true));
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setAutoUncompress(false);
       
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/" + file.getName());
        request.setHeader("Accept-Encoding", "compress, gzip");
        
        IHttpResponse response = httpClient.call(request);
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("text/html", response.getContentType());
        Assert.assertEquals("gzip", response.getHeader("Content-Encoding"));
        
        byte[] cData = response.getBody().readBytes();
        byte[] data = HttpUtils.decompress(cData);
        
        Assert.assertTrue(QAUtil.isEquals(file, new ByteBuffer[] { DataConverter.toByteBuffer(data) }));


        int ratio = cData.length * 100 / data.length; 
        System.out.println("ratio " + ratio + "%");

        
        file.delete();
        httpClient.close();
        server.close();
    }

    @Test
    public void testFoundNoCompress() throws Exception {
        System.out.println("testFoundNoCompress");

        File file = QAUtil.createTestfile_4k();
        String basepath = file.getParentFile().getAbsolutePath();
        
        IServer server = new HttpServer(new FileServiceRequestHandler(basepath, true));
        server.start();
        
        HttpClient httpClient = new HttpClient();
       
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/" + file.getName());
        
        IHttpResponse response = httpClient.call(request);
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("text/html", response.getContentType());
        
        Assert.assertTrue(QAUtil.isEquals(file, response.getBody().readByteBuffer()));
        
        file.delete();
        httpClient.close();
        server.close();
    }    

    
    @Test
    public void testContentTypeEncoding() throws Exception {

        File file = QAUtil.createTestfile_utf8WithBOM();
        String basepath = file.getParentFile().getAbsolutePath();
        
        IServer server = new HttpServer(new FileServiceRequestHandler(basepath, true));
        server.start();
        
        HttpClient httpClient = new HttpClient();
       
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/" + file.getName());
        
        IHttpResponse response = httpClient.call(request);
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("text/plain; charset=UTF-8", response.getContentType());
        
        file.delete();
        httpClient.close();
        server.close();
    }
    
    
    @Test
    public void testBinaryFile() throws Exception {

        File file = QAUtil.createTestfile_40k("etrer");
        String basepath = file.getParentFile().getAbsolutePath();
        
        IServer server = new HttpServer(new FileServiceRequestHandler(basepath, true));
        server.start();
        
        HttpClient httpClient = new HttpClient();
       
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/" + file.getName());
        
        IHttpResponse response = httpClient.call(request);
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertNull(response.getContentType());
        
        file.delete();
        httpClient.close();
        server.close();
    }    
    
    
    @Test
    public void testFoundCompressSmallFile() throws Exception {
        System.out.println("testFoundCompressSmallFile");

        File file = QAUtil.createTestfile_130byte();
        String basepath = file.getParentFile().getAbsolutePath();
        
        HttpServer server = new HttpServer(new FileServiceRequestHandler(basepath, true));
        server.setAutoCompressThresholdBytes(20);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setAutoUncompress(false);
       
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/" + file.getName());
        request.setHeader("Accept-Encoding", "compress, gzip");
        
        IHttpResponse response = httpClient.call(request);
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("gzip", response.getHeader("Content-Encoding"));
        
        byte[] cData = response.getBody().readBytes();
        byte[] data = HttpUtils.decompress(cData);
        
        Assert.assertTrue(QAUtil.isEquals(file, new ByteBuffer[] { DataConverter.toByteBuffer(data) }));


        int ratio = cData.length * 100 / data.length; 
        System.out.println("ratio " + ratio + "%");

        
        file.delete();
        httpClient.close();
        server.close();
    }


 
    @Test
    public void testFoundCompressLargeFile() throws Exception {        
        System.out.println("testFoundCompressLargeFile");

        File file = QAUtil.createTestfile_400k();
        String basepath = file.getParentFile().getAbsolutePath();
        
        IServer server = new HttpServer(new FileServiceRequestHandler(basepath, true));
        server.start();
        
        HttpClient httpClient = new HttpClient();
        httpClient.setAutoUncompress(false);
       
        IHttpRequest request = new GetRequest("http://localhost:" + server.getLocalPort() + "/" + file.getName());
        request.setHeader("Accept-Encoding", "compress, gzip");
        
        IHttpResponse response = httpClient.call(request);
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("gzip", response.getHeader("Content-Encoding"));
        
        byte[] cData = response.getBody().readBytes();
        byte[] data = HttpUtils.decompress(cData);
        
        
        Assert.assertTrue(QAUtil.isEquals(file, new ByteBuffer[] { DataConverter.toByteBuffer(data) }));

        int ratio = cData.length * 100 / data.length; 
        System.out.println("ratio " + ratio + "%");

        file.delete();
        httpClient.close();
        server.close();
    }
    
	
    @Test
    public void testEncoding() throws Exception {
        System.out.println("testEncoding");

        File file = QAUtil.copyToTempfile("utf8WithBOM.txt");
        String basepath = file.getParentFile().getAbsolutePath();
        
        IServer server = new HttpServer(new FileServiceRequestHandler(basepath, true));
        server.start();
        
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

        con.write("GET /" + file.getName() + " HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" +
                  "\r\n");
            
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";

        if (header.indexOf("200") == -1) {
            String txt = "unexpected response " + header;
            System.out.println("ERROR " + txt);
            Assert.fail(txt);
        }

        int contentLength = QAUtil.readContentLength(header);
        File tempFile = QAUtil.createTempfile();
        
        RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
        FileChannel fc = raf.getChannel();
        
        con.transferTo(fc, contentLength);
        
        fc.close();
        raf.close();
        
        Assert.assertTrue(QAUtil.isEquals(file, tempFile));

        tempFile.delete();
        file.delete();
        con.close();
        server.close();
    }	
	
	
	@Test
	public void testMimeType() throws Exception {
	    System.out.println("testMimeType");

		File file = QAUtil.createTempfile(".txt");
		FileOutputStream fos = new FileOutputStream(file);
		fos.write("test1234".getBytes());
		fos.close();
		
		
		String basePath = file.getParent();
		
		IServer server = new HttpServer(new FileServiceRequestHandler(basePath, true));
		server.start();

		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/" + file.getName()));
		
		Assert.assertEquals(200, response.getStatus());
		if (!response.getContentType().equals("text/plain")) {
			System.out.println("got mime type " + response.getContentType() +  " instead of text/plain");
			Assert.fail("got mime type " + response.getContentType() +  " instead of text/plain");
		}
		Assert.assertEquals("test1234", response.getBody().readString());

		file.delete();
		httpClient.close();
		server.close();
	}
	
	 
	
	@Test
	public void testNotFound() throws Exception {
	    System.out.println("testNotFound");
		
    	File file = QAUtil.createTestfile_40k();
    	String basepath = file.getParentFile().getAbsolutePath();

		IServer server = new HttpServer(new FileServiceRequestHandler(basepath));
		server.start();
		

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("GET /doesntExists HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");

		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		
		int contentLength = QAUtil.readContentLength(header);
		con.readStringByLength(contentLength);

		Assert.assertTrue(header.indexOf("404") != -1);
		
		file.delete();
		con.close();		
		server.close();
	}
	
	
	
	
	
	@Test
	public void testLastModified() throws Exception {
	    System.out.println("testLastModified");

    	File file = QAUtil.createTestfile_40k();
    	String basepath = file.getParentFile().getAbsolutePath();
		
		IServer server = new HttpServer(new FileServiceRequestHandler(basepath));
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());

		IHttpResponse response = con.call(new GetRequest("/" + file.getName()));
		Assert.assertEquals(200, response.getStatus());
		
		response.getBody().readString();
		
		String lastModified = response.getHeader("Last-Modified");
		GetRequest secondRequest = new GetRequest("/" + file.getName());
		secondRequest.setHeader("If-Modified-Since", lastModified);
		
		response = con.call(secondRequest);
		Assert.assertEquals("unexpected response " + response, 304, response.getStatus());
		
		
		file.delete();
		con.close();
		server.close();
	}

	
	
	@Test
	public void testChainFound() throws Exception {
	    System.out.println("testChainFound");
		
    	File file = QAUtil.createTestfile_400k();
    	String basepath = file.getParentFile().getAbsolutePath();

		RequestHandlerChain chain = new RequestHandlerChain();
		chain.addLast(new FileServiceRequestHandler(basepath));
		IServer server = new HttpServer(chain);

		server.start();
		
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("GET /" + file.getName() + " HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");

		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";

		int contentLength = QAUtil.readContentLength(header);
		con.readStringByLength(contentLength);

		Assert.assertTrue(header.indexOf("200") != -1);

		file.delete();
		con.close();
		server.close();
	}
	
	
	
	@Test
	public void testChainNotFound() throws Exception {
	    System.out.println("testChainNotFound");

	  	File file = QAUtil.createTestfile_400k();
    	String basepath = file.getParentFile().getAbsolutePath();

		
		RequestHandlerChain chain = new RequestHandlerChain();
		chain.addLast(new FileServiceRequestHandler(basepath));
		IServer server = new HttpServer(chain);
		server.start();
		
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("GET /doenstExists HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");

		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";

		int contentLength = QAUtil.readContentLength(header);
		con.readStringByLength(contentLength);

		Assert.assertTrue(header.indexOf("404") != -1);
		
		file.delete();
		con.close();
		server.close();
	}

	
	
	@Test
	public void testChainHandledBySuccessor() throws Exception {
	    System.out.println("testChainHandledBySuccessor");

	  	File file = QAUtil.createTestfile_400k();
    	String basepath = file.getParentFile().getAbsolutePath();

		RequestHandlerChain chain = new RequestHandlerChain();
		chain.addLast(new FileServiceRequestHandler(basepath));
		chain.addLast(new BusinessHandler());
		
		IServer server = new HttpServer(chain);
		server.start();
		
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("GET /doesntExists HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");

		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";

		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);

		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);

		file.delete();
		con.close();
		server.close();
	}
	
	
	@Test
	public void testChainHandledByFileServiceNotSuccessor() throws Exception {
	    System.out.println("testChainHandledByFileServiceNotSuccessor");
		
	  	File file = QAUtil.createTestfile_400k();
    	String basepath = file.getParentFile().getAbsolutePath();

		RequestHandlerChain chain = new RequestHandlerChain();
		chain.addLast(new FileServiceRequestHandler(basepath));
		chain.addLast(new BusinessHandler());
		
		IServer server = new HttpServer(chain);
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());

		IHttpResponse response = con.call(new GetRequest("/" + file.getName()));
		Assert.assertEquals(200, response.getStatus());

		File tempFile = QAUtil.createTempfile();
		
		RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
		FileChannel fc = raf.getChannel();
		
		BodyDataSource source = response.getBody();
		source.transferTo(fc);
		
		fc.close();
		raf.close();
		
		Assert.assertTrue(QAUtil.isEquals(file, tempFile));

		tempFile.delete();
		file.delete();
		con.close();
		server.close();
	}
	
	
	@Test
	public void testChainLogInterceptor() throws Exception {
	    System.out.println("testChainLogInterceptor");
  
	  	File file = QAUtil.createTestfile_400k();
    	String basepath = file.getParentFile().getAbsolutePath();

		RequestHandlerChain chain = new RequestHandlerChain();
		chain.addLast(new LogHandler());
		FileServiceRequestHandler fileSrv = new FileServiceRequestHandler(basepath);
		chain.addLast(fileSrv);
		
		IServer server = new HttpServer(chain);
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		con.setOption(IConnection.SO_RCVBUF, 64);
		con.suspendReceiving();

		FutureResponseHandler respHdl = new FutureResponseHandler();
		con.send(new GetRequest("/" + file.getName()), respHdl);

		QAUtil.sleep(1000);
		con.resumeReceiving();

		
		IHttpResponse response = respHdl.getResponse();
		Assert.assertEquals(200, response.getStatus());

		File tempFile = QAUtil.createTempfile();		
		RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
		FileChannel fc = raf.getChannel();
		
		BodyDataSource source = response.getBody();
		source.transferTo(fc);
		
		fc.close();
		raf.close();
		
		Assert.assertTrue(QAUtil.isEquals(file, tempFile));


		
		tempFile.delete();
		file.delete();
		con.close();
		server.close();
	}
	
	
    @Test
    public void testCachingValidationBased() throws Exception {
        System.out.println("testCachingValidationBased");

        File file = QAUtil.createTestfile_40k();
        String basepath = file.getParentFile().getAbsolutePath();
        
        RequestHandlerChain chain = new RequestHandlerChain();
        chain.addLast(new CacheHandler(500));
        chain.addLast(new FileServiceRequestHandler(basepath, true));
        
        IServer server = new HttpServer(chain);
        server.start();
        
        // first request
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/" + file.getName()));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(QAUtil.isEquals(file, response.getBody().readByteBuffer()));
        con.close();

        
        QAUtil.sleep(1000);
        
        // repeated request (served by cache!)
        con = new HttpClientConnection("localhost", server.getLocalPort());
        response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/" + file.getName()));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.getHeader("X-Cache").startsWith("HIT - revalidated"));
        Assert.assertTrue(QAUtil.isEquals(file, response.getBody().readByteBuffer()));
        con.close();

        
        server.close();
    }
	
    
    @Test
    public void testCachingExpiredBased() throws Exception {
        System.out.println("testCachingExpiredBased");

        File file = QAUtil.createTestfile_40k();
        String basepath = file.getParentFile().getAbsolutePath();
        
        RequestHandlerChain chain = new RequestHandlerChain();
        chain.addLast(new CacheHandler(500));
        chain.addLast(new FileServiceRequestHandler(basepath, 60));
        
        IServer server = new HttpServer(chain);
        server.start();
        
        // first request
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/" + file.getName()));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertNotNull(response.getHeader("Expires"));
        Assert.assertTrue(QAUtil.isEquals(file, response.getBody().readByteBuffer()));
        con.close();

        
        QAUtil.sleep(1000);
        
        // repeated request (served by cache!)
        con = new HttpClientConnection("localhost", server.getLocalPort());
        response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/" + file.getName()));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertNotNull(response.getHeader("Expires"));
        Assert.assertTrue(response.getHeader("X-Cache").startsWith("HIT"));
        Assert.assertTrue(QAUtil.isEquals(file, response.getBody().readByteBuffer()));
        con.close();

        
        server.close();
    }
        
	
	
	@Test
	public void testFound() throws Exception {
	    System.out.println("testFound");
	
		File file = QAUtil.createTestfile_400k();
		String basepath = file.getParentFile().getAbsolutePath();
		
		IServer server = new HttpServer(new FileServiceRequestHandler(basepath, true));
		server.start();
		
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
	
		con.write("GET /" + file.getName() + " HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
			
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
	
		if (header.indexOf("200") == -1) {
		    String txt = "unexpected response " + header;
		    System.out.println("ERROR " + txt);
		    Assert.fail(txt);
		}
	
		int contentLength = QAUtil.readContentLength(header);
		File tempFile = QAUtil.createTempfile();
		
		RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
		FileChannel fc = raf.getChannel();
		
		con.transferTo(fc, contentLength);
		
		fc.close();
		raf.close();
		
		Assert.assertTrue(QAUtil.isEquals(file, tempFile));
	
		tempFile.delete();
		file.delete();
		con.close();
		server.close();
	}
	
	
	   
    @Test
    public void testFoundHead() throws Exception {
    
        File file = QAUtil.createTestfile_400k();
        String basepath = file.getParentFile().getAbsolutePath();
        
        IServer server = new HttpServer(new FileServiceRequestHandler(basepath, true));
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        IHttpResponse response = httpClient.call(new HeadRequest("http://localhost:" + server.getLocalPort() + "/" + file.getName()));
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertFalse(response.hasBody());
        
        file.delete();
        
        httpClient.close();
        server.close();
    }

    
    
    @Test
    public void testNotFoundChain() throws Exception {

        RequestHandlerChain chain = new RequestHandlerChain();
        
        File file = QAUtil.createTestfile_400k();
        String basepath = file.getParentFile().getAbsolutePath();
        
        IHttpRequestHandler businessHandler = new IHttpRequestHandler() {
            
            public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
                exchange.send(new HttpResponse(200, "text/plain", "OK"));
            }
        };
        chain.addLast(businessHandler);
        
        FileServiceRequestHandler fileHandler = new FileServiceRequestHandler(basepath, true);
        chain.addLast(fileHandler);
        
        IServer server = new HttpServer(chain);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/" + file.getName()));
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("OK", response.getBody().toString());
        
        
        file.delete();
        httpClient.close();
        server.close();
    }



	private static final class BusinessHandler implements IHttpRequestHandler  {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			exchange.send(new HttpResponse(200, "text/plain", "OK"));
		}		
	}
	
	
	private static final class LogHandler implements IHttpRequestHandler {
		
		public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
			IHttpRequest request = exchange.getRequest();
			
			IHttpResponseHandler respHdl = new IHttpResponseHandler() {
				
				public void onResponse(IHttpResponse response) throws IOException {
					exchange.send(response);
				}
				
				public void onException(IOException ioe) throws IOException {
					exchange.sendError(ioe);
				}
			};
			
			exchange.forward(request, respHdl);
		}
		
	}
}