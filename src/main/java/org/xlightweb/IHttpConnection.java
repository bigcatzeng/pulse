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
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.Executor;

import org.xsocket.connection.IConnection;
import org.xsocket.connection.INonBlockingConnection;



/**
 * A http connection (session) between two http client and http server.
 *
 *
 * @author grro@xlightweb.org
 */
public interface IHttpConnection extends IConnection {
	

    public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 60 * 1000;
	public static final long DEFAULT_RESPONSE_TIMEOUT_MILLIS = Long.MAX_VALUE;
	public static final long DEFAULT_DATA_RESPONSE_TIMEOUT_MILLIS = Long.MAX_VALUE;
	

	/**
	 * returns if the connection is persistent 
	 * 
	 * @return true, if connection is persistent
	 */
	boolean isPersistent();
	
	
	/**
	 * ad hoc activation of a secured mode (SSL). By performing of this
	 * method all remaining data to send will be flushed.
	 * After this all data will be sent and received in the secured mode
	 *
	 * @throws IOException If some other I/O error occurs
	 */
	void activateSecuredMode() throws IOException;
	

	/**
	 * returns if the connection is in secured mode
	 * @return true, if the connection is in secured mode
	 */
	boolean isSecure();
	


	/**
	 * set the send delay time. Data to write will be buffered
	 * internally and be written to the underlying subsystem
	 * based on the given write rate.
	 * The write methods will <b>not</b> block for this time. <br>
	 *
	 * By default the write transfer rate is set with UNLIMITED <br><br>
	 *
	 * Reduced write transfer is only supported for FlushMode.ASYNC. see
	 * {@link INonBlockingConnection#setFlushmode(FlushMode))}
	 *
	 * @param bytesPerSecond the transfer rate of the outgoing data
	 * @throws ClosedChannelException If the underlying socket is already closed
	 * @throws IOException If some other I/O error occurs
	 */
	void setWriteTransferRate(int bytesPerSecond) throws ClosedChannelException, IOException;
	
	
	/**
	 * return the worker pool which is used to process the call back methods
	 *
	 * @return the worker pool
	 */
	Executor getWorkerpool();
	
	
	/**
	 * suspend receiving data from the underlying subsystem
	 *
	 * @throws IOException If some other I/O error occurs
	 */
	void suspendReceiving() throws IOException;
	

	/**
	 * returns true if receiving is suspended
	 * 
	 * @return true, if receiving is suspended
	 */
	boolean isReceivingSuspended();
	
	
	/**
	 * resume receiving data from the underlying subsystem
	 *
	 * @throws IOException If some other I/O error occurs
	 */
	void resumeReceiving() throws IOException;
	
	
	/**
	 * returns body data receive timeout
	 * 
	 * @return the body data receive timeout
	 */
	long getBodyDataReceiveTimeoutMillis();
	
	
	/**
	 * set the body data receive timeout
	 * 
	 * @param bodyDataReceiveTimeoutMillis the timeout
	 */
	void setBodyDataReceiveTimeoutMillis(long bodyDataReceiveTimeoutMillis);
	

	
	/**
	 * adds a connection handler
	 * 
	 * @param connectionHandler  the connection handler
	 */
	void addConnectionHandler(IHttpConnectionHandler connectionHandler);

	
	/**
	 * removes a connectrion handler 
	 * @param connectionHandler  the connection handler
	 */
	void removeConnectionHandler(IHttpConnectionHandler connectionHandler);
	
	
	/**
	 * returns the underlying tcp connection
	 * 
	 * @return the underlying tcp connection
	 */
	INonBlockingConnection getUnderlyingTcpConnection();


    
    /**
     * gets the autoconfirmExpect100ContinueHeader
     * @param  true if the 100-continue header should be autoconfirmed 
     */


	/**
     * closes this connection by swallowing io exceptions
     */
    void closeQuitly();
    
    
    /**
     * closes the connection in a unlean way  
     */
    void destroy();
}
