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
 * call back interface to be notified if the response has been received <br><br>
 * 
 * Example:
 * <pre>
 *  ResponseHandler implements IResponseHandler {
 *  
 *     public void onResponse(IHttpResponse response) throws IOException {
 *        status = response.getStatus();
 *        ...
 *     }
 *     
 *     public void void onException(IOException ioe) {
 *        ...
 *     }
 *  }
 * </pre> 
 * 
 * @author grro@xlightweb.org
 */
public interface IHttpResponseHandler {
		
	public static final int DEFAULT_EXECUTION_MODE = Execution.MULTITHREADED;
	public static final int DEFAULT_INVOKE_ON_MODE = InvokeOn.HEADER_RECEIVED;
	
	/**
	 * call back method which will be called if the response is received
	 * 
	 * @param response the response
	 * @throws IOException
	 */
	void onResponse(IHttpResponse response) throws IOException;
	
	
	
	/**
	 * call back method which will be called if an io exception occurs 
	 * 
	 * @param ioe the io exception
	 * @throws IOException
	 */
	void onException(IOException ioe) throws IOException;
	
}
