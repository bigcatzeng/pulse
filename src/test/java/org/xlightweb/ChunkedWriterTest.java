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

import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.DataConverter;




/**
*
* @author grro@xlightweb.org
*/
public final class ChunkedWriterTest  {
	
	
    @Test
    public void testSimple() throws Exception {
        
        RequestHandler reqHdl = new RequestHandler();
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        BodyDataSink dataSink = con.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/"), respHdl);
        
        dataSink.write("test");
        QAUtil.sleep(200);
        
        dataSink.write("OneTwo");
        QAUtil.sleep(200);
        
        dataSink.write("End");
        dataSink.close();
        
        IHttpResponse response = respHdl.getResponse();
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("testOneTwoEnd", DataConverter.toString(reqHdl.getData()));
        
        con.close();
        server.close();
    }
    
    
    @Test
    public void testFile() throws Exception {
        
        RequestHandler reqHdl = new RequestHandler();
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        File file = QAUtil.createTestfile_4k();
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        BodyDataSink dataSink = con.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/"), respHdl);
        
        
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel fc = raf.getChannel();
        dataSink.transferFrom(fc);
        fc.close();
        raf.close();
        dataSink.close();
        
        IHttpResponse response = respHdl.getResponse();
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(QAUtil.isEquals(file, reqHdl.getData()));
        
        file.delete();
        con.close();
        server.close();
    }
    
    
    
    @Test
    public void testFile2() throws Exception {
        
        RequestHandler2 reqHdl = new RequestHandler2();
        HttpServer server = new HttpServer(reqHdl);
        server.start();
        
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        File file = QAUtil.createTestfile_4k();
        
        FutureResponseHandler respHdl = new FutureResponseHandler();
        BodyDataSink dataSink = con.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/"), respHdl);
        
        
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel fc = raf.getChannel();
        dataSink.transferFrom(fc);
        fc.close();
        raf.close();
        dataSink.close();
        
        IHttpResponse response = respHdl.getResponse();
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(QAUtil.isEquals(file, "ISO-8859-1", reqHdl.getData()));
        
        file.delete();
        con.close();
        server.close();
    }
    
    
    
    
    public static final class RequestHandler implements IHttpRequestHandler {
        
        private ByteBuffer[] data;
        
        @InvokeOn(InvokeOn.MESSAGE_RECEIVED)
        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
            
            data = exchange.getRequest().getBody().readByteBuffer(); 
            
            exchange.send(new HttpResponse(200));
        }
        
        
        ByteBuffer[] getData() {
            return data;
        }
    }
    
    
    
 public static final class RequestHandler2 implements IHttpRequestHandler {
        
        private String data;
        
        @InvokeOn(InvokeOn.MESSAGE_RECEIVED)
        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
            
            data = exchange.getRequest().getBody().readString(); 
            exchange.send(new HttpResponse(200));
        }
        
        
        String getData() {
            return data;
        }
    }
}