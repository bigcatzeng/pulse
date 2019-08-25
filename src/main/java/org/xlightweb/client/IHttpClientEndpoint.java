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
package org.xlightweb.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import org.xlightweb.BodyDataSink;
import org.xlightweb.IFutureResponse;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpRequestHeader;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.IHttpSocketTimeoutHandler;




/**
 * Represents the client side endpoint of a http connection
 *
 *
 * @author grro@xlightweb.org
 */
public interface IHttpClientEndpoint extends Closeable {
	
	public static final boolean DEFAULT_AUTOHANDLE_100CONTINUE_RESPONSE = true;
	
	

	/**
	 * set the response time out by performing the call or send method. 
	 *
	 * @param responseTimeout  the response timeout
	 */
	public void setResponseTimeoutMillis(long responseTimeout);


	/**
	 * returns the response timeout
	 *
	 * @return the response timeout
	 */
	public long getResponseTimeoutMillis();


	/**
	 * performs a request. This method blocks until the response (header)
	 * is received. <br><br>
     * 
     * <ul>
     *   <li>Connection-LifeCycle support: If the request header contains a 'Connection: close' entry, the connection will be closed after the HTTP transaction</li>
     * </ul>
	 *
	 * @param request  the request
	 * @return the response
	 * @throws IOException   if an exception occurs
	 * @throws ConnectException if an error occurred while attempting to connect to a remote address and port.
	 * @throws SocketTimeoutException if the received timeout is exceed
	 */
	public IHttpResponse call(IHttpRequest request) throws IOException, ConnectException, SocketTimeoutException;

	/**
	 * send the request. If the content-length header is not set, the message will send 
	 * in chunked mode<br><br>
     * 
     * <ul>
     *   <li>Connection-LifeCycle support: If the request header contains a 'Connection: close' entry, the connection will be closed after the HTTP transaction</li>
     * </ul> 
	 *
	 * @param requestHeader    the request header
	 * @param responseHandler  the response handler or <code>null</code> (supported: {@link IHttpRequestHandler}, {@link IHttpSocketTimeoutHandler})
	 * @return the body handle to write
	 * @throws ConnectException if an error occurred while attempting to connect to a remote address and port. 
	 * @throws IOException if an exception occurs
	 */
	public BodyDataSink send(IHttpRequestHeader requestHeader, IHttpResponseHandler responseHandler) throws IOException, ConnectException;


 
	/**
	 * send the request in a plain body mode<br><br>
     * 
     * <ul>
     *   <li>Connection-LifeCycle support: If the request header contains a 'Connection: close' entry, the connection will be closed after the HTTP transaction</li>
     * </ul>
	 *
	 * @param requestHeader     the request header
	 * @param contentLength     the content length
	 * @param responseHandler   the response handler or <code>null</code> (supported: {@link IHttpRequestHandler}, {@link IHttpSocketTimeoutHandler})
	 * @return the body handle to write
	 * @throws ConnectException if an error occurred while attempting to connect to a remote address and port. 
	 * @throws IOException if an exception occurs
	 */
	public BodyDataSink send(IHttpRequestHeader requestHeader, int contentLength, IHttpResponseHandler responseHandler) throws IOException, ConnectException;


	/**
	 * send the request. <br><br>
	 * 
	 * <ul>
	 *   <li>Connection-LifeCycle support: If the request header contains a 'Connection: close' entry, the connection will be closed after the HTTP transaction</li>
     *   <li>Continue support: If the request header contains a 'Expect: 100-continue' entry, the body will be buffered internal and transmitted after receiving the server's 100-continue response</li>
	 * </ul>
	 *
	 * @param request           the request
	 * @param responseHandler   the response handler or <code>null</code> (supported: {@link IHttpRequestHandler}, {@link IHttpSocketTimeoutHandler})
	 * @throws ConnectException if an error occurred while attempting to connect to a remote address and port. 
	 * @throws IOException if an exception occurs
	 */
	public void send(IHttpRequest request, IHttpResponseHandler responseHandler) throws IOException, ConnectException;

	
	/**
     * send the request. <br><br>
     * 
     * <ul>
     *   <li>Connection-LifeCycle support: If the request header contains a 'Connection: close' entry, the connection will be closed after the HTTP transaction</li>
     *   <li>Continue support: If the request header contains a 'Expect: 100-continue' entry, the body will be buffered internal and transmitted after receiving the server's 100-continue response</li>
     * </ul>
     *
     * @param request           the request
     * @param response          the response
     * @throws ConnectException if an error occurred while attempting to connect to a remote address and port. 
     * @throws IOException if an exception occurs
     */
    public IFutureResponse send(IHttpRequest request) throws IOException, ConnectException;

    
    
	/**
	 * returns the id
	 *
	 * @return id
	 */
	public String getId();
}
