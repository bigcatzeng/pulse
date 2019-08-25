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



import org.junit.Assert;
import org.junit.Test;


import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;





/**
*
* @author grro@xlightweb.org
*/
public final class OutstandingRequestTest  {

 
    private static final int WAIT_LOOPS = 10;
    private static final int WAIT_TIME = 100; 
    
    

    @Test
    public void testOutstandingRequest() throws Exception {
                
        ServerHandler hdl = new ServerHandler();
        HttpServer server = new HttpServer(hdl);
        server.setRequestTimeoutMillis(1000);
        server.start();
    
        HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
        
        QAUtil.sleep(1500);
        if (hdl.getCountRequestTimeout() != 1) {
            System.out.println("request timeout should have been called once. called " + hdl.getCountRequestTimeout());
            Assert.fail("request timeout should have been called once. called " + hdl.getCountRequestTimeout());
        }
        
        con.close();
        server.close();
    }

    
	
	@Test
	public void testOutstandingRequestWithSlowServerSending() throws Exception {
			    
		ServerHandler2 hdl = new ServerHandler2();
		HttpServer server = new HttpServer(hdl);
		server.setRequestTimeoutMillis(1000);
		server.start();
	
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		IHttpResponseHandler resHdl = new IHttpResponseHandler() {
			
			public void onResponse(IHttpResponse response) throws IOException {
			}
			
			public void onException(IOException ioe) {
			}
		};
		
		con.send(new GetRequest("/"), resHdl);

		QAUtil.sleep((WAIT_LOOPS * WAIT_TIME));
		if (hdl.getCountRequestTimeout() != 0) {
		    System.out.println("request timeout should not be called");
		    Assert.fail("request timeout should not be called");
		}
		    
		
		QAUtil.sleep(2500);
		if (hdl.getCountRequestTimeout() != 1) {
		    System.out.println("request timeout should have been called once. called " + hdl.getCountRequestTimeout());
		    Assert.fail("request timeout should have been called once. called " + hdl.getCountRequestTimeout());
		}
		
		con.close();
		server.close();
	}

	
	   private static final class ServerHandler implements IHttpRequestHandler, IHttpRequestTimeoutHandler {
	        
	        private int countRequestTimeout = 0;
	        
	        public void onRequest(IHttpExchange exchange) throws IOException {
	            exchange.send(new HttpResponse(200));
	        }
	        
	        public boolean onRequestTimeout(IHttpConnection connection) throws IOException {
	            countRequestTimeout++;
	            return true;
	        }
	        
	        
	        int getCountRequestTimeout() {
	            return countRequestTimeout;
	        }
	    }
	
	   
	private static final class ServerHandler2 implements IHttpRequestHandler, IHttpRequestTimeoutHandler {
		
		private int countRequestTimeout = 0;
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			final BodyDataSink outChannel = exchange.send(new HttpResponseHeader(200, "text/html"));
			
			Thread t = new Thread() {
				
				@Override
				public void run() {
					try {
						for (int i = 0; i < WAIT_LOOPS; i++) {
							outChannel.write("data");
							QAUtil.sleep(WAIT_TIME);
						}
						
						outChannel.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};

			t.start();
		}
		
		public boolean onRequestTimeout(IHttpConnection connection) throws IOException {
			countRequestTimeout++;
			return true;
		}
		
		
		int getCountRequestTimeout() {
			return countRequestTimeout;
		}
	}

}
