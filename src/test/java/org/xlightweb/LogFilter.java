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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.xlightweb.BodyDataSink;
import org.xlightweb.BodyForwarder;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpRequestHeader;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.IHttpResponseHeader;
import org.xlightweb.NonBlockingBodyDataSource;
import org.xsocket.DataConverter;
import org.xsocket.Execution;


//@Execution(Execution.NONTHREADED)
final class LogFilter implements IHttpRequestHandler {
	
	
	
	public void onRequest(final IHttpExchange exchange) throws IOException {
		
		IHttpRequest req = exchange.getRequest(); 

		
		IHttpResponseHandler respHdl = new IHttpResponseHandler() {
			
			@Execution(Execution.NONTHREADED)
			public void onResponse(IHttpResponse response) throws IOException {

				// does request contain a body? 
				if (response.hasBody()) {
					
					final IHttpResponseHeader header = response.getResponseHeader(); 
					final List<ByteBuffer> bodyData = new ArrayList<ByteBuffer>(); 
					
					NonBlockingBodyDataSource orgDataSource = response.getNonBlockingBody();
					final BodyDataSink inBodyChannel = exchange.send(response.getResponseHeader());
					
					//... by a body forward handler
					BodyForwarder bodyForwardHandler = new BodyForwarder(orgDataSource, inBodyChannel) {
						
						@Override
						public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException, IOException {
							ByteBuffer[] bufs = bodyDataSource.readByteBufferByLength(bodyDataSource.available());
									
							for (ByteBuffer byteBuffer : bufs) {
								bodyData.add(byteBuffer.duplicate());
							}
									
							bodyDataSink.write(bufs);
							bodyDataSink.flush();
						}
						
						@Override
						public void onComplete() {
							System.out.println(header.toString());
							try {
								System.out.println(header.toString() + DataConverter.toString(bodyData, header.getCharacterEncoding()));
							} catch (Exception e) {
								System.out.println("<body not printable>");
							}
						}
					};
					orgDataSource.setDataHandler(bodyForwardHandler);
					
				} else {
					try {
						System.out.println(response.getResponseHeader());
					} catch (Exception ignore) { }
					exchange.send(response);
				}
			}
			
			public void onException(IOException ioe) {
				exchange.sendError(500);
			}
		};
		
		
		// does request contain a body? 
		if (req.hasBody()) {
			
			final IHttpRequestHeader header = req.getRequestHeader(); 
			final List<ByteBuffer> bodyData = new ArrayList<ByteBuffer>(); 
 
			
			// get the body 
			NonBlockingBodyDataSource orgDataSource = req.getNonBlockingBody();
			
			// ... and replace it  
			final BodyDataSink inBodyChannel = exchange.forward(req.getRequestHeader(), respHdl);
			
			//... by a body forward handler
			BodyForwarder bodyForwardHandler = new BodyForwarder(orgDataSource, inBodyChannel) {
				
				@Override
				public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException, IOException {
					ByteBuffer[] bufs = bodyDataSource.readByteBufferByLength(bodyDataSource.available());
							
					for (ByteBuffer byteBuffer : bufs) {
						 bodyData.add(byteBuffer.duplicate());
					}
							
					bodyDataSink.write(bufs);
					bodyDataSink.flush();
				}
				
				@Override
				public void onComplete() {
					System.out.println(header.toString());
					try {
						System.out.println(header.toString() + DataConverter.toString(bodyData, header.getCharacterEncoding()));
					} catch (Exception e) {
						System.out.println("<body not printable>");
					}
				}
			};
			orgDataSource.setDataHandler(bodyForwardHandler);
			
		} else {
			System.out.println(req.getRequestHeader().toString());
			exchange.forward(req, respHdl);
		}
	}
}
