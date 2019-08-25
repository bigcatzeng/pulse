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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;


import org.xlightweb.AbstractHttpConnection.IMultimodeExecutor;
import org.xsocket.connection.IWriteCompletionHandler;




/**
 * file body data sink
 * 
 * @author grro@xlightweb.org
 *
 */
final class FileDataSink extends BodyDataSinkImplBase {
	
	private static final Logger LOG = Logger.getLogger(FileDataSink.class.getName());
	

	private final File file;
	private final RandomAccessFile raf;
	private final FileChannel fc;
	
	FileDataSink(IHeader header, IMultimodeExecutor executor, File file) throws IOException {
		super(header, executor);
	
		this.file = file;
		raf = new RandomAccessFile(file, "rw");
		fc = raf.getChannel();
	}
	
	@Override
	int onWriteData(ByteBuffer[] dataToWrite, IWriteCompletionHandler completionHandler) throws IOException {
		int size = HttpUtils.computeRemaining(dataToWrite);
		
		if (size > 0) { 
			try {
				fc.write(dataToWrite);
				
			} catch (IOException ioe) {
				if (completionHandler != null) {
					completionHandler.onException(ioe);
				}
				throw ioe;
			}
			
		} 

		if (completionHandler != null) {
			completionHandler.onWritten(size);
		}
		return size;
	}
	
	@Override
	void onClose() throws IOException {
		fc.close();
		raf.close();
	}
	
	@Override
	void onDestroy(String reason) {
		try {
			onClose();
		} catch (IOException ignore) { }
		
		boolean isDeleted = file.delete();
		if (!isDeleted) {
			LOG.warning("connection terminated abnormally " + reason + ". Could not delete file " + file.getAbsolutePath() + " ");
		}
	}
	
	@Override
	int getPendingWriteDataSize() {
		return 0;
	}
	
	@Override
	boolean isNetworkendpoint() {
		return false;
	}
}