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
import java.nio.channels.ClosedChannelException;


import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.BodyDataSink;
import org.xlightweb.HttpRequestHeader;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;





/**
*
* @author grro@xlightweb.org
*/
public final class BodyDataSinkCloseTest  {


	@Test
	public void testCloseBoundBody() throws Exception {
		ServerHandler hdl = new ServerHandler();
		HttpServer server = new HttpServer(hdl);
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		IHttpResponseHandler resHdl = new IHttpResponseHandler() {
			
			public void onResponse(IHttpResponse response) throws IOException {
			}
			
			public void onException(IOException ioe) {
			}
		};


		BodyDataSink outChannel = con.send(new HttpRequestHeader("POST", "/"), 100, resHdl);

		outChannel.write(QAUtil.generateByteArray(100));
		
		outChannel.close();
		
		QAUtil.sleep(200);
		Assert.assertFalse(outChannel.isOpen());
		
		con.close();
		server.close();
	}

	
	@Test
	public void testDestroyBoundBody() throws Exception {
	
	    
	    ServerHandler hdl = new ServerHandler();
		HttpServer server = new HttpServer(hdl);
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		IHttpResponseHandler resHdl = new IHttpResponseHandler() {
			
			public void onResponse(IHttpResponse response) throws IOException {
			}
			
			public void onException(IOException ioe) {
			}
		};


		BodyDataSink outChannel = con.send(new HttpRequestHeader("POST", "/"), 100, resHdl);

		outChannel.destroy();
		
		QAUtil.sleep(200);
		Assert.assertFalse(outChannel.isOpen());
		
		con.close();
		server.close();
	}


	
	@Test
	public void testCloseConnection() throws Exception {
		ServerHandler hdl = new ServerHandler();
		HttpServer server = new HttpServer(hdl);
		server.start();
		
		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());
		
		IHttpResponseHandler resHdl = new IHttpResponseHandler() {
			
			public void onResponse(IHttpResponse response) throws IOException {
			}
			
			public void onException(IOException ioe) {
			}
		};


		BodyDataSink outChannel = con.send(new HttpRequestHeader("POST", "/"), 100, resHdl);

		con.close();
		
		QAUtil.sleep(200);
		Assert.assertFalse(outChannel.isOpen());
		
		try {
			outChannel.write("test");
			Assert.fail("ClosedChannelException expected");
		} catch (ClosedChannelException expected) { } 

		Assert.assertFalse(outChannel.isOpen());

		
		server.close();
	}

	
	
	private static final class ServerHandler implements IHttpRequestHandler  {
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			exchange.send(new HttpResponse(200, "text/plain", "OK"));
		}
	}
 
}