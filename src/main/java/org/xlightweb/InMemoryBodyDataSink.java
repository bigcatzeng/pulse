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
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xlightweb.AbstractHttpConnection.IMultimodeExecutor;
import org.xsocket.Execution;
import org.xsocket.connection.IWriteCompletionHandler;
import org.xsocket.connection.IConnection.FlushMode;



/**
 * in-memory body data sink
 * 
 * @author grro@xlightweb.org
 *
 */
final class InMemoryBodyDataSink extends BodyDataSinkImplBase {

    private static final Logger LOG = Logger.getLogger(InMemoryBodyDataSink.class.getName());
        
    private final InMemoryBodyDataSource dataSource;
    private final DestroyListener destroyListener = new DestroyListener();
    
    private final String id; 
    
    
    public InMemoryBodyDataSink(String id, IHttpMessageHeader header) throws IOException {
        this(id, new InMemoryBodyDataSource(header), HttpUtils.newMultimodeExecutor());
    }
        
    private InMemoryBodyDataSink(String id, InMemoryBodyDataSource dataSource, IMultimodeExecutor executor) throws IOException {
        super(dataSource.getHeader(), executor);
        
        this.id = id;
        this.dataSource = dataSource;      
        dataSource.addDestroyListenerSilence(destroyListener);
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("[" + getId() + " -> " + dataSource.getId() + "] body data sink created");
        }
    }
    
    public String getId() {
        return id;
    }
    
    NonBlockingBodyDataSource getDataSource() {
        return dataSource;
    }
    
    
    @Override
    protected boolean isNetworkendpoint() {
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    ByteBuffer[] preWrite(ByteBuffer[] buffers) throws IOException {
        if (getFlushmode() == FlushMode.SYNC) {
            return HttpUtils.copy(buffers);
        } else {
            return buffers;
        }
    }


    /**
     * {@inheritDoc}
     */
    ByteBuffer preWrite(ByteBuffer buffer) throws IOException {
        if (getFlushmode() == FlushMode.SYNC) { 
            return HttpUtils.copy(buffer);
        } else {
            return buffer;
        }
    }


    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy(String reason) {
        dataSource.destroy(reason);
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() throws IOException {
        dataSource.setComplete();
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    int getPendingWriteDataSize() {
        return dataSource.size();
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    int onWriteData(ByteBuffer[] dataToWrite, IWriteCompletionHandler completionHandler) throws IOException {
        return dataSource.append(dataToWrite, completionHandler);
    }
    
    
    @Execution(Execution.NONTHREADED)
    private final class DestroyListener implements IBodyDestroyListener {
        
        public void onDestroyed() throws IOException {
            boolean ignoreAppendError = dataSource.isIgnoreAppendError();
            destroy(ignoreAppendError);
        }
    }
}
	    
