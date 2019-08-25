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
 * Http Header 
 * 
 * @author grro@xlightweb.org
 */
public interface IHttpMessageHeader extends IHeader {

    static final String DEFAULT_ENCODING = "ISO-8859-1";

	
	/**
	 * creates a copy of this header 
	 * 
	 * @return return the copy
	 */
	IHttpMessageHeader copy();
	
	
    /**
     * Returns the length, in bytes, of the message body 
     * and made available by the input stream, or -1 if the
     * length is not known. 
     *
     * @return	an integer containing the length of the message body or -1 if the length is not known
     */
	int getContentLength();
	
	
	
	/**
	 * sets the content length in bytes
	 * 
	 * @param length  the content length in bytes
	 */
	void setContentLength(int length);
	

	
	/**
	 * Returns the name and version of the protocol the message 
	 * uses in the form protocol/majorVersion.minorVersion, for example, HTTP/1.1.  
	 * 
	 * @return a String containing the protocol name and version number
	 */
	String getProtocol();
	


	/**
	 * Returns the version of the protocol the message 
	 * uses in the form majorVersion.minorVersion, for example, 1.1.  
	 * 
	 * @return a String containing the protocol version number
	 */
	String getProtocolVersion();
	
	
	
	
	   

    /**
     * Stores an attribute in this header. Attributes are reset between messages.  
     *
     * <br><br>Attribute names should follow the same conventions as
     * package names. 
     *
     * @param name		a <code>String</code> specifying 
     *					the name of the attribute
     * @param o			the <code>Object</code> to be stored
     *
     */
    void setAttribute(String name, Object o);
    


    /**
     * Returns the value of the named attribute as an <code>Object</code>,
     * or <code>null</code> if no attribute of the given name exists. 
     *
     * <br><br>Attribute names should follow the same conventions as package
     * names.
     *
     * @param name	a <code>String</code> specifying the name of the attribute
     * @return  an <code>Object</code> containing the value of the attribute, 
     *          or <code>null</code> if the attribute does not exist
     */
    Object getAttribute(String name);
    
    

    /**
     * Returns an <code>Enumeration</code> containing the
     * names of the attributes available to this message. 
     * This method returns an empty <code>Enumeration</code>
     * if the message has no attributes available to it.
     * 
     *
     * @return an <code>Enumeration</code> of strings containing the names
     *         of the message's attributes
     *
     */
    @SuppressWarnings("unchecked")
	Enumeration getAttributeNames();
    
    

    /**
     * Returns an <code>Set</code> containing the
     * names of the attributes available to this messaget. 
     * This method returns an empty <code>Set</code>
     * if the request has no attributes available to it.
     * 
     *
     * @return	an <code>Set</code> of strings 
     *			containing the names of the message's attributes
     *
     */
    Set<String> getAttributeNameSet();
    

    
	/**
	 * copy the headers 
	 * 
	 * @param otherHeader         the other header
	 * @param upperExcludenames   the header names to exclude
	 */
	void copyHeaderFrom(IHttpMessageHeader otherHeader, String... upperExcludenames);
	

	
    /**
     * removes all hop-by-hop headers without Transfer-Encoding if set to chunked 
     */
    void removeHopByHopHeaders();
}
