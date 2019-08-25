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


import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;


import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClient.FollowsRedirectMode;
import org.xlightweb.server.HttpServer;




/**
 *
 * @author grro@xlightweb.org
 */
public final class ConstantLoadClientTest {
	
	private static final Logger LOG = Logger.getLogger(ConstantLoadClientTest.class.getName());
	


	@Test
	public void testSimple() throws Exception {
		HttpServer server = new HttpServer(new RequestHandler());
		server.start();
		
		Executor workerpool = Executors.newCachedThreadPool();
		
		List<AbstractConversation> conversations = new ArrayList<AbstractConversation>();
		
		
		// ramp up
		for (int i = 0; i < 10; i++) {
			AbstractConversation conversation = new SimpleConversation("http://localhost:" + server.getLocalPort() + "/", workerpool);
			conversation.run();
			conversations.add(conversation);
			
			QAUtil.sleep(100);
		}
		
		
		// wait until all conversations are closed
		boolean isRunning;
		do {
			QAUtil.sleep(200);
			
			isRunning = false;
			for (AbstractConversation conversation : conversations) {
				isRunning = isRunning || conversation.isRunning();
			}
		} while (isRunning);
		
		
		// check errora
		for (AbstractConversation conversation : conversations) {
			Assert.assertTrue(conversation.getErrors().isEmpty());
		}
		
		server.close();
	}
	
	
	
	private static abstract class AbstractConversation implements IHttpResponseHandler, Runnable, Closeable {
		
		private static final Timer TIMER = new Timer(true); 

		private final AtomicBoolean isRunning = new AtomicBoolean(true);
		private final List<IOException> errors = new ArrayList<IOException>();
		private final List<Double> latencies = new ArrayList<Double>();
		private final HttpClient httpClient;
		
		private static int nextNum = 0;

		private int num;
		private long sendTimeNanos;
		private int state = 0;
		
		
		public AbstractConversation(Executor workerpool) {
			num = ++nextNum;
			
			httpClient = new HttpClient();
			
			httpClient.setWorkerpool(workerpool);
			httpClient.setAutoHandleCookies(true);
			httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
		}
		
		
		public final void run() {
			LOG.info("[" + num + "] starting conversation ");
			try {
				onRun();
			} catch (IOException ioe) {
				onException(ioe);
			}
			
		}
		
		
		public final boolean isRunning() {
			return isRunning.get();
		}
		
		
		public final void close() {
			isRunning.set(false);
			try {
				httpClient.close();
			} catch (IOException ignore) { }
			
			LOG.info("[" + num + "] conversation closed");
		}
		
		
		public final List<IOException> getErrors() {
			return errors;
		}
		
		
		@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
		public final void onResponse(IHttpResponse response) throws IOException {
			
			double latency = ((double) (System.nanoTime() - sendTimeNanos)) / 1000000;
			latencies.add(latency);
			
			LOG.info("[" + num + "] received " + response.getStatus() + " " + response.getReason() + " (elapsed=" + latency + " millis)");
		
			switch (state) {
			case 1:
				onResponseOne(response);
				break;

			case 2:
				onResponseTwo(response);
				break;
				
			case 3:
				onResponseThree(response);
				break;
				
			case 4:
				onResponseFour(response);
				break;
				
			case 5:
				onCallFive(response);
				break;
			
			case 6:
				onResponseSix(response);
				break;
				
			case 7:
				onResponseSeven(response);
				break;
				
			default:
				break;
			}
		}
		
		
		public final void onException(IOException ioe) {
			LOG.info("[" + num + "] exception occured. closing conversation");
			errors.add(ioe);
			close();
		}
		
		
		protected final void sendNow(IHttpRequest request) {
			if (isRunning.get()) {
				try {
					if (request.getQueryString() == null) { 
						LOG.info("[" + num + "] sending " + request.getRequestURI());
					} else {
						LOG.info("[" + num + "] sending " + request.getRequestURI() + "?" + request.getQueryString());
					}
				
					
					state++;
					sendTimeNanos = System.nanoTime();
					
					httpClient.send(request, this);
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			} else {
				LOG.info("[" + num + "] do not send request because conversation is already closed");
			}
		}

		
		protected final void sendLater(int delayMillis, final IHttpRequest request) {
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					sendNow(request);
				}
			};
			TIMER.schedule(task, delayMillis);
		}

		protected final void assertTrue(boolean condition) {
			if (!condition) {
				onException(new IOException("assertion error"));
			}
		}
		
		protected abstract void onRun() throws IOException;

		
		protected void onResponseOne(IHttpResponse response) throws IOException {
			LOG.info("[" + num + "] Error illegal state");
		}
		
		protected void onResponseTwo(IHttpResponse response) throws IOException {
			LOG.info("[" + num + "] Error illegal state");
		}
		
		protected void onResponseThree(IHttpResponse response) throws IOException {
			LOG.info("[" + num + "] Error illegal state");
		}
		
		protected void onResponseFour(IHttpResponse response) throws IOException {
			LOG.info("[" + num + "] Error illegal state");
		}
		
		protected void onCallFive(IHttpResponse response) throws IOException {
			LOG.info("[" + num + "] Error illegal state");
		}
		
		protected void onResponseSix(IHttpResponse response) throws IOException {
			LOG.info("[" + num + "] Error illegal state");
		}
		
		protected void onResponseSeven(IHttpResponse response) throws IOException {
			LOG.info("[" + num + "] Error illegal state");
		}		
	}
	
	
	

	private static final class SimpleConversation extends AbstractConversation {
		
		private final String baseUrl;
		private String id = null;
		
		public SimpleConversation(String baseUrl, Executor workerpool) {
			super(workerpool);
			this.baseUrl = baseUrl;
		}
			
		@Override
		protected void onRun() throws IOException {
			sendNow(new GetRequest(baseUrl + "?cmd=login"));
		}
		
		@Override
		protected void onResponseOne(IHttpResponse response) throws IOException {
			assertTrue(response.getStatus() == 200);
			
			id = response.getBody().readString();
			assertTrue(id != null);
			
			sendLater(1 * 1000, new GetRequest(baseUrl + "?cmd=list&id=" + id));
		}

		@Override
		protected void onResponseTwo(IHttpResponse response) throws IOException {
			assertTrue(response.getStatus() == 200);
			
			String[] list = response.getBody().readString().split(",");
			assertTrue(list[0].equals("aa=56656"));
			
			close();
		}
	}
	
	
	 
	private static final class RequestHandler implements IHttpRequestHandler {
		
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			
			String cmd = exchange.getRequest().getParameter("cmd");
			
			if (cmd.equalsIgnoreCase("login")) {
				exchange.send(new HttpResponse(200, "text/plain", "456345645"));
				
			} else if (cmd.equalsIgnoreCase("list")) {
				exchange.send(new HttpResponse(200, "text/plain", "aa=56656,bb=rewtterte,cc=retret"));
				
			} else {
				exchange.sendError(400);
			}
		}
	}
}
