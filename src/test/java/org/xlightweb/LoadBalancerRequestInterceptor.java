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
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xlightweb.BadMessageException;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.client.HttpClient;
import org.xsocket.Execution;
import org.xsocket.ILifeCycle;



public class LoadBalancerRequestInterceptor implements IHttpRequestHandler, ILifeCycle {
	private final Map<String, List<String>> servers = new HashMap<String, List<String>>();
	private HttpClient httpClient;
	
	public void addVirtualServer(String virtualUrl, String... realServers) {
		servers.put(virtualUrl, Arrays.asList(realServers));
	}
	
	public void onInit() {
		httpClient = new HttpClient();
	}
	
	public void onDestroy() throws IOException {
		httpClient.close();
	}
	
	public void onRequest(final IHttpExchange exchange) throws IOException, BadMessageException {
		IHttpRequest request = exchange.getRequest();
					
		for (String virtualUrl : servers.keySet()) {
			if (request.getRequestUrl().toString().startsWith(virtualUrl)) {
				String id = request.getRequiredStringParameter("id");
				
				int idx = id.hashCode() % servers.get(virtualUrl).size();
				if (idx < 0) {
					idx *= -1;
				}

				String server = servers.get(virtualUrl).get(idx);
				String[] hostPortPair = server.split(":"); 
				String host = hostPortPair[0];
				int port = Integer.parseInt(hostPortPair[1]);
					
				URL url = request.getRequestUrl();
				URL newUrl = new URL(url.getProtocol(), host, port, url.getFile());
				request.setRequestUrl(newUrl);

				// proxy header handling (remove hop-by-hop headers, ...)
				// ...

				IHttpResponseHandler respHdl = new IHttpResponseHandler() {

					@Execution(Execution.NONTHREADED)
					public void onResponse(IHttpResponse response) throws IOException {
						exchange.send(response);
					}
					
					@Execution(Execution.NONTHREADED)
					public void onException(IOException ioe) throws IOException {
						exchange.sendError(ioe);
					}
				};
				httpClient.send(request, respHdl);
				return;
			}
		}
		
		exchange.forward(request);
	}
}
