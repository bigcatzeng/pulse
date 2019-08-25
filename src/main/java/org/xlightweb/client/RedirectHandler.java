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
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xlightweb.BodyDataSink;
import org.xlightweb.HttpRequest;
import org.xlightweb.IBodyDestroyListener;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpRequestHeader;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.IHttpResponseHeader;
import org.xlightweb.ProtocolException;
import org.xlightweb.Supports100Continue;
import org.xlightweb.client.DuplicatingBodyForwarder.BodyDataSinkAdapter;
import org.xlightweb.client.DuplicatingBodyForwarder.ISink;
import org.xlightweb.client.DuplicatingBodyForwarder.InMemorySink;
import org.xlightweb.client.HttpClient.FollowsRedirectMode;
import org.xsocket.Execution;
import org.xsocket.connection.IConnection.FlushMode;




/**
 * Auto redirect handler 
 *  
 * @author grro@xlightweb.org
 */
@Supports100Continue
final class RedirectHandler implements IHttpRequestHandler {
    
    
    /**
     * RedirectHandler is unsynchronized by config. See HttpUtils$RequestHandlerInfo
     */
	
	private static final Logger LOG = Logger.getLogger(RedirectHandler.class.getName());

	private static final String COUNT_REDIRECTS_KEY = "org.xlightweb.client.RedirectHandler.countRedirects";
	
	private final HttpClient httpClient; 
	
	
	/**
	 * constructor 
	 * 
	 * @param httpClient  the http client to perform the redirects
	 */
	public RedirectHandler(HttpClient httpClient) {
		this.httpClient = httpClient; 
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void onRequest(IHttpExchange exchange) throws IOException {
	
		IHttpRequest request = exchange.getRequest();
		
        // has body (e.g. PUT, POST) 
        if (request.hasBody()) {
            BodyRedirectResponseHandler redirectHandler = new BodyRedirectResponseHandler(request.getRequestHeader().copy(), exchange);
            final BodyDataSink dataSink = exchange.forward(request.getRequestHeader(), redirectHandler);
            dataSink.setFlushmode(FlushMode.ASYNC);
            
            // BodyDataSink
            DuplicatingBodyForwarder forwarder = new DuplicatingBodyForwarder(request.getNonBlockingBody(), new BodyDataSinkAdapter(dataSink), redirectHandler);
            HttpClientConnection.setDataHandlerSilence(request.getNonBlockingBody(), forwarder);
            
        // ... no (e.g. GET, DELETE)
        } else {
            exchange.forward(request, new BodylessRedirectResponseHandler(request.getRequestHeader().copy(), exchange));
        }
	}

	
    
    abstract class AbstractRedirectResponseHandler implements IHttpResponseHandler {

        private IHttpRequestHeader requestHeader = null;
        private IHttpExchange exchange = null;
        
        private Integer countRedirects = 0;

        
        AbstractRedirectResponseHandler(IHttpRequestHeader requestHeader, IHttpExchange exchange) throws IOException {
            this.requestHeader = requestHeader;
            this.exchange = exchange;
            
            countRedirects = (Integer) requestHeader.getAttribute(COUNT_REDIRECTS_KEY);
            if (countRedirects == null) {
                countRedirects = 0;
            }
        }
        
        protected final int incCountRedirect() {
            return ++countRedirects;
        }
        
        protected final IHttpRequestHeader getRequestHeader() {
            return requestHeader;
        }
        
        protected final IHttpExchange getExchange() {
            return exchange;
        }
        

        @Execution(Execution.NONTHREADED) 
        public void onResponse(IHttpResponse response) throws IOException {
            
            if (isRedirectResponse(requestHeader, response.getResponseHeader())) {

                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("got redirect response: " + response.getResponseHeader());
                }
                
                // get the new location
                URL newLocation = getRedirectURI(response, requestHeader.isSecure(), requestHeader.getServerName(), requestHeader.getServerPort());
                
                if (newLocation == null) {
                    exchange.send(response);
                } else {
                    
                    // switch to GET?
                    if ((response.getStatus() == 303) || ((response.getStatus() == 302)) && httpClient.isTreat302RedirectAs303()){
                        requestHeader.setMethod("GET");
                        requestHeader.removeHeader("Content-Type");
                        requestHeader.removeHeader("Content-Length");
                        requestHeader.removeHeader("Transfer-Encoding");
                        requestHeader.removeHeader("Trailers");
                        requestHeader.removeHeader("Upgrade");
                        
                    }
                    
                    requestHeader.setRequestUrl(newLocation);
    
                    if (countRedirects < httpClient.getMaxRedirects()) {
    
                        // if is secured perform redirect request in threaded context -> implicit startSSL() has to be performed within threaded context   
                        if (requestHeader.isSecure()) {
                            Runnable task = new Runnable() {
                                public void run() {
                                    sendRedirectedRequest();
                                }
                            };
                            httpClient.getWorkerpool().execute(task);
                            
                        } else {
                            sendRedirectedRequest();
                        }
                        
                        
                    } else {
                        exchange.sendError(new IOException("max redirects " + httpClient.getMaxRedirects() + " reached. request will not be executed: " + requestHeader));
                    }
                }
            } else {
                exchange.send(response);
            }
        }

        abstract void sendRedirectedRequest();
        
        
        @Execution(Execution.NONTHREADED)
        public final void onException(final IOException ioe) {
            exchange.sendError(ioe);
        }
        

        final boolean isRedirectResponse(IHttpRequestHeader requestHeader, IHttpResponseHeader responseHeader) {
            
            switch (responseHeader.getStatus()) {
            
            // 301 Moved permanently
            case 301:
                return isRedirectModeAllOrIsGetOrHeadRequest();
                
            // 302 found
            case 302:
                if (httpClient.isTreat302RedirectAs303() && ((requestHeader.getMethod().equalsIgnoreCase("POST") || requestHeader.getMethod().equalsIgnoreCase("PUT")))) {
                    return true;
                }
                return isRedirectModeAllOrIsGetOrHeadRequest();

                
            // 303 See other
            case 303:
                if (requestHeader.getMethod().equalsIgnoreCase("POST") || requestHeader.getMethod().equalsIgnoreCase("PUT")) {
                    return true;
                } else {
                    return false;
                }
            

                // 307 temporary redirect
            case 307:
                return isRedirectModeAllOrIsGetOrHeadRequest();

            default:
                return false;
            }
        }
        
        private boolean isRedirectModeAllOrIsGetOrHeadRequest() {
            if ((httpClient.getFollowsRedirectMode() == FollowsRedirectMode.ALL) || 
                ((httpClient.getFollowsRedirectMode() == FollowsRedirectMode.RFC) && isGetOrHeadMethod())) {
                return true;
            } else {
                return false;
            }
        }
        
        private boolean isGetOrHeadMethod() {
            return requestHeader.getMethod().equalsIgnoreCase("GET") || requestHeader.getMethod().equalsIgnoreCase("HEAD");
        }
        
        private URL getRedirectURI(IHttpResponse response, boolean isSSL, String originalHost, int originalPort) {
            
            String location = response.getHeader("Location");
            
            if (location != null) {
                
                try {
                    
                    if (isRelativeUrl(location)) {
                        if (isSSL) {
                            if (originalPort == -1) {
                                return new URL("https://" + originalHost + location);
                            } else {
                                return new URL("https://" + originalHost + ":" + originalPort + location);
                            }
                        } else {
                            if (originalPort == -1) {
                                return new URL("http://" + originalHost + location);
                            } else { 
                                return new URL("http://" + originalHost + ":" + originalPort + location);
                            }
                        }
                        
                    } else {
                        return new URL(location);
                    }
                    
                } catch (MalformedURLException e) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("could not create relocation url . reason " + e.toString());
                    }
                }
                    
            }
        
            return null;
        }
        
        
        private boolean isRelativeUrl(String url) {
            url = url.toUpperCase();
            return (!url.startsWith("HTTP") && !url.startsWith("HTTPS")); 
        }
    }   
	
	
	   
    /**
     * ResponseHandler is unsynchronized by config. See HttpUtils$ResponseHandlerInfo
     */
    @Supports100Continue
    private final class BodyRedirectResponseHandler extends AbstractRedirectResponseHandler implements ISink {

        private final InMemorySink inMemorySink = new InMemorySink();
        private boolean is100ContinueReceived = false;
        private boolean isBufferIsActivated = true;
        
        BodyRedirectResponseHandler(IHttpRequestHeader requestHeader, IHttpExchange exchange) throws IOException {
            super(requestHeader, exchange);
        }
        
        @Override
        public void onResponse(IHttpResponse response) throws IOException {

            if (response.getStatus() == 100) {
                is100ContinueReceived = true;
                
                if ((response.getAttribute(HttpClientConnection.TIMEOUT_100_CONTINUE_RESPONSE) != null) && ((Boolean) response.getAttribute(HttpClientConnection.TIMEOUT_100_CONTINUE_RESPONSE) == true)) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Got auto-generated 100-continue response " + response.getReason());
                    }
                    
                } else {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Got 100-continue response. destroying buffer body data sink");
                    }
                    isBufferIsActivated = false;
                    inMemorySink.destroy();
                }
                
            } else if (is100ContinueReceived && isRedirectResponse(getRequestHeader(), response.getResponseHeader())) {
                onException(new ProtocolException("Response order error. Got a redirect response after a 100-continue response", response.getResponseHeader()));
            }

            super.onResponse(response);
        }
        
        public void onData(ByteBuffer data) throws IOException {
            if (isBufferIsActivated) {
                inMemorySink.onData(data);
            }
        }
        
        
        public void close() throws IOException {
            inMemorySink.close();
        }
        
        public void destroy() {
            inMemorySink.destroy();
        }
        
        public void setDestroyListener(IBodyDestroyListener destroyListener) {
            inMemorySink.setDestroyListener(destroyListener);
        }

        public String getId() {
            return inMemorySink.getId();
        }
                        
        
        protected void sendRedirectedRequest() {
            
            Runnable task = new Runnable() {
                 
                 public void run() {
                     if (LOG.isLoggable(Level.FINE)) {
                         LOG.fine("send redirected request (body size " + inMemorySink.getSize() + "): " + getRequestHeader());
                     }
                     
                     try {
                         getRequestHeader().setAttribute(COUNT_REDIRECTS_KEY, incCountRedirect());
                         getRequestHeader().setAttribute(CookieHandler.COOKIE_WARNING_KEY, false);
                         
                         IHttpResponseHandler respHdl = new IHttpResponseHandler() {
                             
                             @Execution(Execution.NONTHREADED)
                             public void onResponse(IHttpResponse response) throws IOException {
                                 getExchange().send(response);
                             }
                             
                             @Execution(Execution.NONTHREADED)
                             public void onException(IOException ioe) throws IOException {
                                 getExchange().sendError(ioe);
                             }
                         };
                         
                         if (getRequestHeader().getMethod().equals("GET")) {
                             httpClient.send(new HttpRequest(getRequestHeader()), respHdl);
                             
                         } else {
                             BodyDataSink ds = httpClient.send(getRequestHeader(), respHdl);
                             ds.setFlushmode(FlushMode.ASYNC);
                             
                             inMemorySink.forwardTo(new BodyDataSinkAdapter(ds));
                         }
                         
                     } catch (IOException ioe) {
                         getExchange().sendError(new IOException("can execute redirect request " + getRequestHeader() + " reason: " + ioe.toString()));
                     }
                 }
             };
             
             // run in own thread to prevent dead locks
             httpClient.getWorkerpool().execute(task);
         }
    }
    
    
	
    /**
     * ResponseHandler is unsynchronized by config. See HttpUtils$ResponseHandlerInfo
     */
    @Supports100Continue
	private final class BodylessRedirectResponseHandler extends AbstractRedirectResponseHandler {

		BodylessRedirectResponseHandler(IHttpRequestHeader requestHeader, IHttpExchange exchange) throws IOException {
		    super(requestHeader, exchange);
		}
		
		
		protected void sendRedirectedRequest() {
		    
		   Runnable task = new Runnable() {
		        
		        public void run() {
		            if (LOG.isLoggable(Level.FINE)) {
		                LOG.fine("send redirected request: " + getRequestHeader());
		            }
		            try {
		                getRequestHeader().setAttribute(COUNT_REDIRECTS_KEY, incCountRedirect());
		                getRequestHeader().setAttribute(CookieHandler.COOKIE_WARNING_KEY, false);
		                
		                IHttpResponseHandler respHdl = new IHttpResponseHandler() {
		                    
		                    @Execution(Execution.NONTHREADED)
		                    public void onResponse(IHttpResponse response) throws IOException {
		                        getExchange().send(response);
		                    }
		                    
		                    @Execution(Execution.NONTHREADED)
		                    public void onException(IOException ioe) throws IOException {
		                        getExchange().sendError(ioe);
		                    }
		                };
		                
		                httpClient.send(new HttpRequest(getRequestHeader()), respHdl);
		            } catch (IOException ioe) {
		                getExchange().sendError(new IOException("can execute redirect request " + getRequestHeader() + " reason: " + ioe.toString()));
		            }
		        }
		    };
		    
		    // run in own thread to prevent dead locks
		    httpClient.getWorkerpool().execute(task);
		}
	}	
}
