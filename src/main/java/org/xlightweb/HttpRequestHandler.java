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

import java.lang.reflect.Method;
import java.net.ConnectException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xsocket.Execution;
import org.xsocket.connection.ConnectionUtils;


 

/**
 * Provides an abstract class to be subclassed to implement a 
 * {@link IHttpRequestHandler}. A subclass of <code>HttpRequestHandlerAdapter</code> 
 * must override at least one method, usually one of these:
 *
 * <ul>
 *    <li> <code>doGet</code>, if the implementation supports HTTP GET requests
 *    <li> <code>doPost</code>, for HTTP POST requests
 *    <li> <code>doPut</code>, for HTTP PUT requests
 *    <li> <code>doDelete</code>, for HTTP DELETE requests 
 * </ul>
 * 
 * If the method for a dedicated HTTP method is not sub classes, 
 * a 405 error will be returned. <br> <br>
 * 
 * Example:
 * <pre>
 * class MyRequestHandler extends HttpRequestHandler {
 * 
 *    &#064Override
 *    protected void doGet(IHttpExchange exchange) throws IOException, BadMessageException {
 *       exchange.send(new HttpResponse(200, "text/plain", "GET called"));
 *    }
 *    
 *    &#064Override
 *    protected void doPost(IHttpExchange exchange) throws IOException, BadMessageException {
 *       exchange.send(new HttpResponse(200, "text/plain", "POST called"));
 *    }
 * }
 * </pre>
 * 
 * For more examples see {@link IHttpRequestHandler}
 * 
 *
 * @author grro@xlightweb.org
 */
public class HttpRequestHandler implements IHttpRequestHandler {

	private static final Logger LOG = Logger.getLogger(HttpRequestHandler.class.getName());
	
	@SuppressWarnings("unchecked")
	private static final Map<Class, HandlerInfo> handlerInfoCache = ConnectionUtils.newMapCache(25);
	
	private final HandlerInfo handlerInfo;
	
	public HttpRequestHandler() {
		handlerInfo = getHandlerInfo(getClass());
	}
	
	
    @SuppressWarnings("unchecked")
	private static HandlerInfo getHandlerInfo(Class clazz) {
        HandlerInfo handlerInfo = handlerInfoCache.get(clazz);

        if (handlerInfo == null) {
        	handlerInfo = new HandlerInfo(clazz);
        	handlerInfoCache.put(clazz, handlerInfo);
        }

        return handlerInfo;
    }	
	
	
		
	/**
	 * {@inheritDoc}
	 */
	@Execution(Execution.NONTHREADED)
	@InvokeOn(InvokeOn.HEADER_RECEIVED)
	public final void onRequest(final IHttpExchange exchange) throws IOException {
		
		String method = exchange.getRequest().getMethod();
		
		if (method.equals(IHttpRequest.GET_METHOD)) {
			if (handlerInfo.isDoGetInvokeOnMessage() && exchange.getRequest().hasBody()) {
				IBodyCompleteListener cl = new IBodyCompleteListener() {
					public void onComplete() throws IOException {
						callDoGet(exchange);
					}
				};
				NonBlockingBodyDataSource ds = exchange.getRequest().getNonBlockingBody(); 
				ds.addCompleteListener(cl);
				ds.addDestroyListener(new DestroyListener(exchange));
				
			} else {
				callDoGet(exchange);
			}
					
			
		} else if (method.equals(IHttpRequest.POST_METHOD)) {
			if (handlerInfo.isDoPostInvokeOnMessage() && exchange.getRequest().hasBody()) {
				IBodyCompleteListener cl = new IBodyCompleteListener() {
					public void onComplete() throws IOException {
						callDoPost(exchange);
					}
				};
                NonBlockingBodyDataSource ds = exchange.getRequest().getNonBlockingBody(); 
                ds.addCompleteListener(cl);
                ds.addDestroyListener(new DestroyListener(exchange));
				
			} else {
				callDoPost(exchange);
			}

			
		} else if (method.equals(IHttpRequest.PUT_METHOD)) {
			if (handlerInfo.isDoPutInvokeOnMessage() && exchange.getRequest().hasBody()) {
				IBodyCompleteListener cl = new IBodyCompleteListener() {
					public void onComplete() throws IOException {
						callDoPut(exchange);
					}
				};
                NonBlockingBodyDataSource ds = exchange.getRequest().getNonBlockingBody(); 
                ds.addCompleteListener(cl);
                ds.addDestroyListener(new DestroyListener(exchange));
				
			} else {
				callDoPut(exchange);
			}

			
		} else if (method.equals(IHttpRequest.DELETE_METHOD)) {
			if (handlerInfo.isDoDeleteInvokeOnMessage() && exchange.getRequest().hasBody()) {
				IBodyCompleteListener cl = new IBodyCompleteListener() {
					public void onComplete() throws IOException {
						callDoDelete(exchange);
					}
				};
                NonBlockingBodyDataSource ds = exchange.getRequest().getNonBlockingBody(); 
                ds.addCompleteListener(cl);
                ds.addDestroyListener(new DestroyListener(exchange));
				
			} else {
				callDoDelete(exchange);
			}


		} else if (method.equals(IHttpRequest.HEAD_METHOD)) {
			if (handlerInfo.isDoHeadInvokeOnMessage() && exchange.getRequest().hasBody()) {
				IBodyCompleteListener cl = new IBodyCompleteListener() {
					public void onComplete() throws IOException {
						callDoHead(exchange);
					}
				};
                NonBlockingBodyDataSource ds = exchange.getRequest().getNonBlockingBody(); 
                ds.addCompleteListener(cl);
                ds.addDestroyListener(new DestroyListener(exchange));
				
			} else {
				callDoHead(exchange);
			}
			
			
		} else if (method.equals(IHttpRequest.OPTIONS_METHOD)) {
			if (handlerInfo.isDoOptionsInvokeOnMessage() && exchange.getRequest().hasBody()) {
				IBodyCompleteListener cl = new IBodyCompleteListener() {
					public void onComplete() throws IOException {
						callDoOptions(exchange);
					}
				};
                NonBlockingBodyDataSource ds = exchange.getRequest().getNonBlockingBody(); 
                ds.addCompleteListener(cl);
                ds.addDestroyListener(new DestroyListener(exchange));
				
			} else {
				callDoOptions(exchange);
			}

		} else if (method.equals(IHttpRequest.TRACE_METHOD)) {
			if (handlerInfo.isDoTraceInvokeOnMessage() && exchange.getRequest().hasBody()) {
				IBodyCompleteListener cl = new IBodyCompleteListener() {
					public void onComplete() throws IOException {
						callDoTrace(exchange);
					}
				};
                NonBlockingBodyDataSource ds = exchange.getRequest().getNonBlockingBody(); 
                ds.addCompleteListener(cl);
                ds.addDestroyListener(new DestroyListener(exchange));
				
			} else {
				callDoTrace(exchange);
			}

		} else {
		    exchange.forward(exchange.getRequest());
		}
	}
	

    private static final class DestroyListener implements IBodyDestroyListener {
     
        private final IHttpExchange exchange;
        
        public DestroyListener(IHttpExchange exchange) {
            this.exchange = exchange;
        }
        
        public void onDestroyed() throws IOException {
            exchange.destroy();
        }
    }

	
	private void callDoGet(final IHttpExchange exchange) throws IOException {		
		if (handlerInfo.isDoGetMultithreaded()) {
			Runnable task = new Runnable() {
				public void run() {
					try {
						doGet(exchange);
					} catch (IOException ioe) {
						if (LOG.isLoggable(Level.FINE)) {
							LOG.fine("error occured by performing doGet()");
						}
						
						if (exchange.getConnection().isOpen()) {
							exchange.sendError(ioe);
						}
					}
				}
			};
			((AbstractHttpConnection) exchange.getConnection()).getExecutor().processMultithreaded(task);
			
		} else {
			doGet(exchange);
		}
	}
	

	private void callDoPost(final IHttpExchange exchange) throws IOException {
		if (handlerInfo.isDoPostMultithreaded()) {
			Runnable task = new Runnable() {
				public void run() {
					try {
						doPost(exchange);
					} catch (IOException ioe) {
						if (LOG.isLoggable(Level.FINE)) {
							LOG.fine("error occured by performing doPost()");
						}
						
						if (exchange.getConnection().isOpen()) {
							exchange.sendError(ioe);
						}
					}
				}
			};
			((AbstractHttpConnection) exchange.getConnection()).getExecutor().processMultithreaded(task);
			
		} else {
			doPost(exchange);
		}

	}
	
	private void callDoPut(final IHttpExchange exchange) throws IOException {
		if (handlerInfo.isDoPutMultithreaded()) {
			Runnable task = new Runnable() {
				public void run() {
					try {
						doPut(exchange);
					} catch (IOException ioe) {
						if (LOG.isLoggable(Level.FINE)) {
							LOG.fine("error occured by performing doPut()");
						}
						
						if (exchange.getConnection().isOpen()) {
							exchange.sendError(ioe);
						}
					}
				}
			};
			((AbstractHttpConnection) exchange.getConnection()).getExecutor().processMultithreaded(task);
			
		} else {
			doPut(exchange);
		}

	}
	
	private void callDoDelete(final IHttpExchange exchange) throws IOException {
		if (handlerInfo.isDoDeleteMultithreaded()) {
			Runnable task = new Runnable() {
				public void run() {
					try {
						doDelete(exchange);
					} catch (IOException ioe) {
						if (LOG.isLoggable(Level.FINE)) {
							LOG.fine("error occured by performing doDelete()");
						}
						
						if (exchange.getConnection().isOpen()) {
							exchange.sendError(ioe);
						}
					}
				}
			};
			((AbstractHttpConnection) exchange.getConnection()).getExecutor().processMultithreaded(task);
			
		} else {
			doDelete(exchange);
		}

	}
	
	private void callDoHead(final IHttpExchange exchange) throws IOException {
		if (handlerInfo.isDoHeadMultithreaded()) {
			Runnable task = new Runnable() {
				public void run() {
					try {
						doHead(exchange);
					} catch (IOException ioe) {
						if (LOG.isLoggable(Level.FINE)) {
							LOG.fine("error occured by performing doHead()");
						}
						
						if (exchange.getConnection().isOpen()) {
							exchange.sendError(ioe);
						}
					}
				}
			};
			((AbstractHttpConnection) exchange.getConnection()).getExecutor().processMultithreaded(task);
			
		} else {
			doHead(exchange);
		}

	}

	private void callDoOptions(final IHttpExchange exchange) throws IOException {
		if (handlerInfo.isDoOptionsMultithreaded()) {
			Runnable task = new Runnable() {
				public void run() {
					try {
						doOptions(exchange);
					} catch (IOException ioe) {
						if (LOG.isLoggable(Level.FINE)) {
							LOG.fine("error occured by performing doOptions()");
						}
						
						if (exchange.getConnection().isOpen()) {
							exchange.sendError(ioe);
						}
					}
				}
			};
			((AbstractHttpConnection) exchange.getConnection()).getExecutor().processMultithreaded(task);
			
		} else {
			doOptions(exchange);
		}

	}


	private void callDoTrace(final IHttpExchange exchange) throws IOException {
		if (handlerInfo.isDoTraceMultithreaded()) {
			Runnable task = new Runnable() {
				public void run() {
					try {
						doTrace(exchange);
					} catch (IOException ioe) {
						if (LOG.isLoggable(Level.FINE)) {
							LOG.fine("error occured by performing doTrace()");
						}
						
						if (exchange.getConnection().isOpen()) {
							exchange.sendError(ioe);
						}
					}
				}
			};
			((AbstractHttpConnection) exchange.getConnection()).getExecutor().processMultithreaded(task);
			
		} else {
			doTrace(exchange);
		}
	}
	
	
	/**
     * call back which will be called, if a GET request is received 
     * 
     * <p>Overriding this method to support a GET request also
     * automatically supports an HTTP HEAD request. A HEAD
     * request is a GET request that returns no body in the
     * response, only the request header fields. </p>
     * 
     * <p>The GET method should also be idempotent, meaning
     * that it can be safely repeated. </p>
     * 
	 * @param exchange  the exchange
	 * @throws IOException if an exception occurred. By throwing this exception an error http response message 
	 *                     will be sent by xSocket, if one or more requests are unanswered. The underlying 
	 *                     connection will be closed
	 * @throws BadMessageException By throwing this exception an error http response message will be sent by xSocket,
	 *                             which contains the exception message. The underlying connection will be closed                 
	 */
	public void doGet(IHttpExchange exchange) throws IOException, BadMessageException {
	    exchange.forward(exchange.getRequest());
	}

	
	/**
     * call back which will be called, if a POST request is received
     * 
     * <p>This method does not need to be either safe or idempotent.
     * Operations requested through POST can have side effects</p>
     * 
	 * @param exchange  the exchange
	 * @throws IOException if an exception occurred. By throwing this exception an error http response message 
	 *                     will be sent by xSocket, if one or more requests are unanswered. The underlying 
	 *                     connection will be closed
	 * @throws BadMessageException By throwing this exception an error http response message will be sent by xSocket,
	 *                             which contains the exception message. The underlying connection will be closed                 
	 */
	public void doPost(IHttpExchange exchange) throws IOException, BadMessageException {  
	    exchange.forward(exchange.getRequest());
	}
	
	
	/**
     * call back which will be called, if a PUT request is received
     * 
     * <p>This method does not need to be either safe or idempotent.
     * Operations that <code>doPut</code> performs can have side
     * effects</p>
     * 
	 * @param exchange  the exchange
	 * @throws IOException if an exception occurred. By throwing this exception an error http response message 
	 *                     will be sent by xSocket, if one or more requests are unanswered. The underlying 
	 *                     connection will be closed
	 * @throws BadMessageException By throwing this exception an error http response message will be sent by xSocket,
	 *                             which contains the exception message. The underlying connection will be closed                 
	 */
	public void doPut(IHttpExchange exchange) throws IOException, BadMessageException {  
	    exchange.forward(exchange.getRequest());
	}
	
	
	/**
     * call back which will be called, if a DELETE request is received
     * 
     * <p>This method does not need to be either safe
     * or idempotent. Operations requested through
     * DELETE can have side effects</p>
     * 
	 * @param exchange  the exchange
	 * @throws IOException if an exception occurred. By throwing this exception an error http response message 
	 *                     will be sent by xSocket, if one or more requests are unanswered. The underlying 
	 *                     connection will be closed
	 * @throws BadMessageException By throwing this exception an error http response message will be sent by xSocket,
	 *                             which contains the exception message. The underlying connection will be closed                 
	 */
	public void doDelete(IHttpExchange exchange) throws IOException, BadMessageException {  
	    exchange.forward(exchange.getRequest());
	}
	
	
	/**
     * call back which will be called, if a HEAD request is received
     * 
     * <p>The client sends a HEAD request when it wants
     * to see only the headers of a response, such as
     * Content-Type or Content-Length.</p>
     * 
	 * @param exchange  the exchange
	 * @throws IOException if an exception occurred. By throwing this exception an error http response message 
	 *                     will be sent by xSocket, if one or more requests are unanswered. The underlying 
	 *                     connection will be closed
	 * @throws BadMessageException By throwing this exception an error http response message will be sent by xSocket,
	 *                             which contains the exception message. The underlying connection will be closed                 
	 */
	public void doHead(IHttpExchange exchange) throws IOException, BadMessageException {
		doGet(new HeadMethodHttpExchange(exchange));
	}

	
	
	/**
     * call back which will be called, if a OPTIONS request is received
     *
	 * @param exchange  the exchange
	 * @throws IOException if an exception occurred. By throwing this exception an error http response message 
	 *                     will be sent by xSocket, if one or more requests are unanswered. The underlying 
	 *                     connection will be closed
	 * @throws BadMessageException By throwing this exception an error http response message will be sent by xSocket,
	 *                             which contains the exception message. The underlying connection will be closed                 
	 */
	public void doOptions(IHttpExchange exchange) throws IOException, BadMessageException {  
	    exchange.forward(exchange.getRequest());
	}

	
	/**
     * call back which will be called, if a TRACE request is received
     * 
     *
	 * @param exchange  the exchange
	 * @throws IOException if an exception occurred. By throwing this exception an error http response message 
	 *                     will be sent by xSocket, if one or more requests are unanswered. The underlying 
	 *                     connection will be closed
	 * @throws BadMessageException By throwing this exception an error http response message will be sent by xSocket,
	 *                             which contains the exception message. The underlying connection will be closed                 
	 */
	public void doTrace(IHttpExchange exchange) throws IOException, BadMessageException {  
	    exchange.forward(exchange.getRequest());
	}

	
	
	
	private static final class HeadMethodHttpExchange implements IHttpExchange {

		private IHttpExchange delegate = null;
		
		public HeadMethodHttpExchange(IHttpExchange delegate) {
			this.delegate = delegate;
		}
		
		public void destroy() {
			delegate.destroy();
		}

		public BodyDataSink forward(IHttpRequestHeader requestHeader, IHttpResponseHandler responseHandler) throws IOException, ConnectException, IllegalStateException {
			return delegate.forward(requestHeader, responseHandler);
		}

		
		public BodyDataSink forward(IHttpRequestHeader requestHeader) throws IOException, ConnectException, IllegalStateException {
			return delegate.forward(requestHeader);
		}
		
		public BodyDataSink forward(IHttpRequestHeader requestHeader, int contentLength) throws IOException, ConnectException, IllegalStateException {
			return delegate.forward(requestHeader, contentLength);
		}
		
		
		public BodyDataSink forward(IHttpRequestHeader requestHeader, int contentLength, IHttpResponseHandler responseHandler) throws IOException, ConnectException, IllegalStateException {
			return delegate.forward(requestHeader, contentLength, responseHandler);
		}

		public void forward(IHttpRequest request) throws IOException, ConnectException, IllegalStateException {
			delegate.forward(request);
		}

		public void forward(IHttpRequest request, IHttpResponseHandler responseHandler) throws IOException, ConnectException, IllegalStateException {
			delegate.forward(request, responseHandler);
		}
		
		public boolean sendContinue() throws IOException {
		    return delegate.sendContinue();
		}
		
		public boolean sendContinueIfRequested() throws IOException {
		    return delegate.sendContinueIfRequested();
		}

		public IHttpConnection getConnection() {
			return delegate.getConnection();
		}

		public IHttpRequest getRequest() {
			return delegate.getRequest();
		}

		public IHttpSession getSession(boolean create) {
			return delegate.getSession(create);
		}
		
		public String encodeURL(String url) {
			return delegate.encodeURL(url);
		}

		
		public BodyDataSink send(final IHttpResponseHeader header) throws IOException, IllegalStateException {
		
			final BodyDataSink bodyDataSink = newEmtpyBodyDataSink();
			
			IBodyCloseListener closeListener = new IBodyCloseListener() {
				
				@Execution(Execution.NONTHREADED)
				public void onClose() throws IOException {
					delegate.send(new HttpResponse(header));
				}
			};
			
			bodyDataSink.addCloseListener(closeListener);
			return bodyDataSink;
		}

		
		public BodyDataSink send(IHttpResponseHeader header, int contentLength) throws IOException, IllegalStateException {
			return send(header);
		}

		public void sendRedirect(String location) throws IllegalStateException {
		    delegate.sendRedirect(location);
		}
	
		
		public void send(IHttpResponse response) throws IOException, IllegalStateException {
			delegate.send(new HttpResponse(response.getResponseHeader()));
		}

		public void sendError(int errorCode, String msg) throws IllegalStateException {
			delegate.sendError(errorCode, msg);
		}
		
		public void sendError(int errorCode) throws IllegalStateException {
			delegate.sendError(errorCode);
		}

		public void sendError(Exception e) throws IllegalStateException {
			delegate.sendError(e);
		}
		
		
		private BodyDataSink newEmtpyBodyDataSink() throws IOException {
		    return null;
		}
	}	
	
	
	
	

	private static final class HandlerInfo {
		
		private boolean isDoGetMultithreaded = true;
		private boolean isDoGetInvokeOnMessage = false;
		private boolean isDoPostMultithreaded = true;
		private boolean isDoPostInvokeOnMessage = false;
		private boolean isDoPutMultithreaded = true;
		private boolean isDoPutInvokeOnMessage = false;
		private boolean isDoHeadMultithreaded = true;
		private boolean isDoHeadInvokeOnMessage = false;
		private boolean isDoOptionsMultithreaded = true;
		private boolean isDoOptionsInvokeOnMessage = false;
		private boolean isDoTraceMultithreaded = true;
		private boolean isDoTraceInvokeOnMessage = false;
		private boolean isDoDeleteMultithreaded = true;
		private boolean isDoDeleteInvokeOnMessage = false;
		
		
		public HandlerInfo(Class<HttpRequestHandler> clazz) {
			boolean isMultithtreadedDefault = (DEFAULT_EXECUTION_MODE == Execution.MULTITHREADED);
			boolean isInvokeOnMessageDefault = (DEFAULT_INVOKE_ON_MODE == InvokeOn.MESSAGE_RECEIVED);
			
			isDoDeleteMultithreaded = isMethodMultithreaded(clazz, "doDelete", isMultithtreadedDefault);
			isDoDeleteInvokeOnMessage = isOnRequestInvokeOnMessageReceived(clazz, "doDelete", isInvokeOnMessageDefault);
			isDoGetMultithreaded = isMethodMultithreaded(clazz, "doGet", isMultithtreadedDefault);
			isDoGetInvokeOnMessage = isOnRequestInvokeOnMessageReceived(clazz, "doGet", isInvokeOnMessageDefault);
			isDoPostMultithreaded = isMethodMultithreaded(clazz, "doPost", isMultithtreadedDefault);
			isDoPostInvokeOnMessage = isOnRequestInvokeOnMessageReceived(clazz, "doPost", isInvokeOnMessageDefault);
			isDoPutMultithreaded = isMethodMultithreaded(clazz, "doPut", isMultithtreadedDefault);
			isDoPutInvokeOnMessage = isOnRequestInvokeOnMessageReceived(clazz, "doPut", isInvokeOnMessageDefault);
			isDoTraceMultithreaded = isMethodMultithreaded(clazz, "doTrace", isMultithtreadedDefault);
			isDoTraceInvokeOnMessage = isOnRequestInvokeOnMessageReceived(clazz, "doTrace", isInvokeOnMessageDefault);
			isDoOptionsMultithreaded = isMethodMultithreaded(clazz, "doOptions", isMultithtreadedDefault);
			isDoOptionsInvokeOnMessage = isOnRequestInvokeOnMessageReceived(clazz, "doOptions", isInvokeOnMessageDefault);
			isDoHeadMultithreaded = isMethodMultithreaded(clazz, "doHead", isMultithtreadedDefault);
			isDoHeadInvokeOnMessage = isOnRequestInvokeOnMessageReceived(clazz, "doHead", isInvokeOnMessageDefault);
		}
		
		
		private boolean isMethodMultithreaded(Class<HttpRequestHandler> clazz, String methodname, boolean dflt) {
			Execution execution = clazz.getAnnotation(Execution.class);
			if (execution != null) {
				return (execution.value() == Execution.MULTITHREADED);
			}

			try {
				Method meth = clazz.getMethod(methodname, new Class[] { IHttpExchange.class });
				execution = meth.getAnnotation(Execution.class);
				if (execution != null) {
					return (execution.value() == Execution.MULTITHREADED);
				}

			} catch (NoSuchMethodException nsme) {
				if (LOG.isLoggable(Level.FINE)) {
					LOG.fine("shouldn't occure because body handler has to have such a method " + nsme.toString());
				}
			}

			return dflt;
		}
		
		private boolean isOnRequestInvokeOnMessageReceived(Class<HttpRequestHandler> clazz, String methodname, boolean dflt) {

			InvokeOn invokeOn = clazz.getAnnotation(InvokeOn.class);
			if (invokeOn != null) {
				return (invokeOn.value() == Execution.MULTITHREADED);
			}

			try {
				Method meth = clazz.getMethod(methodname, new Class[] { IHttpExchange.class });
				invokeOn = meth.getAnnotation(InvokeOn.class);
				if (invokeOn != null) {
					return (invokeOn.value() == Execution.MULTITHREADED);
				}

			} catch (NoSuchMethodException nsme) {
				if (LOG.isLoggable(Level.FINE)) {
					LOG.fine("shouldn't occure because response handler has to have such a method " + nsme.toString());
				}
			}

			return dflt;
		}


		public boolean isDoGetMultithreaded() {
			return isDoGetMultithreaded;
		}


		public boolean isDoGetInvokeOnMessage() {
			return isDoGetInvokeOnMessage;
		}


		public boolean isDoPostMultithreaded() {
			return isDoPostMultithreaded;
		}


		public boolean isDoPostInvokeOnMessage() {
			return isDoPostInvokeOnMessage;
		}


		public boolean isDoPutMultithreaded() {
			return isDoPutMultithreaded;
		}


		public boolean isDoPutInvokeOnMessage() {
			return isDoPutInvokeOnMessage;
		}


		public boolean isDoHeadMultithreaded() {
			return isDoHeadMultithreaded;
		}


		public boolean isDoHeadInvokeOnMessage() {
			return isDoHeadInvokeOnMessage;
		}


		public boolean isDoOptionsMultithreaded() {
			return isDoOptionsMultithreaded;
		}


		public boolean isDoOptionsInvokeOnMessage() {
			return isDoOptionsInvokeOnMessage;
		}


		public boolean isDoTraceMultithreaded() {
			return isDoTraceMultithreaded;
		}


		public boolean isDoTraceInvokeOnMessage() {
			return isDoTraceInvokeOnMessage;
		}


		public boolean isDoDeleteMultithreaded() {
			return isDoDeleteMultithreaded;
		}


		public boolean isDoDeleteInvokeOnMessage() {
			return isDoDeleteInvokeOnMessage;
		}
	}
}
