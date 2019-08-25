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

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;





/**
 * workaround for JSE5 environment (will be replaced by JSE6 CookieManager)
 */
final class CookieManager implements Closeable {
	
	private static final Logger LOG = Logger.getLogger(CookieManager.class.getName()); 
	
    private final InMemoryCookieStore cookieStore = new InMemoryCookieStore();

    
    public void close() throws IOException {
    	cookieStore.close();
    }
    
    
    public Map<String, List<String>> get(URI uri) throws IOException {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        
        String path = uri.getPath();
        if ((path == null) || (path.length() == 0)) {
            path = "/";
        }
        

        List<Cookie> cookies = new ArrayList<Cookie>();
        for (Cookie cookie : cookieStore.get(uri)) {

            if (matches(path, cookie.getPath()) && ("https".equalsIgnoreCase(uri.getScheme()) || !cookie.getSecure())) {

                String ports = cookie.getPortlist();
                if ((ports != null) && (ports.length() > 0)) {
                    int port = uri.getPort();
                    if (port == -1) {
                        port = "https".equals(uri.getScheme()) ? 443 : 80;
                    }
                    
                    if (isInList(ports, port)) {
                        cookies.add(cookie);
                    }
                    
                } else {
                    cookies.add(cookie);
                }
            }
        }
        Collections.sort(cookies, new CookiePathComparator());

        
        List<String> cookieHeader = new ArrayList<String>();
        for (Cookie cookie : cookies) {
            if (cookies.indexOf(cookie) == 0 && cookie.getVersion() > 0) {
                cookieHeader.add("$Version=\"1\"");
            }

            cookieHeader.add(cookie.toString());
        }

        result.put("Cookie", cookieHeader);
        
        return Collections.unmodifiableMap(result);
    }

    
    private boolean matches(String path, String pathToMatchWith) {
        
        if ((path == null) || (pathToMatchWith == null)) {
            return false;
        }
        
        if (path.startsWith(pathToMatchWith)) {
            return true;
        }

        return false;
    }
    

    
    public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {

        for (Entry<String, List<String>> entry : responseHeaders.entrySet()) {
            if ((entry.getKey() == null) || !(entry.getKey().equalsIgnoreCase("Set-Cookie2") || entry.getKey().equalsIgnoreCase("Set-Cookie"))) {
                continue;
            }

            for (String headerValue : entry.getValue()) {
                try {
                    for (Cookie cookie : Cookie.parse(headerValue)) {
                        
                        if (cookie.getPath() == null) {
                            String path = uri.getPath();
                            if (!path.endsWith("/")) {
                                int i = path.lastIndexOf("/");
                                
                                if (i > 0) {
                                    path = path.substring(0, i + 1);
                                    
                                } else {
                                    path = "/";
                                }
                            }
                            cookie.setPath(path);
                        }
                        
                        
                        String ports = cookie.getPortlist();
                        if (ports != null) {
                            int port = uri.getPort();
                            if (port == -1) {
                                port = "https".equals(uri.getScheme()) ? 443 : 80;
                            }
                            
                            if (ports.length() == 0) {
                                cookie.setPortlist("" + port);
                                cookieStore.add(uri, cookie);
                                
                            } else {
                                if (isInList(ports, port)) {
                                    cookieStore.add(uri, cookie);
                                }
                            }
                        } else {
                        	cookieStore.add(uri, cookie);
                        }
                    }
                } catch (IllegalArgumentException iae) { 
                	if (LOG.isLoggable(Level.FINE)) {
                		LOG.fine("error occured by parsing cooke " + iae.toString());
                	}
                }
            }
        }
    }

    

    private boolean isInList(String ports, int port) {
    	
    	for (String p : ports.split(",")) {
            try {
                if (Integer.parseInt(p.trim()) == port) {
                    return true;
                }
            } catch (NumberFormatException nfe) { 
            	if (LOG.isLoggable(Level.FINE)) {
            		LOG.fine("error occured by parsing list " + nfe.toString());
            	}
            }
        }
    	
        return false;
    }

       


    
    private static class CookiePathComparator implements Comparator<Cookie>, Serializable {
    	
		private static final long serialVersionUID = -9015607124598271806L;

		public int compare(Cookie c1, Cookie c2) {
            if (c1 == c2) { 
            	return 0;
            }
            
            if (c1 == null) { 
            	return -1;
            }
            
            if (c2 == null) { 
            	return 1;
            }

            if (!c1.getName().equals(c2.getName())) { 
            	return 0;
            }

            if (c1.getPath().startsWith(c2.getPath())) {
                return -1;
                
            } else if (c2.getPath().startsWith(c1.getPath())) {
                return 1;
                
            } else {
                return 0;
            }
        }
    }
}
