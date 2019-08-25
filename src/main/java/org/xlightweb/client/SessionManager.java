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
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xlightweb.IHttpSession;



/**
 * Session Manager
 * 
 * @author grro@xlightweb.org
 */
final class SessionManager implements Cloneable {
	
	public static final Logger LOG = Logger.getLogger(SessionManager.class.getName());

	private static final long CLEANING_PERIOD_MILLIS = 5 * 1000;
	private Cleaner cleaner;
	
	private final HashMap<DomainPathPair, HttpSession> sessions = new HashMap<DomainPathPair, HttpSession>();

	
	/**
	 * constructor
	 */
	public SessionManager() {
		cleaner = new Cleaner(this); 
		HttpClientConnection.schedule(cleaner, CLEANING_PERIOD_MILLIS, CLEANING_PERIOD_MILLIS);
	}
	
	
	public void close() {
		if (cleaner != null) {
			cleaner.close();
		}
		cleaner = null;
		
		sessions.clear();
	}
	

	
	
	/**
	 * retrieves a session
	 * 
	 * @param domain       the domain
	 * @param path         the path
	 * @param create       true, if a new one should be created if not exits 
	 * @return the session
	 */
	public IHttpSession getSession(String domain, String path, boolean create) {
    	
		synchronized (sessions) {
			for (DomainPathPair pair : sessions.keySet()) {
			    
		    	if (domain.equals(pair.getDomain()) || domain.endsWith(pair.getDomain())) {
		    		
		    		HttpSession session = sessions.get(pair);
		    		if (session != null) {
		    			session.setLastAccessTime(System.currentTimeMillis());
		    		}
		    		return session;
		    	}
			}	    	
		}

		
		if (create) {
			DomainPathPair pair = new DomainPathPair(domain, path);
			HttpSession session = new HttpSession(UUID.randomUUID().toString());
			put(pair, session);
			
			return session;
			
		} else {
			return null;
		}
    }
	

	
			
	private void put(DomainPathPair pair, HttpSession session) {
		synchronized (sessions) {
			sessions.put(pair, session);
		}
	}
	
	

    
    @SuppressWarnings("unchecked")
	private void clean() {
    	
    	try {
    		HashMap<DomainPathPair, HttpSession> sessionsCopy = null;
	    	synchronized (sessions) {
	    		sessionsCopy = (HashMap<DomainPathPair, HttpSession>) sessions.clone(); 
			}
	    	
	    	long currentMillis = System.currentTimeMillis();
	    	for (Entry<DomainPathPair, HttpSession> entry : sessionsCopy.entrySet()) {
				if (entry.getValue().checkMaxIntervalExceeded(currentMillis)) {
		    		if (LOG.isLoggable(Level.FINE)) {
		    			LOG.fine("session " + entry.getValue() + " has been expired. Deleting it");
		    		}
					close();
				}
			}
    	} catch (Exception e) {
    	    // eat and log exception
    		if (LOG.isLoggable(Level.FINE)) {
    			LOG.fine("error occured by cleaning sessions " + e.toString());
    		}
    	}
    }
    
    
    private static final class Cleaner extends TimerTask implements Closeable  {

    	private WeakReference<SessionManager> sessionManagerRef;
    	
    	public Cleaner(SessionManager sessionManager) {
    		sessionManagerRef = new WeakReference<SessionManager>(sessionManager);
		}
    	
    	public void run() {
    		WeakReference<SessionManager> ref = sessionManagerRef;
    		if (ref != null) {
	    		SessionManager sessionManager = ref.get();
	    		if (sessionManager == null) {
	    			close();
	    		} else {
	    			sessionManager.clean();
	    		}
    		}
		}
    	
    	public void close() {
    		cancel();
    		sessionManagerRef = null;
    	}
    }
        
	
	

	private static final class HttpSession implements IHttpSession {

	private static final long serialVersionUID = -5876183451306149950L;

		private final Map<String, Object> attributes = new HashMap<String, Object>();

		private boolean isValid = true;


		private String id = null;
		private long creationTime = System.currentTimeMillis();
		private long lastAccesTime = creationTime;
		private int maxInactiveIntervalSec = -1;
		
		
		public HttpSession(String id) {
			this.id = id;
		}
		
		public String getId() {
			return id;
		}
		
		
		public long getCreationTime() {
			return creationTime;
		}
		
		public long getLastAccessedTime() {
			if (!isValid()) {
				throw new IllegalStateException("Session " + getId() + " is invalid"); 
			}

			return lastAccesTime;
		}
		
		void setLastAccessTime(long lastAccesTime) {
			this.lastAccesTime = lastAccesTime;
		}
		

	    public Object getAttribute(String name) {
			if (!isValid()) {
				throw new IllegalStateException("Session " + getId() + " is invalid"); 
			}

	    	return attributes.get(name);
	    }
	    
	    

	    public void setAttribute(String name, Object value) {
			if (!isValid()) {
				throw new IllegalStateException("Session " + getId() + " is invalid"); 
			}

	    	attributes.put(name, value);
	    }
	    
	    

	    public void removeAttribute(String name) {
			if (!isValid()) {
				throw new IllegalStateException("Session " + getId() + " is invalid"); 
			}

	    	attributes.remove(name);
	    }

	    

        public Enumeration<String> getAttributeNames() {
			if (!isValid()) {
				throw new IllegalStateException("Session " + getId() + " is invalid"); 
			}

	    	return Collections.enumeration(getAttributeNameSet());
	    }
	    

	    public Set<String> getAttributeNameSet() {
			if (!isValid()) {
				throw new IllegalStateException("Session " + getId() + " is invalid"); 
			}

	    	return Collections.unmodifiableSet(attributes.keySet());
	    }
	    
	    
	    public void setMaxInactiveInterval(int maxInactiveIntervalSec) {
	    	this.maxInactiveIntervalSec = maxInactiveIntervalSec;
	    }
	    
	    
	    public int getMaxInactiveInterval() {
	    	return maxInactiveIntervalSec;
	    }
	    
	    
	    public void invalidate() {
	    	isValid = false;
	    }
	    
	    public boolean isValid() {
	    	return isValid;
	    }
	    
	    
	    boolean checkMaxIntervalExceeded(long currentMillis) {
	    	if (System.currentTimeMillis() > (lastAccesTime + (((long ) maxInactiveIntervalSec) * 1000))) {
	    		invalidate();
	    		return true;
	    	}
	    	return false;
	    }
	    


	    
	    
	    @Override
	    public String toString() {
	    	StringBuilder sb = new StringBuilder();
	    	
	    	for (Entry<String, Object> entry : attributes.entrySet()) {
				sb.append(entry.getKey() + "=" + entry.getValue() + "&");
			}
	    	
	    	return sb.toString();
	    }
	}
	
	
	private static final class DomainPathPair {

		
		private String domain = "";
		private String path = "";
		
		
		DomainPathPair(String domain, String path) {
			this.domain = domain;
			
			if (path != null) {
				this.path = path;
			}
		}
		
		
		String getDomain() {
			return domain;
		}
		

		@Override
		public int hashCode() {
			return (domain + path).hashCode();
		}
		
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof DomainPathPair) {
				DomainPathPair other = (DomainPathPair) obj;
				if (other.domain.equals(this.domain) && other.path.equals(this.path)) {
					return true;
				}
			}
			
			return false;
		}
	}

}
