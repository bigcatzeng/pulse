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

import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.xlightweb.AbstractHttpConnection.IMultimodeExecutor;
import org.xsocket.DataConverter;
import org.xsocket.IDataSink;
import org.xsocket.IDestroyable;
import org.xsocket.connection.IConnection;
import org.xsocket.connection.IWriteCompletionHandler;
import org.xsocket.connection.IConnection.FlushMode;





/**
 * I/O resource capable of sending the body data.
 * 
 * @author grro@xlightweb.org
 *
 */
public abstract class BodyDataSink implements IDataSink, IDestroyable, Flushable, Closeable, WritableByteChannel, GatheringByteChannel {

    private static final int TRANSFER_CHUNK_SIZE = 65536;
     
    
    private final IHeader header;
    
    
    // multipart support
    private Multipart multipart;

    
    
    
    BodyDataSink(IHeader header) {
    	this.header = header;
	}
    
    
    public boolean isMultipart() {
    	return ((header.getContentType() != null) && (header.getContentType().toLowerCase(Locale.US).startsWith("multipart")));
    }
    
    
    final IHeader getHeader() {
    	return header;
    }
    
    
    /**
	 * set the sendtimout 
	 * @param sendTimeoutMillis  the send timeout
	 */
	public abstract void setSendTimeoutMillis(long sendTimeoutMillis);
    

    /**
     * {@inheritDoc}
     */
    public abstract void flush() throws IOException;    
   
	
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public final void close() throws IOException {
		if (multipart != null) {
			multipart.close();
		} else {
			doClose();
		}
	}
	
	abstract void doClose() throws IOException;
	
	
	/**
     * closes this connection by swallowing io exceptions
     */
    public abstract void closeQuitly();

    
    /**
     * writes a buffer array 
     * 
     * @param buffers                  the buffers to write
     * @param writeCompletionHandler   the completion handler
     * @throws IOException if an exception occurs 
     */
    public abstract void write(ByteBuffer[] buffers, IWriteCompletionHandler writeCompletionHandler) throws IOException;    
    
    
    /**
     * {@inheritDoc}
     */
    public abstract int write(ByteBuffer buffer) throws IOException, BufferOverflowException;
    

    /**
     * {@inheritDoc}
     */
    public abstract long write(ByteBuffer[] buffers) throws IOException, BufferOverflowException;
    
    
    /**
     * {@inheritDoc}
     */
    public abstract long transferFrom(ReadableByteChannel source, int chunkSize) throws IOException, BufferOverflowException; 



    /**
     * {@inheritDoc}
     */
    public abstract long transferFrom(FileChannel fileChannel) throws IOException, BufferOverflowException;	

    
    /**
	 * transfer the available data from the data source 
	 * 
	 * @param source   the data source 
	 * @return the transfered size 
	 * 
	 * @throws IOException if an exception occurs
	 */
	public abstract long transferFrom(NonBlockingBodyDataSource source) throws IOException;

	
	/**
	 * transfer the available data from the data source 
	 * 
	 * @param source   the data source 
	 * @param length   the length to transfer
	 * @return the transfered size 
	 * 
	 * @throws IOException if an exception occurs
	 */
	public abstract long transferFrom(NonBlockingBodyDataSource source, int length) throws IOException;
	
	
	/**
	 * transfer all data from the data source 
	 * 
	 * @param source   the data source 
	 * @return the transfered size 
	 * 
	 * @throws IOException if an exception occurs
	 */
	public abstract long transferFrom(BodyDataSource source) throws IOException;
	
	
	/**
	 * transfer data from the data source 
	 * 
	 * @param source   the data source 
	 * @param length   the length to transfer 
	 * @return the transfered size 
	 * 
	 * @throws IOException if an exception occurs
	 */
	public abstract long transferFrom(BodyDataSource source, int length) throws IOException;
	

    /**
     * transfer all data from the data source 
     * 
     * @param source   the data source 
     * @return the transfered size 
     * 
     * @throws IOException if an exception occurs
     */
    public final long transferFrom(InputStream is) throws IOException {
        return transferFrom(Channels.newChannel(is));
    }


    

    /**
     * {@inheritDoc}
     */
    public final long transferFrom(ReadableByteChannel source) throws IOException, BufferOverflowException {
        return transferFrom(source, TRANSFER_CHUNK_SIZE);
    }

    


	/**
	 * {@inheritDoc}
	 */
	public final int write(byte b) throws IOException, BufferOverflowException {
	    return write(new byte[] {b});
	}


	/**
	 * {@inheritDoc}
	 */
	public final int write(byte... bytes) throws IOException, BufferOverflowException {
	    return write(ByteBuffer.wrap(bytes));
	}


	/**
	 * {@inheritDoc}
	 */
	public final int write(byte[] bytes, int offset, int length) throws IOException, BufferOverflowException {
	    return write(DataConverter.toByteBuffer(bytes, offset, length));
	}


	

	/**
	 * {@inheritDoc}
	 */
	public final long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
		return write(DataConverter.toByteBuffers(srcs, offset, length));
	}


	/**
	 * {@inheritDoc}
	 */
	public final long write(List<ByteBuffer> buffers) throws IOException, BufferOverflowException {
		return write(buffers.toArray(new ByteBuffer[buffers.size()]));
	}


	/**
	 * {@inheritDoc}
	 */
	public final int write(int i) throws IOException, BufferOverflowException {
		return write(DataConverter.toByteBuffer(i));
	}


	/**
	 * {@inheritDoc}
	 */
	public final int write(short s) throws IOException, BufferOverflowException {
	    return write(DataConverter.toByteBuffer(s));
	}


	/**
	 * {@inheritDoc}
	 */
	public final int write(long l) throws IOException, BufferOverflowException {
        return write(DataConverter.toByteBuffer(l));
	}


	/**
	 * {@inheritDoc}
	 */
	public final int write(double d) throws IOException, BufferOverflowException {
        return write(DataConverter.toByteBuffer(d));
	}


	/**
	 * {@inheritDoc}
	 */
	public final int write(String message) throws IOException, BufferOverflowException {
        return write(DataConverter.toByteBuffer(message, getEncoding()));
	}

	
	public final BodyDataSink writePart(IHeader partHeader) throws IOException, BufferOverflowException {
		if (multipart == null) {
			multipart = new Multipart();
		}
		
		return multipart.addPart(partHeader);
	}

	

	public final void writePart(IPart part) throws IOException, BufferOverflowException {
		NonBlockingBodyDataSource dataSource = part.getNonBlockingBody();
		BodyDataSink dataSink = writePart(part.getPartHeader());
		dataSource.forwardTo(dataSink, new BodyCompleteListener(dataSource, dataSink));
	}

	
	private static final class BodyCompleteListener implements IBodyCompleteListener {
		private final NonBlockingBodyDataSource dataSource;
		private final BodyDataSink dataSink; 
		
		public BodyCompleteListener(NonBlockingBodyDataSource dataSource, BodyDataSink dataSink) {
			this.dataSource = dataSource;
			this.dataSink = dataSink;
		}
		
		public void onComplete() throws IOException {
			dataSource.closeQuitly();
			dataSink.closeQuitly();
		}
	};
 

	/**
	 * sets the default encoding 
	 * 
	 * @param defaultEncoding  the default encoding 
	 */
	public abstract void setEncoding(String defaultEncoding);


	/**
	 * gets the default encoding 
	 * 
	 * @return  the default encoding
	 */
	public abstract String getEncoding();


	/**
	 * see {@link IConnection#setFlushmode(FlushMode)} 
	 */
	public abstract void setFlushmode(FlushMode flushMode);


	/**
	 * see {@link IConnection#getFlushmode()}
	 */
	public abstract FlushMode getFlushmode();

	
	/**
	 * set autoflush. If autoflush is activated, each write call
	 * will cause a flush. <br><br>
	 *
	 * @param autoflush true if autoflush should be activated
	 */
	public abstract void setAutoflush(boolean autoflush);

	
	
	/**
	 * get autoflush
	 * 
	 * @return true, if autoflush is activated
	 */
	public abstract boolean isAutoflush();
  
	
	
	/**
	 * Marks the write position in the connection. 
	 */
	public abstract void markWritePosition();


	/**
	 * Resets to the marked write position. If the connection has been marked,
	 * then attempt to reposition it at the mark.
	 *
	 * @return true, if reset was successful
	 */
	public abstract boolean resetToWriteMark();


	
	/**
	 * remove the write mark
	 */
	public abstract void removeWriteMark();

	
	/**
	 * Attaches the given object to this connection
	 *
	 * @param obj The object to be attached; may be null
	 * @return The previously-attached object, if any, otherwise null
	 */
	public abstract void setAttachment(Object obj);


	/**
	 * Retrieves the current attachment.
	 *
	 * @return The object currently attached to this key, or null if there is no attachment
	 */
	public abstract Object getAttachment();

	
	
	/**
	 * returns true if the data sink is open
	 * 
	 * @return true if the data sink is open
	 */
	public abstract boolean isOpen();
	

	
    public abstract String getId();
    
    
    /**
     * destroys this data sink  
     */
    public abstract void destroy();
  
	
	/**
     * add a destroy listener
     * @param destroyListener the destroy listener to add 
     */
    public abstract void addDestroyListener(IBodyDestroyListener destroyListener);
    
    
    /**
     * returns the size of pending write data 
     *   
     * @return the size of pending write data
     */
    abstract int getPendingWriteDataSize();
       
    abstract int getSizeWritten();
    
    abstract IMultimodeExecutor getExecutor();
	
    abstract boolean isNetworkendpoint();
    
    abstract boolean isIgnoreWriteError();
    
    abstract void addCloseListener(IBodyCloseListener closeListener);

    void setAutocompressThreshold(int autocompressThreshold) {
    	
    }

    
    
    private final class Multipart implements Closeable {
    	
    	private final List<PartBodyDataSink> pendingParts = new ArrayList<PartBodyDataSink>();
    	private final String boundary;
    	private final AtomicBoolean isPendingClose = new AtomicBoolean(false); 
    	
    	
    	public Multipart() throws IOException {

    		String contentType = header.getContentType();
    		if (contentType == null) {
    			boundary = UUID.randomUUID().toString();
    			header.setContentType("multipart/mixed; boundary=" + boundary);
    			
    		} else {
    			if (!HttpUtils.parseMediaType(contentType).toLowerCase(Locale.US).startsWith("multipart")) {
    				throw new RuntimeException("could not add part. Content-Type " + contentType + " is not a multipart content type");
    				
    			} else {
    				String bound = HttpUtils.parseMediaTypeParameter(contentType, "boundary", true, null);
    				if (bound == null) {
    					boundary = UUID.randomUUID().toString();
    					header.setContentType(header.getContentType() + "; boundary=" + boundary);
    				} else {
    					boundary = bound;
    				}
    			}
    		}
    		
    		write("--" + boundary);
		}  
    	
    	
    	public synchronized void close() throws IOException {
            if (pendingParts.isEmpty()) {
                performClose();
                
            } else {
                if (!isPendingClose.getAndSet(true)) {
                    for (PartBodyDataSink part : pendingParts) {
                        IBodyCloseListener cl = new IBodyCloseListener() {
                            
                            public void onClose() throws IOException {
                                close();
                            }
                        };
                        part.addCloseListener(cl);
                    }
                }
            }
    	}
    	
    	private void performClose() throws IOException {
    		write("--" + "\r\n");
    		doClose();
    	}
    	

    	synchronized BodyDataSink addPart(IHeader partHeader) throws IOException {
    		
    		PartBodyDataSink sink;
    		
    		if (pendingParts.isEmpty()) {
    			sink = new PartBodyDataSink(partHeader, boundary, this, getExecutor());
    			pendingParts.add(sink);
    			sink.activate();
    				
    		} else {
    			sink = new BufferingPartBodyDataSink(partHeader, boundary, this, getExecutor());
    			pendingParts.add(sink);
    		}
    		
    		return sink;
    	}    	
    	


    	synchronized void removePart(PartBodyDataSink sink) throws IOException {
    		if (pendingParts != null) {
    			pendingParts.remove(sink);

    			if (pendingParts.isEmpty()) {
    				if (isPendingClose.get()) {
    					close();
    				}
    				
    			} else {
    				pendingParts.get(0).activate();	
    			}
    		}
    	}
    	
    	BodyDataSink getDataSink() {
    		return BodyDataSink.this;
    	}
     
    }
    
    
    private static class PartBodyDataSink extends BodyDataSinkImplBase {
		
    	private final Multipart multipart;
    	private final String boundary;
    	
		public PartBodyDataSink(IHeader header, String boundary, Multipart multipart, IMultimodeExecutor executor) throws IOException {
			super(header, executor);
			
			this.multipart = multipart;
			this.boundary = boundary;
		}
		
		void activate()  throws IOException {
			multipart.getDataSink().write("\r\n" + getHeader().toString() + "\r\n");
		}

		@Override
		void onClose() throws IOException {
			multipart.getDataSink().write("\r\n--" + boundary);
			multipart.removePart(this);
		}

		@Override
		void onDestroy(String reason) {
			
		}

		@Override
		int onWriteData(ByteBuffer[] dataToWrite, IWriteCompletionHandler completionHandler) throws IOException {
			int size = HttpUtils.computeRemaining(dataToWrite);
			multipart.getDataSink().write(dataToWrite, completionHandler);
			
			return size;
		}

		@Override
		final int getPendingWriteDataSize() {
			return multipart.getDataSink().getPendingWriteDataSize();
		}

		@Override
		final boolean isNetworkendpoint() {
			return false;
		}
	}

	
	
	private static final class BufferingPartBodyDataSink extends PartBodyDataSink {
	
		private boolean isActivated = false;
		private boolean isPendingClose = false;
		private final List<PendingWrite> pendingWrites = new ArrayList<PendingWrite>();
		 
		public BufferingPartBodyDataSink(IHeader header, String boundary, Multipart multipart, IMultimodeExecutor executor) throws IOException {
			super(header, boundary, multipart, executor);
		}
		
		@Override
		synchronized void activate() throws IOException {
			isActivated = true;
			
			super.activate();
			
			if (!pendingWrites.isEmpty()) {
				for (PendingWrite pendingWrite : pendingWrites) {
					onWriteData(pendingWrite.getDataToWrite(), pendingWrite.getCompletionHandler());
				}
				
				pendingWrites.clear();
			}
			
			
			if (isPendingClose) {
				onClose();
			}
		}

		@Override
		synchronized void doClose() throws IOException {
			if (isActivated) {
				isPendingClose = false;
				super.onClose();
			} else {
				isPendingClose = true;
			}
		}
		
		
		@Override
		synchronized void onClose() throws IOException {
		}

		
		@Override
		synchronized int onWriteData(ByteBuffer[] dataToWrite, IWriteCompletionHandler completionHandler) throws IOException {
			
			if (isActivated) {
				return super.onWriteData(dataToWrite, completionHandler);
				
			} else {
				int size = HttpUtils.computeRemaining(dataToWrite);
				pendingWrites.add(new PendingWrite(dataToWrite, completionHandler));
				return size;
			}
		}
	}

	
	
	private static final class PendingWrite {
		private ByteBuffer[] dataToWrite;
		private IWriteCompletionHandler completionHandler;
		
		public PendingWrite(ByteBuffer[] dataToWrite, IWriteCompletionHandler completionHandler) {
			this.dataToWrite = dataToWrite;
			this.completionHandler = completionHandler;
		}
		
		
		public ByteBuffer[] getDataToWrite() {
			return dataToWrite;
		}

		public IWriteCompletionHandler getCompletionHandler() {
			return completionHandler;
		}		
	}    
}
	    
