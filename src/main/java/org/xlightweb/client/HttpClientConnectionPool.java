/*
 *  Copyright (c) xlightweb.org, 2008 - 2009. All rights reserved.
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
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;


import org.xlightweb.IHttpConnection;
import org.xlightweb.IHttpRequestHeader;
import org.xsocket.ILifeCycle;
import org.xsocket.connection.IConnectionPool;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.NonBlockingConnectionPool;





/**
 * @deprecated
 */
final class HttpClientConnectionPool implements IConnectionPool {
	
	private static final Logger LOG = Logger.getLogger(HttpClientConnectionPool.class.getName());
	
	
	public static final int DEFAULT_PERSISTENT_CONNECTION_TIMEOUT = 10 * 1000;  // 10 sec
	

	
	private final NonBlockingConnectionPool pool;
	
	// timeouts
	private final AtomicLong responseTimeoutMillis = new AtomicLong(IHttpConnection.DEFAULT_RESPONSE_TIMEOUT_MILLIS);
	private final AtomicLong bodyDataReceiveTimeoutMillis = new AtomicLong(IHttpConnection.DEFAULT_DATA_RESPONSE_TIMEOUT_MILLIS);
	
	
	// auto close support 
	private final AtomicBoolean isAutoCloseAfterResponse = new AtomicBoolean(true);
	
	
	// monitor
	private TransactionMonitor transactionMonitor = null;


	/**
	 * constructor
	 */
	public HttpClientConnectionPool() {
		this(null);
	}

	
	
	/**
	 * constructor
	 *
	 * @param sslContext   the ssl context or <code>null</code> if ssl should not be used
	 */
	public HttpClientConnectionPool(SSLContext sslContext) {
	    
	    // autoload SSL context if not set (works only with Java 1.6 or higher)
	    if (sslContext == null) {
	        try {
	            Method m = SSLContext.class.getMethod("getDefault");
	            sslContext = (SSLContext) m.invoke(SSLContext.class);
	            
	            if (LOG.isLoggable(Level.FINE)) {
	                LOG.fine("default SSLContext -> SSLContext.getDefault() is loaded automatically");
	            }
	        } catch (Exception ignore) { }
	    }
	    
		pool = new NonBlockingConnectionPool(sslContext);
		pool.setPooledMaxIdleTimeMillis(DEFAULT_PERSISTENT_CONNECTION_TIMEOUT); 
	}

	
	
	/**
	 * return the underyling connection pool
	 * 
	 * @return the pool
	 */
	NonBlockingConnectionPool getUnderlyingConnectionPool() {
	    return pool;
	}
	    

	
	/**
	 * {@inheritDoc}
	 */
	public boolean isOpen() {
		return pool.isOpen();
	}

	
	/**
     * get a pool connection for the given http request. If no free connection is in the pool,
     * a new one will be created 
     *
     * @param request                  the request
     * @return the connection
     * @throws SocketTimeoutException if the wait timeout has been reached (this will only been thrown if wait time has been set)
     * @throws IOException  if an exception occurs
     */
    public HttpClientConnection getHttpClientConnection(IHttpRequestHeader header) throws IOException, SocketTimeoutException {
        URL url = header.getRequestUrl();
        String host = url.getHost();
        int port = url.getPort();
        boolean isSSL = url.getProtocol().equalsIgnoreCase("HTTPS");
        
        return newHttpClientConnection(pool.getNonBlockingConnection(host, normalizePort(port, isSSL), IHttpConnection.DEFAULT_CONNECT_TIMEOUT_MILLIS, isSSL));
    }
	
	
	
	/**
     * get a pool connection for the given http request. If no free connection is in the pool,
     * a new one will be created 
     *
     * @param request                  the request
     * @param connectTimeoutMillis     the connected timeout
     * @return the connection
     * @throws SocketTimeoutException if the wait timeout has been reached (this will only been thrown if wait time has been set)
     * @throws IOException  if an exception occurs
     */
    public HttpClientConnection getHttpClientConnection(IHttpRequestHeader header, int connectTimeoutMillis) throws IOException, SocketTimeoutException {
        URL url = header.getRequestUrl();
        String host = url.getHost();
        int port = url.getPort();
        boolean isSSL = url.getProtocol().equalsIgnoreCase("HTTPS");
        
        return newHttpClientConnection(pool.getNonBlockingConnection(host, normalizePort(port, isSSL), connectTimeoutMillis, isSSL));
    }
	

	/**
	 * get a pool connection for the given address. If no free connection is in the pool,
	 * a new one will be created 
	 *
	 * @param host   the server address
	 * @param port   the server port
	 * @return the connection
	 * @throws SocketTimeoutException if the wait timeout has been reached (this will only been thrown if wait time has been set)
	 * @throws IOException  if an exception occurs
	 */
	public HttpClientConnection getHttpClientConnection(String host, int port) throws IOException, SocketTimeoutException {
		return newHttpClientConnection(pool.getNonBlockingConnection(host, normalizePort(port, false), IHttpConnection.DEFAULT_CONNECT_TIMEOUT_MILLIS, false));
	}

	/**
	 * get a pool connection for the given address. If no free connection is in the pool,
	 * a new one will be created 
	 *
	 * @param host   the server address
	 * @param port   the server port
	 * @param isSSL  true, if ssl connection       
	 * @return the connection
	 * @throws SocketTimeoutException if the wait timeout has been reached (this will only been thrown if wait time has been set)
	 * @throws IOException  if an exception occurs
	 */
	public HttpClientConnection getHttpClientConnection(String host, int port, boolean isSSL) throws IOException, SocketTimeoutException {
		return newHttpClientConnection(pool.getNonBlockingConnection(host, normalizePort(port, isSSL), IHttpConnection.DEFAULT_CONNECT_TIMEOUT_MILLIS, isSSL));
	}

	
	/**
	 * get a pool connection for the given address. If no free connection is in the pool,
	 * a new one will be created 
	 * 
	 * @param host                   the server address
	 * @param port                   the server port
	 * @param connectTimeoutMillis   the connection timeout
	 * @return the connection
	 * @throws SocketTimeoutException if the wait timeout has been reached (this will only been thrown if wait time has been set)
	 * @throws IOException  if an exception occurs
	 */
	public HttpClientConnection getHttpClientConnection(String host, int port, int connectTimeoutMillis) throws IOException, SocketTimeoutException {
		return newHttpClientConnection(pool.getNonBlockingConnection(host, normalizePort(port, false), connectTimeoutMillis, false));
	}
	
	/**
	 * get a pool connection for the given address. If no free connection is in the pool,
	 * a new one will be created 
	 * 
	 * @param host                   the server address
	 * @param port                   the server port
	 * @param connectTimeoutMillis   the connection timeout
	 * @param isSSL                  true, if ssl connection      
	 * @return the connection
	 * @throws SocketTimeoutException if the wait timeout has been reached (this will only been thrown if wait time has been set)
	 * @throws IOException  if an exception occurs
	 */
	public HttpClientConnection getHttpClientConnection(String host, int port, int connectTimeoutMillis, boolean isSSL) throws IOException, SocketTimeoutException {
		return newHttpClientConnection(pool.getNonBlockingConnection(host, normalizePort(port, isSSL), connectTimeoutMillis, isSSL));
	}

	/**
	 * get a pool connection for the given address. If no free connection is in the pool,
	 *  a new one will be created 
	 *
	 * @param address the server address
	 * @param port    the server port
	 * @return the connection
	 * @throws SocketTimeoutException if the wait timeout has been reached (this will only been thrown if wait time has been set)
	 * @throws IOException  if an exception occurs
	 */
	public HttpClientConnection getHttpClientConnection(InetAddress address, int port) throws IOException, SocketTimeoutException {
		return newHttpClientConnection(pool.getNonBlockingConnection(address, normalizePort(port, false), IHttpConnection.DEFAULT_CONNECT_TIMEOUT_MILLIS, false));
	}

	/**
	 * get a pool connection for the given address. If no free connection is in the pool,
	 *  a new one will be created 
	 *
	 * @param address  the server address
	 * @param port     the server port
	 * @param isSSL    true, if ssl connection    
	 * @return the connection
	 * @throws SocketTimeoutException if the wait timeout has been reached (this will only been thrown if wait time has been set)
	 * @throws IOException  if an exception occurs
	 */
	public HttpClientConnection getHttpClientConnection(InetAddress address, int port, boolean isSSL) throws IOException, SocketTimeoutException {
		return newHttpClientConnection(pool.getNonBlockingConnection(address, normalizePort(port, isSSL), IHttpConnection.DEFAULT_CONNECT_TIMEOUT_MILLIS, isSSL));
	}
	
	
	/**
	 * get a pool connection for the given address. If no free connection is in the pool,
	 * a new one will be created 
	 * 
	 * @param address                the server address
	 * @param port                   the server port
	 * @param connectTimeoutMillis   the connection timeout
	 * @return the connection
	 * @throws SocketTimeoutException if the wait timeout has been reached (this will only been thrown if wait time has been set)
	 * @throws IOException  if an exception occurs
	 */
	public HttpClientConnection getHttpClientConnection(InetAddress address, int port, int connectTimeoutMillis) throws IOException, SocketTimeoutException {
		return newHttpClientConnection(pool.getNonBlockingConnection(address, normalizePort(port, false), connectTimeoutMillis, false));
	}
	
	/**
	 * get a pool connection for the given address. If no free connection is in the pool,
	 * a new one will be created 
	 * 
	 * @param address                the server address
	 * @param port                   the server port
	 * @param connectTimeoutMillis   the connection timeout
	 * @param isSSL                  true, if ssl connection   
	 * @return the connection
	 * @throws SocketTimeoutException if the wait timeout has been reached (this will only been thrown if wait time has been set)
	 * @throws IOException  if an exception occurs
	 */
	public HttpClientConnection getHttpClientConnection(InetAddress address, int port, int connectTimeoutMillis, boolean isSSL) throws IOException, SocketTimeoutException {
		return newHttpClientConnection(pool.getNonBlockingConnection(address, normalizePort(port, isSSL), connectTimeoutMillis, isSSL));
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
	
	
	private HttpClientConnection newHttpClientConnection(INonBlockingConnection con) throws IOException {
		HttpClientConnection httpConnection = new HttpClientConnection(con);
		
		httpConnection.setResponseTimeoutMillis(responseTimeoutMillis.get());
		httpConnection.setBodyDataReceiveTimeoutMillis(bodyDataReceiveTimeoutMillis.get());
		httpConnection.setAutocloseAfterResponse(isAutoCloseAfterResponse.get());
		
		if (transactionMonitor != null) {
			httpConnection.setTransactionMonitor(transactionMonitor);
		}

		return httpConnection;
	}
	
	
	void setTranactionMonitor(TransactionMonitor transactionMonitor) {
		this.transactionMonitor = transactionMonitor;
	}
	
	
	void setWorkerpool(Executor workerpool) {
		pool.setWorkerpool(workerpool);
	}
	
	Executor getWorkerpool() {
		return pool.getWorkerpool();
	}
	
	/**
	 * set the response timeout
	 * 
	 *  @param responseTimeoutMillis the response timeout
	 */
	public void setResponseTimeoutMillis(long responseTimeoutMillis) {
		if (responseTimeoutMillis < 0) {
			LOG.warning("try to set response timeout with " + responseTimeoutMillis + ". This will be ignored");
			return;
		}
		this.responseTimeoutMillis.set(responseTimeoutMillis);
	}
	
	
	/**
	 * get the response timeout
	 * 
	 * @return the response timeout
	 */
	public long getResponseTimeoutMillis() {
		return responseTimeoutMillis.get();
	}

	
	/**
	 * set the bodydata receive timeout
	 * 
	 * @param bodyDataReceiveTimeoutMillis the bodydata receive timeout
	 */
	public void setBodyDataReceiveTimeoutMillis(long bodyDataReceiveTimeoutMillis) {
		if (responseTimeoutMillis.get() < 0) {
			LOG.warning("try to set bodydata receive timeout with " + responseTimeoutMillis + ". This will be ignored");
			return;
		}
		this.bodyDataReceiveTimeoutMillis.set(bodyDataReceiveTimeoutMillis);
	}

	
	/**
	 * get the bodydata receive timeout
	 * 
	 * @return the bodydata receive timeout
	 */
	public long getBodyDataReceiveTimeoutMillis() {
		return bodyDataReceiveTimeoutMillis.get();
	}
	
	
	
	/**
	 * set if connection should be auto closed after response
	 *  
	 * @param isAutoCloseAfterResponse true, if connection should be auto closed after response 
	 */
	public void setAutocloseAfterResponse(boolean isAutoCloseAfterResponse) {
		this.isAutoCloseAfterResponse.set(isAutoCloseAfterResponse);
	}
	
	
	
	/**
	 * true, if connection should be auto closed after response
	 * 
	 * @return true, if connection should be auto closed after response
	 */
	public boolean isAutoCloseAfterResponse() {
		return isAutoCloseAfterResponse.get();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void addListener(ILifeCycle listener) {
		pool.addListener(listener);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public boolean removeListener(ILifeCycle listener) {
		return pool.removeListener(listener);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public int getPooledMaxIdleTimeMillis() {
		return pool.getPooledMaxIdleTimeMillis();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void setPooledMaxIdleTimeMillis(int idleTimeoutMillis) {
		pool.setPooledMaxIdleTimeMillis(idleTimeoutMillis);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public int getPooledMaxLifeTimeMillis() {
		return pool.getPooledMaxLifeTimeMillis();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void setPooledMaxLifeTimeMillis(int lifeTimeoutMillis) {
		pool.setPooledMaxLifeTimeMillis(lifeTimeoutMillis);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public int getMaxActive() {
		return pool.getMaxActive();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void setMaxActive(int maxActive) {
		pool.setMaxActive(maxActive);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public int getMaxIdle() {
		return pool.getMaxIdle();
	}
	

  
	
	/**
	 * {@inheritDoc}
	 */
	public void setMaxIdle(int maxIdle) {
		pool.setMaxIdle(maxIdle);
	}
	
	
	/**
     * {@inheritDoc}
     */
	public int getMaxActivePerServer() {
	    return pool.getMaxActivePerServer();
	}
	
	
    /**
     * {@inheritDoc}
     */	
	public void setMaxActivePerServer(int maxActivePerServer) {
	    pool.setMaxActivePerServer(maxActivePerServer);
	}

	

    
    /**
     * @deprecated
     */
    public int getMaxWaiting() {
        return 0;
    }
    


    /**
     * @deprecated 
     */
    public boolean isPooled() {
        return false;
    }
    
    
    /**
     * @deprecated 
     */
    public void setPooled(boolean isPooled) {
        if (!isPooled) {
            LOG.warning("isPooled is deprecated and will be ignored");
        }
    }
    
    
       /**
     * @deprecated
     */
    public void setMaxWaiting(int maxWaiting) {
        LOG.warning("maxWaiting is deprecated and will be ignored");
    }

    
       
    /**
     * @deprecated
     */
    public long getCreationMaxWaitMillis() {
        return 0;
    }
    
    
    
    /**
     * @deprecated
     */
    public void setCreationMaxWaitMillis(long maxWaitMillis) {
        LOG.warning("creationMaxWaitMillis is deprecated and will be ignored");
    }
    
    
	
	
	/**
	 * {@inheritDoc}
	 */
	public int getNumActive() {
		return pool.getNumActive();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public int getNumIdle() {
		return pool.getNumIdle();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<String> getActiveConnectionInfos() {
		return pool.getActiveConnectionInfos();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<String> getIdleConnectionInfos() {
		return pool.getIdleConnectionInfos();
	}
	
	/**
	 * {@inheritDoc}
	 */
    public int getNumCreated() {
    	return pool.getNumCreated();
    }
    
    /**
	 * get the number of the creation errors 
	 * 
	 * @return the number of creation errors 
	 */
    public int getNumCreationError() {
    	return pool.getNumCreationError();
    }
    
    /**
	 * {@inheritDoc}
	 */
    public int getNumDestroyed() {
    	return pool.getNumDestroyed();
    }
    
  
    
    /**
	 * {@inheritDoc}
	 */
    public int getNumTimeoutPooledMaxIdleTime() {
    	return pool.getNumTimeoutPooledMaxIdleTime();
    }

    /**
	 * {@inheritDoc}
	 */
    public int getNumTimeoutPooledMaxLifeTime() {
    	return pool.getNumTimeoutPooledMaxLifeTime();
    }
    

	
	
	/**
	 * get the current number of pending get operations to retrieve a resource
	 * 
	 * @return the current number of pending get operations
	 */
	public int getNumPendingGet() {
		return pool.getNumPendingGet();
	}
	
	public void close() {
		pool.close();
	}
	
	public void destroy(HttpClientConnection httpConnection) throws IOException {
		NonBlockingConnectionPool.destroy(httpConnection.getUnderlyingTcpConnection());
	}
	
	 
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return pool.toString();
	}
}
