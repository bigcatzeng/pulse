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
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.xlightweb.BodyDataSink;
import org.xlightweb.CacheHandler;
import org.xlightweb.EventDataSource;
import org.xlightweb.FutureResponseHandler;
import org.xlightweb.HttpRequest;
import org.xlightweb.HttpUtils;
import org.xlightweb.IEventDataSource;
import org.xlightweb.IEventHandler;
import org.xlightweb.IFutureResponse;
import org.xlightweb.IHttpConnectHandler;
import org.xlightweb.IHttpConnection;
import org.xlightweb.IHttpMessage;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpRequestHeader;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.IWebSocketConnection;
import org.xlightweb.IWebSocketHandler;
import org.xlightweb.InvokeOn;
import org.xlightweb.RequestHandlerChain;
import org.xlightweb.WebSocketConnection;
import org.xlightweb.client.HttpClientConnection.ClientExchange;
import org.xlightweb.client.TransactionMonitor.Transaction;
import org.xlightweb.client.TransactionMonitor.TransactionLog;
import org.xsocket.ILifeCycle;
import org.xsocket.connection.IConnectionPool;
import org.xsocket.connection.NonBlockingConnectionPool;
import org.xsocket.connection.IConnection.FlushMode;



/**
 * Higher level client-side abstraction of the client side endpoint. Internally, the HttpClient uses a pool 
 * of {@link HttpClientConnection} to perform the requests.  Example:
 * 
 * <pre>
 *   HttpClient httpClient = new HttpClient();
 *   
 *   // set some properties
 *   httpClient.setFollowsRedirect(true);  
 *   httpClient.setAutoHandleCookies(false);
 *   // ...
 *   
 *   // perform a synchronous call 
 *   IHttpResponse response = httpClient.call(new GetRequest("http://www.gmx.com/index.html"));
 *   System.out.println(response.getStatus());
 *   
 *   BlockingBodyDataSource bodyChannel = response.getBlockingBody();
 *   System.out.println(bodyChannel.readString());
 *   
 *   
 *   // perform an asynchronous request
 *   MyResponseHandler respHdl = new MyResponseHandler();
 *   httpClient.send(new HttpRequestHeader("GET", "http://www.gmx.com/index.html"), respHdl);
 *   
 *   //..
 *   
 *   httpClient.close(); 
 * </pre>  
 *
 * The HttpClient is thread-safe
 *
 * @author grro@xlightweb.org
 */
public class HttpClient implements IHttpClientEndpoint, IConnectionPool, Closeable {
	
	private static final Logger LOG = Logger.getLogger(HttpClient.class.getName());
	
	public static enum FollowsRedirectMode { OFF, RFC, ALL }
	
    public static final int DEFAULT_CREATION_MAX_WAIT_TIMEOUT = 60 * 1000;
	public static final int DEFAULT_POOLED_LIFE_TIMEOUT_MILLIS = 30 * 1000;
	public static final int DEFAULT_POOLED_IDLE_TIMEOUT_MILLIS = 3 * 1000;

	
	public static final boolean DEFAULT_TREAT_302_REDIRECT_AS_303 = false;
	public static final boolean DEFAULT_AUTOCONFIRM_REDIRECT = false;
	public static final Long DEFAULT_RESPONSE_TIMEOUT_SEC = IHttpConnection.DEFAULT_RESPONSE_TIMEOUT_MILLIS;
	
	private final AtomicBoolean isTreat302RedirectAs303 = new AtomicBoolean(DEFAULT_TREAT_302_REDIRECT_AS_303);
	private final AtomicInteger connectTimeoutMillis = new AtomicInteger(IHttpConnection.DEFAULT_CONNECT_TIMEOUT_MILLIS);

	private String defaultEncoding = IHttpMessage.DEFAULT_ENCODING;
	

	private static final int DEFAULT_PERSISTENT_CONNECTION_TIMEOUT = 10 * 1000;  // 10 sec
	private final AtomicLong responseTimeoutMillis = new AtomicLong(IHttpConnection.DEFAULT_RESPONSE_TIMEOUT_MILLIS);
	private final AtomicLong bodyDataReceiveTimeoutMillis = new AtomicLong(IHttpConnection.DEFAULT_DATA_RESPONSE_TIMEOUT_MILLIS);
    private static final boolean DEDFAULT_AUTO_UNCOMPRESS = Boolean.parseBoolean(System.getProperty("org.xlightweb.client.response.isAutouncompressActivated", "true"));
    private final AtomicBoolean isAutoUncompress = new AtomicBoolean(DEDFAULT_AUTO_UNCOMPRESS);
	
	   // auto close support 
    private final AtomicBoolean isAutoCloseAfterResponse = new AtomicBoolean(true);
    
    
    
	
	// pool
	private final NonBlockingConnectionPool pool;

	private final AtomicBoolean isCallReturnOnMessage = new AtomicBoolean(false);

	
    // auto supported handlers
	public static final int DEFAULT_MAX_REDIRECTS = Integer.parseInt(System.getProperty("org.xlightweb.client.maxRedirects", "5"));
    public static final boolean DEFAULT_FOLLOWS_REDIRECT = Boolean.parseBoolean(System.getProperty("org.xlightweb.client.autoredirect", "false"));
    public static final FollowsRedirectMode DEFAULT_FOLLOWS_REDIRECTMODE = FollowsRedirectMode.valueOf(System.getProperty("org.xlightweb.client.autoredirect", FollowsRedirectMode.OFF.toString()));
    private final AtomicReference<FollowsRedirectMode> followsRedirectModeRef = new AtomicReference<FollowsRedirectMode>(DEFAULT_FOLLOWS_REDIRECTMODE);
	private final AtomicInteger maxRedirects = new AtomicInteger(DEFAULT_MAX_REDIRECTS);
    private final RedirectHandler redirectHandler;

    
    public static final int DEFAULT_MAX_RETRIES = Integer.parseInt(System.getProperty("org.xlightweb.client.maxRetries", "4"));
    private final AtomicInteger maxRetries = new AtomicInteger(DEFAULT_MAX_RETRIES);
    private final RetryHandler retryHandler;

    
    public static final boolean DEFAULT_AUTOHANDLING_COOKIES = true;
    private final AtomicBoolean isAutohandlingCookies = new AtomicBoolean(DEFAULT_AUTOHANDLING_COOKIES);
    private final CookieHandler cookiesHandler;

    public static final boolean DEFAULT_PROXY_ACTIVATED = false;
    private final AtomicBoolean isProxyActivated = new AtomicBoolean(DEFAULT_PROXY_ACTIVATED);
    private final ProxyHandler proxyHandler;

    
    public static final int DEFAULT_CACHE_SIZE = 0;
    private final CacheHandler cacheHandler;
    private boolean isShowCache = false;
    
    private final RequestHandlerChain chain = new RequestHandlerChain();

	
	// the assigned session manager 
	private SessionManager sessionManager = null;
	

	// statistics
	private long lastTimeRequestSentMillis = System.currentTimeMillis();

	
	// transaction monitor
	private TransactionMonitor transactionMonitor = null;
	private final TransactionLog transactionLog = new TransactionLog(0);

	
	
	/**
	 * constructor 
	 */
	public HttpClient() {
		this(null, new IHttpRequestHandler[0]);
	}
	
	
	
	/**
	 * constructor 
	 * 
	 * @param interceptors  interceptor
	 */
	public HttpClient(IHttpRequestHandler... interceptors) {
		this(null, interceptors);
	}
	
	
	
	/**
	 * constructor 
	 * 
	 * @param sslCtx   the ssl context to use
	 */
	public HttpClient(SSLContext sslCtx) {
		this(sslCtx, new IHttpRequestHandler[0]);
	}

	
	/**
	 * constructor 
	 * 
	 * @param sslCtx        the ssl context to use
	 * @param interceptors  the interceptors
	 */
	public HttpClient(SSLContext sslCtx, IHttpRequestHandler... interceptors) {
	    
	    
	    pool = new NonBlockingConnectionPool(sslCtx);
	    pool.setPooledMaxIdleTimeMillis(DEFAULT_PERSISTENT_CONNECTION_TIMEOUT);
	    
		setPooledMaxIdleTimeMillis(DEFAULT_POOLED_IDLE_TIMEOUT_MILLIS);
		setPooledMaxLifeTimeMillis(DEFAULT_POOLED_LIFE_TIMEOUT_MILLIS);
		
		sessionManager = new SessionManager();
		
		
		retryHandler = new RetryHandler(HttpClient.this);
		cacheHandler = new CacheHandler(DEFAULT_CACHE_SIZE);
		redirectHandler = new RedirectHandler(this);
		cookiesHandler = new CookieHandler();
		proxyHandler = new ProxyHandler(this);
		
		
		for (IHttpRequestHandler interceptor : interceptors) {
		    addInterceptor(interceptor);
		}
		
		resetChain();
		chain.onInit();
	}


	NonBlockingConnectionPool getUnderlyingConnectionPool() {
	    return pool;
	}
	
	
	
	/**
	 * adds an interceptor. Example:
	 * 
	 * <pre>
	 *  HttpClient httpClient = new HttpClient();
	 *  
	 *  LoadBalancerRequestInterceptor lbInterceptor = new LoadBalancerRequestInterceptor();
	 *  lbInterceptor.addVirtualServer("http://customerService", "srv1:8030", "srv2:8030");
	 *  httpClient.addInterceptor(lbInterceptor);
	 *  
	 *  // ...
	 *  GetRequest request = new GetRequest("http://customerService/price?id=2336&amount=5656");
	 *  IHttpResponse response = httpClient.call(request);
	 *  //...
	 * 
	 * </pre>
	 *  
	 * @param interceptor  the interceptor to add
	 */
	public void addInterceptor(IHttpRequestHandler interceptor) {
	    if (interceptor instanceof ILifeCycle) {
	        ((ILifeCycle) interceptor).onInit();
	    }
	    
        if (!HttpUtils.isConnectHandlerWarningIsSuppressed() && (interceptor instanceof IHttpConnectHandler)) {
            LOG.warning("only IHttpRequestHandler is supported. The onConnect(...) method will not be called. (suppress this warning by setting system property org.xlightweb.httpConnectHandler.suppresswarning=true)");
        }
	    
	    chain.addLast(interceptor);
	    resetChain();
	}
	
	
	private void resetChain() {
	    synchronized (this) {
    	    chain.remove(retryHandler);
    	    chain.remove(cacheHandler);
    	    chain.remove(cookiesHandler);
    	    chain.remove(redirectHandler);
    	    chain.remove(proxyHandler);
    
    	    if (getMaxRetries() > 0) {
    	        chain.addFirst(retryHandler);
    	    }
    	    
    	    if (cacheHandler.getMaxCacheSizeKB() > 0) {
    	        
    	        for (IHttpRequestHandler hdl : chain.getHandlers()) {
    	            if (hdl instanceof CacheHandler) {
    	                LOG.warning("a cache handler is already activated. Adding another one (setting HttpClient's cacheMaxSizeKB larger than zero adds a cache handler automatically)");
    	            }
    	        }
    	        
    	        chain.addFirst(cacheHandler);
    	    }
    	    
    	    if (followsRedirectModeRef.get() != FollowsRedirectMode.OFF) {
    	        chain.addFirst(redirectHandler);
    	    }
    	     
    	    if (isAutohandlingCookies.get()) {
    	        chain.addFirst(cookiesHandler);  
    	    }
    	     
    	    if (isProxyActivated.get()) {
    	        chain.addLast(proxyHandler);
    	    }	 
	    }
	    
	    // Handler Chain order;
	    // cookiesHandler -> redirectHandler -> cacheHandler -> retryHandler -> customHandlers[] -> proxyHandler 
	}

	

	/**
	 * @deprecated use {@link HttpClient#setFollowsRedirectMode(FollowsRedirectMode.ALL))} instead. 
	 */
	public void setFollowsRedirect(boolean isFollowsRedirect) {
	    if (isFollowsRedirect) {
	        setFollowsRedirectMode(FollowsRedirectMode.ALL);
	    } else {
	        setFollowsRedirectMode(FollowsRedirectMode.OFF);
	    }
	}
	
	
	/**
     * @deprecated
     */
    public boolean getFollowsRedirect() {
        return (getFollowsRedirectMode() == FollowsRedirectMode.ALL);
    }

    
    /**
     * sets if redirects should be handled automatically. 
     * 
     * <ul>
     *  <li>OFF: redirects will <b>not</b> be handled automatically (default)<li>
     *  <li>RFC: redirects will be handled automatically only if no user interaction according to RFC 2616 is required<li>
     *  <li>ALL: redirects will be handled automatically for all redirect responses<li>
     * </ul>
     * 
     * @param mode
     */
    public void setFollowsRedirectMode(String mode) {
        setFollowsRedirectMode(FollowsRedirectMode.valueOf(mode));
    }

	/**
	 * sets if redirects should be handled automatically. 
	 * 
	 * <ul>
	 *  <li>OFF: redirects will <b>not</b> be handled automatically (default)<li>
	 *  <li>RFC: redirects will be handled automatically only if no user interaction according to RFC 2616 is required<li>
	 *  <li>ALL: redirects will be handled automatically for all redirect responses<li>
	 * </ul>
	 * 
	 * @param mode
	 */
    public void setFollowsRedirectMode(FollowsRedirectMode mode) {
        if (followsRedirectModeRef.get() == mode) {
            return;
        }
        
        followsRedirectModeRef.set(mode);
        resetChain();
    }
    

    /**
     * returns the follow redirect mode
     * 
     * @return  the follow redirect mode
     */
    public FollowsRedirectMode getFollowsRedirectMode() {
        return followsRedirectModeRef.get();
    }
    
	
	/**
	 * sets if cookies should be auto handled 
	 * 
	 * @param isAutohandlingCookies true, if cookies should be auto handled
	 */
	public void setAutoHandleCookies(boolean isAutohandlingCookies) {
	    if (this.isAutohandlingCookies.get() == isAutohandlingCookies) {
            return;
        }
        this.isAutohandlingCookies.set(isAutohandlingCookies);
        resetChain();
	}
	
    /**
     * sets the cache size (in kilo bytes)
     * 
     * @param maxSizeKB the max cache size in bytes or 0 to deactivate caching
     */
    public void setCacheMaxSizeKB(int maxSizeKB) {
        cacheHandler.setMaxCacheSizeKB(maxSizeKB);
        resetChain();
    }
    
    
    /**
     * returns the max cache size 
     * 
     * @return the max cache size
     */
    public int getCacheMaxSizeKB() {
        return (cacheHandler.getMaxCacheSizeKB());
    }
	
    
    
    /**
     * returns the cache size 
     * 
     * @return the cache size
     */
    public float getCacheSizeKB() {
        return ((float) cacheHandler.getCurrentCacheSizeBytes() / 1000);
    }
    
    /**
     * sets if the cache is shared between users
     * 
     * @param isSharedCache true, if the cache is shared between users
     */
    public void setCacheShared(boolean isSharedCache) {
        cacheHandler.setSharedCache(isSharedCache);
    }
    
    /**
     * returns true, if the cache is shared between users
     * 
     * @return true, if the cache is shared between users
     */
    public boolean isCacheShared() {
        return cacheHandler.isSharedCache();
    }
    
	
	/**
	 * sets the proxy host to use. Example: 
	 * 
	 * <pre>
	 * HttpClient httpClient = new HttpClient();
	 * 
	 * // sets the proxy adress 
	 * httpClient.setProxyHost(host);
	 * httpClient.setProxyPort(port);
	 * 
	 * // set auth params (only necessary if proxy authentication is required) 
	 * httpClient.setProxyUser(user);
	 * httpClient.setProxyPassword(pwd);
	 * 
	 * // calling through the proxy   
	 * IHttpResponse resp = httpClient.call(new GetRequest("http://www.gmx.com/");
	 * // ...
	 * </pre>
	 * 
	 * @param proxyHost the proxy host or <null>
	 */
	public void setProxyHost(String proxyHost) {
	    proxyHandler.setProxyHost(proxyHost);
        
        if ((proxyHost != null) && (proxyHost.length() > 1)) {
            isProxyActivated.set(true);
        }   
        resetChain();
	}
	

	/**
	 * sets the proxy port. Default is 80. For an example see {@link HttpClient#setProxyHost(String)}
	 * 
	 * @param proxyPort the proxy port
	 */
	public void setProxyPort(int proxyPort) {
	    proxyHandler.setProxyPort(proxyPort);
	}

	

	
	/**
	 * sets the user name for proxy authentification  
	 * @param proxyUser  the user name
	 */
	public void setProxyUser(String proxyUser) {
	    proxyHandler.setProxyUser(proxyUser);
	}
	

	/**
	 * sets the user password for proxy authentification  
	 *   
	 * @param proxyPassword the user password 
	 */
	public void setProxyPassword(String proxyPassword) {
	    proxyHandler.setProxyPassword(proxyPassword);
	}

		
	/**
	 * returns if cookies should be auto handled
	 * @return true, if cookies should be auto handled
	 */
	public boolean isAutohandleCookies() {
	    return isAutohandlingCookies.get();
	}


	/**
	 * set if the response will be uncompressed (if compressed)
	 *  
	 * @param isAutoUncompress true, if the response will be uncompressed (if compressed)
	 */
	public final void setAutoUncompress(boolean isAutoUncompress) {
		this.isAutoUncompress.set(isAutoUncompress);
	}

	
	/**
	 * return true, if the response will be uncompressed (if compressed)
	 * 
	 * @return true, if the response will be uncompressed (if compressed)
	 */
	public final boolean isAutoUncompress() {
		return isAutoUncompress.get();
	}

	
	/**
	 * returns the session manager
	 * 
	 * @return the session manager
	 */
	SessionManager getSessionManager() {
		return sessionManager;
	}
	
	
	/**
	 * set the max redirects
	 * 
	 * @param maxRedirects  the max redirects 
	 */
	public void setMaxRedirects(int maxRedirects) {
		this.maxRedirects.set(maxRedirects);
	}
	
	
	/**
	 * get the max redirects (of GET, DELETE and PUT calls)
	 * 
	 * @return the max redirects
	 */
	public int getMaxRedirects() {
		return maxRedirects.get();
	}
	
	
	/**
	 * get the max retries (of GET, DELETE and PUT calls)
	 * 
	 * @return the max retries
	 */
	public int getMaxRetries() {
	   return maxRetries.get();
	}
	
	
	/**
	 * sets the max retries (of GET, DELETE and PUT calls)
	 * 
	 * @param maxRetries  the max retries 
	 */
	public void setMaxRetries(int maxRetries) {
	    if (maxRetries > 0) {
	        this.maxRetries.set(maxRetries);
	    } else {
	        this.maxRetries.set(0);
	    }
	    
	    resetChain();
	}
	
	
	public void setTreat302RedirectAs303(boolean isTreat303RedirectAs302) {
		this.isTreat302RedirectAs303.set(isTreat303RedirectAs302); 
	}
	
	
	public boolean isTreat302RedirectAs303() {
		return isTreat302RedirectAs303.get();
	}
	
	
	/**
	 * get the max size of the transaction log
	 * 
	 * @return the max size of the transaction log
	 */
	int getTransactionLogMaxSize() {
		return transactionLog.getMaxSize();
	}
	
	
	
	/**
	 * returns the number of pending transactions 
	 * 
	 * @return the number of pending transactions
	 */
	Integer getTransactionsPending() {
		if (transactionMonitor != null) {
			return transactionMonitor.getPendingTransactions();
		} else {
			return null;
		}
	}
	
	
	
	/**
	 * sets the max size of the transaction log
	 * 
	 * @param maxSize the max size of the transaction log
	 */
	void setTransactionLogMaxSize(int maxSize) {
		transactionLog.setMaxSize(maxSize);
		
		if (maxSize == 0) {
			transactionMonitor = null;
		} else {
			transactionMonitor = new TransactionMonitor(transactionLog);
		}
	}

	
	/**
	 * set the worker pool which will be assigned to the connections for call back handling
	 * 
	 * @param workerpool the worker pool
	 */
	public void setWorkerpool(Executor workerpool) {
	    pool.setWorkerpool(workerpool);
	}

	
	/**
	 * returns the assigned worker pool
	 * 
	 * @return  the assigned worker pool
	 */
	Executor getWorkerpool() {
	    return pool.getWorkerpool();
	}
	
	
	
	
	/**
     * set the response body default encoding. According to RFC 2616 the 
     * initial value is ISO-8859-1 
     *   
     * @param encoding  the defaultEncoding 
     */
    public void setResponseBodyDefaultEncoding(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }

	
	
	/**
	 *  set that the call will return if the complete message is received. 
	 *  If false is set, the call will return when the response header is received (default is false)
	 *  
	 * @param isCallReturnOnMessage true, if the call is returned on Message 
	 */
	public void setCallReturnOnMessage(boolean isCallReturnOnMessage) {
	    this.isCallReturnOnMessage.set(isCallReturnOnMessage);
	}
	
	
	/**
	 * return true when the call will return, if the message is received
	 *   
	 * @return true when the call will return, if the message is received
	 */
	public boolean getCallReturnOnMessage() {
	    return isCallReturnOnMessage.get();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setResponseTimeoutMillis(long responseTimeoutMillis) {
	    this.responseTimeoutMillis.set(responseTimeoutMillis);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public long getResponseTimeoutMillis() {
		return responseTimeoutMillis.get();
	}

	/**
	 * {@inheritDoc}
	 */
	public final void setBodyDataReceiveTimeoutMillis(long bodyDataReceiveTimeoutMillis) {
		this.bodyDataReceiveTimeoutMillis.set(bodyDataReceiveTimeoutMillis);
	}

	/**
     * {@inheritDoc}
     */
    public final long getBodyDataReceiveTimeoutMillis() {
        return bodyDataReceiveTimeoutMillis.get();
    }

	
	/**
	 * {@inheritDoc}
	 */
	public void close() throws IOException {
		pool.close();

		chain.onDestroy();
		
		if (sessionManager != null) {
		    sessionManager.close();
		    sessionManager = null;
		}
	}
	


	
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isOpen() {
		return pool.isOpen();
	}
	
	
	/**
	 * returns a unique id 
	 * 
	 * @return the id 
	 */
	public String getId() {
		return Integer.toString(this.hashCode());
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
	public void setPooledMaxIdleTimeMillis(int idleTimeoutMillis) {
		pool.setPooledMaxIdleTimeMillis(idleTimeoutMillis);
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
	public void setPooledMaxLifeTimeMillis(int lifeTimeoutMillis) {
		pool.setPooledMaxLifeTimeMillis(lifeTimeoutMillis);
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
	public void setMaxIdle(int maxIdle) {
		pool.setMaxIdle(maxIdle);
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
	public void setMaxActive(int maxActive) {
		pool.setMaxActive(maxActive);
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
	 * {@inheritDoc}
	 */
	public int getMaxActive() {
		return pool.getMaxActive();
	}
	

	/**
	 * sets the connect timeout
	 *  
	 * @param connectTimeoutMillis   the connect timeout
	 */
    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis.set(connectTimeoutMillis);
    }
	
    
    /**
     * returns the connect timeout
     * @return the connect timeout
     */
    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis.get();
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
	int getNumPendingGet() {
		return pool.getNumPendingGet();
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public int getNumCreated() {
		return pool.getNumCreated();
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public int getNumDestroyed() {
		return pool.getNumDestroyed();
	}
	
	
	/**
	 * get the number of creation errors 
	 * 
	 * @return the number of creation errors
	 */
    int getNumCreationError() {
    	return pool.getNumCreationError();
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
     * returns the acquire timeout. Default is null 
     * 
     * @return the acquire timeout
     */
    public Integer getAcquireTimeoutMillis() {
        return pool.getAcquireTimeoutMillis();
    }
    
    
    /**
     * sets the acquire time out. Null deactivates the acquire timeout. 
     * 
     * @param aquireTimeoutMillis the acquire timeout or <code>null</code> 
     */
    public void setAcquireTimeoutMillis(Integer aquireTimeoutMillis) {
        pool.setAcquireTimeoutMillis(aquireTimeoutMillis);
    }    
    
    /**
     * returns the number of cache hits
     * @return the number of cache hits
     */
    public int getNumCacheHit() {
        return cacheHandler.getCountCacheHit();
    }


    /**
     * returns the number of cache misses
     * @return the number of cache misses
     */
    public int getNumCacheMiss() {
        return cacheHandler.getCountCacheMiss();
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

	boolean isCacheInfoDisplay() {
	    return isShowCache;
	}
	
	void setCacheInfoDisplay(boolean isShowCache) {
	    this.isShowCache = isShowCache;
	}
	
	
	List<String> getCacheInfo() {
	    if (isShowCache) {
	        return cacheHandler.getCacheInfo();
	    } else {
	        return null;
	    }
    }
	
	
	double getCacheHitRatio() {
	    return cacheHandler.getCurrentHitRatio();
	}
	
	/**
	 * returns the transaction log 
	 * @return the transaction log
	 */
	List<String> getTransactionInfos() {
		List<String> result = new ArrayList<String>();
		for (Transaction transaction : transactionLog.getTransactions()) {
			result.add(transaction.toString());
		}
		return result;
	}
	

		
	/**
	 * {@inheritDoc}
	 */
	public IHttpResponse call(IHttpRequest request) throws IOException, SocketTimeoutException {
	    
	    if (isCallReturnOnMessage.get()) {
	        try {
	            request.setAttribute(RetryHandler.RETRY_KEY, false);
	            FutureMessageResponseHandler responseHandler = new FutureMessageResponseHandler(request);
	            send(request, responseHandler);
	            return responseHandler.getResponse();
	        } catch (InterruptedException ie) {
	            throw new RuntimeException(ie);
	        }

	    } else {
    		try {
    			IFutureResponse futureResponse = send(request);
    			return futureResponse.getResponse();
    		} catch (InterruptedException ie) {
    			throw new RuntimeException(ie);
    		}
	    }
	}

	
	/**
	 * {@inheritDoc}
	 */	
	public IFutureResponse send(IHttpRequest request) throws IOException, ConnectException {
	    
        FutureResponseHandler responseHandler;
        
        // continue expected?
        if (request.hasBody() && HttpUtils.isContainsExpect100ContinueHeader(request)) {
            
            // do not auto continue for network requests
            if (HttpClientConnection.isNetworkendpoint(request.getNonBlockingBody())) {
                request.setAttribute(HttpClientConnection.IS_AUTOCONTINUE_DEACTIVATED, true);
            }
            
            if ((request.getAttribute(HttpClientConnection.IS_AUTOCONTINUE_DEACTIVATED) == null) || ((Boolean) request.getAttribute(HttpClientConnection.IS_AUTOCONTINUE_DEACTIVATED) != true)) {
                responseHandler = new FutureContinueResponseHandler(request.getRequestHeader(), request.getNonBlockingBody(), getId());
                
                if (LOG.isLoggable(Level.FINE)) {
                	LOG.fine("sending header (body will send after receiving the 100 continue response)");
                }
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
		lastTimeRequestSentMillis = System.currentTimeMillis();

		if (!HttpClientConnection.isSupports100Contine(responseHandler) && HttpUtils.isContainExpect100ContinueHeader(request.getRequestHeader())) {
            // touch body to initiate sending 100-continue if required
            request.getNonBlockingBody().getReadBufferVersion();
		    
            LOG.warning("Request contains 'Excect: 100-coninue' header and response handler is not annotated with Supports100Continue. Removing Expect header");
            request.removeHeader("Expect");
        }	


		// log trace if activated
		if (transactionMonitor != null) {
			transactionMonitor.register(request.getRequestHeader());
		}
	
		ClientExchange clientExchange = new ClientExchange(defaultEncoding, pool, sessionManager, responseHandler, request, connectTimeoutMillis.get(), responseTimeoutMillis.get(), bodyDataReceiveTimeoutMillis.get(), isAutoCloseAfterResponse.get(), isAutoUncompress.get(), transactionMonitor);
		chain.onRequest(clientExchange);
	}
	
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public BodyDataSink send(IHttpRequestHeader requestHeader, int contentLength, IHttpResponseHandler responseHandler) throws IOException, ConnectException {
		requestHeader.setContentLength(contentLength);
		return sendInternal(requestHeader, responseHandler);
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public BodyDataSink send(IHttpRequestHeader requestHeader, IHttpResponseHandler responseHandler) throws IOException, ConnectException {
        if (requestHeader.getContentLength() != -1) {
            return send(requestHeader, requestHeader.getContentLength(), responseHandler);
        }
                         
        if ((requestHeader.getTransferEncoding() == null)) {
            requestHeader.setHeader("Transfer-Encoding", "chunked");
        }

		return sendInternal(requestHeader, responseHandler);
	}
	
	
	private BodyDataSink sendInternal(IHttpRequestHeader requestHeader, IHttpResponseHandler responseHandler) throws IOException, ConnectException {
		lastTimeRequestSentMillis = System.currentTimeMillis();
		
		BodyDataSink dataSink = HttpClientConnection.newInMemoryBodyDataSink(this.getClass().getSimpleName() + "#" + this.hashCode(), requestHeader);
		IHttpRequest request = new HttpRequest(requestHeader, HttpClientConnection.getDataSourceOfInMemoryBodyDataSink(dataSink));

		send(request, responseHandler);		
		return dataSink;
	}
	
	

	
	/**
	 * gets the time when the last request has been sent
	 *  
	 * @return the time when the last request has been sent
	 */
	long getLastTimeRequestSentMillis() {
		return lastTimeRequestSentMillis;
	}
	

	/**
	 * <br/><br/><b>This is an experimental implementation of the HTML5 draft and subject to change</b>
	 * 
	 */
    public IEventDataSource openEventDataSource(String uriString, String... headerlines) throws IOException {
        return openEventDataSource(uriString, true, headerlines);
    }

    
    /**
     * <br/><br/><b>This is an experimental implementation of the HTML5 draft and subject to change</b>
     * 
     */    
    public IEventDataSource openEventDataSource(String uriString, boolean isIgnoreCommentMessage) throws IOException {
        return openEventDataSource(uriString, isIgnoreCommentMessage, new String[0]);
    }
    
    
    /**
     * <br/><br/><b>This is an experimental implementation of the HTML5 draft and subject to change</b>
     * 
     */
    public IEventDataSource openEventDataSource(String uriString, boolean isIgnoreCommentMessage, String... headerlines) throws IOException {
        return openEventDataSource(uriString, isIgnoreCommentMessage, null, headerlines);
    }

    
    /**
     * <br/><br/><b>This is an experimental implementation of the HTML5 draft and subject to change</b>
     * 
     */
    public IEventDataSource openEventDataSource(String uriString, IEventHandler webEventHandler, String... headerlines) throws IOException {
        return new EventDataSource(this, uriString, true, webEventHandler, headerlines);
    }
    
    
    /**
     * <br/><br/><b>This is an experimental implementation of the HTML5 draft and subject to change</b>
     * 
     */
    public IEventDataSource openEventDataSource(String uriString, boolean isIgnoreCommentMessage, IEventHandler webEventHandler, String... headerlines) throws IOException {
        return new EventDataSource(this, uriString, isIgnoreCommentMessage, webEventHandler, headerlines);
    }

    
    /**
     * <br/><br/><b>This is an experimental implementation of the HTML5 draft and subject to change</b>
     * 
     */
    public IWebSocketConnection openWebSocketConnection(String uriString) throws IOException {
        return openWebSocketConnection(uriString, (String) null);
    }

    
    /**
     * <br/><br/><b>This is an experimental implementation of the HTML5 draft and subject to change</b>
     * 
     */
    public IWebSocketConnection openWebSocketConnection(String uriString, String protocol) throws IOException {
        return openWebSocketConnection(uriString, protocol, null);
    }

    
    /**
     * <br/><br/><b>This is an experimental implementation of the HTML5 draft and subject to change</b>
     * 
     */
    public IWebSocketConnection openWebSocketConnection(String uriString, IWebSocketHandler webSocketHandler) throws IOException {
        return openWebSocketConnection(uriString, null, webSocketHandler);
    }
    
    
    
    /**
     * <br/><br/><b>This is an experimental implementation of the HTML5 draft and subject to change</b>
     * 
     */
    public IWebSocketConnection openWebSocketConnection(String uriString, String protocol, IWebSocketHandler webSocketHandler) throws IOException {
        URI uri = URI.create(uriString);
        
        int port = uri.getPort();
        if (port == -1) {
            if (uri.getScheme().toLowerCase().equals("wss")) {
                port = 443;
            } else {
                port = 80;
            }
        }
        
        return new WebSocketConnection(this, uri, protocol, webSocketHandler); 
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
     * @deprecated using {@link HttpClient#setProxyHost(String)} 
     */
    public void setProxySecuredHost(String proxyHost) {
        proxyHandler.setSecuredProxyHost(proxyHost);
        
        if ((proxyHost != null) && (proxyHost.length() > 1)) {
            isProxyActivated.set(true);
        } 
        resetChain();
    }

    

    /**
     * @deprecated use {@link HttpClient#setProxyPort(int)} 
     */
    public void setProxySecuredPort(int proxyPort) {
        proxyHandler.setSecuredProxyPort(proxyPort);
    }
    
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		
		sb.append("\r\nnumCreatedConnections " + getNumCreated());
		sb.append("\r\nnumCreationError " + getNumCreationError());
		sb.append("\r\nnumDestroyedConnections " + getNumDestroyed());
		
		List<String> active = getActiveConnectionInfos();
		if (active.isEmpty()) {
		    sb.append("\r\nnumActiveConnections 0");
		} else {
    		sb.append("\r\n" + active.size() + " active connections:");
    		for (String connectionInfo : getActiveConnectionInfos()) {
    			sb.append("\r\n " + connectionInfo);
    		}
		}
		
		List<String> idle = getActiveConnectionInfos();
        if (idle.isEmpty()) {
            sb.append("\r\nnumIdleConnections 0");
        } else {
            sb.append("\r\n" + idle.size() + " idle connections:");
            for (String connectionInfo : getIdleConnectionInfos()) {
                sb.append("\r\n " + connectionInfo);
            }
        }

		
		sb.append("\r\ntransaction log:");
		for (String transactionInfo : getTransactionInfos()) {
			sb.append("\r\n " + transactionInfo);
		}
		
		return sb.toString();
	}	
	

	@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
	private final class FutureMessageResponseHandler extends FutureResponseHandler {
	    
	    private final IHttpRequest request;
	    private final int currentRetryNum;
	    
	    public FutureMessageResponseHandler(IHttpRequest request) {
	        this.request = request;
	        
	        Integer numReq = (Integer) request.getAttribute("org.xlightweb.client.FutureMessageResponseHandler.currentRetryNum");
	        if (numReq == null) {
	            numReq = 0;
	        }
	        
	        currentRetryNum = numReq + 1;
	        request.setAttribute("org.xlightweb.client.FutureMessageResponseHandler.currentRetryNum", currentRetryNum);	        
        }
	    
	    @Override
	    public void onException(IOException ioe) throws IOException {
	        if (isRetryableMethod() && RetryHandler.isRetryable(ioe)) {
	            if (currentRetryNum < getMaxRetries()) {
	                if (LOG.isLoggable(Level.FINE)) {
	                    LOG.fine("try to retrying request (retry num " +  currentRetryNum + "). I/O exception  caught when processing request " + ioe.toString());
	                }
	                retry();
	                return;
	                
	            } else {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("max retries " + getMaxRetries() + ". I/O exception  caught when processing request " + ioe.toString());
                    }
                }
	        } 
	        
	        super.onException(ioe);
	    }
	    
	    @Override
	    public void onException(SocketTimeoutException stoe) {
	        if (isRetryableMethod()) {
	            if (currentRetryNum < getMaxRetries()) {
	                if (LOG.isLoggable(Level.FINE)) {
	                    LOG.fine("try to retrying request (retry num " +  currentRetryNum + "). I/O exception  caught when processing request " + stoe.toString());
	                }
	                retry();
	                return;
	                
	            } else {
	                if (LOG.isLoggable(Level.FINE)) {
	                    LOG.fine("max retries " + getMaxRetries() + ". I/O exception  caught when processing request " + stoe.toString());
	                }
	            }
	        }
	        
	        super.onException(stoe);
	    }
	    
	    
	    private void retry() {
	        
	        Runnable task = new Runnable() {  
	            
	            public void run() {
        	        try {
        	            IHttpResponse response = call(request);
        	            onResponse(response);
        	        } catch (IOException ioe) {
        	            try {
        	                FutureMessageResponseHandler.super.onException(ioe);
        	            } catch (IOException e) {
        	                if (LOG.isLoggable(Level.FINE)) {
        	                    LOG.fine("error occured by calling onException " + e.toString());
        	                }
        	                
        	            }
        	        }
	            }
	        };
	        
            // run in own thread to prevent dead locks
	        getWorkerpool().execute(task);
	    }
	    
	    
	    private boolean isRetryableMethod() {
	        return (request.getMethod().equalsIgnoreCase("GET"));
	    }
	}

}
