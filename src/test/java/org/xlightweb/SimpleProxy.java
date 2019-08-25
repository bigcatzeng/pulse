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
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.channels.ClosedChannelException;


import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.ILifeCycle;



/**
*
* @author grro@xlightweb.org
*/
public final class SimpleProxy extends HttpServer {


    public SimpleProxy(int listenport) throws IOException {
		super(listenport, new HttpForwardHandler());

		setMaxTransactions(50);
        setRequestTimeoutMillis(5 * 1000);
    }
        
    

	private static final class HttpForwardHandler implements IHttpRequestHandler, ILifeCycle {

        private HttpClient httpClient;

        
        public void onInit() {
            httpClient = new HttpClient();
        }

        
        public void onDestroy() throws IOException {
            httpClient.close();
        }
        

        public void onRequest(IHttpExchange exchange) throws IOException {

            IHttpRequest req = exchange.getRequest();
            
            // connect request?
            if (req.getMethod().equalsIgnoreCase("CONNECT")) {
                try {
                    HttpUtils.establishTcpTunnel(exchange.getConnection(), req.getRequestURI());
                            
                    IHttpResponse response = new HttpResponse(200);
                    response.getResponseHeader().setReason("Connection established");
                    response.setHeader("Proxy-agent", "myProxy");
                    exchange.send(response);
                            
                } catch (IOException ioe) {
                    exchange.sendError(ioe);
                }
            
                
            // .. no
            } else {
                req.removeHopByHopHeaders();
                req.addHeader("Via", "myProxy");
                
                String path = req.getRequestUrl().getFile();
                
                URL target = new URL(path);
                req.setRequestUrl(target);
            
                try {
                    httpClient.send(req, new HttpReverseHandler(exchange));
                } catch (Exception ce) {
                    exchange.sendError(502, ce.getMessage());
                }
            }
        }
    }

    
    
    
    private static final class HttpReverseHandler implements IHttpResponseHandler, IHttpSocketTimeoutHandler {
        
        private IHttpExchange exchange = null;
        
            
        public HttpReverseHandler(IHttpExchange exchange) {
            this.exchange = exchange;
        }


        @Execution(Execution.NONTHREADED)
        @InvokeOn(InvokeOn.HEADER_RECEIVED)
        public void onResponse(IHttpResponse resp) throws IOException {
            resp.removeHopByHopHeaders();
            resp.addHeader("Via", "myProxy");

            exchange.send(resp);
        }

        @Execution(Execution.NONTHREADED)
        public void onException(IOException ioe) {
            if (ioe instanceof ClosedChannelException) {
                exchange.destroy();
            } else {
                exchange.sendError(ioe);
            }
        }
        
        @Execution(Execution.NONTHREADED)
        public void onException(SocketTimeoutException stoe) {
            exchange.sendError(504, stoe.toString());
        }
    }
}
