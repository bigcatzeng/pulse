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
import org.xsocket.MaxWriteSizeExceededException;
import org.xsocket.connection.IWriteCompletionHandler;



/**
 * I/O resource capable of receiving the body data.
 * 
 * @author grro@xlightweb.org
 *
 */
final class FullMessageBodyDataSink extends AbstractNetworkBodyDataSink {

    private static final Logger LOG = Logger.getLogger(FullMessageBodyDataSink.class.getName());

    private final int contentLength;
    
    private int remaining = 0;
    private boolean isHeaderWritten = false;
    
    
    public FullMessageBodyDataSink(AbstractHttpConnection httpConnection, IHttpMessageHeader header, int contentLength) throws IOException {
        super(header, httpConnection);
        
        this.contentLength = contentLength;
        remaining = contentLength;
    }
    
    
    int getSizeWritten() {
        return (contentLength - remaining);
    }

    
    @Override
    int onWriteNetworkData(ByteBuffer[] dataToWrite, IWriteCompletionHandler completionHandler) throws IOException {
        
        if (remaining <= 0) {
            return 0;
        }
        
        int bodyWritten = 0;
        AbstractHttpConnection con = getConnection();
        
        if (con != null) {
            
            try {
                int dataWritten = 0;
                
                if (!isHeaderWritten) {
                    isHeaderWritten = true;
                    
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + getId() + "] writing header and body");
                    }
                    
                    dataWritten = con.write(getHeader().toString() + "\r\n"); 
                    bodyWritten = writeBody(con, dataToWrite, completionHandler);
                    dataWritten += bodyWritten; 
                    
                    con.incCountMessageSent();
                    
                } else {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + getId() + "] writing body (header is already written)");
                    }

                    bodyWritten = writeBody(con, dataToWrite, completionHandler);
                    dataWritten = bodyWritten;
                }
        
                if (dataWritten > 0) {
                    con.flush();
                }
                
            } catch (IOException ioe) {
                if (isIgnoreWriteError()) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + getId()+ "] error occured by flushing bound data sink. Ignoring error (ignoreWriteError=true) " + DataConverter.toString(ioe));
                    }
                    
                    if (completionHandler != null) {
                        completionHandler.onWritten(dataToWrite.length);
                    }
                    
                } else {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + getId()+ "] error occured by flushing bound data sink (data: " + DataConverter.toString(dataToWrite) + "). Destroying connection. reason " + DataConverter.toString(ioe));
                    }
                    
                    destroy();
                    throw ioe;
                }
            }
        }
        
        return bodyWritten;
    }
    

    private int writeBody(AbstractHttpConnection con, ByteBuffer[] bodyData , IWriteCompletionHandler completionHandler) throws IOException {
      
        int written = 0; 
        if (HttpUtils.isEmpty(bodyData)) {
            if (completionHandler != null) {
                completionHandler.onWritten(0);
            }
            
        } else {
            written = (int) con.write(bodyData, completionHandler);
        }
        
        remaining -= written;
        if (remaining == 0) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] full size written. Closing data sink");
            }
            close();
        } else if (remaining < 0) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("ERROR: try to write more data " + written + " than declared (" + contentLength + ") throwing exception");
            }
            throw new MaxWriteSizeExceededException();
        }
        
        return written;
    }
    
    
    @Override
    void performDestroy() throws ProtocolException {
        if (remaining > 0) {
            throw new ProtocolException("destroying data sink. Not all data (FullMessage) is written. only " + (contentLength - remaining) + " bytes of "  + contentLength + " have been send", getHeader()); 
        }        
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    void performClose() throws ProtocolException {
        if (remaining > 0) {
            throw new ProtocolException("illegal close of (FullMessage) data sink. only " + (contentLength - remaining) + " bytes of "  + contentLength + " have been send. connection will be destroyed", getHeader()); 
            
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" +getId() + "] " + DataConverter.toFormatedBytesSize(getSizeWritten()) + " body data written");
            }
        }
    }
    
    
    

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString() + " (contentLength=" + contentLength + ", written=" + getSizeWritten() + ")";
    }
}
	    
