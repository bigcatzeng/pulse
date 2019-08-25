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
package org.xlightweb.client;

import java.io.Closeable;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xlightweb.BodyDataSink;
import org.xlightweb.IBodyDataHandler;
import org.xlightweb.IBodyDestroyListener;
import org.xlightweb.NonBlockingBodyDataSource;




/**
 * A Forwarder which duplicates the data and forwards it to a primary and a secondary sink 
 * 
 * @author grro@xlightweb.org
 */
final class DuplicatingBodyForwarder implements IBodyDataHandler {
    
    private static final Logger LOG = Logger.getLogger(DuplicatingBodyForwarder.class.getName());
    
    private static final ByteBuffer NULL_BYTE_BUFFER = ByteBuffer.allocate(0);
	
	private final NonBlockingBodyDataSource bodyDataSource;
	private final ISink primarySink;
	private final AtomicBoolean isPrimarySinkClosed = new AtomicBoolean(false); 
	private final ISink secondarySink;
	private final AtomicBoolean isSecondarySinkClosed = new AtomicBoolean(false);
	
	
    /**
     * DuplicatingBodyForwarder is unsynchronized by config. See HttpUtils$getExecutionMode
     */

	

	public DuplicatingBodyForwarder(final NonBlockingBodyDataSource bodyDataSource, final ISink primarySink, final ISink secondarySink) {
		this.bodyDataSource = bodyDataSource;
		this.primarySink = primarySink;
		this.secondarySink = secondarySink;
		
		
		IBodyDestroyListener destroyListenerPrimary = new IBodyDestroyListener() {
		    public void onDestroyed() {
		        isPrimarySinkClosed.set(true); 
		        handlePeerDestroy();
		    }
		};
		
		primarySink.setDestroyListener(destroyListenerPrimary);
		
		
		IBodyDestroyListener destroyListenerSecondary = new IBodyDestroyListener() {
		    public void onDestroyed() {
		        isSecondarySinkClosed.set(true);
		        handlePeerDestroy();
		    }
		};

		secondarySink.setDestroyListener(destroyListenerSecondary);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public boolean onData(final NonBlockingBodyDataSource bodyDataSource) throws BufferUnderflowException {
	    
	    try {

	        int available = 0;
	        do {
    	        try {
    	            available = HttpClientConnection.availableSilence(bodyDataSource);
    	        } catch (IOException e) {
    	            destroySinks();
    	            return true;
    	        }

                if (available == -1) {
                    closeSinks();
                    return true;
                    
                } else if (available == 0) {
                	if (isPrimarySinkClosed.get()) {
                        if (!isSecondarySinkClosed.get()) {
                            write(secondarySink, isSecondarySinkClosed, NULL_BYTE_BUFFER);
                        } else {
                            throw new ClosedChannelException();
                        }
                    } else {
                        if (!isSecondarySinkClosed.get()) {
                            write(secondarySink, isSecondarySinkClosed, NULL_BYTE_BUFFER);
                        }
                        write(primarySink, isPrimarySinkClosed, NULL_BYTE_BUFFER);
                    }                	
                	
                } else if (available > 0) {
                    ByteBuffer[] buffers = HttpClientConnection.readByteBufferByLengthSilence(bodyDataSource, available);
                    
                    for (ByteBuffer buffer : buffers) {
                        
                        if (isPrimarySinkClosed.get()) {
                            if (!isSecondarySinkClosed.get()) {
                                write(secondarySink, isSecondarySinkClosed, buffer);
                            } else {
                                throw new ClosedChannelException();
                            }
                        } else {
                            if (!isSecondarySinkClosed.get()) {
                                write(secondarySink, isSecondarySinkClosed, buffer.duplicate());
                            }
                            write(primarySink, isPrimarySinkClosed, buffer);
                        }
                    }
                }

	        } while (available > 0);
	        
        } catch (IOException e) {
            destroySinks();
        } 
        
		return true;
	}
	
	
	private void write(ISink sink, AtomicBoolean isClosed, ByteBuffer buffer) {
	    try {
	        sink.onData(buffer);	
	        
	    } catch (IOException ioe) {
	        if (LOG.isLoggable(Level.FINE)) {
	            LOG.fine("[" + sink.getId() + "] error occured by writing data to " + sink + " " + ioe.toString());
	        }
	        isClosed.set(true);
	        handlePeerDestroy();
	    }
	}
	
	
	private void handlePeerDestroy() {
	    if (isPrimarySinkClosed.get() && isSecondarySinkClosed.get()) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("both data sink are closed. Destroying data source");
            }	        
            bodyDataSource.destroy();
        }
	}
	
	
	
	
	
	private void destroySinks() {	   
	    if (!isPrimarySinkClosed.getAndSet(true)) {
	        primarySink.destroy();
	    }
	        
	    if (!isSecondarySinkClosed.getAndSet(true)) {
	        secondarySink.destroy();
	    }
	}
	    
	

    private void closeSinks() throws IOException {
        if (!isPrimarySinkClosed.getAndSet(true)) {
            primarySink.close();
        }
            
        if (!isSecondarySinkClosed.getAndSet(true)) {
            secondarySink.close();
        }
    }
	
    
   
	public static interface ISink extends Closeable {
	        
	    void onData(ByteBuffer data) throws IOException;
	    
	    void setDestroyListener(IBodyDestroyListener destroyListener);
	        
	    void destroy();
	    
	    String getId();
	}
	
	
	static final class BodyDataSinkAdapter implements ISink {
        
        private final BodyDataSink dataSink; 
            
        public BodyDataSinkAdapter(BodyDataSink dataSink) throws IOException {
            this.dataSink = dataSink;
        }
            
        public void onData(ByteBuffer data) throws IOException {
            dataSink.write(data);
        }
            
        public void close() throws IOException {
            dataSink.close();
        }
            
        public void destroy() {
            dataSink.destroy();
        }
            
        public void setDestroyListener(IBodyDestroyListener destroyListener) {
            dataSink.addDestroyListener(destroyListener);
        }
        
        public String getId() {
            return "wrapped" + dataSink.getId();
        }
    }
	
	
    static class InMemorySink implements ISink {
        
        /*
         * TODO: improve synchronization (make it less expensive) 
         */
        
        private final List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
        private final int maxBufferSize;
        private int bufferSize = 0;
         

        private IBodyDestroyListener destroyListener = null;
        private boolean isDestroyed = false;
        private boolean isClosed = false;
        
        private ISink forwardSink = null;
        
        
        public InMemorySink() {
            this(Integer.MAX_VALUE);
        }

        
        public InMemorySink(int maxBufferSize) {
            this.maxBufferSize = maxBufferSize;
        }
        
        public synchronized void onData(ByteBuffer data) throws IOException {
            if (forwardSink == null) {
                bufferSize += data.remaining();                
                buffers.add(data);
                
                if (bufferSize > maxBufferSize) {
                    onMaxBufferSizeExceeded();
                }
            } else {
                forwardSink.onData(data);
            }
        }
        
        void onMaxBufferSizeExceeded() {
            destroy();
        }
        
        
        public synchronized void close() throws IOException {
            if (forwardSink == null) {
                isClosed = true;
            } else {
                forwardSink.close();
            }
        }
        
        public synchronized boolean isDestroyed() {
            return isDestroyed;
        }
        
        public synchronized void destroy() {
            if (forwardSink == null) {
                isDestroyed = true;
                buffers.clear();
                callDestroyListener();
            } else {
                forwardSink.destroy();
            }
        }
        
        private void callDestroyListener() {
            IBodyDestroyListener ls = destroyListener;
            if (ls != null) {
                try {
                    ls.onDestroyed();
                } catch (IOException ioe) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Error occured by calling destroy listener");
                    }
                }
            }
        }
        
        public String getId() {
            return "<unset>";
        }
        
        public synchronized int getSize() {
            int size = 0;
            
            for (ByteBuffer buffer : buffers) {
                size += buffer.remaining();
            }
            
            return size;
        }
                
        public synchronized void setDestroyListener(IBodyDestroyListener destroyListener) {
            if (forwardSink == null) {
                this.destroyListener = destroyListener;
            } else {
                forwardSink.setDestroyListener(destroyListener);
            }
        }
        
        
        public synchronized boolean forwardTo(ISink sink) throws IOException {
            if (isDestroyed) {
                return false;
            }
            
            forwardSink = sink;
            
            if (destroyListener != null) {
                sink.setDestroyListener(destroyListener);
            }
            
                
            for (ByteBuffer buffer : buffers) {
                onData(buffer);
            }
                
            if (isClosed) {
                close();
            }
            
            return true;
        }
    }	
}
