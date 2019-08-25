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



/**
 * a full body data source
 *  
 * @author grro@xlightweb.org
 */
final class FullMessageBodyDataSource extends AbstractNetworkBodyDataSource {

	private static final Logger LOG = Logger.getLogger(FullMessageBodyDataSource.class.getName());
	
	private final int size;
	private int remaining;
	
	
	public FullMessageBodyDataSource(HttpMessageHeader header, int size, AbstractHttpConnection httpConnection) throws IOException {
		super(header, httpConnection);
		
		this.size = size;
		remaining = size;
		
		postCreate();
	}
	
	


	
	@Override
	void doParse(ByteBuffer[] rawData) throws IOException {
	    
	    if (LOG.isLoggable(Level.FINE)) {
	        LOG.fine("parsing full message body (" + (size - remaining) + " of " + size + " read. New available=" + HttpUtils.computeRemaining(rawData) + ")");
	    }

        remaining = readByteBufferByLength(rawData, remaining);
        
        if (remaining == 0) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] complete body received (size=" + size + ")");
            }
            
            setComplete();
            return;
            
        } else {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("[" + getId() + "] body data read. wating for more data");
            }
            return;
        }
	}
	
	
	private int received() {
	    return (size - remaining);
	}

	
	@Override
	void onClose() throws IOException {
	    if (remaining > 0) {
            throw new ProtocolException("connection has been closed (by user?) while receiving body data. only " + received() + " of " + size + " bytes have been recevied (FullMessage)", getHeader()); 
        }       
	}

	
	@Override
	void performOnDisconnect() throws ProtocolException {
	    if (remaining > 0) {
	        throw new ProtocolException("connection " + getId() + " has been closed (by peer?) while receiving body data. only " + received() + " of " + size + " bytes have been recevied. (FullMessage)", getHeader()); 
	    }
    }
}

 