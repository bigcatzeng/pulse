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
import java.io.InputStream;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.xsocket.DataConverter;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IConnection.FlushMode;



/**
 * data source network based base implementation
 *  
 * @author grro@xlightweb.org
 */
abstract class AbstractNetworkBodyDataSource extends NonBlockingBodyDataSource {
    


	private static final Logger LOG = Logger.getLogger(AbstractNetworkBodyDataSource.class.getName());

	static final String UNCOMPRESSED_KEY = "X-XLightweb-Uncompressed";
	static final String AUTOUNCOPMRESSED_ATTR_KEY = "org.xlightweb.autouncopmressed";

	
	private static final int COMPRESS_BUFFER_SIZE = 512;

    private final AbstractHttpConnection httpConnection;
    
    
    private final HttpMessageHeader header;
    

    private static final boolean DEFAULT_IS_AUTODETECTEDING_ENCODING = Boolean.parseBoolean(System.getProperty("org.xlightweb.autodetectedingEncoding", "true")); 
    private boolean isDetectEncoding = !DEFAULT_IS_AUTODETECTEDING_ENCODING; 
    private final AtomicReference<Runnable> autoEncodingCallbackRef = new AtomicReference<Runnable>(null);
    private byte[] encodingBuffer = null;

    private final AtomicBoolean isConnected = new AtomicBoolean(true);

    
    
    // suspend guard
    private final Object suspendGuard = new Object();
    private boolean isSuspended = false;
    
    
    // uncompress support
    private final BufferInputStream bis;
    private GZIPInputStream gis;
    private boolean dataFinished = false;

    
    public AbstractNetworkBodyDataSource(HttpMessageHeader header, AbstractHttpConnection httpConnection) throws IOException {
        super(header, httpConnection.getExecutor());
        this.header = header;
        this.httpConnection = httpConnection;

        if ((header.getContentType()) != null && HttpUtils.isTextMimeType(header.getContentType()) && HttpUtils.parseEncoding(header.getContentType()) == null) {
            isDetectEncoding = true;
        } else {
            isDetectEncoding = false;
        }
        
        
        if (httpConnection.isAutoUncompress() && 
        	HttpUtils.isGzipEncoded(header)) {
        	
        	bis = new BufferInputStream();
        	isDetectEncoding = false;
        	
        } else {
        	bis = null;
        	gis = null;
        }
    }
    
    
    final void postCreate() {
    	if (bis != null) {
	    	header.removeHeader("Content-Length");
	    	header.removeHeader("Content-Encoding");
	    	
	    	header.setProtocolVersionSilence("1.1");
	    	header.setHeader("Transfer-Encoding", "chunked");
	    	header.addHeader(UNCOMPRESSED_KEY, "true (auto uncompress)");
	    	header.setAttribute(AUTOUNCOPMRESSED_ATTR_KEY, true);
    	}
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isNetworkendpoint() {
        return true;
    }
    
  
    void forwardTo(final BodyDataSink bodyDataSink) throws IOException {
        if (bodyDataSink.isNetworkendpoint()) {
            bodyDataSink.setFlushmode(FlushMode.ASYNC);
        }
      
        super.forwardTo(bodyDataSink);
    } 

  
    private void setDetectEncoding(boolean isDetectEncoding) {
        
        synchronized (autoEncodingCallbackRef) {
            this.isDetectEncoding = isDetectEncoding;
            
            if (isDetectEncoding == false) {
                Runnable task = autoEncodingCallbackRef.getAndSet(null);
                if (task != null) {
                    task.run();
                }
            }
        }
    }
    
    final void registerAutoEncondingDetectCallback(Runnable task) {
        synchronized (autoEncodingCallbackRef) {
            if (!isDetectEncoding) {
                task.run();
            } else {
                autoEncodingCallbackRef.set(task);
            }
        }
    }
    
    
    protected final AbstractHttpConnection getHttpConnection() {
        return httpConnection;
    }
   
    protected final String getId() {
        return httpConnection.getId();
    }
    
    
    protected final void setNonPersistent() {
        httpConnection.setPersistent(false);
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    final boolean suspend() throws IOException {
        
        synchronized (suspendGuard) {

            // has already suspended?
            if (isSuspended) {
                return false;
                
            // ... no
            } else {
                isSuspended = true;

                
                Runnable suspendTask = new Runnable() {
                    
                    public void run() {
                        
                        synchronized (suspendGuard) {
                            
                            // assert that suspend flag is not reset meanwhile
                            if (isSuspended) {
                                try {
                                    httpConnection.suspendReceiving();
                                } catch (IOException ioe) {
                                    if (LOG.isLoggable(Level.FINE)) {
                                        LOG.fine("[" + getId() + "] error occured by calling suspendReceiving " + ioe);
                                    }
                                }
                            }
                        }
                    }
                };
                httpConnection.getExecutor().processNonthreaded(suspendTask);
                
                return true;
            }
        }
    }

    

    
    /**
     * {@inheritDoc}
     */
    @Override
    boolean resume() throws IOException {

        // is suspended?
        synchronized (suspendGuard) {

            if (isSuspended) {
                isSuspended = false;
                
                Runnable resumeTask = new Runnable() {
                    
                    public void run() {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("[" + getId() + "] resume receiving data");
                        }
                        
                        try {
                            httpConnection.resumeReceiving();
                        } catch (IOException ioe) {
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.fine("[" + getId() + "] error occured by calling suspendReceiving " + ioe);
                            }
                        }

                        callBodyDataHandler(true);
                    }               
                };
                httpConnection.getExecutor().processNonthreaded(resumeTask);
                return true;
                
            } else {
                return false;
            }            
        }
    }
    

    final void onDisconnect() throws IOException {
        setDetectEncoding(false);
        
        if (isConnected.getAndSet(false)) {            
            try {
                performOnDisconnect();
            } catch (ProtocolException pe) {
                setException(pe);
                throw pe;
            }
        }
    }

    final long getLastTimeDataReceivedMillis() {
        return httpConnection.getLastTimeDataReceivedMillis();
    }
    
    abstract void performOnDisconnect() throws IOException;
    
    
    @Override
    void onDestroy(String reason) {
        setDetectEncoding(false);
        httpConnection.destroy(reason);
    }


    
    final void setComplete() throws IOException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("[" + getId() +  "] completed reveived");
        }
        
        dataFinished = true;
        onReadNetworkData((ByteBuffer) null);
        
        setDetectEncoding(false);

        super.setComplete();
        httpConnection.onMessageCompleteReceived(header);
        
        
    }
    

    
    final int readByteBufferByLength(ByteBuffer[] buffers, int length) throws IOException {

        if (buffers == null) {
            return 0;
        }
        
        
        if (isDetectEncoding) {
            byte[] data = DataConverter.toBytes(HttpUtils.copy(buffers));
            if (encodingBuffer != null) {
                byte[] newData = new byte[encodingBuffer.length + data.length];
                System.arraycopy(encodingBuffer, 0, newData, 0, encodingBuffer.length);
                System.arraycopy(data, 0, newData, encodingBuffer.length, data.length);
                data = newData; 
            }
            
            try {
                String encoding = HttpUtils.detectEncoding(data);
                if (encoding != null) {
                    setEncoding(encoding);
                    header.setCharacterEncoding(encoding);
                }
                encodingBuffer = null;
                setDetectEncoding(false);
            } catch (BufferUnderflowException bue) {
                encodingBuffer = data;
            }
        }
        
        
        int remaining = length;
        
        for (int i = 0; i < buffers.length; i++) {
            ByteBuffer buffer = buffers[i];
            if (buffer == null) {
                continue;
            }
            
            int available = buffer.remaining();
                
            if (available == 0) {
                continue;
            }
            
                // not enough data
            if (available < remaining) {
                onReadNetworkData(buffer);
                buffers[i] = null;
                remaining -= available;
                continue;
                
            } else if (available == remaining) {
            	onReadNetworkData(buffer);
                buffers[i] = null;
                return 0;
    
            // available > remaining 
            } else {
                int limit = buffer.limit();
                buffer.limit(buffer.position() + remaining);
                ByteBuffer buf = buffer.slice();
                onReadNetworkData(buf);
                    
                buffer.position(buffer.limit());
                buffer.limit(limit);
                
                return 0;
            }
        }
        
        return remaining;
    }

    
    protected final void onReadNetworkData(ByteBuffer buffer) throws IOException {
  
    	if (bis == null) {
    		append(buffer);
    		
    	} else {
    		bis.addBuffer(buffer);

    		// gis will be initialized lazy -> requires GZIP header network data 
    		if (gis == null) {

    			bis.mark();
    	    	try {
    	    		gis = new GZIPInputStream(bis, COMPRESS_BUFFER_SIZE);
    	    	} catch (BufferUnderflowException bue) {
    				bis.reset();
    				return;
    	    	}
    		}
			
    		
			while ((bis.available() > (COMPRESS_BUFFER_SIZE * 2)) || dataFinished) {
				byte[] data = new byte[COMPRESS_BUFFER_SIZE];  
				
	    		int read = gis.read(data);
				if (read > 0) {
					ByteBuffer buf = DataConverter.toByteBuffer(data, 0, read);
					append(buf);
					
				} else if (read == -1) {
					return;
				}
			}
    	}
    }

    

    
    protected final void onReadNetworkData(ByteBuffer[] buffers) throws IOException {
    	
    	if (bis == null) {
    		append(buffers);
    		
    	} else {
    		for (ByteBuffer buffer : buffers) {
    			onReadNetworkData(buffer);
    		}
    	}
    }
    
    
    /**
     * parses the the body   
     *  
     * @param rawData   the raw read network data buffer 
     * @return true, if the body is complete
     * @throws IOException  if an exception occurs 
     * @throws BufferUnderflowException if more data is required
     * @throws MaxReadSizeExceededException if the max read size is exceeded
     */
    final void parse(ByteBuffer[] rawData) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {          
        
        if (!isComplete()) {
            try {
                doParse(rawData);
                
            } catch (BufferUnderflowException bue) {
                throw bue;
                    
            } catch (IOException ioe) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + httpConnection.getId() + "] (protocol?) error occured by reading body " + ioe.toString());
                }
    
                if (!isComplete()) {
                    setException(ioe);
                }
                throw ioe;
            }
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("do not parse, because body is already complete");
            }
        }
    }

 
    /**
     * parse the body 
     * @param rawData       the raw read network data buffer
     * @throws IOException if an exception occurs
     * @return true, if body is complete  
     */
    abstract void doParse(ByteBuffer[] rawData) throws IOException;

    
    void onException(IOException ioe, ByteBuffer[] rawData) {
        setException(ioe); 
    }
    
    
    private final class BufferInputStream extends InputStream {
        
    	private ByteBuffer buffer;
    	private int markedPos;
    	private int markedLimit; 
    	
    	public void addBuffer(ByteBuffer buf) {
    		buffer = HttpUtils.merge(buffer, buf);
    	}
    	
    	public void mark() {
    		mark(Integer.MAX_VALUE);
    	}
    	
    	
    	@Override
    	public void mark(int readlimit) {
    		markedPos = buffer.position();
    		markedLimit = buffer.limit();
    	}
    	
    	@Override
    	public void reset() throws IOException {
    		buffer.position(markedPos);
    		buffer.limit(markedLimit);
    	}
        
    	@Override
    	public int available() throws IOException {
    		return buffer.remaining();
    	}
        
        @Override
        public int read(byte[] b) throws IOException {
        	return read(b, 0, b.length);
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
        	int remaining = buffer.remaining();
        	
        	if (remaining == 0) {
        		throw new BufferUnderflowException();
        	} 
        	
        	if (len > remaining) {
        		len = remaining;
        	}
        	
        	buffer.get(b, off, len);
        	
			if(!dataFinished && (gis != null) && (buffer.remaining() < COMPRESS_BUFFER_SIZE)) {
				// should not occur!!
				throw new IOException("Buffer underflow (remaining " + buffer.remaining() + ")"); 
			}

        	return remaining - buffer.remaining();
        }
        
        
		@Override
		public int read() throws IOException {
			return buffer.get() & 0xff;
		}
		
		@Override
		public String toString() {
			return DataConverter.toHexString(HttpUtils.copy(buffer));
		}
    }        
}