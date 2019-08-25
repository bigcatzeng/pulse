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
package org.xlightweb.client;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xlightweb.HttpRequest;
import org.xlightweb.HttpRequestHeaderWrapper;
import org.xlightweb.HttpUtils;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpRequestHeader;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.Supports100Continue;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;





/**
 * Proxy handler 
 *  
 * @author grro@xlightweb.org, ortjco
 */
@Supports100Continue
final class ProxyHandler implements IHttpRequestHandler {
	
	private static final Logger LOG = Logger.getLogger(ProxyHandler.class.getName());
	
	
	 /**
      *  ProxyHandler is unsynchronized by config. See HttpUtils$RequestHandlerInfo
      */

	
	
	private final HttpClient httpClient;

	private String proxyHost;
	private int proxyPort = -1;
	
	private String securedProxyHost;
	private int securedProxyPort = -1;
	
	private String proxyUser;
	private String proxyPassword;
	private String proxyUserPassword;
	

	
	public ProxyHandler(HttpClient httpClient) {
	    this.httpClient = httpClient;
	}

	
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public void setSecuredProxyHost(String host) {
		if ((host != null) && (host.length() == 0)) {
			host = null;
		}
		this.securedProxyHost = host;
	}

	public void setSecuredProxyPort(int proxyPort) {
		this.securedProxyPort = proxyPort;
	}

	
	public void setProxyUser(String proxyUser) {
		this.proxyUser = proxyUser;
		
		if (proxyPassword != null) {
			try {
				proxyUserPassword = new String(HttpUtils.encodeBase64((proxyUser + ":" + proxyPassword).getBytes()));
			} catch (IOException ioe) {
				throw new RuntimeException(ioe.toString());
			}
		}
	}

	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
		if (proxyUser != null) {
			try {
				proxyUserPassword = new String(HttpUtils.encodeBase64((proxyUser + ":" + proxyPassword).getBytes()));
			} catch (IOException ioe) {
				throw new RuntimeException(ioe.toString());
			}
		}
	
	}
	
	public void setProxyHost(String host) {
		if ((host != null) && (host.length() == 0)) {
			host = null;
		}
		this.proxyHost = host;
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public void onRequest(IHttpExchange exchange) throws IOException {
		
		IHttpRequest request = exchange.getRequest();
		
		if (!hasProxytoUse(request)) {
			exchange.forward(request);
			return;
		}
			
		if (request.isSecure()) {
			connectAndForward(exchange, true);
			
		} else {
		    // is upgrade request?
		    if ((request.getHeader("Connection") != null) && (request.getHeader("Connection").equalsIgnoreCase("Upgrade"))) {
		        connectAndForward(exchange, false);
		        
		    } else {
		        forward(exchange);
		    }
		}
	}
	
	
	private boolean hasProxytoUse(IHttpRequest request) {

		if (!request.isSecure() && (proxyHost == null)) {
			return false;
		}
			
		if (request.isSecure() && ((securedProxyHost == null) && (proxyHost == null))) {
			return false;
		}
		
		return true;
	}
	
	
	private void forward(IHttpExchange exchange) throws IOException {
		
		IHttpRequest request = exchange.getRequest();
		
		if (proxyUser != null) {
			if (proxyUserPassword != null) {
				request.addHeader("Proxy-Authorization", "Basic " + proxyUserPassword);
				
			} else {
				if (LOG.isLoggable(Level.FINE)) {
					LOG.fine("proxy password is not send send error");
				}
				exchange.sendError(new IOException("proxy user password is not set (hint: usage <HttpClient>.setProxyPassword(...)"));
				return;
			}
		}
		
		IHttpRequest wrappedRequest = null; 
		if (request.hasBody()) {
			wrappedRequest = new HttpRequest(new SimpleForwardRequestHeaderWrapper(request.getRequestHeader()), request.getNonBlockingBody());
		} else {
			wrappedRequest = new HttpRequest(new SimpleForwardRequestHeaderWrapper(request.getRequestHeader()));
		}
		
		exchange.forward(wrappedRequest);
	}
	
	
	private void connectAndForward(IHttpExchange exchange, boolean isActivateSecuredMode) throws IOException {
	    String host = proxyHost;
	    int port = proxyPort;
	    if (securedProxyHost != null) {
        host = securedProxyHost;
	        port = securedProxyPort;
	    }
	    
	    
        if (LOG.isLoggable(Level.FINE)) {
            if (isActivateSecuredMode) {
                LOG.fine("opening a secured tunnel to " + host + ":" + port);
            } else {
                LOG.fine("opening a plain tunnel to " + host + ":" + port);
            }
        }

	    ConnectAndForwardRelay relay = new ConnectAndForwardRelay(exchange, proxyUserPassword, isActivateSecuredMode);
	    httpClient.getUnderlyingConnectionPool().getNonBlockingConnection(host, port, relay, false);
	}


	
	private final class SimpleForwardRequestHeaderWrapper extends HttpRequestHeaderWrapper {
		
		public SimpleForwardRequestHeaderWrapper(IHttpRequestHeader delegate) {
			super(delegate);
		}
		
		public URL getRequestUrl() {
			
			try {
				URL orgURL = getWrappedRequestHeader().getRequestUrl();
				
				int port = proxyPort;
				if (port == -1) {
					port = 80;
				}
				
				URL url = new URL("http", proxyHost, port, orgURL.getFile());
				
				return url;
			} catch (MalformedURLException murl) {
				throw new RuntimeException(murl.toString());
			}
		}
		
		@Override
		public String toString() {
			String s = getWrappedRequestHeader().toString();
			int idx = s.indexOf("\r\n");
		
			StringBuilder sb = new StringBuilder(getMethod() + " http://" + getHost() + getRequestURI());
			if (getQueryString() != null) {
				sb.append("?");
				sb.append(getQueryString());
			}

			sb.append(" ");
			sb.append(getProtocol());
			
			sb.append("\r\n");

			sb.append(s.substring(idx + 2, s.length()));
			
			return sb.toString();
		}		
	}
	
	
	
	private final class ConnectAndForwardRelay implements IDataHandler, IConnectHandler {
	    private final IHttpExchange exchange;
	    private final String proxyAuthorization;

	    private final boolean isActivateSecuredMode;
	    private final AtomicBoolean isHandshake = new AtomicBoolean(false);

	    public ConnectAndForwardRelay(IHttpExchange exchange, String proxyAuthorization, boolean isActivateSecuredMode) {
	        this.exchange = exchange;
	        this.proxyAuthorization = proxyAuthorization;
	        this.isActivateSecuredMode = isActivateSecuredMode;
	    }
	    
	    public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
	        if (isHandshake.getAndSet(false)) {
	            String header = connection.readStringByDelimiter("\r\n\r\n");

	            String[] lines = header.split("\r\n");

	            int idx = lines[0].indexOf(" ");
	            String statusAndReason = lines[0].substring(idx, lines[0].length()).trim();

	            if (!statusAndReason.startsWith("200")) {
	                exchange.sendError(new IOException("could not set up tunnel to " + exchange.getRequest().getHeader("Host") + " got " + statusAndReason));
	                connection.close();
	                return true;
	            } 

	            if (isActivateSecuredMode) {
	                connection.activateSecuredMode();
	            }

	            send(connection);
	        }

	        return true;
	    }

	    public boolean onConnect(INonBlockingConnection connection) throws IOException, BufferUnderflowException {
	        if (connection.getReadBufferVersion() == 0) {
	            handshakeAndSendLater(connection);
	        }
	        else {
	            send(connection);
	        }

	        return true;
	    }

	    private void handshakeAndSendLater(INonBlockingConnection connection) throws IOException {
	        isHandshake.set(true);

	        String host = exchange.getRequest().getHeader("Host");
	        String forwardHost = host;
	        int forwardPort = 443;

	        int idx = host.lastIndexOf(":");
	        if (idx != -1) {
	            forwardPort = Integer.parseInt(host.substring(idx + 1, host.length()));
	            forwardHost = host.substring(0, idx);
	        }

	        StringBuilder sb = new StringBuilder();

	        sb.append("CONNECT ").append(forwardHost).append(":").append(forwardPort).append(" HTTP/1.1\r\n" + "Host: ").append(host).append("\r\n" + "User-Agent: xLightweb/").append(HttpUtils.getImplementationVersion()).append("\r\n");

	        if (proxyAuthorization != null) {
	            sb.append("Proxy-Authorization: Basic ").append(proxyAuthorization).append("\r\n");
	        }

	        sb.append("Proxy-Connection: keep-alive\r\n" + "\r\n");

	        connection.write(sb.toString());
	        connection.flush();
	    }

	    public void send(INonBlockingConnection connection) throws IOException {
	        connection.setHandler(null);

	        final HttpClientConnection tunnel = new HttpClientConnection(connection);

	        tunnel.setResponseTimeoutMillis(httpClient.getResponseTimeoutMillis());
	        tunnel.setBodyDataReceiveTimeoutMillis(httpClient.getBodyDataReceiveTimeoutMillis());

	        tunnel.setAutocloseAfterResponse(true);
	        

	        IHttpResponseHandler respHdl = new IHttpResponseHandler() {
                
	            public void onResponse(IHttpResponse response) throws IOException {
	                HttpClientConnection.addConnectionAttribute(response.getResponseHeader(), tunnel);
	                exchange.send(response);
	            }

	            public void onException(IOException ioe) throws IOException {
	                exchange.sendError(ioe);
	            }
            };

	        tunnel.send(exchange.getRequest(), respHdl);
	    }

	}	
}
