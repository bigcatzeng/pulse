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
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xlightweb.HttpUtils.HttpConnectionHandlerInfo;
import org.xlightweb.HttpUtils.RequestHandlerInfo;
import org.xlightweb.HttpUtils.ResponseHandlerInfo;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServerConnection;
import org.xsocket.DataConverter;
import org.xsocket.Execution;
import org.xsocket.IDestroyable;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.IConnection;
import org.xsocket.connection.IConnectionTimeoutHandler;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.IDisconnectHandler;
import org.xsocket.connection.IHandler;
import org.xsocket.connection.IHandlerChangeListener;
import org.xsocket.connection.IIdleTimeoutHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IWriteCompletionHandler;
import org.xsocket.connection.NonBlockingConnectionPool;




/**
 * 
 * Implementation base for the {@link HttpClientConnection} and {@link HttpServerConnection} class
 * 
 * <br/><br/><b>This is a xSocket internal class and subject to change</b> 
 * 
 * @author grro@xlightweb.org
 */
public abstract class AbstractHttpConnection implements IHttpConnection, IDestroyable {
    

    private static final Logger LOG = Logger.getLogger(AbstractHttpConnection.class.getName());

    
    // max write buffer size
    private static final String DEFAULT_MAX_WRITE_BUFFER_SIZE = "65536";
    private static final int MAX_WRITEBUFFER_SIZE = Integer.parseInt(System.getProperty("org.xlightweb.connection.max_write_buffer_size", DEFAULT_MAX_WRITE_BUFFER_SIZE));
    
        
    // default encoding
    private String defaultEncoding = IHttpMessage.DEFAULT_ENCODING;

    // dummy response handler
    private static final DoNothingResponseHandler DO_NOTHING_RESPONSE_HANDLER = new DoNothingResponseHandler();

    
    // underlying tcp connection
    private String closeReason = null;
    private final INonBlockingConnection tcpConnection;

    
    // http connection properties
    private final AtomicBoolean isPersistentRef = new AtomicBoolean(true);

    
    // life cycle management 
    private final AtomicBoolean isClosing = new AtomicBoolean(false);
    private final AtomicReference<AbstractNetworkBodyDataSink> networkBodyDataSinkRef = new AtomicReference<AbstractNetworkBodyDataSink>();

        
    // attachment management
    private AtomicReference<Object> attachmentRef = new AtomicReference<Object>(null);
    

    // protocol handling
    private final DataHandler dataHandler = new DataHandler();
    private final AbstractHttpProtocolHandler protocolHandler;
    
    
    // connection life cycle handler support 
    private final Set<IHttpConnectionHandler> connectionHandlers = Collections.synchronizedSet(new HashSet<IHttpConnectionHandler>());
    private final AtomicBoolean isDisconnectedRef = new AtomicBoolean(false);
    
    
    // call back processing
    private final IMultimodeExecutor multimodeExcutor;


    // timeout support
    private long bodyDataReceiveTimeoutMillis = Long.MAX_VALUE;
    private long lastTimeDataWritten = System.currentTimeMillis();
    private long lastTimeHeaderReceivedMillis = System.currentTimeMillis();
    private long lastTimeMessageTailReceivedMillis = System.currentTimeMillis();
    private long lastTimeDataReceivedMillis = System.currentTimeMillis();
    

    // auto uncompress
    boolean isAutoUncompress = false;
    

    // open transactions
    private final AtomicInteger openTransactions = new AtomicInteger(0);

    // statistics
    private final AtomicInteger countReceivedMessages = new AtomicInteger(0);
    private final AtomicInteger countSentMessages = new AtomicInteger(0);
    private int countSendBytes = 0;
    private final AtomicInteger countReceivedBytes = new AtomicInteger(0);

    
    /**
     * constructor 
     * 
     * @param nativeConnection        the underlying tcp connection
     * @param isClientSideConnection  true, if http connection is client-side connection
     * 
     * @throws IOException if an exception occurs
     */
    protected AbstractHttpConnection(INonBlockingConnection tcpConnection, boolean isClientSideConnection) throws IOException {
        this.tcpConnection = tcpConnection;
        multimodeExcutor = HttpUtils.newMultimodeExecutor(getWorkerpool());
        
        if (isClientSideConnection) {
            protocolHandler = new HttpProtocolHandlerClientSide();
            
        } else {
            protocolHandler = new HttpProtocolHandlerServerSide();
        }       
        
        tcpConnection.setFlushmode(FlushMode.ASYNC);
        tcpConnection.setAutoflush(false);
        tcpConnection.setAttachment(this);
        tcpConnection.setOption(IConnection.TCP_NODELAY, true);
    }
    
    
    /**
     * initialize this connection
     * @throws IOException  if an exception occurs
     */
    protected final void init() throws IOException {
        tcpConnection.setHandler(dataHandler);
        onConnect();
    }

    
    final boolean isClosing() {
        return isClosing.get();
    }
    

    /**
     * increments the the number of retrieved messages 
     * @return the number of the received messages after incrementing it
     */
    protected final int incCountMessageReceived() {
        return countReceivedMessages.incrementAndGet();
    }
        
    
    /**
     * returns the number of the received messages  
     * @return the number of the received messages
     */
    protected int getCountMessagesReceived() {
        return countReceivedMessages.get();
    }
    
    

    /**
     * returns the number of the received bytes
     * @return the number of the received bytes
     */
    protected int getCountReceivedBytes() {
        return countReceivedBytes.get();
    }
    
    private void incReveived(int size) {
        countReceivedBytes.addAndGet(size);
        
        protocolHandler.incReveived(size);
    }
    
    
    /**
     * increments the the number of sent messages 
     * @return the number of the sent messages after incrementing it
     */
    protected final int incCountMessageSent() {
        return countSentMessages.incrementAndGet();
    }
    

    /**
     * returns the number of the sent messages  
     * @return the number of the sent messages
     */
    protected int getCountMessagesSent() {
        return countSentMessages.get();
    }
    
    
    protected final int getNumOpenTransactions() {
        return openTransactions.get();
    }
    
    /**
     * sets the time when header or body data has been received
     * @param time the time when header or body data has been received
     */
    protected final void setLastTimeDataReceivedMillis(long time) {
        lastTimeDataReceivedMillis = time;
    }

    
    /**
     * returns the time when header or body data has been received
     * @return the time when header or body data has been received
     */
    protected long getLastTimeDataReceivedMillis() {
        return lastTimeDataReceivedMillis;
    }
    
    
    /**
     * sets the time when body< data has been received  
     * @param time  the time when body< data has been received
     */
    protected final void setLastTimeMessageTailReceivedMillis(long time) {
        lastTimeMessageTailReceivedMillis = time;
    }
    
    
    /**
     * gets the time when body data has been received 
     * @return the time when body data has been received
     */
    protected final long getLastTimeMessageTailReceivedMillis() {
        return lastTimeMessageTailReceivedMillis;
    }

    
    /**
     * sets the time when the (complete) header has been received    
     * @param time the time when the (complete) header has been received
     */
    protected final void setLastTimeHeaderReceivedMillis(long time) {
        lastTimeHeaderReceivedMillis = time;
    }
    
    
    /**
     * gets the time when the (complete) header has been received 
     * @return the time when the (complete) header has been received
     */
    protected final long getLastTimeHeaderReceivedMillis() {
        return lastTimeHeaderReceivedMillis;
    }
    
    

    /**
     * gets the last time data has been written
     * @return the last time data has been written
     */
    protected final long getLastTimeWritten() {
        return lastTimeDataWritten;
    }
    

    protected final IMultimodeExecutor getExecutor() {
        return multimodeExcutor;
    }
    

   

    /**
     * returns the underlying http connection 
     * @return the underlying http connection 
     */
    public INonBlockingConnection getUnderlyingTcpConnection() {
        return tcpConnection;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public final long getBodyDataReceiveTimeoutMillis() {
        return bodyDataReceiveTimeoutMillis;
    }   

    
    final String getCloseReason() {
        return closeReason;
    }
    
        
    /**
     * {@inheritDoc}
     */
    public final void addConnectionHandler(IHttpConnectionHandler connectionHandler) {
        if (connectionHandler == null) {
            throw new NullPointerException("conection handler has to be set");
        }
        
        connectionHandlers.add(connectionHandler);
        if (isDisconnectedRef.get()) {
            callOnDisconnect(connectionHandler, HttpUtils.getHttpConnectionHandlerInfo(connectionHandler));
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public final void removeConnectionHandler(IHttpConnectionHandler connectionHandler) {
        connectionHandlers.remove(connectionHandler);
    }
    
    
    /**
     * {@inheritDoc}
     */
    public final void setBodyDataReceiveTimeoutMillis(long bodyDataReceiveTimeoutMillis) {
        this.bodyDataReceiveTimeoutMillis = bodyDataReceiveTimeoutMillis;
    }


    /**
     * {@inheritDoc}
     */
    public final void setAttachment(Object attachment) {
        attachmentRef.set(attachment);
        
    }
    

    /**
     * {@inheritDoc}
     */
    public final Object getAttachment() {
        return attachmentRef.get();
    }

    
    /**
     * {@inheritDoc}
     */
    public final long getConnectionTimeoutMillis() {
        return tcpConnection.getConnectionTimeoutMillis();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public final void setConnectionTimeoutMillis(long timeoutMillis) {
        tcpConnection.setConnectionTimeoutMillis(timeoutMillis);
    }
    
    
    /**
     * {@inheritDoc}
     */
    public final long getRemainingMillisToConnectionTimeout() {
        return tcpConnection.getRemainingMillisToConnectionTimeout();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public final long getIdleTimeoutMillis() {
        return tcpConnection.getIdleTimeoutMillis();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public final void setIdleTimeoutMillis(long timeoutInMillis) {
        tcpConnection.setIdleTimeoutMillis(timeoutInMillis);
    }

    
    /**
     * {@inheritDoc}
     */
    public final long getRemainingMillisToIdleTimeout() {
        return tcpConnection.getRemainingMillisToIdleTimeout();
    }
    
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    public final Map<String, Class> getOptions() {
        return tcpConnection.getOptions();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public final Object getOption(String name) throws IOException {
        return tcpConnection.getOption(name);
    }

    
    /**
     * {@inheritDoc}
     */
    public final void setOption(String name, Object value) throws IOException {
        tcpConnection.setOption(name, value);
    }
    

    /**
     * {@inheritDoc}
     */
    public final InetAddress getLocalAddress() {
        return tcpConnection.getLocalAddress();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public final int getLocalPort() {
        return tcpConnection.getLocalPort();
    }
    
    /**
     * {@inheritDoc}
     */
    public final InetAddress getRemoteAddress() {
        return tcpConnection.getRemoteAddress();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public final int getRemotePort() {
        return tcpConnection.getRemotePort();
    }
    


    /**
     * see {@link INonBlockingConnection#setWriteTransferRate(int)} 
     */
    public final void setWriteTransferRate(int bytesPerSecond) throws ClosedChannelException, IOException {
        tcpConnection.setWriteTransferRate(bytesPerSecond);
    }
    
    

    /**
     * see {@link INonBlockingConnection#getFlushmode(FlushMode)} 
     */
    final FlushMode getFlushmode() {
        return tcpConnection.getFlushmode();
    }
    
    
    /**
     * see {@link INonBlockingConnection#getWorkerpool()} 
     */
    public final Executor getWorkerpool() {
        return tcpConnection.getWorkerpool();
    }
    
    
    
    /**
     * sets the worker pool 
     * @param workerpool  the worker pool
     */
    public void setWorkerpool(Executor workerpool) {
        tcpConnection.setWorkerpool(workerpool);
    }

    
    
    /**
     * see {@link INonBlockingConnection#activateSecuredMode()} 
     */
    public void activateSecuredMode() throws IOException {
        tcpConnection.activateSecuredMode();
    }
    
    
    /**
     * see {@link INonBlockingConnection#isSecure()} 
     */
    public final boolean isSecure() {
        return tcpConnection.isSecure();
    }
    
    
    /**
     * checks if the connection is persistent
     * 
     * @return true, if the connection is persistent
     */
    public final boolean isPersistent() {
        return isPersistentRef.get();
    }
    
    
    /**
     * set if the connection is persistent 
     * 
     * @param persistent true, if the connection is persistent
     */
    final protected void setPersistent(boolean persistent) {
        isPersistentRef.set(persistent);
    }

    
    public final void setAutoUncompress(boolean isAutoUncompress) {
    	this.isAutoUncompress = isAutoUncompress;
    }

    public final boolean isAutoUncompress() {
    	return isAutoUncompress;
    }


    /**
     * set the body default encoding. According to RFC 2616 the 
     * initial value is ISO-8859-1 
     *   
     * @param encoding  the defaultEncoding 
     */
    protected void setBodyDefaultEncoding(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }

    /**
     * return the body default encoding
     * @return the body default encoding
     */
    String getBodyDefaultEncoding() {
        return defaultEncoding;
    }
    
    /**
     * {@inheritDoc}
     */
    public final String getId() {
        try {
            return tcpConnection.getId();
        } catch (Throwable t) {
            return "<unknown>";
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public final boolean isOpen() {
        return (!isDisconnectedRef.get() && tcpConnection.isOpen());
    }
    
    
    protected void onMessageCompleteReceived(IHttpMessageHeader header) {
        openTransactions.decrementAndGet();
    }
  

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("[" + getId() + "] closing");
        }

        try {
            AbstractNetworkBodyDataSink messageWriter = networkBodyDataSinkRef.get();
            if (messageWriter != null) {
                messageWriter.close();
            }           
            
            releaseResources();
            
            if (isReuseable()) {
                tcpConnection.close();

            // destroy (underlying tcp) connection by using connection pool. The underlying connection could be a pooled one)
            // The connection pool detects automatically if the connection is pooled or not. The connection will be 
            // closed (logically) anyway
            } else {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + getId() + "] http connection is not reusable (isPersistent=" + isPersistentRef.get() + "). destroying it");
                }
                NonBlockingConnectionPool.destroy(tcpConnection);
            }
        } catch (Exception e) {
            // eat and log exception
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("error occured by closing htttp connection " + getId() + " " + DataConverter.toString(e));
            }
        }           
    }
    

    /**
     * {@inheritDoc}
     */
    public final void closeQuitly() {
        try {
            close();
        } catch (IOException ioe) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] error occured by closing connection " + getId() + " " + ioe.toString());
            }
            
            try {
                NonBlockingConnectionPool.destroy(tcpConnection);
            } catch (IOException e) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + getId() + "] error occured by closing connection " + getId() + " " + e.toString());
                }
            }
        }
    }

    
    private void releaseResources() {       
        AbstractNetworkBodyDataSink messageWriter = networkBodyDataSinkRef.get();
        if (messageWriter != null) {
            networkBodyDataSinkRef.set(null);
            messageWriter.onDisconnect();
        }
    }


    protected boolean isReuseable() {
        return isPersistentRef.get() && !isReceivingSuspended(); 
    }
    
    
    /**
     * destroy the connection
     */
     public final void destroy() {
        destroy(0);
    }
  
     
     /**
      * destroy the connection
      */
     protected final void destroy(int delayMillis) {
         destroy("user initiated", delayMillis);
     }

      
     /**
      * destroy the connection
      * 
      * @param reason       the reason
      */
     protected final void destroy(String reason) {
         destroy(reason, 0);
     }    

    
    /**
     * destroy the connection
     * 
     * @param reason       the reason
     * @param delayMillis  the close delay
     */
    protected final void destroy(String reason, int delayMillis) {
        if (closeReason != null) {
            closeReason = "destroyed - " + reason;
        }

        setPersistent(false);
        isClosing.set(true);

        
        if (delayMillis > 0) {
            schedule(new DelayedCloser(), delayMillis);
            
        } else {
            performDestroy();
        }
    }

    private final class DelayedCloser extends TimerTask {
        public void run() {
            performDestroy();
        }
    }
    
    
    private void performDestroy() {
        releaseResources();
        
        // destroy (underlying tcp) connection by using connection pool. The underlying connection could be a pooled one)
        // The connection pool detects automatically if the connection is pooled or not. The connection will be 
        // closed (logically) anyway
        try {
            NonBlockingConnectionPool.destroy(tcpConnection);
        } catch (IOException ioe) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] error occured by destroying htttp connection " + getId() + " " + ioe.toString());
            }
        }        
    }
    

    /**
     * {@inheritDoc}
     */
    final public void suspendReceiving() throws IOException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("[" + getId() + "] suspend receving");
        }
        tcpConnection.suspendReceiving();
    }


    
    /**
     * {@inheritDoc}
     */
    final public boolean isReceivingSuspended() {
        return tcpConnection.isReceivingSuspended();
    }
    
    
    /**
     * {@inheritDoc}
     */
    final public void resumeReceiving() throws IOException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("[" + getId() + "] resume receving");
        }

        tcpConnection.resumeReceiving();
    }
    
    
    final protected void setNetworkBodyDataSinkIgnoreWriteError() {
        AbstractNetworkBodyDataSink ds = networkBodyDataSinkRef.get();
        if (ds != null) {
            ds.setIgnoreWriteError();
        }
    }

    
    final void setNetworkBodyDataSink(AbstractNetworkBodyDataSink networkBodyDataSink) {
        networkBodyDataSinkRef.set(networkBodyDataSink);
    }
    
    
    final boolean removeNetworkBodyDataSink(AbstractNetworkBodyDataSink networkBodyDataSink) {
        AbstractNetworkBodyDataSink oldNetworkBodyDataSink = networkBodyDataSinkRef.get(); 
        if ((oldNetworkBodyDataSink != null) && (oldNetworkBodyDataSink == networkBodyDataSink)) {
            networkBodyDataSinkRef.set(networkBodyDataSink);
            return true;
        }
        
        return false;
    }
    
    
    final void flush() throws IOException {
        tcpConnection.flush();
    }
    
    final int write(String txt) throws IOException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("[" + getId() + "] TCP write: " + txt);
        }
            
        lastTimeDataWritten = System.currentTimeMillis();
        
        int size = (int) tcpConnection.write(txt);
        
        lastTimeDataWritten = System.currentTimeMillis();
        countSendBytes += size;
            
        return size;
    }

    
    final int write(byte[] bytes, IWriteCompletionHandler completionHandler) throws IOException {
            
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("[" + getId() + "] TCP write: " + new String(bytes));
        }
            
        lastTimeDataWritten = System.currentTimeMillis();
        
        int size = 0;
        if (completionHandler != null) {
            ByteBuffer[] buffer = new ByteBuffer[] { ByteBuffer.wrap(bytes) }; 
            size = bytes.length; 
            tcpConnection.write(buffer, completionHandler);
            
        } else {
            size = (int) tcpConnection.write(bytes);
        }
        
        lastTimeDataWritten = System.currentTimeMillis();
        countSendBytes += size;
            
        return size;
    }

    
    final long write(ByteBuffer[] buffer, IWriteCompletionHandler completionHandler) throws IOException {
        
        if (LOG.isLoggable(Level.FINE)) {
            ByteBuffer[] bufs = new ByteBuffer[buffer.length];
            for (int i = 0; i < buffer.length; i++) {
                bufs[i] = buffer[i].duplicate();
            }
            
            LOG.fine("[" + getId() + "] TCP write: " + DataConverter.toString(bufs));
        }

        
        
        int size = 0;
        if (completionHandler != null) {
            for (ByteBuffer byteBuffer : buffer) {
                size += byteBuffer.remaining();
            }
            tcpConnection.write(buffer, completionHandler);
            
        } else {
            size = (int) tcpConnection.write(buffer);
        }
        
        lastTimeDataWritten = System.currentTimeMillis();
        countSendBytes += size;
        
        return size;
    }
    
    
    final long write(ByteBuffer[] buffer) throws IOException {
           
        if (LOG.isLoggable(Level.FINE)) {
            ByteBuffer[] bufs = new ByteBuffer[buffer.length];
            for (int i = 0; i < buffer.length; i++) {
                bufs[i] = buffer[i].duplicate();
            }
            
            LOG.fine("[" + getId() + "] TCP write: " + DataConverter.toString(bufs));
        }

        int size = (int) tcpConnection.write(buffer);
        
        lastTimeDataWritten = System.currentTimeMillis();
        countSendBytes += size;
        
        return size;
    }
   
   
    
    protected abstract IMessageHeaderHandler getMessageHeaderHandler();
    
    
    protected void onProtocolException(Exception ex) {
        destroy(ex.toString());
    }
    
    
    protected void onMessageWritten() {
        
    }

    
    protected void onConnect() throws IOException {
        
        for (final IHttpConnectionHandler connectionHandler : connectionHandlers) {
            HttpConnectionHandlerInfo connectionHandlerInfo = HttpUtils.getHttpConnectionHandlerInfo(connectionHandler);
            
            if (connectionHandlerInfo.isConnectHandler()) {
                Runnable task = new Runnable() {
                    
                    public void run() {
                        try {
                            ((IHttpConnectHandler) connectionHandler).onConnect(AbstractHttpConnection.this);
                        } catch (IOException ioe) {
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.fine("[" + getId() + "] error occured by performing onConnect on " + connectionHandler + " reason: " + ioe.toString());
                            }
                            destroy();
                        }
                    }
                };
                
                
                if (connectionHandlerInfo.isConnectHandlerMultithreaded()) {
                    multimodeExcutor.processMultithreaded(task);
                } else {
                    multimodeExcutor.processNonthreaded(task);
                }
            }
        }
    }

    
    
    protected void onDisconnect() {
        
        try {
            
            if (!isDisconnectedRef.getAndSet(true)) {
                for (final IHttpConnectionHandler connectionHandler : connectionHandlers) {
                    HttpConnectionHandlerInfo connectionHandlerInfo = HttpUtils.getHttpConnectionHandlerInfo(connectionHandler);
                        
                    if (connectionHandlerInfo.isDisconnectHandler()) {
                        callOnDisconnect(connectionHandler, connectionHandlerInfo);
                    }
                }
                connectionHandlers.clear();
                
                releaseResources();
            }
            
        } catch (Throwable t) {
            // log and eat exception
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] error occured by closing http connection " + DataConverter.toString(t));
            }
        }
    }
    
    
    

    
    private void callOnDisconnect(final IHttpConnectionHandler connectionHandler, HttpConnectionHandlerInfo connectionHandlerInfo) {
        Runnable task = new Runnable() {
                
            public void run() {
                try {
                    ((IHttpDisconnectHandler) connectionHandler).onDisconnect(AbstractHttpConnection.this);
                } catch (IOException ioe) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + getId() + "] error occured by performing ondisconnect on " + connectionHandler + " reason: " + ioe.toString());
                    }
                    destroy();
                }
            }
        };
            
        if (connectionHandlerInfo.isDisconnectHandlerMultithreaded()) {
            multimodeExcutor.processMultithreaded(task);
        } else {
            multimodeExcutor.processNonthreaded(task);
        }
    }
    
    
    protected void onConnectionTimeout() {
        closeQuitly();
    }
    
    
    protected void onIdleTimeout() {
        closeQuitly();
    }

    
    
    
    protected final BodyDataSink writeMessage(int autocompressThresholdBytes, IHttpMessageHeader header) throws IOException {
        lastTimeDataWritten = System.currentTimeMillis();
        openTransactions.incrementAndGet();
        
        BodyDataSink ds = internalWriteMessage(header);
        ds.setAutocompressThreshold(autocompressThresholdBytes);
        
        return ds;
    }
    
    
    private BodyDataSink internalWriteMessage(IHttpMessageHeader header) throws IOException {
        
        BodyDataSink dataSink = null;
        
        // web socket handeshake?
        if ((header.getHeader("Upgrade") != null) && (header.getHeader("Upgrade").equalsIgnoreCase("WebSocket"))) {
            
            // request?
            if ((header.getHeader("Sec-WebSocket-Key1") != null)) {
                dataSink = internalWriteMessage(header, 8);
                
            // ... no, response                
            }  else {
                dataSink = internalWriteMessage(header, 16);
            }
            
        //  transfer encoding header is set with chunked -> chunked body
        } else if (isChunkedTransferEncoding(header)) {
            dataSink = new FullMessageChunkedBodyDataSink(this, header);
            
        // contains a non-zero Content-Length header  -> bound body
        } else if (header.getContentLength() != -1) {
           dataSink = internalWriteMessage(header, header.getContentLength());
           
        // is connection header set with close?  -> SimpleMessage           
        } else if ((header.getHeader("Connection") != null) && (header.getHeader("Connection").equalsIgnoreCase("close"))) {
            setPersistent(false);
            dataSink = new SimpleMessageBodyDataSink(this, header);
            
        // is content-type set?            
        } else if (header.getContentType() != null) {
            setPersistent(false);
            dataSink = new SimpleMessageBodyDataSink(this, header);
            
        // return PseudoBody            
        } else {
            dataSink = new BodylessBodyDataSink(this, header);
        }
        
        return dataSink;
    }
    
    
    
    
    
    
    
    protected final BodyDataSink writeMessage(IHttpMessageHeader header, int length) throws IOException {
        lastTimeDataWritten = System.currentTimeMillis();
        openTransactions.incrementAndGet();
        
        return internalWriteMessage(header, length);
    }

    
    private BodyDataSink internalWriteMessage(IHttpMessageHeader header, int length) throws IOException {
        // contains a non-zero Content-Length header  -> bound body 
        if (length > 0) {
            return new FullMessageBodyDataSink(this, header, length);
                
        } else {
            header.removeHeader("Content-Type");

            // return PseudoBody
            return new BodylessBodyDataSink(this, header);
        }
    }

    
    protected final BodyDataSink writeMessage(int autocompressThreshold, IHttpMessage message) throws IOException {
        lastTimeDataWritten = System.currentTimeMillis();
        openTransactions.incrementAndGet();
        
        return internalWriteMessage(autocompressThreshold, message);
    }
    
    
    protected final BodyDataSink writeMessageSilence(IHttpMessage message) throws IOException {
        lastTimeDataWritten = System.currentTimeMillis();
        return internalWriteMessage(Integer.MAX_VALUE, message);
    }
        
    
    
    private BodyDataSink internalWriteMessage(int autocompressThreshold, IHttpMessage message) throws IOException {
        if (message.hasBody()) {
            return writeMessageWithBody(autocompressThreshold, message);
            
        } else {
            return writeMessageWithoutBody(message);
        }
    }
    
    
    private BodyDataSink writeMessageWithoutBody(IHttpMessage message) throws IOException {
        write(message.getMessageHeader().toString() + "\r\n");
        flush();
        
        return null;
    }
    
    
    private BodyDataSink writeMessageWithBody(int autocompressThreshold, IHttpMessage message) throws IOException {

    	BodyDataSink bodyDataSink = internalWriteMessage(message.getMessageHeader());

    	bodyDataSink.setAutocompressThreshold(autocompressThreshold);
        
        NonBlockingBodyDataSource bodyDataSource = message.getNonBlockingBody();

        // is body complete?
        if (bodyDataSource.isComplete()) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] message body to sent is complete. writing all data to body data sink");
            }
            
            bodyDataSink.setFlushmode(FlushMode.ASYNC);
            bodyDataSink.setAutoflush(false);  // close will flush the data sink
                    
            int available = bodyDataSource.available();
            if (available > 0) {
                bodyDataSink.write(bodyDataSource.readByteBufferByLength(available));
            }
            
            bodyDataSink.close();   
        
        // .. no
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] message body to sent is not complete. creating forwarder");
            }
            
            bodyDataSink.setAutoflush(true);
            bodyDataSink.setFlushmode(FlushMode.ASYNC);
            bodyDataSource.forwardTo(bodyDataSink);
        }
        
        return bodyDataSink;
    }
    
    

    static boolean isChunkedTransferEncoding(IHttpMessageHeader header) {
        
        String transferEncoding = header.getTransferEncoding();
        if ((transferEncoding != null) && (transferEncoding.equalsIgnoreCase("chunked"))) {
            return true;
        }
        
        return false;
    }

    protected static boolean isNetworkendpoint(NonBlockingBodyDataSource dataSource) {
        return dataSource.isNetworkendpoint();
    }
    
    protected static int getDataReceived(NonBlockingBodyDataSource dataSource) {
        return dataSource.getDataReceived();
    }
    
    protected static void setDataHandlerSilence(NonBlockingBodyDataSource dataSource, IBodyDataHandler dataHandler) {
        dataSource.setDataHandlerSilence(dataHandler);
    }
    
    protected static IBodyDataHandler getDataHandlerSilence(NonBlockingBodyDataSource dataSource) {
        return dataSource.getDataHandlerSilence();
    }

    
    protected static int availableSilence(NonBlockingBodyDataSource dataSource) throws IOException {
        return dataSource.availableSilence();
    }
    
    protected static ByteBuffer[] readByteBufferByLengthSilence(NonBlockingBodyDataSource dataSource, int length) throws IOException {
        return dataSource.readByteBufferByLengthSilence(length);
    }


    /**
     * returns true if the requests will accept a chunked body 
     * @param request the request 
     * @return true if the requests will accept a chunked body
     */
    protected static boolean isAcceptingChunkedResponseBody(IHttpRequest request) {
        
        String protocolVersion = request.getProtocolVersion();
        
        if (protocolVersion.equals("1.1")) {
            return true;
            
        } else {
            int idx = protocolVersion.indexOf(".");
            
            int minor = Integer.parseInt(protocolVersion.substring(idx + 1, protocolVersion.length()));
            if (minor > 0) {
                return true;
            }
            
            int major = Integer.parseInt(protocolVersion.substring(0, idx));
            if (major > 1) {
                return true;
            }
        }
        return false;
    }
    
    
    /**
     * schedule a time task 
     * 
     * @param task    the timer task
     * @param delay   the delay 
     * @param period   the period
     */
    protected static void schedule(TimerTask task, long delay, long period) {
        HttpUtils.schedule(task, delay, period);
    }
    
    
    /**
     * schedule a time task 
     * 
     * @param task    the timer task
     * @param delay   the delay 
     */
    protected static void schedule(TimerTask task, long delay) {
        HttpUtils.schedule(task, delay);
    }
    
    protected static void setAutocompressThreshold(BodyDataSink dataSink, int autocompressThreshold) {
    	dataSink.setAutocompressThreshold(autocompressThreshold);
    }

    
    protected static IMultimodeExecutor newMultimodeExecutor(Executor workerpool) {
        return HttpUtils.newMultimodeExecutor(workerpool);
    }

    protected static String parseEncoding(String contentType) {
    	return HttpUtils.parseEncoding(contentType);
    }

    /*
    protected static BodyDataSink newInMemoryBodyDataSink(String id, IHttpMessageHeader header, IMultimodeExecutor executor) throws IOException {
        return new InMemoryBodyDataSink(id, header, executor);
    }*/

    protected static BodyDataSink newInMemoryBodyDataSink(String id, IHttpMessageHeader header) throws IOException {
        return new InMemoryBodyDataSink(id, header);
    }

    
    protected static NonBlockingBodyDataSource getDataSourceOfInMemoryBodyDataSink(BodyDataSink dataSink)  {
        return ((InMemoryBodyDataSink) dataSink).getDataSource();
    }
    
    protected static boolean isComplete(NonBlockingBodyDataSource body) {
        return body.isComplete();
    }
    
    
    protected static boolean isForwardable(NonBlockingBodyDataSource dataSource) {
        return dataSource.isForwardable();
    }

    
    protected static void forwardBody(NonBlockingBodyDataSource dataSource, BodyDataSink bodyDataSink) throws IOException {
        ((IForwardable) dataSource).forwardTo(bodyDataSink);
    }
    
    protected static void forward(NonBlockingBodyDataSource dataSource, BodyDataSink dataSink) throws IOException {
        dataSource.forwardTo(dataSink);
    }
    
    protected static void addConnectionAttribute(IHttpResponseHeader header, IHttpConnection con) {
        HttpUtils.addConnectionAttribute(header, con);
    }

    
    protected static int getSizeDataReceived(NonBlockingBodyDataSource body) {
        return body.getSizeDataReceived();
    }
    
    protected static int getSizeWritten(BodyDataSink body) {
        return body.getSizeWritten();
    }
    
    protected static boolean isEmpty(ByteBuffer[] data) {
        return HttpUtils.isEmpty(data);
    }
    
    protected static ClosedChannelException newDetailedClosedChannelException(String reason) {
        return new DetailedClosedChannelException(reason);
    }
    

    protected static final IHttpResponseHandler newDoNothingResponseHandler() {
        return DO_NOTHING_RESPONSE_HANDLER;
    }
        
    protected static boolean isContentTypeSupportsCharset(String contentType) {
        return HttpUtils.isContentTypeSupportsCharset(contentType);
    }
    
    protected static boolean isSupports100Contine(IHttpResponseHandler handler) {
        return HttpUtils.getResponseHandlerInfo(handler).isContinueHandler();
    }
    
    
    protected static IHeader getReceviedHeader(ProtocolException pe) {
        return pe.getReceivedHeader();
    }
    

    /**
     * wraps the request by a request object which will read parameters from the body 
     * @param request the request to wrap 
     * @return request object which will read parameters from the body
     * @throws IOException if an exception occurs 
     */
    protected static IHttpRequest newFormEncodedRequestWrapper(IHttpRequest request) throws IOException {
        return HttpUtils.newFormEncodedRequestWrapper(request);
    }
    

    /**
     * register the task as which will be executed if the body data sink will be closed 
     * @param bodyDataSink the body data sink
     * @param task the task to execute
     */
    protected final void setBodyCloseListener(BodyDataSink bodyDataSink, Runnable task) {
        bodyDataSink.addCloseListener(new BodyCloseListener(task));
    }
    
    
    private static final class BodyCloseListener implements IBodyCloseListener {
        
        private Runnable task = null;
        
        public BodyCloseListener(Runnable task) {
            this.task = task;
        }
        
        public void onClose() throws IOException {
            task.run();
        }
    }
    

    

    /**
     * return the max write buffer size
     * 
     * @return the max write buffer size
     */
    protected static int getMaxWriteBufferSize() {
        return MAX_WRITEBUFFER_SIZE;
    }
    
    
    /**
     * generates a system error html
     * 
     * @param errorCode      the error code 
     * @param msg            the error message
     * @param connectionId   the connection id
     * @return the html page
     */
    protected static String generateErrorMessageHtml(int errorCode, String msg, String connectionId) {
        
        
        if (msg == null) {
            msg = HttpUtils.getReason(errorCode);
        }
        
        
        msg = msg.replace("\r", "<br/>");

        String txt = "<html>\r\n" +
                     "  <!-- This page is auto-generated by xLightweb (http://xLightweb.org) -->\r\n" +
                     "  <!-- id " + connectionId + " -->\r\n" +
                     "  <!-- xLightweb/"  + HttpUtils.getImplementationVersion() +  " (xSocket/" + ConnectionUtils.getImplementationVersion() + ") -->\r\n" +
                     "  <head>\r\n" +
                     "    <title>Error " + errorCode + "</title>\r\n" +
                     "    <meta http-equiv=\"cache-control\" content=\"no-cache\"/>" +
                     "  </head>\r\n\r\n" +
                     "  <body>\r\n" +
                     "    <H1 style=\"color:#0a328c;font-size:1.5em;\">ERROR " + errorCode + "</H1>\r\n" +
                     "    <p style=\"font-size:1.5em;\">" + msg + "</p>\r\n" +
                     "    <p style=\"font-size:0.8em;\">" + new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z").format(new Date()) + "    xLightweb (" + HttpUtils.getImplementationVersion() + ")</p>\r\n" +
                     "  <body>\r\n" +
                     "</html>\r\n";
            
        return txt;
    }
    
    
    
    
    /**
     * creates a empty body data sink
     * @param  the header
     * @return the body data sink
     * @throws IOException in an exception occurs
     */
    protected final BodyDataSink newEmtpyBodyDataSink(IHttpMessageHeader header) throws IOException {
        return new EmptyBodyDataSink(header);
    }

    
    
    
    private AbstractHttpProtocolHandler getProtocolHandler() {
        return protocolHandler;
    }
    
    
    private static AbstractHttpConnection getHttpConnection(INonBlockingConnection tcpConnection) {
        return (AbstractHttpConnection) tcpConnection.getAttachment();
    }
    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getId() + " " + tcpConnection.getLocalAddress() + ":" + tcpConnection.getLocalPort() + " -> " + tcpConnection.getRemoteAddress() + ":" + tcpConnection.getRemotePort());
        if (!tcpConnection.isOpen()) {
            sb.append("  (closed)");
        }

        if (tcpConnection.isReceivingSuspended()) {
            sb.append("  (suspended)");
        }
        
        return sb.toString();
    }
    
    
    String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getId());
        try {
            sb.append(tcpConnection.getLocalAddress() + ":" + tcpConnection.getLocalPort() + " -> " + tcpConnection.getRemoteAddress() + ":" + tcpConnection.getRemotePort());
        } catch (Throwable ignore) { }
            
            
        if (!tcpConnection.isOpen()) {
            sb.append("  (closed)");
        }

        if (protocolHandler != null) {
            sb.append(" " + protocolHandler.toString());
        } else {
            sb.append(" protocolHandler=null");
        }
        
        sb.append(" isPersistent=" + isPersistentRef.get());
        
        return sb.toString();
    }
    
    

    /**
     * @author grro
     */
    protected interface IMessageHeaderHandler {

        /**
         * call back if a message header is received
         * 
         * @param message       the message
         * @throws IOException if an exception occurs 
         */
        public IMessageHandler onMessageHeaderReceived(IHttpMessage message) throws IOException;
        

        
        /**
         * call back if an exception is occurred
         *  
         * @param ioe      the exception
         * @param rawData  the unprocessed network data
         */
        public void onHeaderException(IOException ioe, ByteBuffer[] rawData);
        
        
        
        /**
         * returns the associated header  
         * @return the associated header 
         */
        public IHttpMessageHeader getAssociatedHeader();
    }
    
    
    /**
     * @author grro
     */
    protected interface IMessageHandler {
   
        /**
         * call back if an exception is occurred
         *  
         * @param ioe the exception
         * @param rawData  the unprocessed network data 
         */
        public void onBodyException(IOException ioe, ByteBuffer[] rawData);
        
        /**
         * call back
         * @throws IOException  io exception
         */
        public void onMessageReceived() throws IOException;
        
        
        /**
         * call back 
         * @throws IOException  io exception
         */
        public void onHeaderProcessed() throws IOException;
    }


     /**
     * multimode executor definition 
     * @author grro
     */
    protected interface IMultimodeExecutor {

        /**
         * process the task multi threaded
         * @param task the task to process
         */
        void processMultithreaded(Runnable task);
        
        
        /**
         * process the task non threaded
         * @param task the task to process
         */
        void processNonthreaded(Runnable task);
        
        
        Executor getWorkerpool();
    }
    
    
    
    protected abstract static class AbstractExchange implements IHttpExchange {
        
        private final AtomicBoolean isResponseCommitted = new AtomicBoolean(false);
        private boolean is100ContinueSent = false;
        
        private final AbstractExchange parentExchange;
        private final IMultimodeExecutor executor;
        private IHttpConnection httpCon = null;
        
        protected AbstractExchange(IHttpExchange parentExchange) {
            if ((parentExchange != null) && (parentExchange instanceof AbstractExchange)) {
                this.parentExchange = (AbstractExchange) parentExchange;
                executor = ((AbstractExchange) parentExchange).getExecutor();
            } else {
                this.parentExchange = null;
                executor = HttpUtils.newMultimodeExecutor();
            }
            
            if (parentExchange != null) {
                httpCon = parentExchange.getConnection();
            }
        }
        
        
        
        protected AbstractExchange(AbstractExchange parentExchange, Executor workerpool) {
            this.parentExchange = parentExchange;
            executor = HttpUtils.newMultimodeExecutor();
        }
        
                
        protected AbstractExchange(AbstractExchange parentExchange, AbstractHttpConnection httpCon) {
            this.parentExchange = parentExchange;
            setHttpConnection(httpCon);
            this.executor = httpCon.getExecutor();
        }
        
        protected final void setHttpConnection(IHttpConnection httpCon) {
            this.httpCon = httpCon;
        }
        
        IMultimodeExecutor getExecutor() {
            return executor;
        }
                
        protected final void setHttpConnection(AbstractHttpConnection httpCon) {
            this.httpCon = httpCon;
        }
        
        protected final boolean isResponseCommitted() {
            return isResponseCommitted.get();
        }
        
        protected final void setResponseCommited(boolean isCommitted) {
            isResponseCommitted.set(isCommitted);
        }
        
        protected final void ensureResponseHasNotBeenCommitted() throws IOException {
            if (isResponseCommitted() == true) {
                throw new IOException("response has already been committed");
            }
        }
    

        protected String getId() {
            if (httpCon != null) {
                return httpCon.getId();
            } else {
                return Integer.toString(hashCode());
            }
        }

        
        /**
         * {@inheritDoc}
         */
        public final IHttpConnection getConnection() {
            if (httpCon != null) {
                return httpCon;
                
            } else {
                return new NullConnection(executor.getWorkerpool());
            }
        }
        
        
        /**
         * {@inheritDoc}
         */
        public final void destroy() {
            if (httpCon != null) {
                httpCon.destroy();
            }
        }
        
        
        protected final void destroy(String reason) {
            if (httpCon != null) {
                httpCon.destroy();
            }
        }
        
       
        public void sendRedirect(String location) throws IllegalStateException {
            
            if (!HttpUtils.isAbsoluteURL(location)) {
                
                String url = HttpUtils.getRequestURLWithoutQueryParams(getRequest().getRequestHeader()); 
                
                if (location.startsWith("/")) {
                    String base = url.substring(0, url.length() - getRequest().getRequestURI().length());
                    
                    location = base + getRequest().getContextPath() + location;
                } else {
                    location = url + "/" + location; 
                }
            }
            
            
            String txt = "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\r\n" + 
                         "<html><head>\r\n" +
                         "<title>302 Found</title>\r\n" +
                         "</head><body>\r\n" +
                         "<h1>Found</h1>\r\n" +
                         "<p>The document has moved <a href=\"" + location + "\">here</a>.</p>\r\n" +
                         "</body></html>";
            
            try {
                HttpResponse response = new HttpResponse(302, "text/html", txt);
                response.setHeader("Location", location);
                
                send(response);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
        
        
        /**
         * send an error response
         *
         * @param errorCode   the error code
         */
        public final void sendError(int errorCode) {
            sendError(errorCode, HttpUtils.getReason(errorCode));
        }

        
        

        /**
         * send an error response
         *
         * @param errorCode   the error code
         * @param msg         the error message
         */
        public void sendError(int errorCode, String msg) {
            try {
                send(new HttpResponse(errorCode, "text/html", generateErrorMessageHtml(errorCode, msg, getId())));
            } catch (IOException ioe) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + getId() + "] could not send error message " + errorCode + " reason " + ioe.toString());
                }
                destroy(ioe.toString());
            }
        }
        
        
        /**
         * @deprecated
         */
        public final boolean sendContinue() throws IOException {
            return sendContinueIfRequested();
        }
        
        
        
        public final boolean sendContinueIfRequested() throws IOException {
            
            if (HttpUtils.isContainExpect100ContinueHeader(getRequest().getRequestHeader()) && (!is100ContinueSent())) {
                is100ContinueSent = true;
                doSendContinue();
                
                if (parentExchange != null) {
                    parentExchange.set100ContinueSent(is100ContinueSent);
                }
                return true;
            }
            
            return false;
        }
        
        void set100ContinueSent(boolean is100ContinueSent) {
            this.is100ContinueSent = is100ContinueSent; 
            
            if (parentExchange != null) {
                parentExchange.set100ContinueSent(is100ContinueSent);
            }
        }
        
        protected void doSendContinue() throws IOException {
            send(new HttpResponse(100));
        }
        
        
        protected final boolean is100ContinueSent() {
            if (parentExchange != null) {
                return (parentExchange.is100ContinueSent() || is100ContinueSent);
            } else {
                return is100ContinueSent;
            }
        }
        
    

        protected final void callResponseHandler(IHttpResponseHandler responseHandler, IHttpResponse response) throws IOException {
            
            ResponseHandlerInfo handlerInfo = HttpUtils.getResponseHandlerInfo(responseHandler);
            
            if (response.hasBody() && handlerInfo.isResponseHandlerInvokeOnMessageReceived()) {
                
                NonBlockingBodyDataSource ds = response.getNonBlockingBody();
                
                BodyListener bodyListener = new BodyListener(ds, responseHandler, handlerInfo, response);
                ds.addCompleteListener(bodyListener);
                ds.addDestroyListener(bodyListener);
            
            } else {
                callResponseHandler(responseHandler, handlerInfo, response);
            }
        }

        
        
        private final class BodyListener implements IBodyCompleteListener, IBodyDestroyListener {
            
            private NonBlockingBodyDataSource ds;
            private IHttpResponseHandler responseHandler; 
            private ResponseHandlerInfo handlerInfo;
            private IHttpResponse response;
            
            public BodyListener(NonBlockingBodyDataSource ds, IHttpResponseHandler responseHandler, ResponseHandlerInfo handlerInfo, IHttpResponse response) {
                this.ds = ds;
                this.responseHandler = responseHandler;
                this.handlerInfo = handlerInfo;
                this.response = response;
            }
            
            public void onComplete() throws IOException {
                callResponseHandler(responseHandler, handlerInfo, response);
            }
            
            public void onDestroyed() throws IOException {
                IOException ioe = ds.getException(); 
                if (ioe == null) {
                    ioe = new IOException("data source hhas been destroyed");
                }
                responseHandler.onException(ioe);
            }
        };
        
        
        protected final void callResponseHandler(final IHttpResponseHandler responseHandler, final ResponseHandlerInfo handlerInfo, final IHttpResponse response) {
            
            if (handlerInfo.isUnsynchronized()) {
                call(responseHandler, response);
                
            } else {
                Runnable task = new Runnable() {
                    public void run() {
                        call(responseHandler, response);
                    }
                };
    
                if (handlerInfo.isResponseHandlerMultithreaded()) {
                    executor.processMultithreaded(task);
                } else {
                    executor.processNonthreaded(task); 
                }
            }
        }
        
        
        private void call(IHttpResponseHandler responseHandler, IHttpResponse response) {
            
            try {
                responseHandler.onResponse(response);
                
            } catch (IOException ioe) {
                if (httpCon != null) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + httpCon.getId() + "] Error occured by calling response handler " + responseHandler + " " + ioe.toString());
                    }
                    destroy(ioe.toString());
                }
            }
        }
        
        
        protected final void callResponseHandler(IHttpResponseHandler responseHandler, IOException ioe) {
            callResponseHandler(responseHandler, HttpUtils.getResponseHandlerInfo(responseHandler), ioe);
        }

        
        protected final void callResponseHandler(final IHttpResponseHandler responseHandler, final ResponseHandlerInfo handlerInfo, final IOException ioe) {
            
            if (handlerInfo.isUnsynchronized()) {
                call(responseHandler, ioe);
                
            } else {
                Runnable task = new Runnable() {
                    public void run() {
                        call(responseHandler, ioe);
                    }
                };
    
                if (handlerInfo.isResponseHandlerMultithreaded()) {
                    executor.processMultithreaded(task);
                } else {
                    executor.processNonthreaded(task); 
                }
            }
        }
        
        
        private void call(IHttpResponseHandler responseHandler, IOException ioe) {
            
            try {
                if ((ioe instanceof SocketTimeoutException) && (HttpUtils.getResponseHandlerInfo(responseHandler).isSocketTimeoutHandler())) {
                    ((IHttpSocketTimeoutHandler) responseHandler).onException((SocketTimeoutException) ioe);
                    
                }else {
                    responseHandler.onException(ioe);
                }
                
            } catch (IOException e) {
                if (httpCon != null) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + httpCon.getId() + "] Error occured by calling response handler " + responseHandler + " " + e.toString());
                    }
                    destroy(e.toString());
                }
            }
        }
    }
    
    
 
    
    
    
    
    @Execution(Execution.NONTHREADED)
    final class DataHandler implements IDataHandler, IConnectHandler, IDisconnectHandler, IIdleTimeoutHandler, IConnectionTimeoutHandler, IHandlerChangeListener {

        // network data
        private ByteBuffer[] rawBuffers = null;

        
        public boolean onData(final INonBlockingConnection connection) throws BufferUnderflowException {
            lastTimeDataReceivedMillis = System.currentTimeMillis();

            if (connection.isOpen() && !isClosing()) {
                
                try {
                    // get the protocol handler
                    AbstractHttpProtocolHandler protocolHandler = (AbstractHttpProtocolHandler) getHttpConnection(connection).getProtocolHandler();

                    
                    // copying available network data into raw data buffer
                    int available = connection.available();
                    if (available > 0) {
                        ByteBuffer[] data = connection.readByteBufferByLength(available);
                        
                        incReveived(available);
    
                        if (rawBuffers == null) {
                            rawBuffers = data;
                        } else {
                            rawBuffers = HttpUtils.merge(rawBuffers, data);
                        }
                        
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("[" + getId() + "] TCP read (total received: " + protocolHandler.getReceived() + ", currentRawBuffer=" + HttpUtils.computeRemaining(rawBuffers) + ") ");
                            LOG.fine("[" + getId() + "] calling protocol handler " + protocolHandler.getClass().getName() + "#" + protocolHandler.hashCode());
                        }
    
                    
                                
                        // handle data based on the buffer
                        rawBuffers = protocolHandler.onData(AbstractHttpConnection.this, rawBuffers);
                    }
                    
                    return true;
                        
                } catch (ClosedChannelException cce) {
                    setPersistent(false);
                    protocolHandler.onDisconnect(AbstractHttpConnection.this, rawBuffers);
                        
                } catch (IOException ex) {
                    setPersistent(false);
                    protocolHandler.onException(AbstractHttpConnection.this, ex, rawBuffers);
                    onProtocolException(ex);
                }
                
            } else {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + getId()+ "] Connection is closed. ignoring  incoming data");
                }
            }
            
            return true;
        }

        
        public void onHanderReplaced(IHandler oldHandler, IHandler newHandler) {
            if (oldHandler == this) {
                try {
                    if (rawBuffers != null) {
                        getUnderlyingTcpConnection().unread(HttpUtils.compact(rawBuffers));
                    }
                } catch (IOException ioe) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + getId() + "] could not unread " + ioe.toString());
                    }
                }
            }
        }
        
        
        public boolean onConnect(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            return true;
        }
        
        
        public boolean onDisconnect(final INonBlockingConnection connection) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] disconnected");
            }
                    
            AbstractHttpProtocolHandler protocolHandler = (AbstractHttpProtocolHandler) getHttpConnection(connection).getProtocolHandler();
            protocolHandler.onDisconnect(AbstractHttpConnection.this, rawBuffers);
                
            AbstractHttpConnection.this.onDisconnect();            
            return true;
        }
        
        
        public boolean onConnectionTimeout(final INonBlockingConnection connection) {
            
            if (!isClosing()) {
                AbstractHttpConnection.this.onConnectionTimeout();
            }
            
            return true;
        }
        
        
        public boolean onIdleTimeout(final INonBlockingConnection connection) {
            
            if (!isClosing()) {
                AbstractHttpConnection.this.onIdleTimeout();
            }
            
            return true;
        }
        
        
        ByteBuffer[] drainBuffer() {
            ByteBuffer[] result = rawBuffers;
            rawBuffers = null;
            
            return result;
        }
    }        

        
    

    
    
    protected final static class RequestHandlerAdapter {

        private final IHttpRequestHandler requestHandler;
        private final IHttpRequestTimeoutHandler requestTimeoutHandler;
        
        private final boolean isInvokeOnMessageReceived; 
        private final boolean isContinueHandler;
        
        
        public RequestHandlerAdapter(IHttpRequestHandler delegate) {
            RequestHandlerInfo handlerInfo = HttpUtils.getRequestHandlerInfo(delegate);

            isInvokeOnMessageReceived = handlerInfo.isRequestHandlerInvokeOnMessageReceived();
            isContinueHandler = handlerInfo.isContinueHandler();
            
            // wrapping request timeout handler
            if (handlerInfo.isRequestTimeoutHandler()) {
                if (handlerInfo.isUnsynchronized()) {
                    requestTimeoutHandler = (IHttpRequestTimeoutHandler) delegate;
                    
                } else if (handlerInfo.isRequestTimeoutHandlerMultithreaded()) {
                    requestTimeoutHandler = new MultithreadedRequestTimeoutHandlerAdapter((IHttpRequestTimeoutHandler) delegate);       
                    
                } else {
                    requestTimeoutHandler = new NonthreadedRequestTimeoutHandlerAdapter((IHttpRequestTimeoutHandler) delegate);
                }
            } else {
                requestTimeoutHandler = new EmptyRequestTimeoutHandlerAdapter();
            }
            

            
            // wrap session-scope request handler 
            if (handlerInfo.isRequestHandlerSynchronizedOnSession()) {
                delegate = new SessionSynchronizedRequestHandlerAdapter(delegate);
            }
            
        
            // wrap threaded request handler 
            if (handlerInfo.isRequestHandlerSynchronizedOnSession()) {
                requestHandler = new MultithreadedRequestHandlerAdapter(delegate);
                
            } else if (handlerInfo.isUnsynchronized()) {
                requestHandler = delegate;
                
            } else if (handlerInfo.isRequestHandlerMultithreaded()) {
                requestHandler = new MultithreadedRequestHandlerAdapter(delegate);
                
            } else {
                requestHandler = new NonthreadedRequestHandlerAdapter(delegate);   
            }
        }
        
        @Override
        public String toString() {
            return requestHandler.toString();
        }
        
        public final IHttpRequestHandler getDelegate() {
            return requestHandler;
        }
        
        
        public final boolean isInvokeOnMessageReceived() {
            return isInvokeOnMessageReceived;
        }
        
        
        private final boolean isContinueHandler() {
            return isContinueHandler;
        }
        
        public final void onRequest(IHttpExchange exchange) {
            performRequestHandler(requestHandler, exchange);
        }
            
        
        private void performRequestHandler(IHttpRequestHandler handler, final IHttpExchange exchange) {
            try {                
               if (!isContinueHandler() && HttpUtils.isContainExpect100ContinueHeader(exchange.getRequest().getRequestHeader())) {
                   if (exchange.getRequest().hasBody()) {
                       IBodyAccessListener al = new IBodyAccessListener() {
                        
                        public boolean onBodyAccess() {
                            try {
                                exchange.sendContinueIfRequested();
                            } catch (IOException ioe) {
                                if (LOG.isLoggable(Level.FINE)) {
                                    LOG.fine("error occured by trying to dsend 100-continue " + ioe.toString());
                                }
                            }
                            return true;
                        }
                    };
                       exchange.getRequest().getNonBlockingBody().setBodyAccessListener(al);
                   }
                }
               
                handler.onRequest(exchange);
                
            } catch (BadMessageException bme) {
                if (HttpUtils.isShowDetailedError()) {
                    exchange.sendError(bme.getStatus(), DataConverter.toString(bme));
                } else {
                    exchange.sendError(bme.getStatus());
                }
                
            } catch (IOException e) {
                
                if (exchange.getConnection() instanceof AbstractHttpConnection) {
                    ((AbstractHttpConnection) exchange.getConnection()).setPersistent(false);
                }
                
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + exchange.getConnection().getId() + "] error occured by calling on request " + requestHandler + " " + DataConverter.toString(e));
                }
                
                if (exchange instanceof AbstractExchange) {
                    if (!((AbstractExchange) exchange).isResponseCommitted()) {
                        exchange.sendError(e);
                        return;
                    }
                }
                
                exchange.destroy();
                
                
            } catch (IllegalStateException e) {
                
                if (exchange.getConnection() instanceof AbstractHttpConnection) {
                    ((AbstractHttpConnection) exchange.getConnection()).setPersistent(false);
                }
                
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + exchange.getConnection().getId() + "] error occured by calling on request " + requestHandler + " " + DataConverter.toString(e));
                }
                
                if (exchange instanceof AbstractExchange) {
                    if (!((AbstractExchange) exchange).isResponseCommitted()) {
                        exchange.sendError(e);
                        return;
                    }
                }
                
                exchange.destroy();            
   
            } catch (Exception e) {
                
                if (exchange.getConnection() instanceof AbstractHttpConnection) {
                    ((AbstractHttpConnection) exchange.getConnection()).setPersistent(false);
                }
                
                LOG.warning("[" + exchange.getConnection().getId() + "] error occured by calling on request " + requestHandler + " " + DataConverter.toString(e));
                
                if (exchange instanceof AbstractExchange) {
                    if (!((AbstractExchange) exchange).isResponseCommitted()) {
                        exchange.sendError(e);
                        return;
                    }
                }
                
                exchange.destroy();
            }
        }
        
        
        public final void onRequestTimeout(IHttpConnection connection) {
            performRequestTimeoutHandler(requestTimeoutHandler, connection);
        }
        
        
        private void performRequestTimeoutHandler(IHttpRequestTimeoutHandler handler, IHttpConnection connection) {
            try {
                boolean isHandled = handler.onRequestTimeout(connection);
                if (!isHandled) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + connection.getId() + "] request timeout reached for http server connection. terminate connection (timeout handler returned false)");
                    }

                    closeConnection(connection);
                }
                
            } catch (Exception e) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + connection.getId() + "] error occured by calling on request " + handler + " " + e.toString());
                }
                
                if (connection instanceof AbstractHttpConnection) {
                    ((AbstractHttpConnection) connection).destroy(e.toString());
                } 
            }
        }
        
        
        private void closeConnection(IHttpConnection connection) {
            try {
                connection.close();
            } catch (IOException ioe) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + connection.getId() + "] error occured by closing connection");
                }
            }
        }

        
        private static final class EmptyRequestTimeoutHandlerAdapter implements IHttpRequestTimeoutHandler {

            public boolean onRequestTimeout(final IHttpConnection connection) throws IOException {
                return false;
            }
            
            @Override
            public String toString() {
                return this.getClass().getSimpleName() + "#" + hashCode();
            }
        }
        
        
        private final class MultithreadedRequestTimeoutHandlerAdapter implements IHttpRequestTimeoutHandler {
            
            private final IHttpRequestTimeoutHandler delegate;
            
            public MultithreadedRequestTimeoutHandlerAdapter(IHttpRequestTimeoutHandler delegate) {
                this.delegate = delegate;
            }

            public boolean onRequestTimeout(final IHttpConnection connection) throws IOException {
                Runnable task = new Runnable() {
                    public void run() {
                        performRequestTimeoutHandler(delegate, (AbstractHttpConnection) connection);
                    }
                };
                
                if (connection instanceof AbstractHttpConnection) {
                    ((AbstractHttpConnection) connection).getExecutor().processMultithreaded(task);
                } else {
                    HttpUtils.newMultimodeExecutor().processMultithreaded(task);
                }
                return true;
            }
            
            @Override
            public String toString() {
                return super.getClass().getName() + "*" + delegate.getClass().getName() + "@" + Integer.toHexString(hashCode());
            }
        }
        
        
        
        private final class NonthreadedRequestTimeoutHandlerAdapter implements IHttpRequestTimeoutHandler {

            private final IHttpRequestTimeoutHandler delegate;
            
            public NonthreadedRequestTimeoutHandlerAdapter(IHttpRequestTimeoutHandler delegate) {
                this.delegate = delegate;
            }

            public boolean onRequestTimeout(final IHttpConnection connection) throws IOException {
                Runnable task = new Runnable() {
                    public void run() {
                        performRequestTimeoutHandler(delegate, (AbstractHttpConnection) connection);
                    }
                };
                
                if (connection instanceof AbstractHttpConnection) {
                    ((AbstractHttpConnection) connection).getExecutor().processNonthreaded(task);
                } else {
                    HttpUtils.newMultimodeExecutor().processNonthreaded(task);
                }

                return true;
            }
            
            @Override
            public String toString() {
                return super.getClass().getName() + "*" + delegate.getClass().getName() + "@" + Integer.toHexString(hashCode());
            }
        }

        @Supports100Continue
        private final class NonthreadedRequestHandlerAdapter implements IHttpRequestHandler{
            
            private final IHttpRequestHandler delegate;
            
            public NonthreadedRequestHandlerAdapter(IHttpRequestHandler delegate) {
                this.delegate = delegate;
            }

            public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
             
                Runnable task = new Runnable() {
                    public void run() {
                        performRequestHandler(delegate, exchange);
                    }
                };
                
                if (exchange instanceof AbstractExchange) {
                    ((AbstractExchange) exchange).getExecutor().processNonthreaded(task);
                } else {
                    HttpUtils.newMultimodeExecutor().processNonthreaded(task);
                }
            }     
            
            @Override
            public String toString() {
                return super.getClass().getName() + "*" + delegate.getClass().getName() + "@" + Integer.toHexString(hashCode());
            }            
        }
        
        
        @Supports100Continue
        private final class MultithreadedRequestHandlerAdapter implements IHttpRequestHandler {
            
            private final IHttpRequestHandler delegate;
            
            public MultithreadedRequestHandlerAdapter(IHttpRequestHandler delegate) {
                this.delegate = delegate;
            }

            public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
                Runnable task = new Runnable() {
                    public void run() {
                        performRequestHandler(delegate, exchange);
                    }
                };
                
                if (exchange instanceof AbstractExchange) {
                    ((AbstractExchange) exchange).getExecutor().processMultithreaded(task);
                } else {
                    HttpUtils.newMultimodeExecutor().processMultithreaded(task);
                }
            }
            
            @Override
            public String toString() {
                return super.getClass().getName() + "*" + delegate.getClass().getName() + "@" + Integer.toHexString(hashCode());
            }            
        }
        
        

        @Supports100Continue
        private static final class SessionSynchronizedRequestHandlerAdapter implements IHttpRequestHandler {

            private IHttpRequestHandler delegate = null;

            public SessionSynchronizedRequestHandlerAdapter(IHttpRequestHandler delegate) {
                this.delegate = delegate;
            }
            
            public void onRequest(IHttpExchange exchange) throws IOException {
                
                IHttpSession session = exchange.getSession(false);
                
                if (session == null) {
                    delegate.onRequest(exchange);
                    
                } else {
                    synchronized (session) {
                        delegate.onRequest(exchange);
                    }
                }
            }
            
            @Override
            public String toString() {
                return super.getClass().getName() + "*" + delegate.getClass().getName() + "@" + Integer.toHexString(hashCode());
            }
        }
    }

    

    private static interface IResponseHandlerAdapter {
     
        void onResponse(IHttpResponse response, AbstractHttpConnection httpConnection);
    }
    
    
    protected static final class ResponseHandlerAdapter implements IResponseHandlerAdapter {

        private final IResponseHandlerAdapter responseHandlerAdapter;
        private final IHttpResponseHandler exceptionHandler;
        private final ResponseHandlerInfo handlerInfo;
        
        
        // continue support
        private final boolean isContinueResponseExpected;
        private final AtomicBoolean is100ContinueHandled = new AtomicBoolean(false);
                
        
        public ResponseHandlerAdapter(IHttpResponseHandler delegate, IHttpRequestHeader requestHeader) {
            handlerInfo = HttpUtils.getResponseHandlerInfo(delegate);
            isContinueResponseExpected = HttpUtils.isContainExpect100ContinueHeader(requestHeader);
            
            exceptionHandler = delegate;

            if (handlerInfo.isUnsynchronized()) {
                responseHandlerAdapter = new UnsynchronizedResponseHandlerAdapter(delegate);

            } else if (handlerInfo.isResponseHandlerMultithreaded()) {
                responseHandlerAdapter = new MultithreadedResponseHandlerAdapter(delegate);
                
            } else {
                responseHandlerAdapter = new NonthreadedResponseHandlerAdapter(delegate);
            }            
        }
        
        
        public boolean isInvokeOnMessageReceived() {
            return handlerInfo.isResponseHandlerInvokeOnMessageReceived();
        }
        
        
        public boolean isContinueHandler() {
            return handlerInfo.isContinueHandler();
        }
        
        
        
        public void onResponse(IHttpResponse response, AbstractHttpConnection httpConnection) {

            if (response.getStatus() == 100) {
                
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("got 100-continue response");
                }
                
                
                // 100-Continue expected?
                if (isContinueResponseExpected && handlerInfo.isContinueHandler()) {
                    if (is100ContinueHandled.getAndSet(true)) {
                        return;
                    }
                    
                // no
                } else {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("ignore 100-continue (isContinueResponseExpected=" + isContinueResponseExpected + 
                                ", isContinueHandler=" + handlerInfo.isContinueHandler() + ")");
                    }
                    return;
                } 
            }
             
            
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("call response handler " + responseHandlerAdapter);
            }
                
            responseHandlerAdapter.onResponse(response, httpConnection);
        }
        
        
        private void performResponseHandler(IHttpResponseHandler handler, IHttpResponse response, AbstractHttpConnection httpConnection) {
            HttpUtils.addConnectionAttribute(response.getResponseHeader(), httpConnection);
            
            try {
                handler.onResponse(response);
                
            } catch (Exception e) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("error occured by calling on response " + responseHandlerAdapter + " " + e.toString());
                }
                httpConnection.destroy(e.toString());
            }   
        }
        
        
        public void onException(final IOException ioe, final AbstractHttpConnection httpConnection) {
            
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("notifing response handler");
            }

            
            if (handlerInfo.isUnsynchronized()) {
                performResponseHandler(ioe, httpConnection);
                return;
            }
            
            boolean isMultithreaded = false;
            if ((ioe instanceof SocketTimeoutException)) {
                if (handlerInfo.isSocketTimeoutHandlerMultithreaded()) {
                    isMultithreaded = true;
                }
            } else if (handlerInfo.isResponseExeptionHandlerMultithreaded()) {
                isMultithreaded = true;
            }
            
            Runnable task = new Runnable() {
                public void run() {
                    performResponseHandler(ioe, httpConnection);
                }
            };
            
            if (isMultithreaded) {
                httpConnection.getExecutor().processMultithreaded(task);
            } else {
                httpConnection.getExecutor().processNonthreaded(task);
            }
        }
        
        
        private void performResponseHandler(IOException ioe, AbstractHttpConnection httpConnection) {
            try {
                if ((ioe instanceof SocketTimeoutException) && (handlerInfo.isSocketTimeoutHandler())) {
                    ((IHttpSocketTimeoutHandler) exceptionHandler).onException((SocketTimeoutException) ioe);
                } else {
                    exceptionHandler.onException(ioe);
                }
                
            } catch (Exception e) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("error occured by calling on exception " + responseHandlerAdapter + " " + e.toString());
                }
                httpConnection.destroy(e.toString());
            }
        }
        
        
        private final class UnsynchronizedResponseHandlerAdapter implements IResponseHandlerAdapter {
            
            private final IHttpResponseHandler delegate;
            
            
            public UnsynchronizedResponseHandlerAdapter(IHttpResponseHandler delegate) {
                this.delegate = delegate;
            }

            public void onResponse(final IHttpResponse response, final AbstractHttpConnection httpConnection)  {
                performResponseHandler(delegate, response, httpConnection);
            }        
            
            @Override
            public String toString() {
                return this.getClass().getName() + "*" + delegate;
            }
        }        

        
        
        private final class NonthreadedResponseHandlerAdapter implements IResponseHandlerAdapter {
            
            private final IHttpResponseHandler delegate;
            
            
            public NonthreadedResponseHandlerAdapter(IHttpResponseHandler delegate) {
                this.delegate = delegate;
            }

            public void onResponse(final IHttpResponse response, final AbstractHttpConnection httpConnection)  {
                Runnable task = new Runnable() {
                    public void run() {
                        performResponseHandler(delegate, response, httpConnection);
                    }
                };
                httpConnection.getExecutor().processNonthreaded(task);
            }      
            
            @Override
            public String toString() {
                return this.getClass().getName() + "*" + delegate;
            }
        }        
        
        
        private final class MultithreadedResponseHandlerAdapter implements IResponseHandlerAdapter {
            
            private final IHttpResponseHandler delegate;
            
            
            public MultithreadedResponseHandlerAdapter(IHttpResponseHandler delegate) {
                this.delegate = delegate;
            }

            public void onResponse(final IHttpResponse response, final AbstractHttpConnection httpConnection)  {
                Runnable task = new Runnable() {
                    public void run() {
                        performResponseHandler(delegate, response, httpConnection);
                    }
                };
                httpConnection.getExecutor().processMultithreaded(task);
            }     
            
            @Override
            public String toString() {
                return this.getClass().getName() + "*" + delegate;
            }
        }        
    }

    
    private static final class DoNothingResponseHandler implements IHttpResponseHandler, IUnsynchronized {

        public void onResponse(IHttpResponse response) throws IOException { 
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("ignoring response " + response.getResponseHeader());
            }
        }

        public void onException(IOException ioe) throws IOException { 
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("ignoring exception " + ioe.toString());
            }
        }
        
        @Override
        public String toString() {
            return this.getClass().getName();
        }
    }
    
    
    private static final class NullConnection implements IHttpConnection {

         private Object attachment;
         private final Executor workerpool;
         
         public NullConnection(Executor workerpool) {
             this.workerpool = workerpool;
        }
     
         public void activateSecuredMode() throws IOException {
         }

         public void addConnectionHandler(IHttpConnectionHandler connectionHandler) {
         }

         public long getBodyDataReceiveTimeoutMillis() {
             return 0;
         }

         public INonBlockingConnection getUnderlyingTcpConnection() {
             return null;
         }

         public Executor getWorkerpool() {
             return workerpool;
         }

         public boolean isPersistent() {
             return false;
         }

         public boolean isReceivingSuspended() {
             return false;
         }

         public boolean isSecure() {
             return false;
         }

         public void removeConnectionHandler(IHttpConnectionHandler connectionHandler) {
         }

         public void resumeReceiving() throws IOException {
         }

         public void setBodyDataReceiveTimeoutMillis(long bodyDataReceiveTimeoutMillis) {
         }

         public void setWriteTransferRate(int bytesPerSecond) throws ClosedChannelException, IOException {
         }

         public void suspendReceiving() throws IOException {
         }

         public long getConnectionTimeoutMillis() {
             return 0;
         }

         public String getId() {
             return "<unset>";
         }

         public long getIdleTimeoutMillis() {
             return 0;
         }

         public InetAddress getLocalAddress() {
             return null;
         }

         public int getLocalPort() {
             return 0;
         }

         public Object getOption(String name) throws IOException {
             return null;
         }

         
         @SuppressWarnings("rawtypes")
        public Map<String, Class> getOptions() {
             return null;
         }
         

         public long getRemainingMillisToConnectionTimeout() {
             return 0;
         }

         public long getRemainingMillisToIdleTimeout() {
             return 0;
         }

         public InetAddress getRemoteAddress() {
             return null;
         }

         public int getRemotePort() {
             return 0;
         }

         public boolean isOpen() {
             return false;
         }

         public boolean isServerSide() {
             return false;
         }

         public void setAttachment(Object obj) {
             this.attachment = obj;
         }

         public Object getAttachment() {
             return attachment;
         }
         
         public void setConnectionTimeoutMillis(long timeoutMillis) {
         }

         public void setIdleTimeoutMillis(long timeoutInMillis) {
         }

         public void setOption(String name, Object value) throws IOException {
         }

         public void close() throws IOException {
         }

         public void closeQuitly() {
         }

         public void destroy() {
         }
    }
    
    
    private static final class EmptyBodyDataSink extends BodyDataSinkImplBase {
        
        
        public EmptyBodyDataSink(IHttpMessageHeader header) throws IOException {
            super(header, HttpUtils.newMultimodeExecutor());
        }
        
        @Override
        protected boolean isNetworkendpoint() {
            return false;
        }
        
        @Override
        void onClose() throws IOException {
            
        }
        
        @Override
        void onDestroy(String reason) {
            
        }
        
        @Override
        int getPendingWriteDataSize() {
            return 0;
        }
        
        @Override
        int onWriteData(ByteBuffer[] dataToWrite, IWriteCompletionHandler completionHandler) throws IOException {
            int written = 0;
            for (ByteBuffer buffer : dataToWrite) {
                written += buffer.remaining();
            }
            return written;
        }       
    }
}
