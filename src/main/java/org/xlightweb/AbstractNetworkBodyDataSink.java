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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import org.xsocket.DataConverter;
import org.xsocket.connection.IWriteCompletionHandler;
import org.xsocket.connection.IConnection.FlushMode;



/**
 * I/O resource capable of receiving the body data.
 * 
 * @author grro@xlightweb.org
 *
 */
abstract class AbstractNetworkBodyDataSink extends BodyDataSinkImplBase {

    private static final Logger LOG = Logger.getLogger(AbstractNetworkBodyDataSink.class.getName());

    private final AbstractHttpConnection httpConnection;
    private CompressingOutputStream cos = null; 
         
    
    /**
     * constructor 
     * 
     * @param header           the header
     * @param httpConnection   the http connection
     */
    public AbstractNetworkBodyDataSink(IHttpMessageHeader header, AbstractHttpConnection httpConnection) throws IOException {
        super(header, httpConnection.getExecutor());
        this.httpConnection = httpConnection;
        
        this.httpConnection.setNetworkBodyDataSink(this);
        httpConnection.getUnderlyingTcpConnection().setFlushmode(FlushMode.ASYNC);
        httpConnection.getUnderlyingTcpConnection().setAutoflush(false);
    }
    
    
    void onDisconnect() {
        httpConnection.setNetworkBodyDataSink(null);
        
        // data sink should not be open
        if (isOpen()) {
            destroy();
        }
    }
    
    
    @Override
    final int onWriteData(ByteBuffer[] dataToWrite, IWriteCompletionHandler completionHandler) throws IOException {
    	if (cos == null) {
    		return onWriteNetworkData(dataToWrite, completionHandler);
    		
    	} else {
    		return cos.write(dataToWrite, completionHandler);
    	}
    }
        
    
    abstract int onWriteNetworkData(ByteBuffer[] dataToWrite, IWriteCompletionHandler completionHandler) throws IOException;
        
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isNetworkendpoint() {
        return true;
    }
    
    
    protected final AbstractHttpConnection getHttpConnection() {
        return httpConnection;
    }
    
    @Override
    void setAutocompressThreshold(int autocompressThreshold) {
    	if ((cos == null) && (autocompressThreshold != Integer.MAX_VALUE)) {
    		
    		if ((((HttpMessageHeader) getHeader()).getAttribute(AbstractNetworkBodyDataSource.AUTOUNCOPMRESSED_ATTR_KEY) != null) && 
    			((Boolean) ((HttpMessageHeader)getHeader()).getAttribute(AbstractNetworkBodyDataSource.AUTOUNCOPMRESSED_ATTR_KEY) == true)) {
    			getHeader().removeHeader(AbstractNetworkBodyDataSource.UNCOMPRESSED_KEY);
    			autocompressThreshold = 0;
    		}
    		
    		cos = new CompressingOutputStream(autocompressThreshold);
    		
    		if (autocompressThreshold == 0) {
    			getHeader().setHeader("Content-Encoding", "gzip");
    		}
    	}
    }
    
    
    /**
     * returns the http connection
     * @return the http connection 
     */
    final AbstractHttpConnection getConnection() {
        return httpConnection;
    }
 
    
    public final String getId() {
        return httpConnection.getId();
    }
    
    @Override
    final void onClose() throws IOException {
    	if (cos != null) {
    		cos.close();
    	}
        
        httpConnection.removeNetworkBodyDataSink(this);
        
        try {
            performClose();
            httpConnection.onMessageWritten();
        } catch(IOException ioe) {
            
            if (isIgnoreWriteError()) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + getId() + "] error occured by closing connection. ignoring it (isIgnoreWriteError=true) " + ioe.toString());
                }
                
            } else {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + getId() + "] error occured by closing connection. destroying it " + ioe.toString());
                }               
                destroy();
                throw ioe;
            }
        }
    }
    
    
    abstract void performClose() throws IOException;
    

    /**
     * {@inheritDoc}
     */
    @Override
    void onDestroy(String reason) {
        httpConnection.removeNetworkBodyDataSink(this);
        
        if (isOpen()) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] destroying connection");
            }
            
            try {
                performDestroy();
            } catch (IOException ioe) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine(ioe.toString());
                }
            }
        }
        
        httpConnection.destroy(reason);
    }
    
    
    abstract void performDestroy() throws IOException;
    
    
    /**
     * {@inheritDoc}
     */
    public int getPendingWriteDataSize() {
        return httpConnection.getUnderlyingTcpConnection().getPendingWriteDataSize();
    }
    
    
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (isOpen()) {
            return getClass().getName() + "#" + hashCode();
        } else {
            return getClass().getName() + "#" + hashCode() + " closed";
        }
    }
    
    
    
    
    private final class CompressingOutputStream {

        private static final int INITIAL = 0;
        private static final int COMPRESSING = 5;
        private static final int PLAIN = 9;
        private int mode = INITIAL; 

    	private int compressThresholdByte;
    	
        private GZIPOutputStream gos = null;
    	private BufferOutputStream bos = null;
    	
    	private int sizePlainData = 0;
    	private int sizeNetworkData = 0;
        
        public CompressingOutputStream(int compressThresholdByte) {
        	this.compressThresholdByte = compressThresholdByte;
        }
        
        
        public int write(ByteBuffer[] dataToWrite, IWriteCompletionHandler completionHandler) throws IOException {

        	int length = 0;
        	if (dataToWrite != null) {
        		length = HttpUtils.computeRemaining(dataToWrite);
        		sizePlainData += length;
        	}
        	
        	switch (mode) {

        	case INITIAL:
                if ((getHeader().containsHeader("Content-Encoding") && getHeader().getHeader("Content-Encoding").equalsIgnoreCase("gzip")) || (compressThresholdByte > 0) && (HttpUtils.computeRemaining(dataToWrite) > compressThresholdByte)) {
                    mode = COMPRESSING;
                    getHeader().setHeader("Content-Encoding", "gzip");
                } else {
                    mode = PLAIN;
                }
                return write(dataToWrite, completionHandler);  // recursive call
                
            case COMPRESSING:
                if (completionHandler != null) {
                    completionHandler = new CompletionHandlerAdapter(completionHandler, length);
                }

                if (gos == null) {
                    if (length > 1024) {
                        bos = new BufferOutputStream(length);
                    } else {
                        bos = new BufferOutputStream(1024);
                    }
                    gos = new GZIPOutputStream(bos);
                }

                byte[] plain = DataConverter.toBytes(dataToWrite);    
                gos.write(plain);
                gos.flush();

                ByteBuffer buffer = bos.drainBuffer();
                return writeCompressedToNetwork(buffer, completionHandler);
                
            default:   // PLAIN
                sizeNetworkData += length;
                return onWriteNetworkData(dataToWrite, completionHandler);
            }
        }
        
        
        
        private int writeCompressedToNetwork(ByteBuffer buffer, IWriteCompletionHandler completionHandler) throws IOException {

        	if (buffer == null) {
        		if (completionHandler != null) {
        			completionHandler.onWritten(0);
        		}
        		return 0;
	            
        	} else {
                sizeNetworkData += buffer.remaining();

	            if (completionHandler == null) {  
	            	buffer = HttpUtils.copy(buffer);  
	            } 
	            return onWriteNetworkData(new ByteBuffer[] { buffer }, completionHandler);
        	}
        }
        
        
        public void close() throws IOException {
        	if (mode == COMPRESSING) {
        		if (LOG.isLoggable(Level.FINE)) {
        			LOG.fine("closing gzip stream");
        		}
                gos.close();
                
                ByteBuffer buffer = bos.drainBuffer();
                writeCompressedToNetwork(buffer, null);
        	}
        	
        	if (LOG.isLoggable(Level.FINE)) {
        		int ratio = 100 - ((sizeNetworkData * 100) / sizePlainData);
        		LOG.fine(sizeNetworkData + " data written to network (plain size " + sizePlainData + ", compression=" + ratio + "%)");
        	}
        }
    }
  
    
    private static final class CompletionHandlerAdapter implements IWriteCompletionHandler {
    	
    	private final int size;
    	private final IWriteCompletionHandler completionHandler;
    	
    	
    	public CompletionHandlerAdapter(IWriteCompletionHandler completionHandler, int size) {
    		this.completionHandler = completionHandler; 
    		this.size = size;
		}
    	
    	public void onWritten(int written) throws IOException {
    		try {
    			completionHandler.onWritten(size);
    		} catch (IOException ioe) {
    			if (LOG.isLoggable(Level.FINE)) {
    				LOG.fine("error occured by calling onWritten on " + completionHandler + " " + ioe.toString());
    			}
    		}
    	}
    	
    	public void onException(IOException ioe) {
    		completionHandler.onException(ioe);
    	}
    }

    
    
    
    private static final class BufferOutputStream extends OutputStream {

        private final int bufferSize;
        private boolean isSafeUse = false;
        private byte[] buffer;
        private int pos;
        
        public BufferOutputStream(int bufferSize) {
        	this.bufferSize = bufferSize;
        	
            buffer = new byte[bufferSize];
            pos = 0;
        }
        
        public ByteBuffer drainBuffer() {
        	if (pos > 0) {
        		ByteBuffer buf = DataConverter.toByteBuffer(buffer, 0, pos);
        		pos = 0;
        
        		if (!isSafeUse) {
        			buffer = new byte[bufferSize];
        		}
        		return buf;
        		
        	} else {
        		return null;
        	}
        }
        
        
        @Override
        public void write(int b) throws IOException {
            write(new byte[] { (byte) b });  // should never be called
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            append(b, off, len);
        }
        
        @Override
        public void write(byte[] b) throws IOException {
            append(b);
        }

        private void append(byte[] b) {
            append(b, 0, b.length);
        }
        
        private void append(byte[] b, int off, int len) {
            int remainingSize = buffer.length - pos; 
            if (remainingSize < len) {
                incBuffer(len - remainingSize);
            }
            
            System.arraycopy(b, off, buffer, pos, len);
            pos += len;
        }
        
        private void incBuffer(int incSize) {
            byte[] newBuffer = new byte[buffer.length + incSize];
            System.arraycopy(buffer, 0, newBuffer, 0, pos);
            buffer = newBuffer;
        }
    }    
}
	    
