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

import java.io.File;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;



import org.xlightweb.AbstractHttpConnection.IMultimodeExecutor;
import org.xsocket.DataConverter;
import org.xsocket.Execution;
import org.xsocket.connection.IWriteCompletionHandler;
import org.xsocket.connection.IConnection.FlushMode;




/**
 * file  body data source
 * 
 * @author grro@xlightweb.org
 *
 */
final class FileDataSource extends NonBlockingBodyDataSource implements IForwardable {

    private final File file;
    private final long length;
    private final String range;
    
    FileDataSource(IHeader header, IMultimodeExecutor executor, File file) throws IOException {
    	this(header, executor, file, null);
    }
    
    FileDataSource(IHeader header, IMultimodeExecutor executor, File file, String range) throws IOException {
        super(header, executor);
        
        this.file = file;
        this.range = range;
        length = file.length();
    }
    
    long getLength() {
        return length;
    }

    public void forwardTo(final BodyDataSink bodyDataSink) throws IOException {
    	forwardTo(bodyDataSink, null);
    }
 
    
    public void forwardTo(BodyDataSink bodyDataSink, IBodyCompleteListener completeListener) throws IOException {
        FileSender sendFile = new FileSender(bodyDataSink.getId(), bodyDataSink, file, range, completeListener);
        sendFile.run();
    }
   
    


    
    File getFile() {
        return file;
    }
   
    @Override
    boolean isForwardable() {
        return true;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isNetworkendpoint() {
        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    String getId() {
        return Integer.toString(hashCode());
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    boolean suspend() throws IOException {
        return false;
    }


    
    /**
     * {@inheritDoc}
     */
    @Override
    boolean resume() throws IOException {
        return false;
    }
  

    /**
     * {@inheritDoc}
     */
    @Override
    void onClose() {
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    void onDestroy(String reason) {
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            
            byte[] data = new byte[(int) getLength()];
            int read = is.read(data);
            sb.append(DataConverter.toString(DataConverter.toByteBuffer(data, 0, read), getEncoding()));
            
        } catch (IOException ignore) { 
        	
        } finally {
        	if (is != null) {
        		try {
        			is.close();
        		} catch (IOException ignore) { }
        	}
        }
        
        return sb.toString();
    }
    
    
    @Execution(Execution.MULTITHREADED)  // necessary to avoid deep stacks 
    static class FileSender implements Runnable, IWriteCompletionHandler {
        
        private static final Logger LOG = Logger.getLogger(FileSender.class.getName());

        static final int TRANSFER_BYTE_BUFFER_MAX_MAP_SIZE = Integer.parseInt(System.getProperty("org.xsocket.connection.transfer.mappedbytebuffer.maxsize", "65536"));
        
        private final String id;
        private final File file; 
        private final RandomAccessFile raf;
        private final FileChannel fc;
        private final BodyDataSink dataSink;
        private final IBodyCompleteListener completeListener;
        
        private boolean isOpen = true;
        
        private long length = 0;
        
        private final AtomicLong remaining = new AtomicLong(0);
        private final AtomicLong offset = new AtomicLong(0);
        
        public FileSender(String id, BodyDataSink dataSink, File file, String range, IBodyCompleteListener completeListener) throws IOException {
            
            if (!file.exists()) {
                throw new IOException("file " + file.getAbsolutePath() + " does not exist");
            }
            
            this.id = id;
            this.file = file;
            this.completeListener = completeListener;
            this.raf = new RandomAccessFile(file, "r");
                
            fc = raf.getChannel();
            this.dataSink = dataSink;           
            dataSink.setFlushmode(FlushMode.ASYNC);
            
            if (range != null) {
            	int[] positions = HttpUtils.computeFromRangePosition(range, (int) file.length());
            	
            	offset.set(positions[0]);
            	remaining.set((positions[1] - positions[0]) + 1);
            } else {
            	offset.set(0);
            	remaining.set(file.length());
            }
        }
        
        final String getAbsolutePath() {
            return file.getAbsolutePath();
        }
        
        public final void run() {
            try {
                write();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
            
        public final void onWritten(int written) throws IOException {
            
            if (LOG.isLoggable(Level.FINE)) {
                if (remaining.get() > 0) {
                    LOG.fine("[" + id + "] data (size=" + written + " bytes) has been written. Writing next chunk");
                } else {
                    LOG.fine("[" + id + "] data (size=" + written + " bytes) has been written.");
                }
            }
            
            write();
        }
        
        
        private void write() throws IOException {
            
            // remaining data to write?
            if (remaining.get() > 0) {
                
                // limit the buffer allocation size 
                if (remaining.get() > TRANSFER_BYTE_BUFFER_MAX_MAP_SIZE) {
                    length = TRANSFER_BYTE_BUFFER_MAX_MAP_SIZE;
                } else {
                    length = remaining.get();
                }
                
                MappedByteBuffer buffer = fc.map(MapMode.READ_ONLY, offset.get(), length);
                ByteBuffer[] bufs = new ByteBuffer[] { buffer };

                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + id + "] writing data (size=" + length + " bytes)");
                }               

                // set the pointers before writing -> write callback occurs within call
                offset.addAndGet(length);
                remaining.set(remaining.get() - length);
                
                doWrite(bufs);
                

            // no, closing dataSink
            } else {
                if (isOpen) {
                    isOpen = false;
                    closeFile();
                    if (completeListener == null) {
        			    dataSink.close();
                    } else {
                    	completeListener.onComplete();
                    }
                }
            } 
        }
    
        void doWrite(ByteBuffer[] bufs) throws IOException {
        	dataSink.write(bufs, this);
        }

        
        public final void onException(IOException ioe) {
            if (isOpen) {
                isOpen = false;
                closeFile();
            }
            dataSink.destroy();
        }
        
        
        void closeFile() {
            try {
                fc.close();
                raf.close();
            } catch (IOException ioe) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("error occured by closing file channel " + getAbsolutePath() + " " + ioe.toString());
                }
            }
        }    
    }
}