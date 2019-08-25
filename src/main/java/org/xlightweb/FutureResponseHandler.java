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
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xsocket.DataConverter;



/**
 * A response handler implementation which supports a future behavior. Typically this 
 * response handler will be used, if the send method is required to send the request,
 * but the response should be read in a blocking behavior. Example:  
 * 
 * <pre>
 *   FutureResponseHandler future = new FutureResponseHandler();
 *    
 *   HttpRequestHeader header = new HttpRequestHeader("POST", url, "application/octet-stream");
 *   
 *   BodyDataSink bodyDataSink = httpClient.send(header, future);
 *   bodyDataSink.transferFrom(source);
 *   bodyDataSink.close();
 *   
 *   IHttpResponse response = future.getResponse(); // blocks until the response header is received
 *   if (response.getStatus() != 200) {
 *      throw new IOException("got status " + response.getStatus());
 *   }
 * </pre>
 *    
 *  
 * @author grro@xlightweb.org
 */
@InvokeOn(InvokeOn.HEADER_RECEIVED)
public class FutureResponseHandler implements IFutureResponse, IHttpResponseHandler, IHttpSocketTimeoutHandler, IUnsynchronized {

	private static final Logger LOG = Logger.getLogger(FutureResponseHandler.class.getName());
	

	private final Object readLock = new Object();
	
	private boolean isDone = false;
	private boolean isCancelled = false;
	private IHttpResponse response;
	private IOException ioException;
	private SocketTimeoutException stException;
	

	/**
	 * {@inheritDoc}
	 */
	public void onResponse(IHttpResponse response) throws IOException {
		synchronized (readLock) {
		    isDone = true;
			this.response = response;
			readLock.notifyAll();
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public void onException(IOException ioe) throws IOException {
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("Exception occured. notify witing reader. " + ioe.toString());
		}
		
		synchronized (readLock) {
			isDone = true;
			this.ioException = ioe;
			readLock.notifyAll();
		}
	}

	
	/**
	 * {@inheritDoc}
	 */
	public void onException(SocketTimeoutException stoe) {
		synchronized (readLock) {
		    isDone = true;
			this.stException = stoe;
			readLock.notifyAll();
		}		
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public IHttpResponse getResponse() throws IOException,  InterruptedException, SocketTimeoutException {	
	    return getResponse(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
	}
	
	

	/**
     * {@inheritDoc}
     */
	public IHttpResponse getResponse(long timeout, TimeUnit unit) throws IOException, SocketTimeoutException {
	    
	    long waittime = unit.toMillis(timeout);
	    long maxTime = System.currentTimeMillis() + waittime; 
	    
	    synchronized (readLock) {

	        do {
	            if (!isDone) {     
	                try {
	                    readLock.wait(waittime);
	                } catch (InterruptedException ie) { 
	                	// Restore the interrupted status
	                    Thread.currentThread().interrupt();
	                }
	            }
	                 
                if (isCancelled) {
                	if (LOG.isLoggable(Level.FINE)) {
                		LOG.fine("request is cancelled. throwing io exception");
                	}
                    throw new IOException("receiving the response is interrupted");
                }
	                
                if (stException != null) {
                	if (LOG.isLoggable(Level.FINE)) {
                		LOG.fine("throwing socket timeout exception " + stException.toString());
                	}
                    throw stException;
                }
	                
                if (ioException != null) {
                	if (LOG.isLoggable(Level.FINE)) {
                		LOG.fine("throwing io exception " + ioException.toString());
                	}
                    throw ioException;
                }
	                
                if (response != null) {
                    return response;
                }
                
                waittime = maxTime - System.currentTimeMillis();
                
            } while (waittime > 0);
	     
	        // timeout reached 
	        isDone = true;
	        throw new SocketTimeoutException("receive timeout " + DataConverter.toFormatedDuration(unit.toMillis(timeout)) +  " reached");
	    }
	}
	

	/**
	 * {@inheritDoc}
	 */
	public IHttpResponse get() throws InterruptedException, ExecutionException {
	    try {
	        return getResponse();
	    } catch (IOException ioe) {
	        throw new ExecutionException(ioe.toString(), ioe);
	    }
	}
	
	
	/**
     * {@inheritDoc}
     */
	public IHttpResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
	    try {
            return getResponse(timeout, unit);
        } catch (SocketTimeoutException stoe) {
            throw new TimeoutException(stoe.toString());
        } catch (IOException ioe) {
            throw new ExecutionException(ioe.toString(), ioe);
        }
	}
	
	

	/**
     * {@inheritDoc}
     */
	public boolean isDone() {
	    return isDone;
	}
	
	
	/**
     * {@inheritDoc}
     */
	public boolean cancel(boolean mayInterruptIfRunning) {
	    
	    synchronized (readLock) {
	        if (!isDone) {
	            isCancelled = true;
	            isDone = true;
	            
	            readLock.notifyAll();
	            return true;
	            
	        } else {
	            return false;
	        }
        }
	}

	
	
    /**
     * {@inheritDoc}
     */
	public boolean isCancelled() {
	    return isCancelled;
	}
}
