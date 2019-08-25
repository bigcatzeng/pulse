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

import java.nio.channels.ClosedChannelException;


import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


import org.junit.Test;
import org.junit.Assert;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;




/**
*
* @author grro@xlightweb.org
*/
public final class MultipartResponseReceiveTest  {


	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 10000; i++) {
			System.out.print(".");
			new MultipartResponseReceiveTest().testTwoPartsBlocking2();
		}
	}
	
	
	@Test
	public void testOnePart() throws Exception {

		IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				
				BodyDataSink dataSink = exchange.send(new HttpResponseHeader(200, "multipart/mixed; boundary=ABCDE"));
				
		        BodyDataSink partDataSink = dataSink.writePart(new Header("text/plain")); 
		        partDataSink.write("0123456789");
		        partDataSink.close();
		        
		        dataSink.close();
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();

        String url = "http://localhost:" + server.getLocalPort() + "/MyResource";
        
        
        HttpClient httpClient = new HttpClient();
        IHttpResponse response = httpClient.call(new GetRequest(url));
        Assert.assertEquals(200, response.getStatus());
    
        PartHandler partHdl = new PartHandler();
        response.getNonBlockingBody().setBodyPartHandler(partHdl);
        
        while (partHdl.getFirstPart() == null) {
        	QAUtil.sleep(100);
        }
        
        Assert.assertEquals("text/plain", partHdl.getFirstPart().getContentType());
        Assert.assertEquals("0123456789", partHdl.getFirstPart().getBody().readString());

        httpClient.close();
        server.close();
    }	


	@Test
	public void testTwoParts() throws Exception {

		IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				
				BodyDataSink dataSink = exchange.send(new HttpResponseHeader(200, "multipart/mixed; boundary=ABCDE"));
				
		        BodyDataSink partDataSink = dataSink.writePart(new Header("text/plain")); 
		        partDataSink.write("0123456789");
		        partDataSink.close();
		        
		        BodyDataSink partDataSink2 = dataSink.writePart(new Header("application/octet-stream")); 
		        partDataSink2.write("trretgdfbdfgbdfgdfgdsfg43652645z7trehdfgdsfgdfgdsfg4562tgdsfgvdsfg");
		        partDataSink2.close();
		        
		        dataSink.close();
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();

        String url = "http://localhost:" + server.getLocalPort() + "/MyResource";
        
        
        HttpClient httpClient = new HttpClient();
        IHttpResponse response = httpClient.call(new GetRequest(url));
        Assert.assertEquals(200, response.getStatus());
    
        PartHandler partHdl = new PartHandler();
        response.getNonBlockingBody().setBodyPartHandler(partHdl);
        
        while (!partHdl.isClosedChannelDetected()) {
        	QAUtil.sleep(100);
        }
        
        Assert.assertEquals("text/plain", partHdl.getFirstPart().getContentType());
        Assert.assertEquals("0123456789", partHdl.getFirstPart().getBody().readString());

        Assert.assertEquals("application/octet-stream", partHdl.getSecondPart().getContentType());
        Assert.assertEquals("trretgdfbdfgbdfgdfgdsfg43652645z7trehdfgdsfgdfgdsfg4562tgdsfgvdsfg", partHdl.getSecondPart().getBody().readString());

        httpClient.close();
        server.close();
    }	
	
	
	@Test
	public void testOnePartBlocking() throws Exception {

		IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				
				BodyDataSink dataSink = exchange.send(new HttpResponseHeader(200, "multipart/mixed; boundary=ABCDE"));
				
		        BodyDataSink partDataSink = dataSink.writePart(new Header("text/plain")); 
		        partDataSink.write("0123456789");
		        partDataSink.close();
		        
		        dataSink.close();
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();

        String url = "http://localhost:" + server.getLocalPort() + "/MyResource";
        
        
        HttpClient httpClient = new HttpClient();
        IHttpResponse response = httpClient.call(new GetRequest(url));
        Assert.assertEquals(200, response.getStatus());
    
        BodyDataSource dataSource = response.getBody();
        
        IPart part = dataSource.readPart();
        Assert.assertEquals("text/plain", part.getContentType());
        Assert.assertEquals("0123456789", part.getBody().readString());

        
        try {
        	dataSource.readPart();
        	Assert.fail("ClosedChannelException expected");
        } catch (ClosedChannelException expected) { }
        
        
        httpClient.close();
        server.close();
    }	

	@Test
	public void testTwoPartsBlocking() throws Exception {

		IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				
				BodyDataSink dataSink = exchange.send(new HttpResponseHeader(200, "multipart/mixed; boundary=ABCDE"));
				
		        BodyDataSink partDataSink = dataSink.writePart(new Header("text/plain")); 
		        partDataSink.write("0123456789");
		        partDataSink.close();
		        
		        BodyDataSink partDataSink2 = dataSink.writePart(new Header("application/octet-stream")); 
		        partDataSink2.write("trretgdfbdfgbdfgdfgdsfg43652645z7trehdfgdsfgdfgdsfg4562tgdsfgvdsfg");
		        partDataSink2.close();
		        
		        dataSink.close();
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();

        String url = "http://localhost:" + server.getLocalPort() + "/MyResource";
        
        
        HttpClient httpClient = new HttpClient();
        IHttpResponse response = httpClient.call(new GetRequest(url));
        Assert.assertEquals(200, response.getStatus());
        
        BodyDataSource body = response.getBody();

        IPart part = body.readPart();
        Assert.assertEquals("text/plain", part.getContentType());
        Assert.assertEquals("0123456789", part.getBody().readString());

        part = body.readPart();
        Assert.assertEquals("application/octet-stream", part.getContentType());
        Assert.assertEquals("trretgdfbdfgbdfgdfgdsfg43652645z7trehdfgdsfgdfgdsfg4562tgdsfgvdsfg", part.getBody().readString());

        httpClient.close();
        server.close();
    }		
	
	@Test
	public void testTwoPartsBlocking2() throws Exception {

		IHttpRequestHandler reqHdl = new IHttpRequestHandler() {
			
			public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
				
				BodyDataSink dataSink = exchange.send(new HttpResponseHeader(200, "multipart/mixed; boundary=ABCDE"));
				
		        BodyDataSink partDataSink = dataSink.writePart(new Header("text/plain")); 
		        partDataSink.write("0123456789");
		        partDataSink.close();
		        
		        BodyDataSink partDataSink2 = dataSink.writePart(new Header("application/octet-stream")); 
		        partDataSink2.write("trretgdfbdfgbdfgdfgdsfg43652645z7trehdfgdsfgdfgdsfg4562tgdsfgvdsfg");
		        partDataSink2.close();
		        
		        dataSink.close();
			}
		};
		HttpServer server = new HttpServer(reqHdl);
		server.start();

        String url = "http://localhost:" + server.getLocalPort() + "/MyResource";
        
        
        HttpClient httpClient = new HttpClient();
        IHttpResponse response = httpClient.call(new GetRequest(url));
        Assert.assertEquals(200, response.getStatus());

        List<IPart> parts = response.getBody().readParts();
        
        IPart part = parts.get(0);
        Assert.assertEquals("text/plain", part.getContentType());
        Assert.assertEquals("0123456789", part.getBody().readString());

        part = parts.get(1);
        Assert.assertEquals("application/octet-stream", part.getContentType());
        Assert.assertEquals("trretgdfbdfgbdfgdfgdsfg43652645z7trehdfgdsfgdfgdsfg4562tgdsfgvdsfg", part.getBody().readString());

        httpClient.close();
        server.close();
    }		
	

	
	private static final class PartHandler implements IPartHandler {
		
		private final AtomicReference<IPart> firstPartRef = new AtomicReference<IPart>();
		private final AtomicReference<IPart> secondPartRef = new AtomicReference<IPart>();
		private final AtomicBoolean closedChannleDetected = new AtomicBoolean(false);
		
		public void onPart(NonBlockingBodyDataSource dataSource) throws IOException, BadMessageException {
            IPart part;
            try {
                part = dataSource.readPart();
                System.out.println("part handler called part " + part);         
            } catch (ClosedChannelException cce) { 
                System.out.println("part handler called part exepction");
                closedChannleDetected.set(true);
                return;
            }

			
            if (firstPartRef.get() == null) {
				firstPartRef.set(part);
			} else if (secondPartRef.get() == null) {
				secondPartRef.set(part);
			} 
		}
		
		public IPart getFirstPart() {
			return firstPartRef.get();
		}
		
		public IPart getSecondPart() {
			return secondPartRef.get();
		}

		public boolean isClosedChannelDetected() {
			return closedChannleDetected.get();
		}
	}
	
}