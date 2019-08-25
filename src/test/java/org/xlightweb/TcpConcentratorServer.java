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


import org.xsocket.Execution;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.IDisconnectHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.NonBlockingConnection;
import org.xsocket.connection.Server;
import org.xsocket.connection.IConnection.FlushMode;
import org.xsocket.connection.multiplexed.MultiplexedConnection;


 

/**  
*
* @author grro@xlightweb.org
*/
public final class TcpConcentratorServer extends Server {
	
	public TcpConcentratorServer(int listenport, String forwardHost, int forwardPort) throws IOException {
		super(listenport, new ForwardHandler(forwardHost, forwardPort));
	}
	

	public static void main(String... args) throws IOException {
		if (args.length != 3) {
			System.out.println("usage org.xsocket.connection.http.TcpMultiplexerServer <listenport> <forwardhost> <forwardport>");
		}
		
		new TcpConcentratorServer(Integer.parseInt(args[0]), args[1], Integer.parseInt(args[2])).run();
	}
	
	
	
	
	private static class Handler implements IDataHandler, IDisconnectHandler {
		
		
		@Execution(Execution.NONTHREADED)
		public final boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
			INonBlockingConnection forwardPipeline = (INonBlockingConnection) connection.getAttachment();
			forwardPipeline.setFlushmode(FlushMode.ASYNC);
			
			ByteBuffer[] data = connection.readByteBufferByLength(connection.available());
			
			ByteBuffer[] copy = new ByteBuffer[data.length];
			for (int i = 0; i < data.length; i++) {
				copy[i] = data[i].duplicate();
			}
			
			forwardPipeline.write(data);
			
			return true;
		}
				
		
		@Execution(Execution.NONTHREADED)
		public final boolean onDisconnect(INonBlockingConnection connection) throws IOException {
			
			INonBlockingConnection peerConnection = (INonBlockingConnection) connection.getAttachment();
			if (peerConnection != null) {
				peerConnection.close();
				connection.setAttachment(null);
			}
			
			connection.close();
			//System.out.println("proxy connection destroyed " + connection.getId() + " -> " + peerConnection.getId());
			
			return true;
		}
	}
	
	
	private static final class ForwardHandler extends Handler implements IConnectHandler {
		
		private static final long IDLE_TIMEOUT_MILLIS = 60 * 1000;
		
		private MultiplexedConnection forwardMultiplexedConnection = null;
		
		public ForwardHandler(String forwardHost, int forwardPort) throws IOException {
			forwardMultiplexedConnection = new MultiplexedConnection(new NonBlockingConnection(forwardHost, forwardPort));
		}

		
		@Execution(Execution.MULTITHREADED)
		public boolean onConnect(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {

			connection.setIdleTimeoutMillis(IDLE_TIMEOUT_MILLIS);
			
			String pipelineId = forwardMultiplexedConnection.createPipeline();
			INonBlockingConnection forwardPipeline = forwardMultiplexedConnection.getNonBlockingPipeline(pipelineId);
		    forwardPipeline.setHandler(new Handler());
			
		    forwardPipeline.setIdleTimeoutMillis(IDLE_TIMEOUT_MILLIS * 2);
						
			connection.setAttachment(forwardPipeline);
			forwardPipeline.setAttachment(connection);
			
			//System.out.println("proxy connection established " + connection.getId() + " <-> " + pipelineId);			
			return true;
		}
	}

}
