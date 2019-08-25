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

import java.io.File;



import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


import org.xlightweb.AbstractHttpConnection.IMultimodeExecutor;
import org.xsocket.DataConverter;
import org.xsocket.Execution;
import org.xsocket.connection.IWriteCompletionHandler;
import org.xsocket.connection.IConnection.FlushMode;



/**
 * file  body data source
 * 
 * @author grro@xlightweb.org
 *
 */
final class MultipartByteRangeFileDataSource extends NonBlockingBodyDataSource implements IForwardable {

    private final List<MultipartByteRangeFilePart> parts = new ArrayList<MultipartByteRangeFilePart>();
    private final String dashBoundary;
    private final String closeDelimiter;
   
    private int bodySize = 0;
    
    
    MultipartByteRangeFileDataSource(IHttpMessageHeader header, IMultimodeExecutor executor, File file, String[] ranges, String boundary) throws IOException {
        super(header, executor);
        
	    dashBoundary = "--" + boundary;
	    closeDelimiter = "\r\n" + dashBoundary + "--";

	    String contentType = HttpUtils.resolveContentTypeByFileExtension(file);
        int fileLength = (int) file.length();
        int bodySize = 0;
        
        for (String range : ranges) {
            int[] positions = HttpUtils.computeFromRangePosition(range, fileLength);
            
            MultipartByteRangeFilePart part = new MultipartByteRangeFilePart(header, new FileDataSource(header, executor, file, range));
            part.setHeader("Content-Range", "bytes " + positions[0] + "-" + positions[1] + "/" + fileLength);
            part.setContentType(contentType);
            
    		parts.add(part);
            
            bodySize += positions[1] - positions[0] + 1;        	
        }
        
    }
    
    
    long getLength() {
    	return bodySize;
    }

    public void forwardTo(final BodyDataSink bodyDataSink) throws IOException {
    	bodyDataSink.setFlushmode(FlushMode.ASYNC);
    	
    	Iterator<MultipartByteRangeFilePart> it = parts.iterator();
    	new PartWriter(it, true).forwardTo(bodyDataSink);
    }

    
    

    @Execution(Execution.NONTHREADED)
    private final class PartWriter implements IForwardable, IWriteCompletionHandler {
    	
    	private final AtomicReference<BodyDataSink> dataSinkRef = new AtomicReference<BodyDataSink>();
    	private final Iterator<MultipartByteRangeFilePart> it;
    	private final boolean isFirstPart;
    	private final AtomicReference<MultipartByteRangeFilePart> partRef = new AtomicReference<MultipartByteRangeFilePart>();
    	
    	public PartWriter(Iterator<MultipartByteRangeFilePart> it, boolean isFirstPart) {
    		this.it = it;
    		this.isFirstPart = isFirstPart;
		}
    	
    	public void forwardTo(BodyDataSink dataSink) throws IOException {
    		
    		if (it.hasNext()) {
    			MultipartByteRangeFilePart part = it.next();
    			dataSinkRef.set(dataSink);
    			partRef.set(part);
    			
    			if (isFirstPart) {
    				write(dataSink, dashBoundary + "\r\n", this);
    			} else {
    				write(dataSink, "\r\n" + dashBoundary + "\r\n", this);
    			}
    			
    		} else {
				write(dataSink, closeDelimiter + "\r\n", new WriteCompletionHandler(dataSink));
    		}
    	}
    	
    	private void write(BodyDataSink dataSink, String header, IWriteCompletionHandler completionHandler) throws IOException {
    		dataSink.write(new ByteBuffer[] { DataConverter.toByteBuffer(header, "utf-8") }, completionHandler);
    	}
    	
		public void onWritten(int written) throws IOException {
			
			IBodyCompleteListener completeListener = new IBodyCompleteListener() {
				
				public void onComplete() throws IOException {
					new PartWriter(it, false).forwardTo(dataSinkRef.get());
				}
			};
			
    		partRef.get().forwardTo(dataSinkRef.get(), completeListener);
			
		}
		
		public void onException(IOException ioe) {
			destroy(ioe.toString());
		}
    }
  
 	private static final class WriteCompletionHandler implements IWriteCompletionHandler {
		
		private final BodyDataSink dataSink;
		
		public WriteCompletionHandler(BodyDataSink dataSink) {
			this.dataSink = dataSink;
		}
		
		public void onWritten(int written) throws IOException {
			dataSink.close();
		}
		
		public void onException(IOException ioe) {
			dataSink.destroy();
		}
	};
    
    boolean isFileDataSource() {
        return true;
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
    String getId() {
        return Integer.toString(hashCode());
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
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
    	for (MultipartByteRangeFilePart part : parts) {
        	sb.append(dashBoundary + "\r\n");
    		sb.append(part);
    	}
    	
    	sb.append(closeDelimiter + "\r\n");        
        return sb.toString();
    }   
        
}