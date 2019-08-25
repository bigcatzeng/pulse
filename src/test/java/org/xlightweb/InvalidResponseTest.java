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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;


import org.junit.Assert;

import org.junit.Test;
import org.xlightweb.client.HttpClient;



/**
*
* @author grro@xlightweb.org
*/
public final class InvalidResponseTest {


    public static void main(String[] args) throws Exception {
        
        for (int i = 0; i < 100000; i++) {
            new InvalidResponseTest().testSimple();
        }
    }
    
    
    @Test
    public void testBulk() throws Exception {
        
        for (int i = 0; i < 5; i++) {
            testSimple();
        }
    }
    
	@Test
	public void testSimple() throws Exception {
	    System.out.println("starting Server");
	    TestServer testServer = new TestServer();
	    new Thread(testServer).start();
	    
	    QAUtil.sleep(500);
	    
	    System.out.println("creating client");
	    HttpClient httpclient = new HttpClient();
	    
	    String url = "http://localhost:" + testServer.getPort();
	    GetRequest request = new GetRequest(url);
	    
	    System.out.println("call");
	    
	    IHttpResponse response = httpclient.call(request);
	    
	    System.out.println("read bytes");
	    byte[] data = response.getBody().readBytes();
	    
	    System.out.println("got it");
	    Assert.assertEquals("<html><body>Hello</body></html>\r\n", new String(data));
	    
	    
	    httpclient.close();
	    testServer.close();
	}

	

    private static final class TestServer implements Runnable {
     
        private ServerSocket ss = null;
        
        
        public void run() {

            try {
                String strbuf = "HTTP/1.1 200 OK\r\n" +
                                "Date: Fri, 12 Jun 2009 05:07:45 GMT\r\n" +
                                "Server: Microsoft-IIS/5.0\r\n" +
                                "\r\n";
                
                ss = new ServerSocket(0);
                while (true) {
                    Socket socket = ss.accept();
                    
                    InputStream is = socket.getInputStream();
                    LineNumberReader lnr = new LineNumberReader(new InputStreamReader(is));
                    
                    // read first line
                    lnr.readLine();
                    
                    OutputStream out = socket.getOutputStream();
                    OutputStreamWriter w = new OutputStreamWriter(out);
                    
                    w.write(strbuf.toString());
                    w.flush();
                    w.write("<html><body>Hello</body></html>\r\n");
                    w.flush();
                    w.close();
                }
            } catch (Exception ignore) { }
        }
        
        
        public int getPort() {
            return ss.getLocalPort();
        }
        
        public void close() throws IOException {
            ss.close();
        }
    }
}
