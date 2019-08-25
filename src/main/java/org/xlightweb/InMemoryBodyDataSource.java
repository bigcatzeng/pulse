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




/**
 * in-memory body data source
 * 
 * @author grro@xlightweb.org
 *
 */
final class InMemoryBodyDataSource extends NonBlockingBodyDataSource {
    
   
    InMemoryBodyDataSource(IHeader header) throws IOException {
        super(header, HttpUtils.newMultimodeExecutor());
    }
 
    InMemoryBodyDataSource(IHeader header, byte[] body) throws IOException {
        this(header, new ByteBuffer[] { ByteBuffer.wrap(body) });
    }
    
    
    InMemoryBodyDataSource(IHeader header, ByteBuffer[] body) throws IOException {
        super(header, HttpUtils.newMultimodeExecutor(), body);
        setComplete();
    }
    
 
   

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return this.getClass().getSimpleName() + "#" + hashCode();
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
}
	    
