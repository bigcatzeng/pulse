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

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * workaround for JSE5 environment (will be replaced by JSE6 HttpCookie)
 */
final class Cookie {

	private static final Logger LOG = Logger.getLogger(Cookie.class.getName());
	
  
	private final String name; 
	private String value;
    private String domain;     
    private long maxAge = -1; 
    private String path;       
    private String portlist;   
    private boolean secure;    
    private int version = 1;   

    private final long creationTime;



    
    public Cookie(String name, String value) {
    	creationTime = System.currentTimeMillis();

    	name = name.trim();
    	this.name = name;
    	this.value = value;
    	secure = false;
    	portlist = null;
    }


    
    public static List<Cookie> parse(String header) {
    	int version = retrieveVersion(header);

   		if (startsWithIgnoreCase(header, "set-cookie2:")) {
            header = header.substring("set-cookie2:".length());
            
        } else if (startsWithIgnoreCase(header, "set-cookie:")) {
            header = header.substring("set-cookie:".length());
        }


        List<Cookie> cookies = new ArrayList<Cookie>();
        if (version == 0) {
            Cookie cookie = parseHeader(header);
            cookie.setVersion(0);
            cookies.add(cookie);
            
        } else {
            for (String cookieString : splitCookies(header)) {
                Cookie cookie = parseHeader(cookieString);
                cookie.setVersion(1);
                cookies.add(cookie);
            }
        }

        return cookies;
    }

    
    
    private static List<String> splitCookies(String header) {
    	List<String> cookies = new ArrayList<String>();
    	int quoteCount = 0;
    	int p, q;

    	for (p = 0, q = 0; p < header.length(); p++) {
    		char c = header.charAt(p);
    		if (c == '"') {
    			quoteCount++;
    		}
    		
    		if ((c == ',') && (quoteCount % 2 == 0)) {
    			cookies.add(header.substring(q, p));
    			q = p + 1;
    		}
    	}

    	cookies.add(header.substring(q));

    	return cookies;
    }	
    
    
    private static int retrieveVersion(String header) {
    	int version = 0;

    	header = header.toLowerCase();
    	if (header.indexOf("expires=") != -1) {
    		version = 0;
    		
    	} else if (header.indexOf("version=") != -1) {
    		version = 1;
    		
    	} else if (header.indexOf("max-age") != -1) {
    		version = 1;
    		
    	} else if (startsWithIgnoreCase(header, "set-cookie2:")) {
    		version = 1;
    	}

    	return version;
    }



    public boolean hasExpired() {
        if (maxAge == 0) {
        	return true;
        }

        if (maxAge == -1) {
        	return false;
        }

        long diffSec = (System.currentTimeMillis() - creationTime) / 1000;
        if (diffSec > maxAge) {
            return true;
            
        } else {
            return false;
        }
    }

    public void setPortlist(String ports) {
    	portlist = ports;
    }


    public String getPortlist() {
    	return portlist;
    }

    public String getDomain() {
        return domain;
    }


    public void setMaxAge(long expiry) {
    	maxAge = expiry;
    }


    public long getMaxAge() {
    	return maxAge;
    }


    public void setPath(String uri) {
    	path = uri;
    }

    public String getPath() {
    	return path;
    }


    public void setSecure(boolean flag) {
    	secure = flag;
    }


    public boolean getSecure() {
    	return secure;
    }


    public String getName() {
    	return name;
    }

    
    public void setValue(String newValue) {
    	value = newValue;
    }

    
    public String getValue() {
    	return value;
    }


    public int getVersion() {
    	return version;
    }

    public void setVersion(int version) {
    	this.version = version;
    }

    
    
    static boolean matches(String domain, String host) {
    	if (domain == null || host == null) {
    		return false;
    	}

        boolean isLocalDomain = ".local".equalsIgnoreCase(domain);
        int embeddedDotInDomain = domain.indexOf('.');
        if (embeddedDotInDomain == 0) {
        	embeddedDotInDomain = domain.indexOf('.', 1);
        }
	     
        if (!isLocalDomain && (embeddedDotInDomain == -1 || embeddedDotInDomain == domain.length() - 1)) {
        	return false;
        }

        int firstDotInHost = host.indexOf('.');
        if (firstDotInHost == -1 && isLocalDomain) {
        	return true;
        }

        int domainLength = domain.length();
        int lengthDiff = host.length() - domainLength;
        if (lengthDiff == 0) {
        	return host.equalsIgnoreCase(domain);
        	
        } else if (lengthDiff > 0) {
        	String s1 = host.substring(0, lengthDiff);
        	String s2 = host.substring(lengthDiff);

        	return ((s1.indexOf('.') == -1) && s2.equalsIgnoreCase(domain));
        	
        } else if (lengthDiff == -1) {
        	return ((domain.charAt(0) == '.') && host.equalsIgnoreCase(domain.substring(1)));
        }

        return false;
    }


    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();

    	if (getVersion() > 0) {
        	sb.append(getName()).append("=\"").append(getValue()).append('"');

        	if (getPath() != null)  {
        			sb.append(";$Path=\"").append(getPath()).append('"');
        	}
        	
        	if (getPortlist() != null) {
        		sb.append(";$Port=\"").append(getPortlist()).append('"');
        	}
        	
        	if (getDomain() != null) {
        		sb.append(";$Domain=\"").append(getDomain()).append('"');
        	}
        	        		    		
    	} else {
        	sb.append(getName() + "=" + getValue());
    	}
    	
    	return sb.toString();
    }


    

    private static Cookie parseHeader(String header) {
    	Cookie cookie = null;
    	String namevaluePair = null;

    	StringTokenizer tokenizer = new StringTokenizer(header, ";");

    	try {
    		namevaluePair = tokenizer.nextToken();
    		int index = namevaluePair.indexOf('=');
    		if (index != -1) {
    			String name = namevaluePair.substring(0, index).trim();
    			String value = namevaluePair.substring(index + 1).trim();
    			cookie = new Cookie(name, removeQuote(value));
    		} else {
    			throw new IllegalArgumentException("Invalid cookie name-value pair");
    		}
    	} catch (NoSuchElementException ignored) {
    		return null;
    	}

    	while (tokenizer.hasMoreTokens()) {
    		namevaluePair = tokenizer.nextToken();
    		int index = namevaluePair.indexOf('=');
    		String name, value;
    		if (index != -1) {
    			name = namevaluePair.substring(0, index).trim();
    			value = namevaluePair.substring(index + 1).trim();
    		} else {
    			name = namevaluePair.trim();
    			value = null;
    		}

    		if (name.equalsIgnoreCase("domain")) {
        		value = removeQuote(value);
    			cookie.domain = value;
    			
    		} else if (name.equalsIgnoreCase("max-age")) {
    			try {
    				if (cookie.getMaxAge() == -1) {
    					cookie.setMaxAge(Long.parseLong(value));
    				}
    			} catch (NumberFormatException nfe) { 
    				if (LOG.isLoggable(Level.FINE)) {
    					LOG.fine("error occured by parsing max-age " + nfe.toString());
    				}
    			}
    			
    		} else if (name.equalsIgnoreCase("path")) {
        		value = removeQuote(value);
   				cookie.setPath(value);
   				
    		} else if (name.equalsIgnoreCase("port")) {
    			cookie.setPortlist(value == null ? "" : value);
    			
    		} else if (name.equalsIgnoreCase("secure")) {
    			cookie.setSecure(true);
    		}
    	}

    	return cookie;
    }



    private static String removeQuote(String str) {
    	if (str != null && str.length() > 0 && str.charAt(0) == '"' && str.charAt(str.length() - 1) == '"') {
    		return str.substring(1, str.length() - 1);
    	} else {
    		return str;
    	}
    }

    

    private static boolean startsWithIgnoreCase(String s, String start) {
    	if (s == null || start == null) {
    		return false;
    	}

    	if (s.length() >= start.length() && start.equalsIgnoreCase(s.substring(0, start.length()))) {
    		return true;
    	}

    	return false;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        
        if (!(obj instanceof Cookie)) {
            return false;
        }
        
        Cookie other = (Cookie) obj;
        
        return equalsIgnoreCase(getName(), other.getName()) &&
               equalsIgnoreCase(getDomain(), other.getDomain()) &&
               equals(getPath(), other.getPath());
    }

    
    private boolean equalsIgnoreCase(String s, String t) {
        if (s == t) {
            return true;
        }
        
        if ((s != null) && (t != null)) {
            return s.equalsIgnoreCase(t);
        }
        
        return false;
    }

    private static boolean equals(String s, String t) {
        if (s == t) {
            return true;
        }
        
        if ((s != null) && (t != null)) {
            return s.equals(t);
        }
        
        return false;
    }



    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int h1 = name.toLowerCase().hashCode();
        int h2 = (domain!=null) ? domain.toLowerCase().hashCode() : 0;
        int h3 = (path!=null) ? path.hashCode() : 0;

        return h1 + h2 + h3;
    }
}
