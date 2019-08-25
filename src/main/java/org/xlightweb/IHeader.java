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
import java.util.List;
import java.util.Set;



/**
 * Header definition 
 * 
 * @author grro@xlightweb.org
 */
public interface IHeader {

	
    /**
     * Adds a header with the given name and value.
     * This method allows headers to have multiple values.
     * 
     * @param   headername  the name of the header
     * @param   headervalue the additional header value. If it contains octet string, it should be encoded
     *                  according to RFC 2047 (http://www.ietf.org/rfc/rfc2047.txt)
     */
    void addHeader(String headername, String headervalue);

    
    /**
     * Sets a header with the given name and value.
     * If the header had already been set, the new value overwrites the
     * previous one.  
     * 
     * @param   headername   the name of the header
     * @param   headervalue  the header value  If it contains octet string,
     *                 it should be encoded according to RFC 2047 (http://www.ietf.org/rfc/rfc2047.txt)
     */
    void setHeader(String headername, String headervalue);

    
    /**
     * removes a header with the given name
     *  
     * @param headername the name of the header
     */
    void removeHeader(String headername);

    
 
    
    /**
     * Returns a boolean indicating whether the named header 
     * has already been set.
     * 
     * @param   name  the header name
     * @return  <code>true</code> if the named header has already been set; <code>false</code> otherwise
     */
    boolean containsHeader(String headername);


    /**
     * Returns an set of all the header names. 
     * If the part has no headers, this method 
     * returns an empty Set.
     * 
     * @return an Set of all the header names
     *
     */
    Set<String> getHeaderNameSet();
    
    
    /**
     * Returns an enumeration of all the header names. 
     * If the part has no headers, this method 
     * returns an empty enumeration.
     * 
     * @return an enumeration of all the header names
     *
     */
    @SuppressWarnings("unchecked")
    Enumeration getHeaderNames();
    
    

    /**
     * Returns all the values of the specified header as an 
     * <code>List</code> of <code>String</code> objects.
     * 
     * <p>Some headers, such as <code>Accept-Language</code> can be set
     * by part producer as several headers each with a different value rather than
     * sending the header as a comma separated list.
     * 
     * 
     * @param headername a <code>String</code> specifying the header name
     * @return an <code>List</code> containing the values of the requested header. 
     *         If the part does not have any headers of that name return an empty enumeration.
     */
    List<String> getHeaderList(String headername);
    
    
    /**
     * Returns all the values of the specified header as an 
     * <code>Enumeration</code> of <code>String</code> objects.
     * 
     * <p>Some headers, such as <code>Accept-Language</code> can be set
     * by part producer as several headers each with a different value rather than
     * sending the header as a comma separated list.
     * 
     * 
     * @param headername a <code>String</code> specifying the header name
     * @return an <code>Enumeration</code> containing the values of the requested header. 
     *         If the part does not have any headers of that name return an empty enumeration.
     */
    @SuppressWarnings("unchecked")
    Enumeration getHeaders(String headername);

    


    /**
     * adds raw header lines
     * 
     * @param lines the headerlines
     */
    void addHeaderlines(String... lines);
    
    
    /**
     * adds a raw header line 
     * 
     * @param line  the headerline
     */
    void addHeaderLine(String line);
    

    /**
     * Returns the value of the specified header as a <code>String</code>.
     * If the part did not include a header of the specified name, 
     * this method returns <code>null</code>.
     * 
     * If there are multiple headers with the same name, this method 
     * returns the first head in the part.
     * 
     * @param headername a <code>String</code> specifying the header name
     * @return  a <code>String</code> containing the value of the 
     *          requested header, or <code>null</code> if the part
     *          does not have a header of that name
     */
    String getHeader(String headername);    	
    
    
    /**
     * Returns the value of the specified header as a <code>String</code>.
     * If the part did not include a header of the specified name, 
     * this method returns <code>null</code>.
     * 
     * If there are multiple headers with the same name, this method 
     * returns the first head in the part.
     * 
     * @param headername  a <code>String</code> specifying the header name
     * @param dfltValue   the default value if the header is not set
     * @return  a <code>String</code> containing the value of the 
     *          requested header, or <code>null</code> if the part
     *          does not have a header of that name
     */
    String getHeader(String headername, String dfltValue);  
    
    /**
     * sets the MIME type of the body of the part
     * 
     * @param type the MIME type of the body of the part
     */
    void setContentType(String type);


    /**
     * Returns the MIME type of the body of the messag, or 
     * <code>null</code> if the type is not known. 
     *
     * @return  a <code>String</code> containing the name of the MIME type of 
     * 			the message, or null if the type is not known
     */
	String getContentType();
		
	

    /**
     * Returns the name of the character encoding used in the body of this
     * message.  If the header defines no encoding, the default encoding will be returned
     *
     * @return a <code>String</code> containing the name of the character encoding
     *
     */
    String getCharacterEncoding();
        
	    
    
    /**
     * returns the content disposition header of the part, or
     * <code>null</code> if the type is not known.<br><br>
     * 
     * see also <a href="http://www.ietf.org/rfc/rfc2183.txt">rfc2183</a>
     * 
     * @return  a <code>String</code> containing the name of the content disposition of 
     *          the part, or null if the type is not known
     */
    String getDisposition();
    
    
    
    /**
     * returns the content disposition type or <code>null</code> if not set<br><br>
     * 
     * see also <a href="http://www.ietf.org/rfc/rfc2183.txt">rfc2183</a>
     *  
     * @return the content disposition type or <code>null</code> if not set
     */
    String getDispositionType();
    
    
    /**
     * returns the content disposition param value or <code>null</code> if not set<br><br>
     * 
     * see also <a href="http://www.ietf.org/rfc/rfc2183.txt">rfc2183</a>
     * 
     * @param name  the parameter name
     * @return the content disposition param value or <code>null</code> if not set
     */
    String getDispositionParam(String name);
    
    
    /**
     * returns the Transfer-Encoding header parameter or <code>null</code> if the header is not set
     *
     * @return the Transfer-Encoding header parameter or <code>null</code> if the header is not set
     */
    String getTransferEncoding();

    
    /**
     * sets the Transfer-Encoding parameter
     *   
     * @param transferEncoding the Transfer-Encoding parameter
     */
    void setTransferEncoding(String transferEncoding);
}
