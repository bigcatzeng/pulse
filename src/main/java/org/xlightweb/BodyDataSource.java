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
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Closeable;
import java.net.SocketTimeoutException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xsocket.DataConverter;
import org.xsocket.IDataSource;
import org.xsocket.MaxReadSizeExceededException;



/**
 * 
 * I/O resource capable of providing body data in a blocking way. Read operations will be suspended, 
 * if not enough data is available.
 * 
 * The BBodyDataSource wraps a {@link NonBlockingBodyDataSource}
 *   
 * @author grro@xlightweb.org
 *
 */
public class BodyDataSource implements IDataSource, ReadableByteChannel, Closeable {
    
    private static final Logger LOG = Logger.getLogger(BodyDataSource.class.getName());
    
    public static final int DEFAULT_RECEIVE_TIMEOUT = Integer.MAX_VALUE;


    // listeners
    private final Listener handler = new Listener();
    private final Object readGuard = new Object();
    
    // flags
    private final AtomicInteger notifyRevision = new AtomicInteger();
    private final AtomicBoolean isComplete = new AtomicBoolean(false);
    private final AtomicBoolean isDestroyed = new AtomicBoolean(false);

    
    private final NonBlockingBodyDataSource delegate;
    private int receiveTimeoutSec = DEFAULT_RECEIVE_TIMEOUT;

    // part support
    private PartHandler partHandler = null;
    
    
    
    
    
    /**
     * constructor 
     * 
     * @param delegate the underlying non NonBlockingBodyDataSource
     */
    BodyDataSource(NonBlockingBodyDataSource delegate) throws IOException {
        this.delegate = delegate;
        
        setReceiveTimeoutSec(HttpUtils.convertMillisToSec(delegate.getBodyDataReceiveTimeoutMillisSilence()));
        
        delegate.setDataHandler(handler);
        delegate.addCompleteListener(handler);
        delegate.addDestroyListener(handler);
    }
    
    
    IHeader getHeader() {
        return delegate.getHeader();
    }
    
    String getEncoding() {
        return delegate.getEncoding();
    }
   
    
    /**
     * returns the underlying tcp connection
     * @return the underlying tcp connection
     */
    NonBlockingBodyDataSource getUnderliyingBodyDataSource() {
        return delegate;
    }


    
    /**
     * sets the receive time out by reading data  
     *   
     * @param timeout  the receive timeout
     */
    public void setReceiveTimeoutSec(int timeout)  {
        this.receiveTimeoutSec = timeout;
    }


    /**
     * gets receive time out by reading data  
     * @return the receive timeout
     */
    public int getReceiveTimeoutSec()  {
        return receiveTimeoutSec;
    }   
    


    /**
     * returns, if the connection is open. 
     *
     * @return true if the connection is open
     */
    public boolean isOpen() {
        return delegate.isOpen();
    }
        

    /**
     * return true if the body is a mulipart 
     * @return true, if the body is a mulipart
     */
    public final boolean isMultipart() {
        return delegate.isMultipart(); 
    }
    
    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        delegate.close();
    }

  
    
    /**
     * get the body size 
     * 
     * @return  the body size
     * @throws IOException if an exception occurs
     */
    public int size() throws IOException {
        return new SizeReadTask().read();
    }

    private final class SizeReadTask extends ReadTask<Integer> {
        
        Integer doRead() throws IOException ,SocketTimeoutException ,MaxReadSizeExceededException {
            throwBufferUnderflowExceptionIfNotComplete();
            
            return delegate.available(); 
        }       
    }

    
    
    /**
     * Marks the read position in the connection. Subsequent calls to resetToReadMark() will attempt
     * to reposition the connection to this point.
     *
     */
    public void markReadPosition() {
       delegate.markReadPosition();
    }


    /**
     * Resets to the marked read position. If the connection has been marked,
     * then attempt to reposition it at the mark.
     *
     * @return true, if reset was successful
     */
    public boolean resetToReadMark() {
        return delegate.resetToReadMark();
    }


    
    /**
     * remove the read mark
     */
    public void removeReadMark() {
        delegate.removeReadMark();
    }
  
    
    /**
     * read the next part of the mulipart body. {@link BodyDataSource#isMultipart()} can
     *  be used to verify if the body is a multipart one
     * 
     * <pre>
     *  // ...
     *   
     *  BlockingBodyDataSource body = response.getBlockingBody();
     *  if (body.isMultipart()) {
     *      IPart part = body.readPart();
     *      // ...
     *  } else {
     *      // ...
     *  }
     * </pre>
     * 
     * @return the next part
     * @throws IOException if an exception occurs
     * @throws NoMultipartTypeException if the body type is not a multipart type 
     */
    public IPart readPart() throws NoMultipartTypeException, IOException {
        initPartReader();
        return new PartReadTask().read();
    }
    
    
    private final class PartReadTask extends ReadTask<IPart> {
        
        IPart doRead() throws IOException ,SocketTimeoutException, MaxReadSizeExceededException {
            return delegate.readPart();
        }       
    }

    

    /**
     * return all parts of the multipart body. {@link BodyDataSource#isMultipart()} can
     *  be used to verify if the body is a multipart one
     * 
     * <pre>
     *  // ...
     *   
     *  BlockingBodyDataSource body = response.getBlockingBody();
     *  if (body.isMultipart()) {
     *      List<IPart> parts = body.readParts();
     *      // ...
     *  } else {
     *      // ...
     *  }
     * </pre>
     * 
     * @return the list of all parts
     * @throws IOException if an exception occurs
     * @throws NoMultipartTypeException if the surrounding body type is not a multipart type 
     */
    public List<IPart> readParts() throws NoMultipartTypeException, IOException {
        List<IPart> parts = new ArrayList<IPart>();
        
        while (true) {
            try {
                parts.add(readPart());
            } catch (ClosedChannelException cce) {
                return parts;
            }
        } 
    }
    
  
    private synchronized void initPartReader() throws IOException {
        if (partHandler == null) {
            partHandler = new PartHandler();
            delegate.setBodyPartHandler(partHandler);
        } 
    }
  
    
     
    private final class PartHandler implements IPartHandler, IUnsynchronized {
        
        public PartHandler() {
        }
    
        public void onPart(NonBlockingBodyDataSource dataSource) throws IOException, BadMessageException {
            onData();
        }
    }

      
   
    
    /**
     * read the body 
     * 
     * @return the body as byte buffer
     *  
     * @throws IOException if an exception occurs
     */
    public ByteBuffer[] readByteBuffer() throws IOException {
        return new ByteBuffersReadTask().read();
    }
    
    private final class ByteBuffersReadTask extends ReadTask<ByteBuffer[]> {
        
        ByteBuffer[] doRead() throws IOException ,SocketTimeoutException ,MaxReadSizeExceededException {
            throwBufferUnderflowExceptionIfNotComplete();
            
            return readByteBufferByLength(delegate.available()); 
        }       
    }


    
        
    /**
     * read the body 
     * 
     * @return the body as bytes
     *  
     * @throws IOException if an exception occurs
     */
    public byte[] readBytes() throws IOException {
        return new BytesReadTask().read();
    }

    private final class BytesReadTask extends ReadTask<byte[]> {
        
        byte[] doRead() throws IOException ,SocketTimeoutException ,MaxReadSizeExceededException {
            throwBufferUnderflowExceptionIfNotComplete();
            
            return readBytesByLength(delegate.available()); 
        }       
    }

    
    
    /**
     * read the body 
     * 
     * @return the body as string
     *  
     * @throws IOException if an exception occurs
     */
    public String readString() throws IOException {
        return readString(delegate.getEncoding());
    }


    
    /**
     * read the body 
     * 
     * @param encoding  the encoding
     * @return the body as string
     *  
     * @throws IOException if an exception occurs
     */
    public String readString(String encoding) throws IOException {
        return new StringReadTask(encoding).read();
    }   
    
    private final class StringReadTask extends ReadTask<String> {
        private String encoding;
        
        public StringReadTask(String encoding) {
            this.encoding = encoding;
        }
        
        String doRead() throws IOException ,SocketTimeoutException ,MaxReadSizeExceededException {
            throwBufferUnderflowExceptionIfNotComplete();
            
            delegate.removeLeadingBOM();
            return readStringByLength(delegate.available(), encoding); 
        }       
    }

        
    
    /**
     * {@inheritDoc}.
     */
    public int read(ByteBuffer buffer) throws IOException {
        int size = buffer.remaining();
        if (size < 1) {
            return 0;
        }

        return  new ByteBufferReadTask(buffer).read();
    }
    
    
    private final class ByteBufferReadTask extends ReadTask<Integer> {
        
        private final ByteBuffer buffer;
        
        public ByteBufferReadTask(ByteBuffer buffer) {
            this.buffer = buffer;
        }
        
        Integer doRead() throws IOException, SocketTimeoutException, MaxReadSizeExceededException {
            
            try {
                available(1); // ensure that at minimum the required length is available
                return delegate.read(buffer);
                
            } catch (ClosedChannelException cce) {
                return -1;
            }
        }    
        
        @Override
        Integer doNotOpen() throws ClosedChannelException {
            return -1;
        }
    }
            
    
    
    
    /**
     * {@inheritDoc}
     */
    public byte readByte() throws IOException, BufferUnderflowException, SocketTimeoutException {
        return new ByteReadTask().read();
    }

    private final class ByteReadTask extends ReadTask<Byte> {
        Byte doRead() throws IOException, SocketTimeoutException, MaxReadSizeExceededException {
            return delegate.readByte();
        }       
    }



    /**
     * {@inheritDoc}
     */
    public short readShort() throws IOException, BufferUnderflowException, SocketTimeoutException {
        return new ShortReadTask().read();
    }
    
    private final class ShortReadTask extends ReadTask<Short> {
        Short doRead() throws IOException, SocketTimeoutException, MaxReadSizeExceededException {
            return delegate.readShort();
        }       
    }

    
    
    /**
     * {@inheritDoc}
     */
    public int readInt() throws IOException, BufferUnderflowException, SocketTimeoutException {
        return new IntegerReadTask().read();
    }
    
    private final class IntegerReadTask extends ReadTask<Integer> {
        Integer doRead() throws IOException, SocketTimeoutException, MaxReadSizeExceededException {
            return delegate.readInt();
        }       
    }

    
    
    /**
     * {@inheritDoc}
     */
    public long readLong() throws IOException, BufferUnderflowException, SocketTimeoutException {
        return new LongReadTask().read();
    }

    private final class LongReadTask extends ReadTask<Long> {
        Long doRead() throws IOException, SocketTimeoutException, MaxReadSizeExceededException {
            return delegate.readLong();
        }       
    }

    
    /**
     * {@inheritDoc}
     */
    public double readDouble() throws IOException, BufferUnderflowException, SocketTimeoutException {
        return new DoubleReadTask().read();
    }
    
    private final class DoubleReadTask extends ReadTask<Double> {
        Double doRead() throws IOException, SocketTimeoutException, MaxReadSizeExceededException {
            return delegate.readDouble();
        }       
    }

    
    
    /**
     * {@inheritDoc}
     */
    public ByteBuffer[] readByteBufferByDelimiter(String delimiter) throws IOException, BufferUnderflowException, SocketTimeoutException {
        return readByteBufferByDelimiter(delimiter, Integer.MAX_VALUE);
    }
    
    

    /**
     * {@inheritDoc}
     */
    public ByteBuffer[] readByteBufferByDelimiter(String delimiter, int maxLength) throws IOException, BufferUnderflowException, MaxReadSizeExceededException, SocketTimeoutException {
        return new ByteBufferByDelimiterReadTask(delimiter, maxLength).read();
    }
    
    
    private final class ByteBufferByDelimiterReadTask extends ReadTask<ByteBuffer[]> {
        private final String delimiter;
        private final int maxLength;
        
        ByteBufferByDelimiterReadTask(String delimiter, int maxLength) {
            this.delimiter = delimiter;
            this.maxLength = maxLength;
        }
        
        ByteBuffer[] doRead() throws IOException ,SocketTimeoutException ,MaxReadSizeExceededException {
            return delegate.readByteBufferByDelimiter(delimiter, maxLength);
        }       
    }
        

    
    /**
     * {@inheritDoc}
     */
    public ByteBuffer[] readByteBufferByLength(int length) throws IOException, BufferUnderflowException, SocketTimeoutException {
        if (length <= 0) {
            return new ByteBuffer[0];
        }

        return new ByteBufferByLengthReadTask(length).read();
    }

    
    private final class ByteBufferByLengthReadTask extends ReadTask<ByteBuffer[]> {
        private final int length;
        
        ByteBufferByLengthReadTask(int length) {
            this.length = length;
        }
        
        ByteBuffer[] doRead() throws IOException ,SocketTimeoutException ,MaxReadSizeExceededException {
            return delegate.readByteBufferByLength(length);
        }       
    }
    
    
    /**
     * {@inheritDoc}
     */
    public byte[] readBytesByDelimiter(String delimiter) throws IOException, BufferUnderflowException, SocketTimeoutException {
        return readBytesByDelimiter(delimiter, Integer.MAX_VALUE);
    }
    
    /**
     * {@inheritDoc}
     */
    public byte[] readBytesByDelimiter(String delimiter, int maxLength) throws IOException, BufferUnderflowException, MaxReadSizeExceededException, SocketTimeoutException {
        return DataConverter.toBytes(readByteBufferByDelimiter(delimiter, maxLength));
    }
    
    
    /**
     * {@inheritDoc}
     */
    public byte[] readBytesByLength(int length) throws IOException, BufferUnderflowException, SocketTimeoutException {
        return DataConverter.toBytes(readByteBufferByLength(length));
    }
    
    /**
     * {@inheritDoc}
     */
    public String readStringByDelimiter(String delimiter) throws IOException, BufferUnderflowException, UnsupportedEncodingException, SocketTimeoutException {
        return readStringByDelimiter(delimiter, delegate.getEncoding());
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
     * @throws SocketTimeoutException if a timeout occurs
     */
    public String readStringByDelimiter(String delimiter, String encoding) throws IOException, BufferUnderflowException, UnsupportedEncodingException, SocketTimeoutException {
        delegate.removeLeadingBOM();
        return DataConverter.toString(readByteBufferByDelimiter(delimiter), encoding);
    }
    
    /**
     * {@inheritDoc}
     */
    public String readStringByDelimiter(String delimiter, int maxLength) throws IOException, BufferUnderflowException, UnsupportedEncodingException, MaxReadSizeExceededException, SocketTimeoutException {
        return readStringByDelimiter(delimiter, maxLength, delegate.getEncoding());
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
     * @throws SocketTimeoutException if the timout is reached
     */
    public String readStringByDelimiter(String delimiter, int maxLength, String encoding) throws IOException, BufferUnderflowException, UnsupportedEncodingException, MaxReadSizeExceededException, SocketTimeoutException {
        delegate.removeLeadingBOM();
        return DataConverter.toString(readByteBufferByDelimiter(delimiter, maxLength), encoding);
    }
    
    /**
     * {@inheritDoc}
     */
    public String readStringByLength(int length) throws IOException, BufferUnderflowException, UnsupportedEncodingException, SocketTimeoutException {
        return readStringByLength(length, delegate.getEncoding());
    }
    
    

    
    /**
     * read a string by using a length definition
     * 
     * @param length    the amount of bytes to read
     * @param encoding  the encoding
     * @return the string
     * @throws IOException If some other I/O error occurs
     * @throws SocketTimeoutException if the timeout is reached 
     * @throws BufferUnderflowException if not enough data is available
     */
    public String readStringByLength(int length, String encoding) throws IOException, BufferUnderflowException, UnsupportedEncodingException, SocketTimeoutException {
        delegate.removeLeadingBOM();
        return DataConverter.toString(readByteBufferByLength(length), encoding);
    }
    
    
    /**
     * {@inheritDoc}
     */
    public long transferTo(WritableByteChannel target, int length) throws IOException, BufferUnderflowException, SocketTimeoutException {
        long written = 0;
        
        ByteBuffer[] buffers = readByteBufferByLength(length);
        for (ByteBuffer buffer : buffers) {
            written += target.write(buffer);
        }
        
        return written;
    }
        
    

    /**
     * transfers the content to the given channel
     * 
     * @param target                the target channel
     * @return the number of transfered bytes
     * @return the number of transfered bytes
     * @throws ClosedChannelException If either this channel or the target channel is closed
     * @throws IOException If some other I/O error occurs 
     */
    public long transferTo(WritableByteChannel target) throws IOException, BufferUnderflowException, SocketTimeoutException {
        
        long written = 0;
        
        while (true) {
            long w = new TransferToTask(target).read();
            if (w == -1) {
                return written;
            } else {
                written += w;
            }
        }
    }
    
    
    private final class TransferToTask extends ReadTask<Long> {
        
        private final WritableByteChannel target;
        
        public TransferToTask(WritableByteChannel target) {
            this.target = target;
        }
        
        Long doRead() throws IOException, SocketTimeoutException, MaxReadSizeExceededException {

            try {
                int available = available(1); // ensure that at minimum one byte is available
                return transferTo(target, available);
            } catch (ClosedChannelException cce) {
                return -1L;
            }          
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
     * @throws BufferUnderflowException if not enough data is available
     */
    public long transferTo(BodyDataSink dataSink) throws ProtocolException, IOException, ClosedChannelException,BufferUnderflowException {
        return transferTo(dataSink, size());
    }
    
    
    /**
     * transfer the available data of the this source channel to the given data sink
     * 
     * @param dataSink   the data sink
     * 
     * @return the number of transfered bytes
     * @throws ClosedChannelException If either this channel or the target channel is closed
     * @throws IOException If some other I/O error occurs
     * @throws BufferUnderflowException if not enough data is available
     */
    public long transferTo(OutputStream dataSink) throws ProtocolException, IOException, ClosedChannelException,BufferUnderflowException {
        return transferTo(Channels.newChannel(dataSink));
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
     * @throws BufferUnderflowException if not enough data is available
     */ 
    public long transferTo(BodyDataSink dataSink, int length) throws ProtocolException, IOException, ClosedChannelException,BufferUnderflowException {
        long written = 0;
        do {
            written += new TransferToByLengthTask(dataSink, (int) (length - written)).read();
        } while (written < length);
        
        return written;
    }
    
    
    private final class TransferToByLengthTask extends ReadTask<Long> {
        
        private final BodyDataSink dataSink;
        private final int length;
        
        public TransferToByLengthTask(BodyDataSink dataSink, int length) {
            this.dataSink = dataSink;
            this.length = length;
        }
        
        Long doRead() throws IOException, SocketTimeoutException, MaxReadSizeExceededException {
            available(length); // ensure that at minimum the required length is available 
            return delegate.transferTo(dataSink, length);
        }       
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        try {
            return new ToStringReadTask().read();
        } catch (Exception e) {
            return "error occured by performing toString: " + e.toString(); 
        }
    }
    
    private final class ToStringReadTask extends ReadTask<String> {
            
        String doRead() throws IOException ,SocketTimeoutException ,MaxReadSizeExceededException {
            throwBufferUnderflowExceptionIfNotComplete();
            return delegate.toString();
        }
    }
    
    
    
    /**
     * returns this {@link ReadableByteChannel} as {@link InputStream} 
     * @return the input stream
     */
    public InputStream toInputStream() {
        return Channels.newInputStream(this);
    }
    
    
    /**
     * returns this {@link ReadableByteChannel} as {@link Reader} 
     * @return the input stream
     */
    public Reader toReader() {
        return Channels.newReader(this, getEncoding());
    }


 

    private void onData() { 
        if (LOG.isLoggable(Level.FINE)) {
            try {
                LOG.fine("notifying waiting threads isCompleteReceived=" + delegate.isCompleteReceived() +
                         " available=" + delegate.size() + 
                         " moreDataExpected=" + delegate.isMoreInputDataExpected() +
                         " (guard: " + readGuard + ")");
            } catch (IOException ioe) {
                LOG.fine("logging error occured " + ioe.toString());
            }
        }
        
        synchronized (readGuard) {
            notifyRevision.incrementAndGet();
            readGuard.notifyAll();
        }
    }

    
    private void onComplete() {
        synchronized (readGuard) {
            notifyRevision.incrementAndGet();
            isComplete.set(true);
            
            readGuard.notifyAll();
        }
    }
    
    
    private void onDestroy() {
        synchronized (readGuard) {
            notifyRevision.incrementAndGet();
            isDestroyed.set(true);
            
            readGuard.notifyAll();
        }
    }
    
    
    
    private final class Listener implements IBodyDataHandler, IBodyCompleteListener, IBodyDestroyListener, IUnsynchronized {

        public boolean onData(NonBlockingBodyDataSource bodyDataSource) {
            BodyDataSource.this.onData();
            return true;
        }
        
        public void onComplete() throws IOException {
            BodyDataSource.this.onComplete();
        }
        
        public void onDestroyed() throws IOException {
            BodyDataSource.this.onDestroy();
        }
    }
    
    
    private abstract class ReadTask<T extends Object> {
        
        final T read() throws IOException, SocketTimeoutException, MaxReadSizeExceededException, ClosedChannelException {
            long start = System.currentTimeMillis();
            long remainingTime = receiveTimeoutSec;
            
            do {
                int revision = notifyRevision.get();
                
                try {
                    
                    try {
                        return doRead();
                    } catch (RevisionAwareBufferUnderflowException nce) {
                        synchronized (readGuard) {
                            if (nce.getRevision() != notifyRevision.get()) {
                                continue;
                            } else { 
                                throw new BufferUnderflowException();  // "jump" into catch (BufferUnderflowException)
                            }
                        }
                    }
                        
                } catch (BufferUnderflowException bue) {
                    
                    synchronized (readGuard) {
                        
                        // is notification event occurred meanwhile, the loop will be continued
                        if (revision != notifyRevision.get()) {
                            continue;
                        }

                        // no more data expected? (connection is destroyed)
                        if (isDestroyed.get()) {
                            return doNotOpen();
                                
                        } else {
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.fine("waiting for more reveived data (guard: " + readGuard + ")");
                            }
                                
                            try {
                                readGuard.wait(remainingTime);
                            } catch (InterruptedException ie) { 
                                // Restore the interrupted status
                                Thread.currentThread().interrupt();
                            }
                        } 
                    }
                }

                remainingTime = HttpUtils.computeRemainingTime(start, receiveTimeoutSec);
            } while (remainingTime > 0);
        
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("receive timeout " + receiveTimeoutSec + " sec reached. throwing timeout exception");
            }
            
            throw new ReceiveTimeoutException(((long) receiveTimeoutSec) * 1000);          
        }

        protected final void throwBufferUnderflowExceptionIfNotComplete() throws RevisionAwareBufferUnderflowException { 
            synchronized (readGuard) {
                if (!isComplete.get()) {
                    throw new RevisionAwareBufferUnderflowException(notifyRevision.get());
                }
            }
        }

        protected int available(int required) throws IOException, BufferUnderflowException, ClosedChannelException {
            
            synchronized (readGuard) {
                int available = delegate.available();
                
                if (available == -1 ){
                    throwBufferUnderflowExceptionIfNotComplete();
                    throw new ClosedChannelException();

                } else if (available < required) {
                     throw new BufferUnderflowException();

                }  else {
                    return available;
                }
            }            
        }
        
        
        T doNotOpen() throws IOException, ClosedChannelException, ProtocolException {
            IOException ioe = delegate.getException();
            if (ioe == null) {
                throw new ProtocolException("connection destroyed ", null);
            } else {
                throw ioe;
            }
        }
                
        
        abstract T doRead() throws IOException, SocketTimeoutException, MaxReadSizeExceededException;
    }
    
    
    
    private static final class RevisionAwareBufferUnderflowException extends BufferUnderflowException {
        private static final long serialVersionUID = 3183778067682558356L;
        
        private final int revision;
        
        public RevisionAwareBufferUnderflowException(int revision) {
            this.revision = revision;
        }
        
        public int getRevision() {
            return revision;
        }
    }

}
