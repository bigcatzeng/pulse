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
import java.net.ConnectException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.ClosedChannelException;

import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xlightweb.server.IHttpServer;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.Server;



/**
*
* @author grro@xlightweb.org
*/
public final class ForwardSinkTest  {

 

	@Test
	public void testSimple() throws Exception {
	    
	    
	    IDataHandler dh = new IDataHandler() {
	        
	        public boolean onData(INonBlockingConnection connection) throws IOException {
	            connection.readStringByDelimiter("\r\n\r\n");
	            
	            connection.write("HTTP/1.0 301 Moved Permanently\r\n" +
	                             "Server: server\r\n" +
	                             "Date: Tue, 21 Apr 2009 13:12:37 GMT\r\n" +
	                             "Connection: close\r\n" +
	                             "Content-Type: text/html; charset=utf-8\r\n" +
	                             "Location: http://www.heise.de/newsticker/foren/S-Kinderporno-Sperren-Frontalangriff-auf-die-freie-Kommunikation-befuerchtet/forum-157463/list/\r\n" +
	                             "Vary: User-Agent\r\n" +
	                             "\r\n");
               connection.close();
	            
	            return true;
	        }
	    };
	    
	    
	    Server srv = new Server(dh);
	    srv.start();
	    
	    
	    IHttpRequestHandler forwardHdl = new IHttpRequestHandler() {

	        private HttpClient httpClient = new HttpClient();
	        
	        public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
	            IHttpRequest req = exchange.getRequest();
	            
	            String path = req.getRequestUrl().getFile();
	            URL target = new URL(path);
	            req.setRequestUrl(target);
	            
	            
	            // .. and forward the request
	            try {
	                
	                final IHttpResponseHandler respHdl = new IHttpResponseHandler() {
	                  
	                    public void onResponse(IHttpResponse response) throws IOException {
	                        exchange.send(response);	                        
	                    }
	                    
	                    
	                    public void onException(IOException ioe) throws IOException {
	                        if (ioe instanceof ClosedChannelException) {
	                            exchange.destroy();
	                        } else {
	                            exchange.sendError(500, ioe.toString());
	                        }
	                    }
	                };
	                
	                httpClient.send(req, respHdl);
	            } catch (ConnectException ce) {
	                exchange.sendError(502, ce.getMessage());
	            }
	            
	        }
	    };

	    
	    
        RequestHandlerChain chain = new RequestHandlerChain();
        chain.addLast(new LogFilter());
        chain.addLast(forwardHdl);
        
        IHttpServer proxy = new HttpServer(chain);
        proxy.start();
	    
	    IBlockingConnection con = new BlockingConnection("localhost", proxy.getLocalPort());
	    con.write("GET http://localhost:" + srv.getLocalPort() + "/ HTTP/1.1\r\n" +
	              "Keep-Alive: 300\r\n" +
	              "Host: localhost:" + srv.getLocalPort() + "\r\n" +
	              "User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; de; rv:1.9.0.8) Gecko/2009032609 Firefox/3.0.8\r\n" +
	              "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n" +
	              "Accept-Language: de-de,de;q=0.8,en-us;q=0.5,en;q=0.3\r\n" +
	              "Accept-Encoding: gzip,deflate\r\n" +
	              "Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7\r\n" +
	              "Proxy-Connection: keep-alive\r\n" +
	              "Cookie: RMID=54adb69c49d4fe30; RMFD=011LwE8hO20bs9|O10btZ|O20cCh|O20cFU|P10cIy;\r\n" +
	              " POPUPCHECK=1240399709578; RMFW=011LvnI7710cJZ; RMFM=011LwEEAC20cI1; RMFS=011LwFk0U20Zv3; RMFL=011Lse0NU10cJD|U30cJE; mpt_vid=124006166948487470; mpt_rec_ign=true\r\n" +
	              "\r\n");

	    String header = con.readStringByDelimiter("\r\n\r\n");
	    Assert.assertTrue(header.indexOf("HTTP/1.0 301 Moved Permanently") != -1);
	    
	    con.close();
	    proxy.close();
	    srv.close();
	    
	}
	
	
	
	private static final class LogFilter implements IHttpRequestHandler {
	    
	    
	    public void onRequest(final IHttpExchange exchange) throws IOException {

	        final IHttpRequest request = exchange.getRequest();
	        
	        
	        final IHttpResponseHandler respHdl = new IHttpResponseHandler() {
	            
	            public void onResponse(final IHttpResponse response) throws IOException {
	            
	                if (response.hasBody()) {
	        
	                    if (isBodyPrintable(response)) {
	                        IBodyCompleteListener cl = new IBodyCompleteListener() {
	                            public void onComplete() throws IOException {
	                                if (isDecodeable(response)) {
	                                    System.out.println("\r\n --------- RESPONSE   (decoded body) ---\r\n" + response.getResponseHeader() + "\r\n" + URLDecoder.decode(response.getNonBlockingBody().toString()));
	                                } else { 
	                                    System.out.println("\r\n --------- RESPONSE  ---\r\n" + response.toString());
	                                }
	                                exchange.send(response);
	                            }
	                        };
	                        response.getNonBlockingBody().addCompleteListener(cl);
	                        
	                    } else {
	                        System.out.println("\r\n --------- RESPONSE  (Body is not printed) ---\r\n" + response.getResponseHeader());
	                        exchange.send(response);                        
	                    }
	                    
	                } else {
	                    System.out.println("\r\n --------- RESPONSE ---\r\n" + response);
	                    exchange.send(response);
	                }
	            }
	            
	            public void onException(IOException ioe) {
	                ioe.printStackTrace();
	                exchange.sendError(500);
	            }
	        };
	        

	        if (request.hasBody()) {            
	            if (isBodyPrintable(request)) {
	                
	                IBodyCompleteListener cl = new IBodyCompleteListener() {
	                    public void onComplete() throws IOException {
	                        if (isDecodeable(request)) {
	                            System.out.println("\r\n --------- REQUEST (decoded body) ---\r\n" + request.getRequestHeader() + "\r\n" + URLDecoder.decode(request.getNonBlockingBody().toString()));
	                        } else {
	                            System.out.println("\r\n --------- REQUEST ---\r\n" + request.toString());
	                        }
	                        exchange.forward(exchange.getRequest(), respHdl);
	                    }
	                };
	                request.getNonBlockingBody().addCompleteListener(cl);

	            } else {
	                System.out.println("\r\n --------- REQUEST  (Body is not printed) ---\r\n" + request.getRequestHeader());
	                exchange.forward(exchange.getRequest(), respHdl);
	            }

	            
	        } else {
	            System.out.println("\r\n --------- REQUEST  ---\r\n" + request);
	            exchange.forward(exchange.getRequest(), respHdl);
	        }
	    }   

	    
	    private boolean isBodyPrintable(IHttpMessage message) {
	        if ((message.getCharacterEncoding() == null) || (message.getHeader("Accept-Ranges") != null) || (message.getContentType() == null) || (message.getHeader("Content-Encoding") != null)) {
	            return false;
	        }
	        
	        
	        String contentType = message.getContentType().trim().toLowerCase();
	        return (contentType.startsWith("text") || contentType.startsWith("application/json") || contentType.startsWith("application/x-www-form-urlencoded"));
	    }
	    
	    private boolean isDecodeable(IHttpMessage message) {
	        String contentType = message.getContentType().trim().toLowerCase();
	        return (contentType.startsWith("application/x-www-form-urlencoded"));
	    }
	}   

	
	
	
}
