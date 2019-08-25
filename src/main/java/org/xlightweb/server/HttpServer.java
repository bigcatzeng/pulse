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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;



import org.xlightweb.BadMessageException;
import org.xlightweb.HttpResponse;
import org.xlightweb.HttpUtils;
import org.xlightweb.IBodyCompleteListener;
import org.xlightweb.IBodyDestroyListener;
import org.xlightweb.IHttpConnectionHandler;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IWebHandler;
import org.xlightweb.IWebSocketHandler;
import org.xlightweb.WebSocketConnection;
import org.xlightweb.server.TransactionMonitor.Transaction;
import org.xlightweb.server.TransactionMonitor.TransactionLog;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.NonBlockingConnection;
import org.xsocket.connection.Server;


 


/**
 * A HttpServer. The http server accepts incoming connections and forwards the request to
 * the assigned {@link IHttpRequestHandler}. Example:
 * 
 * <pre>
 * 
 *  // defining a request handler 
 *  class MyRequestHandler implements IHttpRequestHandler  {
 *  
 *     public void onRequest(IHttpExchange exchange) throws IOException {
 *        IHttpRequest request = exchange.getRequest();
 *        //...
 *        
 *        exchange.send(new HttpResponse(200, "text/plain", "it works"));
 *     }
 *  } 
 *
 * 
 *  // creates a server
 *  IServer server = new HttpServer(8080, new MyRequestHandler());
 * 
 *  // setting some properties 
 *  server.setMaxTransactions(400);
 *  server.setRequestTimeoutMillis(5 * 60 * 1000);  
 *  //...
 * 
 * 
 *  // executing the server 
 *  server.run();  // (blocks forever)
 * 
 *  // or server.start();  (blocks until the server has been started)
 *  //...
 * </pre> 
 *
 * 
 * @author grro@xlightweb.org
 */
public class HttpServer extends Server implements IHttpServer {

    

    // transaction monitor
    private TransactionMonitor transactionMonitor = null;
    private final TransactionLog transactionLog = new TransactionLog(0);

    
    
    /**
     * constructor <br><br>
     *
     * @param webHandler the webHandler
     * @throws IOException If some other I/O error occurs
     * @throws UnknownHostException if the local host cannot determined
     */
    public HttpServer(IWebHandler webHandler) throws UnknownHostException, IOException {
        this(webHandler, new HashMap<String, Object>());
    }




    /**
     * constructor <br><br>
     *
     * @param webHandler           the webHandler
     * @param options              the socket options  
     * @throws IOException If some other I/O error occurs
     * @throws UnknownHostException if the local host cannot determined
     */
    public HttpServer(IWebHandler webHandler, Map<String, Object> options) throws UnknownHostException, IOException {
        this(new InetSocketAddress(0), options, webHandler, null, false, MIN_SIZE_WORKER_POOL, SIZE_WORKER_POOL);
    }


    /**
     * constructor  <br><br>
     *
     * @param port        the local port
     * @param webHandler  the webHandler
     * @throws UnknownHostException if the local host cannot determined
     * @throws IOException If some other I/O error occurs
     */
    public HttpServer(int port, IWebHandler webHandler) throws UnknownHostException, IOException {
        this(port, webHandler, MIN_SIZE_WORKER_POOL, SIZE_WORKER_POOL);
    }

    
    /**
     * constructor  <br><br>
     *
     * @param port         the local port
     * @param webHandler   the webHandler
     * @param minPoolsize  the min workerpool size
     * @param maxPoolsize  the max workerpool size
     * @throws UnknownHostException if the local host cannot determined
     * @throws IOException If some other I/O error occurs
     */
    public HttpServer(int port, IWebHandler webHandler, int minPoolsize, int maxPoolsize) throws UnknownHostException, IOException {
        this(port, webHandler, null, false, minPoolsize, maxPoolsize);
    }   



    /**
     * constructor <br><br>
     *
     *
     * @param port                 the local port
     * @param webHandler           the webHandler
     * @param options              the acceptor socket options 
     * @throws UnknownHostException if the local host cannot determined
     * @throws IOException If some other I/O error occurs
     */
    public HttpServer(int port, IWebHandler webHandler, Map<String , Object> options) throws UnknownHostException, IOException {
        this(port, webHandler, options, null, false);
    }


    /**
     * constructor <br><br>
     *
     *
     * @param address     the local address
     * @param port        the local port
     * @param webHandler  the webHandler
     * @throws UnknownHostException if the local host cannot determined
     * @throws IOException If some other I/O error occurs
     */
    public HttpServer(InetAddress address, int port, IWebHandler webHandler) throws UnknownHostException, IOException {
        this(address, port, webHandler, null, false);
    }



    /**
     * constructor  <br><br>
     *
     *
     * @param ipAddress   the local ip address
     * @param port        the local port
     * @param webHandler  the webHandler 
     * @throws UnknownHostException if the local host cannot determined
     * @throws IOException If some other I/O error occurs
     */
    public HttpServer(String ipAddress, int port, IWebHandler webHandler) throws UnknownHostException, IOException {
        this(InetAddress.getByName(ipAddress), port, webHandler);
    }



    /**
     * constructor <br><br>
     *
     *
     * @param ipAddress            the local ip address
     * @param port                 the local port
     * @param webHandler           the webHandler 
     * @param options              the socket options 
     * @throws UnknownHostException if the local host cannot determined
     * @throws IOException If some other I/O error occurs
     */
    public HttpServer(String ipAddress, int port, IWebHandler webHandler, Map<String, Object> options) throws UnknownHostException, IOException {
        this(InetAddress.getByName(ipAddress), port, webHandler, options, null, false);
    }




    /**
     * constructor <br><br>
     *
     *
     * @param port               local port
     * @param webHandler         the webHandler
     * @param sslOn              true, is SSL should be activated
     * @param sslContext         the ssl context to use
     * @throws UnknownHostException if the local host cannot determined
     * @throws IOException If some other I/O error occurs
     */
    public HttpServer(int port, IWebHandler webHandler, SSLContext sslContext, boolean sslOn) throws UnknownHostException, IOException {
        this(port, webHandler, new HashMap<String, Object>(), sslContext, sslOn);
    }


    
       /**
     * constructor <br><br>
     *
     *
     * @param port               local port
     * @param webHandler         the webHandler
     * @param sslOn              true, is SSL should be activated
     * @param sslContext         the ssl context to use
     * @param minPoolsize  the min workerpool size
     * @param maxPoolsize  the max workerpool size 
     * @throws UnknownHostException if the local host cannot determined
     * @throws IOException If some other I/O error occurs
     */
    public HttpServer(int port, IWebHandler webHandler, SSLContext sslContext, boolean sslOn, int minPoolsize, int maxPoolsize) throws UnknownHostException, IOException {
        this(new InetSocketAddress(port), new HashMap<String, Object>(), webHandler, sslContext, sslOn, minPoolsize, maxPoolsize);
    }

    



    /**
     * constructor <br><br>
     *
     * @param port                 local port
     * @param options              the acceptor socket options
     * @param webHandler           the webHandler 
     * @param sslOn                true, is SSL should be activated
     * @param sslContext           the ssl context to use
     * @throws UnknownHostException if the local host cannot determined
     * @throws IOException If some other I/O error occurs
     */
    public HttpServer(int port, IWebHandler webHandler, Map<String, Object> options, SSLContext sslContext, boolean sslOn) throws UnknownHostException, IOException {
        this(new InetSocketAddress(port), options, webHandler, sslContext, sslOn, MIN_SIZE_WORKER_POOL, SIZE_WORKER_POOL);
    }
 

    /**
     * constructor <br><br>
     *
     * @param ipAddress          local ip address
     * @param port               local port
     * @param webHandler         the webHandler 
     * @param sslOn              true, is SSL should be activated
     * @param sslContext         the ssl context to use
     * @throws UnknownHostException if the local host cannot determined
     * @throws IOException If some other I/O error occurs
     */
    public HttpServer(String ipAddress, int port, IWebHandler webHandler, SSLContext sslContext, boolean sslOn) throws UnknownHostException, IOException {
        this(ipAddress, port, webHandler, new HashMap<String, Object>(), sslContext, sslOn);
    }



    /**
     * constructor <br><br>
     *
     *
     * @param ipAddress            local ip address
     * @param port                 local port
     * @param options              the acceptor socket options
     * @param webHandler           the webHandler 
     * @param sslOn                true, is SSL should be activated
     * @param sslContext           the ssl context to use
     * @throws UnknownHostException if the local host cannot determined
     * @throws IOException If some other I/O error occurs
     */
    public HttpServer(String ipAddress, int port, IWebHandler webHandler, Map<String, Object> options, SSLContext sslContext, boolean sslOn) throws UnknownHostException, IOException {
        this(InetAddress.getByName(ipAddress), port, webHandler, options, sslContext, sslOn);
    }


    /**
     * constructor <br><br>
     *
     *
     * @param address            local address
     * @param port               local port
     * @param webHandler         the webHandler
     * @param sslOn              true, is SSL should be activated
     * @param sslContext         the ssl context to use
     * @throws UnknownHostException if the local host cannot determined
     * @throws IOException If some other I/O error occurs
     */
    public HttpServer(InetAddress address, int port, IWebHandler webHandler, SSLContext sslContext, boolean sslOn) throws UnknownHostException, IOException {
        this(address, port, webHandler, new HashMap<String, Object>(), sslContext, sslOn);
    }


    /**
     * constructor <br><br>
     *
     *
     * @param address              local address
     * @param port                 local port
     * @param options              the socket options
     * @param webHandler           the webHandler 
     * @param sslOn                true, is SSL should be activated
     * @param sslContext           the ssl context to use
     * @throws UnknownHostException if the local host cannot determined
     * @throws IOException If some other I/O error occurs
     */
    public HttpServer(InetAddress address, int port, IWebHandler webHandler, Map<String, Object> options, SSLContext sslContext, boolean sslOn) throws UnknownHostException, IOException {
        this(new InetSocketAddress(address, port), options, webHandler, sslContext, sslOn, MIN_SIZE_WORKER_POOL, SIZE_WORKER_POOL);
    }
     
    
    HttpServer(InetSocketAddress address, Map<String, Object> options, IWebHandler webHandler, SSLContext sslContext, boolean sslOn, int minPoolsize, int maxPoolsize) throws UnknownHostException, IOException {
        this(address, options, getRequestHandler(webHandler), new WebSocketUpgradeHandler(webHandler), sslContext, sslOn, minPoolsize, maxPoolsize);
    }
    
    
   	
	HttpServer(InetSocketAddress address, Map<String, Object> options, IHttpRequestHandler requestHandler, IUpgradeHandler upgradeHandler, SSLContext sslContext, boolean sslOn, int minPoolsize, int maxPoolsize) throws UnknownHostException, IOException {
        super(address, options, new HttpProtocolAdapter(requestHandler, upgradeHandler), sslContext, sslOn, 0, minPoolsize, maxPoolsize);
    }
	
	
	/**
	 * adds a connection handler 
	 * 
	 * @param connectionHandler  the connection handler to add
	 */
	public void addConnectionHandler(IHttpConnectionHandler connectionHandler) {
		((HttpProtocolAdapter) super.getHandler()).addConnectionHandler(connectionHandler);
	}

	
	protected final void onPreRejectConnection(NonBlockingConnection connection) throws IOException {
	    HttpResponse response = new HttpResponse(503);
	    response.setServer(ServerUtils.getComponentInfo());
	    response.setHeader("Connection", "close");
	    response.setHeader("Content-Length", "0");
	    
        connection.write(response.toString());
    }
	
	
	/**
     * returns the implementation version
     *  
     * @return the implementation version
     */
    public String getImplementationVersion() {
        return HttpUtils.getImplementationVersion();
    }
    
    
    int getNumHandledConnections() {
        return ((HttpProtocolAdapter) super.getHandler()).getNumHandledConnections();
    }
    
    
    /**
     * returns the implementation date
     * 
     * @return  the implementation date 
     */
    public String getImplementationDate() {
        return HttpUtils.getImplementationDate();
    }
	

    /**
     * {@inheritDoc}
     */
    public IHttpRequestHandler getRequestHandler() {
    	return (IHttpRequestHandler) ((HttpProtocolAdapter) getHandler()).getRequestHandler();
    }

    
    /**
     * returns the xSocket implementation version
     *  
     * @return the xSocket implementation version
     */ 
    String getXSocketImplementationVersion() {
        return ConnectionUtils.getImplementationVersion();
    }
    
    
    
    /**
     * returns the xScoket implementation date
     * 
     * @return  the xSocket implementation date 
     */
    String getXSocketImplementationDate() {
        return ConnectionUtils.getImplementationDate();
    }
	
	/**
	 * {@inheritDoc}
	 */
	public void setRequestTimeoutMillis(long receivetimeout) {
		((HttpProtocolAdapter) super.getHandler()).setRequestTimeoutMillis(receivetimeout); 
	}

	
	
	/**
	 * {@inheritDoc}
	 */
	public void setAutoCompressThresholdBytes(int autocompressThresholdBytes) {
		((HttpProtocolAdapter) super.getHandler()).setAutoCompressThresholdBytes(autocompressThresholdBytes);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public int getAutoCompressThresholdBytes() {
		return ((HttpProtocolAdapter) super.getHandler()).getAutoCompressThresholdBytes();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void setAutoUncompress(boolean isAutoUncompress) {
		((HttpProtocolAdapter) super.getHandler()).setAutoUncompress(isAutoUncompress);
	}

	
	/**
	 * {@inheritDoc}
	 */
	public final boolean isAutoUncompress() {
		return ((HttpProtocolAdapter) super.getHandler()).isAutoUncompress();
	}
	   
    /**
     *{@inheritDoc}
     */
    public long getRequestTimeoutMillis() {
        return ((HttpProtocolAdapter) super.getHandler()).getRequestTimeoutMillis();
    }
    
    
	
	/**
	 * {@inheritDoc}
	 */
	public final void setBodyDataReceiveTimeoutMillis(long bodyDataReceiveTimeoutMillis) {
		((HttpProtocolAdapter) super.getHandler()).setBodyDataReceiveTimeoutMillis(bodyDataReceiveTimeoutMillis);
	}

	/**
	 * {@inheritDoc}
	 */
	public long getBodyDataReceiveTimeoutMillis() {
		return ((HttpProtocolAdapter) super.getHandler()).getBodyDataReceiveTimeoutMillis();
	}
	
	/**
	 * {@inheritDoc}
	 */
    public void setSessionMaxInactiveIntervalSec(int sessionMaxInactiveIntervalSec) {
        ((HttpProtocolAdapter) super.getHandler()).setSessionMaxInactiveIntervalSec(sessionMaxInactiveIntervalSec);
    }

    
	/**
	 * {@inheritDoc}
	 */
    public int getSessionMaxInactiveIntervalSec() {
        return ((HttpProtocolAdapter) super.getHandler()).getSessionMaxInactiveIntervalSec();
    }
	
	
	/**
	 * {@inheritDoc}
	 */
	public void setCloseOnSendingError(boolean isCloseOnSendingError) {
		((HttpProtocolAdapter) super.getHandler()).setCloseOnSendingError(isCloseOnSendingError);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isCloseOnSendingError() {
		return ((HttpProtocolAdapter) super.getHandler()).isCloseOnSendingError();
	}
	
	
	/**
     *{@inheritDoc}
     */
    public void setRequestBodyDefaultEncoding(String defaultEncoding) {
        ((HttpProtocolAdapter) super.getHandler()).setRequestBodyDefaultEncoding(defaultEncoding);
    }


	/**
     *{@inheritDoc}
     */
    public String getRequestBodyDefaultEncoding() {
        return ((HttpProtocolAdapter) super.getHandler()).getRequestBodyDefaultEncoding();
    }
	
	
    /**
     * {@inheritDoc} 
     */
	public void setMaxTransactions(int maxTransactions) {
		((HttpProtocolAdapter) getHandler()).setMaxTransactions(maxTransactions);
	}

	   
    /**
     * {@inheritDoc} 
     */
    public int getMaxTransactions() {
        return ((HttpProtocolAdapter) getHandler()).getMaxTransactions();
    }
    
	
	/**
	 * {@inheritDoc}
	 */
	public void setSessionManager(ISessionManager sessionManager) {
		((HttpProtocolAdapter) getHandler()).setSessionManager(sessionManager);
	}
	

	/**
	 * {@inheritDoc}
	 */
	public ISessionManager getSessionManager() {
		return ((HttpProtocolAdapter) getHandler()).getSessionManager();
	}
	
	
	

	/**
	 * {@inheritDoc} 
	 */
	public void setUsingCookies(boolean useCookies) {
		((HttpProtocolAdapter) getHandler()).setUsingCookies(useCookies);
	}
	

	/**
	 * {@inheritDoc} 
	 */
	public boolean isUsingCookies() {
		return ((HttpProtocolAdapter) getHandler()).isUsingCookies();
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
        
        ((HttpProtocolAdapter) getHandler()).setTransactionMonitor(transactionMonitor);
    }
    
    Integer getTransactionsPending() {
        if (transactionMonitor != null) {
            return transactionMonitor.getPendingTransactions();
        } else {
            return null;
        }
    }

    

    /**
     * get the max size of the transaction log
     * 
     * @return the max size of the transaction log
     */
    int getTransactionLogMaxSize() {
        return transactionLog.getMaxSize();
    }  
    
    
    private static IHttpRequestHandler getRequestHandler(IWebHandler webHandler) {
        if (webHandler == null) {
            return null;
        }
        
        if (IHttpRequestHandler.class.isAssignableFrom(webHandler.getClass())) {
            return ((IHttpRequestHandler) webHandler);
        } else {
            return null;
        }
    }
    
    
    private static final class WebSocketUpgradeHandler implements IUpgradeHandler {
        
        private final IWebSocketHandler webSocketHandler;
        
        public WebSocketUpgradeHandler(IWebHandler webHandler) {
            if ((webHandler != null) && (IWebSocketHandler.class.isAssignableFrom(webHandler.getClass()))) {
                webSocketHandler = (IWebSocketHandler) webHandler;
            } else {
                webSocketHandler = null;
            }
        }
        
        public boolean onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
            IHttpRequest request = exchange.getRequest();
            
            if ((webSocketHandler != null) && request.getHeader("Upgrade").equalsIgnoreCase("WebSocket") && request.getProtocolVersion().endsWith("1.1")) {
                if (request.hasBody()) {
                    CompleteListener cl = new CompleteListener(exchange, webSocketHandler);
                    request.getNonBlockingBody().addCompleteListener(cl);
                    request.getNonBlockingBody().addDestroyListener(cl);
                } else {
                    new WebSocketConnection((HttpServerConnection) exchange.getConnection(), webSocketHandler, exchange);
                }
                return true;
            } else {
                return false;
            }
        }
    }   
    
    private static final class CompleteListener implements IBodyCompleteListener, IBodyDestroyListener {
        
        private final IHttpExchange exchange;
        private final IWebSocketHandler webSocketHandler;
        
        public CompleteListener(IHttpExchange exchange, IWebSocketHandler webSocketHandler) {
            this.exchange = exchange;
            this.webSocketHandler = webSocketHandler;
        }
        
        public void onComplete() throws IOException {
            new WebSocketConnection((HttpServerConnection) exchange.getConnection(), webSocketHandler, exchange);
        }
        
        public void onDestroyed() throws IOException {
            exchange.destroy();
        }
    }    
}
