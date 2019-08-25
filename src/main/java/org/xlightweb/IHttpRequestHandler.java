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

import org.xsocket.Execution;



 

/**
 * call back interface which will be notified if a new requests is received. Example:
 * 
 * <pre>
 *  class MyRequestHandler implements IHttpRequestHandler  {
 *  
 *     public void onRequest(IHttpExchange exchange) throws IOException {
 *        IHttpRequest request = exchange.getRequest();
 *        //...
 *        
 *        exchange.send(new HttpResponse(200, "text/plain", "OK"));
 *     }
 *  }
 * </pre> 
 *
 * @author grro@xlightweb.org
 */
public interface IHttpRequestHandler extends IWebHandler {

	public static final int DEFAULT_EXECUTION_MODE = Execution.MULTITHREADED;
	public static final int DEFAULT_INVOKE_ON_MODE = InvokeOn.HEADER_RECEIVED;
	public static final int DEFAULT_SYNCHRONIZED_ON_MODE = SynchronizedOn.CONNECTION;


	/**
	 * call back method, which will be called if a request message (header) is received
	 * 
	 * @param exchange the exchange contains the request from the client is used to send the response 
	 * @throws IOException if an exception occurred. By throwing this exception an error http response message 
	 *                     will be sent by xSocket, if one or more requests are unanswered. The underlying 
	 *                     connection will be closed
	 * @throws BadMessageException By throwing this exception an error http response message will be sent by xSocket,
	 *                             which contains the exception message. The underlying connection will be closed                
	 */
	void onRequest(IHttpExchange exchange) throws IOException, BadMessageException;
}
