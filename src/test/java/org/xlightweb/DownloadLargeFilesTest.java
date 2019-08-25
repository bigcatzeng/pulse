/*
 *  Copyright (c) xsocket.org, 2006 - 2009. All rights reserved.
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
 * The latest copy of this software may be found on http://www.xsocket.org/
 */
package org.xlightweb;



import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.xlightweb.server.HttpServer;



/**
*
* @author grro@xsocket.org
*/
public final class DownloadLargeFilesTest {
	

    private final List<String> errors = new ArrayList<String>();
    private final AtomicInteger running = new AtomicInteger(0);
    
    
    @Before
    public void setup() {
        errors.clear();
        running.set(0);
    }
    
    
	@Test 
	public void testConcurrent() throws Exception {
	    
	    File file = QAUtil.createTestfile_400k();
	    String path = file.getParent();
	    
	    HttpServer httpServer = new HttpServer(new FileServiceRequestHandler(path));
	    httpServer.start();
	    
	    final String surl = "http://localhost:" + httpServer.getLocalPort() + "/" + file.getName();
	    
	    
	    for (int i = 0; i < 3; i++) {
	        new Thread() {
	          
	            @Override
	            public void run() {
	                running.incrementAndGet();
	                try {
	                    int read = 0;
	                    int totalRead = 0;
	                    HttpURLConnection conn = null;
	                    InputStream in = null;

	                    URL url = new URL(surl);
	                    conn = (HttpURLConnection) url.openConnection();
	                    in = new BufferedInputStream(conn.getInputStream(), 4096);
	                    byte[] buffer = new byte[4096];
	                    while ((read = in.read(buffer)) > 0) {
	                        totalRead += read;
	                    }
	                    in.close();
	                         
	                    if (totalRead == conn.getContentLength()) {
	                        System.out.print(".");
	                    } else {
	                        errors.add("got " + totalRead + " bytes. expected: " + conn.getContentLength()); 
	                    }
	                    
	                } catch (Exception e) {
	                    e.printStackTrace();
	                } 
	                
	                running.decrementAndGet();
	            }
	        }.start();
	    }
	    
	    
	    do {
	        QAUtil.sleep(500);
	    } while (running.get() > 0);

	    
	    file.delete();
	    httpServer.close();
	}
	
	
	@Test 
	public void testLargeFile() throws Exception {
        File file = QAUtil.createTestfile_400k();
        String path = file.getParent();
        
        HttpServer httpServer = new HttpServer(new FileServiceRequestHandler(path));
        httpServer.start();
        
        String surl = "http://localhost:" + httpServer.getLocalPort() + "/" + file.getName();
        
        
        int read = 0;
        int totalRead = 0;
        HttpURLConnection conn = null;
        InputStream in = null;

        System.out.println("downloading large file...");
        
        URL url = new URL(surl);
        conn = (HttpURLConnection) url.openConnection();
        in = new BufferedInputStream(conn.getInputStream(), 4096);
        byte[] buffer = new byte[4096];
        while ((read = in.read(buffer)) > 0) {
            totalRead += read;
        }
        in.close();
             
        if (totalRead == conn.getContentLength()) {
            System.out.print(".");
        } else {
            errors.add("got " + totalRead + " bytes. expected: " + conn.getContentLength()); 
        }
        
        file.delete();
        httpServer.close();
	}	        
}
