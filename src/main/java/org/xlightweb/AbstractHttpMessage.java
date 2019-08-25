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

import java.io.IOException;
import java.nio.ByteBuffer;


import java.util.Enumeration;
import java.util.Set;

import org.xsocket.DataConverter;







/**
 * Implementation base of a message
 * 
 * @author grro@xlightweb.org
 */
abstract class AbstractHttpMessage extends Part implements IHttpMessage {
	
	AbstractHttpMessage(IHttpMessageHeader header) {
		super(header);
	}

	/**
	 * {@inheritDoc}
	 */
	public final IHttpMessageHeader getMessageHeader() {
		return (IHttpMessageHeader) getPartHeader();
	}
	

	/**
	 * {@inheritDoc}
	 */
	public final void setAttribute(String name, Object o) {
		getMessageHeader().setAttribute(name, o);
	}

	
	/**
	 * {@inheritDoc}
	 */
	public final Object getAttribute(String name) {
		return getMessageHeader().getAttribute(name);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public final Enumeration getAttributeNames() {
		return getMessageHeader().getAttributeNames();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public final Set<String> getAttributeNameSet() {
		return getMessageHeader().getAttributeNameSet();
	}

	

    
	
	/**
	 * {@inheritDoc}
	 */
	public final int getContentLength() {
		return getMessageHeader().getContentLength();
	}

	
	/**
	 * {@inheritDoc}
	 */	
	public final void setContentLength(int length) {
		getMessageHeader().setContentLength(length);
	}
	

	/**
	 * {@inheritDoc}
	 */
	public final void removeHopByHopHeaders() {
	    getMessageHeader().removeHopByHopHeaders();
	    
	    // body complete and no length indicator set?
        try {
            if (hasBody() && (getContentLength() == -1) && (getHeader("Transfer-Encoding") == null)) {
                
                if (HttpUtils.hasContentType(getPartHeader(), "text/event-stream")) {
                    setHeader("Connection", "close"); 
                } else if (getNonBlockingBody().isComplete()) {
                    setContentLength(getNonBlockingBody().getSize());
                } else {
                    setHeader("Transfer-Encoding", "chunked");
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
	}
	
	
	final void setBody(IHttpMessageHeader header, ByteBuffer[] body, boolean compress) throws IOException {
        if (compress) {
            byte[] data = HttpUtils.compress(DataConverter.toBytes(body));
            setBody(new InMemoryBodyDataSource(header, data));
            getNonBlockingBody().getHeader().setHeader("Content-Encoding", "gzip");
            
        } else {
        	super.setBody(header, body);
        }
        
        header.setContentLength(getNonBlockingBody().available());
	}

	
	@Override
	boolean setBody(NonBlockingBodyDataSource body) throws IOException {
		boolean isAdded = super.setBody(body);
		
		if (isAdded) {
		    
            if (AbstractHttpConnection.isChunkedTransferEncoding(getMessageHeader())) {
                return isAdded;
                
            } else if (getMessageHeader().getContentLength() >= 0) {
                return isAdded;
            }
            
            if (getNonBlockingBody().isComplete()) {
                getMessageHeader().setContentLength(getNonBlockingBody().available());
                
            } else {
                    
                if (getNonBlockingBody() instanceof FullMessageBodyDataSource) {
                    getMessageHeader().setContentLength(((FullMessageBodyDataSource) getNonBlockingBody()).getSize());
                
                } else if (getNonBlockingBody() instanceof FullMessageChunkedBodyDataSource) {
                    getMessageHeader().setTransferEncoding("chunked");
                    
                } else if (getNonBlockingBody() instanceof MultipartByteRangeMessageBodyDataSource) {
                    if (getMessageHeader().getContentType().toLowerCase().startsWith("multipart/byteranges")) {
                        return isAdded;
                    }
                    String separator = ((MultipartByteRangeMessageBodyDataSource) getNonBlockingBody()).getSeparator();
                    getMessageHeader().setContentType("Content-Type: multipart/byteranges; boundary=" + separator); 
                    
                } 
            }
		}
		
		return isAdded;
	}
}