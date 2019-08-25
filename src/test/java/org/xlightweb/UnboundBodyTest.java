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


import org.junit.Assert;

import org.junit.Test;
import org.xlightweb.client.HttpClientConnection;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;



/**
*
* @author grro@xlightweb.org
*/
public final class UnboundBodyTest {


	@Test
	public void testOldStyled() throws Exception {
		IServer server = new Server(new OldStyledHandler());
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		IHttpResponse response = con.call(new GetRequest("/"));
		
		String body = response.getBody().readString();
		Assert.assertTrue(body.endsWith("</html>\r\n"));
		
		server.close();	
	}
	
	
	
	@Test
	public void testOldStyledProxied() throws Exception {
		IServer server = new Server(new OldStyledHandler());
		server.start();
		
		HttpProxy proxy = new HttpProxy(0, "localhost", server.getLocalPort(), true, 60 * 60, 30 * 60);
		proxy.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", proxy.getLocalPort());
		IHttpResponse response = con.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
		
		String body = response.getBody().readString();
		Assert.assertTrue(body.endsWith("</html>\r\n"));
		
		server.close();	
	}
	
	
	
	
	
	private static final class OldStyledHandler implements IDataHandler {
		
		public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
			connection.setAutoflush(false);
			connection.write("<html>\r\n");
			connection.write("<body>\r\n");
			connection.write("<h1>Hello</h1>\r\n");
			connection.write("</body>\r\n");
			connection.write("</html>\r\n");
			connection.close();
			
			return true;
		}
	}
}
