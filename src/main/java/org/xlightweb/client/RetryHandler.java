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

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xlightweb.BodyDataSink;
import org.xlightweb.IBodyDestroyListener;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHeader;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.IHttpRequest;
import org.xlightweb.HttpRequest;
import org.xlightweb.IHttpRequestHeader;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.ProtocolException;
import org.xlightweb.Supports100Continue;
import org.xlightweb.client.DuplicatingBodyForwarder.ISink;
import org.xlightweb.client.DuplicatingBodyForwarder.BodyDataSinkAdapter;
import org.xlightweb.client.DuplicatingBodyForwarder.InMemorySink;
import org.xsocket.DataConverter;
import org.xsocket.Execution;
import org.xsocket.connection.IConnection.FlushMode;




/*
 * Retry handler 
 *  
 * @author grro@xlightweb.org
 */
@Supports100Continue
final class RetryHandler implements IHttpRequestHandler {
    
    
    /**
     * RetryHandler is unsynchronized by config. See HttpUtils$RequestHandlerInfo
     */
	
	private static final Logger LOG = Logger.getLogger(RetryHandler.class.getName());

	private static final int MAX_BUFFER_SIZE = Integer.parseInt(System.getProperty("org.xlightweb.retryhandler.maxBufferSize", "4194304"));
	
	static final String RETRY_KEY = "org.xlightweb.client.RetryHandler.retry";
	private static final String RETRY_COUNT_KEY = "org.xlightweb.client.RetryHandler.countTrials";
	private static final String RETRY_PREVIOUS_EXCEPTION_KEY = "org.xlightweb.client.RetryHandler.previousException";
	private static final String RETRY_PREVIOUS_ERROR_RESPONSE_KEY = "org.xlightweb.client.RetryHandler.previousErrorResponse";

	
	private final HttpClient httpClient; 
	    
    
	
	public RetryHandler(HttpClient httpClient) {
	    this.httpClient = httpClient;
    }
	

	
	static boolean isRetryable(IOException ioe) {
        if ((ProtocolException.class.isAssignableFrom(ioe.getClass())) &&
            (!isErrorStatusCodeRecevied((ProtocolException) ioe))) {
            return true;
            
        } else {
            return false;
        }
	}

    
    private static boolean isErrorStatusCodeRecevied(ProtocolException pe) {
        IHttpResponseHeader header = (IHttpResponseHeader) HttpClientConnection.getReceivedHeader(pe);
        if ((header != null) && (header.getStatus() >= 400)) {
            return true;
        }
        
        return false;
    }


	private static boolean isRetryable(int status) {
        if ((status == 408) ||   // request Time-out
            (status == 421) ||   // too many connections from your client-side address
            (status == 423) ||   // currently locked
            (status == 500) ||   // generic unexpected server-side error
            (status == 502) ||   // bad gateway
            (status == 503) ||   // service unavailable, e.g. overloaded
            (status == 504) ||   // gateway Time-out
            (status == 509)) {   // Bandwidth Limit Exceeded
            return true;
        } else {
            return false;
        }
	}
	
	

    private static boolean isErrorStatus(int status) {
        if ((status == 408) ||   // request Time-out
            (status == 421) ||   // too many connections from your client-side address
            (status == 423) ||   // currently locked
            (status >= 500)) {
            return true;
        } else {
            return false;
        }
    }
	
	/**
	 * {@inheritDoc}
	 */
	public void onRequest(final IHttpExchange exchange) throws IOException {

	    IHttpRequest request = exchange.getRequest();
	    
	    Boolean retry = (Boolean) request.getAttribute(RETRY_KEY);
	    if (retry == null) {
	        retry = true;
	    }
	    
	    if (request.getHeader("Upgrade") != null) {
	        retry = false;
	    }
	    
	    if (retry) {
    	    // handle GET and Delete request 
    	    if (request.getMethod().equalsIgnoreCase("GET")  || request.getMethod().equalsIgnoreCase("DELETE")) {
    	        BodylessRetryResponseHandler retryHandler = new BodylessRetryResponseHandler(exchange, request.getRequestHeader().copy());
    	        exchange.forward(request, retryHandler);
    	        return;
    	        
    	    // handle PUT
    	    } else if (request.getMethod().equalsIgnoreCase("PUT")) {  
    	        BodyRetryResponseHandler retryHandler = new BodyRetryResponseHandler(exchange, request.getRequestHeader().copy());
    	        
    	        final BodyDataSink dataSink = exchange.forward(request.getRequestHeader(), retryHandler);
    	        dataSink.setFlushmode(FlushMode.ASYNC);
    	        
    	        // BodyDataSink
    	        DuplicatingBodyForwarder forwarder = new DuplicatingBodyForwarder(request.getNonBlockingBody(), new BodyDataSinkAdapter(dataSink), retryHandler);
    	        HttpClientConnection.setDataHandlerSilence(request.getNonBlockingBody(), forwarder);
    	        return;
    	    }
	    }
	    
	    exchange.forward(request);
    }
	
	
	
	private static void setPreviousError(IHttpRequestHeader header, IOException ioe, IHttpResponse errorResponse) {
	    if ((header.getAttribute(RETRY_PREVIOUS_EXCEPTION_KEY) == null) && (header.getAttribute(RETRY_PREVIOUS_ERROR_RESPONSE_KEY) == null)) {
	        if (errorResponse != null) {
	            header.setAttribute(RETRY_PREVIOUS_ERROR_RESPONSE_KEY, errorResponse);
	        } else  {
	            header.setAttribute(RETRY_PREVIOUS_EXCEPTION_KEY, ioe);
	        }
	    }
	}

	
	private static void sendError(IHttpExchange exchange, IHttpRequestHeader header, IOException ioe, IHttpResponse errorResponse) { 
	    
	    if (header.getAttribute(RETRY_PREVIOUS_ERROR_RESPONSE_KEY) != null) {
	        try {
	            exchange.send((IHttpResponse) header.getAttribute(RETRY_PREVIOUS_ERROR_RESPONSE_KEY));
	        } catch (IOException e) {
	            exchange.sendError(e);
	        }
	        
	    } else if(header.getAttribute(RETRY_PREVIOUS_EXCEPTION_KEY) != null) {
	        exchange.sendError((IOException) header.getAttribute(RETRY_PREVIOUS_EXCEPTION_KEY));
	        
	    } else if (errorResponse != null) {
	        try {
                exchange.send(errorResponse);
            } catch (IOException e) {
                exchange.sendError(e);
            }
	        
	    } else if (ioe != null) {
	        exchange.sendError(ioe);
	        
	    } else {
	        exchange.sendError(500);
	    }
	}

	
    /**
     * BodyRetryResponseHandler is unsynchronized by config. See HttpUtils$RequestHandlerInfo
     */
	@Supports100Continue
	private final class BodyRetryResponseHandler implements IHttpResponseHandler, ISink {
	    
	    private final IHttpExchange exchange;
	    private final IHttpRequestHeader requestHeader;
	    
	    
	    private final InMemorySink inMemorySink = new InMemorySink(MAX_BUFFER_SIZE) {
	        @Override
	        void onMaxBufferSizeExceeded() {
	            if (LOG.isLoggable(Level.FINE)) {
	                LOG.fine("max buffer size (" + DataConverter.toFormatedBytesSize(MAX_BUFFER_SIZE) + ") for request body exceeded. Retry support for this request is deactivated (set the buffer size by using the system property 'org.xlightweb.retryhandler.maxBufferSize')");
	            }
	            super.onMaxBufferSizeExceeded();
	        }
	    };
	    private Integer countTrials;

	    
	    BodyRetryResponseHandler(IHttpExchange exchange, IHttpRequestHeader requestHeader) {
	        this.exchange = exchange;
	        this.requestHeader = requestHeader;
	        
	        countTrials = (Integer) requestHeader.getAttribute(RETRY_COUNT_KEY);
	        if (countTrials == null) {
	            countTrials = 0;
	        }
	    }
	    
	    
	    public void onData(ByteBuffer data) throws IOException {
	        inMemorySink.onData(data);
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
        	    
	    public void onResponse(IHttpResponse response) throws IOException {
	        if (isErrorStatus(response.getStatus())) {
	            
	            if (isRetryable(response.getStatus())) {
    	            setPreviousError(requestHeader, null, response);
    	            
    	            if ((countTrials < httpClient.getMaxRetries()) && !inMemorySink.isDestroyed()) {
        	            if (LOG.isLoggable(Level.FINE)) {
        	                LOG.fine("retry sending request (retry " + (countTrials + 1) + " of " + httpClient.getMaxRetries() + "). got " + response.getStatus() + " " + response.getReason() + " by calling " + requestHeader.getRequestUrl().toString());
        	            }
                        sendRetry();
                        return;
    	            }
	            }
	            
                sendError(exchange, requestHeader, null, response);
                
	        } else {
	        	exchange.send(response);
	        }
	    }

	    
	    public void onException(IOException ioe) throws IOException {
	        if (isRetryable(ioe)) {
	            setPreviousError(requestHeader, ioe, null);

	            if ((countTrials < httpClient.getMaxRetries()) && !inMemorySink.isDestroyed()) {
    	            if (LOG.isLoggable(Level.FINE)) {
    	                LOG.fine("retry sending request (retry " + (countTrials + 1) + " of " + httpClient.getMaxRetries() + "). I/O exception " + ioe.toString() + " caught when processing request " + requestHeader.getRequestUrl().toString());
    	            }
                    sendRetry();
                    
	            } else {
	                sendError(exchange, requestHeader, ioe, null);
	            }
                
            } else {
                sendError(exchange, requestHeader, ioe, null);
            }
	    }

	    
	    
        private void sendRetry() throws IOException {
            
            Runnable task = new Runnable() {
                
                public void run() {
                
                    try {
                        requestHeader.setAttribute(RETRY_COUNT_KEY, ++countTrials);
                        requestHeader.setAttribute(CookieHandler.COOKIE_WARNING_KEY, false);
                            
                        IHttpResponseHandler respHdl = new IHttpResponseHandler() {
                            
                            @Execution(Execution.NONTHREADED)
                            public void onResponse(IHttpResponse response) throws IOException {
                                if (LOG.isLoggable(Level.FINE)) {
                                    LOG.fine("forward response");
                                }
                                exchange.send(response);
                            }
                            
                            @Execution(Execution.NONTHREADED)
                            public void onException(IOException ioe) throws IOException {
                                sendError(exchange, requestHeader, ioe, null);
                            }
                        };
                        
                        BodyDataSink ds = httpClient.send(requestHeader, respHdl);
                        ds.setFlushmode(FlushMode.ASYNC);
                        
                        boolean isForwarding = inMemorySink.forwardTo(new BodyDataSinkAdapter(ds));
                        if (!isForwarding) {
                            sendError(exchange, requestHeader, null, null);
                        } 
                        
                    } catch (IOException ioe) {
                        sendError(exchange, requestHeader, ioe, null);
                    }
                }
            };
                    
            httpClient.getWorkerpool().execute(task);
        }
	}
	
	

	
    /**
     * BodyRetryResponseHandler is unsynchronized by config. See HttpUtils$RequestHandlerInfo
     */
	@Supports100Continue
    private final class BodylessRetryResponseHandler implements IHttpResponseHandler {

        private final AtomicBoolean isHandled = new AtomicBoolean(false);
        private final IHttpExchange exchange;
        private final IHttpRequestHeader requestHeader;
        private Integer countTrials;
        
        
        BodylessRetryResponseHandler(IHttpExchange exchange, IHttpRequestHeader requestHeader) {
            this.exchange = exchange;
            this.requestHeader = requestHeader;

            countTrials = (Integer) requestHeader.getAttribute(RETRY_COUNT_KEY);
            if (countTrials == null) {
                countTrials = 0;
            }
        }
        
        
        public void onResponse(IHttpResponse response) throws IOException {

            if (!isHandled.getAndSet(true)) {
    	    
                if (isErrorStatus(response.getStatus())) {
                    
                    if (isRetryable(response.getStatus())) {
        	            setPreviousError(requestHeader, null, response);
        	            
        	            if (countTrials < httpClient.getMaxRetries()) {
        	                if (LOG.isLoggable(Level.FINE)) {
        	                    LOG.fine("retry sending request (retry " + (countTrials + 1) + " of " + httpClient.getMaxRetries() + "). got " + response.getStatus() + " " + response.getReason() + " by calling " + requestHeader.getRequestUrl().toString());
        	                }
        	                sendRetry();
        	                return;
        	            }
                    }
                    
                    sendError(exchange, requestHeader, null, response);
                    
    	        } else {
    	        	exchange.send(response);
    	        }
            }
        }
        
        public void onException(IOException ioe) throws IOException {
            
            if (!isHandled.getAndSet(true)) {
                setPreviousError(requestHeader, ioe, null);
                
                if (isRetryable(ioe)) {
                    
                    if (countTrials < httpClient.getMaxRetries()) {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("retry sending request (retry " + (countTrials + 1) + " of " + httpClient.getMaxRetries() + "). I/O exception " + ioe.toString() + " caught when processing request "  + requestHeader.getRequestUrl().toString());
                        }
                        sendRetry();
                        
                    } else {
                        sendError(exchange, requestHeader, ioe, null);
                    }
                    
                } else {
                    sendError(exchange, requestHeader, ioe, null);
                }
            }
        }

        
        private void sendRetry() throws IOException {
            
        
            Runnable task = new Runnable() {
                
                public void run() {
                    
                    try {
                        requestHeader.setAttribute(RETRY_COUNT_KEY, ++countTrials);
                        requestHeader.setAttribute(CookieHandler.COOKIE_WARNING_KEY, false);
                            
                        IHttpResponseHandler respHdl = new IHttpResponseHandler() {
                            
                            @Execution(Execution.NONTHREADED)
                            public void onResponse(IHttpResponse response) throws IOException {
                                exchange.send(response);
                            }
                            
                            @Execution(Execution.NONTHREADED)
                            public void onException(IOException ioe) throws IOException {
                                sendError(exchange, requestHeader, ioe, null);
                            }
                        };
                        
                        httpClient.send(new HttpRequest(requestHeader), respHdl);
                    } catch (IOException ioe) {
                        sendError(exchange, requestHeader, ioe, null);
                    }
                }
            };
          
            httpClient.getWorkerpool().execute(task);
        }
    }	
}
