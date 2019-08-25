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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xlightweb.HttpUtils.RequestHandlerInfo;
import org.xsocket.Execution;
import org.xsocket.ILifeCycle;




/**
 * By using the <ocde>Context</code> specific request handlers can be assigned
 * via a url-pattern to a set of URLs (Routing). The url-pattern syntax is equals 
 * to the Servlet API. A request handler will be assigned to a url by using a Context. <br>
 * 
 * Typically, this approach is required, if static resources have to be 
 * supported as well as dynamic resources. See example:
 * 
 * <pre>
 *  Context ctx = new Context("");
 *  ctx.addHandler("/site/*", new FileServiceRequestHandler(basePath, true));   
 *  ctx.addHandler("/rpc/*", new MyBusinessHandler());
 *  ctx.addHandler(new MappingAnnotatedHandler());  
 *  
 *  Server server = new HttpServer(8080, ctx);
 *  server.start();
 * </pre>  
 *
 * <br/><br/><b>This is an experimental implementation of the HTML5 draft and subject to change</b>
 *
 * @author grro@xlightweb.org
 */
@Execution(Execution.MULTITHREADED)
public class Context implements IHttpRequestHandler, IHttpRequestTimeoutHandler, IWebSocketHandler, ILifeCycle, Cloneable  {

	private static final Logger LOG = Logger.getLogger(Context.class.getName());
	
	
	private final List<IWebHandler> handlers = new ArrayList<IWebHandler>();
	
	private final List<ILifeCycle> lifeCycleChain = new ArrayList<ILifeCycle>();
	
	private final List<IHolder> holders = new ArrayList<IHolder>();
	private final HolderCache holderCache = new HolderCache(40);
	
	private boolean isOnRequestTimeoutPathMultithreaded = false;
	private final List<IHttpRequestTimeoutHandler> requestTimeoutHandlerChain = new ArrayList<IHttpRequestTimeoutHandler>();

	private final String contextPath;
	
	
	
	/**
	 * constructor
	 * 
	 * @param contextPath   the context path
	 */
	public Context(String contextPath) {
		this.contextPath = contextPath;
	}

	
	/**
	 * constructor
	 * 
	 * @param contextPath  the context path
	 * @param handlers     handler map 
	 */
	public Context(String contextPath, Map<String, IHttpRequestHandler> handlers) {
		this.contextPath = contextPath;
		
		for (Entry<String, IHttpRequestHandler> entry : handlers.entrySet()) {
			addHandler(entry.getKey(), entry.getValue());
		}
	}

	
	/**
	 * constructor
	 * 
	 * @param parentContext   the parent context
	 * @param contextPath     the context path
	 */
	public Context(Context parentContext, String contextPath) {
		this.contextPath = contextPath;
		
		parentContext.addContext(this);
	}
	
	
	private void addContext(Context ctx) {
		holders.add(new ContextHolder(ctx)); 
		sortHolderList();
	}
	
	
	/**
	 * adds an annotated handler to the current context 
	 * 
	 * @param webHandler   the annotated handler (supported: {@link IHttpRequestHandler}, {@link ILifeCycle})
	 */
	public void addHandler(IWebHandler webHandler) {
		String[] mappings = HttpUtils.retrieveMappings(webHandler);
		if (mappings == null) {
			throw new RuntimeException("handler mapping is not annotated (hint: use @Mapping()");
		}
		
		for (String mapping : mappings) {
			addHandler(mapping, webHandler);
		}
	}
	
	
	
	
	/**
	 * adds a handler to the current context 
	 * 
	 * @param pattern          the pattern
	 * @param webHandler       the handler (supported: {@link IHttpRequestHandler}, {@link ILifeCycle})
	 */
	public void addHandler(String pattern, IWebHandler webHandler) {
		RequestHandlerHolder holder = new RequestHandlerHolder(pattern, webHandler);
		
		for (IHolder hld : holders) {
			if (hld.getPattern().equalsIgnoreCase(holder.getPattern())) {
				holders.remove(hld);
				if (LOG.isLoggable(Level.FINE)) {
					LOG.fine("handler already exists for pattern " + pattern + " Replacing existing holder");
				}
				break;
			}
		}
		
		holders.add(holder);
		sortHolderList();
		
		handlers.add(webHandler);		
		computePath();
	}
	
	
	private void computePath() {
		holderCache.clear();		
		lifeCycleChain.clear();
		
		requestTimeoutHandlerChain.clear();
		isOnRequestTimeoutPathMultithreaded = false;
		
		
		for (IWebHandler handler : handlers) {

			if (ILifeCycle.class.isAssignableFrom(handler.getClass())) {
				lifeCycleChain.add((ILifeCycle) handler);
			}
			
			if (handler instanceof IHttpRequestHandler) {
		        RequestHandlerInfo requestHandlerInfo = HttpUtils.getRequestHandlerInfo((IHttpRequestHandler) handler);
		        if (requestHandlerInfo.isRequestTimeoutHandler()) {
    				requestTimeoutHandlerChain.add((IHttpRequestTimeoutHandler) handler);
    				isOnRequestTimeoutPathMultithreaded = isOnRequestTimeoutPathMultithreaded || requestHandlerInfo.isRequestTimeoutHandlerMultithreaded();
		        }
			}
		}		
	}
	 
	
	
	private void sortHolderList() {
		Comparator<IHolder> comparator = new Comparator<IHolder>() {

			public int compare(IHolder o1, IHolder o2) {
				return (0 - o1.getPattern().compareTo(o2.getPattern()));
			}
		};

		Collections.sort(holders, comparator);
	}
	

	/**
	 * {@inheritDoc}
	 */
	public void onInit() {
		for (IHolder holder : holders) {
			holder.onInit();
		}
	}

	
	/**
	 * {@inheritDoc}
	 */
	public void onDestroy() throws IOException {
		for (IHolder holder : holders) {
			holder.onDestroy();
		}		
	}
	
	
	/**
	 * returns the context path
	 * 
	 * @return the context path
	 */
	public String getContextPath() {
		return contextPath;
	}

	
	/**
	 * returns the mappings 
	 * 
	 * @return the mappings
	 */
	List<String> getMapping() {
		List<String> result = new ArrayList<String>();
		
		for (IHolder holder : holders) {
			result.add("[" + holder.getPattern() + "] -> " + holder.getTarget());
		}
		
		Collections.sort(result);
		
		return result;
	}
	
	
	/**
	 * returns the handlers
	 * 
	 * @return the handlers
	 */
	public List<IWebHandler> getHandlers() {
		List<IWebHandler> result = new ArrayList<IWebHandler>();
		
		for (IHolder holder : holders) {
			result.add(holder.getTarget());
		}
		
		return result;
	}
	
	
	/**
	 * returns the current size 
	 * 
	 * @return the current site
	 */
	public int size() {
		return holders.size();
	}
	

	
	
	
	/**
	 * {@inheritDoc}
	 */
	public void onRequest(IHttpExchange exchange) throws IOException {
		
		String path = exchange.getRequest().getRequestURI();
		
		if (path.startsWith(contextPath)) {
			path = path.substring(contextPath.length(), path.length());
			
			onRequest(path, exchange, contextPath);
			
		} else {
			sendNotFoundError(exchange);
		}
	}	
	



	private void onRequest(String path, IHttpExchange exchange, String totalContextPath) throws IOException {
		
		if (holderCache.containsKey(path)){
			holderCache.get(path).onRequest(path, exchange, totalContextPath);
			return;
		}
			
		for (IHolder holder : holders) {
			if (holder.match(path)) {
				holderCache.put(path, holder);
				holder.onRequest(path, exchange, totalContextPath);
				return;
			}
		}
		
		sendNotFoundError(exchange);
	}	
	
	private void sendNotFoundError(IHttpExchange exchange) {
		if (HttpUtils.isShowDetailedError()) {
			
			StringBuilder sb = new StringBuilder("Not found\r\n\r\nsupported context:\r\n"); 
			for (IHolder holder : holders) {
				sb.append("<a href=\"" + holder.getPattern() + "\">" + holder.getPattern() + "</a><br>");
			}

			exchange.sendError(404, sb.toString());
		} else {
			exchange.sendError(404);
		}
	}
	
		
	/**
	 * {@inheritDoc}
	 */
	public boolean onRequestTimeout(IHttpConnection connection) throws IOException {

		if (requestTimeoutHandlerChain.isEmpty()) {
			if (LOG.isLoggable(Level.FINER)) {
				LOG.finer("no request timeout handler set. ignore callback");
			}
			
			return false;
		}
		
        for (IHttpRequestTimeoutHandler requestTimeoutHandler : requestTimeoutHandlerChain) {
            boolean result = requestTimeoutHandler.onRequestTimeout(connection);
            if (result == true) {
                return true;
            }
        }   
        
        return false;
	}
	


	/**
	 * {@inheritDoc}
	 */
	public void onConnect(IWebSocketConnection con) throws IOException, UnsupportedProtocolException {
        String path = con.getUpgradeRequestHeader().getRequestURI();
        
        if (path.startsWith(contextPath)) {
            path = path.substring(contextPath.length(), path.length());
            onConnect(path, con, contextPath);
            
        } else {
            throw new UnsupportedProtocolException();
        }
    }   
    

    private void onConnect(String path, IWebSocketConnection con, String totalContextPath) throws IOException {
        if (holderCache.containsKey(path)){
            holderCache.get(path).onConnect(path, con, totalContextPath);
            return;
        }
            
        for (IHolder holder : holders) {
            if (holder.match(path)) {
                holderCache.put(path, holder);
                holder.onConnect(path, con, totalContextPath);
                return;
            }
        }
        
        throw new UnsupportedProtocolException();
    }   

	
	
	public void onDisconnect(IWebSocketConnection con) throws IOException {
        String path = con.getUpgradeRequestHeader().getRequestURI();
        
        if (path.startsWith(contextPath)) {
            path = path.substring(contextPath.length(), path.length());
            onDisconnect(path, con, contextPath);
        }
    }   
    

    private void onDisconnect(String path, IWebSocketConnection con, String totalContextPath) throws IOException {
        if (holderCache.containsKey(path)){
            holderCache.get(path).onDisconnect(path, con, totalContextPath);
            return;
        }
            
        for (IHolder holder : holders) {
            if (holder.match(path)) {
                holderCache.put(path, holder);
                holder.onDisconnect(path, con, totalContextPath);
                return;
            }
        }
    }   

	
	public void onMessage(IWebSocketConnection con) throws IOException {
        String path = con.getUpgradeRequestHeader().getRequestURI();
        
        if (path.startsWith(contextPath)) {
            path = path.substring(contextPath.length(), path.length());
            onMessage(path, con, contextPath);
        } 
    }   
    

    private void onMessage(String path, IWebSocketConnection con, String totalContextPath) throws IOException {
        if (holderCache.containsKey(path)){
            holderCache.get(path).onMessage(path, con, totalContextPath);
            return;
        }
            
        for (IHolder holder : holders) {
            if (holder.match(path)) {
                holderCache.put(path, holder);
                holder.onMessage(path, con, totalContextPath);
                return;
            }
        }
    }   

		
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		Context copy = (Context) super.clone();
		return copy;
	}
	

	/**
	 * copies the context
	 * @return the copy
	 */
	Context copy() {
		try {
			return (Context) clone();
		} catch (CloneNotSupportedException cnse) {
			throw new RuntimeException(cnse.toString());
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("\"" + contextPath + "\"->{");
		for (IHolder holder  : holders) {
			sb.append(holder + " ");
		}
		return sb.toString().trim() + "}";
	}
	
	
	private interface IHolder {
				
		public void onInit();
		
		public void onDestroy() throws IOException;
		
		public void onRequest(String path, IHttpExchange exchange, String totalContextPath) throws IOException;
		 
		public void onConnect(String path, IWebSocketConnection wsConnection, String totalContextPath) throws IOException, UnsupportedProtocolException;
		
		public void onDisconnect(String path, IWebSocketConnection wsConnection, String totalContextPath) throws IOException;
		
		public void onMessage(String path, IWebSocketConnection wsConnection, String totalContextPath) throws IOException;
		
		public boolean match(String requestedRessource);
		
		public String getPattern();
		
		public IWebHandler getTarget();
	}
	
	
	private static final class ContextHolder implements IHolder {

		private Context context = null;
		
		ContextHolder(Context context) {
			this.context = context.copy();
		}
		
		public void onInit() {
			context.onInit();
		}
		
		public void onDestroy() throws IOException {
			context.onDestroy();
		}
		
		
		public void onRequest(String path, IHttpExchange exchange, String totalContextPath) throws IOException {
			path = path.substring(context.contextPath.length(), path.length());
			context.onRequest(path, exchange, totalContextPath + context.contextPath);
		}
		
        public void onConnect(String path, IWebSocketConnection wsConnection, String totalContextPath) throws IOException {
            path = path.substring(context.contextPath.length(), path.length());
            context.onConnect(path, wsConnection, totalContextPath + context.contextPath);
        }
		
		public void onDisconnect(String path, IWebSocketConnection wsConnection, String totalContextPath) throws IOException, UnsupportedProtocolException {
		    path = path.substring(context.contextPath.length(), path.length());
            context.onDisconnect(path, wsConnection, totalContextPath + context.contextPath);		    
		}
		
		public void onMessage(String path, IWebSocketConnection wsConnection, String totalContextPath) throws IOException, UnsupportedProtocolException {
            path = path.substring(context.contextPath.length(), path.length());
            context.onMessage(path, wsConnection, totalContextPath + context.contextPath);           
        }
		
		public boolean match(String requestedRessource) {
			return requestedRessource.startsWith(context.contextPath) || requestedRessource.equals(context.contextPath);
		}
		
		public String getPattern() {
			return context.contextPath;
		}
		
		public IHttpRequestHandler getTarget() {
			return context;
		}
				
		@Override
		public String toString() {
			return context.toString();
		}
	}	

	
	
	
	private static final class RequestHandlerHolder implements IHolder {

		private String path = null;
		private String pattern = null; 
		private boolean isWildcardPath = false;
		private boolean isWildcardPathExt = false;
		
		private final IWebHandler handler; 
		private boolean isLifeCycleHandler;
		private final RequestHandlerInfo requestHandlerInfo;
		private final WebSocketHandlerInfo webSocketHandlerInfo;

			 
		RequestHandlerHolder(String pattern, IWebHandler handler) {
			this.handler = handler;
			
			isLifeCycleHandler = (handler instanceof ILifeCycle);
			
			if (handler instanceof IHttpRequestHandler)  {
			    requestHandlerInfo = HttpUtils.getRequestHandlerInfo((IHttpRequestHandler) handler);
			} else {
			    requestHandlerInfo = null;
			}
			
			if (handler instanceof IWebSocketHandler) {
			    webSocketHandlerInfo = HttpUtils.getWebSocketHandlerInfo((IWebSocketHandler) handler);
			} else {
			    webSocketHandlerInfo = null;
			}
			
			
			    
			this.pattern = pattern;
			this.path = pattern;
			
			if (pattern.endsWith("/*")) {
				isWildcardPath = true;
				path = pattern.substring(0, pattern.indexOf("/*"));
					
			} else if (pattern.startsWith("*")) {
				path = pattern.substring(1, pattern.length());
				isWildcardPathExt = true;

			} else {
				isWildcardPath = false;
				path = pattern;
			}
		}
		
		
		public void onInit() {
			if (isLifeCycleHandler) {
				((ILifeCycle) handler).onInit();
			}
		}
		
		public void onDestroy() throws IOException {
			if (isLifeCycleHandler) {
				((ILifeCycle) handler).onDestroy();
			}
		}
		
		
		@Execution(Execution.NONTHREADED)
		public void onRequest(String reqPath, IHttpExchange exchange, String totalContextPath) throws IOException {
			
		    if (requestHandlerInfo == null) {
		        exchange.forward(exchange.getRequest());
		        return;
		    }
		    
			IHttpRequest request = exchange.getRequest();
			if (request == null) {
				exchange.destroy();
				return;
			}
			
			request.setContextPath(totalContextPath);
			request.setRequestHandlerPath(path);

			
			// mode MESSAGE_RECEIVED			
			if (requestHandlerInfo.isRequestHandlerInvokeOnMessageReceived()) {
				if (request.hasBody()) {
						
				    BodyListener bodyListener = new BodyListener(exchange, this);
				    NonBlockingBodyDataSource ds = request.getNonBlockingBody(); 
				    ds.addCompleteListener(bodyListener);
				    ds.addDestroyListener(bodyListener);

				} else {
					performOnRequest(exchange);
				}
				
			// mode HEADER_RECEIVED			
			} else {
				performOnRequest(exchange);
			}
		}
		
		
		private void performOnRequest(IHttpExchange exchange)  {
		    try {
		        ((IHttpRequestHandler) handler).onRequest(exchange);
		    } catch (Exception e) {
		        if (LOG.isLoggable(Level.FINE)) {
		            LOG.fine("error occured by calling on request " + handler + " " + e.toString());
		        }
		        throw new RuntimeException(e);
		    }
		}
		
		
		public void onConnect(String path, IWebSocketConnection wsConnection, String totalContextPath) throws IOException, UnsupportedProtocolException {
            if (webSocketHandlerInfo == null) {
                throw new UnsupportedProtocolException();
            }
            
            ((IWebSocketHandler) handler).onConnect(wsConnection);
        }
        		

        public void onDisconnect(String path, IWebSocketConnection wsConnection, String totalContextPath) throws IOException, UnsupportedProtocolException {
            if (webSocketHandlerInfo == null) {
                return;
            }
            
            ((IWebSocketHandler) handler).onDisconnect(wsConnection);
        }

        public void onMessage(String path, IWebSocketConnection wsConnection, String totalContextPath) throws IOException, UnsupportedProtocolException {
            if (webSocketHandlerInfo == null) {
                return;
            }
            
            ((IWebSocketHandler) handler).onMessage(wsConnection);
        }
        
		
		
		public boolean match(String requestedRessource) {
				
			if (isWildcardPath) {
				return requestedRessource.startsWith(path) || requestedRessource.equals(path);
					
			} else if (isWildcardPathExt) {
				return requestedRessource.endsWith(path);
					
			} else {
				return requestedRessource.equals(path);
			}
		}
		
		
		public String getPattern() {
			return pattern;
		}
		
		public IWebHandler getTarget() {
			return handler;
		}
		
		@Override
		public String toString() {
			return "\"" + pattern + "\"->" + handler.getClass().getSimpleName();
		}
	}
	
	
	private static final class BodyListener implements IBodyCompleteListener, IBodyDestroyListener {
		
		private IHttpExchange exchange = null;
		private RequestHandlerHolder requestHandlerHolder = null;
		
		public BodyListener(IHttpExchange exchange, RequestHandlerHolder requestHandlerHolder) {
			this.exchange = exchange;
			this.requestHandlerHolder = requestHandlerHolder;
		}
		
		@Execution(Execution.NONTHREADED)
		public void onComplete() {
			requestHandlerHolder.performOnRequest(exchange);
		}
		
		public void onDestroyed() throws IOException {
		    exchange.destroy();
		}
	}
	
	
	private static final class HolderCache extends LinkedHashMap<String, IHolder> {
        
        private static final long serialVersionUID = 4513864504007457500L;
        
        private int maxSize = 0;
        
        HolderCache(int maxSize) {
            this.maxSize = maxSize;
        }
        
        @Override
        protected boolean removeEldestEntry(Entry<String, IHolder> eldest) {
            return size() > maxSize;
        }   
    }
}