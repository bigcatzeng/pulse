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


import org.junit.Assert;

import org.junit.Test;
import org.xlightweb.client.HttpClient;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;


/**
*
* @author grro@xlightweb.org
*/
public final class HttpClientBodyDisconnect {


	@Test
	public void testSendingBody() throws Exception {
	    
	    IDataHandler dh = new IDataHandler() {

	        private int state = 0;
	        
            public boolean onData(INonBlockingConnection connection) throws IOException {
                
                switch (state) {
                case 0:
                    String header = connection.readStringByDelimiter("\r\n\r\n");
                    state = 1;

                case 1:
                    connection.readByteBufferByLength(5);
                    connection.close();
                    break;
                }
                
                connection.close();
                return true;
            }
        };
        
        IServer server = new Server(dh);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        FutureResponseHandler hdl = new FutureResponseHandler();
        BodyDataSink ds = httpClient.send(new HttpRequestHeader("POST", "http://localhost:" + server.getLocalPort() + "/", "text/plain"), 20, hdl);
        ds.write("12");
        QAUtil.sleep(500);
        ds.write("3456");
        
        try {
            hdl.getResponse();
            Assert.fail("IOException expected");
        } catch (IOException expected) { }
	    
	}
}
