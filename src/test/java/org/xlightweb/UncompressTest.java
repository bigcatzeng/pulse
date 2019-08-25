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

import java.io.RandomAccessFile;


import java.nio.channels.FileChannel;

import junit.framework.Assert;

import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.BlockingConnection;





/**
*
* @author grro@xlightweb.org
*/
public final class UncompressTest  {

    

    @Test
    public void testFile() throws Exception {
        
        File file = QAUtil.copyToTempfile("compressedfile.zip");
        String path = file.getParent();
        
        HttpServer httpServer = new HttpServer(new FileServiceRequestHandler(path, true));
        httpServer.start();
        
        HttpClient httpClient = new HttpClient();

        for (int i = 0; i < 500; i++) {
            GetRequest req = new GetRequest("http://localhost:" + httpServer.getLocalPort() + "/" + file.getName());
            req.setHeader("Accept-Encoding", "gzip");
            
            IHttpResponse resp = httpClient.call(req);
            Assert.assertEquals(5867, resp.getContentLength());
        }
        
        file.delete();
        httpClient.close();
        httpServer.close();
    }
}
