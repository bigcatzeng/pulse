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
import java.util.List;
import java.util.Set;

import org.xsocket.DataConverter;




/**
 * http response header
 *
 * @author grro@xlightweb.org
 */
public class HttpResponseHeader extends HttpMessageHeader implements IHttpResponseHeader {
	
	 
	// response elements
	private int statusCode = -1;
	private String reason; 

	
	// cache
	private String server;
	private String date;
	
	
 	
	/**
	 * constructor 
	 * 
	 */
	HttpResponseHeader() {
	
	}

	
	/**
	 * constructor 
	 * 
	 * @param statusCode  the status code
	 */
	public HttpResponseHeader(int statusCode) {
		this(statusCode, null, HttpUtils.getReason(statusCode));
	}

	
	/**
	 * constructor. The status will be set to 200 
	 * 
	 * @param contentType  the content type 
	 */
	public HttpResponseHeader(String contentType) {
		this(200, contentType);
	}

	

	/**
	 * constructor 
	 * 
	 * @param statusCode   the status code 
	 * @param contentType  the content type
	 */
	public HttpResponseHeader(int statusCode, String contentType) {
		this(statusCode, contentType, HttpUtils.getReason(statusCode));
	}

	
	
	/**
	 * constructor 
	 * 
	 * @param statusCode   the status code
	 * @param contentType  the content type
	 * @param reason       the reason
	 */
	public HttpResponseHeader(int statusCode, String contentType, String reason) {
		this("1.1", statusCode, contentType, reason);
	}
	


	HttpResponseHeader(String protocolVersion, int statusCode, String contentType, String reason) {
		
		setProtocolSchemeSilence("HTTP");
		setProtocolVersionSilence(protocolVersion);
		this.statusCode = statusCode;
		this.reason = reason;
		
		if (contentType != null) {
			setContentType(contentType);
		}
	}


	

	/**
	 * {@inheritDoc}
	 */
	final public void copyHeaderFrom(HttpResponseHeader otherHeader, String... upperExcludenames) {
		super.copyHeaderFrom(otherHeader, upperExcludenames);
	}
	
	

	
	/**
	 * {@inheritDoc}
	 */
	final public int getStatus() {
		return statusCode;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	final public void setStatus(int status) {
		this.statusCode = status;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	final public String getReason() {
		return reason;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	final public void setReason(String reason) {
		this.reason = reason;
	}
	


    /**
     * set the caching expires headers of a response 
     * 
     * @param expireSec  the expire time or 0 to set no-cache headers
     */
	final public void setExpireHeaders(int expireSec) {
    	HttpUtils.setExpireHeaders(this, expireSec);
    }

    
    /**
     * set a last modified header of the response
     * 
     * @param timeMillis  the last modified time in millis
     */
	final public void setLastModifiedHeader(long timeMillis) {
    	HttpUtils.setLastModifiedHeader(this, timeMillis);
    }
    
    /**
     * set the date header of the response
     * 
     * @param timeMillis  the last modified time in millis
     */
	final public void setDate(long timeMillis) {
    	setDate(DataConverter.toFormatedRFC822Date(timeMillis));
    }

    
	/**
	 * {@inheritDoc}
	 */
	final public void setProtocol(String protocol) {
		int idx = protocol.indexOf("/");
		setProtocolSchemeSilence(protocol.substring(0, idx));
		setProtocolVersionSilence(protocol.substring(idx + 1, protocol.length()));
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	final public String getDate() {
		return getHeader(DATE);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	final public void setDate(String date) {
		setHeader(DATE, date);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	final public void setServer(String server) {
		this.server = server;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	final public String getServer() {
		return server;
	}
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	final protected boolean onHeaderAdded(String headername, String headervalue) {
		
		if (headername.equalsIgnoreCase("Server")) {
			server = headervalue;
			return true;
		} 
		
		if (headername.equalsIgnoreCase("Date")) {
			date = headervalue;
			return true;
		}
		
		return super.onHeaderAdded(headername, headervalue);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	final protected boolean onHeaderRemoved(String headername) {

		if (headername.equalsIgnoreCase("Server")) {
			server = null;
			return true;
			
		} 

		if (headername.equalsIgnoreCase("Date")) {
			date = null;
			return true;
		}
		
		return super.onHeaderRemoved(headername);
	}
	
	

	/**
	 * {@inheritDoc}
	 */
	final public Set<String> getHeaderNameSet() {
		
		Set<String> headerNames = super.getHeaderNameSet();
			
		if (server != null) {
			headerNames.add(SERVER);
		}
			
		if (date != null) {
			headerNames.add(DATE);
		}
		
		
		return headerNames;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	final public List<String> getHeaderList(String headername) {
		
		if ((headername.equalsIgnoreCase(SERVER)) && (server != null)) {
			List<String> result = new ArrayList<String>();
			result.add(server);
			return Collections.unmodifiableList(result);
		}
		
		if ((headername.equalsIgnoreCase(DATE)) && (date != null)) {
			List<String> result = new ArrayList<String>();
			result.add(date);
			return Collections.unmodifiableList(result);
		} 

		
		return super.getHeaderList(headername);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	final public String getHeader(String headername) {
		
		if ((headername.equalsIgnoreCase(SERVER)) && (server != null)) {
			return server;
		}
		
		if ((headername.equalsIgnoreCase(DATE)) && (date != null)) {
			return date;
		} 
		
		return super.getHeader(headername);
	} 
	
	
	/**
	 * {@inheritDoc}
	 */
	final public boolean containsHeader(String headername) {
		
		if ((headername.equalsIgnoreCase(SERVER)) && (server != null)) {
			return true;
		}

		if ((headername.equalsIgnoreCase(DATE)) && (date != null)) {
			return true;
		}

		return super.containsHeader(headername);
	}

	
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	final protected Object clone() throws CloneNotSupportedException {
		HttpResponseHeader copy = (HttpResponseHeader) super.clone();
		return copy;
	}

	
	
	/**
	 * {@inheritDoc}
	 */
	final public IHttpResponseHeader copy() {
		try {
			return (IHttpResponseHeader) this.clone();
		} catch (CloneNotSupportedException cnse) {
			throw new RuntimeException(cnse.toString());
		}
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(256);
		
		if (getProtocolScheme() != null) {
			sb.append(getProtocolScheme());
			sb.append("/");
			sb.append(getProtocolVersion());
			sb.append(" ");
		}
		
		if (statusCode != -1) {
		    sb.append(statusCode);
		}
		
		if (reason != null) {		
			sb.append(" ");
			sb.append(reason);
		}
		
		sb.append("\r\n");

	
		if (server != null) {
			sb.append("Server: ");
			sb.append(server);
			sb.append("\r\n");
		} 

		if (date != null) {
			sb.append("Date: ");
			sb.append(date);
			sb.append("\r\n");
		} 

		
		
		writeHeadersTo(sb);
		
		return sb.toString();
	}
}	