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


import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.xlightweb.BadMessageException;
import org.xlightweb.HttpUtils;
import org.xlightweb.IHttpConnectHandler;
import org.xlightweb.IHttpConnectionHandler;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpMessage;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.server.HttpServerConnection.AutoUpgradeHandler;
import org.xsocket.DataConverter;
import org.xsocket.Execution;
import org.xsocket.ILifeCycle;
import org.xsocket.Resource;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;




/**
 * The http protocol adapter. The protocol adapter implements the {@link IConnectHandler} interface, and
 * "maps" each incoming connection into a {@link HttpServerConnection}. <br> <br>
 * HttpProtcol adapter will be required, if the {@link HttpServer} can not be used. See example:
 * 
 * <pre>
 * // establishing a http server based on a mutliplexed tcp connection
 * // for multiplexed connections see http://xsocket.sourceforge.net/multiplexed/tutorial/V2/TutorialMultiplexed.htm    
 *    
 * IRequestHandler hdl = new MyRequestHandler();
 * IConnectHandler httpAdapter = new HttpProtocolAdapter(hdl);
 * 
 * 
 * IServer mutliplexedHttpServer = new Server(8080, new MultiplexedProtocolAdapter(httpAdapter)));
 * mutliplexedHttpServer.start();
 * ...
 * </pre> 
 * 
 * 
 * @author grro
 */
@Execution(Execution.NONTHREADED)
public final class HttpProtocolAdapter implements IConnectHandler, ILifeCycle, MBeanRegistration {
	
	private static final Logger LOG = Logger.getLogger(HttpProtocolAdapter.class.getName());
	

	private static final boolean DEFAULT_CLOSE_ON_SENDING_ERROR = false;
	
	private static final NullRequestHandler NULL_REQUESTHANDLER = new NullRequestHandler();  
	
	
	
	@Resource
	private IServer server;
	

	private ISessionManager sessionManager = new SessionManager();
	private boolean useCookies = true;
	
	
	private IHttpRequestHandler requestHandler;
	private Object requestHandlerAdapter;
	private IUpgradeHandler upgradeHandler; 
	
	private final List<IHttpConnectionHandler> connectionHandlers = new ArrayList<IHttpConnectionHandler>();
	
	private Long requestTimeoutMillis;
	private Long bodyDataReceiveTimeoutMillis;
	
	private boolean isCloseOnSendingError = DEFAULT_CLOSE_ON_SENDING_ERROR;

	
	// compress support
	private static final int DEFAULT_AUTO_COMPRESS_THRESHOLD = Integer.parseInt(System.getProperty("org.xlightweb.server.response.autocompressThreshold", "512"));
	private int autocompressThresholdBytes = DEFAULT_AUTO_COMPRESS_THRESHOLD;
	private static final boolean DEFAULT_AUTO_UNCOMPRESS = Boolean.parseBoolean(System.getProperty("org.xlightweb.server.request.isAutouncompressActivated", "true"));
	private boolean isAutoUncompress = DEFAULT_AUTO_UNCOMPRESS; 
	
	
	// keep alive support 
	private Integer maxTransactions;

	
	// session support
	private int sessionMaxInactiveIntervalSec = Integer.MAX_VALUE;
	
	
    private String defaultEncoding = IHttpMessage.DEFAULT_ENCODING;

	
	// transaction monitor support
	private TransactionMonitor transactionMonitor = null;
	

	// statistics 
	private int numHandledConnections = 0;
	
	
	/**
	 * constructor 
	 *
	 * @param requestHandler     the requestHandler
	 * @param connectionHandler  the connectionHandler or <code>null</code>
	 * @param requestTimeoutHandler the request timeout handler or <code>null</code> 
	 */
	public HttpProtocolAdapter(IHttpRequestHandler requestHandler) {
	    this(requestHandler, null);
	}
  
	
    HttpProtocolAdapter(IHttpRequestHandler requestHandler, IUpgradeHandler upgradeHandler) {
        if (requestHandler == null) {
            requestHandler = NULL_REQUESTHANDLER;
        } else {
            
            if (!HttpUtils.isConnectHandlerWarningIsSuppressed() && (requestHandler instanceof IHttpConnectHandler)) {
                LOG.warning("only IHttpRequestHandler is supported. The onConnect(...) method will not be called. (suppress this warning by setting system property org.xlightweb.httpConnectHandler.suppresswarning=true)");
            }
        }

        this.upgradeHandler =  new AutoUpgradeHandler(upgradeHandler);
        this.requestHandler = requestHandler;
        requestHandlerAdapter = HttpServerConnection.wrap(requestHandler);
    }
  
	
	
	/**
	 * returns the request handler 
	 * 
	 * @return the request handler
	 */
	public IHttpRequestHandler getRequestHandler() {
		return requestHandler;
	}

	
	

	/**
	 * sets the session manager
	 * 
	 * @param sessionManager  the session manager
	 */
	public void setSessionManager(ISessionManager sessionManager) {
		try {
			this.sessionManager.close();
		} catch (IOException ioe) {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("error occured by closing session manager " + ioe.toString());
			}
		}
		
		this.sessionManager = sessionManager;
	}

	
	

    /**
     * sets the transaction monitor
     * 
     * @param transactionMonitor  the transaction monitor
     */
    void setTransactionMonitor (TransactionMonitor transactionMonitor) {
        this.transactionMonitor = transactionMonitor;
    }

    
	
	
	/**
	 * sets if cookies is used for session state management 
	 *  
	 * @param useCookies true, if cookies isused for session state management 
	 */
	public void setUsingCookies(boolean useCookies) {
		this.useCookies = useCookies;
	}

	/**
	 * returns true, if cookies is used for session state management
	 * 
	 * @return true, if cookies is used for session state management
	 */
	public boolean isUsingCookies() {
		return useCookies;
	}

	/**
	 * returns the session manager
	 * 
	 * @return the session manager
	 */
	public ISessionManager getSessionManager() {
		return sessionManager;
	}
	
	
	int getNumHandledConnections() {
	    return numHandledConnections;
	}
	
	
   /**
     * set the request body default encoding. According to RFC 2616 the 
     * initial value is ISO-8859-1 
     *   
     * @param encoding  the defaultEncoding 
     */
    public void setRequestBodyDefaultEncoding(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }

	/**
     * get the request body default encoding. According to RFC 2616 the 
     * initial value is ISO-8859-1 
     *   
     * @return the defaultEncoding 
     */
    public String getRequestBodyDefaultEncoding() {
    	return defaultEncoding;
    }

	/**
	 * {@inheritDoc}
	 */
	public ObjectName preRegister(MBeanServer mbeanServer, ObjectName name) throws Exception {
	    
        ISessionManager sessionManager = ((HttpServer) server).getSessionManager();
        
        ObjectName objectName = new ObjectName(name.getDomain() + ":type=" + "sessionManager" + ",name=" + sessionManager.hashCode());
        mbeanServer.registerMBean(new SessionManagerInfo(sessionManager), objectName);


        if (requestHandler instanceof MBeanRegistration) {
            MBeanRegistration mbeanRegistration = (MBeanRegistration) requestHandler;
            mbeanRegistration.preRegister(mbeanServer, name);
        } 

		return ServerUtils.exportMbean(mbeanServer, name, requestHandler);		
	}

	/**
	 * {@inheritDoc}
	 */
	public void postRegister(Boolean registrationDone) {
        if (requestHandler instanceof MBeanRegistration) {
            MBeanRegistration mbeanRegistration = (MBeanRegistration) requestHandler;
            mbeanRegistration.postRegister(registrationDone);
        }

	}
	
	/**
	 * {@inheritDoc}
	 */
	public void preDeregister() throws Exception {
	    if (requestHandler instanceof MBeanRegistration) {
            MBeanRegistration mbeanRegistration = (MBeanRegistration) requestHandler;
            mbeanRegistration.preDeregister();
	    }
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void postDeregister() {
	    if (requestHandler instanceof MBeanRegistration) {
            MBeanRegistration mbeanRegistration = (MBeanRegistration) requestHandler;
            mbeanRegistration.postDeregister();
	    }
	}
	

	/**
	 * sets the message receive timeout
	 * 
	 * @param requesttimeout the message receive timeout
	 */
	public void setRequestTimeoutMillis(long requesttimeout) {
		if (requesttimeout < HttpServerConnection.MIN_REQUEST_TIMEOUT_MILLIS) {
			LOG.warning("try to set request timeout with " + requesttimeout + ". This will be ignored because the value is smaller than the min request timout " + HttpServerConnection.MIN_REQUEST_TIMEOUT_MILLIS + " millis");
			return;
		}		

		this.requestTimeoutMillis = requesttimeout;
	}

	/**
	 * sets the autocompress threshold of responses
	 * 
	 * @param autocompressThresholdBytes the autocompress threshold
	 */
	public void setAutoCompressThresholdBytes(int autocompressThresholdBytes) {
		this.autocompressThresholdBytes = autocompressThresholdBytes;
	}
	
	/**
	 * gets the autocompress threshold of responses
	 * 
	 * @return the autocompress threshold
	 */
	public int getAutoCompressThresholdBytes() {
		return autocompressThresholdBytes;
	}

	
	
	/**
	 * set if the request will be uncompressed (if compressed)
	 *  
	 * @param isAutoUncompress true, if the request will be uncompressed (if compressed)
	 */
	public void setAutoUncompress(boolean isAutoUncompress) {
		this.isAutoUncompress = isAutoUncompress;
	}

	
	/**
	 * return true, if the request will be uncompressed (if compressed)
	 * 
	 * @return true, if the request will be uncompressed (if compressed)
	 */
	public boolean isAutoUncompress() {
		return isAutoUncompress;
	}
	   
	
	
	/**
     * gets the message receive timeout
     * 
     * @return the message receive timeout
     */
    public long getRequestTimeoutMillis() {
        return requestTimeoutMillis;
    }
	
	
	/**
	 * set the body data receive timeout
	 * 
	 * @param bodyDataReceiveTimeoutSec the timeout
	 */
	public void setBodyDataReceiveTimeoutMillis(long bodyDataReceiveTimeoutMillis) {
		this.bodyDataReceiveTimeoutMillis = bodyDataReceiveTimeoutMillis;
	}

	
	/**
	 * get the body data receive timeout
	 * 
	 * @return the timeout
	 */
	public long getBodyDataReceiveTimeoutMillis() {
		return bodyDataReceiveTimeoutMillis;
	}



	
	
	/**
	 * set is if the server-side connection will closed, if an error message is sent
	 * 
	 * @param isCloseOnSendingError if the connection will closed, if an error message is sent
	 */
	public void setCloseOnSendingError(boolean isCloseOnSendingError) {
		this.isCloseOnSendingError = isCloseOnSendingError;
	}
	
	
	/**
	 * returns if the server-side connection will closed, if an error message will be sent
	 * @return true, if the connection will closed by sending an error message 
	 */
	public boolean isCloseOnSendingError() {
		return isCloseOnSendingError;
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
     * get the max transactions per connection. Setting this filed causes that 
     * a keep-alive response header will be added 
     * 
     * @return the max transactions 
     */
    public int getMaxTransactions() {
        return maxTransactions;
    }
	
	
	/**
	 * sets the session max inactive interval in seconds
	 *  
	 * @param sessionMaxInactiveIntervalSec the session max inactive interval in seconds
	 */
	public void setSessionMaxInactiveIntervalSec(int sessionMaxInactiveIntervalSec) {
	    this.sessionMaxInactiveIntervalSec = sessionMaxInactiveIntervalSec;
	}
	
	
	/**
	 * gets the session max inactive interval in sconds
	 * 
	 * @return the session max inactive interval in sconds
	 */
	public int getSessionMaxInactiveIntervalSec() {
	    return sessionMaxInactiveIntervalSec;
	}
	

	/**
     * adds a connection handler 
     * 
     * @param connectionHandler  the connection handler to add
     */
    public void addConnectionHandler(IHttpConnectionHandler connectionHandler) {
    	connectionHandlers.add(connectionHandler);
    }





    /**
	 * returns the implementation version string 
	 * 
	 * @return the implementation version string
	 */
	public String getImplementationVersion() {
		return HttpUtils.getImplementationVersion();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void onInit() {
		server.setStartUpLogMessage("xLightweb " + HttpUtils.getImplementationVersion() + "/" + server.getStartUpLogMessage());

		ServerUtils.injectServerField(requestHandler, server);
		ServerUtils.injectProtocolAdapter(requestHandler, this);
		
		if (requestHandler instanceof ILifeCycle) {
			((ILifeCycle) requestHandler).onInit();
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void onDestroy() throws IOException {
		
		if (requestHandler instanceof ILifeCycle) {
			try {
				((ILifeCycle) requestHandler).onDestroy();
			} catch (IOException ioe) {
				if (LOG.isLoggable(Level.FINE)) {
					LOG.fine("exception occured by destroying " + requestHandler + " " + ioe.toString());
				}
			}
		}
		
		sessionManager.close();

		connectionHandlers.clear();
		server = null;
		sessionManager = null;
		requestHandler = null;
		requestHandlerAdapter = null;
	}
	
	
	
	
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public boolean onConnect(INonBlockingConnection connection) throws IOException {
		
	    numHandledConnections++;
		HttpServerConnection httpCon = new HttpServerConnection(defaultEncoding, sessionManager, transactionMonitor, sessionMaxInactiveIntervalSec, connection, requestHandlerAdapter, upgradeHandler, isCloseOnSendingError, connectionHandlers, useCookies, autocompressThresholdBytes, isAutoUncompress);
		
		if (maxTransactions != null) {
			httpCon.setMaxTransactions(maxTransactions);
		}
		
		if (requestTimeoutMillis != null) {
			httpCon.setRequestTimeoutMillis(requestTimeoutMillis);
		}

		if (bodyDataReceiveTimeoutMillis != null) {
			httpCon.setBodyDataReceiveTimeoutMillis(bodyDataReceiveTimeoutMillis);
		}
		
		return true;
	}
	
	
	@Execution(Execution.NONTHREADED)
	private static final class NullRequestHandler implements IHttpRequestHandler {
		public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
			// do nothing
		}
	}
	
	
    public static interface SessionManagerInfoMBean {
        
        Integer getNumCreatedSessions();
        
        Integer getNumRemovedSessions();
        
        Integer getNumExpiredSessions();
        
        String[] getSessionsInfo();
        
        int getNumSessions();
        
        Integer getTotalSessionStoreSize();
        
        boolean isShowDetailedInfo();
        
        void setShowDetailedInfo(boolean isShowDetailedInfo);
    }
    
    
    private static final class SessionManagerInfo implements SessionManagerInfoMBean {

        private final ISessionManager sessionManager;
        
        private boolean isShowDetailedInfo = false; 
        
       
        public SessionManagerInfo(ISessionManager sessionManager) {
            this.sessionManager = sessionManager;
        }
       
        
        public Integer getNumCreatedSessions() {
            if (sessionManager instanceof SessionManager) {
                return ((SessionManager) sessionManager).getNumCreatedSessions();
            } else {
                return null;
            }
        }
        
        public Integer getNumExpiredSessions() {
            if (sessionManager instanceof SessionManager) {
                return ((SessionManager) sessionManager).getNumExpiredSessions();
            } else {
                return null;
            }
        }
        
        public Integer getNumRemovedSessions() {
            if (sessionManager instanceof SessionManager) {
                return ((SessionManager) sessionManager).getNumExpiredSessions();
            } else {
                return null;
            }
        }
        
        
        
        public boolean isShowDetailedInfo() {
            return isShowDetailedInfo;
        }
        
        public void setShowDetailedInfo(boolean isShowDetailedInfo) {
            this.isShowDetailedInfo = isShowDetailedInfo; 
        }
        
        public int getNumSessions() {
            return sessionManager.getSessionMap().size();
        }
        
        
        public String[] getSessionsInfo() {
            List<String> result = new ArrayList<String>();

            if (isShowDetailedInfo) {
                for (HttpSession session : sessionManager.getSessionMap().values()) {
                    result.add("[" + session.getId() + "; serSize=" + DataConverter.toFormatedBytesSize(determineSerializedSize(session)) + "] " + session.toString());
                }
            }
                
            return result.toArray(new String[result.size()]); 
        }
        
        
        public Integer getTotalSessionStoreSize() {
            
            if (isShowDetailedInfo) {
                int size = 0;
                for (HttpSession session : sessionManager.getSessionMap().values()) {
                    size += determineSerializedSize(session);
                }
                
                return size;
    
            } else {
                return null;
            }
        }
        
        private int determineSerializedSize(Serializable object) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(object);
                oos.close();
                
                return bos.toByteArray().length;
            } catch (IOException ioe) {
                return -1;
            }
        }        
    }	
}