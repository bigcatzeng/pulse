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

import java.nio.BufferUnderflowException;

import org.xsocket.Execution;



/**
 * The body handler. Example
 * 
 * <pre>
 * class BodyToFileStreamer implements IBodyDataHandler {
 * 
 *    private IHttpExchange exchange = null;
 *    
 *    private String path = null;
 *    private RandomAccessFile raf = null;
 *    private FileChannel fc = null;
 *    
 *    private String id = null;
 *    
 *    BodyToFileStreamer(IHttpExchange exchange, File file, String id) throws IOException {
 *       this.exchange = exchange;
 *       this.id = id;
 *       
 *       raf = new RandomAccessFile(file, "rw");
 *       fc = raf.getChannel();
 *    }
 *    
 *    public boolean onData(NonBlockingBodyDataSource bodyDataSource) throws BufferUnderflowException {
 *       try {
 *          int available = bodyDataSource.available();
 *          
 *          // data to transfer?
 *          if (available > 0) {
 *             bodyDataSource.transferTo(fc, available);
 *             
 *          // end of stream reached?
 *          } else if (available == -1) {
 *             fc.close();
 *             raf.close();
 *             exchange.send(new HttpResponse(201, "text/plain", id));
 *          }
 *       } catch (IOException ioe) {
 *          try {
 *             fc.close();
 *             raf.close();
 *             new File(path).delete();
 *          } catch (Exception ignore) { }
 *          
 *          exchange.sendError(500);
 *       }
 *       return true;
 *    }
 * }
 * 
 *
 * class MyRequestHandler implements IHttpRequestHandler  {
 * 
 *    // will be called if request header is received (Default-InvokeOn is HEADER_RECEIVED)
 *    public void onRequest(IHttpExchange exchange) throws IOException {
 *       // ..
 *
 *       BodyToFileStreamer streamer = new BodyToFileStreamer(exchange, file, uid);
 *       exchange.getRequest().getNonBlockingBody().setDataHandler(streamer);
 *    }
 * }
 * </pre>
 * 
 * 
 * @author grro@xlightweb.org
 */
public interface IBodyDataHandler {
	
	public static final int DEFAULT_EXECUTION_MODE = Execution.MULTITHREADED;
	
	
	/**
	 * call back. This method will be called each time body data has been received 
	 * for the given body 
	 * 
	 * @param bodyDataSource    the body  
	 * @return true, if the event has been handled
 	 * @throws BufferUnderflowException if more incoming data is required to process (e.g. delimiter hasn't yet received -> readByDelimiter methods or size of the available, received data is smaller than the required size -> readByLength). The BufferUnderflowException will be swallowed by the framework
	 */
	boolean onData(NonBlockingBodyDataSource bodyDataSource) throws BufferUnderflowException;
}
