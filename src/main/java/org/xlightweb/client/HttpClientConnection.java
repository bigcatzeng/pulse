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

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xlightweb.AbstractHttpConnection;
import org.xlightweb.BodyDataSink;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.IBodyDataHandler;
import org.xlightweb.IHeader;
import org.xlightweb.IHttpConnection;
import org.xlightweb.NonBlockingBodyDataSource;
import org.xlightweb.FutureResponseHandler;
import org.xlightweb.HttpResponse;
import org.xlightweb.IFutureResponse;
import org.xlightweb.IHttpMessageHeader;
import org.xlightweb.IHttpSession;
import org.xlightweb.HttpUtils;
import org.xlightweb.IHttpConnectionHandler;
import org.xlightweb.IHttpMessage;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHeader;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.IHttpResponseHeader;
import org.xlightweb.ProtocolException;

import org.xsocket.DataConverter;
import org.xsocket.Execution;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IConnectExceptionHandler;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.NonBlockingConnection;
import org.xsocket.connection.NonBlockingConnectionPool;





/**
 * Represents the client side endpoint implementation of a http connection. The HttpClientConnection supports
 * constructors which accepts the remote address or a existing {@link INonBlockingConnection}.  A INonBlockingConnection
 * can become a HttpClientConnection at any time.
 * 
 * @author grro@xlightweb.org
 */
public final class HttpClientConnection extends AbstractHttpConnection implements IHttpClientEndpoint {

	
	private static final Logger LOG = Logger.getLogger(HttpClientConnection.class.getName());
	
	private static String implementationVersion;
	
	private static final DoNothingMessageHandler DO_NOTHING_HANDLER = new DoNothingMessageHandler();

	static final String TIMEOUT_100_CONTINUE_RESPONSE = "org.xlightweb.HttpClientConnection.100-continueTimeout";
	static final String IS_AUTOCONTINUE_DEACTIVATED = "org.xlightweb.HttpClientConnection.isAutocontinueDeactivated";
	
	
	// life cycle
	private static boolean IS_CLOSE_CONNECTION_ON_5XX_RESPONSE = Boolean.parseBoolean(System.getProperty("org.xlightweb.client.closeConnectionOn5xxResponse", "true"));
	private boolean isAutocloseAfterResponse = false;
	private final AtomicBoolean isDisconnected = new AtomicBoolean(false);
    private final ArrayList<MessageHeaderHandler> handlersWaitingForResponseHeader = new ArrayList<MessageHeaderHandler>();
	
	
	// response timeout support
	private static final long MIN_WATCHDOG_PERIOD_MILLIS = 30 * 1000;
	private long responseTimeoutMillis = IHttpConnection.DEFAULT_RESPONSE_TIMEOUT_MILLIS;
	private WatchDogTask watchDogTask;

	
    // remove Expect: Continue header
    private static final int CONTINUE_TIMEOUT_MILLIS = Integer.parseInt(System.getProperty("org.xlightweb.client.response.continueTimeoutMillis", "3000"));

	
	// transaction monitor support
	private TransactionMonitor transactionMonitor;
	

	
	/**
	 * constructor 
	 * 
	 * @param host            the remote host
	 * @param port            the remote port 
	 * @throws ConnectException if an error occurred while attempting to connect to a remote address and port. 
	 * @throws IOException    if an exception occurs
	 */
	public HttpClientConnection(String host, int port) throws IOException, ConnectException {
		this(newNonBlockingConnection(new InetSocketAddress(host, port), null), null);
	}

    /**
     * constructor 
     * 
     * @param host            the remote host
     * @param port            the remote port
     * @param workerpool      the workerpool to use 
     * @throws ConnectException if an error occurred while attempting to connect to a remote address and port. 
     * @throws IOException    if an exception occurs
     */
    public HttpClientConnection(String host, int port, Executor workerpool) throws IOException, ConnectException {
        this(newNonBlockingConnection(new InetSocketAddress(host, port), workerpool), null);
    }

    	
	
	/**
	 * constructor 
	 * 
	 * @param address the server address          
	 * @throws ConnectException if an error occurred while attempting to connect to a remote address and port.
	 * @throws IOException    if an exception occurs
	 */
	public HttpClientConnection(InetSocketAddress address) throws IOException, ConnectException {
		this(newNonBlockingConnection(address, null), null);
	}
	
	/**
     * constructor 
     * 
     * @param address    the server address
     * @param workerpool the workerpool to use         
     * @throws ConnectException if an error occurred while attempting to connect to a remote address and port.
     * @throws IOException    if an exception occurs
     */
    public HttpClientConnection(InetSocketAddress address, Executor workerpool) throws IOException, ConnectException {
        this(newNonBlockingConnection(address, workerpool), null);
    }
	



	private static INonBlockingConnection newNonBlockingConnection(InetSocketAddress address, Executor workerpool) throws ConnectException {
		try {
		    if (workerpool == null) {
		        return new NonBlockingConnection(address);
		    } else {
		        return new NonBlockingConnection(address.getHostName(), address.getPort(), null, workerpool);
		    }
		} catch (IOException ioe) {
			throw new ConnectException(ioe.toString());
		}
	}
	
	protected static boolean isComplete(NonBlockingBodyDataSource body) {
        return AbstractHttpConnection.isComplete(body);
    }
	
    protected static boolean isNetworkendpoint(NonBlockingBodyDataSource dataSource) {
        return AbstractHttpConnection.isNetworkendpoint(dataSource);
    }
    
    
    protected static void addConnectionAttribute(IHttpResponseHeader header, IHttpConnection con) {
        AbstractHttpConnection.addConnectionAttribute(header, con);
    }    
    
    protected static boolean isSupports100Contine(IHttpResponseHandler handler) {
        return AbstractHttpConnection.isSupports100Contine(handler);
    }
	
    protected static void forward(NonBlockingBodyDataSource dataSource, BodyDataSink dataSink) throws IOException {
        AbstractHttpConnection.forward(dataSource, dataSink);
    }

	
    protected static int getDataReceived(NonBlockingBodyDataSource dataSource) {
        return AbstractHttpConnection.getDataReceived(dataSource);
    }

    protected static BodyDataSink newInMemoryBodyDataSink(String id, IHttpMessageHeader header) throws IOException {
        return AbstractHttpConnection.newInMemoryBodyDataSink(id, header);
    }

	protected static IHeader getReceivedHeader(ProtocolException pe) {
	    return AbstractHttpConnection.getReceviedHeader(pe);
	}
	
    protected static NonBlockingBodyDataSource getDataSourceOfInMemoryBodyDataSink(BodyDataSink dataSink) {
        return AbstractHttpConnection.getDataSourceOfInMemoryBodyDataSink(dataSink);
    }

    protected static void setDataHandlerSilence(NonBlockingBodyDataSource dataSource, IBodyDataHandler dataHandler) {
        AbstractHttpConnection.setDataHandlerSilence(dataSource, dataHandler);
    }
	
	protected static IBodyDataHandler getDataHandlerSilence(NonBlockingBodyDataSource dataSource) {
        return AbstractHttpConnection.getDataHandlerSilence(dataSource);
    }
	
    protected static int availableSilence(NonBlockingBodyDataSource dataSource) throws IOException {
        return AbstractHttpConnection.availableSilence(dataSource);
    }
    
    protected static ByteBuffer[] readByteBufferByLengthSilence(NonBlockingBodyDataSource dataSource, int length) throws IOException {
        return AbstractHttpConnection.readByteBufferByLengthSilence(dataSource, length);
    }


	/**
	 * constructor 
	 * 
	 * @param connection    the underlying tcp connection
	 * @throws IOException    if an exception occurs
	 */
	public HttpClientConnection(INonBlockingConnection connection) throws IOException {
		this(connection, null);
		init();
	}
	

	
	
	/**
	 * constructor 
	 * 
	 * @param host               the remote host
	 * @param port               the remote port
	 * @param connectionHandler  the connection handler
	 * @throws ConnectException if an error occurred while attempting to connect to a remote address and port. 
	 * @throws IOException    if an exception occurs
	 */
	public HttpClientConnection(String host, int port, IHttpConnectionHandler connectionHandler) throws IOException, ConnectException {
		this(newNonBlockingConnection(new InetSocketAddress(host, port), null), connectionHandler);
	}

    
    /**
     * constructor 
     * 
     * @param host               the remote host
     * @param port               the remote port
     * @param workerpool         the workerpool to use 
     * @param connectionHandler  the connection handler
     * @throws ConnectException if an error occurred while attempting to connect to a remote address and port. 
     * @throws IOException    if an exception occurs
     */
    public HttpClientConnection(String host, int port, Executor workerpool, IHttpConnectionHandler connectionHandler) throws IOException, ConnectException {
        this(newNonBlockingConnection(new InetSocketAddress(host, port), workerpool), connectionHandler);
    }	
	

	/**
	 * constructor 
	 * 
	 * @param connection    the underlying tcp connection
	 * @param handler       the handler
	 * @throws IOException    if an exception occurs
	 */
	private HttpClientConnection(INonBlockingConnection connection, IHttpConnectionHandler connectionHandler) throws IOException {
		super(connection, true);

		if (connectionHandler != null) {
			addConnectionHandler(connectionHandler);
		}
				
		init();
	}
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
	    if (getNumOpenTransactions() != 0) {
	        if (LOG.isLoggable(Level.FINE)) {
	            LOG.fine("[" + getId() + "] " + getNumOpenTransactions() + " open transaction(s). setting persistent to false");
	        }
	        setPersistent(false);

	        if (!handlersWaitingForResponseHeader.isEmpty()) {
	            if (LOG.isLoggable(Level.FINE)) {
	                for (MessageHeaderHandler headerHandler : handlersWaitingForResponseHeader) {
	                    LOG.fine("[" + getId() + "] removing waiting measage header handler " + headerHandler);
	                }
	            }
	            
	            handlersWaitingForResponseHeader.clear();
	        }
	    }
	    
	    super.close();
	}
	

	/**
	 * sets a transaction monitor
	 * 
	 * @param transactionMonitor the transaction monitor
	 */
	void setTransactionMonitor(TransactionMonitor transactionMonitor) {
		this.transactionMonitor = transactionMonitor;
	}

	
	
	/**
     * {@inheritDoc}
     */
	@Override
	protected void onIdleTimeout() {

	    // notify waiting handler
        for (MessageHeaderHandler messageHandler : getHandlersWaitingForResponseCopy()) {
            messageHandler.onException(new SocketTimeoutException("idle timeout " + DataConverter.toFormatedDuration(getIdleTimeoutMillis()) + " reached"));
        }
        handlersWaitingForResponseHeader.clear();

	    super.onIdleTimeout();
	}
	
	
	
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onConnectionTimeout() {

		// notify waiting handler
		for (MessageHeaderHandler messageHandler : getHandlersWaitingForResponseCopy()) {
			messageHandler.onException(new SocketTimeoutException("connection timeout " + DataConverter.toFormatedDuration(getConnectionTimeoutMillis()) + " reached"));
		}
		handlersWaitingForResponseHeader.clear();

		super.onConnectionTimeout();
	}
	
	@SuppressWarnings("unchecked")
	private List<MessageHeaderHandler> getHandlersWaitingForResponseCopy() {
		synchronized (handlersWaitingForResponseHeader) {
			return (List<MessageHeaderHandler>) handlersWaitingForResponseHeader.clone();
		}
	}
	
	
	
	@Override
	protected void onDisconnect() {
	    
		if (!isDisconnected.getAndSet(true)) {
		    setPersistent(false);
			
			// notify waiting handler 
			for (MessageHeaderHandler messageHandler : getHandlersWaitingForResponseCopy()) {
				ClosedChannelException cce = newDetailedClosedChannelException("channel " + getId() + " is closed (by peer?) while receiving response data " +
						                                                                "(countMessagesSent=" + getCountMessagesSent() + ", countMessagesReceived=" + getCountMessagesReceived() +
						                                                                ", countReceivedBytes=" + getCountReceivedBytes() + ")");
				messageHandler.onException(cce);
			}
			handlersWaitingForResponseHeader.clear();
			 
			transactionMonitor = null;
			
			if (watchDogTask != null) {
				watchDogTask.close();
			}
			watchDogTask = null;
			
			super.onDisconnect();
		}
	}
	
	
	
	/**
	 * generates a error page
	 * 
	 * 
	 * @param errorCode  the error code
	 * @param msg        the message
	 * @param id         the connection id
	 * @return the error page
	 */
	protected static String generateErrorMessageHtml(int errorCode, String msg, String id) {
		return AbstractHttpConnection.generateErrorMessageHtml(errorCode, msg, id);
	}

	
	
	/**
	 * schedules a timer task 
	 * 
	 * @param task     the timer task 
	 * @param delay    the delay 
	 * @param period   the period 
	 */
	protected static void schedule(TimerTask task, long delay, long period) {
		AbstractHttpConnection.schedule(task, delay, period);
	}


	/**
	 * {@inheritDoc}
	 */
	public IHttpResponse call(IHttpRequest request) throws IOException, ConnectException, SocketTimeoutException {
		
	    IFutureResponse futureResponse = send(request);
        
        try {
            return futureResponse.getResponse();
        } catch (InterruptedException ie) {
            throw new IOException(ie.toString());
        }
    }
	

 
	
	/**
	 * set if the connection should be closed after sending a response 
	 * 
	 * @param isAutocloseAfterResponse true, if the connection should be closed after sending a response
	 */
	void setAutocloseAfterResponse(boolean isAutocloseAfterResponse) {
		this.isAutocloseAfterResponse = isAutocloseAfterResponse;
	}
	
	

	

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onMessageCompleteReceived(IHttpMessageHeader header) {

	    // ignore 1xx responses
	    int status = ((IHttpResponseHeader) header).getStatus();
	    if ((status >= 100) && (status < 200)) {
	        setPersistent(false);
	        return;
	    } 
	        
	    super.setNetworkBodyDataSinkIgnoreWriteError();
        super.onMessageCompleteReceived(header);
        
        if (!isPersistent()) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] connection is not persistent. destroying it");
            }
            destroy();  // only closing will not be engough (if the connection is a pooled one)
            
        } else if (isAutocloseAfterResponse) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] closing connection");
            }
            closeQuitly();
        }
	}
	

	/**
     * {@inheritDoc}
     */
	public boolean isServerSide() {
	    return false;
	}
	
	
	
	/**
	 * set the response body default encoding. According to RFC 2616 the 
	 * initial value is ISO-8859-1 
	 *   
	 * @param encoding  the defaultEncoding 
	 */
	public void setResponseBodyDefaultEncoding(String defaultEncoding) {
	    setBodyDefaultEncoding(defaultEncoding);
	}
	
	

	/**
	 * {@inheritDoc}
	 */
	public void setResponseTimeoutMillis(long responseTimeoutMillis) {
		
		
		if (this.responseTimeoutMillis != responseTimeoutMillis) {
			this.responseTimeoutMillis = responseTimeoutMillis;

			if (responseTimeoutMillis == Long.MAX_VALUE) {
				terminateWatchDogTask();
				
			} else {
				
				long watchdogPeriod = 100;
				if (responseTimeoutMillis > 1000) {
					watchdogPeriod = responseTimeoutMillis / 10;
				}
				
				if (watchdogPeriod > MIN_WATCHDOG_PERIOD_MILLIS) {
					watchdogPeriod = MIN_WATCHDOG_PERIOD_MILLIS;
				}
			
				if (LOG.isLoggable(Level.FINE)) {
					LOG.fine("[" + getId() +"] response timeout to " + DataConverter.toFormatedDuration(responseTimeoutMillis) + ". Updating wachdog tas to check period " + watchdogPeriod + " millis");
				}
				
				updateWatchDog(watchdogPeriod);
			}
		}
	}
	
	
	private synchronized void updateWatchDog(long watchDogPeriod) {
		terminateWatchDogTask();

        watchDogTask = new WatchDogTask(this); 
        schedule(watchDogTask, watchDogPeriod, watchDogPeriod);
	}

	
	
	private synchronized void terminateWatchDogTask() {
		if (watchDogTask != null) {
            watchDogTask.close();
        }
	}

	
	/**
	 * {@inheritDoc}
	 */
	public long getResponseTimeoutMillis() {
		return responseTimeoutMillis;
	}
	



	private void checkTimeouts() {
		try {
			
			long currentMillis = System.currentTimeMillis();
			
			for (Object hdl : handlersWaitingForResponseHeader.toArray()) {
				boolean isTimeoutReached = ((MessageHeaderHandler) hdl).isResponseTimeoutReached(currentMillis);
				if (isTimeoutReached) {
					
					if (handlersWaitingForResponseHeader.remove(hdl)) {
						onResponseTimeout((MessageHeaderHandler) hdl);
					}
					
					destroy();
				}
			}
			
		} catch (Exception e) {
            // eat and log exception
			LOG.warning("exception occured by checking timouts. " + DataConverter.toString(e));
		}
	}
	

	private void onResponseTimeout(MessageHeaderHandler internalResponseHandler) {
		
		final SocketTimeoutException ste = new SocketTimeoutException("response timeout " + DataConverter.toFormatedDuration(internalResponseHandler.getTimeout()) + " reached");
		
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine(ste.getMessage());
		}
		
		internalResponseHandler.onException(ste);
        destroy();
    }
	
	
	private boolean isValid() {
	    return (isOpen() && isPersistent()); // isPersistent check is required by repeated calls to prevent illegal use
	}

	
	
	/**
	 * {@inheritDoc}
	 */
	public BodyDataSink send(IHttpRequestHeader requestHeader, IHttpResponseHandler responseHandler) throws IOException, ConnectException {

        if (requestHeader.getContentLength() != -1) {
            return send(requestHeader, requestHeader.getContentLength(), responseHandler);
        }

	    
		if (isValid()) {
		    
			if (responseHandler == null) {
			    if (LOG.isLoggable(Level.FINE)) {
			        LOG.fine("waring no response handler is set. ignoring response");
			    }

				responseHandler = newDoNothingResponseHandler();
			}
			
							 
			if ((requestHeader.getTransferEncoding() == null)) {
				requestHeader.setHeader("Transfer-Encoding", "chunked");
			}

			enhanceHeader(requestHeader);

			
			try{
				return sendInternal(requestHeader, responseHandler);
				
			} catch (IOException ioe) {
				String msg = "can not send request \r\n " + requestHeader.toString() +
				 "\r\n\r\nhttpConnection:\r\n" + this.toString() +
	            "\r\n\r\nreason:\r\n" + ioe.toString();

				destroy();
				throw new IOException(msg);
			}
			
		} else {
			throw new ClosedChannelException();
		}		
	}
	
	
	
	private BodyDataSink sendInternal(IHttpRequestHeader requestHeader, IHttpResponseHandler responseHandler) throws IOException, ConnectException {
	    
	    final ResponseHandlerAdapter adapter = new ResponseHandlerAdapter(responseHandler, requestHeader);
        addMessageHeaderHandler(new MessageHeaderHandler(adapter, requestHeader, responseTimeoutMillis));
        
        if (!adapter.isContinueHandler() && HttpUtils.isContainExpect100ContinueHeader(requestHeader)) {
            LOG.warning("Request contains 'Excect: 100-coninue' header and response handler is not annotated with Supports100Continue. Removing Expect header");
            requestHeader.removeHeader("Expect");
        }
            
        BodyDataSink dataSink = writeMessage(Integer.MAX_VALUE, requestHeader);
        
        return dataSink;
	}


	/**
	 * {@inheritDoc}
	 */
	public BodyDataSink send(IHttpRequestHeader requestHeader, int contentLength, IHttpResponseHandler responseHandler) throws IOException, ConnectException {

		if (isValid()) {

			if (responseHandler == null) {
			    if (LOG.isLoggable(Level.FINE)) {
			        LOG.fine("waring no response handler is set. ignoring response");
			    }
				responseHandler = newDoNothingResponseHandler();
			}

			
			if (requestHeader.getContentLength() != -1) {
				requestHeader.removeHeader("Content-Length");
			}
							 
			if ((requestHeader.getTransferEncoding() == null)) {
				requestHeader.setHeader("Transfer-Encoding", "chunked");
			}


			
			enhanceHeader(requestHeader);


			try{
				 if ((requestHeader.getTransferEncoding() != null) && (requestHeader.getTransferEncoding().equalsIgnoreCase("chunked"))) {
					 requestHeader.removeHeader("Transfer-Encoding");
				 }
						
				 if (requestHeader.getContentLength() == -1) {
					 requestHeader.setContentLength(contentLength);
				 }

				 return sendInternal(requestHeader, contentLength, responseHandler);
				
			} catch (IOException ioe) {
				String msg = "can not send request \r\n " + requestHeader.toString() +
				 "\r\n\r\nhttpConnection:\r\n" + this.toString() +
	            "\r\n\r\nreason:\r\n" + ioe.toString();
	
				destroy();
				throw new IOException(msg);
			}
			
		} else {
			throw new ClosedChannelException();
		}		

	}
	
	
	private BodyDataSink sendInternal(IHttpRequestHeader requestHeader, int contentLength, IHttpResponseHandler responseHandler) throws IOException, ConnectException {
        final ResponseHandlerAdapter adapter = new ResponseHandlerAdapter(responseHandler, requestHeader);
        addMessageHeaderHandler(new MessageHeaderHandler(adapter, requestHeader, responseTimeoutMillis));
    
        if (!adapter.isContinueHandler() && HttpUtils.isContainExpect100ContinueHeader(requestHeader)) {
            LOG.warning("Request contains 'Excect: 100-coninue' header and response handler is not annotated with Supports100Continue. Removing Expect header");
            requestHeader.removeHeader("Expect");
        }

        BodyDataSink dataSink = writeMessage(requestHeader, contentLength);
         
        return dataSink;
	}


    /**
     * {@inheritDoc}
     */
    public IFutureResponse send(IHttpRequest request) throws IOException, ConnectException {
        
        FutureResponseHandler responseHandler;
        
        // continue expected?
        if (request.hasBody() && HttpUtils.isContainsExpect100ContinueHeader(request)) {
            
            // do not auto continue for network requests
            if (isNetworkendpoint(request.getNonBlockingBody())) {
                request.setAttribute(IS_AUTOCONTINUE_DEACTIVATED, true);
            }
            
            if ((request.getAttribute(IS_AUTOCONTINUE_DEACTIVATED) == null) || ((Boolean) request.getAttribute(IS_AUTOCONTINUE_DEACTIVATED) != true)) {
                responseHandler = new FutureContinueResponseHandler(request.getRequestHeader(), request.getNonBlockingBody(), getId());
                
                BodyDataSink dataSink = send(request.getRequestHeader(), responseHandler);
                dataSink.setFlushmode(FlushMode.ASYNC);
                
                ((FutureContinueResponseHandler) responseHandler).setBodyDataSink(dataSink);
                dataSink.flush(); //writes the header
                return responseHandler;
            }
        } 

        // ... no
        responseHandler = new FutureResponseHandler();
        send(request, responseHandler);
        
        return responseHandler;
    }	
	
    
    
	/**
	 * {@inheritDoc}
	 */
	public void send(IHttpRequest request, IHttpResponseHandler responseHandler) throws IOException, ConnectException {

		if (isValid()) {
			if (responseHandler == null) {
			    if (LOG.isLoggable(Level.FINE)) {
			        LOG.fine("waring no response handler is set. ignoring response");
			    }
				responseHandler = newDoNothingResponseHandler();
			}
			
			IHttpRequestHeader requestHeader = request.getRequestHeader();
			enhanceHeader(requestHeader);
			    
			try {
				sendInternal(request, responseHandler);									
				
			} catch (IOException ioe) {
				String msg = "can not send request \r\n " + request.toString() +
							 "\r\n\r\nhttpConnection:\r\n" + this.toString() +
				             "\r\n\r\nreason:\r\n" + ioe.toString();
				
				destroy();
				throw new IOException(msg);
			}
				
		} else {
			throw new ClosedChannelException();
		}
	}
	
	
	
	private void sendInternal(IHttpRequest request, IHttpResponseHandler responseHandler) throws IOException, ConnectException {

        ResponseHandlerAdapter adapter = new ResponseHandlerAdapter(responseHandler, request.getRequestHeader());
        addMessageHeaderHandler(new MessageHeaderHandler(adapter, request.getRequestHeader(), responseTimeoutMillis));
        
        
        if (!adapter.isContinueHandler() && HttpUtils.isContainExpect100ContinueHeader(request.getRequestHeader())) {
            // touch body to initiate sending 100-continue if required
            request.getNonBlockingBody().getReadBufferVersion();

            LOG.warning("Request contains 'Excect: 100-continue' header and response handler is not annotated with Supports100Continue. Removing Expect header");
            request.getRequestHeader().removeHeader("Expect");
        }
        
        // does the message contain a body?
        if (request.hasBody()) {    
        	if (request.getNonBlockingBody().getDataHandler() != null) {
                throw new IOException("a body handler is already assigned to the message body. sending such messages is not supported (remove data handler)"); 
            }    
                
            if (isForwardable(request.getNonBlockingBody())) {
                BodyDataSink dataSink = send(request.getRequestHeader(), responseHandler);
                forwardBody(request.getNonBlockingBody(), dataSink);
                
            } else {
                writeMessage(Integer.MAX_VALUE, request);
            }
            
            
        // no, its bodyless
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] sending (bodyless): " + request.getRequestHeader());
            }

            BodyDataSink bodyDataSink = writeMessage(request.getRequestHeader(), 0);
            bodyDataSink.setFlushmode(FlushMode.ASYNC);
            bodyDataSink.close();
        }       
	}
	
	
	
    private void addMessageHeaderHandler(MessageHeaderHandler handler) {
        synchronized (handlersWaitingForResponseHeader) {
            handlersWaitingForResponseHeader.add(handler);
        }
    }
    
    private void addMessageHeaderHandlerFirst(MessageHeaderHandler handler) {
        synchronized (handlersWaitingForResponseHeader) {
            ArrayList<MessageHeaderHandler> hdls = new ArrayList<MessageHeaderHandler>(); 
            handlersWaitingForResponseHeader.removeAll(hdls);
            handlersWaitingForResponseHeader.add(handler);
            handlersWaitingForResponseHeader.addAll(hdls);
        }
    }
	

	private void enhanceHeader(IHttpRequestHeader header) throws IOException {
		String host = header.getHost();
		if (host == null) {
			header.setHost(getRemoteHostInfo());
		}
				
		String userAgent = header.getUserAgent();
		if (userAgent == null) {
			header.setUserAgent(getImplementationVersion());
		}		
	}
	

	 
	private static String getImplementationVersion() {
		if (implementationVersion == null) {
			implementationVersion = "xLightweb/" + HttpUtils.getImplementationVersion();
		}
		
		return implementationVersion;
	}

	
	private String getRemoteHostInfo() throws IOException {
		InetAddress remoteAddress = getRemoteAddress();
		
		if (remoteAddress == null) {
			return "";
		}
		
		int remotePort = getRemotePort();
		
		return remoteAddress.getHostName() + ":" + remotePort;
	}
	

	

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected IMessageHeaderHandler getMessageHeaderHandler() {
		synchronized (handlersWaitingForResponseHeader) {
			if (handlersWaitingForResponseHeader.isEmpty()) {
				return null;
			} else {
			    IMessageHeaderHandler messageHandler = handlersWaitingForResponseHeader.remove(0);
			    
		        if (LOG.isLoggable(Level.FINE)) {
		            LOG.fine("retrieve (and removing) message handler " + messageHandler);
		        }
			    
			    return messageHandler;
			}
		}
	}

	
	/**
     * {@inheritDoc}
     */
    protected void removeMessageHandler(IMessageHeaderHandler messageHandler) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("removing message handler " + messageHandler);
        }
        
        synchronized (handlersWaitingForResponseHeader) {
            handlersWaitingForResponseHeader.remove(messageHandler);
        }
    }

    

    static int toInt(long l) {
        if (l > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return (int) l; 
        }
    }
    
 
    
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		try {
		    sb.append(getId());
		    try {
		        sb.append(" " + getUnderlyingTcpConnection().getLocalAddress() + ":" + getUnderlyingTcpConnection().getLocalPort() + " -> " + getUnderlyingTcpConnection().getRemoteAddress() + ":" + getUnderlyingTcpConnection().getRemotePort());
		    } catch (Exception ignore) { }
		    
    		if (getNumOpenTransactions() > 0) {
    		    sb.append(" openTransactions=" + getNumOpenTransactions());
    		}
    		
    		if (!getUnderlyingTcpConnection().isOpen()) {
    			sb.append("  (closed)");
    		}
		
		} catch (Exception ignore) { }
		
		return sb.toString();
	}
	
	
	
	/**
	 * message handler 
	 * 
	 * @author grro
	 */
	final class MessageHeaderHandler implements IMessageHeaderHandler {

		private final AtomicBoolean isCommitted = new AtomicBoolean(false);

		private final ResponseHandlerAdapter responseHandlerAdapter;
		private final IHttpRequestHeader requestHeader;
			
		private long timeout = Long.MAX_VALUE;
		private long timeoutDate = Long.MAX_VALUE;
		
	    private IHttpResponse response = null;
		
	    
	    private final boolean is100ContinueExpected;
        private final TimerTask missing100ResponseHandler;
        private boolean is100ResponseNotified;
        

		/**
		 * constructor 
		 * 
		 * @param delegate         the response handler 
		 * @param requestHeader   the request header
		 * @param responseTimeout hte response timeout
		 */
		public MessageHeaderHandler(ResponseHandlerAdapter responseHandlerAdapter, IHttpRequestHeader requestHeader, long responseTimeout) {
		    this.responseHandlerAdapter = responseHandlerAdapter;
			this.requestHeader = requestHeader;
			
			this.timeout = responseTimeout;
			if ((responseTimeout != Long.MAX_VALUE) && (responseTimeout < Long.MAX_VALUE * 0.9)) {
				timeoutDate = timeout + System.currentTimeMillis();
			}
			
			
		    // 100-continue response expected?
            if (HttpUtils.isContainExpect100ContinueHeader(requestHeader)) {
                is100ContinueExpected = true;
                is100ResponseNotified = false;
                missing100ResponseHandler = new TimerTask() {
                    
                    @Override
                    public void run() {
                        cancel(); 
                        
                        if (!is100ResponseNotified) {
                            HttpResponseHeader header = new HttpResponseHeader(100);
                            header.setReason("Continue (100-continue response timeout)");
                            header.setAttribute(TIMEOUT_100_CONTINUE_RESPONSE, true);
                            
                            LOG.warning("100-continue timeout reached (" + DataConverter.toFormatedDuration(CONTINUE_TIMEOUT_MILLIS) + "). Generating local 100-continue response");
                            callResponseHandler(new HttpResponse(header));
                        }
                    }
                };
                schedule(missing100ResponseHandler, CONTINUE_TIMEOUT_MILLIS);
                
            // ... no    
            } else {
                is100ContinueExpected = false;
                is100ResponseNotified = true;
                missing100ResponseHandler = null;
            }			
		}
		
	

		/**
		 * returns if the response timeout is reached
		 *  
		 * @param currentMillis the current time
		 * @return true, if the respnose timeout is reached
		 */
		boolean isResponseTimeoutReached(long currentMillis) {
			if (isCommitted.get()) {
				return false;
			}
			
			return (currentMillis > timeoutDate);
		}
		
		
		/**
		 * returns the timeout
		 * 
		 * @return the timeout
		 */
		long getTimeout() {
			return timeout;
		}


		/**
         * {@inheritDoc}
         */
		public IHttpMessageHeader getAssociatedHeader() {
		    return requestHeader;
		}
		
		
		
		
		/**
		 * {@inheritDoc} 
		 */
		public IMessageHandler onMessageHeaderReceived(IHttpMessage message) throws IOException {
		    
		    response = (IHttpResponse) message;

			if (transactionMonitor != null) {
			    transactionMonitor.register(HttpClientConnection.this, requestHeader, response);
			}
	            
			if (LOG.isLoggable(Level.FINE)) {
			    
			    if (response.hasBody()) {
                    String body = "";
                        
                    String contentType = response.getContentType();
                    if ((contentType != null) && (contentType.startsWith("application/x-www-form-urlencode"))) {
                        body = response.getNonBlockingBody().toString() + "\n";
                    } 
                        
                    LOG.fine("[" + getId() + "] response received from " + getRemoteAddress() + 
                             ":" + getRemotePort() + 
                             " (" + getCountMessagesReceived() + ". request) " + response.getMessageHeader().toString() + body);
	                
			    } else {
	                 LOG.fine("[" + getId() + "] bodyless response received from " + getRemoteAddress() +
	                            ":" + getRemotePort() + 
	                            " (" + getCountMessagesReceived() + ". request) " + response.getMessageHeader().toString());
			    }
			}
			
			
			boolean isInvokeOnMessageReceived = responseHandlerAdapter.isInvokeOnMessageReceived();
	                    

			// if 1xx response?
	        if ((response.getStatus() >= 100) && (response.getStatus() < 200)) {
               setPersistent(true);
	        } 

            // is 100-continue response?
            if (response.getStatus() == 100) {               
                // re-add the message handler for "true" response
                addMessageHeaderHandlerFirst(this);
                
 
                // is Expect: 100-continue sent?
                if (HttpUtils.isContainExpect100ContinueHeader(requestHeader)) {
                    isInvokeOnMessageReceived = true;  // wait for body if exist
                    
                // ... no -> swallow 100 response
                } else {
                    return DO_NOTHING_HANDLER; 
                }
                
                
            // .. no    
            } else {
                
                // if response code 5xx -> set connection not reusable
                if (IS_CLOSE_CONNECTION_ON_5XX_RESPONSE) {
                    if ((response.getStatus() >= 500) && (response.getStatus() < 600)) {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("got return code 5xx. Set connection " + getId() + " to non persistent");
                        }
                        setPersistent(false);
                    }
                }
                
                incCountMessageReceived();

                handleLifeCycleHeaders(response.getResponseHeader());
            } 
            

			
			
			// has body?
			if (response.hasBody()) {
			    
			    // should handler invoke onMessageReceived
			    if (isInvokeOnMessageReceived) {
			        return new InvokeOnMessageReceivedMessageHandler(this);
			        
			    } else {
                    return new IMessageHandler() {
                        public void onHeaderProcessed() throws IOException {
                            callResponseHandler();
                        }
                        
                        public void onBodyException(IOException ioe, ByteBuffer[] rawData) { 
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.fine("[" + getId() + "] error occured by receiving request body " + ioe.toString());
                            }
                            destroy();
                        }
                        
                        public void onMessageReceived() throws IOException { }
                    };			        
			    }
			    
			    
			// ... no call onMessageBodyReceived    
			} else {
			    onMessageCompleteReceived(response.getResponseHeader());
			    			    
			    return new IMessageHandler() {
			    	public void onHeaderProcessed() throws IOException {
			    		callResponseHandler();
			    	}
			    	
			    	public void onBodyException(IOException ioe, ByteBuffer[] rawData) { 
			    		if (LOG.isLoggable(Level.FINE)) {
			    			LOG.fine("[" + getId() + "] error occured by receiving request body " + ioe.toString());
			    		}
			    		destroy();
			    	}
			    	
			    	public void onMessageReceived() throws IOException { }
			    };
			}
		}
		
		  
        /**
         * {@inheritDoc}
         */
		public void onHeaderException(IOException ioe, ByteBuffer[] rawData) {
		    onException(ioe);
		}
		
		
		void onException(IOException ioe) {
            responseHandlerAdapter.onException(ioe, HttpClientConnection.this);
        }

        /**
         * {@inheritDoc}
         */             
		public void onMessageReceived() throws IOException {
		    
		}
		
		
        /**
         * {@inheritDoc}
         */
		public void onBodyException(IOException ioe, ByteBuffer[] rawData) {
		    onException(ioe);
		}



        private void callResponseHandler() {
            if (is100ContinueExpected) {
                callSafeResponseHandler(response);
            } else {
                callUnSafeResponseHandler(response);
            }
        }

        
        private void callUnSafeResponseHandler(IHttpResponse response) {
            callResponseHandler(response);
        }
        
        
        private synchronized void callSafeResponseHandler(IHttpResponse response) {
            if (missing100ResponseHandler != null) {
                missing100ResponseHandler.cancel();
            }

            callResponseHandler(response);
        }
        
        
        private void callResponseHandler(IHttpResponse response) {
            
            if (response.getStatus() == 100) {
                if (!is100ResponseNotified) {
                    is100ResponseNotified = true;
                    responseHandlerAdapter.onResponse(response, HttpClientConnection.this);
                }
                
            } else {
                isCommitted.set(true);
                responseHandlerAdapter.onResponse(response, HttpClientConnection.this);
            }
        }
                
        
        void callResponseHandler(IOException ioe) {
            isCommitted.set(true);
            responseHandlerAdapter.onException(ioe, HttpClientConnection.this);
        }
        
    
		private void handleLifeCycleHeaders(IHttpResponseHeader responseHeader) throws IOException {

			// if HTTP 1.1 -> connection is persistent by default
			if ((responseHeader.getProtocol() != null) && responseHeader.getProtocol().equals("HTTP/1.1")) {
				setPersistent(true && isPersistent());
				
			} else {
				
				// response of connect is persistent, implicitly
				if (requestHeader.getMethod().equals(IHttpMessage.CONNECT_METHOD)) {
					setPersistent(true && isPersistent());
				}
			}				
		
			// handle connection header
			handleConnectionHeaders(responseHeader);			
		}

		
		
		private void handleConnectionHeaders(IHttpResponseHeader responseHeader) throws IOException {	
			
			String keepAliveHeader = responseHeader.getHeader("KeepAlive");
			if (keepAliveHeader != null) {
				String[] tokens = keepAliveHeader.split(",");
				for (String token : tokens) {
					handleKeepAlive(token.trim());
				}
			} 
			
			
			//check if persistent connection
			String connectionHeader = responseHeader.getHeader("Connection");
			
			
			if (connectionHeader != null) {
				String[] values = connectionHeader.split(",");
				
				
				for (String value : values) {
					value = value.trim();
					
					if (value.equalsIgnoreCase("close")) {
						if (LOG.isLoggable(Level.FINER)) {
							LOG.finer("[" + getId() + " http client connection received 'connection: close' header. set isPersistent=false");
						}
						setPersistent(false);
					}
				}
			}
		}

		
		
		private void handleKeepAlive(String option) {
		    if (option.toUpperCase().startsWith("TIMEOUT=")) {
                int timeoutSec = Integer.parseInt(option.substring("TIMEOUT=".length(), option.length()));
                
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("response keep alive defines timout. set timeout " + timeoutSec + " sec");
                }
                setResponseTimeoutMillis(timeoutSec * 1000L);

                
            } else if (option.toUpperCase().startsWith("MAX=")) {
                int maxTransactions = Integer.parseInt(option.substring("MAX=".length(), option.length()));
                if (maxTransactions == 0) {
                    setPersistent(false);
                }
                
            } else {
            	try {
	                Integer timeoutSec = Integer.parseInt(option);
	                
	                if (LOG.isLoggable(Level.FINE)) {
	                    LOG.fine("response keep alive defines timout. set timeout " + timeoutSec + " sec");
	                }
	                setResponseTimeoutMillis(timeoutSec * 1000L);
            	} catch (NumberFormatException nfe) {
            		if (LOG.isLoggable(Level.FINE)) {
            			LOG.fine("unknown kep-alive option '" + option + "'");
            		}
            	}
            }			
		}
	}
	
	
	private static final class InvokeOnMessageReceivedMessageHandler implements IMessageHandler {
	    
	    private final MessageHeaderHandler headerHandler;
	    
	    public InvokeOnMessageReceivedMessageHandler(MessageHeaderHandler headerHandler) {
	        this.headerHandler = headerHandler;
        }
	    
	    public void onHeaderProcessed() throws IOException {
	    	
	    }
	    
	    public void onMessageReceived() throws IOException {
            headerHandler.callResponseHandler();
        }
        
        
        public void onBodyException(IOException ioe, ByteBuffer[] rawData) {
            headerHandler.onHeaderException(ioe, rawData);
        }
	}
	
	
	

	 
	private static final class WatchDogTask extends TimerTask implements Closeable {
			
		 private WeakReference<HttpClientConnection> httpClientConnectionRef;
			
		 public WatchDogTask(HttpClientConnection httpClientConnection) {
			 httpClientConnectionRef = new WeakReference<HttpClientConnection>(httpClientConnection);
		 }
		
			
		 @Override
		 public void run() {
			 WeakReference<HttpClientConnection> ref = httpClientConnectionRef;
			 if (ref != null) {
				 HttpClientConnection httpClientConnection = ref.get();
					
				 if (httpClientConnection == null)  {
					 this.close();						
				 } else {
					 httpClientConnection.checkTimeouts();
				 }
			 }
		 }	
		 
		 public void close() {
			 this.cancel();
			 httpClientConnectionRef = null;
		}
	 }

	 
	 
    static final class ClientExchange extends AbstractExchange {

        private final NonBlockingConnectionPool pool;
        private final IHttpRequest request;         
        private final SessionManager sessionManager;
        private final IHttpResponseHandler rootResponseHandler;
        private final int connectTimeoutMillis;
        private final String defaultEncoding;
        private final long responseTimeoutMillis;
        private final long bodyDataReceiveTimeoutMillis;
        private final boolean isAutoCloseAfterResponse;
        private final boolean isAutoUncompress;
        private final TransactionMonitor transactionMonitor;
        
        
        public ClientExchange(String defaultEncoding, NonBlockingConnectionPool pool, SessionManager sessionManager, IHttpResponseHandler rootResponseHandler, IHttpRequest request, int connectTimeoutMillis, long responseTimeoutMillis, long bodyDataReceiveTimeoutMillis, boolean isAutoCloseAfterResponse, boolean isAutoUncompress, TransactionMonitor transactionMonitor) {
            super(null, pool.getWorkerpool());
            
            this.defaultEncoding = defaultEncoding;
            this.pool = pool;
            this.request = request;
            this.sessionManager = sessionManager;
            this.rootResponseHandler = rootResponseHandler;
            this.connectTimeoutMillis = connectTimeoutMillis;
            this.responseTimeoutMillis = responseTimeoutMillis;
            this.bodyDataReceiveTimeoutMillis = bodyDataReceiveTimeoutMillis;
            this.isAutoCloseAfterResponse = isAutoCloseAfterResponse;
            this.isAutoUncompress = isAutoUncompress;
            this.transactionMonitor = transactionMonitor;
        }

        
        public IHttpSession getSession(boolean create) {
            return sessionManager.getSession(request.getRemoteHost(), request.getRequestURI(), create);
        }

        public String encodeURL(String url) {
            return url;
        }
        
        public IHttpRequest getRequest() {
            return request;
        }

        
        public BodyDataSink forward(IHttpRequestHeader requestHeader) throws IOException, ConnectException, IllegalStateException {
            return forward(requestHeader, rootResponseHandler);
        }

       
        public BodyDataSink forward(IHttpRequestHeader requestHeader, IHttpResponseHandler responseHandler) throws IOException, ConnectException, IllegalStateException {
            return forwardInternal(requestHeader, null, responseHandler);
        }

        
        public BodyDataSink forward(IHttpRequestHeader requestHeader, int contentLength) throws IOException, ConnectException, IllegalStateException {
            return forward(requestHeader, contentLength, rootResponseHandler);
        }

        
        public BodyDataSink forward(IHttpRequestHeader requestHeader, int contentLength, IHttpResponseHandler responseHandler) throws IOException, ConnectException, IllegalStateException {            
            return forwardInternal(requestHeader, contentLength, responseHandler);
        }

        
        private BodyDataSink forwardInternal(IHttpRequestHeader requestHeader, Integer contentLength, IHttpResponseHandler responseHandler) throws IOException, ConnectException, IllegalStateException {            
            URL url = requestHeader.getRequestUrl();
            String host = url.getHost();
            int port = url.getPort();
            boolean isSSL = url.getProtocol().equalsIgnoreCase("HTTPS");
            

            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("try to get a connection from pool (waitForConnect = true)");
            }
            INonBlockingConnection con = pool.getNonBlockingConnection(InetAddress.getByName(host), normalizePort(port, isSSL), true, connectTimeoutMillis, isSSL);

            HttpClientConnection httpCon = newHttpClientConnection(con);
            setHttpConnection(httpCon);
            
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + con.getId() + "] sending request to remote endpoint");
            }
            
            if (contentLength == null) {
                return httpCon.send(requestHeader, responseHandler);
            } else {
                return httpCon.send(requestHeader, contentLength, responseHandler);
            }
        }
      


        public void forward(IHttpRequest request) throws IOException, ConnectException, IllegalStateException {
            forward(request, rootResponseHandler);
        }
        
        
        public void forward(IHttpRequest request, IHttpResponseHandler responseHandler) throws IOException, ConnectException, IllegalStateException {
            URL url = request.getRequestUrl();
            String host = url.getHost();
            int port = url.getPort();
            boolean isSSL = url.getProtocol().equalsIgnoreCase("HTTPS");
            
            
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("try to get a connection from pool (waitForConnect = false)");
            }
            
            ConnectHandler conHdl = new ConnectHandler(request, responseHandler);
            try {
                pool.getNonBlockingConnection(InetAddress.getByName(host), normalizePort(port, isSSL), conHdl, false, connectTimeoutMillis, isSSL);
            } catch (IOException ioe) {
                conHdl.onConnectException(null, ioe);
            }
        }
        
        
        @Execution(Execution.NONTHREADED)
        private final class ConnectHandler implements IConnectHandler, IConnectExceptionHandler {
            
            private final IHttpRequest req;
            private final IHttpResponseHandler respHandler;
            
            public ConnectHandler(IHttpRequest req, IHttpResponseHandler respHandler) {
                this.req = req;
                this.respHandler = respHandler;
            }
            
            public boolean onConnect(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
                HttpClientConnection con = newHttpClientConnection(connection);
                setHttpConnection(con);

                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + con.getId() + "] sending request to " + connection.getRemoteAddress() + ":" + connection.getRemotePort());
                }
                con.send(req, respHandler);
                return true;
            }
            
            public boolean onConnectException(INonBlockingConnection connection, IOException ioe) throws IOException {
                sendError(ioe, respHandler);
                return true;
            }
        }
        

        
        public BodyDataSink send(IHttpResponseHeader header) throws IOException, IllegalStateException {
            
            // return response to caller (request will not be send to remote endpoint)
            BodyDataSink dataSink = newInMemoryBodyDataSink(this.toString(), header);
            
            IHttpResponse response = new HttpResponse(header, getDataSourceOfInMemoryBodyDataSink(dataSink)); 
            send(response);
            
            return dataSink;
        }

        
        public BodyDataSink send(IHttpResponseHeader header, int contentLength) throws IOException, IllegalStateException {
            
            // return response to caller (request will not be send to remote endpoint)
            BodyDataSink dataSink = newInMemoryBodyDataSink(this.toString(), header);
            
            IHttpResponse response = new HttpResponse(header, getDataSourceOfInMemoryBodyDataSink(dataSink)); 
            send(response);
            
            return dataSink;
        }

        
        public void send(IHttpResponse response) throws IOException, IllegalStateException {

            // return response to caller (request will not be send to remote endpoint)
            ensureResponseHasNotBeenCommitted();

            callResponseHandler(rootResponseHandler, response);
            if (response.getStatus() > 100) {
                setResponseCommited(true);
            }
        }

        
        public void sendError(Exception e) throws IllegalStateException {
            sendError(e, rootResponseHandler);
        }

        
        private void sendError(Exception e, IHttpResponseHandler respHdl) throws IllegalStateException {
            // return response to caller (request will not be send to remote endpoint)
            if (isResponseCommitted()) {
                return;
            }

            IOException ioe = HttpUtils.toIOException(e);
            
            callResponseHandler(respHdl, ioe);
            destroy();
        }

        
        private HttpClientConnection newHttpClientConnection(INonBlockingConnection con) throws IOException {
            HttpClientConnection httpConnection = new HttpClientConnection(con);
            
            httpConnection.setResponseTimeoutMillis(responseTimeoutMillis);
            httpConnection.setBodyDataReceiveTimeoutMillis(bodyDataReceiveTimeoutMillis);
            httpConnection.setAutocloseAfterResponse(isAutoCloseAfterResponse);
            httpConnection.setAutoUncompress(isAutoUncompress);
            httpConnection.setResponseBodyDefaultEncoding(defaultEncoding);

            
            if (transactionMonitor != null) {
                httpConnection.setTransactionMonitor(transactionMonitor);
            }

            return httpConnection;
        }        
        
        
        private int normalizePort(int port, boolean isSecured) {
            
            if (port == -1) {
                if (isSecured) {
                    return 443;
                } else {
                    return 80;
                } 
                
            } else {
                return port;
            }
        }
     }
	 
	
	 private static final class DoNothingMessageHandler implements IMessageHandler {
		 
		 public void onHeaderProcessed() throws IOException { }
	  
	     public void onMessageReceived() throws IOException {  }
         
         public void onBodyException(IOException ioe, ByteBuffer[] rawData) {  }
     }
}
