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
final class BodylessBodyDataSink extends AbstractNetworkBodyDataSink {

    private static final Logger LOG = Logger.getLogger(BodylessBodyDataSink.class.getName());

        private boolean isHeaderWritten = false;

    
    public BodylessBodyDataSink(AbstractHttpConnection httpConnection, IHttpMessageHeader header) throws IOException {
        super(header, httpConnection);
        
    }
    
    
    
    @Override
    int onWriteNetworkData(ByteBuffer[] dataToWrite, IWriteCompletionHandler completionHandler) throws IOException {
        if (completionHandler != null) {
            completionHandler.onWritten(0);
        }
        
        return 0;
    }

    
    /**
     * {@inheritDoc}
     */
    void performClose() throws IOException {
        AbstractHttpConnection con = getConnection();
        
        if (con != null) {
            
            try {
                if (!isHeaderWritten) {
                    isHeaderWritten = true;
                    
                    con.write(getHeader().toString() + "\r\n");
                    con.flush();

                    con.incCountMessageSent();
                }
                
            } catch (IOException ioe) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + getId()+ "] error occured by flushing bodyless message writer. Destroying connection. reason " + DataConverter.toString(ioe));
                }
                
                destroy();
                throw ioe;
            }
        }
    }
    
    
    @Override
    void performDestroy() throws IOException {
        
    }
}
	    
