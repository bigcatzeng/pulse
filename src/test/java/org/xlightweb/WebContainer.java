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


import javax.servlet.Servlet;


import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;



  

/**
* 
* WebContainer for test purposes 
*  
* @author grro@xlightweb.org
*/
public final class WebContainer  {

	
	private int port = 0;
	private Servlet servlet = null;
	private Server jettyServer = null;
	private String servletPath = "";
	private String contextPath = "";
	
	
	public WebContainer(Servlet servlet) {
		this(servlet, "");
	}
	
	public WebContainer(Servlet servlet, String servletPath) {
		this.servlet = servlet;
		this.servletPath = servletPath;
	}

	public WebContainer(Servlet servlet, String servletPath, String contextPath) {
		this.servlet = servlet;
		this.servletPath = servletPath;
		this.contextPath = contextPath;
	}


	public void start() throws Exception {
		jettyServer = new Server(0);
		
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(contextPath);
		jettyServer.setHandler(context);
	 
		context.addServlet(new ServletHolder(servlet), servletPath + "/*");
	        
		jettyServer.start();
		
		port = jettyServer.getConnectors()[0].getLocalPort();
	}
	
	
	public int getLocalPort() {
	    return port;
	}
	

	public void stop() throws Exception {
		jettyServer.stop();
	}	
}
