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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.DataConverter;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IServer;



/**
*
* @author grro@xlightweb.org
*/
public final class SimplePerformanceBenchmark  {


	private int running = 0;
	private final List<String> errors = new ArrayList<String>();
	
	
	public static void main(String[] args) throws Exception {
		
		new SimplePerformanceBenchmark().testClientInMemory();
		new SimplePerformanceBenchmark().testClientFile();
	}
	

	@Test
	public void testClientInMemory() throws Exception {
		
		WebContainer servletEngine = new WebContainer(new HeaderInfoServlet());
		servletEngine.start();
		
		IServer server = new HttpServer(new HeaderInfoServerHandler());
		ConnectionUtils.start(server);
		int xSocketPort = server.getLocalPort();

		
		// warm up
		System.out.println("warm up jetty");
		perform(3, 100, servletEngine.getLocalPort(), null);
		QAUtil.sleep(200);
		
		System.out.println("warm up xSocket");
		perform(3, 100, xSocketPort, null);
		QAUtil.sleep(200);
		
		
		System.out.println("run jetty");
		int elapsedJetty = perform(4, 300, servletEngine.getLocalPort(), null);
		QAUtil.sleep(200);
		
		System.out.println("run xSocket");
		int elapsedXSocket = perform(4, 300, xSocketPort, null);
		QAUtil.sleep(200);
		
		
		server.close();
		servletEngine.stop();
		
		printResult("in memory", elapsedJetty, elapsedXSocket, 30);
	}
	

	@Test
	public void testClientFile() throws Exception {
		
		WebContainer servletEngine = new WebContainer(new DocServlet());
		servletEngine.start();
		
		IServer server = new HttpServer(new DocServerHandler());
		ConnectionUtils.start(server);
		int xSocketPort = server.getLocalPort();

		
		// warm up
		System.out.println("warm up jetty");
		perform(5, 200, servletEngine.getLocalPort(), 3232);
		QAUtil.sleep(200);
		
		System.out.println("warm up xSocket");
		perform(5, 20, xSocketPort, 3232);
		QAUtil.sleep(200);
		
		
		System.out.println("run jetty");
		int elapsedJetty = perform(5, 100, servletEngine.getLocalPort(), 3232);
		QAUtil.sleep(200);
		
		System.out.println("run xSocket");
		int elapsedXSocket = perform(5, 100, xSocketPort, 3232);
		QAUtil.sleep(200);
		
		
		server.close();
		servletEngine.stop();
		
		printResult("file test", elapsedJetty, elapsedXSocket, 30);
	}

	
	
	private void printResult(String msg, int elapsedJetty, int elapsedXSocket, int maxDeviation) {
		
		int diff = 100 -  ((elapsedJetty * 100) / elapsedXSocket); 
		
		System.out.println(msg + " diff "  + diff + "% (jetty=" + DataConverter.toFormatedDuration(elapsedJetty) + 
				           ", xSocket: " + DataConverter.toFormatedDuration(elapsedXSocket) + ")");
		
		Assert.assertTrue("diff is larger than " + maxDeviation + "%: " + diff + "%",  diff < maxDeviation);
	}
		
	
	int perform(final int workers, final int loops, final int port, final Integer size) throws Exception {
		
		final AtomicInteger total = new AtomicInteger();
	

		running = 0;
		errors.clear();
		
	 	
		for (int i = 0; i < workers; i++) {
			
			Thread t = new Thread() {
				public void run() {
					running++;
					
					try {
						HttpClient httpClient = new HttpClient();
	
						for (int j = 0; j < loops; j++) {
							long start = System.currentTimeMillis();
							IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + port + "/"));

							long elapsed = (System.currentTimeMillis() - start);
							total.addAndGet((int) elapsed);
					
							
							Assert.assertEquals(200, response.getStatus());
							if (size != null) {
								Assert.assertEquals(size.intValue(), response.getBody().size());
							}

							//System.out.println(elapsed + " millis");
						}
						
					} catch (Exception e) {
						errors.add(e.toString());
					}

					running--;
				}
				
			};
			t.start();
		}
				
		

		do {
			QAUtil.sleep(100);
		} while (running > 0);

		return total.get();		
	}

}
