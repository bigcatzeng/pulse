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


import org.xsocket.DataConverter;
import org.xsocket.Execution;
import org.xsocket.connection.IWriteCompletionHandler;




/**
 * A file part
 * 
 * @author grro@xlightweb.org
 */ 
final class MultipartByteRangeFilePart extends Header implements IPart {
    
	private final IHeader header;
    private final FileDataSource dataSource;
     
    
    MultipartByteRangeFilePart(IHeader header, FileDataSource dataSource) {
    	this.header = header;
    	this.dataSource = dataSource;
    }
    
    
	@SuppressWarnings("deprecation")
    public BlockingBodyDataSource getBlockingBody() throws IOException {
	    return new BlockingBodyDataSource(dataSource);
	}

    public BodyDataSource getBody() throws IOException {
	    return new BodyDataSource(dataSource);
	}
	
	public NonBlockingBodyDataSource getNonBlockingBody() throws IOException {
		return dataSource;
	}

	public boolean hasBody() {
		return (dataSource != null);
	}  

	public boolean hasMultipartBody() {
		return hasBody();
	}

	public IHeader getPartHeader() {
		return header;
	}
	
	public void forwardTo(final BodyDataSink bodyDataSink, final IBodyCompleteListener completeListener) throws IOException {
		
		StringBuilder sb = new StringBuilder();
		writeHeadersTo(sb);

		IWriteCompletionHandler ch = new IWriteCompletionHandler() {
			
			@Execution(Execution.NONTHREADED)
			public void onWritten(int written) throws IOException {
				dataSource.forwardTo(bodyDataSink, completeListener);
			}
			
			public void onException(IOException ioe) {
				bodyDataSink.destroy();
				dataSource.destroy();
			}
		};
		bodyDataSink.write(new ByteBuffer[] { DataConverter.toByteBuffer(sb.toString() + "\r\n", "utf-8") }, ch);
	}
}
