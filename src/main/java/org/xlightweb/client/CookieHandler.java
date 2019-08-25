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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.Supports100Continue;
import org.xsocket.Execution;
import org.xsocket.ILifeCycle;




/**
 * Cookie handler 
 *  
 * @author grro@xlightweb.org
 */
@Supports100Continue
final class CookieHandler implements IHttpRequestHandler, ILifeCycle {
    
    /**
     * CookieHandler is unsynchronized by config. See HttpUtils$RequestHandlerInfo
     */
	
	private static final Logger LOG = Logger.getLogger(CookieHandler.class.getName());

	static final String COOKIE_WARNING_KEY = "org.xlightweb.client.cookieHandler.cookieWarning";
	
	private CookieManager cookieManager = null;
	
	
	public void onInit() {
	    cookieManager = new CookieManager();
	}
	
	public void onDestroy() throws IOException {
		cookieManager.close();		
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void onRequest(final IHttpExchange exchange) throws IOException {

		IHttpRequest request = exchange.getRequest();
		
		try {
			Map<String, List<String>> cookieHeaders = cookieManager.get(getRequestURI(exchange));
			
			
			for (Entry<String, List<String>> entry : cookieHeaders.entrySet()) {
				
				if (!entry.getValue().isEmpty()) {
					StringBuilder sb = new StringBuilder();
					
					List<String> cookies = entry.getValue();
					for (int i = 0; i < cookies.size(); i++) {
						sb.append(cookies.get(i));
						if ((i +1) < cookies.size()) {
							sb.append("; ");
						}
					}
					
					if ((request.getHeader("Cookie") != null) && isCookieWarning(request)) {
						LOG.warning("cookie is set manually and auto handle cookie is activated " +
								    "(hint: deactivate auto handling cookie by calling <httpClient>.setAutoHandleCookies(false) or " +
								    "suppress this message by setting system property 'org.xlightweb.client.cookieHandler.cookieWarning=false')");
					}					
					request.addHeader(entry.getKey(), sb.toString());
				}
			}
		} catch (URISyntaxException ue) {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("invcalid URI. ignore handling cookies " + ue.toString());
			}
		}


		exchange.forward(request, new ResponseHandler(exchange));
	}	

	
    /**
     * ResponseHandler is unsynchronized by config. See HttpUtils$ResponseHandlerInfo
     */
	@Supports100Continue
	private final class ResponseHandler implements IHttpResponseHandler {
        
	    private final IHttpExchange exchange;
	    
	    
	    public ResponseHandler(IHttpExchange exchange) {
	        this.exchange = exchange;
        }
	    
	    
        @Execution(Execution.NONTHREADED)
        public void onResponse(IHttpResponse response) throws IOException {
            
            if (response.getStatus() > 100) {
                Map<String, List<String>> responseHeaders = new HashMap<String, List<String>>(); 
                
                for (String headername : response.getHeaderNameSet()) {
                    responseHeaders.put(headername, response.getHeaderList(headername));
                }
                try {
                    cookieManager.put(getRequestURI(exchange), responseHeaders);
                } catch (URISyntaxException ue) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("invcalid URI. ignore handling cookies " + ue.toString());
                    }
                }
            }

            exchange.send(response);
        }

        @Execution(Execution.NONTHREADED)
        public void onException(IOException ioe) throws IOException {
            exchange.sendError(ioe);
        }
    };
    

	
	private boolean isCookieWarning(IHttpRequest request) {
	    if (request.getAttribute(COOKIE_WARNING_KEY) != null) {
	        return ((Boolean) request.getAttribute(COOKIE_WARNING_KEY)).equals(Boolean.TRUE);
	    }
	    
	    return Boolean.parseBoolean(System.getProperty(COOKIE_WARNING_KEY, "true"));	    
	}
	
	
	private URI getRequestURI(IHttpExchange exchange) throws URISyntaxException {
		return exchange.getRequest().getRequestUrl().toURI();
	}
}
