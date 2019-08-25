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

import java.util.Enumeration;
import java.util.Set;






/**
 * The HttpSession. Provides a way to identify a user across more than one page request
 * or visit to a Web site and to store information about that user. 
 * 
 * @author grro@xlightweb.org
 */
public interface IHttpSession {

    
    /**
     * Returns the object bound with the specified name in this session, or
     * <code>null</code> if no object is bound under the name.
     *
     * @param name		a string specifying the name of the object
     * @return			the object with the specified name
     * @exception IllegalStateException	if this method is called on an invalidated session
     */
    public Object getAttribute(String name);
    
    
   
    /**
     * Binds an object to this session, using the name specified.
     * If an object of the same name is already bound to the session,
     * the object is replaced.
     *
     * <p>If the value passed in is null, this has the same effect as calling 
     * <code>removeAttribute()<code>.
     *
     *
     * @param name	 the name to which the object is bound; cannot be null
     * @param value  the object to be bound
     *
     * @exception IllegalStateException	if this method is called on an invalidated session
     */
    public void setAttribute(String name, Object value);
    
    
    /**
     * Removes the object bound with the specified name from
     * this session. If the session does not have an object
     * bound with the specified name, this method does nothing.
     *
     *
     * @param name	the name of the object to remove from this session
     *
     * @exception IllegalStateException	if this method is called on an invalidated session
     */
    public void removeAttribute(String name);

    

    
    /**
     * Returns an <code>Enumeration</code> of <code>String</code> objects
     * containing the names of all the objects bound to this session. 
     *
     * @return 	an <code>Enumeration</code> of <code>String</code> objects specifying the
     *		    names of all the objects bound to this session
     *
     * @exception IllegalStateException	if this method is called on an invalidated session
     */
	@SuppressWarnings("unchecked")
	public Enumeration getAttributeNames();
    

    /**
     * Returns an <code>Set</code> of <code>String</code> objects containing the 
     * names of all the objects bound to this session. 
     *
     * @return 	an <code>Set</code> of <code>String</code> objects specifying the
     *		    names of all the objects bound to this session
     *
     * @exception IllegalStateException	if this method is called on an invalidated session
     */
    public Set<String> getAttributeNameSet();
    
    

    /**
     * Returns the time when this session was created, measured
     * in milliseconds since midnight January 1, 1970 GMT.
     *
     * @return	a <code>long</code> specifying when this session was created,
     *          expressed in milliseconds since 1/1/1970 GMT
     *
     * @exception IllegalStateException	if this method is called on an invalidated session
     */
    public long getCreationTime();
    
    
    
    
    /**
     * Returns a string containing the unique identifier assigned 
     * to this session.
     * 
     * @return a string specifying the identifier assigned to this session
     */
    public String getId();
    
    


    /**
     * Returns the last time the client sent a request associated with
     * this session, as the number of milliseconds since midnight
     * January 1, 1970 GMT, and marked by the time the container received the request. 
     *
     * <p>Actions that your application takes, such as getting or setting
     * a value associated with the session, do not affect the access
     * time.
     *
     * @return	a <code>long</code> representing the last time
     *          the client sent a request associated with this session, expressed in
     *          milliseconds since 1/1/1970 GMT
     *
     * @exception IllegalStateException	if this method is called on an invalidated session
     *
     */
    public long getLastAccessedTime();     
    

    /**
     * Specifies the time, in seconds, between client requests before the 
     * container will invalidate this session.  A negative time
     * indicates the session should never timeout.
     *
     * @param interval	An integer specifying the number of seconds 
     */
    public void setMaxInactiveInterval(int interval);



   /**
    * Returns the maximum time interval, in seconds, that 
    * the container will keep this session open between 
    * client accesses. After this interval, the container
    * will invalidate the session.  The maximum time interval can be set
    * with the <code>setMaxInactiveInterval</code> method.
    * A negative time indicates the session should never timeout.
    *
    * @return	an integer specifying the number of seconds this session remains open
    *			between client requests
    */
   public int getMaxInactiveInterval();
   
    
    
    /**
     * Invalidates this session then unbinds any objects bound to it. 
     *
     * @exception IllegalStateException	if this method is called on an already invalidated session
     */
    public void invalidate();
    
    
    /**
     * returns if the session is valid
     * 
     * @return true, if the session is valid
     */
    public boolean isValid();
    
}
