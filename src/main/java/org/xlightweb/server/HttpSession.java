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

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.xlightweb.IHttpSession;
import org.xsocket.DataConverter;

 

/**
 * HttpSession. A HttpSession will be managed by the {@link ISessionManager}  
 *  
 * @author grro@xlightweb.org
 */
public final class HttpSession implements IHttpSession, Serializable {

	private static final long serialVersionUID = 3712677521662304844L;

	
	private final Map<String, Object> attributes = Collections.synchronizedMap(new HashMap<String, Object>());
	private final AtomicBoolean isValid = new AtomicBoolean(true);
	
	private final String id;
	private final long creationTime = System.currentTimeMillis();
	private long lastAccesTime = creationTime;
	private int maxInactiveIntervalSec = Integer.MAX_VALUE;
	private long changeVersion;


	/**
	 * constructor 
	 * 
	 * @param id  the session id
	 */
	HttpSession(String id) {
		this.id = id;
	}

	
	
	/**
	 * {@inheritDoc}
	 */
	public String getId() {
		return id;
	}
	

	/**
	 * {@inheritDoc}
	 */
	public long getCreationTime() {
		return creationTime;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public long getLastAccessedTime() {
		if (!isValid()) {
			throw new IllegalStateException("Session " + getId() + " is invalid"); 
		}
		
		return lastAccesTime;
	}
	
	
	/**
	 * sets the last access time
	 * 
	 * @param lastAccesTime  the last access time
	 */
	void setLastAccessTime(long lastAccesTime) {
		this.lastAccesTime = lastAccesTime;
		incChangeVersion();
	}

	
	/**
	 * {@inheritDoc}
	 */
    public Object getAttribute(String name) {
		if (!isValid()) {
			throw new IllegalStateException("Session " + getId() + " is invalid"); 
		}

    	return attributes.get(name);
    }
    
    
	/**
	 * {@inheritDoc}
	 */
    public void setAttribute(String name, Object value) {
		if (!isValid()) {
			throw new IllegalStateException("Session " + getId() + " is invalid"); 
		}

		attributes.put(name, value);
		incChangeVersion();
    }
    
    
	/**
	 * {@inheritDoc}
	 */
    public void removeAttribute(String name) {
		if (!isValid()) {
			throw new IllegalStateException("Session " + getId() + " is invalid"); 
		}

		attributes.remove(name);
		incChangeVersion();
    }
    
    
	/**
	 * {@inheritDoc}
	 */
	public void invalidate() {
		isValid.set(false);
		incChangeVersion();
    }
    
    
	/**
	 * {@inheritDoc}
	 */
    public boolean isValid() {
    	
    	if (isValid.get() && (System.currentTimeMillis() > (lastAccesTime + (((long ) maxInactiveIntervalSec) * 1000)))) {
    		invalidate();
    	} 
    
    	return isValid.get();
    }

    
    
	/**
	 * {@inheritDoc}
	 */
    @SuppressWarnings("unchecked")
	public Enumeration getAttributeNames() {
		if (!isValid()) {
			throw new IllegalStateException("Session " + getId() + " is invalid"); 
		}

    	return Collections.enumeration(getAttributeNameSet());
    }
    

	/**
	 * {@inheritDoc}
	 */
    public Set<String> getAttributeNameSet() {
		if (!isValid()) {
			throw new IllegalStateException("Session " + getId() + " is invalid"); 
		}

		return Collections.unmodifiableSet(attributes.keySet());
    }
    
    
	/**
	 * {@inheritDoc}
	 */
    public void setMaxInactiveInterval(int maxInactiveIntervalSec) {
    	this.maxInactiveIntervalSec = maxInactiveIntervalSec;
    }
    
    
	/**
	 * {@inheritDoc}
	 */
    public int getMaxInactiveInterval() {
    	return maxInactiveIntervalSec;
    }
    
    private void incChangeVersion() {
    	changeVersion++;
    }
    
    
    /**
     * returns the object change version. The change version can be used 
     * to check if the session is modified
     *    
     * @return the change version
     */
    public long getChangeVersion() {
    	return changeVersion;
    }
    


    
	/**
	 * {@inheritDoc}
	 */
    @Override
    public String toString() {
        
        try {
        	StringBuilder sb = new StringBuilder("created=" + DataConverter.toFormatedDate(creationTime) + 
        	                                     ", lastAccess=" + DataConverter.toFormatedDate(lastAccesTime) +
        	                                     ", maxInactiveIntervalSec=" + maxInactiveIntervalSec + ",  Data: ");

        	boolean isFrist = true;
        	for (Entry<String, Object> entry : attributes.entrySet()) {
        	    
    			sb.append(entry.getKey() + "=" + entry.getValue());
    			
    			if (isFrist) {
    			    isFrist = false;
    			} else {
    			    sb.append("&");
    			}
    		}

        	return sb.toString();
        } catch (Exception e) {
            return "exception occured by performing toSting() " + e.toString(); 
        }
    }
}
