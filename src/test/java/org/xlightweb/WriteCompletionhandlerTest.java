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


import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.BodyDataSink;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xlightweb.server.HttpServerConnection;
import org.xsocket.connection.IWriteCompletionHandler;
import org.xsocket.connection.IConnection.FlushMode;





/**
*
* @author grro@xlightweb.org
*/
public final class WriteCompletionhandlerTest  {

	
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 10000; i++) {
            new WriteCompletionhandlerTest().testServersideAsyncWrite();
        }
    }
    
    
	@Test
	public void testServersideAsyncWrite() throws Exception {
	    
	    System.out.println("testServersideAsyncWrite");

	    HttpServer server = new HttpServer(new AsyncFileHandler());
	    server.start();
	    
	    HttpClient httpClient = new HttpClient();
	    
	    File file = QAUtil.createTestfile_40k();
	    
	    IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/" + file.getAbsolutePath()));
	    BodyDataSource dataSource = response.getBody();
	    
	    File tempFile = QAUtil.createTempfile();
        
        RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
        FileChannel fc = raf.getChannel();
        
        dataSource.transferTo(fc);
        fc.close();
        raf.close();
        
        Assert.assertTrue(QAUtil.isEquals(tempFile, file));
        dataSource.close();
	    
        file.delete();	    
        tempFile.delete();
	    httpClient.close();
	    server.close();
	}
	
	
	
	@Test
	public void testServersideWriteWithInterceptor() throws Exception {	    
	    System.out.println("testServersideWriteWithInterceptor");
		
		RequestHandlerChain chain = new RequestHandlerChain();
		chain.addFirst(new Interceptor());
		chain.addFirst(new EchoHandler());
		
	    HttpServer server = new HttpServer(chain);
	    server.start();
	    
	    HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
	    
	    
	    FutureResponseHandler respHandler = new FutureResponseHandler();
	    BodyDataSink dataSink = con.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/"), respHandler);
	    dataSink.write("test");
	    QAUtil.sleep(300);
	    
	    dataSink.write("123");
	    QAUtil.sleep(300);
	    
	    dataSink.write("456");
	    QAUtil.sleep(300);
	    
	    dataSink.close();
	    
	    
	    IHttpResponse response = respHandler.getResponse();
	    Assert.assertEquals("test123456", response.getBody().readString());
	    
        
	    con.close();
	    server.close();
	}
	
	@Test
	public void testServersideWriteWithInterceptor2() throws Exception {
	    
	    System.out.println("testServersideWriteWithInterceptor2");
	    
		RequestHandlerChain chain = new RequestHandlerChain();
		chain.addFirst(new Interceptor());
		EchoHandler echoHandler = new EchoHandler();
		chain.addFirst(echoHandler);
		
	    HttpServer server = new HttpServer(chain);
	    server.start();
	    
	    HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
	    
	
	    FutureResponseHandler respHandler = new FutureResponseHandler();
	    BodyDataSink dataSink = con.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/?isSuspend=true&durationMillis=2000"), respHandler);
	    
	    StringBuilder sb = new StringBuilder();
	    
	    for (int i = 1; i < 2; i++) {
	    	String data = new String(QAUtil.generateByteArray(222000 * i));
	    	sb.append(data);
	    	
	    	System.out.println("write data");
	    	dataSink.write(data);
		    QAUtil.sleep(200);
	    }
	    
	    System.out.println("clsoe data sink");
	    dataSink.close();
	    
	    
	    System.out.println("get response handle");
	    IHttpResponse response = respHandler.getResponse();
	    
	    System.out.println("reading response");
	    Assert.assertEquals(sb.toString(), response.getBody().readString());
	 
	    System.out.println("close con & server");
	    con.close();
	    server.close();
	    
	    System.gc();
	}
	
	

    @Test
    public void testServersideWriteWithInterceptor2Bulk() throws Exception {
        System.out.println("testServersideWriteWithInterceptor2Bulk");
        
        for (int i = 1; i < 5; i++) {
            testServersideWriteWithoutInterceptor2();
        }
    }
    
	

	@Test
	public void testServersideWriteWithoutInterceptor() throws Exception {
	    
        EchoHandler echoHandler = new EchoHandler();
        
        HttpServer server = new HttpServer(echoHandler);
        server.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
    
        FutureResponseHandler respHandler = new FutureResponseHandler();
        BodyDataSink dataSink = con.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/?isSuspend=true&durationMillis=2000"), respHandler);
        
        StringBuilder sb = new StringBuilder();
        
        for (int i = 1; i < 20; i++) {
            String data = new String(QAUtil.generateByteArray(22 * i));
            sb.append(data);
            dataSink.write(data);
            QAUtil.sleep(200);
        }
        
        dataSink.close();
        
        
        IHttpResponse response = respHandler.getResponse();
        Assert.assertEquals(sb.toString(), response.getBody().readString());
     
        con.close();
        server.close();
        
        System.gc();

	}

	
	@Test
	public void testServersideWriteWithoutInterceptor2() throws Exception {
        EchoHandler echoHandler = new EchoHandler();
        
        HttpServer server = new HttpServer(echoHandler);
        server.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
    
        FutureResponseHandler respHandler = new FutureResponseHandler();
        BodyDataSink dataSink = con.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/?isSuspend=true&durationMillis=2000"), respHandler);
        
        StringBuilder sb = new StringBuilder();
        
        for (int i = 1; i < 2; i++) {
            String data = new String(QAUtil.generateByteArray(222000 * i));
            sb.append(data);
            dataSink.write(data);
            QAUtil.sleep(200);
        }
        
        System.out.println("data sink close");
        dataSink.close();
        
        System.out.println("get response handle");
        IHttpResponse response = respHandler.getResponse();
        Assert.assertEquals(sb.toString(), response.getBody().readString());
           
        con.close();
        server.close();
        
        System.gc();
	}
	  
	
	@Test
    public void testServersideAsyncWriteBulk() throws Exception {
	    System.out.println("testServersideAsyncWriteBulk");
	    
	    int calls = 20;
	    for (int i = 0; i < calls; i++) {
            System.out.println("Call " + i + " of " + calls  + " testServersideAsyncWriteBulk");
	        testServersideAsyncWrite();
	    }
	    
	    System.gc();
    }
	
	
	@Test
	public void testServersideAsyncWriteBulk2() throws Exception {
	    
	    System.out.println("testServersideAsyncWriteBulk2");
	    
	    int calls = 60;
	    for (int i = 0; i < calls; i++) {
	        System.out.println("Call " + i + " of " + calls  + "testServersideAsyncWriteBulk2");
	        testServersideAsyncWrite2();
	    }
	    System.gc();
	}
	  
	
	   
    @Test
    public void testServersideAsyncWrite2() throws Exception {
        
        System.out.println("testServersideAsyncWrite2");
        
        HttpServer server = new HttpServer(new AsyncFileHandler());
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        File file = QAUtil.createTestfile_40k();
        
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/" + file.getAbsolutePath()));
        int length = Integer.parseInt(response.getHeader("x-length"));
        BodyDataSource dataSource = response.getBody();
        
        File tempFile = QAUtil.createTempfile();
        
        RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
        FileChannel fc = raf.getChannel();
        
        dataSource.transferTo(fc, length);
        fc.close();
        raf.close();
        
        Assert.assertTrue(QAUtil.isEquals(tempFile, file));
        dataSource.close();
        
        
        file.delete();
        tempFile.delete();
        httpClient.close();
        server.close();
    }
    
	
    @Test
    public void testClientsideAsyncWrite() throws Exception {
        
        System.out.println("testClientsideAsyncWrite");

        FileHandler srvHdl = new FileHandler();
        HttpServer server = new HttpServer(srvHdl);
        server.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        File file = QAUtil.createTestfile_40k();
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        BodyDataSink dataSink = con.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/"), respHdl);
           
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        AsyncWriter writer = new AsyncWriter(dataSink, raf);
        writer.onWritten(0);
        
        

        IHttpResponse response = respHdl.getResponse();
        
        Assert.assertEquals(200,response.getStatus());
        Assert.assertTrue(QAUtil.isEquals(file, "ISO-8859-1", srvHdl.getData()));
        
        file.delete();
        con.close();
        server.close();
    }



    
    @Test
    public void testClientsideHttpClientAsyncWrite() throws Exception {
        System.out.println("testClientsideHttpClientAsyncWrite");
        
        FileHandler srvHdl = new FileHandler();
        HttpServer server = new HttpServer(srvHdl);
        server.start();
        
        HttpClient httpclient = new HttpClient();
        
        File file = QAUtil.createTestfile_40k();
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        BodyDataSink dataSink = httpclient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/"), respHdl);
           
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        AsyncWriter writer = new AsyncWriter(dataSink, raf);
        writer.onWritten(0);
        
        

        IHttpResponse response = respHdl.getResponse();
        
        System.out.println("got response header");
        
        Assert.assertEquals(200,response.getStatus());
        Assert.assertTrue(QAUtil.isEquals(file, "ISO-8859-1", srvHdl.getData()));
        
        raf.close();
        file.delete();
        httpclient.close();
        server.close();
    }

    
    

	private static final class AsyncFileHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			String filename = exchange.getRequest().getRequestURI();
			
			RandomAccessFile raf = new RandomAccessFile(filename, "r");
            HttpResponseHeader header = new HttpResponseHeader(200);
            header.addHeader("x-length", Long.toString(raf.length()));
            BodyDataSink outChannel = exchange.send(header, (int) raf.length());

            AsyncWriter asyncWriter = new AsyncWriter(outChannel, raf);
            asyncWriter.onWritten(0);
		}
	}
	
	
	
	private static final class FileHandler implements IHttpRequestHandler {
	    
	    private String data;
	    
	    @InvokeOn(InvokeOn.MESSAGE_RECEIVED)
	    public void onRequest(IHttpExchange exchange) throws IOException {
System.out.println("server got request ");	            
	        data = exchange.getRequest().getBody().readString();
	        
	        exchange.send(new HttpResponse(200));
	    }
	    
	    String getData() {
	        return data;
	    }
	}
	
	
	
	private static final class EchoHandler implements IHttpRequestHandler {
		
		private final AtomicReference<HttpServerConnection> conRef = new AtomicReference<HttpServerConnection>();
	    
	    public void onRequest(final IHttpExchange exchange) throws IOException {
	
	    	conRef.set((HttpServerConnection) exchange.getConnection());
	    	
	    	final IHttpRequest request = exchange.getRequest();
	    	
	    	boolean isSupsend = request.getBooleanParameter("isSuspend", false);
	    	
	    	if (isSupsend) {
	    		final int durationMillis = request.getIntParameter("durationMillis", 0);

	    		new Thread() {

	    			public void run() {

	    				try {
		    				exchange.getConnection().suspendReceiving();
		    				QAUtil.sleep(durationMillis);
		    				exchange.getConnection().resumeReceiving();
	    					
	    					exchange.send(new HttpResponse(200, request.getContentType(), request.getNonBlockingBody()));
	    				} catch (Exception e) {
	    					throw new RuntimeException(e);
	    				}
	    			}
	    		}.start();
	    		
	    		
	    	} else {
		        exchange.send(new HttpResponse(200, request.getContentType(), request.getNonBlockingBody()));
	    	}
	    }
	}
	
	
	
	private static final class Interceptor implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			IHttpRequest req = exchange.getRequest();
			exchange.forward(req);			
		}
	}
	
	
	private static final class AsyncWriter implements IWriteCompletionHandler {
	    
		private final RandomAccessFile raf;
	    private final FileChannel fc;
	    private final BodyDataSink outChannel;
	    private ByteBuffer transferBuffer = ByteBuffer.allocate(4096);
	    private ByteBuffer[] bufs = new ByteBuffer[] { transferBuffer };
	    
	    
	    public AsyncWriter(BodyDataSink outChannel, RandomAccessFile raf) {
	    	this.raf = raf;
	    	fc = raf.getChannel();
	        this.outChannel = outChannel;
	        
	        outChannel.setFlushmode(FlushMode.SYNC);
        }
	    
	    
	    public void onWritten(int written) throws IOException {
	        transferBuffer.clear();
            
	        int read = fc.read(transferBuffer);
	        transferBuffer.flip();
	              
	        if (read > 0) {
	        	outChannel.write(bufs, this);
	        } else {
	           outChannel.close();
	           fc.close();
	           raf.close();
	        } 
	    }
	    
	    public void onException(IOException ioe) {
	        outChannel.destroy();
	    }
	}
}

