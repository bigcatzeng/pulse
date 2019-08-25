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


import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xlightweb.AbstractHttpConnection.IMultimodeExecutor;
import org.xsocket.DataConverter;
import org.xsocket.connection.IWriteCompletionHandler;
import org.xsocket.connection.IConnection.FlushMode;




/**
 * Implementation base of a body forwarder. This class handles closing of he data source and exceptions by 
 * forwarding the body data. Example:
 * 
 * <pre>
 * 
 * class ForwardHandler implements IHttpRequestHandler {
 * 
 *   private HttpClient httpClient = ...
 *    
 *    
 *   // will be called if request header is received (Default-InvokeOn is HEADER_RECEIVED) 
 *   public void onRequest(IHttpExchange exchange) throws IOException {
 *      
 *      IHttpRequestHeader requestHeader = exchange.getRequest().getRequestHeader();
 *      NonBlockingBodyDataSource requestBodyChannel = exchange.getRequest().getNonBlockingBody();
 *      
 *       // forwards the request (header)
 *       BodyDataSink dataSink = httpClient.send(requestHeader);
 *       
 *       // defines the body forwarder
 *       BodyForwarder bodyForwarder = new BodyForwarder(requestBodyChannel, dataSink) {
 *       
 *          public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException, IOException {
 *             int available = bodyDataSink.available();
 *             ByteBuffer[] data  = bodyDataSink.readByteBufferByLength(available);
 *             
 *             for (ByteBuffer byteBuffer : data) {
 *                ByteBuffer copy = byteBuffer.duplicate();
 *                // ...
 *             }
 *             
 *             bodyDataSink.write(data);
 *          }
 *       };
 *       
 *       // assign the body forwarder to the body data source 
 *       requestBodyChannel.setDataHandler(bodyForwarder);
 *    }
 * }
 * </pre>
 * 
 * 
 * @author grro@xlightweb.org
 */
public class BodyForwarder implements IBodyDataHandler {
	
	private static final Logger LOG = Logger.getLogger(BodyForwarder.class.getName());
	
	private static final int DEFAULT_AUTOSUSPEND_THRESHOLD = 32768;
    private static final int AUTO_SUSPEND_THRESHOLD = readAutosuspendThreshold(DEFAULT_AUTOSUSPEND_THRESHOLD); 
	
	private final AtomicBoolean isClosed = new AtomicBoolean(false);
	private final NonBlockingBodyDataSource bodyDataSource;
	private final BodyDataSink bodyDataSink;
	private final IBodyCompleteListener completeListener;
	private boolean isCompleteLisgenerCalled = false;
	
	
	
	
	/**
	 * constructor
	 * 
	 * @param bodyDataSource   the body data source 
	 * @param bodyDataSink     the body data sink
	 */
	public BodyForwarder(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) {
		this(null, bodyDataSource, bodyDataSink, null);
	}
	
	
	BodyForwarder(IHeader header, NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink, IBodyCompleteListener completeListener) {
		this.bodyDataSource = bodyDataSource;
		this.completeListener = completeListener;

		
		if ((header != null) && (bodyDataSink.getFlushmode() == FlushMode.ASYNC) && bodyDataSource.isNetworkendpoint() && bodyDataSink.isNetworkendpoint() && (AUTO_SUSPEND_THRESHOLD != Integer.MAX_VALUE)) {
		    this.bodyDataSink = new FlowControlledBodyDataSink(header, bodyDataSource, bodyDataSink);
		} else {
		    this.bodyDataSink = bodyDataSink;
		}
		
		bodyDataSink.addDestroyListener(new DestroyListener());
	}
	
	
    private static int readAutosuspendThreshold(int dflt) {
        int threshold = Integer.parseInt(System.getProperty("org.xlightweb.forwarding.autosuspend.thresholdbytes", Integer.toString(dflt)));
        if (threshold <= 0) {
            threshold = Integer.MAX_VALUE;
        }
        
        return threshold;
    }
    	
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public final boolean onData(final NonBlockingBodyDataSource bodyDataSource) throws BufferUnderflowException {
	    
        boolean isModified = true;
        int available = 0;
	    
	    try {
	        do {
    	        try {
    	            available = bodyDataSource.availableSilence();
    	        } catch (Exception e) {
    	            if (LOG.isLoggable(Level.FINE)) {
    	                LOG.fine("[" + bodyDataSource.getId() + " -> " +  bodyDataSink.getId() + "] data source error occured " + e.toString());
    	            }
    	            bodyDataSink.destroy();
    	            onException(e);
    	            return true;
    	        }

                    
                if (available >= 0) {
                    isModified = forwardData();
                    
                } else {
                   	if ((completeListener != null) && bodyDataSource.isComplete()) {
                   	    if (!isCompleteLisgenerCalled) {
                   	        isCompleteLisgenerCalled = true;
                   	        completeListener.onComplete();
                   	    }
    	        		return true;
    	        	}
                }

	        } while ((available > 0) && isModified);

	        
            if (available == -1) {
                handleEndOfSourceStream();
                return true;
            }
	        
        } catch (IOException e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + bodyDataSource.getId() + " -> " +  bodyDataSink.getId() + "] error by reading body source or forwarding (" + available + ") data to data sink " + e);
            }
            onException(e);
            destroy(e.toString());
        } 
        
		return true;
	}
	
	
	private boolean forwardData() throws IOException {
	    int version = bodyDataSource.getReadBufferVersionSilence();

	    onData(bodyDataSource, bodyDataSink);
                
	    if (version == bodyDataSource.getReadBufferVersionSilence()) {
	        return false;
	    } else {
	        return true;
	    }
	}

	
    /**
     * call back method which will be called if the body data source contains a least one byte
     * 
     * @param bodyDataSource   the body data source 
     * @param bodyDataSink     the body data sink
     * @throws BufferUnderflowException if not enough data is available 
     * @throws IOException if an exception occurs 
     */
    public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException, IOException {
        int available = bodyDataSource.availableSilence();

        if (available >= 0) {
            ByteBuffer[] data;

            if (available == 0) {
                data = new ByteBuffer[0];
                
            } else {
                data = bodyDataSource.readByteBufferByLengthSilence(available);
               	if (LOG.isLoggable(Level.FINE)) {
               		LOG.fine("[" + bodyDataSource.getId() + " -> " +  bodyDataSink.getId() + "] forwarding " + DataConverter.toString(HttpUtils.copy(data)));
               	}
            }
    	     
           	bodyDataSink.write(data);
        }
    }
	
    
  
    

	/**
	 * call back which will be executed, if an exception is occurred
	 * 
	 * @param e the excption
	 */
	public void onException(Exception e) {
	}


	


    
	
	
	private void handleEndOfSourceStream() {
	    
	    if (!isClosed.getAndSet(true)) {
    		if (LOG.isLoggable(Level.FINE)) {
    			LOG.fine("[" + bodyDataSource.getId() + " -> " +  bodyDataSink.getId() + "] end of stream reached. dettach data source and closing data sink");
    		}
    		
    		// detach the link   dataSource -> body forwarder
    		bodyDataSource.setDataHandler(null);  

            onComplete();
            // detach the link   body forwarder -> bodyDataSink 
            try {
                bodyDataSink.close();
            } catch (Exception e) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + bodyDataSource.getId() + " -> " +  bodyDataSink.getId() + "] error occured by closing body data sink " + e.toString());
                }
                destroy(e.toString());
            }
	    }    		
	}


	


	/**
	 * call back which will be executed, if the body data source's end of data is reached 
	 */
    public void onComplete() {
        
    }
        
	
	private void destroy(String reason) {
		bodyDataSink.destroy();      // detach the link   dataSource -> body forwarder 
		bodyDataSource.destroy(reason);    // detach the link   dataSource -> body forwarder 
	}
	
	
	
	private final class DestroyListener implements IBodyDestroyListener {
	    
	    public void onDestroyed() throws IOException {
	        if (LOG.isLoggable(Level.FINE)) {
	            LOG.fine("[" + bodyDataSource.getId() + " -> " +  bodyDataSink.getId() + "] data sink has been destroyed. destroying data source");	            
	        }
	        boolean isIgnoreWriteError = bodyDataSink.isIgnoreWriteError();
	        bodyDataSource.destroy("Forwarder: body data sink is closed", isIgnoreWriteError);  
	    }
	}
	
	




	
	
	private static final class FlowControlledBodyDataSink extends BodyDataSink {
	    
        private final BodyDataSink dataSink;
        private final NonBlockingBodyDataSource dataSource;
	    private final WriteCompletionHandler writeCompletionHandler;
	    
	    public FlowControlledBodyDataSink(IHeader header, NonBlockingBodyDataSource dataSource, BodyDataSink dataSink) {
	    	super(header); 
	    	
	        this.writeCompletionHandler = new WriteCompletionHandler(dataSource, dataSink);
	        this.dataSource = dataSource;
	        this.dataSink = dataSink;
        }
	    
	    @Override
	    void addCloseListener(IBodyCloseListener closeListener) {
	        dataSink.addCloseListener(closeListener);
	    }
	    
	    @Override
	    public void addDestroyListener(IBodyDestroyListener destroyListener) {
	        dataSink.addDestroyListener(destroyListener);
	    }

	    
	    @Override
	    IMultimodeExecutor getExecutor() {
	    	return dataSink.getExecutor();
	    }
	    
	    @Override
	    public void doClose() throws IOException {
	        dataSink.close();
	    }
	    
	    @Override
	    public void closeQuitly() {
	        dataSink.closeQuitly();
	    }
	    
	    
	    @Override
	    public void destroy() {
	        dataSink.destroy();
	    }
	    
	    @Override
	    public void flush() throws IOException {
	        dataSink.flush();
	    }
	    
	    @Override
	    public Object getAttachment() {
	        return dataSink.getAttachment();
	    }
	    
	    @Override
	    public String getEncoding() {
	        return dataSink.getEncoding();
	    }
	    
	    
	    @Override
	    public FlushMode getFlushmode() {
	        return dataSink.getFlushmode();
	    }
	    
	    @Override
	    public String getId() {
	        return dataSink.getId();
	    }
	    
	    @Override
	    public boolean isAutoflush() {
	        return dataSink.isAutoflush();
	    }
	    
	    @Override
	    public boolean isOpen() {
	        return dataSink.isOpen();
	    }
	    
	    @Override
	    public void markWritePosition() {
	        dataSink.markWritePosition();
	    }
	    
	    @Override
	    public void removeWriteMark() {
	        dataSink.removeWriteMark();
	    }
	    
	    @Override
	    public boolean resetToWriteMark() {
	        return dataSink.resetToWriteMark();
	    }
	    
	    @Override
	    public void setAttachment(Object obj) {
	        dataSink.setAttachment(obj);
	    }
	    
	    @Override
	    public void setAutoflush(boolean autoflush) {
	        dataSink.setAutoflush(autoflush);
	    }
	    
	    @Override
	    public void setEncoding(String defaultEncoding) {
	        dataSink.setEncoding(defaultEncoding);
	    }
	    
	    @Override
	    public void setFlushmode(FlushMode flushMode) {
	        dataSink.setFlushmode(flushMode);
	    }
	    
	    @Override
	    public void setSendTimeoutMillis(long sendTimeoutMillis) {
	        dataSink.setSendTimeoutMillis(sendTimeoutMillis);
	    }
	    
	       
        @Override
        boolean isNetworkendpoint() {
            return false;
        }
        
        @Override
        boolean isIgnoreWriteError() {
            return dataSink.isIgnoreWriteError();
        }
        
        @Override
        int getPendingWriteDataSize() {
            return dataSink.getPendingWriteDataSize();
        }
        
        @Override
        int getSizeWritten() {
            return dataSink.getSizeWritten();
        }
        
	    @Override
	    public long transferFrom(BodyDataSource source) throws IOException {
	        return dataSink.transferFrom(source);
	    }
	    
	    @Override
	    public long transferFrom(BodyDataSource source, int length) throws IOException {
	        return dataSink.transferFrom(source, length);
	    }
	    
	    @Override
	    public long transferFrom(FileChannel fileChannel) throws IOException, BufferOverflowException {
	        return dataSink.transferFrom(fileChannel);
	    }
	    
	    @Override
	    public long transferFrom(NonBlockingBodyDataSource source) throws IOException {
	        return dataSink.transferFrom(source);
	    }
	    
	    @Override
	    public long transferFrom(NonBlockingBodyDataSource source, int length) throws IOException {
	        return dataSink.transferFrom(source, length);
	    }
	    
	    @Override
	    public long transferFrom(ReadableByteChannel source, int chunkSize) throws IOException, BufferOverflowException {
	        return dataSink.transferFrom(source, chunkSize);
	    }
	    
	    @Override
	    public int write(ByteBuffer buffer) throws IOException, BufferOverflowException {
	        return dataSink.write(buffer);
	    }
	    
	    @Override
	    public long write(ByteBuffer[] buffers) throws IOException, BufferOverflowException {
	        int size = HttpUtils.computeRemaining(buffers);
	        
	        if (size > 0) {
    	        if ((dataSink.getPendingWriteDataSize() + size) > AUTO_SUSPEND_THRESHOLD) {
                    boolean isSuspended = dataSource.suspend();
                    if (isSuspended) {  
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("[" + dataSource.getId() + " -> " +  dataSink.getId() + "] suspended (auto suspend threshold " + AUTO_SUSPEND_THRESHOLD + " exceeded)");
                        }
                    }
                } 
	        }
	        dataSink.write(buffers, writeCompletionHandler);
	        
	        return size;
	    }
	    
	    
	    @Override
	    public void write(ByteBuffer[] buffers, IWriteCompletionHandler writeCompletionHandler) throws IOException {
	        dataSink.write(buffers, writeCompletionHandler);
	    }
	}
	
	
	
	
    private static final class WriteCompletionHandler implements IWriteCompletionHandler, IUnsynchronized {
        
        private final NonBlockingBodyDataSource bodyDataSource;
        private final BodyDataSink bodyDataSink;
        
        
        public WriteCompletionHandler(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) {
            this.bodyDataSource = bodyDataSource;
            this.bodyDataSink = bodyDataSink;
        }
        
        
        public void onWritten(int size) throws IOException {
            if (bodyDataSink.getPendingWriteDataSize() <= AUTO_SUSPEND_THRESHOLD) {      
                boolean isResumed = bodyDataSource.resume();
                if (isResumed) {   
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + bodyDataSource.getId() + " -> " +  bodyDataSink.getId() + "] resumed");
                    }
                }
                
            }
        }
        
        public void onException(IOException ex) {
            try {
                bodyDataSource.resume();
            } catch (IOException ignore) { }

            bodyDataSource.destroy();
            bodyDataSink.destroy();
        }
    }	
}
