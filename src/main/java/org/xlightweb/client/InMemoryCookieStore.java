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
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 *  
 *  In memory cookie store
 *  
 * @author grro@xlightweb.org
 */
final class InMemoryCookieStore implements Closeable {
	
	
	private static final Logger LOG = Logger.getLogger(InMemoryCookieStore.class.getName());

	
	private static final long CLEANING_PERIOD_MILLIS = 30 * 1000;
	private Cleaner cleaner;
	
    private final HashMap<String, List<Cookie>> domainIndex = new HashMap<String, List<Cookie>>();
    private final HashMap<URI, List<Cookie>> uriIndex = new HashMap<URI, List<Cookie>>();

	private final Object guard = new Object();
	
	
		
	
	/**
	 * constructor 
	 */
	public InMemoryCookieStore() {
		cleaner = new Cleaner(this);
		HttpClientConnection.schedule(cleaner, CLEANING_PERIOD_MILLIS, CLEANING_PERIOD_MILLIS);
	}
	
	
	public void close() throws IOException {
	    domainIndex.clear();
	    uriIndex.clear();

		if (cleaner != null) {
			cleaner.close();
		}
	    cleaner = null;
	}
	

	/**
	 * adds a cookie 
	 * 
	 * @param uri     the uri
	 * @param cookie  the cookie
	 */
	public void add(URI uri, Cookie cookie) {
		
		synchronized (guard) {
            if (cookie.getMaxAge() != 0) {
                if (cookie.getDomain() != null) {
                    addIndex(domainIndex, cookie.getDomain(), cookie);
                }
                addIndex(uriIndex, getEffectiveURI(uri), cookie);
            }
        }
	}

	private <T> void addIndex(Map<T, List<Cookie>> indexStore, T index, Cookie cookie) {
		if (index != null) {
			List<Cookie> cookies = indexStore.get(index);
			if (cookies != null) {
				cookies.remove(cookie);

				cookies.add(cookie);
			} else {
				cookies = new ArrayList<Cookie>();
				cookies.add(cookie);
				indexStore.put(index, cookies);
			}
		}
	}
	
	  
		
	private URI getEffectiveURI(URI uri) {
		URI effectiveURI = null;
		try {
			effectiveURI = new URI("http", uri.getHost(), null, null, null );
		} catch (URISyntaxException ignored) {
			effectiveURI = uri;
		}

		return effectiveURI;
	}

		
	
	/**
	 * returns the cookies for the uri
	 * 
	 * @param uri  the uri
	 * @return the cookies
	 */
	public List<Cookie> get(URI uri) {
		List<Cookie> cookies = new ArrayList<Cookie>();
		
		boolean secureLink = "https".equalsIgnoreCase(uri.getScheme());
	    
		synchronized (guard) {
            fetchFromDomainIndex(cookies, uri.getHost(), secureLink);
            fetchFromUriIndex(cookies, getEffectiveURI(uri), secureLink);
		}
		
		return cookies;
	}
	
	
	 private void fetchFromDomainIndex(List<Cookie> resultList, String host, boolean secureLink) {
		 
		 for (Entry<String, List<Cookie>> entry : domainIndex.entrySet()) {
			 String domain = entry.getKey();
			 List<Cookie> cookieList = entry.getValue();
			 for (Cookie cookie : cookieList) {
				 if (((cookie.getVersion() == 0 && netscapeDomainMatches(domain, host)) || (cookie.getVersion() == 1 && Cookie.matches(domain, host))) && 
					 (!cookie.hasExpired()) && ((secureLink || !cookie.getSecure()) && !resultList.contains(cookie))) {
					 	resultList.add(cookie);
				 }
			 }
		 }
	 }


	 private void fetchFromUriIndex(List<Cookie> resultList, Comparable<URI> comparator, boolean secureLink) {

		 for (URI uri : uriIndex.keySet()) {
			if (comparator.compareTo(uri) == 0) {
				List<Cookie> indexedCookies = uriIndex.get(uri);
		
				if (indexedCookies != null) {
					for (Cookie httpCookie : indexedCookies) {
					    if (!httpCookie.hasExpired()) {
					        if ((secureLink || !httpCookie.getSecure()) && !resultList.contains(httpCookie)) {
					            resultList.add(httpCookie);
					        }
					    } else {
					        if (LOG.isLoggable(Level.FINE)) {
					            LOG.fine("cookie " + httpCookie + " is expired");
					        }
					    }
					}
				} 
			}
		} 
	}

	 
	 
	 private boolean netscapeDomainMatches(String domain, String host) {
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

		 
		 if ((host.indexOf('.') == -1) && isLocalDomain) {
			 return true;
		 }

		 int sizeDiff = host.length() - domain.length();
		 if (sizeDiff == 0) {
			 return host.equalsIgnoreCase(domain);
			 
		 } else if (sizeDiff > 0) {
			 String s = host.substring(sizeDiff);
			 return (s.equalsIgnoreCase(domain));
			 
		 } else if (sizeDiff == -1) {
			 return ((domain.charAt(0) == '.') && (host.equalsIgnoreCase(domain.substring(1))));
		 }

		 return false;
	 }	 
	 
	
	
    @SuppressWarnings("unchecked")
	void clean() {
    	
    	try {
    		HashMap<String, List<Cookie>> domainIndexCopy = null;
    		HashMap<URI, List<Cookie>> uriIndexCopy = null;
    		
    		synchronized (guard) {
    			domainIndexCopy = (HashMap<String, List<Cookie>>) domainIndex.clone();
    			uriIndexCopy = (HashMap<URI, List<Cookie>>) uriIndex.clone();
			}

    		for (Entry<String, List<Cookie>> entry : domainIndexCopy.entrySet()) {
				for (Cookie cookie : entry.getValue()) {
					if (cookie.hasExpired()) {

						LOG.fine("cookie " + cookie + " has been expired. Deleting it");
						
						synchronized (guard) {
							domainIndex.get(entry.getKey()).remove(cookie);
							if (domainIndex.get(entry.getKey()).isEmpty()) {
								domainIndex.remove(entry.getKey());
							}
						}
					}
				}
			}

    		for (Entry<URI, List<Cookie>> entry : uriIndexCopy.entrySet()) {
				for (Cookie cookie : entry.getValue()) {
					if (cookie.hasExpired()) {

						LOG.fine("cookie " + cookie + " has been expired. Deleting it");
						
						synchronized (guard) {
							uriIndex.get(entry.getKey()).remove(cookie);
							if (uriIndex.get(entry.getKey()).isEmpty()) {
								uriIndex.remove(entry.getKey());
							}
						}
					}
				}
			}
    		
    		
    	} catch (Throwable e) {
    		if (LOG.isLoggable(Level.FINE)) {
    			LOG.fine("error occured by cleaning store " + e.toString());
    		}
    	}
    }
    
    
    private static final class Cleaner extends TimerTask implements Closeable {

    	private WeakReference<InMemoryCookieStore> inMemoryCookieStoreRef;
    	
    	public Cleaner(InMemoryCookieStore inMemoryCookieStore) {
    		inMemoryCookieStoreRef = new WeakReference<InMemoryCookieStore>(inMemoryCookieStore);
		}
    	
    	public void run() {
    		WeakReference<InMemoryCookieStore> ref = inMemoryCookieStoreRef;
    		if (ref != null) {
    			InMemoryCookieStore inMemoryCookieStore = ref.get();
    			if (inMemoryCookieStore == null) {
    				close();
    			} else {
    				inMemoryCookieStore.clean();
    			}
    		}
		}
    	
    	public void close() {
    		cancel();
    		inMemoryCookieStoreRef = null;
    	}

    }
}
