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

import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;



/**
*
* @author grro@xlightweb.org
*/
public final class LifeCycleTest  {

 
	@Test
	public void testClientConnectDisconnect() throws Exception {
		
		HttpServer server = new HttpServer(new ItWorksServerHandler());
		server.start();
	
		ConnectionLifeCycleHandler lifecycleHandler = new ConnectionLifeCycleHandler();
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort(), lifecycleHandler);
		
		Assert.assertEquals(1, lifecycleHandler.getCountCalledConnect());
		Assert.assertEquals(0, lifecycleHandler.getCountCalledDisconnect());
		
		con.close();
		QAUtil.sleep(1000);
		
		Assert.assertEquals(1, lifecycleHandler.getCountCalledConnect());
		Assert.assertEquals(1, lifecycleHandler.getCountCalledDisconnect());
		
		
		server.close();
	}

	
	
	@Test
	public void testServerConnectDisconnect() throws Exception {
		
		HttpServer server = new HttpServer(new ItWorksServerHandler());
		
		ConnectionLifeCycleHandler lifecycleHandler = new ConnectionLifeCycleHandler();
		server.addConnectionHandler(lifecycleHandler);
		
		server.start();
	


		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		QAUtil.sleep(200);
		
		Assert.assertEquals(1, lifecycleHandler.getCountCalledConnect());
		Assert.assertEquals(0, lifecycleHandler.getCountCalledDisconnect());
		
		con.close();
		QAUtil.sleep(200);
		
		Assert.assertEquals(1, lifecycleHandler.getCountCalledConnect());
		Assert.assertEquals(1, lifecycleHandler.getCountCalledDisconnect());
		
		
		server.close();
	}

	
	
	
	private static final class ConnectionLifeCycleHandler implements IHttpConnectHandler, IHttpDisconnectHandler {
		
		private int countCalledConnect = 0; 
		private int countCalledDisconnect = 0;

		@Execution(Execution.NONTHREADED)
		public boolean onConnect(IHttpConnection httpConnection) throws IOException {
			countCalledConnect++;
			return true;
		}
		
		@Execution(Execution.NONTHREADED)
		public boolean onDisconnect(IHttpConnection httpConnection) throws IOException {
			countCalledDisconnect++;
			return true;
		}
		
		
		public int getCountCalledConnect() {
			return countCalledConnect;
		}
		
		public int getCountCalledDisconnect() {
			return countCalledDisconnect;
		}
		
	}
	
	
}