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


import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xsocket.DataConverter;



/**
 * 
 * @author grro@xlightweb.org
 */
final class WebSocketHandlerAdapter {
    
    private static final Logger LOG = Logger.getLogger(WebSocketHandlerAdapter.class.getName());
    
    
    private final IPostConnectInterceptor interceptor;
    private final AtomicBoolean isOnConnectCalled = new AtomicBoolean(false);
    private final AtomicBoolean isOnDisconnectCalled = new AtomicBoolean(false);
    private final WebSocketHandlerInfo handlerInfo;
    private final IWebSocketHandler handler;
    
    
    public WebSocketHandlerAdapter(IWebSocketHandler handler, IPostConnectInterceptor interceptor) {
        this.handler = handler;
        this.handlerInfo = HttpUtils.getWebSocketHandlerInfo(handler);
        this.interceptor = interceptor;
    }
    
    
    public void onConnect(final WebSocketConnection con) {
        if ((handler != null) && !isOnConnectCalled.getAndSet(true)) {
            
            if (handlerInfo.isUnsynchronized()) {
                performOnConnect(con);
                
            } else {
                Runnable task = new Runnable() {
                    public void run() {
                       performOnConnect(con);
                    }
                };
                
                if (handlerInfo.isOnConnectMultithreaded()) {
                    con.processMultithreaded(task);
                } else {
                    con.processNonthreaded(task);
                }
            }
        }
    }
    
    
    private void performOnConnect(WebSocketConnection con) {
        try {
            
            try {
                handler.onConnect(con);
                
            } catch (IOException ioe) {
                if (interceptor != null) {
                    interceptor.onConnectException(ioe);
                }
                
            } catch (Exception e) {
                if (interceptor != null) {
                    interceptor.onConnectException(new IOException(e.toString()));
                }
            }
            
            if (interceptor != null) {
                interceptor.onPostConnect();
            }
            
            performOnMessage(con);
            
        } catch (IOException ioe) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + con.getId() + "] closing connection because an error has been occured by performing onConnect of " + handler + " Reason: " + DataConverter.toString(ioe));
            }
            con.closeQuitly();

        } catch (Throwable t) {
            LOG.warning("[" + con.getId() + "] closing connection. Error occured by performing onConnect of " + handler +  " " + t.toString());
            con.closeQuitly();
        } 
    }

    public void onDisconnect(final WebSocketConnection con) throws IOException {
        if ((handler != null) && !isOnDisconnectCalled.getAndSet(true)) {
            
            if (handlerInfo.isUnsynchronized()) {
                performOnConnect(con);

            } else {
                Runnable task = new Runnable() {
                    public void run() {
                        performOnDisconnect(con);
                    }
                };
                
                if (handlerInfo.isOnDisconnectMultithreaded()) {
                    con.processMultithreaded(task);
                } else {
                    con.processNonthreaded(task);
                }
            }
        }        
    }
    
    
    private void performOnDisconnect(WebSocketConnection con) {
        try {
            handler.onDisconnect(con);

        } catch (IOException ioe) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + con.getId() + "] closing connection because an error has been occured by performing onDisconnect of " + handler + " Reason: " + DataConverter.toString(ioe));
            }
            con.closeQuitly();

        } catch (Throwable t) {
            LOG.warning("[" + con.getId() + "] closing connection. Error occured by performing onDisconnect of " + handler +  " " + t.toString());
            con.closeQuitly();
        }
    }
    
    public void onMessage(final WebSocketConnection con) throws IOException {
        
        if (handler != null) {
            
            if (handlerInfo.isUnsynchronized()) {
                performOnMessage(con);

            } else {
                Runnable task = new Runnable() {
                    public void run() {
                        performOnMessage(con);
                    }
                };
                
                if (handlerInfo.isOnMessageMultithreaded()) {
                    con.processMultithreaded(task);
                } else {
                    con.processNonthreaded(task);
                }
                con.processNonthreaded(task);
            }
        }
    }
    
    
    private void performOnMessage(WebSocketConnection con) {
        try {
            while (con.availableMessages() > 0) {
                int ver = con.getInQueueVersion();
                
                handler.onMessage(con);
                if (ver == con.getInQueueVersion()) {
                    return;
                }
            }

        } catch (IOException ioe) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + con.getId() + "] closing connection because an error has been occured by performing onMessage of " + handler + " Reason: " + DataConverter.toString(ioe));
            }
            con.closeQuitly();

        } catch (Throwable t) {
            LOG.warning("[" + con.getId() + "] closing connection. Error occured by performing onMessage of " + handler +  " " + t.toString());
            con.closeQuitly();
        }
    }
    
    
    
    
    static interface IPostConnectInterceptor {
        
        void onConnectException(IOException ioe);
        
        void onPostConnect() throws IOException ;
    }
}
