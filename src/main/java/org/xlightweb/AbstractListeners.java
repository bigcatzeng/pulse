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
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xlightweb.AbstractHttpConnection.IMultimodeExecutor;
import org.xsocket.Execution;



/**
 * listeners managment
 *  
 * @author grro@xlightweb.org
 */
abstract class AbstractListeners<T extends Object> {
	
    private static final Logger LOG = Logger.getLogger(AbstractListeners.class.getName());

    
    private final ArrayList<ListenerHolder> listenerHolders = new ArrayList<ListenerHolder>();
        

    /**
     * add listener
     *  
     * @param listener the listener to add
     */
    void addListener(T listener, boolean callAndRemove, IMultimodeExecutor executor, int executionMode) {
        ListenerHolder holder = new ListenerHolder(listener, executionMode);
        synchronized (listenerHolders) {
            listenerHolders.add(holder);
        }
        
        if (callAndRemove) {
            callAndRemoveListener(holder, executor);
        }
    }
    
    

    private boolean removeListener(ListenerHolder holder) {
        synchronized (listenerHolders) {
            return listenerHolders.remove(holder);
        }
    }
    
    
    /**
     * remove a listener
     * 
     * @param listener the listener to remove
     * @return true, if the listener is removed
     */
    boolean removeListener(T listener) {
        synchronized (listenerHolders) {
            for (ListenerHolder holder : listenerHolders) {
                if (holder.getListener() == listener) {
                    return removeListener(holder);
                }
            }
        }
        return false;
    }
    
    /**
     * calls and remove the registered listeners
     */
    @SuppressWarnings("unchecked")
    protected void callAndRemoveListeners(IMultimodeExecutor executor) {
        ArrayList<ListenerHolder> listenersHolderCopy = null;
        synchronized (listenerHolders) {
            listenersHolderCopy = (ArrayList<ListenerHolder>) listenerHolders.clone();
        }
        
        for (ListenerHolder holder : listenersHolderCopy) {
            callAndRemoveListener(holder, executor);
        }
    }
    
    
    private void callAndRemoveListener(ListenerHolder holder, IMultimodeExecutor executor) {
        removeListener(holder);
        holder.call(executor);
    }
    
    
    abstract void onCall(T listener) throws IOException;
    
    
    private final class ListenerHolder {
        
        private final int executionMode;
        private final T listener;
        private final AtomicBoolean isCalled = new AtomicBoolean(false);
        
        public ListenerHolder(T listener, int executionMode) {
            this.listener = listener;
            this.executionMode = executionMode;
        }
        
        T getListener() {
            return listener;
        }
        
        void call(IMultimodeExecutor executor) {
            if (!isCalled.getAndSet(true)) {
                try {
                    
                    if (executionMode == HttpUtils.EXECUTIONMODE_UNSYNCHRONIZED) {
                        onCall(listener);
                        
                    } else {
                        Runnable task = new Runnable() {
                            
                            public void run() {
                                try {
                                    onCall(listener);
                                } catch (IOException ioe) {
                                    if (LOG.isLoggable(Level.FINE)) {
                                        LOG.fine("error occured by calling " + listener + " " + ioe.toString());
                                    }
                                }
                            }
                        };  
                        
                        if (executionMode == Execution.MULTITHREADED) {
                            executor.processMultithreaded(task);
                        } else {
                            executor.processNonthreaded(task);
                        }
                    }
                } catch (IOException ioe) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("error occured by calling " + listener + " " + ioe.toString());
                    }
                }
            }
        }
    }
    
    
    static final class CloseListeners extends AbstractListeners<IBodyCloseListener> {
        
        @Override
        void onCall(IBodyCloseListener listener) throws IOException {
            listener.onClose();            
        }
    }
    
    
    static final class DestroyListeners extends AbstractListeners<IBodyDestroyListener> {
        
        @Override
        void onCall(IBodyDestroyListener listener) throws IOException {
            listener.onDestroyed();            
        }
    }
}
