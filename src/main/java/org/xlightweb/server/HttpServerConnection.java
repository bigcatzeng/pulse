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
package org.xlightweb.server;

import java.io.Closeable;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xlightweb.AbstractHttpConnection;
import org.xlightweb.BadMessageException;
import org.xlightweb.BodyDataSink;
import org.xlightweb.BodyForwarder;
import org.xlightweb.HttpResponse;
import org.xlightweb.HttpResponseHeader;
import org.xlightweb.IBodyCompleteListener;
import org.xlightweb.IBodyDestroyListener;
import org.xlightweb.IHttpMessageHeader;
import org.xlightweb.IHttpSession;
import org.xlightweb.HttpUtils;
import org.xlightweb.IHttpConnectionHandler;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpMessage;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpRequestHeader;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.IHttpResponseHeader;
import org.xlightweb.NonBlockingBodyDataSource;
import org.xsocket.DataConverter;
import org.xsocket.Execution;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.MaxConnectionsExceededException;




/**
 * Represents the server side endpoint implementation of a http connection. Typically, a
 * HttpServerConnection will be created by the {@link HttpProtocolAdapter} which is used
 * internally by the {@link HttpServer}.
 *
 * @author grro@xlightweb.org
 */
public final class HttpServerConnection extends AbstractHttpConnection {

    private static final Logger LOG = Logger.getLogger(HttpServerConnection.class.getName());
	
	private static final int CLOSE_DELAY_ON_OPEN_TRANSACTIONS_MILLIS = 1000;
    
	public static final boolean DEFAULT_REMOVE_REQUEST_CONNECTION_HEADER = false;
	public static final Integer DEFAULT_RECEIVE_TIMEOUT_MILLIS = Integer.MAX_VALUE; 
	public static final boolean DEFAULT_AUTOHANDLE_CONNECTION_UPGRADE_HEADER = true;
	public static final boolean DEFAULT_SESSION_MANAGEMENT = true;

	static final int MIN_REQUEST_TIMEOUT_MILLIS = 1000; 

	
	private static final DoNothingMessageHandler DO_NOTHING_HANDLER = new DoNothingMessageHandler();

	
	// close management
	private static boolean IS_CLOSE_CONNECTION_ON_5XX_RESPONSE = Boolean.parseBoolean(System.getProperty("org.xlightweb.server.closeConnectionOn5xxResponse", "true"));
	private boolean isCloseOnSendingError = false;
	private boolean isDelayedClosed = false;
	private final ConnectionCloser connectionCloser = new ConnectionCloser();

	
	// timeout support
	private static final long MIN_WATCHDOG_PERIOD_MILLIS = 10 * 1000;
	private TimeoutWatchDogTask watchDogTask;
	private Long requestTimeoutMillis;


	
	// session support
	private static final boolean IS_SESSION_COOKIE_HTTPONLY = Boolean.parseBoolean(System.getProperty("org.xlightweb.server.sessionCookieHttpOnly", "true")); 
	private final ISessionManager sessionManager;
	private final int sessionMaxInactiveIntervalSec; 
	private boolean useCookies = true;
	
	
	// compress support 
	private final int autocompressThresholdBytes;
	
	// max transaction support
	private Integer maxTransactions;
	
	   
    // transaction monitor support
    private final TransactionMonitor transactionMonitor;
    

	
	// message handling
	private final IMessageHeaderHandler messageHandler;
	
	
	// handler support
	private final RequestHandlerAdapter requestHandlerAdapter;
	

    // upgrade support
    boolean isAutohandleUpgadeHeader = DEFAULT_AUTOHANDLE_CONNECTION_UPGRADE_HEADER;
    final IUpgradeHandler upgradeHandler;

	
	
	/**
	 * constructor 
	 * 
	 * @param defaultEncncoding           the body default encoding
	 * @param sessionManager              the session manager
	 * @param transactionMonitor          the transaction monitor or <code>null</code>
	 * @param tcpConnection               the tcp connection
	 * @param requestHandler              the request handler
	 * @param requestHandlerInfo          the request handle info
	 * @param isCloseOnSendingError       true, if connection should be closed by sending an error
	 * @param connectionHandlers          the connection handler
	 * @param useCookies                  true, if cookies is used for session state management
	 * @param autocompressThresholdBytes  the autocompress threshold
	 * @param isAutoUncompress            true, if the request should be uncompressed 
	 * @throws IOException if an exception occurs
	 */
	HttpServerConnection(String defaultEncoding, ISessionManager sessionManager, TransactionMonitor transactionMonitor, int defaultMaxInactiveIntervalSec, INonBlockingConnection tcpConnection, Object requestHandlerAdapter, IUpgradeHandler upgradeHandler, boolean isCloseOnSendingError, List<IHttpConnectionHandler> connectionHandlers, boolean useCookies, int autocompressThresholdBytes, boolean isAutoUncompress) throws IOException {
		super(tcpConnection, false);
		
		this.sessionManager = sessionManager;
		this.transactionMonitor = transactionMonitor;
		this.sessionMaxInactiveIntervalSec = defaultMaxInactiveIntervalSec;
		this.isCloseOnSendingError = isCloseOnSendingError;
		this.useCookies = useCookies;
		this.autocompressThresholdBytes = autocompressThresholdBytes;
		
		setAutoUncompress(isAutoUncompress);

		this.requestHandlerAdapter = (RequestHandlerAdapter) requestHandlerAdapter;		
		
		if (connectionHandlers != null) {
			for (IHttpConnectionHandler connectionHandler : connectionHandlers) {
				addConnectionHandler(connectionHandler);
			}
		}
		
		this.upgradeHandler = upgradeHandler;
		messageHandler = new MessageHeaderHandler();
		
		setBodyDefaultEncoding(defaultEncoding);
		
		init();
		
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("[" + getId() + "] http server connection established");
		}		
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isServerSide() {
	    return true;
	}
	

	/**
	 * set the max transactions per connection. Setting this filed causes that 
	 * a keep-alive response header will be added 
	 * 
	 * @param maxTransactions  the max transactions 
	 */
	public void setMaxTransactions(int maxTransactions) {
		this.maxTransactions = maxTransactions;
	}
	

	
	/**
     * set the request body default encoding. According to RFC 2616 the 
     * initial value is ISO-8859-1 
     *   
     * @param encoding  the defaultEncoding 
     */
    public void setRequestBodyDefaultEncoding(String defaultEncoding) {
        setBodyDefaultEncoding(defaultEncoding);
    }	
	
    
    boolean isAutohandleUpgade() {
        return isAutohandleUpgadeHeader;
    }
    
    void setAutohandleUpgade(boolean isAutohandleUpgade) {
        this.isAutohandleUpgadeHeader = isAutohandleUpgade;
    }
    
    
	
	/**
	 * schedules the task
	 * 
	 * @param task      the task to schedule
	 * @param delay     the delay 
	 * @param period    the period
	 */
	protected static void schedule(TimerTask task, long delay, long period) {
		AbstractHttpConnection.schedule(task, delay, period);
	}
	
	
	protected static int getSizeDataReceived(NonBlockingBodyDataSource body) {
	    return AbstractHttpConnection.getSizeDataReceived(body);
	}
	    
    protected static int getSizeWritten(BodyDataSink body) {
        return AbstractHttpConnection.getSizeWritten(body);
    }
       	
	
    static Object wrap(IHttpRequestHandler requestHandler) {
		return new RequestHandlerAdapter(requestHandler);
	}

	
	
	/**
	 * set the max receive timeout which is accepted for the connection. Setting this
	 * field cases, that a keep-alive response header will be added 
	 * 
	 * @param requestTimout the timeout
	 */
	public void setRequestTimeoutMillis(long requestTimeoutMillis) {
		
		if (requestTimeoutMillis < MIN_REQUEST_TIMEOUT_MILLIS) {
			LOG.warning("[" + getId() + "] try to set request timeout with " + requestTimeoutMillis + " millis. This value will be ignored because it is smaller that " + MIN_REQUEST_TIMEOUT_MILLIS + " millis");
		}

		if (requestTimeoutMillis <= 0) {
			requestHandlerAdapter.onRequestTimeout(this);
			return;
		}
		
		setLastTimeDataReceivedMillis(System.currentTimeMillis());
		
		if ((this.requestTimeoutMillis == null) || (this.requestTimeoutMillis != requestTimeoutMillis)) {
			this.requestTimeoutMillis = requestTimeoutMillis;
			
			if (requestTimeoutMillis == Long.MAX_VALUE) {
				terminateWatchDogTask();
				
			} else {
				
				long watchdogPeriod = 100; 
				
				if (requestTimeoutMillis > 1000) {
					watchdogPeriod = requestTimeoutMillis / 10;
				}
				
				if (watchdogPeriod > MIN_WATCHDOG_PERIOD_MILLIS) {
					watchdogPeriod = MIN_WATCHDOG_PERIOD_MILLIS;
				}
				
				updateWatchDog(watchdogPeriod);
			}
		}
	}




	private void checkRequestTimeout(long currentTimeMillis) {
	      
	    if (requestTimeoutMillis != null) {
	        long timeoutReceived = getLastTimeDataReceivedMillis() + requestTimeoutMillis;
	        long timeoutSend = getLastTimeWritten() + requestTimeoutMillis;
	            
	        if ((currentTimeMillis > timeoutReceived) && (currentTimeMillis > timeoutSend)) {
	            onRequestTimeout();
	        }
	    }
	}
	
	
	private void onRequestTimeout() {
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("[" + getId() + "] request timeout " + DataConverter.toFormatedDuration(requestTimeoutMillis) + " reached");
		}
		
		terminateWatchDogTask();
		requestHandlerAdapter.onRequestTimeout(this);
	}
	
	
	private synchronized void updateWatchDog(long watchDogPeriod) {
	    if (LOG.isLoggable(Level.FINE)) {
	        LOG.fine("[" + getId() + "] update timeout watchdog task period to " + DataConverter.toFormatedDuration(watchDogPeriod));
	    }
		terminateWatchDogTask();

	    watchDogTask = new TimeoutWatchDogTask(this); 
	    AbstractHttpConnection.schedule(watchDogTask, watchDogPeriod, watchDogPeriod);
	}
	
	
	private synchronized void terminateWatchDogTask() {
		if (watchDogTask != null) {
	        watchDogTask.close();
	        watchDogTask = null;
	    }
	}

	
	private static final class TimeoutWatchDogTask extends TimerTask implements Closeable {
		
		private WeakReference<HttpServerConnection> connectionRef = null;
		
		public TimeoutWatchDogTask(HttpServerConnection connection) {
			connectionRef = new WeakReference<HttpServerConnection>(connection);
		}
	
		
		@Override
		public void run() {
			
			WeakReference<HttpServerConnection> ref = connectionRef;
			if (ref != null) {
				HttpServerConnection connection = ref.get();
					
				if (connection == null)  {
					this.close();
						
				} else {
					connection.checkRequestTimeout(System.currentTimeMillis());
				}
			}
		}
		
		public void close() {
			this.cancel();
			connectionRef = null;
		}
	}
	

	

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onProtocolException(Exception ex) {
		if (ex instanceof BadMessageException) {
			setPersistent(false);
			try {
				sendResponseMessage(new HttpResponse(((BadMessageException) ex).getStatus(), "text/html", generateErrorMessageHtml(400, ex.getMessage(), getId())), false);
			} catch (IOException ioe) {
				if (LOG.isLoggable(Level.FINE)) {
					LOG.fine("[" + getId() + "] could not send error message " + 400 + " reason " + ioe.toString());
				}
				destroy();
			}
		} else {
			super.onProtocolException(ex);
		}
	}
	
	
	@Override
	protected void onMessageWritten() {
	    
	    if (!isPersistent()) {	       
	        if (getNumOpenTransactions() > 0) {
	            if (LOG.isLoggable(Level.FINE)) {
	                LOG.fine("[" + getId() + "] connection is not persistent. closing it after " + DataConverter.toFormatedDuration(CLOSE_DELAY_ON_OPEN_TRANSACTIONS_MILLIS) + " -> openTransactions " + getNumOpenTransactions());
	            }
	            
	            isDelayedClosed = true;
	            destroy(CLOSE_DELAY_ON_OPEN_TRANSACTIONS_MILLIS);
	            
	        } else {
	            if (LOG.isLoggable(Level.FINE)) {
	                LOG.fine("[" + getId() + "] connection is not persistent. closing it now");
	            }
	            
	            destroy();
	        }
	    }
	}
	
	boolean isDelayedClosed() {
	    return isDelayedClosed;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected IMessageHeaderHandler getMessageHeaderHandler() {
		return messageHandler;
	}

	
	
	
	@Override
	protected void onDisconnect() {	    
		super.onDisconnect();
		
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("[" + getId() + "] http server connection destroyed");
		}
	}

	

/*	
	private void suspendMessageReceiving() {
		try  {
			super.suspendReceiving();
		} catch (IOException ioe) {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("[" + getId() + "] error occured by suspending read " + ioe.toString());
			}
		}
	}
	
	private void resumeMessageReceiving() {
		try  {
			super.resumeReceiving();
		} catch (IOException ioe) {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("[" + getId() + "] error occured by resuming read " + ioe.toString());
			}
		}
	}
*/
	
	
	private void handleLifeCycleHeaders(IHttpRequest request) {

		IHttpRequestHeader header = request.getRequestHeader();
		
		String keepAliveHeader = header.getHeader("KeepAlive");
		if (keepAliveHeader != null) {
			String[] tokens = keepAliveHeader.split(",");
			for (String token : tokens) {
				handleKeepAlive(token);
			}
		}


		String connectionHeader = header.getHeader("Connection");
		if (connectionHeader != null) {
			String[] values = connectionHeader.split(",");

			for (String value : values) {
				value = value.trim();

				if (value.equalsIgnoreCase("close")) {
					if (LOG.isLoggable(Level.FINE)) {
						LOG.fine("[" + getId() + "] received connection: closed header. destroying connection");
					}
					setPersistent(false);
				}
				
				if (value.equalsIgnoreCase("Keep-Alive")) {
					setPersistent(true);
				}
			}
		}
	}

	

	private void handleKeepAlive(String option) {
        Integer timeoutSec = null; 
        
        if (option.toUpperCase().startsWith("TIMEOUT=")) {
            timeoutSec = Integer.parseInt(option.substring("TIMEOUT=".length(), option.length()));

        } else {
            timeoutSec = Integer.parseInt(option);
        }
        

        if ((timeoutSec != null) && (getBodyDataReceiveTimeoutMillis() == Long.MAX_VALUE)) {
            setBodyDataReceiveTimeoutMillis(timeoutSec * 1000L);
        }
	}
	
	

	private BodyDataSink sendResponseHeader(IHttpResponseHeader header, boolean autocompress) throws IOException {
		
		try{
			enhanceResponseHeader(header);
			
			BodyDataSink bodyDataSink;
			
			
			if (autocompress) {
				int length = header.getContentLength();
				if (length != -1) {
					header.removeHeader("Content-Length");
					header.setHeader("Transfer-Encoding", "chunked");
				}

				bodyDataSink = writeMessage(autocompressThresholdBytes, header);
				
				if (length != -1) {
					setAutocompressThreshold(bodyDataSink, 0);
				}
			} else {
				bodyDataSink = writeMessage(Integer.MAX_VALUE, header);
			}
			
			if (!isPersistent()) {
				setBodyCloseListener(bodyDataSink, connectionCloser);
			}
			
			return bodyDataSink;
			
		} catch (IOException ioe) {
			destroy();
			throw ioe;
		}
	}
	
	

	private BodyDataSink sendResponseHeader(IHttpResponseHeader header, int contentLength, boolean autocompress) throws IOException {

		try{
			enhanceResponseHeader(header);

			BodyDataSink bodyDataSink;
			if (autocompress && (contentLength > autocompressThresholdBytes)) {
				header.removeHeader("Content-Length");
				header.setHeader("Transfer-Encoding", "chunked");
				
				bodyDataSink = writeMessage(0, header);
			} else {
				bodyDataSink = writeMessage(header, contentLength);	
			}
			
			if(!isPersistent()) {
				setBodyCloseListener(bodyDataSink, connectionCloser);
			}
		
			
			return bodyDataSink;
			
		} catch (IOException ioe) {
			destroy();
			throw ioe;
		}
	}


	
	private BodyDataSink sendResponseMessage(IHttpResponse response, boolean autocompress) throws IOException {

		try{	
			IHttpResponseHeader responseHeader = response.getResponseHeader();
			
			if (response.getStatus() == 100) {
                writeMessageSilence(response);
                return null;

			} else {
    			
    			enhanceResponseHeader(responseHeader);
    			
    			// does the message contain a body?
    			if (response.hasBody()) {
                    if (response.getNonBlockingBody().getDataHandler() != null) {
                        LOG.warning("[" + getId() + "] a body handler is already assigned to the message body. current body handler will be removed");
                    }
        
                                       
                    
                    
                    if (autocompress) {
                    	response.removeHeader("Content-Length");
                    	response.setHeader("Transfer-Encoding", "chunked");
                    	
                    	if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("[" + getId() + "] sending: " + response.getResponseHeader());
                        }

                    	return writeMessage(autocompressThresholdBytes, response);
                    } else {
                    	
                    	if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("[" + getId() + "] sending: " + response.getResponseHeader());
                        }
                    	return writeMessage(Integer.MAX_VALUE, response);
                    }

                    
    			// ... no its bodyless
    			} else {
                    
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + getId() + "] sending (bodyless): " + response.getResponseHeader());
                    }
    			    
                    if ((response.getContentLength() == -1) && (response.getStatus() != 100) && (response.getStatus() != 101)) {
                        response.setContentLength(0);
                    }
            
                    BodyDataSink bodyDataSink = writeMessage(responseHeader, 0);
                    bodyDataSink.setFlushmode(FlushMode.ASYNC);
                    bodyDataSink.close();
                        
                    if(!isPersistent()) {
                        closeQuitly();
                    }
                    
                    return null;
    			}
			}
		} catch (IOException ioe) {
			destroy();
			throw ioe;
		}
	}
	

	private void enhanceResponseHeader(IHttpResponseHeader header) {

	    if (header.getStatus() > 199) { 
    		String server = header.getServer();
    		if (server == null) {
    			header.setServer(ServerUtils.getComponentInfo());
    		}
        		
    		if (header.getProtocol().equalsIgnoreCase("HTTP/1.0")) {
    		    setPersistent(false);
    		}
    		
    		int remainingTransactions = 0; 
    		if (maxTransactions != null) {
    			remainingTransactions = maxTransactions - getCountMessagesReceived();
    			if (remainingTransactions <= 0) {
    				setPersistent(false);
    			}
    		}
    
            if ((header.getStatus() >= 400) && (isCloseOnSendingError)) {
                setPersistent(false);
            }

    		
    		if ((isPersistent() && (header.getHeader("Connection") == null)) &&
    			((maxTransactions != null) || (requestTimeoutMillis != null))) {
    			
    			header.setHeader("Connection" , "Keep-Alive");
    			
    			String keepAliveValue = null;
    			if (maxTransactions != null) {
    				keepAliveValue = "max=" + remainingTransactions;
    			}
    			
    			if (requestTimeoutMillis != null) {
    				if (keepAliveValue == null) {
    					keepAliveValue = "timeout=" + requestTimeoutMillis / 1000; 
    				} else {
    					keepAliveValue += ", timeout=" + requestTimeoutMillis / 1000;
    				}
    			}
    			
    			header.setHeader("Keep-Alive", keepAliveValue);
    
    		}
    
    
    		String connectionHeader = header.getHeader("Connection");
    		if ((connectionHeader != null) && connectionHeader.equalsIgnoreCase("close")) {
    			setPersistent(false);
    
    		} else {
    			if (!isPersistent()) {
    				header.setHeader("Connection", "close");
    				header.removeHeader("Keep-Alive");
    			}
    			
    		}
	    }
	}
	
	
	private HttpSession getSession(IHttpRequest request) throws IOException {
		HttpSession session = null;
		
		if (useCookies) {
			List<String> cookieHeaders = request.getHeaderList("Cookie");
			for (String cookieHeader : cookieHeaders) {
				String[] cookies = cookieHeader.split(";");
				for (String cookie : cookies) {
					cookie = cookie.trim();
					int idx = cookie.indexOf("=");
					if (idx != -1) {
						String name = cookie.substring(0, idx);
						if (name.equals("JSESSIONID")) {
							String sessionId = cookie.substring(idx + 1, cookie.length()).trim();
							session = sessionManager.getSession(sessionId);
							if (session != null) {
								return session;
							}
						}
					}
				}
			}
			
		} else {
		    String sessionToken = request.getMatrixParameter("jsessionid");
		    if (sessionToken != null) {
		        request.removeMatrixParameter("jsessionid");
		        session = sessionManager.getSession(sessionToken);
		        if (session != null) {
		            return session;
				}
			}
		}
		
		return null;
	}

	

	private boolean isLargerOrEquals(String protocolVersion, String otherProtocolVersion) {
		
		try {
			int idx = protocolVersion.indexOf(".");
			int major = Integer.parseInt(protocolVersion.substring(0, idx));
			int minor = Integer.parseInt(protocolVersion.substring(idx + 1, protocolVersion.length()));
				
			int idxOther = otherProtocolVersion.indexOf(".");
			int majorOther = Integer.parseInt(otherProtocolVersion.substring(0, idxOther));
			int minorOther = Integer.parseInt(otherProtocolVersion.substring(idxOther + 1, otherProtocolVersion.length()));
				
			if (major > majorOther) {
				return true;
			} else if (major < majorOther) {
				return false;
			}
					
			if (minor > minorOther) {
				return true;
			} else if (minor < minorOther) {
				return false;
			}
			
			return true;
		} catch (Exception e) {
			throw new RuntimeException("[" + getId() + "] error occured by comparing protocol version " + protocolVersion + " and " + otherProtocolVersion);
		}
	}
	
	

	private boolean isContentTypeFormUrlencoded(IHttpMessage message) {
		if (!message.hasBody()) {
			return false;
		}
		
		String contentType = message.getContentType();
		if ((contentType != null) && (contentType.startsWith("application/x-www-form-urlencoded"))) {
			return true;
		}
		
		return false;
	}
	
  
	
	private final class ConnectionCloser implements Runnable {
		
		public void run() {
			closeQuitly();
		}
	}
	
	
	
	private final class MessageHeaderHandler implements IMessageHeaderHandler {
	    

	    public IHttpMessageHeader getAssociatedHeader() {
	        return null;
	    }

		
		public IMessageHandler onMessageHeaderReceived(IHttpMessage message) throws IOException {
			final IHttpRequest request = (IHttpRequest) message;
		
			
			if (transactionMonitor != null) {
			    transactionMonitor.registerMessageHeaderReceived(HttpServerConnection.this, request.getRequestHeader());
			}
			
		
			// handle Connection header
			handleLifeCycleHeaders(request);
			
			
			if (LOG.isLoggable(Level.FINE)) {
				
				if (request.hasBody()) {
                    String body = "";
                    
                    String contentType = request.getContentType();
                    if ((contentType != null) && (contentType.startsWith("application/x-www-form-urlencode"))) {
                        body = request.getNonBlockingBody().toString() + "\n";                      
                    } 
                    
                    LOG.fine("[" + getId() + "] request received  from " + getRemoteAddress() + 
                             ":" + getRemotePort() + 
                             " (" + getCountMessagesReceived() + ". request) " + request.getRequestHeader().toString() + body);
				
				} else {
                    LOG.fine("[" + getId() + "] bodyless request received from " + getRemoteAddress() +
                            ":" + getRemotePort() + 
                            " (" + getCountMessagesReceived() + ". request) " + request.getRequestHeader().toString());
				}
			}
			
			
	
			boolean isFormUrlEncoded = isContentTypeFormUrlencoded(request);

			if (message.hasBody()) {

			    // handle 100 continue header
	            if (HttpUtils.isContainExpect100ContinueHeader(request.getRequestHeader())) {
	                
	                if (isFormUrlEncoded || requestHandlerAdapter.isInvokeOnMessageReceived()) {
	                    if (LOG.isLoggable(Level.FINE)) {
	                        if (requestHandlerAdapter.isInvokeOnMessageReceived()) {
	                            LOG.fine("request handler (chain) will be invoked onMessageReceived -> autohandle 100-continue");
	                        } else if (isFormUrlEncoded) {
	                            LOG.fine("request contains FormUrlEncoded body -> autohandle 100-continue");
	                        } 
	                    }
	                    
	                    request.setAttribute("org.xlightweb.100-continue-has-been-sent", "true");
	                    writeMessageSilence(new HttpResponse(100));
	                    return DO_NOTHING_HANDLER;
	                } 
	            } 


			    
			    
			    // will set body receive timeout, if exits
			    if (getBodyDataReceiveTimeoutMillis() != Long.MAX_VALUE) {
			        message.getNonBlockingBody().setBodyDataReceiveTimeoutMillis(getBodyDataReceiveTimeoutMillis());
			    }
			    
			    // is FORM encoded request?
	            if (isFormUrlEncoded) {
	                ServerExchange exchange = new ServerExchange(HttpServerConnection.this, newFormEncodedRequestWrapper(request));
	                return new InvokeOnMessageReceivedMessageHandler(this, exchange);

	            // is invokeOnMessage?
	            } else if (requestHandlerAdapter.isInvokeOnMessageReceived()) {
	                ServerExchange exchange = new ServerExchange(HttpServerConnection.this, request);
	                return new InvokeOnMessageReceivedMessageHandler(this, exchange);
	            }
			} 
			
			
			return new IMessageHandler() {
				
		    	public void onHeaderProcessed() throws IOException {
		    		ServerExchange exchange = new ServerExchange(HttpServerConnection.this, request);
					exchange.perform();
		    	}
		    	
		    	public void onMessageReceived() throws IOException { 
					 if (transactionMonitor != null) {
						 transactionMonitor.registerMessageReceived(HttpServerConnection.this, request);
					 }
		    	}
		    	
		    	public void onBodyException(IOException ioe, ByteBuffer[] rawData) { 
					 if (transactionMonitor != null) {
						 transactionMonitor.registerMessageReceivedException(HttpServerConnection.this, request, ioe);
					 }
		    	}
		    };
		}

		
		
        /**
         * {@inheritDoc}
         */		  
		public void onHeaderException(IOException ioe, ByteBuffer[] rawData) {
		    if (LOG.isLoggable(Level.FINE)) {
		        LOG.fine("[" + getId() + "] error occured by receiving request header " + ioe.toString());
		    }
		    destroy();
        }
        
		
	}

	

    private final class InvokeOnMessageReceivedMessageHandler implements IMessageHandler {
        
        private final MessageHeaderHandler headerHandler;
        private final ServerExchange exchange;
        
        public InvokeOnMessageReceivedMessageHandler(MessageHeaderHandler headerHandler, ServerExchange exchange) {
            this.headerHandler = headerHandler;
            this.exchange = exchange;
        }
        
        public void onHeaderProcessed() throws IOException {
        	
        }
        
        
        /**
         * {@inheritDoc}
         */
        public void onMessageReceived() throws IOException {
            if (transactionMonitor != null) {
                transactionMonitor.registerMessageReceived(HttpServerConnection.this, exchange.getRequest());
            }
            
            exchange.perform();
        }
        
        
        /**
         * {@inheritDoc}
         */
        public void onBodyException(IOException ioe, ByteBuffer[] rawData) {
            if (transactionMonitor != null) {
                transactionMonitor.registerMessageReceivedException(HttpServerConnection.this, exchange.getRequest(), ioe);
            }
            
            headerHandler.onHeaderException(ioe, rawData);
        }
    }
    
	
	
	
	private class ServerExchange extends AbstractExchange {

		private final IHttpRequest request;
		
		private HttpSession session = null;
		private boolean isSessionCreated = false;
		

		protected ServerExchange(HttpServerConnection httpCon, IHttpRequest request) throws IOException {
		    super(null, httpCon);
		    
		    this.request = request;
		    
		    if (HttpUtils.isContainExpect100ContinueHeader(request.getRequestHeader()) || 
		    	(request.getHeader("Connection") != null) && (request.getHeader("Connection").equalsIgnoreCase("Upgrade"))) {
		    	isCloseOnSendingError = true;
		    }
						
			if (!sessionManager.isEmtpy()) {
				resolveSession();
			}
		}
		
		
		void perform() throws BadMessageException, IOException {
		    

            // handle upgrade
            String upgrade = request.getRequestHeader().getHeader("Upgrade");
            if (upgrade != null) {
                boolean isUpgrade = upgradeHandler.onRequest(this);
                if (isUpgrade) {
                    return;
                }
            }
 		    
		    requestHandlerAdapter.onRequest(this);
		}
		
		
		public IHttpSession getSession(boolean create) {

			resolveSession();
			if (session != null) {
				return session;
			}
			
			if (create) {
				isSessionCreated = true;
				try {
					int prefix = request.getContextPath().hashCode();
					session = sessionManager.getSession(sessionManager.newSession(Integer.toHexString(prefix)));
					session.setMaxInactiveInterval(sessionMaxInactiveIntervalSec);
					return session;
				} catch (IOException ioe) {
					throw new RuntimeException(ioe.toString());
				}
			}
			
			return null;
		}
		
		
		public String encodeURL(String url) {
			if (useCookies) {
				return url;
				
			} else {
				
				// add path parameter (for URL path paramter see http://doriantaylor.com/policy/http-url-path-parameter-syntax)
				
			    int pos = url.indexOf('?');
			    if (pos == -1) {
		            pos = url.indexOf('#');
			    } 
			    
			    if (pos == -1) {
			    	return url + ";jsessionid=" + session.getId();
			    } else {
		            return url.substring(0, pos) + ";jsessionid=" + session.getId() + url.substring(pos);
			    }
		 	}				
		}

		
		
		private void resolveSession() {
			if (session != null) {
				return;
			}
			
			try {
				session = HttpServerConnection.this.getSession(request);
				if (session != null) {
					String idPrefix = Integer.toHexString(request.getContextPath().hashCode());
					if (session.getId().startsWith(idPrefix)) {
						session.setLastAccessTime(System.currentTimeMillis());
					} else {
						session = null;
					}
				}
			} catch (IOException ioe) {
				if (LOG.isLoggable(Level.FINE)) {
					LOG.fine("[" + getId() + "] error occured by resolving session " + ioe.toString());
				}
			}
		}
		
		
		public final IHttpRequest getRequest() {
			return request;
		}
	
		
		
		private boolean isResponseCompressable(IHttpResponseHeader header) {
			return (autocompressThresholdBytes != Integer.MAX_VALUE) &&
			       HttpUtils.isAcceptEncdoingGzip(request.getRequestHeader()) && 
			       header.getProtocolVersion().equals("1.1") &&
			       ((header.getStatus() == 200) || (header.getStatus() == 201) || (header.getStatus() == 206)) &&
			       HttpUtils.isCompressableMimeType(header.getContentType()) && 
			       (header.getHeader("Content-Encoding") == null);  
		}


		/**
		 * {@inheritDoc}
		 */
		public BodyDataSink send(IHttpResponseHeader header) throws IOException {
            ensureResponseHasNotBeenCommitted();
			
			handleCookieOnSend(header);

			// is chunked?
            if (header.getContentLength() == -1) {
                
                // is HTTP 0.9 or HTTP 1.0?
                if (header.getProtocolVersion().equalsIgnoreCase("0.9") || header.getProtocolVersion().equalsIgnoreCase("1.0")) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + getId() + "] response is " + header.getProtocolVersion() + " version");
                    }

                    HttpResponseHeader newHeader;
                    if (header.getProtocolVersion().equalsIgnoreCase("0.9")) {
                        newHeader = new HttpResponseHeader(200);
                    } else {
                        newHeader = new HttpResponseHeader(header.getStatus());
                    }
                    newHeader.copyHeaderFrom(header);
                    
                    header = newHeader;
                }
                
                
                // is event-stream content type?
                if (HttpUtils.hasContentType(header, "text/event-stream")) {
                    header.setHeader("Connection", "close");
                    HttpUtils.setExpireHeaders(header, 0);
                    
                    
                // does caller support chunked response? if not convert it to length-based response                    
                } else if (!AbstractHttpConnection.isAcceptingChunkedResponseBody(getRequest())) {
    				if (LOG.isLoggable(Level.FINE)) {
    					LOG.fine("[" + getId() + "] the requestor does not support chunked response messages (request protocol: " + getRequest().getProtocol() + "). Converting chunked response into simple response.");
    				}
    				
    				HttpResponseHeader newHeader = new HttpResponseHeader(header.getStatus(), header.getReason());
    				newHeader.copyHeaderFrom(header);
    				newHeader.setProtocol(getRequest().getProtocol());
    				newHeader.setHeader("Connection", "close");
    				
    				header = newHeader;
    				
    			// default 
    			} else {
    			    header.setTransferEncoding("chunked");
    			}
            }
			
			setResponseCommited(true);
			
			final BodyDataSink bodyDataSink = HttpServerConnection.this.sendResponseHeader(header, isResponseCompressable(header));
			
			if (transactionMonitor != null) {
			    transactionMonitor.registerMessageHeaderSent(request, header, bodyDataSink);
			    
			    Runnable task = new Runnable() {
			        public void run() {
			            transactionMonitor.registerMessageSent(request);
			        }
			    };
			    setBodyCloseListener(bodyDataSink, task);
			}
			
			return bodyDataSink;
		}
		


    
		
				

		/**
		 * send the response in a plain body non-blocking mode
		 *
		 * @param header         the header
		 * @param contentLength  the body content length
		 * @return the body handle to write
		 * @throws IOException if an exception occurs
		 */
		public BodyDataSink send(IHttpResponseHeader header, int contentLength) throws IOException {
			ensureResponseHasNotBeenCommitted();

			handleCookieOnSend(header);

			header.setContentLength(contentLength);

			try {
				// http protocol version downgrade necessary?
				if ((!getRequest().getProtocolVersion().equals(header.getProtocolVersion())) &&
					(isLargerOrEquals(header.getProtocolVersion(), getRequest().getRequestHeader().getProtocolVersion()))) {
					
					header.setHeader("Connection", "close");
					header.setProtocol(getRequest().getProtocol());
				}
				
			} catch (Exception e) {
				HttpResponse errorResponse = null;
				if (HttpUtils.isShowDetailedError()) {
					errorResponse = new HttpResponse(400, "text/html", generateErrorMessageHtml(400, DataConverter.toString(e), getId()));
					
				} else {
					errorResponse = new HttpResponse(400, "text/html", generateErrorMessageHtml(400, HttpUtils.getReason(400), getId()));
				}
				setResponseCommited(true);
				HttpServerConnection.this.sendResponseMessage(errorResponse, false);
				throw new IOException(e.toString());
			}


			
			setResponseCommited(true);
			final BodyDataSink bodyDataSink = HttpServerConnection.this.sendResponseHeader(header, contentLength, isResponseCompressable(header));	
			
			if (transactionMonitor != null) {
			    transactionMonitor.registerMessageHeaderSent(request, header, bodyDataSink);

			    Runnable task = new Runnable() {
                    public void run() {
                        transactionMonitor.registerMessageSent(request);
                    }
                };
                setBodyCloseListener(bodyDataSink, task);
			}
			
			return bodyDataSink;
		}

		
		@Override
		protected void doSendContinue() throws IOException {
		    writeMessageSilence(new HttpResponse(100));
		}
		

		/**
		 * send the response
		 *
		 * @param response   the response
		 * @throws IOException if an exception occurs
		 */
		public void send(IHttpResponse response) throws IOException { 
		    
            if (response.hasBody() && isForwardable(response.getNonBlockingBody())) {
                BodyDataSink dataSink = send(response.getResponseHeader());
                forwardBody(response.getNonBlockingBody(), dataSink);
                return;
                
            } else { 
    			ensureResponseHasNotBeenCommitted();
    			
    			handleCookieOnSend(response.getResponseHeader());
    
    			try {
    				// request protocol version not equals response protocol version?
    				if (!response.getProtocolVersion().equals(getRequest().getProtocolVersion())) {
    					
    					// simple (HTTP/0.9) response?
    					if (response.getProtocolVersion().equals("0.9") && (response.getContentLength() == -1)) {
    						
    						HttpResponseHeader header = new HttpResponseHeader(200);
    						header.copyHeaderFrom(response.getResponseHeader());
    						header.setProtocol(getRequest().getProtocol());
    						header.setHeader("Connection", "close");
    						
    						BodyDataSink bodyDataSink = HttpServerConnection.this.sendResponseHeader(header, false);
    						
    						NonBlockingBodyDataSource bodyDataSource = response.getNonBlockingBody();
    						BodyForwarder forwarder = new BodyForwarder(bodyDataSource, bodyDataSink);
    						bodyDataSource.setDataHandler(forwarder);
    						return;
    					} 
    					
    					// http protocol version downgrade necessary?
    					if (isLargerOrEquals(response.getResponseHeader().getProtocolVersion(), getRequest().getRequestHeader().getProtocolVersion())) {
    						response.getResponseHeader().setProtocol(getRequest().getProtocol());
    						response.getResponseHeader().setHeader("Connection", "close");
    					}
    				}
    			} catch (Exception e) {
    				HttpResponse errorResponse = null;
    				if (HttpUtils.isShowDetailedError()) {
    					errorResponse = new HttpResponse(400, "text/html", generateErrorMessageHtml(400, DataConverter.toString(e), getId()));
    					
    				} else {
    					errorResponse = new HttpResponse(400, "text/html", generateErrorMessageHtml(400, HttpUtils.getReason(400), getId()));
    				}
    				setResponseCommited(true);
    				HttpServerConnection.this.sendResponseMessage(errorResponse, false);
    				throw new IOException(e.toString());
    			}
    
    			if (response.getStatus() != 100) {
    			    setResponseCommited(true);
    			}
    			
                addLengthHeaderIfRequired(response);
    			
    			BodyDataSink dataSink = HttpServerConnection.this.sendResponseMessage(response, isResponseCompressable(response.getResponseHeader()));
    			
    			if (transactionMonitor != null) {
    			    transactionMonitor.registerMessageHeaderSent(request, response.getResponseHeader(), dataSink);
    			    
    			    if (response.hasBody()) {
    			        
    			        BodyListener bodyListener = new BodyListener();
    			        NonBlockingBodyDataSource ds = response.getNonBlockingBody();
    			        ds.addCompleteListener(bodyListener);
    			        ds.addDestroyListener(bodyListener);
    			        
    			    } else {
    			        transactionMonitor.registerMessageSent(request);
    			    }
    			}
            }
		}
		
		
		private void addLengthHeaderIfRequired(IHttpResponse response) throws IOException {
		    
		    // body less?
		    if (!response.hasBody() && !HttpUtils.isBodylessStatus(response.getStatus())) {
                // add content-length header if not exists
                if ((response.getContentLength() == -1)) {
                    response.setContentLength(0);
                }
                
                // remove chunked header if present
                response.removeHeader("Transfer-Encoding");
		    }
		}
		
		
		private final class BodyListener implements IBodyCompleteListener, IBodyDestroyListener {
            
            @Execution(Execution.NONTHREADED)
            public void onComplete() throws IOException {
                transactionMonitor.registerMessageSent(request);
            }
            
            public void onDestroyed() throws IOException {
                transactionMonitor.registerMessageBodySentError(request);
            }
        }
		
		
		private void handleCookieOnSend(IHttpResponseHeader header) {
			
		    if (header.getStatus() != 100) {
    			if (session != null) {
    				try {
    					sessionManager.saveSession(session.getId());
    				} catch (IOException ioe) {
    					if (LOG.isLoggable(Level.FINE)) {
    						LOG.fine("[" + getId() + "] error occured by saving session " + session.getId());
    					}
    				}
    			}
    			
    			if (isSessionCreated && useCookies) {
    			    StringBuilder sb = new StringBuilder("JSESSIONID=" + session.getId() + ";path=/");
    			    if (IS_SESSION_COOKIE_HTTPONLY)  {
    			        sb.append(";HttpOnly");
    			    } 
    				
    				if (isSecure()) {
    					sb.append(";secure");
    				}
    				header.addHeader("Set-Cookie", sb.toString());
    			}
		    }
		}
		

		public BodyDataSink forward(IHttpRequestHeader requestHeader) throws IOException, ConnectException, IllegalStateException {
			return forward(requestHeader, new ForwardingResponseHandler(this));
		}
		

		public BodyDataSink forward(IHttpRequestHeader requestHeader, IHttpResponseHandler responseHandler) throws IOException, ConnectException {
		
			if (responseHandler == null) {
				responseHandler = new ForwardingResponseHandler(this);
			}

			
			BodyDataSink bodyDataSink = newEmtpyBodyDataSink(requestHeader);
				
			// send not handled error after the data sink is closed
			setBodyCloseListener(bodyDataSink, newCloseListener(responseHandler));   

				
			return bodyDataSink;
		}

		
		public BodyDataSink forward(IHttpRequestHeader requestHeader, int contentLength) throws IOException, ConnectException, IllegalStateException {
			return forward(requestHeader, contentLength, new ForwardingResponseHandler(this));
		}
		

		public BodyDataSink forward(IHttpRequestHeader requestHeader, int contentLength, IHttpResponseHandler responseHandler) throws IOException, ConnectException {
			
			if (responseHandler == null) {
				responseHandler = new ForwardingResponseHandler(this);
			}

			
			BodyDataSink bodyDataSink = newEmtpyBodyDataSink(requestHeader);
				
				
			// send not handled error after the data sink is closed
			setBodyCloseListener(bodyDataSink, newCloseListener(responseHandler));   
				
			return bodyDataSink;
		}

		
		private Runnable newCloseListener(final IHttpResponseHandler responseHandler) {
			return new Runnable() {
				
				public void run() {
					sendNotHandledError(responseHandler);
				}
			};
		}
	
		

		public void forward(IHttpRequest request, IHttpResponseHandler responseHandler) throws IOException, ConnectException {
			if (responseHandler == null) {
				responseHandler = new ForwardingResponseHandler(this);
			}
			
			sendNotHandledError(responseHandler);
		}
		
		public void forward(IHttpRequest request) throws IOException, ConnectException, IllegalStateException {
			forward(request, new ForwardingResponseHandler(this));
		}

			
		private void sendNotHandledError(final IHttpResponseHandler responseHandler) {
				
		    if (LOG.isLoggable(Level.FINE)) {
		        LOG.fine("[" + getId() + "] no handler found (for requested resource). returning error message");
		    }
		    
		    try {
		        IHttpResponse response = new HttpResponse(404, "text/html", generateErrorMessageHtml(404, null, getId()));
		        callResponseHandler(responseHandler, response);
		    } catch (IOException ioe) {
		        if (LOG.isLoggable(Level.FINE)) {
		            LOG.fine("[" + getId() + "] could not send not handle response. " + ioe.toString());
		        }
		    }
		}
		
		
		
		/**
		 * {@inheritDoc}
		 */
		public void sendError(Exception e) {
			int code = 500;
			
			if (e instanceof BadMessageException) {
				code = ((BadMessageException) e).getStatus();
			} else if (e instanceof MaxConnectionsExceededException) {
                code = 503;
            }
			
			if (HttpUtils.isShowDetailedError()) {
				sendError(code, DataConverter.toString(e));
				
			} else {
				if (e instanceof BadMessageException) {
					sendError(code, e.getMessage());
				} else {
					sendError(code);
				}
			}
		}
		
		
		
	

		/**
		 * send an error response
		 *
		 * @param errorCode   the error code
		 * @param msg         the error message
		 */
		public void sendError(int errorCode, String msg) {
			if (isCloseOnSendingError || ((errorCode >= 500) && IS_CLOSE_CONNECTION_ON_5XX_RESPONSE)) {
				setPersistent(false);
			}
			
			if (isResponseCommitted()) {
				throw new IllegalStateException("response is already committed"); 
			}

			super.sendError(errorCode, msg);
		}
	}
	
	
	@Execution(Execution.NONTHREADED)
	private static final class ForwardingResponseHandler implements IHttpResponseHandler {

		private IHttpExchange exchange = null;
		
		public ForwardingResponseHandler(IHttpExchange exchange) {
			this.exchange = exchange;
		}

		
		public void onResponse(IHttpResponse response) throws IOException {
			exchange.send(response);
		}

		public void onException(IOException ioe) throws IOException {
			exchange.sendError(ioe);
		}
	}
	
	private static final class DoNothingMessageHandler implements IMessageHandler {
		 
		public void onHeaderProcessed() throws IOException { }
	  
		public void onMessageReceived() throws IOException {  }
         
		public void onBodyException(IOException ioe, ByteBuffer[] rawData) {  }
	}	
	
	
	
	static final class AutoUpgradeHandler implements IUpgradeHandler {
	    
	    
	    private final IUpgradeHandler successor;
	    
	    
	    public AutoUpgradeHandler(IUpgradeHandler successor) {
	        this.successor = successor;
        }
	    
	    
	    public boolean onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
	        
	        IHttpRequest request = exchange.getRequest();
	        
	        HttpServerConnection httpCon = (HttpServerConnection) exchange.getConnection();
	        
	        if (httpCon.isAutohandleUpgadeHeader) {
    	        String upgrade = request.getRequestHeader().getHeader("Upgrade");
    	        
    	        if ((upgrade != null) && upgrade.equalsIgnoreCase("TLS/1.0")) {
                    if (httpCon.getUnderlyingTcpConnection().isSecuredModeActivateable()) {
                        httpCon.suspendReceiving();
                        
                        HttpResponse response = new HttpResponse(101);
                        response.setHeader("Connection", "Upgrade");
                        response.setHeader("Upgrade", "TLS/1.0, HTTP/1.1");
                        
                        exchange.send(response);
                        
                        httpCon.getUnderlyingTcpConnection().activateSecuredMode();
                    
                        httpCon.resumeReceiving();
                        
                    } else {
                        exchange.send(new HttpResponse(400, "text/html", generateErrorMessageHtml(400, "upgrade TLS is not supported", httpCon.getId())));
                    }
                    return true;
                    
    	        } else {
    	            if (successor != null) {
    	                return successor.onRequest(exchange);
    	            }  
    	        }
	        }
	        
	        return false;
	    }
	}
}
