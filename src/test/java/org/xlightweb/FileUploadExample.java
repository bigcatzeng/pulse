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

import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IServer;





/**
*
* @author grro@xlightweb.org
*/
public final class FileUploadExample  {
	
	
	public static void main(String[] args) throws IOException {
		
		IServer server = new HttpServer(new FileUploadHandler());
		server.run();
		
	}
	
	
	
	public static final class FileUploadHandler implements IHttpRequestHandler {
			
		public void onRequest(IHttpExchange exchange) throws IOException {
			
			IHttpRequest request = exchange.getRequest();
			if (request.getMethod().equals("GET")) {
				
				String txt = "<form method=\"post\" enctype=\"multipart/form-data\" action=\"test\">\r\n"+
                             "  <input type=\"text\" name=\"input_text\" value=\"mytext\"><br>\r\n" +
                             "  <input type=\"file\" name=\"file\"><br>\r\n" + 
                             "  <input type=\"submit\">\r\n" +
                             "</form>";
				
				exchange.send(new HttpResponse(200, "text/html", txt));
				
			} else {
				System.out.println("STOP");
			}
		}
	}
}