/*
 *  Coorg.xsocket.connection xsocket.org, 2006 - 2009. All rights reserved.
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
 * The latest copy of this software may be found on http://www.xsocket.org/
 */
package org.xlightweb;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.JMException;



import org.xlightweb.client.HttpClient;
import org.xsocket.Execution;
import org.xsocket.connection.ConnectionUtils;


/**
*
* @author grro@xsocket.org
*/
public final class HttpClientApp  {
	
	
	private int counts = 0;
	private long lastTime = System.currentTimeMillis(); 
	
	private final HttpClient httpClient;
	
	
	public HttpClientApp() throws JMException {
		httpClient = new HttpClient();
		httpClient.setAutoHandleCookies(false);
		ConnectionUtils.registerMBean(httpClient);		 
	}
	
	
    public static void main(String[] args) throws Exception {
    	
    	System.setProperty("org.xsocket.connection.client.readbuffer.usedirect", "true");
    	
    	
    	if (args.length < 3) {
    		System.out.println("usage java org.xsocket.connection.HttpClientApp <host> <port> <waitTimeBetweenRequests> [<maxActiveConnections>]");
    	}
    		
    	
    	String host = args[0];
    	int port = Integer.parseInt(args[1]);
    	int waittime = Integer.parseInt(args[2]);
    	int maxActive = Integer.MAX_VALUE;
    	if (args.length > 3) {
    		maxActive = Integer.parseInt(args[3]);
    	}
    	
    	new HttpClientApp().launch(host, port, waittime, maxActive);
    	
    }

    public void launch(String host, int port, int waittime, int maxActive) throws Exception {
    	
    	System.out.println("calling " + host + ":" + port + " (waitime between requests " + waittime + " millis; maxActive=" + maxActive + ")");
    	
    	TimerTask printTask = new TimerTask() {
    		
    		@Override
    		public void run() {
    			printRate();
    		}
    	};
    	
    	new Timer(false).schedule(printTask, 3 * 1000, 3 * 1000); 
    	
    	
    	httpClient.setMaxActive(maxActive);
    	
    	
    	while (true) {
    		try {

    	    	GetRequest request = new GetRequest("http://" + host + ":" + port + "/?cmd=login");
    	    	
    			IHttpResponseHandler respHdl = new IHttpResponseHandler() {
    				private long sendTime = System.currentTimeMillis();

        	    	@Execution(Execution.NONTHREADED)
        	    	@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
    				public void onResponse(IHttpResponse response) throws IOException {
    					registerResponse(sendTime);
    				}

        	    	@Execution(Execution.NONTHREADED)
    				public void onException(IOException ioe) throws IOException {
    					
    				}
    			};

    			httpClient.send(request, respHdl);
    			QAUtil.sleep(waittime);
    			
    		} catch (IOException ioe) {
    			System.out.println(ioe.toString());
    		}		
    	}
	}
    
    
    private synchronized void registerResponse(long sendTime) {
    	counts++;
    }
    
    
    
    private synchronized void printRate() {
    	
    	long current = System.currentTimeMillis();
    	
    	int c = counts;
    	long t = lastTime;
    	
    	counts = 0;
    	lastTime = current;
    	
    	
    	try {
    		System.out.println((c * 1000) / (current - t) + " req/sec (active cons " + httpClient.getNumActive() + ", idle cons " + httpClient.getNumIdle() + ", destroyed cons " + httpClient.getNumDestroyed() + ")");
    	} catch (Exception ignore) { }    	
    }
}
