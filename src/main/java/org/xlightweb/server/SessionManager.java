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
package org.xlightweb.server;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

 

/**
 * In-memory SessionManager implementation.
 * 
 * @author grro@xlightweb.org
 */
public class SessionManager implements ISessionManager {

	public static final Logger LOG = Logger.getLogger(SessionManager.class.getName());
	
	
	private static final long CLEANING_PERIOD_MILLIS = 5 * 1000;
	private Cleaner cleaner;
	
	private final HashMap<String, HttpSession> sessions = new HashMap<String, HttpSession>();
	
	
	// statistics
	private int numCreatedSessions = 0;
	private int numRemovedSessions = 0;
	private int numExpiredSessions = 0;
	
	
	
	
	/**
	 * constructor 
	 */
	public SessionManager() {
		cleaner = new Cleaner(this);
		HttpServerConnection.schedule(cleaner, CLEANING_PERIOD_MILLIS, CLEANING_PERIOD_MILLIS);
	}
	

	
	/**
	 * {@inheritDoc}
	 */
	public boolean isEmtpy() {
		return sessions.isEmpty();
	}
	

	
	/**
	 * {@inheritDoc}
	 */
	public HttpSession getSession(String sessionId) {
		
		synchronized (sessions) {
			HttpSession session = sessions.get(sessionId);
			if (session != null) {
				long currentMillis = System.currentTimeMillis();
				
				session.setLastAccessTime(currentMillis);
				if (!session.isValid()) {
					sessions.remove(session.getId());
					session = null;
				}
			}
			
			return session;
		}
 	}

	
    /**
     * {@inheritDoc}
     */
	@SuppressWarnings("unchecked")
    public Map<String, HttpSession> getSessionMap() {
	    
	    Map<String, HttpSession> sessionMap = null;
        synchronized (sessions) {
            sessionMap = (Map<String, HttpSession>) sessions.clone();
        }
        
        return sessionMap;
    }

    
	/**
	 * {@inheritDoc}
	 */
	public void registerSession(HttpSession session) throws IOException {
		synchronized (sessions) {
			sessions.put(session.getId(), session);
		}
 	}

	
	/**
	 * {@inheritDoc}
	 */	
	public void saveSession(String sessionId) throws IOException {
		
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public String newSession(String idPrefix) throws IOException {
	    
	    numCreatedSessions++;
		String sessionId = idPrefix + "-" + UUID.randomUUID().toString();
		registerSession(new HttpSession(sessionId));
		
		return sessionId;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void removeSession(String sessionId) {
	    
	    numRemovedSessions++;
	    
		synchronized (sessions) {
			sessions.remove(sessionId);
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public void close() {
		
		HashMap<String, HttpSession> sessionsCopy = null;
    	synchronized (sessions) {
    		sessionsCopy = (HashMap<String, HttpSession>) sessions.clone(); 
		}
    	
		for (HttpSession httpSession : sessionsCopy.values()) {
			httpSession.invalidate();
		}
		
		synchronized (sessions) {
			sessions.clear(); 
		}
		
		if (cleaner != null) {
			cleaner.close();
		}
		cleaner = null; 
	}
	

    
    @SuppressWarnings("unchecked")
	void clean() {
    	
    	try {
	    	HashMap<String, HttpSession> sessionsCopy = null;
	    	synchronized (sessions) {
	    		sessionsCopy = (HashMap<String, HttpSession>) sessions.clone(); 
			}
	    	
	    	for (Entry<String, HttpSession> entry : sessionsCopy.entrySet()) {
				if (!entry.getValue().isValid()) {
		    		if (LOG.isLoggable(Level.FINE)) {
		    			LOG.fine("session " + entry.getValue() + " has been expired. Deleting it");
		    		}
		    		
		    		numExpiredSessions++;
		    		removeSession(entry.getValue().getId());
				}
			}
    	} catch (Exception e) {
    	    // eat and log exception
    		if (LOG.isLoggable(Level.FINE)) {
    			LOG.fine("error occured by cleaning sessions " + e.toString());
    		}
    	}
    }
    
    
    public int getNumCreatedSessions() {
        return numCreatedSessions;
    }
    
    public int getNumRemovedSessions() {
        return numRemovedSessions;
    }
    
    public int getNumExpiredSessions() {
        return numExpiredSessions;
    }
    
    
	/**
	 * {@inheritDoc}
	 */
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder(super.toString());
    	sb.append(" (");
    	
    	for (Entry<String, HttpSession> entry: sessions.entrySet()) {
			sb.append(entry.getKey() + "-> " + entry.getValue() + "  ");
		}
    	
    	sb.append(")");
    	return sb.toString();
    }
    
    
    
    
    private static final class Cleaner extends TimerTask implements Closeable {

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
}
