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



import java.io.IOException;
import java.net.SocketTimeoutException;

import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.NonBlockingConnection;




/**
*
* @author grro@xlightweb.org
*/	    
public class ConnectionTimeoutTest {

    
    
    @Test 
    public void testConnectionTimeout() throws Exception {

        IServer server = new HttpServer(new DoNothingRequestHandler());
        
        INonBlockingConnection con = new NonBlockingConnection("localhost", server.getLocalPort());
        con.setConnectionTimeoutMillis(1000);
        
        HttpClientConnection httpCon = new HttpClientConnection(con);
        
        ResponseHandler respHdl = new ResponseHandler();
        httpCon.send(new GetRequest("/"), respHdl);
        
        QAUtil.sleep(2000);
        Assert.assertTrue(respHdl.getException() instanceof SocketTimeoutException);
        
        server.close();
        
    }
    
    
    @Execution(Execution.NONTHREADED)
    private static final class ResponseHandler implements IHttpResponseHandler {
        
        private IOException ioe = null;
        
        public void onResponse(IHttpResponse response) throws IOException {
            
        }
        
        public void onException(IOException ioe) throws IOException {
            this.ioe = ioe;
        }
        
        IOException getException() {
            return ioe;
        }
    }

    
    private static final class DoNothingRequestHandler implements IHttpRequestHandler {
        
        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
            // do nothing
        }
    }
    
}
