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
 * header wrapper
 * 
 * @author grro@xlightweb.org
 */
abstract class HeaderWrapper implements IHeader {

	
	private final IHeader header;
	

	/**
	 * constructor 
	 * 
	 * @param header the header to wrap
	 */
	public HeaderWrapper(IHeader header) {
		this.header = header;
	}
	
	
	protected IHeader getWrappedHeader() {
		return header;
	}
	

	/**
	 * {@inheritDoc}
	 */
	public void addHeader(String headername, String headervalue) {
		header.addHeader(headername, headervalue);		
	}


	/**
	 * {@inheritDoc}
	 */
	public void addHeaderLine(String line) {
		header.addHeaderLine(line);		
	}

	
	/**
     * {@inheritDoc}
     */
	public void addHeaderlines(String... lines) {
	    header.addHeaderlines(lines);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean containsHeader(String headername) {
		return header.containsHeader(headername);
	}


	/**
	 * {@inheritDoc}
	 */
	public String getCharacterEncoding() {
		return header.getCharacterEncoding();
	}


	/**
	 * {@inheritDoc}
	 */
	public String getContentType() {
		return header.getContentType();
	}
	
	
	/**
     * {@inheritDoc}
     */
	public String getDisposition() {
	    return header.getDisposition();
	}

	
	/**
     * {@inheritDoc}
     */
	public String getDispositionType() {
	    return header.getDispositionType();
	}

	
	/**
     * {@inheritDoc}
     */
	public String getDispositionParam(String name) {
	    return header.getDispositionParam(name);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public String getHeader(String headername) {
		return header.getHeader(headername);
	}


    /**
     * {@inheritDoc}
     */
	public String getHeader(String headername, String dfltValue) {
	    return header.getHeader(headername, dfltValue);
	}
	

	/**
	 * {@inheritDoc}
	 */
	public List<String> getHeaderList(String headername) {
		return header.getHeaderList(headername);
	}


	/**
	 * {@inheritDoc}
	 */
	public Set<String> getHeaderNameSet() {
		return header.getHeaderNameSet();
	}


	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public Enumeration getHeaderNames() {
		return header.getHeaderNames();
	}


	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public Enumeration getHeaders(String headername) {
		return header.getHeaders(headername);
	}


	/**
	 * {@inheritDoc}
	 */
	public String getTransferEncoding() {
		return header.getTransferEncoding();
	}



	/**
	 * {@inheritDoc}
	 */
	public void removeHeader(String headername) {
		header.removeHeader(headername);		
	}
	

	/**
	 * {@inheritDoc}
	 */
	public void setContentType(String type) {
		header.setContentType(type);		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setHeader(String headername, String headervalue) {
		header.setHeader(headername, headervalue);		
	}


	/**
	 * {@inheritDoc}
	 */
	public void setTransferEncoding(String transferEncoding) {
		header.setTransferEncoding(transferEncoding);		
	}	
	
	@Override
	public String toString() {
		return header.toString();
	}
}
