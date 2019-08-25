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




import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;


import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IServer;




/**
*
* @author grro@xlightweb.org
*/
public final class FileChannelTransferTest  {

	private static File file;

	
	@BeforeClass
	public static void setup() {
		file = QAUtil.createTestfile_40k();
	}
	

	@AfterClass
	public static void teardown() {
		file.delete();
	}
	
	
	@Test
	public void testSimple() throws Exception {

		IServer server = new HttpServer(new DocHandler());
		server.start();

		HttpClientConnection con = new HttpClientConnection("localhost", server.getLocalPort());

		IHttpResponse response = con.call(new GetRequest("/"));
		BodyDataSource body = response.getBody();
	
		int size = (int) file.length();
		InputStream is = new FileInputStream(file);
		
		Assert.assertTrue(QAUtil.isEquals(is, body, size));
		
		con.close();
		server.close();
	}



	public final class DocHandler implements IHttpRequestHandler {


		public void onRequest(IHttpExchange exchange) throws IOException {

			RandomAccessFile raf = new RandomAccessFile(file, "r");
			FileChannel fc = raf.getChannel();
			BodyDataSink bodyDataSink = exchange.send(new HttpResponseHeader(200, "text/plain"), (int) fc.size());
			bodyDataSink.transferFrom(fc);
			bodyDataSink.close();
			fc.close();
			raf.close();
		}
	}

}