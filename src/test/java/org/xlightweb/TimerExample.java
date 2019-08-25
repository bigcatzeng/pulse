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
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IServer;



/**
*
* @author grro@xlightweb.org
*/
public final class TimerExample  {
	
	
	public static void main(String... args) throws IOException {
		
		Context rootCtx = new Context("");
		rootCtx.addHandler("/service/*", new TimerHandler());
		
		String basePath = TimerExample.class.getResource("").getFile();
		rootCtx.addHandler("/*", new FileServiceRequestHandler(basePath, true));
		
		IServer server = new HttpServer(9091, rootCtx);
		server.run();
	}

	
  
	private static final class TimerHandler implements IHttpRequestHandler {
        
		private final Timer timer = new Timer("timer", true);
		        
		        
		public void onRequest(IHttpExchange exchange) throws IOException {
		  
			// handle the time request
			if (exchange.getRequest().getRequestURI().endsWith("/time")) {
		                
				// write the message header by retrieving the body handle	
				final BodyDataSink outChannel = exchange.send(new HttpResponseHeader(200, "text/html"));

				// timer task definition                  
				TimerTask timerTask = new TimerTask() {
		                
		            public void run() {
		               try {
		                  String script = "<script>\r\n" +
		                                  "  parent.printTime(\"" + new Date().toString() + "\");\r\n" +
		                                  "</script>";
		                  outChannel.write(script);
		               } catch (IOException ioe) {
		                  cancel();
		                  try {
		                     outChannel.close();
		                  } catch (IOException ignore) { }
		               }
		            }      
		         };
		         
		         
		         // start the timer task 
		         timer.schedule(timerTask, 0, 1000);
			}
		}
	}
		
}