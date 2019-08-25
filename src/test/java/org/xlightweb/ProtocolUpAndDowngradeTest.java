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
import java.nio.channels.ClosedChannelException;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;




/**
*
* @author grro@xlightweb.org
*/
public final class ProtocolUpAndDowngradeTest  {


	

	@Test
	public void testHttp1_1Request_1_0_Response_LengthDefinied() throws Exception {
	
		IDataHandler serverHdl = new IDataHandler() {
			public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
				connection.readStringByDelimiter("\r\n\r\n");
				
				connection.write("HTTP/1.0 200 OK\r\n" +
						         "Content-Length: 10\r\n" + 
						         "\r\n" +
						         "1234567890");
				return true;
			}
		};
		IServer server = new Server(serverHdl);
		server.start();
		
		HttpProxy proxy =  new HttpProxy(0, "localhost", server.getLocalPort(), true, 60, 30);
		proxy.start();

		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + proxy.getLocalPort() + "/test"));
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("HTTP/1.0", response.getProtocol());
		
		httpClient.close();
		proxy.close();
		server.close();
	}	
	
	
}
