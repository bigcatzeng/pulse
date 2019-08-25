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
import java.net.URI;
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xlightweb.AbstractHttpConnection.IMultimodeExecutor;
import org.xlightweb.WebSocketHandlerAdapter.IPostConnectInterceptor;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.client.IHttpClientEndpoint;
import org.xlightweb.server.HttpServerConnection;
import org.xsocket.DataConverter;
import org.xsocket.Execution;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.IDisconnectHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.NonBlockingConnectionPool;




/**
 * NonBlockingWebSocketConnection
 * 
 * <br/><br/><b>This is an experimental implementation of the HTML5 draft and subject to change</b>
 * 
 * 
 * @author grro
 */
public final class WebSocketConnection implements IWebSocketConnection {

    private static final Logger LOG = Logger.getLogger(WebSocketConnection.class.getName());
    
    private INonBlockingConnection tcpConnection;
    
    // sec key computation
    private static final Random RANDOM = new Random();


    // receive timeout
    public static final int DEFAULT_RECEIVE_TIMEOUT = Integer.MAX_VALUE;
    private int receiveTimeoutSec = DEFAULT_RECEIVE_TIMEOUT;

    
    // protocol handling
    private final WebSocketProtocolHandler protocolHandler = new WebSocketProtocolHandler();
    private final IMultimodeExecutor executor;
    
    private final List<WebSocketMessage> inQueue = new ArrayList<WebSocketMessage>();
    private int inQueueVersion = 0;
    
    
    // client protocol mode
    private static final boolean CLIENT_USING_SEC_KEY = Boolean.parseBoolean(System.getProperty("org.xlightweb.websocket.client.usingSecKey", "true"));
    
    // upgrade headers
    private final IHttpRequestHeader upgradeRequestHeader;
    private final IHttpResponseHeader upgradeResponseHeader;
    
    // interceptor support
    private final IPostWriteInterceptor interceptor;
    
    // web socket handler
    private final Object webSocketHandlerGuard = new Object();
    private WebSocketHandlerAdapter webSocketHandlerAdapter = null;
    
    // exception holder
    private final AtomicReference<IOException> exceptionRef = new AtomicReference<IOException>(null);
    
    // flags
    private final AtomicBoolean isDisconnected = new AtomicBoolean(false);
    private final AtomicBoolean isCloseMsgSent = new AtomicBoolean(false);
        
    // attachment management
    private AtomicReference<Object> attachmentRef = new AtomicReference<Object>(null);

   
    public WebSocketConnection(String uriString) throws IOException {
        this(uriString, (String) null);
    }
    
    public WebSocketConnection(String uriString, String protocol) throws IOException {
        this(uriString, protocol, null);
    }

    
    public WebSocketConnection(String uriString, IWebSocketHandler webSocketHandler) throws IOException {
        this(uriString, null, webSocketHandler);
    }
    
    public WebSocketConnection(String uriString, String protocol, IWebSocketHandler webSocketHandler) throws IOException {
        this(URI.create(uriString), protocol, webSocketHandler);
    }
    
    private WebSocketConnection(URI uri, String protocol, IWebSocketHandler webSocketHandler) throws IOException {
        this(connect(uri), uri, protocol, webSocketHandler);
    }
  
    private static HttpClientConnection connect(URI uri) throws IOException {
        
        int port = uri.getPort();
        if (port == -1) {
            if (uri.getScheme().toLowerCase(Locale.US).equals("wss")) {
                port = 443;
            } else {
                port = 80;
            }
        }
        
        return new HttpClientConnection(uri.getHost(), port);
    }
    
    
    
    public WebSocketConnection(IHttpClientEndpoint httpClientEndpoint, URI uri, String protocol, IWebSocketHandler webSocketHandler) throws IOException {
        this(performHandshake(httpClientEndpoint, uri, protocol), webSocketHandler);

    }
    
    private WebSocketConnection(HandeshakeResult handeshakeResult, IWebSocketHandler webSocketHandler) throws IOException {
        this(handeshakeResult.con, webSocketHandler, handeshakeResult.upgradeRequestHeader, handeshakeResult.upgradeResponseHeader);
    }

    private WebSocketConnection(HttpClientConnection httpConnection, IWebSocketHandler webSocketHandler, IHttpRequestHeader upgradeRequestHeader, IHttpResponseHeader upgradeResponseHeader) throws IOException {
        this(convertToTcpConnection(httpConnection), null, webSocketHandler, null, upgradeRequestHeader, upgradeResponseHeader);
    }

    
    private static HandeshakeResult performHandshake(IHttpClientEndpoint httpClientEndpoint, URI uri, String protocol) throws IOException {
         
        GetRequest request;
        byte[] challengeMd5 = null; 
        
        if (CLIENT_USING_SEC_KEY) { 
            ByteBuffer buf = ByteBuffer.allocate(16);
            for (int i = 0; i < buf.limit(); i++) {
                buf.put((byte) (33 + RANDOM.nextInt(80)));
            }
            buf.flip();
            
            long key1 = buf.getInt();
            long key2 = buf.getInt();
            long challenge = buf.getLong();
            challengeMd5 = computeMD5((int) key1, (int) key2, challenge);

            
            // obfuscating the keys 
            int numSpaces1 = 10 + RANDOM.nextInt(Integer.MAX_VALUE) % 5;
            int numSpaces2 = 10 + RANDOM.nextInt(Integer.MAX_VALUE) % 5;
            String secKey1 = new String(generateSecKey(35, key1 * numSpaces1, numSpaces1));
            String secKey2 = new String(generateSecKey(34, key2 * numSpaces2, numSpaces2));
            
            
            request = new GetRequest(uri.toString(), DataConverter.toByteBuffer(challenge).array());
            request.removeHeader("Content-Length");
            request.setHeader("Sec-WebSocket-Key1", secKey1);
            request.setHeader("Sec-WebSocket-Key2", secKey2);
            
        } else {
            request = new GetRequest(uri.toString());
        }
        
        if (protocol != null) {
            if (CLIENT_USING_SEC_KEY) {
                request.setHeader("Sec-WebSocket-Protocol", protocol);
            } else {
                request.setHeader("WebSocket-Protocol", protocol);
            }
        }
        
        request.setHeader("Upgrade", "WebSocket");
        request.setHeader("Connection", "Upgrade");
        
        URL originURL = request.getRequestUrl();
        String origin = originURL.getProtocol() + "//" + originURL.getHost();
        if (originURL.getPort() != -1) {
            origin += ":" + originURL.getPort();
        }
        request.setHeader("Origin", origin);
        
        
        IHttpResponse response = httpClientEndpoint.call(request);


        if (response.getStatus() != 101) {
            if (response.getStatus() == 501) {
                if (response.hasBody()) {
                    throw new UnsupportedProtocolException(response.getBody().toString());
                } else {
                    throw new UnsupportedProtocolException();
                }
            } else {
                throw new IOException(response.getStatus() + " " + response.getReason());
            }
        }
        
        
        if (challengeMd5 != null) {
            boolean match = true;
            byte[] challengeMd5Response = response.getBody().readBytesByLength(16);
            for (int i = 0; i < challengeMd5.length; i++) {
                if (challengeMd5[i] != challengeMd5Response[i]) {
                    match = false;
                }
            }
            
            if (!match) {
                throw new IOException("server returns wrong md5");
            }
        }
        
        HttpClientConnection httpCon = (HttpClientConnection) HttpUtils.getConnectionFromAttribute(response.getResponseHeader());

        
        return new HandeshakeResult(httpCon, request.getRequestHeader(), response.getResponseHeader());
    }
    
   
    
    private static byte[] generateSecKey(int length, long key, int numSpaces) {

        while (true) {
            byte[] num = Long.toString(key).getBytes();
            int numIdx = 0;
            
            byte[] data = new byte[length];
            
            for (int i = 0; i < data.length; i++) {
                
                boolean isPrintNum = ((RANDOM.nextInt(length / num.length) % 2) == 1);
                if (isPrintNum && (numIdx < num.length)) {
                    data[i] = num[numIdx];
                    numIdx++;
                
                } else {
                    // generated random char (printable chars only)
                    data[i] = (byte) (33 + RANDOM.nextInt(15));
                }
            }
        
            
            // add spaces
            int spacesIdx = 0;
            while (spacesIdx < numSpaces) {
                int idx = 1 + RANDOM.nextInt(length - 3);
                if ((data[idx] == 32) || ((data[idx] >= 48) && (data[idx] <= 57))) {
                    continue;
                }
                
                data[idx] = 32;
                spacesIdx++;
            }
            
            if (numIdx == num.length) {
                return data;
            }
        }
    }
    
    
    private static byte[] computeMD5(int key1, int key2, long num) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putInt(key1);
        buf.putInt(key2);
        buf.putLong(num);
        buf.flip();
        
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(buf.array());
            return md5.digest();
        } catch (NoSuchAlgorithmException nse) {
            throw new IOException(nse.toString());
        }
    }
    
    
    
    private static final class HandeshakeResult {
        
        private HttpClientConnection con;
        private IHttpRequestHeader upgradeRequestHeader;
        private IHttpResponseHeader upgradeResponseHeader;
        
        public HandeshakeResult(HttpClientConnection con, IHttpRequestHeader upgradeRequestHeader, IHttpResponseHeader upgradeResponseHeader) {
            this.con = con;
            this.upgradeRequestHeader = upgradeRequestHeader;
            this.upgradeResponseHeader = upgradeResponseHeader;
        }
    }



    public WebSocketConnection(HttpServerConnection httpConnection, IWebSocketHandler webSocketHandler, IHttpExchange exchange) throws IOException {
        this(httpConnection, webSocketHandler, new UpgradeResponseSender(exchange));
    }
    
    private WebSocketConnection(HttpServerConnection httpConnection, IWebSocketHandler webSocketHandler, UpgradeResponseSender upgradeHandler) throws IOException {
        this(httpConnection, webSocketHandler, upgradeHandler.getRequestHeader(), upgradeHandler.getResponseHeader(), upgradeHandler);
    }
    
    
    
    private static INonBlockingConnection convertToTcpConnection(AbstractHttpConnection httpConnection) throws IOException {
        INonBlockingConnection tcpCon = httpConnection.getUnderlyingTcpConnection();
        tcpCon.setHandler(null);
        
        return tcpCon;
    }
    
    
    private WebSocketConnection(HttpServerConnection httpConnection, IWebSocketHandler webSocketHandler, IHttpRequestHeader upgradeRequestHeader, IHttpResponseHeader upgradeResponseHeader, UpgradeResponseSender upgradeResponseSender) throws IOException {
        this(convertToTcpConnection(httpConnection), upgradeResponseSender, webSocketHandler, upgradeResponseSender, upgradeRequestHeader, upgradeResponseHeader);
    }
    
    private static final class UpgradeResponseSender implements IPostConnectInterceptor, IPostWriteInterceptor {

        private final IHttpExchange exchange;
        private final AtomicBoolean isUpgradeSent = new AtomicBoolean(false);
        private final byte[] md5;
        
        private final IHttpResponseHeader responseHeader;
        
        public UpgradeResponseSender(IHttpExchange exchange) throws IOException {
            this.exchange = exchange;
            
            IHttpRequest request = exchange.getRequest();

            String webSocketLocation; 
            if (request.isSecure()) {
                webSocketLocation = "wss://" + request.getHost() + request.getRequestURI();
            } else {
                webSocketLocation = "ws://" + request.getHost() + request.getRequestURI();
            }

            
            if (request.getHeader("Sec-WebSocket-Key1") != null) {
                String protocol = request.getHeader("Sec-WebSocket-Protocol"); 
                String webSocketOrigin = request.getHeader("Origin");
                
                String secKey1 = request.getHeader("Sec-WebSocket-Key1");
                long obfuscatedKey1 = extractNumber(secKey1.getBytes());
                int numSpacesKey1 = computeNumSpaces(secKey1);
                int key1 = (int) (obfuscatedKey1 / numSpacesKey1); 
                
                String secKey2 = request.getHeader("Sec-WebSocket-Key2");
                long obfuscatedKey2 = extractNumber(secKey2.getBytes());
                int numSpacesKey2 = computeNumSpaces(secKey2);
                int key2 = (int) (obfuscatedKey2 / numSpacesKey2);
                
                long challenge = request.getBody().readLong();
                md5 = computeMD5(key1, key2, challenge);
                
                
                responseHeader = new HttpResponseHeader(101);
                responseHeader.setReason("Web Socket Protocol Handshake");
                
                responseHeader.setHeader("Upgrade", "WebSocket");
                responseHeader.setHeader("Connection", "Upgrade");
                responseHeader.setHeader("Sec-WebSocket-Origin", webSocketOrigin);
                responseHeader.setHeader("Sec-WebSocket-Location", webSocketLocation);
                
                if (protocol != null) {
                    responseHeader.setHeader("Sec-WebSocket-Protocol", protocol);
                }

            } else {
                md5 = null;
                String protocol = request.getHeader("WebSocket-Protocol"); 
                String webSocketOrigin = request.getHeader("Origin");
                
                responseHeader = new HttpResponseHeader(101);
                responseHeader.setReason("Web Socket Protocol Handshake");
                
                responseHeader.setHeader("Upgrade", "WebSocket");
                responseHeader.setHeader("Connection", "Upgrade");
                responseHeader.setHeader("WebSocket-Origin", webSocketOrigin);
                responseHeader.setHeader("WebSocket-Location", webSocketLocation);
                
                if (protocol != null) {
                    responseHeader.setHeader("WebSocket-Protocol", protocol);
                }
            }
        }
        
        
        public void onConnectException(IOException ioe) {
            if (ioe instanceof UnsupportedProtocolException) {
                exchange.sendError(501, ioe.getMessage());
            } else {
                exchange.sendError(501);
            }
        }
        
             
        public void onPostConnect() throws IOException {
            sentUpgradeIfNecessary();
        }
        
        public void onPreWrite() throws IOException {
            sentUpgradeIfNecessary() ;
        }
        
        private void sentUpgradeIfNecessary() throws IOException {
            if (!isUpgradeSent.getAndSet(true)) {
                HttpResponse response;
                if (md5 == null) {
                    response = new HttpResponse(responseHeader);
                } else {
                    response = new HttpResponse(responseHeader, DataConverter.toByteBuffer(md5).array());
                    response.removeHeader("Content-Length");
                }
                
                exchange.send(response);
            }
        }
        
        IHttpRequestHeader getRequestHeader() {
            return exchange.getRequest().getRequestHeader();
        }
        
        IHttpResponseHeader getResponseHeader() {
            return responseHeader;
        }
    }
    
    private static int computeNumSpaces(String key) {
        int numSpaces = 0;
        
        for (int b : key.getBytes()) {
            if (b == (int) ' ') {
                numSpaces++;
            }
        }

        return numSpaces;
    }
    
    private static long extractNumber(byte[] key) {
        StringBuilder sb = new StringBuilder();
        for (int b : key) {
            if ((b >= 48) && (b <= 57)) {
                sb.append((char) b);
            }
        }

        return Long.parseLong(sb.toString());
    }
    
   
   
    private WebSocketConnection(INonBlockingConnection tcpConnection, IPostWriteInterceptor connectInterceptor, IWebSocketHandler webSocketHandler, IPostConnectInterceptor postConnectInterceptor, IHttpRequestHeader upgradeRequestHeader, IHttpResponseHeader upgradeResponseHeader) throws IOException {
        this.interceptor = connectInterceptor;
        this.tcpConnection = tcpConnection;
        
        this.upgradeRequestHeader = upgradeRequestHeader;
        this.upgradeResponseHeader = upgradeResponseHeader;
              
        
        executor = HttpUtils.newMultimodeExecutor(tcpConnection.getWorkerpool());
        setMessageHandler(webSocketHandler, postConnectInterceptor);

        protocolHandler.onConnect(tcpConnection);
        tcpConnection.setHandler(protocolHandler);
	}

    
    /**
     * {@inheritDoc}
     */
    public String getProtocol() {
        return getUpgradeResponseHeader().getHeader("WebSocket-Protocol");        
    }

    /**
     * {@inheritDoc}
     */
    public String getWebSocketLocation() {
        return getUpgradeResponseHeader().getHeader("WebSocket-Location");
    }
    
    /**
     * {@inheritDoc}
     */
    public String getWebSocketOrigin() {
        return getUpgradeResponseHeader().getHeader("WebSocket-Origin");
    }

    
    public IHttpRequestHeader getUpgradeRequestHeader() {
        return upgradeRequestHeader;
    }
    
    
    public IHttpResponseHeader getUpgradeResponseHeader() {
        return upgradeResponseHeader;
    }
    
    
    public void destroy() {
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
    
    
    public INonBlockingConnection getUnderlyingTcpConnection() {
        return tcpConnection;
    }
   
    
    public int availableMessages() {
        synchronized (inQueue) {
            return inQueue.size();
        }
    }
    
    int getInQueueVersion() {
        synchronized (inQueue) {
            return inQueueVersion;
        }
    }

    
    @Override
    public String toString() {
        return tcpConnection.toString();
    }

    
    /**
     * {@inheritDoc}
     */
    public TextMessage readTextMessage() throws IOException, SocketTimeoutException {
        WebSocketMessage msg = readMessage();

        if (msg.isTextMessage()) {
            return (TextMessage) msg;
        } else {
            throw new IOException("got a " + msg.getClass().getSimpleName() +  " message");
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public WebSocketMessage readMessage() throws BufferUnderflowException, SocketTimeoutException, ClosedChannelException, IOException {
        long start = System.currentTimeMillis();
        long remainingTime = receiveTimeoutSec;

        do {
            synchronized (inQueue) {
                IOException ioe = exceptionRef.getAndSet(null); 
                if (ioe != null) {
                    throw ioe;
                }
                
                if (isDisconnected.get()) {
                    throw new ClosedChannelException();
                }
                
                if (inQueue.isEmpty()) {
                    try {
                        inQueue.wait(remainingTime);
                    } catch (InterruptedException ie) { 
                        // Restore the interrupted status
                        Thread.currentThread().interrupt();
                    }
                } else {
                    inQueueVersion++;
                    return inQueue.remove(0);
                }
            }

            remainingTime = HttpUtils.computeRemainingTime(start, receiveTimeoutSec);
        } while (remainingTime > 0);
        

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("receive timeout " + receiveTimeoutSec + " sec reached. throwing timeout exception");
        }

        throw new SocketTimeoutException("timeout " + receiveTimeoutSec + " sec reached");
    }


    public int writeMessage(TextMessage msg) throws IOException {
        return writeMessage((WebSocketMessage) msg);
    }
    
    
    public int writeMessage(WebSocketMessage msg) throws IOException {
        if (isCloseMsgSent.get()) {
            throw new ClosedChannelException();
        }
        
        return writeMessageIgnoreClose(msg);
    }
    
    
    public void writeMessage(TextMessage msg, IWriteCompleteHandler completeHandler) throws IOException {
        writeMessage((WebSocketMessage) msg, completeHandler); 
    }
    
    private int writeMessageIgnoreClose(WebSocketMessage msg) throws IOException {
        if (interceptor != null) {
            interceptor.onPreWrite();
        }
        return msg.writeTo(this, null);
    }
    
    public void writeMessage(WebSocketMessage msg, IWriteCompleteHandler writtenHandler) throws IOException {
        if (isCloseMsgSent.get()) {
            throw new ClosedChannelException();
        }

        if (interceptor != null) {
            interceptor.onPreWrite();
        }
        msg.writeTo(this, writtenHandler);
    }
    
    
    public void closeQuitly() {
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
    
    
    public void close() throws IOException {
        if (!isCloseMsgSent.getAndSet(true)) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] sending close message");
            }
            writeMessageIgnoreClose(new CloseMessage());
        }
    }
    
    
    void processNonthreaded(Runnable task) {
        executor.processNonthreaded(task);
    }
    
    void processMultithreaded(Runnable task) {
        executor.processMultithreaded(task);
    }
    
    

    private void setMessageHandler(IWebSocketHandler webSocketHandler, IPostConnectInterceptor postConnectInterceptor) throws IOException {
        
        synchronized (webSocketHandlerGuard) {
            if (webSocketHandlerAdapter != null) {
                webSocketHandlerAdapter.onDisconnect(this);
            }
            
            webSocketHandlerAdapter = new WebSocketHandlerAdapter(webSocketHandler, postConnectInterceptor);
            webSocketHandlerAdapter.onConnect(this);
        }
    }

    
  

    public boolean isOpen() {
        return !isCloseMsgSent.get() && tcpConnection.isOpen();
    }
    
    public boolean isServerSide() {
        return tcpConnection.isServerSide();
    }
    
    public INonBlockingConnection getTcpConnection() {
        return tcpConnection;
    }
    
    
    
    public void setAttachment(Object obj) {
        attachmentRef.set(obj);
    }
    
    public Object getAttachment() {
        return attachmentRef.get();
    }
    
    public long getConnectionTimeoutMillis() {
        return tcpConnection.getConnectionTimeoutMillis();
    }
    
    public void setConnectionTimeoutMillis(long timeoutMillis) {
        tcpConnection.setConnectionTimeoutMillis(timeoutMillis);
    }
    
    public long getRemainingMillisToConnectionTimeout() {
        return tcpConnection.getRemainingMillisToConnectionTimeout();
    }
    
    public long getIdleTimeoutMillis() {
        return tcpConnection.getIdleTimeoutMillis();
    }
    
    public void setIdleTimeoutMillis(long timeoutInMillis) {
        tcpConnection.setIdleTimeoutMillis(timeoutInMillis);
    }
    
    public long getRemainingMillisToIdleTimeout() {
        return tcpConnection.getRemainingMillisToIdleTimeout();
    }
    
    public String getId() {
        return tcpConnection.getId();
    }
    
    public int getLocalPort() {
        return tcpConnection.getLocalPort();
    }
    
    public InetAddress getLocalAddress() {
        return tcpConnection.getLocalAddress();
    }
    
    public InetAddress getRemoteAddress() {
        return tcpConnection.getRemoteAddress();
    }
    
    public int getRemotePort() {
        return tcpConnection.getRemotePort();
    }
    
    public Object getOption(String name) throws IOException {
        return tcpConnection.getOption(name);
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Class> getOptions() {
        return tcpConnection.getOptions();
    }
    
    public void setOption(String name, Object value) throws IOException {
        tcpConnection.setOption(name, value);
    }
    
    
    
 
    
    @Execution(Execution.NONTHREADED)
    private final class WebSocketProtocolHandler implements IConnectHandler, IDataHandler, IDisconnectHandler {


        // network data
        private ByteBuffer rawBuffer = null;

        
        public boolean onConnect(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            synchronized (webSocketHandlerGuard) {
                if (webSocketHandlerAdapter != null) {
                    webSocketHandlerAdapter.onConnect(WebSocketConnection.this);
                }
            }
            
            return true;
        }
        
        
        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
            
            if (connection.isOpen()) {
                // copying available network data into raw data buffer
                int available = connection.available();
                
                ByteBuffer[] data = null;
                if (available > 0) {
                    data = connection.readByteBufferByLength(available);
                }
                onData(data);
            } 
            
            return true;
        }
        
        
        void onData(ByteBuffer[] data) throws IOException {
            if (data == null) {
                if (rawBuffer == null) {
                    rawBuffer = ByteBuffer.allocate(0);
                }
            } else {
                if (rawBuffer == null) {
                    rawBuffer = HttpUtils.merge(data);
                } else {
                    rawBuffer = HttpUtils.merge(rawBuffer, data);
                }
            }  
            
            parse(rawBuffer);
            
            if (!rawBuffer.hasRemaining()) {
                rawBuffer = null;
            }
        }
        
        
        void parse(ByteBuffer buffer) throws IOException {

            while (buffer.hasRemaining()) {
                
                WebSocketMessage msg = WebSocketMessage.parse(buffer);
                
                if (msg == null) {
                    return;
                    
                } else {
                    
                    if (msg.isTextMessage()) {
                        synchronized (inQueue) {
                            inQueueVersion++;
                            inQueue.add(msg);
                            inQueue.notifyAll();
                        }
                        
                        synchronized (webSocketHandlerGuard) {
                            if (webSocketHandlerAdapter != null) {
                                webSocketHandlerAdapter.onMessage(WebSocketConnection.this);
                            }
                        }
                    } else if (msg.isCloseMessage()) {
                        if (isCloseMsgSent.get()) {
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.fine("[" + getId() + "] echo close msg reveived. Destroying connection");
                            }
                            writeMessageIgnoreClose(msg);
                            destroy();
                            
                        // peer initiated close    
                        } else {
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.fine("[" + getId() + "] close msg reveived. echoing it and destroying connection");
                            }
                            isCloseMsgSent.set(true);
                            writeMessageIgnoreClose(msg);
                            destroy();
                        }
                        
                    } else {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("[" + getId() + "] binary message received. The ws draft does not longer allow binary messages. Ignoring it");
                        }
                    }
                }
            }
        }
        
        
        public boolean onDisconnect(INonBlockingConnection connection) throws IOException {
            
            synchronized (inQueue) {
                isDisconnected.set(true);
                
                if ((rawBuffer != null) && rawBuffer.hasRemaining()) {
                    exceptionRef.set(new IOException("connection terminated while receiving data"));
                }
                
                inQueue.notifyAll();
            }
          
            
            synchronized (webSocketHandlerGuard) {
                if (webSocketHandlerAdapter != null) {
                    webSocketHandlerAdapter.onDisconnect(WebSocketConnection.this);
                }
            }
            
            return true;
        }   
    }
    
    
    private static interface IPostWriteInterceptor {
        
        void onPreWrite() throws IOException ;
    }
}
	