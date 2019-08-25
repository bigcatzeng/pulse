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
import java.net.ConnectException;

import org.xsocket.IDestroyable;





/**
 * This class encapsulates a HTTP request received and a
 * response to be generated in one exchange. It provides methods  <br><br>
 * <ul>
 *   <li>to retrieve and forward the request (within the local chain), and</li>
 *   <li>to send the response</li>
 * </ul>
 *
 * For an example see {@link IHttpRequestHandler}
 *
 * @author grro@xlightweb.org
 */
public interface IHttpExchange extends IDestroyable {
	
	
	/**
	 * System property key to define if detailed error should been shown in error page
	 */
	public static final String SHOW_DETAILED_ERROR_KEY = "org.xlightweb.showDetailedError";
	public static final String SHOW_DETAILED_ERROR_DEFAULT = "false";

	
	
	/**
	 * get the request of this exchange
	 * 
	 * @return the request
	 */
	IHttpRequest getRequest();

	
	
	/**
	 * returns the underlying connection
	 *
	 * @return the connection
	 */
	IHttpConnection getConnection();
	
	
	

	/**
	 * send the response. If the content-length header is not set, the message will send 
	 * in chunked mode. This send method will be used if the body  size is unknown by sending the header. <br><br>
	 * 
	 * By calling the {@link IHttpMessage#getProtocolVersion()} method of the request, it can be verified, if the requestor
	 * supports a chunked responses (HTTP/1.0 request) <br><br>
     * 
     * <ul>
     *   <li>Connection-LifeCycle support: If the request header contains a 'Connection: close' entry, the connection will be closed after the HTTP transaction</li>
     * </ul>
	 *
	 * @param header   the header
	 * @return the body handle to write
	 * 
	 * @throws IOException if an exception occurs
	 * @throws IllegalStateException if a response has already been send 
	 */
	BodyDataSink send(IHttpResponseHeader header) throws IOException, IllegalStateException;
	
	
	/**
	 * send the response in a plain body non-blocking mode. For performance reasons this is 
	 * the preferred send method <br><br>
     * 
     * <ul>
     *   <li>Connection-LifeCycle support: If the request header contains a 'Connection: close' entry, the connection will be closed after the HTTP transaction</li>
     * </ul> 
	 *
	 * @param header         the header
	 * @param contentLength  the body content length
	 * @return the body handle to write
	 * @throws IOException if an exception occurs
	 * @throws IllegalStateException if a response has already been send 
	 */
	BodyDataSink send(IHttpResponseHeader header, int contentLength) throws IOException, IllegalStateException;
	

	/**
	 * send the response. <br><br>
     * 
     * <ul>
     *   <li>Connection-LifeCycle support: If the request header contains a 'Connection: close' entry, the connection will be closed after the HTTP transaction</li>
     * </ul> 
	 *
	 * @param response   the response
	 * @throws IOException if an exception occurs
	 * @throws IllegalStateException if a response has already been send 
	 */
	void send(IHttpResponse response) throws IOException, IllegalStateException;
	

	/**
	 * send an error response
	 *
	 * @param errorCode   the error code
	 * @param msg         the error message
	 * @throws IllegalStateException if a response has already been send 
	 */
	void sendError(int errorCode, String msg) throws IllegalStateException;


	/**
	 * send an error response
	 *
	 * @param errorCode   the error code
	 * @param msg         the error message
	 * @throws IllegalStateException if a response has already been send 
	 */
	void sendError(int errorCode) throws IllegalStateException;

	
	/**
	 * send an error response
	 *
	 * @param e  the exception
	 * @throws IllegalStateException if a response has already been send 
	 */
	void sendError(Exception e) throws IllegalStateException;

	
	
	/**
	 * Sends a temporary redirect response using the specified redirect location URL. 
	 *
	 * @param location the redirect location URL 
     * @throws IllegalStateException if a response has already been send  
	 */
	void sendRedirect(String location) throws IllegalStateException;
	

	/**
	 * forwards the a request. If the content-length header is not set, the message will send 
	 * in chunked mode. Forwarding will be used to forward the request to the next handler of the chain.
	 * If no successor handler exits an error response will be returned to the client. For an example see {@link RequestHandlerChain} 
	 *  
	 * This method should only be used if the original request supports chunked responses, which can be check by request.supportsChunkedResponse()
	 *     
	 * @param requestHeader    the request header
	 * @param responseHandler  the response handler or <code>null</code> (supported: {@link IHttpRequestHandler}, {@link IHttpSocketTimeoutHandler})
	 * @return the body handle to write
	 * @throws ConnectException if an error occurred while attempting to connect to a remote address and port. 
	 * @throws IOException if an exception occurs	
	 * @throws IllegalStateException if a request has already been forwarded
	 */
	BodyDataSink forward(IHttpRequestHeader requestHeader, IHttpResponseHandler responseHandler) throws IOException, ConnectException, IllegalStateException;

	
	
	/**
	 * forwards the a request. If the content-length header is not set, the message will send 
	 * in chunked mode. Forwarding will be used to forward the request to the next handler of the chain.
	 * If no successor handler exits an error response will be returned to the client. For an example see {@link RequestHandlerChain} 
	 *  
	 * This method should only be used if the original request supports chunked responses, which can be check by request.supportsChunkedResponse()
	 *     
	 * @param requestHeader    the request header
	 * @return the body handle to write
	 * @throws ConnectException if an error occurred while attempting to connect to a remote address and port. 
	 * @throws IOException if an exception occurs	
	 * @throws IllegalStateException if a request has already been forwarded
	 */
	BodyDataSink forward(IHttpRequestHeader requestHeader) throws IOException, ConnectException, IllegalStateException;
 

	/**
	 * forwards a request. The request will be send in a plain body mode. Forwarding will be used to forward the request to the next handler of the chain.
	 * If no successor handler exits an error response will be returned to the client. For an example see {@link RequestHandlerChain}
	 *
	 * @param requestHeader     the request header
	 * @param contentLength     the content length
	 * @param responseHandler   the response handler or <code>null</code> (supported: {@link IHttpRequestHandler}, {@link IHttpSocketTimeoutHandler})
	 * @return the body handle to write
	 * @throws ConnectException if an error occurred while attempting to connect to a remote address and port. 
	 * @throws IOException if an exception occurs
	 * @throws IllegalStateException if a request has already been forwarded 
	 */	 
	BodyDataSink forward(IHttpRequestHeader requestHeader, int contentLength, IHttpResponseHandler responseHandler) throws IOException, ConnectException, IllegalStateException;


	
	/**
	 * forwards a request. The request will be send in a plain body mode. Forwarding will be used to forward the request to the next handler of the chain.
	 * If no successor handler exits an error response will be returned to the client. For an example see {@link RequestHandlerChain}
	 *
	 * @param requestHeader     the request header
	 * @param contentLength     the content length
	 * @return the body handle to write
	 * @throws ConnectException if an error occurred while attempting to connect to a remote address and port. 
	 * @throws IOException if an exception occurs
	 * @throws IllegalStateException if a request has already been forwarded 
	 */	 
	BodyDataSink forward(IHttpRequestHeader requestHeader, int contentLength) throws IOException, ConnectException, IllegalStateException;

	
	
	
	/**
	 * forwards a request locally. Forwarding will be used to forward the request to the next handler of the chain.
	 * If no successor handler exits an error response will be returned to the client. For an example see {@link RequestHandlerChain}     
	 *  
	 * @param request           the request
	 * @throws ConnectException if an error occurred while attempting to connect to a remote address and port. 
	 * @throws IOException if an exception occurs
	 * @throws IllegalStateException if a request has already been forwarded 
	 */
	void forward(IHttpRequest request) throws IOException, ConnectException, IllegalStateException;
	

	/**
	 * forwards a request. Forwarding will be used to forward the request to the next handler of the chain.
	 * If no successor handler exits an error response will be returned to the client. For an example see {@link RequestHandlerChain} 
	 *  
	 * @param request           the request
	 * @param responseHandler   the response handler or <code>null</code> (supported: {@link IHttpRequestHandler}, {@link IHttpSocketTimeoutHandler})
	 * @throws ConnectException if an error occurred while attempting to connect to a remote address and port. 
	 * @throws IOException if an exception occurs
	 * @throws IllegalStateException if a request has already been forwarded 
	 */
	void forward(IHttpRequest request, IHttpResponseHandler responseHandler) throws IOException, ConnectException, IllegalStateException;

	
	
	/**
	 * send a 100 Continue response if the request contains a Expect: 100-Continue header. The 100 
	 * response will be send once, even though in the case of a repeated sendContinue() call
	 * 
	 * @return true if a 100 Continue response has been sent
     * @throws IOException if an exception occurs
	 */
	boolean sendContinueIfRequested() throws IOException ;
	
	
	/**
	 * Returns the current HttpSession associated with this exchange or, if there is no current session and create is true, returns a new session.
	 * If create is false and the exchange has no valid HttpSession, this method returns null.
	 * 
	 * @param create    true to create a new session if necessary; false to return null if there's no current session 
	 * @return   the HttpSession associated with this exchange or null if create is false and the request has no valid session
	 */
	IHttpSession getSession(boolean create);
	


	/**
	 * Encodes the specified URL by including the session ID in it, or, if encoding 
	 * is not needed, returns the URL unchanged. The implementation of this 
	 * method includes the logic to determine whether the session ID needs to 
	 * be encoded in the URL. For example, if the browser supports cookies, 
	 * or session tracking is turned off, URL encoding is unnecessary.
	 * 
	 * @param url   the url to be encoded.
	 * @return the encoded URL if encoding is needed; the unchanged URL otherwise.
	 */
	String encodeURL(String url);

	
	
	
	/**
	 * destroy the exchange and the underlying connection 
	 */
	void destroy();
	
	
	   
    /**
     * @deprecated
     */
    boolean sendContinue() throws IOException ;
    
}
