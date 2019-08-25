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


import java.io.Closeable;






import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xlightweb.AbstractHttpConnection.IMultimodeExecutor;
import org.xlightweb.AbstractListeners.CloseListeners;
import org.xlightweb.AbstractListeners.DestroyListeners;
import org.xlightweb.HttpUtils.CompletionHandlerInfo;
import org.xsocket.DataConverter;
import org.xsocket.Execution;
import org.xsocket.IDataSource;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.AbstractNonBlockingStream;
import org.xsocket.connection.IWriteCompletionHandler;
import org.xsocket.connection.IConnection.FlushMode;



/**
 * data source base implementation
 *  
 * @author grro@xlightweb.org
 */
public abstract class NonBlockingBodyDataSource implements IDataSource, ReadableByteChannel, Closeable {
	
    private static final Logger LOG = Logger.getLogger(NonBlockingBodyDataSource.class.getName());
    
    private final AtomicBoolean isDestroyed = new AtomicBoolean(false);
    
    private final AtomicBoolean isComplete = new AtomicBoolean(false);
	private final AtomicReference<IOException> exceptionRef = new AtomicReference<IOException>();
	
	private final NonBlockingStream nonBlockingStream = new NonBlockingStream();
    
	private final AtomicReference<BodyDataHandlerAdapter> bodyDataHandlerAdapterRef = new AtomicReference<BodyDataHandlerAdapter>(new BodyDataHandlerAdapter(null));
	
	private final CompleteListeners completeListeners = new CompleteListeners();
    private final CloseListeners closeListeners = new CloseListeners();
    private final DestroyListeners destroyListeners = new DestroyListeners();
	
	private final IMultimodeExecutor executor;
	
	
	   // life cycle management
    private static final long MIN_WATCHDOG_PERIOD_MILLIS = 10 * 1000;

    private long bodyDataReceiveTimeoutMillis = IHttpConnection.DEFAULT_DATA_RESPONSE_TIMEOUT_MILLIS;
    private long creationTimeMillis = 0;
    private long lastTimeDataReceivedMillis = System.currentTimeMillis();
    private TimeoutWatchDogTask watchDogTask;

    
    private final IHeader header;
    
    private boolean isIgnoreAppendError = false;
    private boolean isDataRead = false;
    
    private final AtomicBoolean isDataAppended = new AtomicBoolean(false); 
    
    // body access listener
    private IBodyAccessListener bodyAccessListener = null;
    
    
    // part support
    private Boolean isMultipart = null;

 
    // statistics
    private int dataReceived = 0;
    
	
	NonBlockingBodyDataSource(IHeader header, IMultimodeExecutor executor) {
	    nonBlockingStream.setEncoding(header.getCharacterEncoding());
	    this.executor = executor;
	    this.header = header;
    }

	NonBlockingBodyDataSource(IHeader header, IMultimodeExecutor executor, ByteBuffer[] data) throws IOException {
        nonBlockingStream.setEncoding(header.getCharacterEncoding());
        this.executor = executor;
        this.header = header;
        
        if (data != null) {
            append(data);
        }
        
        isComplete.set(true);
    }
	
	
	final IHeader getHeader() {
		return header;
	}
	
	
    
    /**
     * returns the id 
     * 
     * @return the id
     */
    abstract String getId();

    
    
    boolean isForwardable() {
        return false;
    }
    
    boolean isSimpleMessageBody() {
    	return false;
    }

    
    final void setBodyAccessListener(IBodyAccessListener bodyAccessListener) {
        this.bodyAccessListener = bodyAccessListener;
    }
    
    private void callBodyAccessListener() {
        if (bodyAccessListener != null) {
            boolean remove = bodyAccessListener.onBodyAccess();
            if (remove) {
                bodyAccessListener = null;
            }
        }
    }

    /**
     * add a close listener 
     * @param listener the close listener to add
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
    
    
    /**
     * add a destroy listener
     * @param destroyListener the destroy listener to add 
     */
    public final void addDestroyListener(IBodyDestroyListener destroyListener) {
        callBodyAccessListener();
        addDestroyListenerSilence(destroyListener);
    }
    

    final void addDestroyListenerSilence(IBodyDestroyListener listener) {
        synchronized (destroyListeners) {
            destroyListeners.addListener(listener, isDestroyed(), getExecutor(), HttpUtils.getListenerExecutionMode(listener, "onDestroyed"));
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

    
	/**
     * appends data 
     *
     * @param isContentImmutable if the buffer is immutable 
     * @param data               the data to append 
     */
    int append(ByteBuffer data) throws IOException {
        isDataAppended.set(true);
        
        int added = nonBlockingStream.append(data);
        dataReceived += added;
        
        return added;
    }

    
    /**
     * appends data 
     *
     * @param isContentImmutable if the buffer is immutable 
     * @param data               the data to append 
     */
    int append(ByteBuffer[] data) throws IOException {
        isDataAppended.set(true);
        
        int added = nonBlockingStream.append(data);
        dataReceived += added;
        
        return added;
    }
    
    
    /**
     * appends data 
     *
     * @param isContentImmutable if the buffer is immutable 
     * @param data               the data to append 
     */
    int append(ByteBuffer[] data, IWriteCompletionHandler completionHandler) throws IOException {
        isDataAppended.set(true);
        int added = 0;
        
        if (completionHandler != null) {
            added = nonBlockingStream.append(data, completionHandler, false);
        } else {
            added = nonBlockingStream.append(data);
        }
            
        dataReceived += added;
        return added;
    }
    
     
     
    final boolean isMoreInputDataExpected() {
        
        // no, if data source is complete
        if (isComplete.get()) {
            return false;
        }
       
        // no, if data source is destroyed
        if (isDestroyed.get()) {
            return false;
        }
        
        // ... yes
        return true;
    }
    
    
    void setComplete() throws IOException {
        
        synchronized (completeListeners) {
            isComplete.set(true);
            nonBlockingStream.setComplete();        
        }
        completeListeners.callAndRemoveListeners(getExecutor());

        terminateWatchDog();
    }
    
	
	/**
     * return true, if all body data has been received
     *  
     * @return true, if all body data has been received
     * @throws IOException if an error exists
     */
    final boolean isCompleteReceived() throws IOException {
        if (isComplete.get()) {
            return true;            
        } else {
            throwExceptionIfExist();
            return false;
        }
    }

    /**
     * return true if the body is a mulipart 
     * @return true, if the body is a mulipart
     */
    public final boolean isMultipart() {
        callBodyAccessListener();

    	if (isMultipart == null) {
    		isMultipart = (getHeader().getContentType() != null) && getHeader().getContentType().startsWith("multipart/");
    	} 
    	return isMultipart; 
    }
    

    final boolean isComplete() {
        return isComplete.get();
    }

    final int getSizeDataReceived() {
        return dataReceived;
    }
    
    final boolean isDestroyed() {
        return isDestroyed.get();
    }
    
    final void setEncoding(String encoding) {
        nonBlockingStream.setEncoding(encoding);
    }
  
    
    final void setException(IOException ioe) {
        
        IOException oldException = exceptionRef.get(); 
        if (oldException != null) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] warning a exception alreday exits. ignore exception (old: " + oldException + ", new: " + ioe);    
            }
            return;
        }
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("[" + getId() + "] set exception " + ioe);    
        }
        
        exceptionRef.set(ioe);         
        callBodyDataHandler(true);
        destroy(ioe.toString()); 
    }

    

    /**
     * destroys the data source
     */
    public void destroy() {
        destroy("user initiated");
    }
    
    /**
     * destroys the data source
     */
    void destroy(String reason) {
        destroy(reason, false);
    }
    

    /**
     * destroys the data source
     */
    void destroy(String reason, boolean isIgnoreAppendError) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("[" + getId() + "] initiate destroying sink " + reason);
        }
        
        this.isIgnoreAppendError = isIgnoreAppendError;
        
        terminateWatchDog();
        getExecutor().processNonthreaded(new DestroyTask(reason));
    }
    
    final boolean isIgnoreAppendError() {
        return isIgnoreAppendError;
    }

    
    private final class DestroyTask implements Runnable {
        
        private final String reason;
        
        public DestroyTask(String reason) {
            this.reason = reason;
        }
        
        public void run() {
            performDestroy(reason);
        }
    }
    
    private void performDestroy(String reason) {
        if (!isDestroyed.getAndSet(true)) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] destroying data source");
            }
            
            synchronized (destroyListeners) {
                onDestroy(reason);
            }
            
            destroyListeners.callAndRemoveListeners(getExecutor());
            nonBlockingStream.destroy();
        }
    }
    
    
    abstract void onDestroy(String reason);
    


      
    /**
     * adds a complete listener 
     * 
     * @param listener  the complete listener
     */
    public void addCompleteListener(IBodyCompleteListener listener) {
        callBodyAccessListener();
        
        synchronized (completeListeners) {
            completeListeners.addListener(listener, isComplete.get(), getExecutor(), HttpUtils.getListenerExecutionMode(listener, "onComplete"));
        }
    }
    

    
    /**
     * set the body handler
     * 
     * @param bodyDataHandler  the body handler
     */
    public final void setDataHandler(IBodyDataHandler bodyDataHandler) {
        callBodyAccessListener();
        setDataHandlerSilence(bodyDataHandler);
    }
    
    

    void setDataHandlerSilence(IBodyDataHandler bodyDataHandler) {
        BodyDataHandlerAdapter bodyDataHandlerAdapter = bodyDataHandlerAdapterRef.get().newBodyDataHandlerAdapter(bodyDataHandler);
        setBodyHandler(bodyDataHandlerAdapter);
        callBodyDataHandler(isDataAppended.get());
    }
    
    /**
     * read the part of the multipart body. {@link BlockingBodyDataSource#isMultipart()} can
     *  be used to verify if the body is a multipart one
     * 
     * @return  the part
     * @throws NoMultipartTypeException if the body is not a multipart body 
     * @throws IOException if an exception occurs
     * @throws BufferUnderflowException if not enough data is available
     */
	public IPart readPart() throws NoMultipartTypeException, IOException, BufferUnderflowException {
		if (!isMultipart()) {
			throw new NoMultipartTypeException("body ist not a multipart type " + getHeader().getContentType());
		}
		
		initPartHandler(null);
		
		PartParser partParser = bodyDataHandlerAdapterRef.get().partParserRef.get();
		if (partParser != null) {
		    return partParser.readPart();
		} else {
		    throw new BufferUnderflowException();
		}
	}
	
	
	private synchronized void initPartHandler(final IPartHandler partHandler) throws IOException {
		
		if (bodyDataHandlerAdapterRef.get().getPartParser() == null) {
    		if (isMultipart()) { 
    			if (LOG.isLoggable(Level.FINE)) {
    				LOG.fine("part handler set. parsing body");
    			}
	    		final String boundary = HttpUtils.parseMediaTypeParameter(getHeader().getContentType(), "boundary", true, null);
	    		if (boundary == null) {
	    			throw new IOException("no boundary set " + getHeader().getContentType());
	    			
	    		} else {
	    		    bodyDataHandlerAdapterRef.get().setPartParser(new PartParser(partHandler, NonBlockingBodyDataSource.this, "--" + boundary, null));
	    		}
	    		
	    		
	    		
	    	} else {
	    		throw new NoMultipartTypeException("body is not multipart type " + getHeader().getContentType()); 
	    	}
    	} else {
    		if (partHandler != null) {
    		    bodyDataHandlerAdapterRef.get().getPartParser().setPartHandler(partHandler);
    		}
    	}
	}
	
    /**
     * set the part handler. {@link BlockingBodyDataSource#isMultipart()} can
     *  be used to verify if the body is a multipart one
     * 
     * @param bodyDataHandler  the body handler
     * @throws IOException if an exception occurs
     * @throws NoMultipartTypeException if the body is not a multipart body
     */
    public void setBodyPartHandler(IPartHandler partHandler) throws NoMultipartTypeException, IOException {
        callBodyAccessListener();

    	initPartHandler(new PartHandlerAdapter(partHandler));
    }
  
  
    
    final void setBodyHandler(BodyDataHandlerAdapter handler) {
        bodyDataHandlerAdapterRef.set(handler);
    }
   
    
    final void callBodyDataHandler(boolean force) {
        
        IBodyDataHandler bodyDataHandler = bodyDataHandlerAdapterRef.get();
        
        if (bodyDataHandler != null) {
            if ((getSize() != 0) || force || (exceptionRef.get() != null)) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + getId() + "] calling body data handler " + bodyDataHandler.toString() + "#" + bodyDataHandler.hashCode());
                }
                bodyDataHandler.onData(this);
            } else {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("body data handler " + bodyDataHandler.getClass().getName() + "#" + bodyDataHandler.hashCode() +
                             " will not be called (size == 0)");
                }
            }
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("no body data handler assigned");
            }
        }
    }

    final int getSize() {
        int size = nonBlockingStream.getSize();
        if ((size == 0) && isComplete.get()) {
            return -1;
        }
        
        return size;
    }
    
    
    int getDataReceived() {
        return dataReceived;
    }
    
    
    private int getVersion() throws IOException {
        return nonBlockingStream.getReadBufferVersion();
    }

    

    /**
     * returns the body data handler or <code>null</code> if no data handler is assigned 
     * 
     * @return the body data handler or <code>null</code> if no data handler is assigned
     */
    public IBodyDataHandler getDataHandler() {
        callBodyAccessListener();
        
        return getDataHandlerSilence();
    }
    
    
    IBodyDataHandler getDataHandlerSilence() {
        return bodyDataHandlerAdapterRef.get().getDelegate();
    }

 
    /**
     * set the body data receive timeout
     * 
     * @param bodyDataReceiveTimeoutMillis the timeout
     */
    public final void setBodyDataReceiveTimeoutMillis(long bodyDataReceiveTimeoutMillis) {
        callBodyAccessListener();
        setBodyDataReceiveTimeoutMillisSilence(bodyDataReceiveTimeoutMillis);    
    }
    
    
    final void setBodyDataReceiveTimeoutMillisSilence(long bodyDataReceiveTimeoutMillis) {
        if (bodyDataReceiveTimeoutMillis <= 0) {
            if (!isComplete.get()) {
                setException(new ReceiveTimeoutException(bodyDataReceiveTimeoutMillis));
            }
            return;
        }
        
        creationTimeMillis = System.currentTimeMillis();
        
        if (this.bodyDataReceiveTimeoutMillis != bodyDataReceiveTimeoutMillis) {
            this.bodyDataReceiveTimeoutMillis = bodyDataReceiveTimeoutMillis;
        
            if (bodyDataReceiveTimeoutMillis == Long.MAX_VALUE) {
                terminateWatchDog();

            } else{
                
                long watchdogPeriod = 100;
                if (bodyDataReceiveTimeoutMillis > 1000) {
                    watchdogPeriod = bodyDataReceiveTimeoutMillis / 10;
                }
                
                if (watchdogPeriod > MIN_WATCHDOG_PERIOD_MILLIS) {
                    watchdogPeriod = MIN_WATCHDOG_PERIOD_MILLIS;
                }
                
                updateWatchDog(watchdogPeriod);
            }
        }
    }
    
    
    private synchronized void updateWatchDog(long watchDogPeriod) {
        terminateWatchDog();

        watchDogTask = new TimeoutWatchDogTask(this); 
        AbstractHttpConnection.schedule(watchDogTask, watchDogPeriod, watchDogPeriod);
    }

    
    private synchronized void terminateWatchDog() {
        if (watchDogTask != null) {
            watchDogTask.cancel();
            watchDogTask = null;
        }
    }
    
    
    private void checkTimeouts() {
        
        if (isComplete.get() || isDestroyed.get()) {
            terminateWatchDog();
            return;
        }
        
        long currentTimeMillis = System.currentTimeMillis();
            
        if (currentTimeMillis > (lastTimeDataReceivedMillis + bodyDataReceiveTimeoutMillis) && 
            currentTimeMillis > (creationTimeMillis + bodyDataReceiveTimeoutMillis)) {
            
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] receive timeout reached. set exception");
            }

            if (!isComplete.get()) {
                setException(new ReceiveTimeoutException());
            }
            destroy("receive timeout reached");
        } 
    }    
    
 
    /**
     * returns the available bytes
     * 
     * @return the number of available bytes, possibly zero, or -1 if the channel has reached end-of-stream
     * 
     * @throws ProtocolException if a protocol error occurs 
     * @throws IOException if some other exception occurs 
     */
    public final int available() throws ProtocolException, IOException  {
        callBodyAccessListener();
        
        return availableSilence();
    }
    
	
    final int availableSilence() throws ProtocolException, IOException  {
        
        // if an exception is pending -> throwing it
        IOException ioe = exceptionRef.get();
        if (ioe != null) {
            
            // ClosedChannelException should not occur here. Anyway, handle it because available() should never throw a ClosedChannelException 
            if (!(ioe instanceof ClosedChannelException)) {
                throw ioe;
            }
        }
        
        // retrieve the available data size 
        int available = nonBlockingStream.getSize();
        
        
        // non data available?
        if ((available == 0)) {
        
            // if body is complete return -1 to signal end-of-stream
            if (isComplete.get()) {
            	if (bodyDataHandlerAdapterRef.get().getPartParser() != null) {
            		if (bodyDataHandlerAdapterRef.get().getPartParser().availableParts() > 0) {
            			return 0;
            		}
            	}
            	return -1;
                
            } else {
                
                // is destroyed? 
                if (isDestroyed.get()) {
                    close();
                    throw new ClosedChannelException();
                } else {
                    return 0;
                }
            }
            
        } else {
            return available;
        }
    }

    
   final IOException getException() {
        IOException ioe = exceptionRef.get();
        if (ioe != null) {
            
            // ClosedChannelException should not occur here. Anyway, handle it because available() should never throw a ClosedChannelException 
            if (!(ioe instanceof ClosedChannelException)) {
                return ioe;
            }
        }
        
        return null;
    }

    
    /**
     * returns the current content size
     * 
     * @return the current content size
     */
    int size() {
        int available = nonBlockingStream.getSize();
        if ((available <= 0) && isComplete.get()) {
            return -1;
        } else {
            return available;
        }
    }
    
    
    

    
    /**
     * closes the body data source
     * 
     *  @throws IOException if an exception occurs
     */
    public final void close() throws IOException {
        
        if (isOpen()) {
            
            try {
                synchronized (closeListeners) {
                    nonBlockingStream.close();
                    onClose();
                }
                
                closeListeners.callAndRemoveListeners(getExecutor());    // call close listener only in case of success closing 
            } catch (IOException ioe) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + getId() + "] error occured by closing connection. destroying it " + ioe.toString());
                }  
                setException(ioe);
            }
        }
    }
    

    abstract void onClose() throws IOException;
    

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
     

    /**
     * returns true, if the body data source is open
     * 
     *  @return  true, if the body data source is open
     */
    public final boolean isOpen() {
        callBodyAccessListener();
        
        return nonBlockingStream.isOpen();
    }
    
	
	
	
	final void throwExceptionIfExist() throws IOException {
	    IOException ioe = exceptionRef.get();
	    if (ioe != null) {
	        throw ioe;
	    }
	}

	
	/**
     * see {@link ReadableByteChannel#read(ByteBuffer)}
     */
	public final int read(ByteBuffer buffer) throws IOException {
	    throwExceptionIfExist();
	    
	    int size = buffer.remaining();
        int available = available();
        
        if (available == -1) {
            close();
            return -1;
        }
        
        if (available == 0) {
            return 0;
        }
        
        if (available > 0) {
            if (available < size) {
                size = available;
            }

            if (size > 0) {
                ByteBuffer[] bufs = readByteBufferByLength(size);
                copyBuffers(bufs, buffer);
            }           
        }         
        
        isDataRead = true;
        
        return size;
	}	   

	
    private void copyBuffers(ByteBuffer[] source, ByteBuffer target) {
        for (ByteBuffer buf : source) {
            if (buf.hasRemaining()) {
                target.put(buf);
            }
        }
    }
    
    
    void forwardTo(final BodyDataSink bodyDataSink) throws IOException {
       forwardTo(bodyDataSink, null);
    } 
    
       
    void forwardTo(BodyDataSink bodyDataSink, IBodyCompleteListener completeListener) throws IOException {
        BodyForwarder bodyForwarder;
        
        if (bodyDataSink.getFlushmode() == FlushMode.ASYNC) {
            bodyForwarder = new NonThreadedBodyForwarder(getHeader(), this, bodyDataSink, completeListener);
        } else {
            bodyForwarder = new BodyForwarder(getHeader(), this, bodyDataSink, completeListener);
        }
        this.setDataHandlerSilence(bodyForwarder);
        
        if (HttpUtils.isContainExpect100ContinueHeader(header)) {
            bodyDataSink.flush();
        }
    } 

    
    @Execution(Execution.NONTHREADED)
    private static final class NonThreadedBodyForwarder extends BodyForwarder {

        public NonThreadedBodyForwarder(IHeader header, NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink, IBodyCompleteListener completeListener) {
            super(header, bodyDataSource, bodyDataSink, completeListener);
        }
    }



    /**
     * transfer the available data of the this source channel to the given data sink
     * 
     * @param dataSink   the data sink
     * 
     * @return the number of transfered bytes
     * @throws ClosedChannelException If either this channel or the target channel is closed
     * @throws IOException If some other I/O error occurs
     */
    public long transferTo(BodyDataSink dataSink) throws ProtocolException, IOException, ClosedChannelException {
        return transferTo(dataSink, available());
    }
        
        
    

    /**
     * transfer the data of the this source channel to the given data sink
     * 
     * @param dataSink            the data sink
     * @param length              the size to transfer
     * 
     * @return the number of transfered bytes
     * @throws ClosedChannelException If either this channel or the target channel is closed
     * @throws IOException If some other I/O error occurs
     */ 
    public long transferTo(final BodyDataSink dataSink, final int length) throws ProtocolException, IOException, ClosedChannelException {
        return transferTo((WritableByteChannel) dataSink, length);
    }
    
    

    /**
     * transfer the data of the this source channel to the given data sink
     * 
     * @param dataSink   the data sink
     * @param length     the size to transfer
     * 
     * @return the number of transfered bytes
     * @throws ClosedChannelException If either this channel or the target channel is closed
     * @throws IOException If some other I/O error occurs
     */
    public final long transferTo(WritableByteChannel target, int length) throws IOException, ClosedChannelException {
        callBodyAccessListener();

        throwExceptionIfExist();
        
        isDataRead = true;
        if (length > 0) {
            long written = 0;

            ByteBuffer[] buffers = readByteBufferByLength(length);
            for (ByteBuffer buffer : buffers) {
                while(buffer.hasRemaining()) {
                    written += target.write(buffer);
                }
            }
            return written;

        } else {
            return 0;
        }
    }



    /**
     * transfer the data of the this source channel to the given file
     * 
     * @param file          the file
     * @param resultHandler the transferResultHandler 
     * 
     * @throws FileNotFoundException If the file does not exist
     * @throws ClosedChannelException If either this channel or the target channel is closed
     * @throws IOException If some other I/O error occurs
     */
    public final void transferTo(File file, ITransferResultHandler resultHandler) throws IOException, FileNotFoundException, ClosedChannelException {
        callBodyAccessListener();

        throwExceptionIfExist();
        
        isDataRead = true;
        
        if (!file.exists()) {
        	throw new FileNotFoundException("file " + file.getAbsolutePath() + " does not exist");
        } else {
        	BodyDataSink dataSink = new FileDataSink(getHeader(), executor, file);
        	
        	TransferResultHandlerAdapter adapter = new TransferResultHandlerAdapter(resultHandler);
        	dataSink.addCloseListener(adapter);
        	dataSink.addDestroyListener(adapter);
        	
        	dataSink.setFlushmode(FlushMode.ASYNC);
        	
        	if (LOG.isLoggable(Level.FINE)) {
        		LOG.fine("forwarding body to file "  + file.getAbsolutePath());
        	}
            forwardTo(dataSink);
        }
    }    

    private final class TransferResultHandlerAdapter implements IBodyCloseListener, IBodyDestroyListener {

    	private final ITransferResultHandler resultHandler;
    	
    	public TransferResultHandlerAdapter(ITransferResultHandler resultHandler) {
    		this.resultHandler = resultHandler;
		}
    	
    	
    	public void onClose() throws IOException {
    		resultHandler.onComplete();
    	}
    	
    	public void onDestroyed() throws IOException {
    		IOException ioe = exceptionRef.get();
    		resultHandler.onException(ioe);
    	}
    }
    
    

    /**
     * suspend the (underlying connection of the) body data source
     *  
     * @throws IOException if an error occurs
     */
    abstract boolean suspend() throws IOException;

    

    /**
     * resume the (underlying connection of the) body data source
     * 
     * @throws IOException if an error occurs
     */
    abstract boolean resume() throws IOException;
    
 
    
    /**
     * read a ByteBuffer by using a delimiter 
     * 
     * For performance reasons, the ByteBuffer readByteBuffer method is 
     * generally preferable to get bytes 
     * 
     * @param delimiter   the delimiter
     * @param maxLength   the max length of bytes that should be read. If the limit is exceeded a MaxReadSizeExceededException will been thrown  
     * @return the ByteBuffer
     * @throws MaxReadSizeExceededException If the max read length has been exceeded and the delimiter hasn't been found     
     * @throws IOException If some other I/O error occurs
     * @throws BufferUnderflowException if not enough data is available  
     */
	public final ByteBuffer[] readByteBufferByDelimiter(String delimiter, int maxLength) throws IOException,MaxReadSizeExceededException {
        callBodyAccessListener();

        throwExceptionIfExist();

        isDataRead = true;
        ByteBuffer[] buffers = nonBlockingStream.readByteBufferByDelimiter(delimiter, maxLength);
        
        onRead();
        return buffers;
	}


	   
    /**
     * read a ByteBuffer  
     * 
     * @param length   the length could be negative, in this case a empty array will be returned
     * @return the ByteBuffer
     * @throws IOException If some other I/O error occurs
     * @throws BufferUnderflowException if not enough data is available 
     */
    public final ByteBuffer[] readByteBufferByLength(int length) throws IOException, BufferUnderflowException {
        callBodyAccessListener();
        return readByteBufferByLengthSilence(length); 
    }
    
    final ByteBuffer[] readByteBufferByLengthSilence(int length) throws IOException, BufferUnderflowException {
        throwExceptionIfExist();
        
        isDataRead = true;
        ByteBuffer[] buffers = nonBlockingStream.readByteBufferByLength(length);
        
        onRead();
        return buffers;
    }

    
    protected void onRead() throws IOException {
        
    }
	

    /**
     * returns the body encoding
     * 
     * @return the body encoding
     */
    String getEncoding() {
        return nonBlockingStream.getEncoding();
    }
    
    
    protected abstract boolean isNetworkendpoint();
    
    
    private ByteBuffer readSingleByteBufferByLength(int length) throws IOException {
        return DataConverter.toByteBuffer(readByteBufferByLength(length));
    }
    
    
    /**
     * read a ByteBuffer by using a delimiter. The default encoding will be used to decode the delimiter 
     * To avoid memory leaks the {@link IReadWriteableConnection#readByteBufferByDelimiter(String, int)} method is generally preferable  
     * <br> 
     * For performance reasons, the ByteBuffer readByteBuffer method is 
     * generally preferable to get bytes 
     * 
     * @param delimiter   the delimiter
     * @return the ByteBuffer
     * @throws IOException If some other I/O error occurs
     * @throws BufferUnderflowException if not enough data is available 
     */
    public final ByteBuffer[] readByteBufferByDelimiter(String delimiter) throws IOException {
        return readByteBufferByDelimiter(delimiter, Integer.MAX_VALUE);
    }

    
    
    /**
     * read a byte array by using a delimiter
     * 
     * For performance reasons, the ByteBuffer readByteBuffer method is
     * generally preferable to get bytes 
     * 
     * @param delimiter   the delimiter  
     * @return the read bytes
     * @throws IOException If some other I/O error occurs
     * @throws BufferUnderflowException if not enough data is available
     */ 
    public final byte[] readBytesByDelimiter(String delimiter) throws IOException {
        return DataConverter.toBytes(readByteBufferByDelimiter(delimiter));
    }

    
    /**
     * read a byte array by using a delimiter
     *
     * For performance reasons, the ByteBuffer readByteBuffer method is
     * generally preferable to get bytes
     *
     * @param delimiter   the delimiter
     * @param maxLength   the max length of bytes that should be read. If the limit is exceeded a MaxReadSizeExceededException will been thrown
     * @return the read bytes
     * @throws MaxReadSizeExceededException If the max read length has been exceeded and the delimiter hasn�t been found     
     * @throws IOException If some other I/O error occurs
     * @throws BufferUnderflowException if not enough data is available 
     */
    public final byte[] readBytesByDelimiter(String delimiter, int maxLength) throws IOException, MaxReadSizeExceededException {
        return DataConverter.toBytes(readByteBufferByDelimiter(delimiter, maxLength));
    }

    
    /**
     * read bytes by using a length definition 
     *  
     * @param length the amount of bytes to read  
     * @return the read bytes
     * @throws IOException If some other I/O error occurs
     * @throws IllegalArgumentException, if the length parameter is negative 
     * @throws BufferUnderflowException if not enough data is available 
     */ 
    public final byte[] readBytesByLength(int length) throws IOException {
        return DataConverter.toBytes(readByteBufferByLength(length));
    }

    
    /**
     * read a string by using a delimiter 
     * 
     * @param delimiter   the delimiter
     * @return the string
     * @throws IOException If some other I/O error occurs
     * @throws UnsupportedEncodingException if the default encoding is not supported
     * @throws BufferUnderflowException if not enough data is available
     */
    public final String readStringByDelimiter(String delimiter) throws IOException, UnsupportedEncodingException {
        return readStringByDelimiter(delimiter, getEncoding());
    }

    /**
     * read a string by using a delimiter 
     * 
     * @param delimiter   the delimiter
     * @param encoding    encoding
     * @return the string
     * @throws IOException If some other I/O error occurs
     * @throws UnsupportedEncodingException if the default encoding is not supported
     * @throws BufferUnderflowException if not enough data is available
     */
    public final String readStringByDelimiter(String delimiter, String encoding) throws IOException, UnsupportedEncodingException {
        removeLeadingBOM();
        return DataConverter.toString(readByteBufferByDelimiter(delimiter), encoding);
    }
    
    /**
     * read a string by using a delimiter
     *
     * @param delimiter   the delimiter
     * @param maxLength   the max length of bytes that should be read. If the limit is exceeded a MaxReadSizeExceededException will been thrown
     * @return the string
     * @throws MaxReadSizeExceededException If the max read length has been exceeded and the delimiter hasn�t been found     
     * @throws IOException If some other I/O error occurs
     * @throws UnsupportedEncodingException If the given encoding is not supported
     * @throws BufferUnderflowException if not enough data is available
     */
    public final String readStringByDelimiter(String delimiter, int maxLength) throws IOException,UnsupportedEncodingException, MaxReadSizeExceededException {
        return readStringByDelimiter(delimiter, getEncoding(), maxLength);
    }

    
    
    /**
     * read a string by using a delimiter
     *
     * @param delimiter   the delimiter
     * @param maxLength   the max length of bytes that should be read. If the limit is exceeded a MaxReadSizeExceededException will been thrown
     * @param encoding    the encoding
     * @return the string
     * @throws MaxReadSizeExceededException If the max read length has been exceeded and the delimiter hasn�t been found     
     * @throws IOException If some other I/O error occurs
     * @throws UnsupportedEncodingException If the given encoding is not supported
     * @throws BufferUnderflowException if not enough data is available
     */
    public final String readStringByDelimiter(String delimiter, String encoding, int maxLength) throws IOException,UnsupportedEncodingException, MaxReadSizeExceededException {
        removeLeadingBOM();
        return DataConverter.toString(readByteBufferByDelimiter(delimiter, maxLength), encoding);
    }

    
    /**
     * read a string by using a length definition
     * 
     * @param length the amount of bytes to read  
     * @return the string
     * @throws IOException If some other I/O error occurs
     * @throws UnsupportedEncodingException if the given encoding is not supported 
     * @throws IllegalArgumentException, if the length parameter is negative 
     * @throws BufferUnderflowException if not enough data is available
     */
    public final String readStringByLength(int length) throws IOException, BufferUnderflowException {
        return readStringByLength(length, getEncoding());
    }

    
    /**
     * read a string by using a length definition
     * 
     * @param length    the amount of bytes to read
     * @param encoding  the encoding
     * @return the string
     * @throws IOException If some other I/O error occurs
     * @throws UnsupportedEncodingException if the given encoding is not supported 
     * @throws IllegalArgumentException, if the length parameter is negative 
     * @throws BufferUnderflowException if not enough data is available
     */
    public final String readStringByLength(int length, String encoding) throws IOException, BufferUnderflowException {
        removeLeadingBOM();
        return DataConverter.toString(readByteBufferByLength(length), encoding);
    }
    
    final void removeLeadingBOM() throws IOException {
        if (!isDataRead) {
            ByteBuffer copy = HttpUtils.duplicateAndMerge(copyContent());
            
            if (getEncoding().equalsIgnoreCase("UTF-8")) {
                if (HttpUtils.startsWithUTF8BOM(copy)) {
                    readByteBufferByLength(3);
                }
                
            } else if (getEncoding().equalsIgnoreCase("UTF-16BE")) {
                if (HttpUtils.startsWithUTF16BEBOM(copy)) {
                    readByteBufferByLength(2);
                }
    
            } else if (getEncoding().equalsIgnoreCase("UTF-16LE")) {
                if (HttpUtils.startsWithUTF16LEBOM(copy)) {
                    readByteBufferByLength(2);
                }
                
            } else if (getEncoding().equalsIgnoreCase("UTF-32BE")) {
                if (HttpUtils.startsWithUTF32BEBOM(copy)) {
                    readByteBufferByLength(4);
                }            
            } else if (getEncoding().equalsIgnoreCase("UTF-32LE")) {
                if (HttpUtils.startsWithUTF32LEBOM(copy)) {
                    readByteBufferByLength(4);
                }       
            }
        }
    }
    
    
    /**
     * read a double
     * 
     * @return the double value
     * @throws IOException If some other I/O error occurs
     * @throws BufferUnderflowException if not enough data is available
     */
    public final double readDouble() throws IOException {
        isDataRead = true;
        return readSingleByteBufferByLength(8).getDouble();
    }

    

    /**
     * read a long
     * 
     * @return the long value
     * @throws IOException If some other I/O error occurs
     * @throws BufferUnderflowException if not enough data is available
     */
    public final long readLong() throws IOException {
        isDataRead = true;
        return readSingleByteBufferByLength(8).getLong();
    }

    
    /**
     * read an int
     * 
     * @return the int value
     * @throws IOException If some other I/O error occurs
     * @throws BufferUnderflowException if not enough data is available
     */
    public final int readInt() throws IOException {
        isDataRead = true;
        return readSingleByteBufferByLength(4).getInt();
    }

    
    /**
     * read a short value
     * 
     * @return the short value
     * @throws IOException If some other I/O error occurs
     * @throws BufferUnderflowException if not enough data is available
     */
    public final short readShort() throws IOException {
        isDataRead = true;
        return DataConverter.toByteBuffer(readBytesByLength(2)).getShort();
    }

    

    /** 
     * read a byte
     * 
     * @return the byte value
     * @throws IOException If some other I/O error occurs
     * @throws BufferUnderflowException if not enough data is available 
     */
    public final byte readByte() throws IOException {
        callBodyAccessListener();

        return DataConverter.toByteBuffer(readBytesByLength(1)).get();
    }
    
    /**
     * Marks the read position in the connection. Subsequent calls to resetToReadMark() will attempt
     * to reposition the connection to this point.
     *
     */
    public final void markReadPosition() {
        callBodyAccessListener();

        nonBlockingStream.markReadPosition();
    }


    /**
     * Resets to the marked read position. If the connection has been marked,
     * then attempt to reposition it at the mark.
     *
     * @return true, if reset was successful
     */
    public final boolean resetToReadMark() {
        callBodyAccessListener();

        return nonBlockingStream.resetToReadMark();
    }


    
    /**
     * remove the read mark
     */
    public final void removeReadMark() {
        callBodyAccessListener();

        nonBlockingStream.removeReadMark();
    }

    
    /**
     * Returns the index of the first occurrence of the given string.
     *
     * @param str any string
     * @return if the string argument occurs as a substring within this object, then
     *         the index of the first character of the first such substring is returned;
     *         if it does not occur as a substring, -1 is returned.
     * @throws IOException If some other I/O error occurs
     */
    public final int indexOf(String str) throws IOException {
        callBodyAccessListener();
        
        throwExceptionIfExist();
        return nonBlockingStream.indexOf(str);
    }
    


    /**
     * Returns the index  of the first occurrence of the given string.
     *
     * @param str          any string
     * @param encoding     the encoding to use
     * @return if the string argument occurs as a substring within this object, then
     *         the index of the first character of the first such substring is returned;
     *         if it does not occur as a substring, -1 is returned.
     * @throws IOException If some other I/O error occurs
     */
    public final int indexOf(String str, String encoding) throws IOException, MaxReadSizeExceededException {
        callBodyAccessListener();

        throwExceptionIfExist();
        return nonBlockingStream.indexOf(str, encoding);
    }
    
    /**
     * get the version of read buffer. The version number increases, if
     * the read buffer queue has been modified 
     *
     * @return the read buffer version
     * @throws IOException if an exception occurs
     */
    public int getReadBufferVersion() throws IOException {
        callBodyAccessListener();
        
        return getReadBufferVersionSilence();
    }
    
    
    final int getReadBufferVersionSilence() throws IOException {
        throwExceptionIfExist();
        return nonBlockingStream.getReadBufferVersion();
    }
    

    
    /**
     * returns body data receive timeout
     * 
     * @return the body data receive timeout or <code>null</code>
     */
    public long getBodyDataReceiveTimeoutMillis() {
        callBodyAccessListener();

        return getBodyDataReceiveTimeoutMillisSilence();
    }
    
    
    long getBodyDataReceiveTimeoutMillisSilence() {
        return bodyDataReceiveTimeoutMillis;
    }
    
    /**
     * copies the body content
     *  
     * @return the copy
     */
    final ByteBuffer[] copyContent() {
        return nonBlockingStream.copyContent();
    }
    
    
    final IMultimodeExecutor getExecutor() {
        return executor;
    }
    
    
   
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        try {
            return nonBlockingStream.toString();
        } catch (Exception e) {
            return "[" + getId() + "] error occured by performing toString: " + DataConverter.toString(e);
        }
    }
    
    
    
   static interface ITransferResultHandler {

       /**
        * call back if the artifact is complete received
        * 
        * @throws IOException if an exception occurs
        */
       	void onComplete() throws IOException;	

		
    	/**
    	 * call back if an error is occurred
    	 * 
    	 * @param ioe  the exception
    	 * @throws IOException if an exception occurs
    	 */
		void onException(IOException ioe) throws IOException;
	}

    
	
    private final class NonBlockingStream extends AbstractNonBlockingStream {
        
        public void destroy() {
            drainReadQueue();
            callBodyDataHandler(true);
        }
        
        public void setComplete() {
            callBodyDataHandler(true);
        }
        
        @Override
        protected boolean isDataWriteable() {
            return false;
        }
        
        int getSize() {
            return getReadQueueSize();
        }

        @Override
        protected boolean isMoreInputDataExpected() {
            return NonBlockingBodyDataSource.this.isMoreInputDataExpected();
        }

        public boolean isOpen() {
            return (super.available() != -1);
        }
        
        public int append(ByteBuffer buffer) {
            
            int size = 0;
            if (buffer != null) {
                size = buffer.remaining();
                appendDataToReadBuffer(new ByteBuffer[] { buffer }, size);
            }

            callBodyDataHandler(false);
            
            return size;
        }

        
        public int append(ByteBuffer[] buffer) {
            int size = 0;

            if (buffer != null) {
                for (ByteBuffer byteBuffer : buffer) {
                    size += byteBuffer.remaining();
                }
                
                appendDataToReadBuffer(buffer, size);
            } 

            callBodyDataHandler(true);
            
            return size;
        }
        
               
        
        public int append(ByteBuffer[] buffers, IWriteCompletionHandler completionHandler, boolean force) {

            int size = 0;
            
            if (buffers != null) {
                size += append(buffers);
            } 
               
            if (completionHandler != null) {
                new WriteCompletionHolder(completionHandler, executor, buffers).callOnWritten();
            }
            
            callBodyDataHandler(true);
            
            return size;
        }

        
        ByteBuffer[] copyContent() {
            return super.copyReadQueue();
        }
        
        @Override
        public String toString() {
            return printReadBuffer(NonBlockingBodyDataSource.this.getEncoding());
        }
    }	
    
        
      private final class BodyDataHandlerAdapter implements IBodyDataHandler {
        
        private final IBodyDataHandler delegate; 
        private final int executionMode;

        private final AtomicReference<PartParser> partParserRef = new AtomicReference<PartParser>();

        
        public BodyDataHandlerAdapter(PartParser partParser) {
            delegate = null;
            executionMode = -1;
            
            setPartParser(partParser);
        }
    
        BodyDataHandlerAdapter(PartParser partParser, IBodyDataHandler bodyDataHandler, int executionMode) {
            assert (bodyDataHandler != null);
            
            this.delegate = bodyDataHandler;
            this.executionMode = executionMode;
            
            setPartParser(partParser);
        }
    
        
        BodyDataHandlerAdapter newBodyDataHandlerAdapter(IBodyDataHandler bodyDataHandler) {
            BodyDataHandlerAdapter adapter;

            if (bodyDataHandler == null) {
                adapter = new BodyDataHandlerAdapter(getPartParser());
            } else {
                Integer executionMode = HttpUtils.getExecutionMode(bodyDataHandler);
                adapter = new BodyDataHandlerAdapter(getPartParser(), bodyDataHandler, executionMode);
            }
            return adapter;
        }
        
        
        void setPartParser(PartParser partParser) {
            partParserRef.set(partParser);
            
            if (partParser != null) {
                Runnable task = new Runnable() {
                    
                    public void run() {
                        callBodyDataHandler(true);
                    }
                };
                
                getExecutor().processNonthreaded(task);
            }
        }
        
        PartParser getPartParser() {
            return partParserRef.get();
        }
        IBodyDataHandler getDelegate() {
            return delegate;
        }
        
        
        public synchronized boolean onData(final NonBlockingBodyDataSource bodyDataSource) throws BufferUnderflowException {
           
            final PartParser partParser = partParserRef.get(); 
            if (partParser != null) {
                Runnable task = new Runnable() {
                    public void run() {
                        partParser.onData(bodyDataSource);
                    }
                };
                bodyDataSource.getExecutor().processNonthreaded(task);
            }
            
            
            if (delegate != null) {
                if (executionMode == Execution.MULTITHREADED) {
                    Runnable task = new Runnable() {
                        public void run() {
                            performOnData(bodyDataSource);
                        }
                    };
                    bodyDataSource.getExecutor().processMultithreaded(task);
                    
                } else if (executionMode == Execution.NONTHREADED){
                    Runnable task = new Runnable() {
                        public void run() {
                            performOnData(bodyDataSource);
                        }
                    };
                    bodyDataSource.getExecutor().processNonthreaded(task);
                    
                } else {  // unsynchronized
                    performOnData(bodyDataSource);
                }
            }
            
            return true;
        }
        
        private boolean performOnData(NonBlockingBodyDataSource bodyDataSource) {
            
            try {
                // get pre version
                int preVersion = getVersion();
                
                // perform call 
                boolean success = delegate.onData(bodyDataSource);

                // get post version 
                int postVersion = getVersion();

                
                // should call be repeated?
                if (success && (preVersion != postVersion) && ((getSize() != 0))) {
                    // yes, initiate it
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + getId() + "] re-initiate calling body data handler (read queue size=" + getSize() + ")");
                    }
                    callBodyDataHandler(false);
                }
                    
            } catch (BufferUnderflowException bue) {
                // swallow it
            
            } catch (Exception e) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + bodyDataSource.getId() + "] error occured by calling onData of " + delegate.getClass().getName() + "#" + delegate.hashCode() + " " + e.toString() + " destroying body data source");
                }
                bodyDataSource.destroy(e.toString());
            }
            
            return true;
        }
        
        
        @Override
        public String toString() {
            if (delegate == null) {
                return this.getClass().getName() + " -> null";
            } else {
                return this.getClass().getName() + " -> " + delegate;
            }
        }
    }
  
    
      
    
    
    private static final class TimeoutWatchDogTask extends TimerTask {
        
        private WeakReference<NonBlockingBodyDataSource> dataSourceRef = null;
        
        public TimeoutWatchDogTask(NonBlockingBodyDataSource dataSource) {
            dataSourceRef = new WeakReference<NonBlockingBodyDataSource>(dataSource);
        }
    
        
        @Override
        public void run() {
            try {
                NonBlockingBodyDataSource dataSource = dataSourceRef.get();
                
                if (dataSource == null)  {
                    this.cancel();
                    
                } else {
                    dataSource.checkTimeouts();
                }
            } catch (Exception e) {
                // eat and log exception
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("error occured by checking timeouts " + e.toString());
                }
            }
        }       
    }
    
    
    static final class WriteCompletionHolder implements Runnable {
        
        private final IWriteCompletionHandler handler;
        private final CompletionHandlerInfo handlerInfo;
        private final IMultimodeExecutor executor;
        private final int size;

        

        public WriteCompletionHolder(IWriteCompletionHandler handler, IMultimodeExecutor executor, ByteBuffer[] bufs) {
            this.handler = handler;
            this.executor = executor;
            this.handlerInfo = HttpUtils.getCompletionHandlerInfo(handler);
            this.size = computeSize(bufs);
        }

        
        private static int computeSize(ByteBuffer[] bufs) {
            if (bufs == null) {
                return 0;
            }
            
            int i = 0;
            for (ByteBuffer byteBuffer : bufs) {
                i += byteBuffer.remaining();
            }
            
            return i;
        }
        
      
        void performOnWritten(boolean isForceMultithreaded) {
            executor.processMultithreaded(this);
        }
      
      
        public void run() {
            callOnWritten();
        }

        private void callOnWritten() {
            
            try {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("data (size=" + size + " bytes) has been written. calling " + handler.getClass().getSimpleName() + "#" + handler.hashCode() +  " onWritten method");
                }
                handler.onWritten(size);
            } catch (IOException ioe) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("error occured by calling onWritten " + ioe.toString() + " closing connection");
                }
                
                performOnException(ioe);
            }
        }

      
        void performOnException(final IOException ioe) {
            if (handlerInfo.isOnExceptionMutlithreaded()) {
                Runnable task = new Runnable() {
                    public void run() {
                        callOnException(ioe);
                    }
                };
                executor.processMultithreaded(task);
              
            } else {
                Runnable task = new Runnable() {
                    public void run() {
                        callOnException(ioe);
                    }
                };
                executor.processNonthreaded(task);
            }
        }
      
      
        private void callOnException(IOException ioe) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("calling " + handler.getClass().getSimpleName() + "#" + handler.hashCode() +  " onException with " + ioe.toString());
            }

            handler.onException(ioe);
        }
    } 
    
	
    private final class PartHandlerAdapter implements IPartHandler {
        
        private final IPartHandler delegate; 
        private final PartHandlerInfo partHandlerInfo;
        
        PartHandlerAdapter(IPartHandler partHandler) {
            assert (partHandler != null);
            
            partHandlerInfo = HttpUtils.getPartHandlerInfo(partHandler);
            if (partHandlerInfo.isHandlerInvokeOnMessageReceived()) {
            	delegate = new InvokeIOnMessagePartHandlerAdapter(partHandler);
            } else {
            	delegate = partHandler;
            }
        }
    
        public void onPart(final NonBlockingBodyDataSource dataSource) throws IOException, BadMessageException {
        	
            if (partHandlerInfo.isHandlerMultithreaded()) {
                Runnable task = new Runnable() {
                    public void run() {
                        performOnPart(dataSource);
                    }
                };
                dataSource.getExecutor().processMultithreaded(task);
                
            } else  {
                Runnable task = new Runnable() {
                    public void run() {
                        performOnPart(dataSource);
                    }
                };
                dataSource.getExecutor().processNonthreaded(task);
            } 
        }
        
        
        private void performOnPart(NonBlockingBodyDataSource dataSource) {
            try {
            	delegate.onPart(dataSource);

            } catch (BufferUnderflowException bue) {
                // ignore
                                
            } catch (IOException e) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + getId() + "] closing data source.  An io exception occured while performing onPart " + DataConverter.toString(e));
                }
                closeQuitly();
                
            } catch (Throwable t) {
                LOG.warning("[" + getId() + "] closing data source. Error occured by performing onPart " + DataConverter.toString(t));
                closeQuitly();
            }            	
        }
    }
    
    
    private static final class InvokeIOnMessagePartHandlerAdapter implements IPartHandler, IBodyCloseListener, IBodyCompleteListener  {
    	
    	private final IPartHandler delegate;
    	private NonBlockingBodyDataSource dataSource = null;
    	
    	public InvokeIOnMessagePartHandlerAdapter(IPartHandler delegate) {
    		this.delegate = delegate;
		}
    	
    	public void onPart(NonBlockingBodyDataSource dataSource) throws IOException, BadMessageException {
    		this.dataSource = dataSource;
    		
    		dataSource.addCloseListener(this);
    		dataSource.addCompleteListener(this);
    	}
    	
    	public void onComplete() throws IOException {
    		delegate.onPart(dataSource);
    	}
    	
    	public void onClose() throws IOException {
    		delegate.onPart(dataSource);
    	}
    }	  
    
    
    
    private final class CompleteListeners extends AbstractListeners<IBodyCompleteListener> {
        
        @Override
        void onCall(final IBodyCompleteListener listener) throws IOException {
            
            Integer executionMode = HttpUtils.getListenerExecutionMode(listener, "onComplete");
            
            if (executionMode == HttpUtils.EXECUTIONMODE_UNSYNCHRONIZED) {
                listener.onComplete();
                
            } else {
                
                Runnable task = new Runnable() {
                    public void run() {
                        try {
                            listener.onComplete();
                        } catch (IOException ioe) {
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.fine("[" + getId() + "] Error occured by calling complete listener " + listener + " " + ioe.toString());
                            }
                            destroy(ioe.toString());
                        }
                    }
                };
                
                if (executionMode == Execution.MULTITHREADED) {
                    getExecutor().processMultithreaded(task);
                } else {
                    getExecutor().processNonthreaded(task);
                }
            }
        }
    }
}
