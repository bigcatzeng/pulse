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


import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xsocket.connection.INonBlockingConnection;




/**
 * a message header
 * 
 * @author grro@xlightweb.org
 */
class HttpMessageHeader extends Header implements IHttpMessageHeader {
    
    private static final Logger LOG = Logger.getLogger(HttpMessageHeader.class.getName());
	
	
	static final byte CR = 13;
	static final byte LF = 10;
	static final byte SPACE = 32;
	static final byte HTAB = 9;
	
	
	static final String HOST = "Host";
	static final String USER_AGENT = "User-Agent";
	static final String SERVER = "Server";
	static final String DATE = "Date";
	static final String CONTENT_LENGTH = "Content-Length";


	// parsing support
	private int parsedChars = 0;

	
	// attributes 
	private HashMap<String, Object> attributes = new HashMap<String, Object>();

	
	// caching
	private int contentLength = -1;


	private String protocolScheme;
	private String protocolVersion;

	
	/**
	 * constructor
	 */
	public HttpMessageHeader() {
		
	}
	

	/**
	 * constructor
	 * 
	 * @param contentType the content type
	 */
	public HttpMessageHeader(String contentType) {
		setContentType(contentType);
	}
	

	/**
	 * sets the protocol scheme silence
	 * @param protocolScheme the protocol scheme
	 */
	final void setProtocolSchemeSilence(String protocolScheme) {
		this.protocolScheme = protocolScheme;
	}
	

	/**
	 * returns the protocol scheme
	 * 
	 * @return the protocol scheme
	 */
	final String getProtocolScheme() {
		return protocolScheme;
	}

	
	/**
	 * sets the protocol version silence
	 * @param protocolVersion the protocol version
	 */
	final void setProtocolVersionSilence(String protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	

	/**
	 * {@inheritDoc}
	 */
	public String getProtocolVersion() {
		return protocolVersion;
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public String getProtocol() {
		return protocolScheme + "/" + protocolVersion;
	}

	
	
	
	/**
	 * {@inheritDoc}
	 */
	public final void setAttribute(String name, Object o) {
		attributes.put(name, o);
	}
	

	/**
	 * {@inheritDoc}
	 */
	public final Object getAttribute(String name) {
		return attributes.get(name);
	}

	
	/**
	 * {@inheritDoc}
	 */
	public final Set<String> getAttributeNameSet() {
		return Collections.unmodifiableSet(attributes.keySet());
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public final Enumeration getAttributeNames() {
		return Collections.enumeration(getAttributeNameSet());
	}
	
	
	/**
	 * returns the number of parsed characters
	 * @return the number of parsed characters
	 */
	int getCountParsedChars() {
		return parsedChars;
	}

	
	/**
	 * increments the number of parsed characters
	 *  
	 * @param parsedChars the number of additional parsed characters 
	 * @return the number of parsed characters
	 */
	int incCountParsedChars(int parsedChars) {
		this.parsedChars += parsedChars;
		
		return this.parsedChars;
	}

		
	/**
	 * {@inheritDoc}
	 */
	public final int getContentLength() {
		return contentLength;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public final void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}

		
	/**
	 * copy the headers 
	 * 
	 * @param otherHeader         the other header
	 * @param upperExcludenames   the header names to exclude
	 */
	public final void copyHeaderFrom(IHttpMessageHeader otherHeader, String... upperExcludenames) {
		ol : for (String headername : otherHeader.getHeaderNameSet()) {
			
			String upperheadername = headername.toUpperCase(Locale.US); 
			for (String upperExcludename : upperExcludenames) {
				if (upperheadername.equals(upperExcludename)) {
					continue ol;
				}
			}

			for (String headervalue : otherHeader.getHeaderList(headername)) {
				addHeader(headername, headervalue);
			}
		}
	}
	

	
	
	boolean onHeaderAdded(String headername, String headervalue) {
		if (headername.equalsIgnoreCase(CONTENT_LENGTH)) {
		    try {
		        contentLength = Integer.parseInt(headervalue);
		    } catch (NumberFormatException nfe) {
		        if (LOG.isLoggable(Level.FINE)) {
		            LOG.fine("error oocured by paring content length " + headervalue + " " + nfe.toString());
		        }
		        return super.onHeaderAdded(headername, headervalue);
		    }
			return true;
		}
		
		return super.onHeaderAdded(headername, headervalue);
	}
	
	


	boolean onHeaderRemoved(String headername) {
		
		if (headername.equalsIgnoreCase(CONTENT_LENGTH)) {
			contentLength = -1;
			return true;
		}
		
		
		
		return super.onHeaderRemoved(headername);
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public boolean containsHeader(String headername) {
		
		if ((contentLength != -1) && headername.equalsIgnoreCase(CONTENT_LENGTH)) {
			return true;
		}
		
	
		return super.containsHeader(headername);
	}

	

	
	/**
	 * {@inheritDoc}
	 */
	public Set<String> getHeaderNameSet() {
		
		HashSet<String> headerNames = new HashSet<String>();
			
		if (contentLength != -1) {
			headerNames.add(CONTENT_LENGTH);
		}
		
		headerNames.addAll(super.getHeaderNameSet());
				
		return headerNames;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public List<String> getHeaderList(String headername) {
		
		if (headername.equalsIgnoreCase(CONTENT_LENGTH)) {
			List<String> result = new ArrayList<String>();
			result.add(Integer.toString(contentLength));
			return Collections.unmodifiableList(result); 
		}
		
		return super.getHeaderList(headername);
	}
	
		

	/**
	 * {@inheritDoc}
	 */
	public String getHeader(String headername) {
		
		if (headername.equalsIgnoreCase(CONTENT_LENGTH)) {
		    if (contentLength == -1) {
		        return null;
		    } else {
		        return Integer.toString(contentLength);
		    }
		} 

		return super.getHeader(headername);
	}
	
	
    /**
     * {@inheritDoc}
     */
    public final void removeHopByHopHeaders() {
        
        for (String connectionHeader : getHeaderList("Connection")) {
            removeHeader(connectionHeader);
        }
        
        removeHeader("Connection");            
        removeHeader("Proxy-Connection");
        removeHeader("Keep-Alive");
        removeHeader("Proxy-Authenticate");
        removeHeader("Proxy-Authorization");
        removeHeader("TE");
        removeHeader("Trailers");
        removeHeader("Upgrade");
        
        if ((getHeader("Transfer-Encoding") !=  null) && !(getHeader("Transfer-Encoding").equalsIgnoreCase("chunked"))) { 
            removeHeader("Transfer-Encoding");
        }       
    }
    

	
	
	final void writeHeadersTo(StringBuilder sb) {
		
		if (contentLength != -1) {
			sb.append("Content-Length: ");
			sb.append(contentLength);
			sb.append("\r\n");
		}
	
		super.writeHeadersTo(sb);
	}
	

	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Object clone() throws CloneNotSupportedException {
		HttpMessageHeader copy = (HttpMessageHeader) super.clone();
		copy.attributes = (HashMap<String, Object>) this.attributes.clone();
		
		return copy;
	}
	

	
	
	/**
	 * returns the scheme  
	 * 
	 * @param protcolLeft  the scheme token  
	 * @param connection    the http connection    
	 * @return the scheme
	 */
	static String computeScheme(String protcolLeft, INonBlockingConnection connection) {
		if ((protcolLeft == null) || protcolLeft.equalsIgnoreCase("HTTP")) {
			if (connection.isSecure()) {
				return "https";
			} else {
				return "http";
			}
		}
		
		return null;
	}	
	
	
	/**
	 * {@inheritDoc}
	 */
	public IHttpMessageHeader copy() {
		try {
			return (IHttpMessageHeader) this.clone();
		} catch (CloneNotSupportedException cnse) {
			throw new RuntimeException(cnse.toString());
		}
	}
}
