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
final class EventStreamHandlerAdapter {
    
    private static final Logger LOG = Logger.getLogger(EventStreamHandlerAdapter.class.getName());
    
    
    private final AtomicBoolean isOnConnectCalled = new AtomicBoolean(false);
    private final AtomicBoolean isOnDisconnectCalled = new AtomicBoolean(false);
    private final IEventHandlerInfo handlerInfo;
    private final IEventHandler handler;
    
    
    public EventStreamHandlerAdapter(IEventHandler handler) {
        this.handler = handler;
        this.handlerInfo = HttpUtils.getWebEventHandlerInfo(handler);
    }
    
    
    public void onConnect(final EventDataSource ds) throws IOException {
        if ((handler != null) && !isOnConnectCalled.getAndSet(true)) {
            
            if (handlerInfo.isUnsynchronized()) {
                performOnConnect(ds);

            } else {
                Runnable task = new Runnable() {
                    public void run() {
                        performOnConnect(ds);
                    }
                };
                
                if (handlerInfo.isOnConnectMultithreaded()) {
                    ds.processMultithreaded(task);
                } else {
                    ds.processNonthreaded(task);
                }
            }
        }        
    }
    
    
    private void performOnConnect(EventDataSource ds) {
        try {
            handler.onConnect(ds);

            performOnMessage(ds);
        } catch (IOException ioe) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + ds.getId() + "] closing data source because an error has been occured by performing onConnect of " + handler + " Reason: " + DataConverter.toString(ioe));
            }
            ds.destroy();

        } catch (Throwable t) {
            LOG.warning("[" + ds.getId() + "] closing data source. Error occured by performing onConnect of " + handler +  " " + t.toString());
            ds.closeQuitly();
        }
    }

    public void onDisconnect(final EventDataSource ds) {
        if ((handler != null) && !isOnDisconnectCalled.getAndSet(true)) {
            
            if (handlerInfo.isUnsynchronized()) {
                performOnDisconnect(ds);

            } else {
                Runnable task = new Runnable() {
                    public void run() {
                        performOnDisconnect(ds);
                    }
                };
                
                if (handlerInfo.isOnDisconnectMultithreaded()) {
                    ds.processMultithreaded(task);
                } else {
                    ds.processNonthreaded(task);
                }
            }
        }        
    }
    
    
    private void performOnDisconnect(EventDataSource ds) {
        try {
            handler.onDisconnect(ds);

        } catch (IOException ioe) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + ds.getId() + "] closing data source because an error has been occured by performing onDisconnect of " + handler + " Reason: " + DataConverter.toString(ioe));
            }
            ds.destroy();

        } catch (Throwable t) {
            LOG.warning("[" + ds.getId() + "] closing data source. Error occured by performing onDisconnect of " + handler +  " " + t.toString());
            ds.closeQuitly();
        }
    }
    
    public void onMessage(final EventDataSource ds) throws IOException {
        if (handler != null) {
            
            if (handlerInfo.isUnsynchronized()) {
                performOnMessage(ds);

            } else {
                Runnable task = new Runnable() {
                    public void run() {
                        performOnMessage(ds);
                    }
                };
                
                if (handlerInfo.isOnMessageMultithreaded()) {
                    ds.processMultithreaded(task);
                } else {
                    ds.processNonthreaded(task);
                }
                ds.processNonthreaded(task);
            }
        }
    }
    
    
    private void performOnMessage(EventDataSource ds) {
        try {
            while (ds.availableMessages() > 0) {
                int ver = ds.getInQueueVersion();
                
                handler.onMessage(ds);
                if (ver == ds.getInQueueVersion()) {
                    break;
                }
            }

        } catch (IOException ioe) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + ds.getId() + "] closing dataSource because an error has been occured by performing onMessage of " + handler + " Reason: " + DataConverter.toString(ioe));
            }
            ds.closeQuitly();

        } catch (Throwable t) {
            LOG.warning("[" + ds.getId() + "] closing data source. Error occured by performing onMessage of " + handler +  " " + t.toString());
            ds.closeQuitly();
        }
    }
}
