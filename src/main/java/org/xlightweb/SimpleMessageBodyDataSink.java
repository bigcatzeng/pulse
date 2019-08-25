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
import org.xsocket.connection.IWriteCompletionHandler;



/**
 * I/O resource capable of receiving the body data.
 * 
 * @author grro@xlightweb.org
 *
 */
final class SimpleMessageBodyDataSink extends AbstractNetworkBodyDataSink {

    private static final Logger LOG = Logger.getLogger(SimpleMessageBodyDataSink.class.getName());

    private boolean isHeaderWritten = false;

    
    
    public SimpleMessageBodyDataSink(AbstractHttpConnection httpConnection, IHttpMessageHeader header) throws IOException {
        super(header, httpConnection);
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    int onWriteNetworkData(ByteBuffer[] dataToWrite, IWriteCompletionHandler completionHandler) throws IOException {

        int bodyWritten = 0;
        
        AbstractHttpConnection con = getConnection();
        if (con != null) {
            
            try {
                int dataWritten = 0;
                
                if (!isHeaderWritten) {
                    isHeaderWritten = true;
                    
                    dataWritten = con.write(getHeader().toString() + "\r\n");
                    bodyWritten = writeBody(con, dataToWrite, completionHandler);
                    dataWritten += bodyWritten; 
                    
                    con.incCountMessageSent();
                    
                } else {
                    bodyWritten = writeBody(con, dataToWrite, completionHandler);
                    dataWritten += bodyWritten;
                }
        
                if (dataWritten > 0) {
                    con.flush();
                }
                
            } catch (IOException ioe) {
                if (isIgnoreWriteError()) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + getId()+ "] error occured by flushing simple body data sink. Ignoring error (ignoreWriteError=true) " + DataConverter.toString(ioe));
                    }
                    
                    if (completionHandler != null) {
                        completionHandler.onWritten(dataToWrite.length);
                    }

                } else {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + getId()+ "] error occured by flushing simple body data sink. Destroying connection. reason " + DataConverter.toString(ioe));
                    }
                    
                    destroy();
                    throw ioe;
                }
            }
        }
        
        return bodyWritten;
    }
    

    /**
     * write body data
     *  
     * @param con                 the http connection
     * @param bodyData            the data to write
     * @param completionHandler   the completion handler
     * @return number of written bytes 
     * @throws IOException if an exception occurs
     */
    protected int writeBody(AbstractHttpConnection con, ByteBuffer[] bodyData, IWriteCompletionHandler completionHandler) throws IOException {
        if (bodyData != null) {
            return (int) con.write(bodyData, completionHandler);
        } else {
            if (completionHandler != null) {
                completionHandler.onWritten(0);
            }
            return 0;
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    void performClose() throws IOException {
        writeData(null, null);
    }

    
    @Override
    void performDestroy() throws IOException {        
    }

}
	    
