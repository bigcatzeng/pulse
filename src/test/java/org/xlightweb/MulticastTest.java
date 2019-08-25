/*
 *  Copyright (c) xlightweb.org, 2006 - 2009. All rights reserved.
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
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;





/**
*
* @author grro@xlightweb.org
*/
public final class MulticastTest  {



	@Test
	public void testSimple() throws Exception {
		MulticastService multicastService = new MulticastService();

		HttpServer server = new HttpServer(new MulticastServerHandler(multicastService));
		server.setIdleTimeoutMillis(3 * 60 * 1000);
		server.start();


		HttpClientConnection con1 = new HttpClientConnection("localhost", server.getLocalPort());
		HttpRequestHeader header1 = new HttpRequestHeader("POST", "/mutlicast");
		MulticastClientHandler receiveChannel1 = new MulticastClientHandler();
		BodyDataSink sendChannel1 = con1.send(header1, receiveChannel1);
		sendChannel1.setAutoflush(false);
		sendChannel1.flush();

		QAUtil.sleep(200);
		Assert.assertEquals(1, multicastService.getCountPeers());

		send(sendChannel1, "1");
		sendChannel1.flush();


		int length = receiveChannel1.readInt();
		String data = receiveChannel1.readStringByLength(length);
		Assert.assertEquals("1", data);





		HttpClientConnection con2 = new HttpClientConnection("localhost", server.getLocalPort());
		HttpRequestHeader header2 = new HttpRequestHeader("POST", "/mutlicast");
		MulticastClientHandler receiveChannel2 = new MulticastClientHandler();
		BodyDataSink sendChannel2 = con2.send(header2, receiveChannel2);
		sendChannel2.setAutoflush(false);
		sendChannel2.flush();

		QAUtil.sleep(200);
		Assert.assertEquals(2, multicastService.getCountPeers());

		send(sendChannel1, "2");
		sendChannel1.flush();


		length = receiveChannel1.readInt();
		data = receiveChannel1.readStringByLength(length);
		Assert.assertEquals("2", data);

		length = receiveChannel2.readInt();
		data = receiveChannel2.readStringByLength(length);
		Assert.assertEquals("2", data);





		con1.close();

		QAUtil.sleep(200);
		Assert.assertEquals(1, multicastService.getCountPeers());



		send(sendChannel2, "3");
		length = receiveChannel2.readInt();
		data = receiveChannel2.readStringByLength(length);
		Assert.assertEquals("3", data);


		con2.close();
		server.close();
	}



	private void send(BodyDataSink dataSink, String msg) throws IOException {
		dataSink.markWritePosition();
		dataSink.write((int) 0);                  // write a empty length field

		int written = dataSink.write(msg);       // write the data

		dataSink.resetToWriteMark();
		dataSink.write(written);                // write the length field
		dataSink.flush();
	}



	private static final class MulticastClientHandler implements IHttpResponseHandler {

		private BodyDataSource channel = null;

		private final Object guard = new Object();


		public void onResponse(IHttpResponse response) throws IOException {
			synchronized (guard) {
				channel = response.getBody();
				guard.notifyAll();
			}
		}
		
		public void onException(IOException ioe) {
		}


		public int readInt() throws IOException {

			synchronized (guard) {

				do {
					if (channel == null) {
						try {
							guard.wait();
						} catch (InterruptedException ignore) { }

					} else {
						return channel.readInt();
					}
				} while (channel != null);
			}

			throw new IOException();
		}


		public String readStringByLength(int length) throws IOException {

			synchronized (guard) {

				do {
					if (channel == null) {
						try {
							guard.wait();
						} catch (InterruptedException ignore) { }

					} else {
						return channel.readStringByLength(length);
					}
				} while (channel != null);
			}

			throw new IOException();
		}
	}



	private static final class MulticastServerHandler implements IHttpRequestHandler, IHttpDisconnectHandler {

		private MulticastService multicastService = null;


		public MulticastServerHandler(MulticastService multicastService) {
			this.multicastService = multicastService;
		}



		@Execution(Execution.NONTHREADED)
		public void onRequest(final IHttpExchange exchange) throws IOException {

			IHttpRequest request = exchange.getRequest();
			NonBlockingBodyDataSource requestBody = request.getNonBlockingBody();


			// prepare and send response
			HttpResponseHeader responseHeader = new HttpResponseHeader(200);
			BodyDataSink bodyDataSink =exchange.send(responseHeader);

			multicastService.registerPeer(exchange.getConnection().getId(), bodyDataSink);



			IBodyDataHandler bodyHandler = new IBodyDataHandler() {
				
				
		        public boolean onData(NonBlockingBodyDataSource bodyDataSource) throws BufferUnderflowException {

		        	try {
		        		// each message start with a packet size field
		        		HttpUtils.validateSufficientDatasizeByIntLengthField(bodyDataSource, false);

		        		ByteBuffer[] availableData =  bodyDataSource.readByteBufferByLength(bodyDataSource.available());

		        		multicastService.sendToPeers(availableData);
		        		return true;
			        	
		        	} catch (IOException ioe) {
		        		multicastService.deregisterPeer(exchange.getConnection().getId());
		        	}
		        	
		        	
		        	return true;
				}
			};
			requestBody.setDataHandler(bodyHandler);
		}



		@Execution(Execution.NONTHREADED)
		public boolean onDisconnect(IHttpConnection httpConnection) throws IOException {
			multicastService.deregisterPeer(httpConnection.getId());
			return true;
		}
	}



	private static final class MulticastService {

		  private final Map<String, WritableByteChannel> peers = new HashMap<String, WritableByteChannel>();

		      synchronized int getCountPeers() {
		         return peers.size();
		      }

		      synchronized void registerPeer(String id, WritableByteChannel channel) {
		         peers.put(id, channel);
		      }

		      synchronized void deregisterPeer(String id) {
		        peers.remove(id);
		      }

		      synchronized void sendToPeers(ByteBuffer[] data) {
		         for (Entry<String, WritableByteChannel> peer : peers.entrySet()) {
		            try {
		               for (ByteBuffer buf : data) {
		                  peer.getValue().write(buf);
		                  buf.flip();
		               }
		            }  catch (IOException ioe) {
		            	deregisterPeer(peer.getKey());
		            }
		        }
		      }
		}

}