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
import java.util.Map;


 

/**
 * Session Manager. The API required to manage sessions. By implementing and assigning the SessionManager
 * the <code>HttpSession</code> can be managed by a custom implementation. It can also be used to 
 * intercept the (default) SessionManager data flow. See example:
 * <pre>
 * class MySessionManagerInterceptor implements ISessionManager {
 *    private ISessionManager delegate = null;
 *    
 *    void MySessionManagerInterceptor(ISessionManager delegate) {
 *       this.delegate = delegate;
 *    }
 *    
 *    public boolean isEmtpy() {
 *       return delegate.isEmtpy();
 *    }
 *    
 *    public Map<String, HttpSession> getSessionMap() {
 *       return delegate.getSessionMap();
 *    }
 *    
 *    public String newSession(String idPrefix) throws IOException {
 *       String id = delegate.newSession(idPrefix);
 *       System.out.println("session " + id + " created");
 *       return id;
 *    }
 *    
 *    public void saveSession(String sessionId) throws IOException {
 *       delegate.saveSession(sessionId);
 *    }
 *
 *    public void registerSession(HttpSession session) throws IOException {
 *       delegate.registerSession(session);
 *    }
 *    
 *    public HttpSession getSession(String sessionId) throws IOException {
 *       return delegate.getSession(sessionId);
 *    }
 *    
 *    public void removeSession(String sessionId) throws IOException {
 *       delegate.removeSession(sessionId);
 *    }
 *   
 *    public void close() throws IOException {
 *       delegate.close();
 *    }
 * }
 *
 * ... 
 * 
 * HttpServer httpServer = new HttpServer(8030, myHandler);
 * httpServer.setSessionManager(new MySessionManagerInterceptor(httpServer.getSessionManager()));
 * httpServer.start();
 * </pre> 
 * 
 * @author grro@xlightweb.org
 */
public interface ISessionManager extends Closeable {

	
	/**
	 * returns true, if no session exists
	 * 
	 * @return true, if no session exists
	 */
	boolean isEmtpy();
	
	
	/**
	 * returns the session for a session id
	 * 
	 * @param sessionId  the session id 
	 * @return the session or <code>null</code>
	 * 
	 * @exception IOException if an exception occurs
	 */
	HttpSession getSession(String sessionId) throws IOException;



    /**
     * return the session map
     * 
     * @return the session map
     */
    Map<String, HttpSession> getSessionMap();
	
	
	/**
	 * creates a new session 
	 * 
	 * @param idPrefix   the id prefix
	 * @return the session id
	 * @exception IOException if an exception occurs 
	 */
	String newSession(String idPrefix) throws IOException;

	
	/**
	 * save the session 
	 * 
	 * @param session id the session id
	 * @exception IOException if an exception occurs 
	 */
	void saveSession(String sessionId) throws IOException;

	
	/**
	 * registers a session
	 * 
	 * @param session    the session 
	 * @throws IOException if an exception occurs
	 */
	void registerSession(HttpSession session) throws IOException;
	
	
	/**
	 * removes a session
	 * 
	 * @param session  the session id
	 * @exception IOException if an exception occurs 
	 */
	void removeSession(String sessionId) throws IOException;	
}
