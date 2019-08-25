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
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xsocket.DataConverter;
import org.xsocket.connection.IWriteCompletionHandler;



/**
 * I/O resource capable of receiving the body data.
 * 
 * @author grro@xlightweb.org
 *
 */
final class FullMessageChunkedBodyDataSink extends AbstractNetworkBodyDataSink {

    private static final Logger LOG = Logger.getLogger(FullMessageChunkedBodyDataSink.class.getName());
    private static final byte[] DELIMITER = new byte[] { 13, 10 };

    private boolean isHeaderWritten = false;

    private int writtenData;
    private final ArrayList<Integer> writtenChunks = new ArrayList<Integer>();
    
    
    public FullMessageChunkedBodyDataSink(AbstractHttpConnection httpConnection, IHttpMessageHeader header) throws IOException {
        super(header, httpConnection);
    }
    
    
    @Override
    int onWriteNetworkData(ByteBuffer[] dataToWrite, IWriteCompletionHandler completionHandler) throws IOException {

        try {
            return writeChunk(dataToWrite, completionHandler);
            
        } catch (IOException ioe) {
            if (isIgnoreWriteError()) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + getId()+ "] error occured by flushing chunked data sink. Ignoring error (ignoreWriteError=true) " + DataConverter.toString(ioe));
                }
                
                if (completionHandler != null) {
                    completionHandler.onWritten(dataToWrite.length);
                }
                
            } else {                
                if (getConnection() != null) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + getId()+ "] error occured by flushing chunked data sink. Destroying connection. reason " + DataConverter.toString(ioe));
                    }
                
                    destroy();
                    throw ioe;
                }
            }
            
            return 0;
        }
    }
    
    

    private int writeChunk(ByteBuffer[] bodyData, IWriteCompletionHandler completionHandler) throws IOException {
        
        AbstractHttpConnection con = getConnection();
        if (con != null) {
            
            // write header (if not already written)
            if (!isHeaderWritten) {
                isHeaderWritten = true;
                
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + getId()+ "]  sending header ");
                }
                written += con.write(getHeader().toString() + "\r\n");       
                
                con.incCountMessageSent();
            }
    
            
            // write body (if content size larger than 0)
            int length = 0;
            
            if (bodyData != null) {
                for (ByteBuffer buffer : bodyData) {
                    length += buffer.remaining();
                }
            }
            
            if (length > 0) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + getId()+ "] writing chunk (size=" + length + ")");
                }
                
                // write length field
                written += con.write(Integer.toHexString(length) + "\r\n");
                
            
                
                // write data 
                written += con.write(bodyData);
                written += con.write(DELIMITER, completionHandler);
                
                writtenData += length;
                writtenChunks.add(length);
                
            } else {
                if (completionHandler != null) {
                    completionHandler.onWritten(0);
                }
            }
            
            if (written > 0) {
                con.flush();
            }
            
            return length;
            
        } else {
            return 0;
        }
    }

    
  
    /**
     * {@inheritDoc}
     */
    void performClose() throws IOException {

        AbstractHttpConnection con = getConnection();
        if (con == null) {
            return;
        }
        
        try {
            
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId()+ "] closing chunked body by writing termination chunk (body size=" + writtenData + ")");
            }

            
            if (!isHeaderWritten) {
                isHeaderWritten = true;
                
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + getId()+ "]  sending header ");
                }
                
                if (written == 0) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("body is empty. switch from chunkedbody to full message body (remove Transfer-Encoding and add Content-Length re-write header with content-length 0)");
                    }
                    
                    getHeader().removeHeader("Transfer-Encoding");
                    ((HttpMessageHeader) getHeader()).setContentLength(0);
                    con.write(getHeader().toString() + "\r\n");       
                    con.incCountMessageSent();
                    con.flush();
                    return;
                    
                } else {
                    con.write(getHeader().toString() + "\r\n");       
                    con.incCountMessageSent();
                }
            }
            // write termination chunk
            con.write("0" + "\r\n\r\n");
            con.flush();
            
        } catch (IOException ioe) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId()+ "] error occured by closing chunked data sink. Destroying connection. reason " + DataConverter.toString(ioe));
            }
                
            con.destroy();
            throw ioe;
        }
    }

    
    @Override
    void performDestroy() throws IOException {
    }
    

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        
        StringBuilder sb = new StringBuilder();
        for (int chunkSize : writtenChunks) {
            sb.append(chunkSize + ", ");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 2);
        }
        
        return super.toString() + " (written=" + writtenData + " [" + sb.toString() + "])";
    }
}
	    
