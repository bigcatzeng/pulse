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


import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IWriteCompletionHandler;



/**
 * 
 * @author grro@xlightweb.org
 */
final class WebSocketMessageCompleteHandlerAdapter implements IWriteCompletionHandler {
    
    private static final Logger LOG = Logger.getLogger(WebSocketMessageCompleteHandlerAdapter.class.getName());

    
    @SuppressWarnings("unchecked")
    private static final Map<Class, MessageCompleteHandlerInfo> completeHandlerInfoCache = ConnectionUtils.newMapCache(25);
    private static final MessageCompleteHandlerInfo EMPTY_COMPLETEHANDLER_INFO = new MessageCompleteHandlerInfo(null);
    
    private final WebSocketConnection con;
    private final IWriteCompleteHandler handler;
    private final MessageCompleteHandlerInfo handlerInfo;
    
    
    public WebSocketMessageCompleteHandlerAdapter(WebSocketConnection con, IWriteCompleteHandler handler) {
        this.con = con;
        this.handler = handler;
        this.handlerInfo = getMessageCompleteHandlerInfo(handler);
    }
    
    
    public void onWritten(final int written) throws IOException {
        
        if (handler != null) {
            Runnable task = new Runnable() {
                public void run() {
                    try {
                        handler.onWritten(written);
                    } catch (IOException ioe) {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("[" + con.getId() + "] error occured by calling write complete handler " + ioe.toString());
                        }
                        con.destroy();
                    }
                }
            };
            
            if (handlerInfo.isOnWrittenMultithreaded()) {
                con.processMultithreaded(task);
            } else {
                con.processNonthreaded(task);
            }
            con.processNonthreaded(task);
        }
    }

    
    public void onException(final IOException ioe) {
        
        if (handler != null) {
            Runnable task = new Runnable() {
                public void run() {
                    handler.onException(ioe);
                }
            };
            
            if (handlerInfo.isOnErrorMultithreaded()) {
                con.processMultithreaded(task);
            } else {
                con.processNonthreaded(task);
            }
            con.processNonthreaded(task);
        }
    }
    

    
    
    private static MessageCompleteHandlerInfo getMessageCompleteHandlerInfo(IWriteCompleteHandler handler) {
        if (handler == null) {
            return EMPTY_COMPLETEHANDLER_INFO;
        }

        MessageCompleteHandlerInfo info = completeHandlerInfoCache.get(handler.getClass());

        if (info == null) {
            info = new MessageCompleteHandlerInfo(handler.getClass());
            completeHandlerInfoCache.put(handler.getClass(), info);
        }

        return info;
    }
    
    
    
    static final class MessageCompleteHandlerInfo  {
        
        private final boolean isOnWrittenMultithreaded;
        private final boolean isOnErrorMultithreaded;

        @SuppressWarnings("unchecked")
        MessageCompleteHandlerInfo(Class clazz) {
            
            if ((clazz != null)) {
                boolean isMultiThreaded = HttpUtils.isHandlerMultithreaded(clazz, true);

                isOnWrittenMultithreaded = HttpUtils.isMethodMultithreaded(clazz, "onWritten", isMultiThreaded, int.class);
                isOnErrorMultithreaded = HttpUtils.isMethodMultithreaded(clazz, "onException", isMultiThreaded, IOException.class);
                
            } else { 
                isOnWrittenMultithreaded = false;
                isOnErrorMultithreaded = false;
            }
        }


        public boolean isOnWrittenMultithreaded() {
            return isOnWrittenMultithreaded;
        }
        
        public boolean isOnErrorMultithreaded() {
            return isOnErrorMultithreaded;
        }
    }
}
