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
import java.net.SocketTimeoutException;

import org.junit.Assert;
import org.junit.Test;


import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IServer;
import org.xsocket.connection.ConnectionUtils;





/**
*
* @author grro@xlightweb.org
*/
public final class TimeoutTest  {


	@Test
	public void testHttpClientConnectionCallResponseTimeout() throws Exception {
		HttpServer server = new HttpServer(new BlackholeHandler());
		server.start();

		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());

		con.setResponseTimeoutMillis(1000);
		try {
			con.call(new GetRequest("/"));
			Assert.fail("timeout exception should have been thrown");
		} catch (SocketTimeoutException expected) { }


		con.close();
		server.close();
	}
	

    @Test
    public void testHttpClientCallResponseTimeout() throws Exception {
        System.setProperty("org.xlightweb.showDetailedError", "true");
        
        HttpServer server = new HttpServer(new BlackholeHandler());
        server.start();

        HttpClient client = new HttpClient();

        client.setResponseTimeoutMillis(1000);
        try {
            client.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/"));
            Assert.fail("timeout exception should have been thrown");
        } catch (SocketTimeoutException expected) { }


        client.close();
        server.close();
    }



	@Test
	public void testClientSendResponseTimeout() throws Exception {

		IServer server = new HttpServer(new BlackholeHandler());
		ConnectionUtils.start(server);


		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		con.setResponseTimeoutMillis(1000);

		ResponseHandler hdl = new ResponseHandler();
		con.send(new GetRequest("/"), hdl);


		QAUtil.sleep(2000);
		Assert.assertEquals(1, hdl.getCountIOException());


		con.close();
		server.close();
	}





	@Test
	public void testServerConnectionTimeout() throws Exception {
		
		DisconnectHandler srvHdl = new DisconnectHandler();
		HttpServer server = new HttpServer(null);
		server.addConnectionHandler(srvHdl);
		server.setConnectionTimeoutMillis(1 * 1000);
		server.start();


		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		QAUtil.sleep(2000);

		
		
		if (srvHdl.getConnection().isOpen()) {
            System.out.println("server connection should be closed");
            Assert.fail("server connection should be closed");
        }
		
		if (srvHdl.getCountDesconnect() != 1) {
            System.out.println("disconnect should be called once not " + srvHdl.getCountDesconnect());
            Assert.fail("disconnect should be called once not " + srvHdl.getCountDesconnect());
        }
		
		con.close();
		server.close();

		con.close();
		server.close();
	}




	

	
	private static final class BlackholeHandler implements IHttpRequestHandler {

		public void onRequest(IHttpExchange exchange) throws IOException {

		}
	}


	
	private static final class DisconnectHandler implements IHttpDisconnectHandler {
		
		private int countDisconnect = 0;
		private IHttpConnection connection = null;
		
		public boolean onDisconnect(IHttpConnection httpConnection) throws IOException {
			countDisconnect++;
			this.connection = httpConnection;
			return true;
		}
		
		int getCountDesconnect() {
			return countDisconnect;
		}
		
		IHttpConnection getConnection() {
			return connection;
		}
	}
	
	

	private static final class ResponseHandler implements IHttpResponseHandler {

		private int countIOException = 0;


		public void onResponse(IHttpResponse response) throws IOException {

		}

		public void onException(IOException ioe) {
			System.out.println(ioe.toString());
			countIOException++;
		}


		int getCountIOException() {
			return countIOException;
		}
	}



	@Test
	public void testSlowResponse() throws Exception {
		
		System.setProperty("org.xlightweb.server.showDetailedError", "true");
	
		IServer server = new HttpServer(new SlowServerHandler());
		ConnectionUtils.start(server);
	
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		con.setResponseTimeoutMillis(1000);
		
		IHttpResponse response = con.call(new GetRequest("/?pauseMillis=300&chunks=8"));
	
		Assert.assertEquals(200, response.getStatus());
		response.getBody().readString();
	
		con.close();

		con.close();
		server.close();	
	}
	
	
	private static final class SlowServerHandler implements IHttpRequestHandler {
		
	
		public void onRequest(IHttpExchange exchange) throws IOException {

			IHttpRequest request = exchange.getRequest();
			int pause = Integer.parseInt(request.getParameter("pauseMillis"));
			int chunks = Integer.parseInt(request.getParameter("chunks"));
		
			BodyDataSink outChannel = exchange.send(new HttpResponseHeader(200, "text/plain"));
			
			for (int i = 0; i < chunks; i++) {
				outChannel.write("test");
				QAUtil.sleep(pause);
				System.out.print(".");
			}

			outChannel.close();
		}
	}
}

