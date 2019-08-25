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

import java.io.File;
import java.io.IOException;

import java.nio.ByteBuffer;

import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.xsocket.DataConverter;



/**
 * Part
 * 
 * @author grro@xlightweb.org
 */
public class Part implements IPart {


	private final IHeader header;
	private final AtomicReference<NonBlockingBodyDataSource> bodyDataSourceRef = new AtomicReference<NonBlockingBodyDataSource>();
	
	Part(IHeader header) {
		this.header = header;
	}

	/**
	 * constructor 
	 * 
	 * @param contentType the content type
	 * @param body the body 
	 * @throws IOException if an exception occurs
	 */
	public Part(String contentType, String body) throws IOException {
		this(new Header(contentType), body);
	}

	/**
	 * constructor 
	 * 
	 * @param header the header
	 * @param body   the body 
	 * @throws IOException if an exception occurs 
	 */
	public Part(IHeader header, String body) throws IOException {
		this(header, convert(header, body));
	}

	
	/**
	 * constructor 
	 * 
	 * @param contentType the content type
	 * @param body the body 
	 * @throws IOException if an exception occurs
	 */
	public Part(String contentType, ByteBuffer[] body) throws IOException {
		this(new Header(contentType), body);
	}

	
	   
    /**
     * constructor 
     * 
     * @param file   the file 
     * @throws IOException if an exception occurs 
     */
    public Part(File file) throws IOException {
        this(new Header(), file);
    }

	
	/**
	 * constructor 
	 * 
	 * @param header the header
	 * @param file   the file 
	 * @throws IOException if an exception occurs 
	 */
	public Part(IHeader header, File file) throws IOException {
		this(header);
		setBody(new FileDataSource(header, HttpUtils.newMultimodeExecutor(), file));
		if (header.getContentType() == null) {
			header.setContentType(HttpUtils.resolveContentTypeByFileExtension(file));
		}
	}

	
	/**
	 * constructor 
	 * 
	 * @param header the header
	 * @param body   the body 
	 * @throws IOException if an exception occurs 
	 */
	public Part(IHeader header, ByteBuffer[] body) throws IOException {
		this(header);
		setBody(header, body);
	}

	
	/**
	 * constructor 
	 * 
	 * @param header the header
	 * @param body   the body 
	 * @throws IOException if an exception occurs 
	 */
	public Part(IHeader header, NonBlockingBodyDataSource body) throws IOException {
		this(header);
		setBody(body);
	}



	/**
	 * {@inheritDoc}
	 */
	public IHeader getPartHeader() {
		return header;
	}

	
	
	/**
	 * {@inheritDoc}
	 */
	public final NonBlockingBodyDataSource getNonBlockingBody() throws IOException {
	    NonBlockingBodyDataSource ds = bodyDataSourceRef.get();
	    if (ds == null)  {
	        try {
	            ds = new InMemoryBodyDataSource(header);
	            ds.setComplete();
	        } catch (IOException ioe) {
	            throw new RuntimeException(ioe);
	        }
	    } 
	        
	    return ds;
	}

	
	/**
	 * {@inheritDoc}
	 */
	public final boolean hasBody() {
		return bodyDataSourceRef.get() != null;
	}



	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("deprecation")
    public final BlockingBodyDataSource getBlockingBody() throws IOException {
	    return new BlockingBodyDataSource(getNonBlockingBody());
	}


   /**
     * {@inheritDoc}
     */	
    public BodyDataSource getBody() throws IOException {
         return new BodyDataSource(getNonBlockingBody());
	}


	/**
	 * {@inheritDoc}
	 */
	public final void addHeader(String headername, String headervalue) {
		header.addHeader(headername, headervalue);
	}

	
	/**
	 * {@inheritDoc}
	 */
	public final void addHeaderLine(String line) {
		header.addHeaderLine(line);
	}
	
	
	/**
     * {@inheritDoc}
     */
	public final void addHeaderlines(String... lines) {
	    header.addHeaderlines(lines);
	}


	/**
	 * {@inheritDoc}
	 */
	public final boolean containsHeader(String headername) {
		return header.containsHeader(headername);
	}
	

	/**
	 * {@inheritDoc}
	 */
	public final String getCharacterEncoding() {
		return header.getCharacterEncoding();
	}
	
	

	/**
	 * {@inheritDoc}
	 */
	public final String getContentType() {
		return header.getContentType();
	}


    /**
     * {@inheritDoc}
     */
	public final String getDisposition() {
	    return header.getDisposition();
	}
	

	/**
     * {@inheritDoc}
     */
	public final String getDispositionParam(String name) {
	    return header.getDispositionParam(name);
	}
	

	/**
     * {@inheritDoc}
     */
	public final String getDispositionType() {
	    return header.getDispositionType();
	}
	

	/**
	 * {@inheritDoc}
	 */
	public final String getHeader(String headername) {
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
	public final List<String> getHeaderList(String headername) {
		return header.getHeaderList(headername);
	}



	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public final Enumeration getHeaderNames() {
		return header.getHeaderNames();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public final Set<String> getHeaderNameSet() {
		return header.getHeaderNameSet();
	}
	

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public final Enumeration getHeaders(String headername) {
		return header.getHeaders(headername);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public final String getTransferEncoding() {
		return header.getTransferEncoding();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public final void removeHeader(String headername) {
		header.removeHeader(headername);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public final void setContentType(String type) {
		header.setContentType(type);
	}


	/**
	 * {@inheritDoc}
	 */
	public final void setHeader(String headername, String headervalue) {
		header.setHeader(headername, headervalue);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public final void setTransferEncoding(String transferEncoding) {
		header.setTransferEncoding(transferEncoding);
		removeHeader("Content-length");
	}	
	
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		if (bodyDataSourceRef.get() == null) {
			return header.toString() + "\r\n";
		} else {
		    return header.toString() + "\r\n" + bodyDataSourceRef.toString();
		}
	}
	
	
	final void setBody(IHeader header, ByteBuffer[] body) throws IOException {
		setBody(new InMemoryBodyDataSource(header, body));
	}
	
	boolean setBody(NonBlockingBodyDataSource body) throws IOException {
        if (body == null) {
        	return false;
        }
        
        bodyDataSourceRef.set(body);
		return true; 
	}
	
	
	   
    static ByteBuffer[] convert(IHeader header, String body) {
        if ((header.getContentType() != null) && (HttpUtils.isTextMimeType(header.getContentType()) && (HttpUtils.parseEncoding(header.getContentType()) == null))) {
        	header.setContentType(header.getContentType() + "; charset=utf-8");
            
        }
        
        return new ByteBuffer[] { DataConverter.toByteBuffer(body, header.getCharacterEncoding()) };
    }
}