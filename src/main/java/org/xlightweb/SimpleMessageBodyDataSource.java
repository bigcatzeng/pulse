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
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * a full body data source
 *  
 * @author grro@xlightweb.org
 */
final class SimpleMessageBodyDataSource extends AbstractNetworkBodyDataSource {
    
    private static final Logger LOG = Logger.getLogger(SimpleMessageBodyDataSource.class.getName());

    
    private TimerTask watchdog = null;
    
	/**
	 * constructor 
	 * 
	 * @param httpConnection   the http connection
	 * @param header           the header
	 * @throws IOException if an exception occurs
	 */
	public SimpleMessageBodyDataSource(HttpMessageHeader header, AbstractHttpConnection httpConnection) throws IOException {
		super(header, httpConnection);
		
		postCreate();
	}


    /**
     * {@inheritDoc}
     */
    @Override
    void doParse(ByteBuffer[] rawData) throws IOException {
        ByteBuffer[] duplicated = new ByteBuffer[rawData.length];
        for (int i = 0; i < rawData.length; i++) {
            duplicated[i] = rawData[i].duplicate();
            rawData[i].position(rawData[i].limit());
        }
            
        onReadNetworkData(duplicated);
    }
    

	@Override
	void onClose() throws IOException {
	    if (watchdog != null) {
	        watchdog.cancel();
	    }
	    throw new ProtocolException("connection has been closed (by user?) while receiving body data", getHeader()); 
	}
	

	 @Override
	 void onDestroy(String reason) {
	     try {
	         performOnDisconnect();
	     } catch (IOException ioe) {
	         if (LOG.isLoggable(Level.FINE)) {
	             LOG.fine("error occured by performing on disconnect " + ioe.toString());
	         }
	     }
	     super.onDestroy(reason);
	 }
	
	@Override
	void performOnDisconnect() throws IOException {
	    if (watchdog != null) {
	        watchdog.cancel();
	    }
	    setNonPersistent();
	    setComplete();
	}
	

	/**
	 * {@inheritDoc}
	 */
	void setDataHandlerSilence(IBodyDataHandler bodyDataHandler) {
	    super.setDataHandlerSilence(bodyDataHandler);
        callBodyDataHandler(true);
    }
}

 