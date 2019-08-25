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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xsocket.DataConverter;



/**
 * a full body data source
 *  
 * @author grro@xlightweb.org
 */
final class MultipartByteRangeMessageBodyDataSource extends AbstractNetworkBodyDataSource {

	private static final Logger LOG = Logger.getLogger(MultipartByteRangeMessageBodyDataSource.class.getName());
	
	private String separator;
    private String endSeparator;
    private int separatorIdx = 0;
    private boolean isRead = false;
    
    private final StringBuilder stringBuilder = new StringBuilder(); 

	
	/**
	 * constructor 
	 * 
	 * @param httpConnection   the http connection
	 * @param header           the header
	 * @throws IOException if an exception occurs
	 */
	public MultipartByteRangeMessageBodyDataSource(AbstractHttpConnection httpConnection, HttpMessageHeader header) throws IOException {
		super(header, httpConnection);
		
        String contentType = header.getContentType();
        for (String token : contentType.split(";")) {
            token = token.trim();
            if (token.toLowerCase().startsWith("boundary=")) {
                separator = token.substring("boundary=".length(), token.length()).trim();
                endSeparator = "--" +  separator + "--";
            }
        }
        
		postCreate();
	}
	
	
	String getSeparator() {
	    return separator;
	}

	
	@Override
	void doParse(ByteBuffer[] rawData) throws IOException {
        
        for (ByteBuffer buffer : rawData) {
            if (buffer == null) {
                continue;
            }
            
            int remaining = buffer.remaining();
        
            for (int i = 0; i < remaining; i++) {
                byte b = buffer.get();
                
                if (b == (byte) endSeparator.charAt(separatorIdx)) {
                    separatorIdx++;
                    if (separatorIdx == endSeparator.length()) {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("body read. set complete true");
                        }
                        stringBuilder.append(endSeparator);
                        stringBuilder.append("\r\n");
                        
                        onReadNetworkData(DataConverter.toByteBuffer(stringBuilder.toString(), getHeader().getCharacterEncoding()));
                        isRead = true;
                        setComplete();
                        return;
                    }
                    
                } else {
                    if (separatorIdx > 0) {
                        stringBuilder.append(endSeparator.substring(0, separatorIdx));
                        separatorIdx = 0;
                    } else {
                        stringBuilder.append((char) b);
                    }
                }
            }
        }
    }


	@Override
	void onClose() throws IOException {
	    if (!isRead) {
	        throw new ProtocolException("connection has been closed (by user?) while receiving body data. (MultipartByteRangeMessage)", null);	        
	    }
	}

	@Override
	void performOnDisconnect() throws ProtocolException {
	    if (!isRead) {
	        throw new ProtocolException("connection has been closed (by peer?) while receiving body data. (MultipartByteRangeMessage)", null); 
	    }
    }
}

 