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
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.concurrent.atomic.AtomicInteger;


import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import org.junit.Test;

import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.server.HttpServer;
import org.xlightweb.client.HttpClientConnection;
import org.xsocket.connection.IWriteCompletionHandler;
import org.xsocket.connection.IConnection.FlushMode;



/**
*
* @author grro@xlightweb.org
*/
public final class SendFileTest {

    private File file;

    @Before
    public void setUp() {
        file = QAUtil.createTestfile_4000k();
    }
    
    @After
    public void tearDown() {
        file.delete();
    }
    
    
    
    
    
    @Test
    public void testSync() throws Exception {

        SyncRequestHandler hdl = new SyncRequestHandler();
        HttpServer server = new HttpServer(hdl);
        server.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort()); 
        IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));

        con.suspendReceiving(); 
        QAUtil.sleep(1000);
        
        Assert.assertTrue(hdl.getWritten() < response.getContentLength());
        con.resumeReceiving();
        
        Assert.assertEquals(200, response.getStatus());
        QAUtil.isEquals(file, response.getBody().readByteBuffer());
        
        server.close();
    }
    
    
    
    @Test
    public void testAsync() throws Exception {

        AsyncRequestHandler hdl = new AsyncRequestHandler();
        HttpServer server = new HttpServer(hdl);
        server.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort()); 
        IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));

        con.suspendReceiving();
        QAUtil.sleep(1000);
        
        Assert.assertTrue(hdl.getWritten() < response.getContentLength());
        con.resumeReceiving();
        
        Assert.assertEquals(200, response.getStatus());
        QAUtil.isEquals(file, response.getBody().readByteBuffer());
        
        server.close();
    }
    
    
    
	@Test
	public void testNative() throws Exception {
        RequestHandler hdl = new RequestHandler();
        HttpServer server = new HttpServer(hdl);
        server.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort()); 
        IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));

        con.suspendReceiving();
        QAUtil.sleep(1000);
        
        System.out.println("available " + response.getNonBlockingBody().available());
        Assert.assertTrue(response.getNonBlockingBody().available() < 4000000);
        con.resumeReceiving();

        Assert.assertEquals(200, response.getStatus());
        QAUtil.isEquals(file, response.getBody().readByteBuffer());
        
        server.close();	
    }
	
	

    private final class RequestHandler implements IHttpRequestHandler {
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        
	        QAUtil.sleep(500); // FOR TEST PURPOSES ONLY
	        
	        exchange.send(new HttpResponse(file));  
	    }
	}
    
	
	private final class SyncRequestHandler implements IHttpRequestHandler {
	    
	    private final AtomicInteger written = new AtomicInteger(0);
	        
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        
	        BodyDataSink outChannel = exchange.send(new HttpResponseHeader(200), (int) file.length());
	        outChannel.flush(); // forces to write header FOR TEST PURPOSES ONLY
	        QAUtil.sleep(500); // FOR TEST PURPOSES ONLY
	        
	        RandomAccessFile raf = new RandomAccessFile(file, "r");
	        ReadableByteChannel fc = raf.getChannel();
	          
	        ByteBuffer copyBuffer = ByteBuffer.allocate(4096); 
	           
	        int read = 0;
	        while (read >= 0) {
	           read = fc.read(copyBuffer);
	           copyBuffer.flip();
	              
	           if (read > 0) {
	              int size = outChannel.write(copyBuffer);
	              written.addAndGet(size);
	              if (copyBuffer.hasRemaining()) {
	                 copyBuffer.compact();
	              } else {
	                 copyBuffer.clear();
	              }
	           }
	        } 
	        
	        outChannel.close();
	        
	        System.out.println("Server: all data written");
	    }
	    
	    
	    public int getWritten() {
	        return written.get();
	    }
	}
	
	
	private final class AsyncRequestHandler implements IHttpRequestHandler {
	    
	    private final AtomicInteger written = new AtomicInteger(0);

        
        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
            
            HttpResponseHeader responseHeader = new HttpResponseHeader(200);
            final BodyDataSink outChannel = exchange.send(responseHeader, (int) file.length());
            outChannel.setFlushmode(FlushMode.ASYNC);
            
            outChannel.flush(); // forces to write header FOR TEST PURPOSES ONLY
            QAUtil.sleep(500); // FOR TEST PURPOSES ONLY

            
            final RandomAccessFile raf = new RandomAccessFile(file, "r");
            final FileChannel fc = raf.getChannel();
            
            
            IWriteCompletionHandler sendFileProcess = new IWriteCompletionHandler() {
                
                private long remaining = file.length();
                private long offset = 0;
                private long length = 0;
                
                
                public void onWritten(int written) throws IOException {
                    write();
                }
                
                
                private void write() throws IOException {
                    
                    // remaining data to write?
                    if (remaining > 0) {
                        
                        // limit the buffer allocation size 
                        if (remaining > 4096) {
                            length = 4096;
                        } else {
                            length = remaining;
                        }
                        
                        MappedByteBuffer buffer = fc.map(MapMode.READ_ONLY, offset, length);
                        ByteBuffer[] bufs = new ByteBuffer[] { buffer };
                  
                        outChannel.write(bufs, this);
                            
                        offset += length;
                        remaining -= length;

                    // no, closing channel
                    } else {
                        closeFile();
                        outChannel.close();
                    } 
                }
                
                public void onException(IOException ioe) {
                    closeFile();
                    outChannel.destroy();
                }
                
                
                private void closeFile() {
                    try {
                        fc.close();
                        raf.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            };
            
            sendFileProcess.onWritten(0);  // start sending 
        }
        
        public int getWritten() {
            return written.get();
        }
    }
}
