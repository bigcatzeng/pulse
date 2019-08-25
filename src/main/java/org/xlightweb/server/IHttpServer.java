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
package org.xlightweb.server;


import org.xlightweb.IHttpRequestHandler;
import org.xsocket.connection.IServer;


 


/**
 * A HttpServer definition. 
 * 
 * The http server accepts incoming connections and forwards the request to
 * the assigned {@link IHttpRequestHandler}. Example:
 * 
 * <pre>
 * 
 *  // defining a request handler 
 *  class MyRequestHandler implements IHttpRequestHandler  {
 *  
 *     public void onRequest(IHttpExchange exchange) throws IOException {
 *        IHttpRequest request = exchange.getRequest();
 *        //...
 *        
 *        exchange.send(new HttpResponse(200, "text/plain", "it works"));
 *     }
 *  } 
 *
 * 
 *  // creates a server
 *  IServer server = new HttpServer(8080, new MyRequestHandler());
 * 
 *  // setting some properties 
 *  server.setMaxTransactions(400);
 *  server.setRequestTimeoutMillis(5 * 60 * 1000);  
 *  //...
 * 
 * 
 *  // executing the server 
 *  server.run();  // (blocks forever)
 * 
 *  // or server.start();  (blocks until the server has been started)
 *  //...
 * </pre> 
 *
 * 
 * @author grro@xlightweb.org
 */
public interface IHttpServer extends IServer {

    /**
     * returns the request handler 
     * 
     * @return the request handler
     */
    IHttpRequestHandler getRequestHandler();
    
    
	/**
	 * set if the request will be uncompressed (if compressed)
	 *  
	 * @param isAutoUncompress true, if the request will be uncompressed (if compressed)
	 */
	void setAutoUncompress(boolean isAutoUncompress);

	
	/**
	 * return true, if the request will be uncompressed (if compressed)
	 * 
	 * @return true, if the request will be uncompressed (if compressed)
	 */
	boolean isAutoUncompress();
	
	
	
	/**
	 * set is if the server-side connection will closed, if an error message (4xx or 5xx) is sent
	 * 
	 * @param isCloseOnSendingError if the connection will closed, if an error message is sent
	 */
	void setCloseOnSendingError(boolean isCloseOnSendingError);
	
	
	/**
	 * returns if the server-side connection will closed, if an error message (4xx or 5xx) will be sent
	 * @return true, if the connection will closed by sending an error message 
	 */
	boolean isCloseOnSendingError();
	
	
	/**
	 * sets if cookies is used for session state management 
	 *  
	 * @param useCookies true, if cookies isused for session state management 
	 */
	void setUsingCookies(boolean useCookies);
	

	/**
	 * returns true, if cookies is used for session state management
	 * 
	 * @return true, if cookies is used for session state management
	 */
	boolean isUsingCookies();
	
	
	/**
	 * sets the autocompress threshold of responses. Setting the threshold to Integer.MAX_VALUE deactivates autocompressing
	 * 
	 * @param autocompressThresholdBytes the autocompress threshold
	 */
	void setAutoCompressThresholdBytes(int autocompressThresholdBytes);
	
	
	/**
	 * gets the autocompress threshold of responses
	 * 
	 * @return the autocompress threshold
	 */
	int getAutoCompressThresholdBytes();
	
	

	/**
	 * set the body data receive timeout
	 * 
	 * @param bodyDataReceiveTimeoutSec the timeout
	 */
	void setBodyDataReceiveTimeoutMillis(long bodyDataReceiveTimeoutMillis);

	
	/**
	 * get the body data receive timeout
	 * 
	 * @return the timeout
	 */
	long getBodyDataReceiveTimeoutMillis();

	
    /**
     * sets the session max inactive interval in seconds
     *  
     * @param sessionMaxInactiveIntervalSec the session max inactive interval in seconds
     */
    void setSessionMaxInactiveIntervalSec(int sessionMaxInactiveIntervalSec);
    

    
    /**
     * gets the session max inactive interval in seconds
     * 
     * @return the session max inactive interval in seconds
     */
    int getSessionMaxInactiveIntervalSec();
	
    
	/**
	 * set the max transactions per connection. Setting this filed causes that 
	 * a keep-alive response header will be added 
	 * 
	 * @param maxTransactions  the max transactions 
	 */
	void setMaxTransactions(int maxTransactions);
	   
	
    /**
     * get the max transactions per connection. Setting this filed causes that 
     * a keep-alive response header will be added 
     * 
     * @return the max transactions 
     */
    int getMaxTransactions();
    
    
	/**
     * set the request body default encoding. According to RFC 2616 the 
     * initial value is ISO-8859-1 
     *   
     * @param encoding  the defaultEncoding 
     */
    void setRequestBodyDefaultEncoding(String defaultEncoding);


	/**
     * get the request body default encoding. According to RFC 2616 the 
     * initial value is ISO-8859-1 
     *   
     * @return the defaultEncoding 
     */
    String getRequestBodyDefaultEncoding();
    
    
	/**
	 * sets the session manager
	 * 
	 * @param sessionManager the session manager
	 */
	void setSessionManager(ISessionManager sessionManager);


	/**
	 * returns the session manager
	 * 
	 * @return the session manager
	 */
	ISessionManager getSessionManager();
	
	
    /**
     * gets the message receive timeout
     * 
     * @return the message receive timeout
     */
    long getRequestTimeoutMillis();
    
    /**
     * sets the message receive timeout
     * 
     * @param timeoutMillis the message receive timeout
     */
    void setRequestTimeoutMillis(long timeoutMillis);    
	
}
