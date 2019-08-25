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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IConnection.FlushMode;

public final class DownloadTest  {



	@Test
	public void testSimple() throws Exception {
	    HttpServer server = new HttpServer(new FileDownloadHandler());
	    server.start();
	    
	    HttpClient client = new HttpClient();

	    
	    File file = QAUtil.createTestfile_40k();
	    
	    IHttpResponse resp = client.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/" + file.getAbsolutePath()));
	    byte[] data = resp.getBody().readBytes();
	    
	    Assert.assertTrue(QAUtil.isEquals(file, new ByteBuffer[] { ByteBuffer.wrap(data) } ));
	    
	    file.delete();
	    
	    client.close();
	    server.close();
	}
	
	
	private static final class FileDownloadHandler implements IHttpRequestHandler {
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        IHttpRequest request = exchange.getRequest();
            String filename = request.getRequestURI();
            
            HttpResponseHeader responseHeader = new HttpResponseHeader(200);
            
            RandomAccessFile raf = new RandomAccessFile(filename, "r");
            FileChannel fc = raf.getChannel();
            BodyDataSink outChannel = exchange.send(responseHeader, (int) fc.size());
            outChannel.setFlushmode(FlushMode.SYNC);  // unnecessary, because it is SYNC default 
            
            ByteBuffer transferBuffer = ByteBuffer.allocate(8192);
            
            int read = 0;
            do {
                transferBuffer.clear();
            
                read = fc.read(transferBuffer);
                if (read > 0) {
                    transferBuffer.flip();
                    outChannel.write(transferBuffer);
                }
            } while (read != -1);
            
            outChannel.close();
            fc.close();
            raf.close();
	    }
	}
}
