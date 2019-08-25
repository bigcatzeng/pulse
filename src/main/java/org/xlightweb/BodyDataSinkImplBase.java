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

import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xlightweb.AbstractHttpConnection.IMultimodeExecutor;
import org.xlightweb.AbstractListeners.CloseListeners;
import org.xlightweb.AbstractListeners.DestroyListeners;
import org.xlightweb.HttpUtils.CompletionHandlerInfo;
import org.xsocket.DataConverter;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IConnection;
import org.xsocket.connection.IWriteCompletionHandler;
import org.xsocket.connection.NonBlockingConnection;
import org.xsocket.connection.IConnection.FlushMode;





/**
 * Implbase of a BodyDataSink
 * 
 * @author grro@xlightweb.org
 *
 */
abstract class BodyDataSinkImplBase extends BodyDataSink {
	
    private static final Logger LOG = Logger.getLogger(BodyDataSinkImplBase.class.getName());

    private static final String SUPPRESS_SYNC_FLUSH_WARNING_KEY = "org.xlightweb.bodydatasink.suppressSyncFlushWarning";
    private static final boolean IS_SUPPRESS_SYNC_FLUSH_WARNING = Boolean.parseBoolean(System.getProperty(SUPPRESS_SYNC_FLUSH_WARNING_KEY, "false"));
    
    private static final int TRANSFER_CHUNK_SIZE = 65536;
	private static final long DEFAULT_SEND_TIMEOUT_MILLIS = Long.valueOf(System.getProperty(NonBlockingConnection.SEND_TIMEOUT_KEY, Long.toString(NonBlockingConnection.DEFAULT_SEND_TIMEOUT_MILLIS)));
	
	private long sendTimeoutMillis = DEFAULT_SEND_TIMEOUT_MILLIS;

	private final WriteQueue writeQueue = new WriteQueue();
	
	private final AtomicBoolean isOpen = new AtomicBoolean(true);
	private final AtomicBoolean isDestroyed = new AtomicBoolean(false);
	private final AtomicBoolean isIgnoreWriteError = new AtomicBoolean(false);
	
	
    private final CloseListeners closeListeners = new CloseListeners();
    private final DestroyListeners destroyListeners = new DestroyListeners();
	
	
	// flags
	private String encoding = null;
	private boolean isAutoflush = true;
	private FlushMode flushMode = FlushMode.SYNC;
	private boolean isFlushed = false; 
	
	
	// attachment
	private Object attachment = null;
	
	
	// write completion handler support
	private List<WriteCompletionHandlerCaller> writeCompletionHandlerCallers = new ArrayList<WriteCompletionHandlerCaller>();
	
	
	// executor
	private final IMultimodeExecutor executor; 


	// statistics
	int written = 0;
	private final AtomicInteger numIgnoreWriteErrors = new AtomicInteger(0);
	
	
	BodyDataSinkImplBase(IHeader header, IMultimodeExecutor executor) throws IOException {
		super(header);
		
	    this.executor = executor;
		setEncoding(header.getCharacterEncoding());
	}


    /**
     * {@inheritDoc
     */
    final void addCloseListener(IBodyCloseListener listener) {
        synchronized (closeListeners) { 
            closeListeners.addListener(listener, !isOpen(), getExecutor(), HttpUtils.getListenerExecutionMode(listener, "onClose"));
        }
    }

    /**
     * remove a close listener
     * 
     * @param closeListener a close listener
     * @return true, if the listener is removed
     */
    final boolean removeCloseListener(IBodyCloseListener closeListener) {
        synchronized (closeListeners) {
            return closeListeners.removeListener(closeListener);
        }
    }
    

    protected final void callCloseListener() {
        closeListeners.callAndRemoveListeners(getExecutor());
    }
    

    /**
     * add a destroy listener
     * @param listener the destroy listener to add 
     */
    public final void addDestroyListener(IBodyDestroyListener listener) {
        synchronized (destroyListeners) {
            destroyListeners.addListener(listener, isDestroyed.get(), getExecutor(), HttpUtils.getListenerExecutionMode(listener, "onDestroyed"));
        }
    }

    
    /**
     * remove a destroy listener
     * 
     * @param destroyListener a destroy listener
     * @return true, if the listener is removed
     */
    final boolean removeDestroyListener(IBodyDestroyListener destroyListener) {
        synchronized (destroyListeners) {
            return destroyListeners.removeListener(destroyListener);
        }
    }

    
    protected final void callDestroyListener() {
        destroyListeners.callAndRemoveListeners(getExecutor());
    } 

    
	final IMultimodeExecutor getExecutor() {
	    return executor;
	}
	
	
	/**
	 * set the sendtimout 
	 * @param sendTimeoutMillis  the send timeout
	 */
	public final void setSendTimeoutMillis(long sendTimeoutMillis) {
	    this.sendTimeoutMillis = sendTimeoutMillis;
	}
	
	
	final void setIgnoreWriteError() {
	    if (LOG.isLoggable(Level.FINE)) {
	        LOG.fine("[" + getId() + "] setIgnoreWriteError=true");
	    }
	    isIgnoreWriteError.set(true);
	}

	final boolean isIgnoreWriteError() {
	    return isIgnoreWriteError.get();
	}
	
	int getSizeWritten() {
	    return written;
	}

	
	int getNumIgnoreWriteErrors() {
	    return numIgnoreWriteErrors.get();
	}


    

    /**
     * {@inheritDoc}
     */
    public final void flush() throws IOException {
    	isFlushed = true;
        if (!isOpen.get() && !isIgnoreWriteError.get() && !writeQueue.isEmpty()) {
            throw new ClosedChannelException();
        }

        internalFlush();
    }
    
    final boolean isFlushed() {
    	return isFlushed;
    }
    
   
	/**
	 * {@inheritDoc}
	 */
	public final void internalFlush() throws IOException {
        removeWriteMark();
        ByteBuffer[] dataToWrite = writeQueue.drain();


        // ASYNC flush mode
        if (getFlushmode() == FlushMode.ASYNC) {
            if (writeCompletionHandlerCallers.isEmpty()) {
                written += writeData(dataToWrite, null);
                
            } else {
                WriteCompletionHandlerAdapter completionHandlerAdapter = new WriteCompletionHandlerAdapter(writeCompletionHandlerCallers);
                writeCompletionHandlerCallers = new ArrayList<WriteCompletionHandlerCaller>();
                
                written += writeData(dataToWrite, completionHandlerAdapter);
            }
           
            
        // SYNC flush mode            
        } else {
            if (!IS_SUPPRESS_SYNC_FLUSH_WARNING && ConnectionUtils.isDispatcherThread()) {
                String msg = "synchronized flushing in NonThreaded mode could cause dead locks (hint: set flush mode to ASYNC). This message can be suppressed by setting system property " + SUPPRESS_SYNC_FLUSH_WARNING_KEY;
                LOG.warning("[" + getId() + "] " + msg);
            }
            
            if (writeCompletionHandlerCallers.isEmpty()) {
                SyncCaller caller = new SyncCaller(dataToWrite, null);
                caller.call();
                
            } else {
                WriteCompletionHandlerAdapter completionHandlerAdapter = new WriteCompletionHandlerAdapter(writeCompletionHandlerCallers);
                writeCompletionHandlerCallers = new ArrayList<WriteCompletionHandlerCaller>();
                
                SyncCaller caller = new SyncCaller(dataToWrite, completionHandlerAdapter);
                caller.call();
            }
        }
	}
	
	
    
    private final class WriteCompletionHandlerAdapter implements IWriteCompletionHandler, IUnsynchronized {

        private final List<WriteCompletionHandlerCaller> callers;
        
        
        public WriteCompletionHandlerAdapter(List<WriteCompletionHandlerCaller> callers) throws IOException {
            this.callers = callers;
        }

        
        public void onWritten(int written) throws IOException {
            for (WriteCompletionHandlerCaller caller : callers) {
                caller.onWritten();
            }
        }
        
        public void onException(IOException ioe) {
            
            if (isIgnoreWriteError.get()) {
                for (WriteCompletionHandlerCaller caller : callers) {
                    caller.onWritten();
                }
                
            } else {
                for (WriteCompletionHandlerCaller caller : callers) {
                    caller.onException(ioe);
                }
            }
        }
    }

	
   
	
	private final class SyncCaller implements IWriteCompletionHandler, IUnsynchronized {

	    private final Object writeGuard = new Object();
	    private final WriteCompletionHandlerAdapter writeCompletionHandlerAdapter;
	    
	    private IOException ioe = null;
	    private boolean isWritten = false;
	    
	    private ByteBuffer[] dataToWrite = null;
	    
	    
	    public SyncCaller(ByteBuffer[] buffers, WriteCompletionHandlerAdapter writeCompletionHandlerAdapter) throws IOException {
	        this.writeCompletionHandlerAdapter = writeCompletionHandlerAdapter;
	        dataToWrite = buffers;
        }
	    

	    public void call() throws IOException {

	        written += writeData(dataToWrite, this);	
	        
	        synchronized (writeGuard) {

	            // is not written -> wait
                if (!isWritten) {
                    long start = System.currentTimeMillis();
                    long remainingTime = sendTimeoutMillis;
                    do {
                        // wait
                        try {
                            writeGuard.wait(remainingTime);
                        } catch (InterruptedException ie) { 
                        	// Restore the interrupted status
                            Thread.currentThread().interrupt();
                        }
                        
                        if (ioe != null) {
                            throw ioe;
                            
                        } else if (isWritten) {
                            return;
                        }
                
                        remainingTime = (start + sendTimeoutMillis) - System.currentTimeMillis();
                    } while (remainingTime > 0);

                    String msg = "[" + getId() + "] send timeout " + DataConverter.toFormatedDuration(sendTimeoutMillis) + " reached)";
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine(msg);
                    }
                    throw new SocketTimeoutException(msg);
                }
	        } 	        
	    }
	    
	    
	    public void onWritten(int written) throws IOException {
	        if (writeCompletionHandlerAdapter != null) {
	            writeCompletionHandlerAdapter.onWritten(written);
	        }
	        
	        synchronized (writeGuard) {
	            isWritten = true;
	            writeGuard.notifyAll();
            }
	    }
	    
	    public void onException(IOException ioe) {
	        
	        if (writeCompletionHandlerAdapter != null) {
	            writeCompletionHandlerAdapter.onException(ioe);
	        }
	        
            synchronized (writeGuard) {
                this.ioe = ioe;
                isWritten = true;
                writeGuard.notifyAll();
            }	        
	    }
	}



	
	
	
	void doClose() throws IOException {
	    
		if (isOpen.getAndSet(false)) {
		    try {
		        if (!writeQueue.isEmpty()) {
		            internalFlush();
		        }
    		    onClose();
    		} catch (IOException ioe) {
    		    if (!isIgnoreWriteError.get()) {
    		        throw ioe;
    		    }
    			
    		} catch (Exception e) {
    		    if (LOG.isLoggable(Level.FINE)) {
    		        LOG.fine("[" + getId() + "] error occured by flushing BodyDataSink " + e.toString());
    		    }
    			throw new IOException(e.toString());
    				
    		} finally {
    			callCloseListener();
    		}
		}
	}
	
	
	
	/**
     * closes this connection by swallowing io exceptions
     */
    public final void closeQuitly() {
        try {
            close();
        } catch (IOException ioe) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] Error occured by closing connection " + ioe.toString());
            }
        }
    }
	
    abstract void onClose() throws IOException;

    
    private void ensureStreamIsOpenAndWritable() throws ClosedChannelException {
        if (!isOpen.get() && !isIgnoreWriteError.get()) {
            throw new DetailedClosedChannelException("data sink " + getId() +  " is closed");
        }
    }
    

    
    
    final int writeData(ByteBuffer[] dataToWrite, IWriteCompletionHandler completionHandler) throws IOException {

        try {
            return onWriteData(dataToWrite, completionHandler);
            
        } catch (IOException ioe) {
            
            if (isIgnoreWriteError.get()) {
                int size = HttpUtils.computeRemaining(dataToWrite);
                numIgnoreWriteErrors.addAndGet(size);
                
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("DataSink is deactivated (e.g. complete response message is received). writing " + size + " bytes to \"dev0\"");
                }
                
                if (completionHandler != null) {
                    completionHandler.onWritten(size);
                }
                
                return size;
                
            } else {
                throw ioe;
            }
        }
    }
    
    abstract int onWriteData(ByteBuffer[] dataToWrite, IWriteCompletionHandler completionHandler) throws IOException;
    


    
    /**
     * writes a buffer array 
     * 
     * @param buffers                  the buffers to write
     * @param writeCompletionHandler   the completion handler
     * @throws IOException if an exception occurs 
     */
    public final void write(ByteBuffer[] buffers, IWriteCompletionHandler writeCompletionHandler) throws IOException {
    	if (!HttpUtils.isEmpty(buffers)) {
    		ensureStreamIsOpenAndWritable();
    	}

        if (writeCompletionHandler == null) {
            write(buffers);
            return;
            
        } else {
            buffers = preWrite(buffers);
            
            writeCompletionHandlerCallers.add(new WriteCompletionHandlerCaller(writeCompletionHandler, buffers));
            write(buffers);
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public final int write(ByteBuffer buffer) throws IOException, BufferOverflowException {
    	if (!HttpUtils.isEmpty(buffer)) {
    		ensureStreamIsOpenAndWritable();
    	}

        int written = buffer.remaining();
        try {
            ByteBuffer internalBuffer = preWrite(buffer);
            
            written = writeQueue.append(internalBuffer);
            
            if (isAutoflush) {
                flush();
            }
            
            if (buffer != internalBuffer) {
                buffer.position(buffer.position() + written);
            }
            
        } catch (IOException ioe) {
            if (!isIgnoreWriteError.get()) {
                throw ioe;
            }
        }
            
        return written;
    }


    /**
     * {@inheritDoc}
     */
    public final long write(ByteBuffer[] buffers) throws IOException, BufferOverflowException {
    	if (!HttpUtils.isEmpty(buffers)) {
    		ensureStreamIsOpenAndWritable();
    	}

        long written = HttpUtils.computeRemaining(buffers);
        
        try {
            buffers = preWrite(buffers);
            
            written = writeQueue.append(buffers);
            
            if (isAutoflush) {
            	flush();
            }
                    
        } catch (IOException ioe) {
            if (!isIgnoreWriteError.get()) {
                throw ioe;
            }
        }
        
        return written;
    }

    
    
    ByteBuffer[] preWrite(ByteBuffer[] buffers) throws IOException {
        return buffers;
    }
    

    ByteBuffer preWrite(ByteBuffer buffer) throws IOException {
        return buffer;
    }




    /**
     * {@inheritDoc}
     */
    public final long transferFrom(ReadableByteChannel source, int chunkSize) throws IOException, BufferOverflowException {
        long transfered = 0;

        int read = 0;
        do {
            ByteBuffer transferBuffer = ByteBuffer.allocate(chunkSize);
            read = source.read(transferBuffer);

            if (read > 0) {
                if (transferBuffer.remaining() == 0) {
                    transferBuffer.flip();
                    write(transferBuffer);

                } else {
                    transferBuffer.flip();
                    write(transferBuffer.slice());
                }

                transfered += read;
            }
        } while (read > 0);

        return transfered;
    }

    
   
    /**
     * {@inheritDoc}
     */
    public final long transferFrom(FileChannel fileChannel) throws IOException, BufferOverflowException {
        if (getFlushmode() == FlushMode.SYNC) {
            final long size = fileChannel.size();
            long remaining = size;
                
            long offset = 0;
            long length = 0;
                
            do {
                if (remaining > TRANSFER_CHUNK_SIZE) {
                    length = TRANSFER_CHUNK_SIZE;
                } else {
                    length = remaining;
                }
                    
                MappedByteBuffer buffer = fileChannel.map(MapMode.READ_ONLY, offset, length);
                long written = write(buffer);
                    
                offset += written;
                remaining -= written;
            } while (remaining > 0);
                    
            return size;
            
        } else {
            return transferFrom((ReadableByteChannel) fileChannel);
        }
    }
    

    
	
	
	/**
	 * transfer the available data from the data source 
	 * 
	 * @param source   the data source 
	 * @return the transfered size 
	 * 
	 * @throws IOException if an exception occurs
	 */
	public final long transferFrom(NonBlockingBodyDataSource source) throws IOException {
		return source.transferTo(this);
	}
	

	
	/**
	 * transfer the available data from the data source 
	 * 
	 * @param source   the data source 
	 * @param length   the length to transfer
	 * @return the transfered size 
	 * 
	 * @throws IOException if an exception occurs
	 */
	public final long transferFrom(NonBlockingBodyDataSource source, int length) throws IOException {
		return source.transferTo(this, length);
	}

	
	/**
	 * transfer all data from the data source 
	 * 
	 * @param source   the data source 
	 * @return the transfered size 
	 * 
	 * @throws IOException if an exception occurs
	 */
	public final long transferFrom(BodyDataSource source) throws IOException {
		return source.transferTo(this);
	}
	
	
	/**
	 * transfer data from the data source 
	 * 
	 * @param source   the data source 
	 * @param length   the length to transfer 
	 * @return the transfered size 
	 * 
	 * @throws IOException if an exception occurs
	 */
	public final long transferFrom(BodyDataSource source, int length) throws IOException {
		return source.transferTo(this);
	}

   

	/**
	 * sets the default encoding 
	 * 
	 * @param defaultEncoding  the default encoding 
	 */
	public final void setEncoding(String defaultEncoding) {
		this.encoding = defaultEncoding;
	}


	/**
	 * gets the default encoding 
	 * 
	 * @return  the default encoding
	 */
	public final String getEncoding() {
		return encoding;
	}


	/**
	 * see {@link IConnection#setFlushmode(FlushMode)} 
	 */
	public void setFlushmode(FlushMode flushMode) {
	    this.flushMode = flushMode;
	} 


	/**
	 * see {@link IConnection#getFlushmode()}
	 */
	public final FlushMode getFlushmode() {
		return flushMode;
	}

	
	/**
	 * set autoflush. If autoflush is activated, each write call
	 * will cause a flush. <br><br>
	 *
	 * @param autoflush true if autoflush should be activated
	 */
	public final void setAutoflush(boolean autoflush) {
	    this.isAutoflush = autoflush;
	}

	
	/**
	 * get autoflush
	 * 
	 * @return true, if autoflush is activated
	 */
	public final boolean isAutoflush() {
		return isAutoflush;
	}
  
	
	
	/**
	 * Marks the write position in the connection. 
	 */
	public final void markWritePosition() {
	    writeQueue.markWritePosition();
	}


	/**
	 * Resets to the marked write position. If the connection has been marked,
	 * then attempt to reposition it at the mark.
	 *
	 * @return true, if reset was successful
	 */
	public final boolean resetToWriteMark() {
		return writeQueue.resetToWriteMark();
	}


	
	/**
	 * remove the write mark
	 */
	public final void removeWriteMark() {
	    writeQueue.removeWriteMark();
	}

	
	/**
	 * Attaches the given object to this connection
	 *
	 * @param obj The object to be attached; may be null
	 * @return The previously-attached object, if any, otherwise null
	 */
	public final void setAttachment(Object obj) {
	    this.attachment = obj;
	}


	/**
	 * Retrieves the current attachment.
	 *
	 * @return The object currently attached to this key, or null if there is no attachment
	 */
	public final Object getAttachment() {
		return attachment;
	}

	
	
	/**
	 * returns true if the data sink is open
	 * 
	 * @return true if the data sink is open
	 */
	public boolean isOpen() {
		return isOpen.get();
	}
	

	
	/**
	 * call back if the underlying connection is closed
	 */
	void onUnderlyingHttpConnectionClosed() {
		if (isOpen.get()) {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("[" + getId() + "] underlying connection is closed. closing data source");
			}
	        synchronized (closeListeners) {
	            isOpen.set(false);
	        }
		
			callCloseListener();
		}
	}
	
	
	
	   
    public String getId() {
        return this.getClass().getSimpleName() + "#" + hashCode(); 
    }
    
    /**
     * destroys this data sink  
     */
    public void destroy() {
        destroy("user initiated");
    }

    /**
     * destroys this data sink  
     */
    final void destroy(boolean isIgnoreError) {
        destroy("user initiated", isIgnoreError);
    }
	
	/**
	 * destroys this data sink  
	 */
	final void destroy(String reason) {
	    destroy(reason, false);
	}

    /**
     * destroys this data sink  
     */
    final void destroy(String reason, boolean isIgnoreError) {
	    
        if (isIgnoreError) {
            setIgnoreWriteError();
        }
		isOpen.set(false);
		if (!isDestroyed.getAndSet(true)) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] destroying data sink");
            }

            synchronized (destroyListeners) {
                onDestroy(reason);
            }
    		
		    callDestroyListener();
		}
	}
	
	
	
	abstract void onDestroy(String reason);
	
	
    
	
	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return writeQueue.toString();
	}		
	
	
    private final class WriteQueue implements Cloneable {

        private final Queue queue = new Queue(); 
        
        // mark support
        private RewriteableBuffer writeMarkBuffer = null;
        private boolean isWriteMarked = false;
        
        
        
        /**
         * returns true, if empty
         *
         * @return true, if empty
         */
        public boolean isEmpty() {
            return queue.isEmpty() && (writeMarkBuffer == null);
        }
        

        /**
         * drain the queue
         *
         * @return the queue content
         */
        public ByteBuffer[] drain() {
            return queue.drain();
        }

            
        
        /**
         * append a byte buffer to this queue.
         *
         * @param data the ByteBuffer to append
         */
        public int append(ByteBuffer data) {
            
            if (data == null) {
                return 0;
            }
            
            int size = data.remaining(); 
                    
            if (isWriteMarked) {
                writeMarkBuffer.append(data);

            } else {
                queue.append(data);
            }
            
            return size;
        }
        
        
        
        
        /**
         * append a list of byte buffer to this queue. By adding a list,
         * the list becomes part of to the buffer, and should not be modified outside the buffer
         * to avoid side effects
         *
         * @param bufs  the list of ByteBuffer
         */
        public long append(ByteBuffer[] bufs) {
            
            if (bufs == null) {
                return 0;
            }
            
            if (bufs.length < 1) {
                return 0;
            }
            
            int size = 0;

            if (isWriteMarked) {
                for (ByteBuffer buffer : bufs) {
                    size += buffer.remaining();
                    writeMarkBuffer.append(buffer); 
                }
                
            } else {
                for (ByteBuffer buffer : bufs) {
                    size += buffer.remaining();
                }
                
                queue.append(bufs);
            }
            
            return size;
        }
        
        


        
        /**
         * mark the current write position  
         */
        public void markWritePosition() {
            removeWriteMark();

            isWriteMarked = true;
            writeMarkBuffer = new RewriteableBuffer();
        }



        /**
         * remove write mark 
         */
        public void removeWriteMark() {
            if (isWriteMarked) {
                isWriteMarked = false;
                
                append(writeMarkBuffer.drain());
                writeMarkBuffer = null;
            }
        }
        
        
        /**
         * reset the write position the the saved mark
         * 
         * @return true, if the write position has been marked  
         */
        public boolean resetToWriteMark() {
            if (isWriteMarked) {
                writeMarkBuffer.resetWritePosition();
                return true;

            } else {
                return false;
            }
        }

        
        
        @Override
        protected Object clone() throws CloneNotSupportedException {
            WriteQueue copy = new WriteQueue();
            copy.queue.append(this.queue.copyContent());
        
            if (this.writeMarkBuffer != null) {
                copy.writeMarkBuffer = (RewriteableBuffer) this.writeMarkBuffer.clone();
            }
            
            return copy;
        }

                 
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return queue.toString();
        }
    }
	
	
	private static final class Queue {

        private ByteBuffer[] buffers;


        /**
         * returns true, if empty
         *
         * @return true, if empty
         */
        public synchronized boolean isEmpty() {
            return empty();
        }
        

        private boolean empty() {
            return (buffers == null);
        }
                 
        public synchronized void append(ByteBuffer data) {          
            if (buffers == null) {
                buffers = new ByteBuffer[1];
                buffers[0] = data;
                                    
            } else {
                ByteBuffer[] newBuffers = new ByteBuffer[buffers.length + 1];
                System.arraycopy(buffers, 0, newBuffers, 0, buffers.length);
                newBuffers[buffers.length] = data;
                buffers = newBuffers;
            }
        }
        
        
        /**
         * append a list of byte buffer to this queue. By adding a list,
         * the list becomes part of to the buffer, and should not be modified outside the buffer
         * to avoid side effects
         *
         * @param bufs  the list of ByteBuffer
         */
        public synchronized void append(ByteBuffer[] bufs) {
            if (buffers == null) {
                buffers = bufs;
                                      
            }  else {
                ByteBuffer[] newBuffers = new ByteBuffer[buffers.length + bufs.length];
                System.arraycopy(buffers, 0, newBuffers, 0, buffers.length);
                System.arraycopy(bufs, 0, newBuffers, buffers.length, bufs.length);
                buffers = newBuffers;
            }
        }
        


        /**
         * drain the queue
         *
         * @return the queue content
         */
        public synchronized ByteBuffer[] drain() {
            ByteBuffer[] result = buffers;
            buffers = null;
                        
            return result;
        }
        
        
        
        public synchronized ByteBuffer[] copyContent()  {
            return ConnectionUtils.copy(buffers);
        }
        
        
        
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return asString("US-ASCII");
        }
        
        
        /**
         * {@inheritDoc}
         */
        public synchronized String asString(String encoding) {
            StringBuilder sb = new StringBuilder();
            if (buffers != null) {
                ByteBuffer[] copy = new ByteBuffer[buffers.length];
                try {
                    for (int i = 0; i < copy.length; i++) {
                        if (buffers[i] != null) {
                            copy[i] = buffers[i].duplicate();
                        }
                    }
                    sb.append(DataConverter.toString(copy, encoding, Integer.MAX_VALUE));
                    
                } catch (UnsupportedEncodingException use) { 
                    sb.append(DataConverter.toHexString(copy, Integer.MAX_VALUE));
                }
            }

            return sb.toString();
        }
	}
	
	
    private static final class RewriteableBuffer implements Cloneable {
        private ArrayList<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
        private int writePosition = 0;
        
        
        public void append(ByteBuffer buffer) {
            
            if (buffer.remaining() < 1) {
                return;
            }
            
            if (writePosition == bufs.size()) {
                bufs.add(buffer);
                writePosition++;
                
            } else {
                ByteBuffer currentBuffer = bufs.remove(writePosition);
                
                if (currentBuffer.remaining() == buffer.remaining()) {
                    bufs.add(writePosition, buffer);
                    writePosition++;
                    
                } else if (currentBuffer.remaining() > buffer.remaining()) {
                    currentBuffer.position(currentBuffer.position() + buffer.remaining());
                    bufs.add(writePosition, currentBuffer);
                    bufs.add(writePosition, buffer);
                    writePosition++;
                    
                } else { // currentBuffer.remaining() < buffer.remaining()
                    bufs.add(writePosition, buffer);
                    writePosition++;
                    
                    int bytesToRemove = buffer.remaining() - currentBuffer.remaining();
                    while (bytesToRemove > 0) {
                        // does tailing buffers exits?
                        if (writePosition < bufs.size()) {
                            
                            ByteBuffer buf = bufs.remove(writePosition);
                            if (buf.remaining() > bytesToRemove) {
                                buf.position(buf.position() + bytesToRemove);
                                bufs.add(writePosition, buf);
                            } else {
                                bytesToRemove -= buf.remaining();
                            }
                            
                        // ...no
                        } else {
                            bytesToRemove = 0;
                        }
                    }
                }
                
            }   
        }
    
        public void resetWritePosition() {
            writePosition = 0;
        }
        
    
        public ByteBuffer[] drain() {
            ByteBuffer[] result = bufs.toArray(new ByteBuffer[bufs.size()]);
            bufs.clear();
            writePosition = 0;
            
            return result;
        }
        
        
        @Override
        protected Object clone() throws CloneNotSupportedException {
            RewriteableBuffer copy = (RewriteableBuffer) super.clone();
            
            copy.bufs = new ArrayList<ByteBuffer>();
            for (ByteBuffer buffer : this.bufs) {
                copy.bufs.add(buffer.duplicate());
            }

            return copy;
        }
    }   	
    
    
    
    private final class WriteCompletionHandlerCaller implements IUnsynchronized {
        
        private final IWriteCompletionHandler writeCompletionHandler;
        private final CompletionHandlerInfo writeCompletionHandlerInfo;
        private final int size;
        
        
        public WriteCompletionHandlerCaller(IWriteCompletionHandler writeCompletionHandler, ByteBuffer[] buffers) {
            this.writeCompletionHandler = writeCompletionHandler;
            writeCompletionHandlerInfo = HttpUtils.getCompletionHandlerInfo(writeCompletionHandler);
            
            size = HttpUtils.computeRemaining(buffers);
        }
        
        
        void onWritten(){
            
            if (writeCompletionHandlerInfo.isUnsynchronized()) {
                performCompletionHandler();
                
            } else {
                Runnable task = new Runnable() {
                    public void run() {
                        performCompletionHandler();
                    }
                };
                
                if (writeCompletionHandlerInfo.isOnWrittenMultithreaded()) {
                    executor.processMultithreaded(task);
                } else {
                    executor.processNonthreaded(task);
                }
            }
        }
        
        
        private void performCompletionHandler() {
            try {
                writeCompletionHandler.onWritten(size);
            } catch (IOException ioe) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + getId() + "] error occured by perforing onWritten of " + writeCompletionHandler + " " + ioe.toString());
                }
                destroy();
            }
        }
        
        
        
        void onException(final IOException ioe) {
            
            if (writeCompletionHandlerInfo.isUnsynchronized()) {
                writeCompletionHandler.onException(ioe);
                
            } else {
                Runnable task = new Runnable() {
                    public void run() {
                        writeCompletionHandler.onException(ioe);
                    }
                };
                
                if (writeCompletionHandlerInfo.isOnExceptionMutlithreaded()) {
                    executor.processMultithreaded(task);
                } else {
                    executor.processNonthreaded(task);
                }
            }
        }
    }
}
	    
