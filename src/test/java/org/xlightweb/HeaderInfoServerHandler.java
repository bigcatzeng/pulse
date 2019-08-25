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
import java.util.Enumeration;

import org.xsocket.Execution;



/**
*
* @author grro@xlightweb.org
*/
public final class HeaderInfoServerHandler implements IHttpRequestHandler {


	@SuppressWarnings("unchecked")
	@Execution(Execution.NONTHREADED)
	public void onRequest(IHttpExchange exchange) throws IOException {

		IHttpRequest request = exchange.getRequest();
		StringBuilder sb = new StringBuilder();
		sb.append("method= " + request.getMethod() + "\r\n");

		for (Enumeration<String> en = request.getHeaderNames(); en.hasMoreElements(); ) {
			String headername = en.nextElement();

			for (Enumeration<String> en2 = request.getHeaders(headername); en2.hasMoreElements(); ) {
				String headervalue = en2.nextElement();
				sb.append("[header] " + headername + ": " + headervalue + "\r\n");
			}
		}
		
		exchange.send(new HttpResponse(200, "text/plain", sb.toString()));
	}
}
