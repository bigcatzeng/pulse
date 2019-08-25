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

import org.apache.commons.codec.binary.Base64;



/**
*
* @author grro@xlightweb.org
*/
public class AuthFilter implements IHttpRequestHandler {


	public void onRequest(final IHttpExchange exchange) throws IOException {

	      IHttpRequest request = exchange.getRequest();
	      String authorization = request.getHeader("Authorization");
	      if (authorization != null) {
	         String[] s = authorization.split(" ");
	         if (!s[0].equalsIgnoreCase("BASIC")) {
	         exchange.sendError(401);
	         return;
	      }

	      String decoded = new String(Base64.decodeBase64(s[1].getBytes()));
	      String[] userPasswordPair = decoded.split(":");

	      if (!userPasswordPair[0].equalsIgnoreCase(userPasswordPair[1])) {
	    	  exchange.sendError(401);
	    	  return;
	      }

	      IHttpResponseHandler respHdl = new IHttpResponseHandler() {

	         public void onResponse(IHttpResponse response) throws IOException {
	            exchange.send(response);
	         }

	         public void onException(IOException ioe) {
	            exchange.sendError(500); 
	         }
	      };

	      exchange.forward(exchange.getRequest(), respHdl);
	      return;
	   }


	   String authentication = request.getHeader("Authentication");
	   if (authentication == null) {
	      HttpResponse resp = new HttpResponse(401);
	      resp.setHeader("WWW-Authenticate", "basic");
	      exchange.send(resp);
	   }
	}
} 