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




/**
*
* @author grro@xlightweb.org
*/
public final class ConnectDisconnectTest  {



	@Test
	public void testSimple() throws Exception {

		ServerHandler srvHdl = new ServerHandler();
		HttpServer server = new HttpServer(srvHdl);
		server.addConnectionHandler(srvHdl);
		server.start();

		ClientHandler cltHdl = new ClientHandler();
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort(), cltHdl);


		QAUtil.sleep(1000);
		Assert.assertEquals(1, cltHdl.connectCalled());
		Assert.assertEquals(1, srvHdl.connectCalled());


		con.close();
		QAUtil.sleep(1000);

		Assert.assertEquals(1, cltHdl.connectCalled());
		Assert.assertEquals(1, cltHdl.disconnectCalled());

		Assert.assertEquals(1, srvHdl.connectCalled());
		Assert.assertEquals(1, srvHdl.disconnectCalled());



		server.close();
	}




	private static class ClientHandler implements IHttpConnectHandler, IHttpDisconnectHandler {

		private int connectCalled = 0;
		private int disconnectCalled = 0;


		public boolean onConnect(IHttpConnection httpConnection) throws IOException {
			connectCalled++;
			return true;
		}


		public boolean onDisconnect(IHttpConnection httpConnection) throws IOException {
			disconnectCalled++;
			return true;
		}


		int disconnectCalled() {
			return disconnectCalled;
		}

		int connectCalled() {
			return connectCalled;
		}
	}


	private static class ServerHandler extends ClientHandler implements IHttpRequestHandler {

		public void onRequest(IHttpExchange exchange) throws IOException {
			exchange.send(new HttpResponse(200, "text/plain", "OK"));
		}

	}
}