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
import java.util.UUID;

import org.xlightweb.server.HttpServer;



 
/**
 *
 * @author grro@xlightweb.org
 */
public final class OpenIdExample  {

	
    public static void main(String[] args) throws Exception {

	    HttpServer server = new HttpServer(9595, new RequestHandler());
	    server.run();
	}
	
	
	private static final class RequestHandler implements IHttpRequestHandler {
	    
	    
	    private final String id = UUID.randomUUID().toString();
	    
	    
	    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {

	        IHttpRequest req = exchange.getRequest();
	        
	        
	        if (req.getRequestURI().equals("/")) {
    	        String loginSite = "<html>\r\n" +
    	                           " <body>\r\n" +
    	                           "  <h1>Login OpenIdURL</h1>\r\n" +
    	                           "  <form action=\"logon\" method=\"post\">\r\n" +
    	                           "    <p>OpenId<br><input name=\"openidUrl\" type=\"text\" size=\"100\" maxlength=\"500\"></p>\r\n" +
    	                           "    <input type=\"submit\" value=\"login\">\r\n" +
    	                           "  </form>\r\n" +
    	                           " </body>\r\n" +
    	                           "</html>\r\n";
    	     
    	        exchange.send(new HttpResponse(200, "text/html", loginSite));
    	        
	        } else if (req.getRequestURI().equals("/logon")) {
	            String openidURL = req.getParameter("openidUrl");
	            if (!openidURL.toLowerCase().startsWith("http://")) {
	                openidURL = "http://" + openidURL;
	            }
	            
	            
	            IHttpRequest request  = new GetRequest("http://pip.verisignlabs.com/server", 
	            		                               new NameValuePair("openid.ns", "http://specs.openid.net/auth/2.0"),
	            		                               new NameValuePair("openid.mode", "checkid_setup"),
	            		                               new NameValuePair("openid.identity", openidURL),
	            		                               new NameValuePair("openid.claimed_id", openidURL),
	            		                               new NameValuePair("openid.assoc_handle", id),
	            		                               new NameValuePair("openid.return_to", "http://xlightweb.org/authVerify"),
	            		                               new NameValuePair("openid.realm", "http://xlightweb.org"),
	            		                               new NameValuePair("openid.ns.sreg", "http://openid.net/extensions/sreg/1.1"));
	            
	            exchange.sendRedirect(request.getRequestUrl().toString());

            } else if (req.getRequestURI().equals("/authVerify")) {
         
                String page;
                
                String error = req.getParameter("openid.error");
                if (error != null) {
                    page = " <body>\r\n" +
                           "  <h1>Error</h1>\r\n" +
                           error + " \r\n" +
                           " </body>\r\n" +
                           "</html>\r\n";
                } else {
                    
                    String refId = req.getParameter("openid.assoc_handle");
                    if (refId != id) {
                        exchange.sendError(403);
                        return;
                        
                    } else {
                        page = "<html>\r\n" +
                               " <body>\r\n" +
                               "  <h1>Success</h1>\r\n" +
                               " login passed\r\n" +
                               " </body>\r\n" +
                               "</html>\r\n";
                    }
                }
                
                exchange.send(new HttpResponse(200, "text/html", page));
	        } else {
	            exchange.sendError(400);
	        }
	    }
	}
}
