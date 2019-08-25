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



import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xlightweb.GetRequest;
import org.xlightweb.HttpUtils;
import org.xlightweb.IBodyCompleteListener;
import org.xlightweb.IBodyDataHandler;
import org.xlightweb.IBodyDestroyListener;
import org.xlightweb.IHttpResponse;
import org.xlightweb.NonBlockingBodyDataSource;
import org.xlightweb.client.HttpClient;
import org.xsocket.DataConverter;
import org.xsocket.Execution;



/**
 * EventStreamDataSource
 * 
 * <br/><br/><b>This is an experimental implementation of the HTML5 draft and subject to change</b>
 * 
 * 
 * @author grro
 */
public final class EventDataSource implements IEventDataSource {

    private static final Logger LOG = Logger.getLogger(EventDataSource.class.getName());


    // reconnect
    private static final int DEFAULT_MAX_RECONNECT_TRIALS = Integer.parseInt(System.getProperty("org.xlightweb.eventdatasource.maxreconnectrials", "5"));
    private static final int DEFAULT_RECONNECT_TIME_MILLIS = 10 * 1000;
    private int reconnectionTimeMillis = DEFAULT_RECONNECT_TIME_MILLIS;
    private final AtomicInteger reconnectTrials = new AtomicInteger(0);
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    private final AtomicInteger numReconnects = new AtomicInteger();
    private long timeLastConnectTrial = 0;
    
    
    // read timeout
    public static final int DEFAULT_READ_TIMEOUT_MILLIS = Integer.MAX_VALUE;


    // connection related objects
    private final HttpClient httpClient;
    private final String uriString;
    private final Object bodyDataSourceGuard = new Object();
    private NonBlockingBodyDataSource bodyDataSource;

    // flags
    private final AtomicBoolean isOpen = new AtomicBoolean(true);

    // events
    private boolean isIgnoreCommentMessages = true;
    private final List<Event> inQueue = new ArrayList<Event>();
    private int inQueueVersion = 0;
    private AtomicReference<String> lastEventIdRef = new AtomicReference<String>();
        
    
    // additional headers
    private final String[] headerlines; 
    
    // event handler adapter
    private final EventStreamHandlerAdapter webEventHandlerAdapter;
    
    
    public EventDataSource(HttpClient httpClient, String uriString, IEventHandler webEventHandler) throws MalformedURLException, IOException {
        this(httpClient, uriString, true, webEventHandler);
    }
    
    public EventDataSource(HttpClient httpClient, String uriString, boolean isIgnoreCommentMessage, IEventHandler webEventHandler, String... headerlines) throws MalformedURLException, IOException {
        this(httpClient, uriString, isIgnoreCommentMessage, webEventHandler, DEFAULT_MAX_RECONNECT_TRIALS, headerlines);
    }
    
    EventDataSource(HttpClient httpClient, String uriString, boolean isIgnoreCommentMessage, IEventHandler webEventHandler, int maxReconnectTrials, String... headerlines) throws MalformedURLException, IOException {
        this.httpClient = httpClient;
        this.uriString = uriString;
        this.isIgnoreCommentMessages = isIgnoreCommentMessage;
        this.headerlines = headerlines;
        
        webEventHandlerAdapter = new EventStreamHandlerAdapter(webEventHandler);
        connect();
    }
    
    
    public boolean isIgnoreCommentMessages() {
        return isIgnoreCommentMessages;
    }
    
    public void setIgnoreCommentMessages(boolean isIgnoreCommentMessages) {
        this.isIgnoreCommentMessages = isIgnoreCommentMessages;
    }
    
    
    
    private void connect() throws MalformedURLException, IOException {
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("open data event stream " + uriString);
        }
        
        GetRequest request = new GetRequest(uriString);
        
        request.setHeader("Accept", "text/event-stream");
        request.setHeader("Cache-Control", "no-cache");
        
        for (String headerline : headerlines) {
            request.addHeaderLine(headerline);
        }
          
        String lastEventId = lastEventIdRef.get();
        if (lastEventId != null) {
            request.setHeader("Last-Event-ID", lastEventId);
        }

        timeLastConnectTrial = System.currentTimeMillis();

        IHttpResponse response = httpClient.call(request);
        
        if ((response.getStatus() < 200) || (response.getStatus() > 299)) {
            throw new IOException("got " + response.getStatus() + " " + response.getReason());
        }
        
        if (!response.getContentType().toLowerCase().startsWith("text/event-stream")) {
            throw new IOException("got content type " + response.getContentType() + " instead text/event-stream");
        }
        
        synchronized (bodyDataSourceGuard) {
            bodyDataSource = response.getNonBlockingBody();
            
            EventHandler eh = new EventHandler();
            bodyDataSource.setDataHandler(eh);
            bodyDataSource.addDestroyListener(eh);
            bodyDataSource.addCompleteListener(eh);
        }
        
        webEventHandlerAdapter.onConnect(this);
    }
    

    
    public void destroy() {
        synchronized (bodyDataSourceGuard) {
            if (isOpen.getAndSet(false)) {
                bodyDataSource.destroy();
            }
        }
        
        webEventHandlerAdapter.onDisconnect(this);
    }


    public void close() throws IOException {
        synchronized (bodyDataSourceGuard) {
            if (isOpen.getAndSet(false)) {
                bodyDataSource.close();
            }
        }
        
        webEventHandlerAdapter.onDisconnect(this);
    }    
    

    public void closeQuitly() {
        try {
            close();
        } catch (IOException ignore) { }
    }


    public String getId() {
        return bodyDataSource.getId();
    }
    
    public String getLastEventId() {
        return lastEventIdRef.get();
    }
    
    public int getReconnectionTimeMillis() {
        return 0;
    }


    public int availableMessages() {
        synchronized (inQueue) {
            return inQueue.size();
        }
    }


    public Event readMessage() throws IOException, SocketTimeoutException, ClosedChannelException {
        return readMessage(DEFAULT_READ_TIMEOUT_MILLIS);
    }
    
    public Event readMessage(int readTimeoutMillis) throws IOException, SocketTimeoutException, ClosedChannelException {
        long start = System.currentTimeMillis();
        long remainingTime = readTimeoutMillis;

        do {
            synchronized (inQueue) {
                
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

            remainingTime = HttpUtils.computeRemainingTime(start, readTimeoutMillis);
        } while (remainingTime > 0);
        

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("receive timeout " + DataConverter.toFormatedDuration(readTimeoutMillis) + " reached. throwing timeout exception");
        }

        throw new SocketTimeoutException("timeout " + DataConverter.toFormatedDuration(readTimeoutMillis) + " reached");
    }
    
    
    public void setReconnectionTimeMillis(int reconnectionTimeMillis) {
        this.reconnectionTimeMillis = reconnectionTimeMillis;
    }
    
    public int getNumReconnects() {
        return numReconnects.get();
    }
    
    
    void processMultithreaded(Runnable task) {
        bodyDataSource.getExecutor().processMultithreaded(task);
    }
    
    void processNonthreaded(Runnable task) {
        bodyDataSource.getExecutor().processNonthreaded(task);
    }
    
    
    int getInQueueVersion() {
        synchronized (inQueue) {
            return inQueueVersion;
        }
    }
    
    private void performReconnect() {
        
        if (!isOpen.get()) {
            return;
        }
        
        // is already reconnecting?
        if (!isReconnecting.getAndSet(true)) {

            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("connection is terminated. Try to reconnect to "  + uriString);
            }

            TimerTask reconnectTask = new TimerTask() {
                
                public void run() {
                    try {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("try reconnect " + uriString);
                        }
                        connect();
                       
                        numReconnects.incrementAndGet();
                        reconnectTrials.set(0);
                        isReconnecting.set(false);
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("reconnected to " + uriString);
                        }
                        
                    } catch (IOException ioe) {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("reconnecting " + uriString + " failed " + ioe.toString());
                        }
                        
                        isReconnecting.set(false);
                        performReconnect();
                    }        
                }
            };
            
            long waitTime = (reconnectionTimeMillis + timeLastConnectTrial) - System.currentTimeMillis();
            if (waitTime > 0) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("reconnecting " + uriString + " in " + DataConverter.toFormatedDuration(waitTime));
                }
                HttpUtils.schedule(reconnectTask, waitTime);
            } else {
                processMultithreaded(reconnectTask);
            }
        }
    }
    

   
    
    @Execution(Execution.NONTHREADED)
    private final class EventHandler implements IBodyDataHandler, IBodyDestroyListener, IBodyCompleteListener {
        
        public boolean onData(NonBlockingBodyDataSource bodyDataSource) throws BufferUnderflowException {
            Event webEvent = null;
            
            try {
                int idx = bodyDataSource.indexOf("\n\n");
                if (idx != -1) {
                    webEvent = Event.parse(bodyDataSource.readStringByLength(idx + 2));
                } else {
                    idx = bodyDataSource.indexOf("\r\n\r\n");
                    if (idx != -1) {
                        webEvent = Event.parse(bodyDataSource.readStringByLength(idx + 4));
                    }
                }

                if (webEvent != null) {
                   
                    // should comment message be ignored? 
                    if (isIgnoreCommentMessages && webEvent.isCommentMessage()) {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("comment message received. ignoring it (property isIgnoreCommentMessages=true)");
                        }
                        return true;
                    }
                   
                    synchronized (inQueue) {
                        lastEventIdRef.set(webEvent.getId());
                        if (webEvent.getRetryMillis() != null) {
                            reconnectionTimeMillis = webEvent.getRetryMillis();
                        }

                        inQueueVersion++;
                        inQueue.add(webEvent);
                        inQueue.notifyAll();
                    }
                    
                   webEventHandlerAdapter.onMessage(EventDataSource.this);
                }

            } catch (IOException ioe) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + bodyDataSource.getId() + "] error occured by parsing event " + ioe.toString());
                }
                bodyDataSource.destroy();
            }
            
            return true;
        }
        
        
        public void onDestroyed() throws IOException {
            performReconnect();
        }
        
        public void onComplete() {
            performReconnect();
        }
    }    
}
	