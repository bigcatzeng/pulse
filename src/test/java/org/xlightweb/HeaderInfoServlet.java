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
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;




/**
*
* @author grro@xlightweb.org
*/
public final class HeaderInfoServlet extends HttpServlet {
	
	private static final long serialVersionUID = 6489398831844159793L;


	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handle("GET", req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handle("POST", req, resp);
	}
	
	
	@SuppressWarnings("unchecked")
	private void handle(String method, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/plain");
		PrintWriter writer = resp.getWriter();
		
		writer.write("method= " + method + "\r\n");

		
		for (Enumeration<String> en = req.getHeaderNames(); en.hasMoreElements(); ) {
			String headername = en.nextElement();
			
			for (Enumeration<String> en2 = req.getHeaders(headername); en2.hasMoreElements(); ) {
				String headervalue = en2.nextElement();
				writer.write("[header] " + headername + ": " + headervalue + "\r\n");
			}
		}
						
		writer.close();
	}
}
