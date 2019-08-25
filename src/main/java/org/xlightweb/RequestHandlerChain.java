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
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xlightweb.AbstractHttpConnection.AbstractExchange;
import org.xlightweb.AbstractHttpConnection.RequestHandlerAdapter;
import org.xsocket.Execution;
import org.xsocket.ILifeCycle;




/**
 * Implements a handler chain. The next handler of the chain will be called (in the registering order) by forwarding 
 * the request. See {@link IHttpExchange}. Example:
 * 
 * <pre>
 * &#064Execution(Execution.NONTHREADED)
 * class HeaderLogInterceptor implements IHttpRequestHandler {
 *     
 *    public void onRequest(final IHttpExchange exchange) throws IOException {
 *       
 *       IHttpResponseHandler respHdl = new IHttpResponseHandler() {
 *       
 *          public void onResponse(IHttpResponse response) throws IOException {
 *             System.out.println(response.getResponseHeader());
 *             exchange.send(response);
 *          }
 *          
 *          public void onException(IOException ioe) {
 *             System.out.println(ioe.toString());
 *             exchange.sendError(500);
 *          }
 *       };
 *       
 *       System.out.println(exchange.getRequest().getRequestHeader());
 *       exchange.forward(exchange.getRequest(), respHdl);
 *    }
 * }
 * 
 * 
 * RequestHandlerChain chain = new RequestHandlerChain();
 * chain.addLast(new HeaderLogInterceptor());
 * chain.addLast(new MySeviceHandler());
 * 
 * IServer server = new HttpServer(8080, chain);
 * server.start();
 * </pre>
 * 
 * 
 * @author grro@xlightweb.org
 */
@Supports100Continue
public class RequestHandlerChain implements IHttpRequestHandler, IHttpRequestTimeoutHandler, ILifeCycle, IUnsynchronized {

    private static final Logger LOG = Logger.getLogger(RequestHandlerChain.class.getName());
    
    private final List<IHttpRequestHandler> handlers = new ArrayList<IHttpRequestHandler>();

    private final List<ILifeCycle> lifeCycleChain = new ArrayList<ILifeCycle>();
    
    private List<RequestHandlerAdapter> handlerChain = new ArrayList<RequestHandlerAdapter>();

    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    
    

    /**
     * constructor 
     * 
     */
    public RequestHandlerChain() {
        
    }
    
    
    /**
     * constructor 
     * 
     * @param handlers the initial handlers 
     */
    public RequestHandlerChain(List<IHttpRequestHandler> handlers) {
        for (IHttpRequestHandler hdl : handlers) {
            addLast(hdl);
        }
    }

    
    
    /**
     * add a handler to the top of the chain
     * 
     * @param handler the handler to add
     */
    public void addFirst(IHttpRequestHandler handler) {
        handlers.add(0, handler);
        
        computePath();
        
        if (isInitialized.get()) {
            if (handler instanceof ILifeCycle) {
                ((ILifeCycle) handler).onInit();
            }
        }
    }
    
    
    
    
    /**
     * add a handler to the end of the chain
     * 
     * @param handler the handler to add
     */
    public void addLast(IHttpRequestHandler handler) {
        handlers.add(handler);
        
        computePath();
        
        if (isInitialized.get()) {
            if (handler instanceof ILifeCycle) {
                ((ILifeCycle) handler).onInit();
            }
        }
    }
    
    
    /**
     * removes the handler 
     * 
     * @param handler the handler to remove
     */
    public void remove(IHttpRequestHandler handler) {
        boolean isRemoved = handlers.remove(handler);
        
        computePath();
        
        if (isRemoved && isInitialized.get()) {
            if (handler instanceof ILifeCycle) {
                try {
                    ((ILifeCycle) handler).onDestroy();
                } catch (IOException ioe)  {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Error occured by calling onDestroy on " + handler + " " + ioe.toString());
                    }
                }
            }
        }

    }
    
    
    
    /**
     * removes all handler 
     */
    public void clear() {
        handlers.clear();
        
        computePath();
    }
    
    
    /**
     * returns all handler 
     * 
     * @return the handlers
     */
    public List<IHttpRequestHandler> getHandlers() {
        return Collections.unmodifiableList(handlers);
    }
    
    
    
    private void computePath() {

        lifeCycleChain.clear();
        List<RequestHandlerAdapter> newHandlerChain = new ArrayList<RequestHandlerAdapter>();

        for (IHttpRequestHandler handler : handlers) {
            RequestHandlerAdapter handlerAdapter = new RequestHandlerAdapter(handler);
            newHandlerChain.add(handlerAdapter);
            
            if (handler instanceof ILifeCycle) {
                lifeCycleChain.add((ILifeCycle) handler);
            }
        }
                
        handlerChain = Collections.unmodifiableList(newHandlerChain);
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void onInit() {
        isInitialized.set(true);
        for (ILifeCycle lifeCycle : lifeCycleChain) {
            lifeCycle.onInit();
        }
    }
    

    /**
     * {@inheritDoc}
     */
    public void onDestroy() throws IOException {
        for (ILifeCycle lifeCycle : lifeCycleChain) {
            lifeCycle.onDestroy();
        }       
    }


    
    /**
     * {@inheritDoc}
     */
    @InvokeOn(InvokeOn.HEADER_RECEIVED)
    public void onRequest(final IHttpExchange exchange) throws IOException {

        try {
            if (handlerChain.isEmpty()) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("chain is empty. Forwarding to environment");
                }
                
                exchange.forward(exchange.getRequest(), new HttpResponseHandlerAdapter(exchange));
                
            } else {
                ChainExchange chainExchange = new ChainExchange(exchange, handlerChain, 0, exchange.getRequest(), null);
                chainExchange.handle();
            }
        } catch (IOException ioe) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("error occured by handling request by chain " + this);
            }
            exchange.sendError(ioe);
        }
    }
     
    
    
    
    public boolean onRequestTimeout(final IHttpConnection connection) throws IOException {
       
        if (handlerChain.isEmpty()) {
            return true;
            
        } else {
            for (RequestHandlerAdapter requestAdapter : handlerChain) {
                requestAdapter.onRequestTimeout(connection);
            }
        }
        
        return true;
    }

    
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        
        StringBuilder hc = new StringBuilder(); 
        for (RequestHandlerAdapter handler : handlerChain) {
            hc.append(handler.getDelegate().getClass().getName() + " -> ");
        }
        if (hc.length() > 0) {
            hc.setLength(hc.length() - 4);
        }
        sb.append(" (handlerChain: " + hc.toString());
        
        return sb.toString();
    }
    

    private static final class HttpResponseHandlerAdapter implements IHttpResponseHandler, IUnsynchronized {

        private IHttpExchange exchange = null;
        
        public HttpResponseHandlerAdapter(IHttpExchange exchange) {
            this.exchange = exchange;
        }
        
        public void onResponse(IHttpResponse response) throws IOException {
            exchange.send(response);
        }
        
        public void onException(IOException ioe) {
            exchange.sendError(ioe);
        }
    }    
    

  
    
    
    private final class ChainExchange extends AbstractExchange {
        private final List<RequestHandlerAdapter> chain; 
        private final int num; 
        private final IHttpExchange rootExchange;
        private final IHttpRequest request;
        private final IHttpResponseHandler responseHandler;
        
        // log support
        private RequestHandlerAdapter handler = null;
                
        public ChainExchange(final IHttpExchange rootExchange, List<RequestHandlerAdapter> chain, int num, IHttpRequest request, IHttpResponseHandler responseHandler) {
            super(rootExchange);

            this.rootExchange = rootExchange;
            this.chain = chain;
            this.num = num;
            this.request = request;
            this.responseHandler = responseHandler;
        }



        
        
        public IHttpRequest getRequest() {
            return request;
        }

        
        public IHttpSession getSession(boolean create) {
            return rootExchange.getSession(create);
        }
        

        public String encodeURL(String url) {
            return rootExchange.encodeURL(url);
        }

        
        
        void handle() throws IOException, BadMessageException {
        
            try {
                
                handler = chain.get(num);

                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + chain.hashCode() + "] handling request by chain element (" + num + " of " + chain.size() + ") " + handler);
                }
                
                if (handler.isInvokeOnMessageReceived() && request.hasBody()) {
                    NonBlockingBodyDataSource bodyDataSource = request.getNonBlockingBody();
                    
                    if (!bodyDataSource.isCompleteReceived()) {
                        
                        BodyListener bodyListener = new BodyListener();
                        bodyDataSource.addCompleteListener(bodyListener);
                        bodyDataSource.addDestroyListener(bodyListener);
                        
                    } else {
                        handler.onRequest(this);
                    }
                    
                } else {
                    handler.onRequest(this);
                }
                
                
            } catch (IndexOutOfBoundsException iobe) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + chain.hashCode() + "] end of request chain reached. Forwarding to environment");
                }
                
                rootExchange.forward(request, responseHandler);
            }
        }
        
        
        private final class BodyListener implements IBodyCompleteListener, IBodyDestroyListener {
         
            @Execution(Execution.MULTITHREADED)
            public void onComplete() throws IOException {
                handler.onRequest(ChainExchange.this);
            }
            
            public void onDestroyed() throws IOException {
                rootExchange.destroy();
            }
        }


        
        
        
        
        public void send(IHttpResponse response) throws IOException, IllegalStateException {
            HttpUtils.addConnectionAttribute(response.getResponseHeader(), rootExchange.getConnection());

            if (response.hasBody() && response.getNonBlockingBody().isForwardable()) {
                BodyDataSink dataSink = send(response.getResponseHeader());
                ((IForwardable) response.getNonBlockingBody()).forwardTo(dataSink);
                
            } else { 
                if (responseHandler == null) {
                    rootExchange.send(response);
    
                } else {
                    callResponseHandler(responseHandler, response);
                }
            }
        }
        
        
        public BodyDataSink send(IHttpResponseHeader header, int contentLength) throws IOException, IllegalStateException {
            HttpUtils.addConnectionAttribute(header, rootExchange.getConnection());

            if (header.getContentLength() == -1)  {
                header.setContentLength(contentLength);
            }

            if (responseHandler == null) {
                return rootExchange.send(header, contentLength);
            } else {
                InMemoryBodyDataSink dataSink = new InMemoryBodyDataSink(handler.toString(), header);
                IHttpResponse response = new HttpResponse(header, dataSink.getDataSource());
                send(response);
                
                return dataSink;
            }
        }
     
        
        public BodyDataSink send(IHttpResponseHeader header) throws IOException, IllegalStateException {
            HttpUtils.addConnectionAttribute(header, rootExchange.getConnection());
            
            if (responseHandler == null) {
                return rootExchange.send(header);
            } else {
                InMemoryBodyDataSink dataSink = new InMemoryBodyDataSink(handler.toString(), header);
                IHttpResponse response = new HttpResponse(header, dataSink.getDataSource());
                send(response);
                
                return dataSink;
            }
        }

        
        public void sendError(Exception e) throws IllegalStateException {
            if (responseHandler == null) {
                rootExchange.sendError(e);
            } else {
                IOException ioe = HttpUtils.toIOException(e);
                callResponseHandler(responseHandler, ioe);
            }
        }
        

        public void forward(IHttpRequest request) throws IOException, ConnectException, IllegalStateException {
            forward(request, responseHandler);
        }

                
        public void forward(IHttpRequest request, IHttpResponseHandler responseHandler) throws IOException, ConnectException, IllegalStateException {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + chain.hashCode() + "] request is forwarded (to next handler of chain) by " + handler);
            }
            
            if (responseHandler == null) {
                responseHandler = new DefaultResponseHandler();
            }
            
            if (request.hasBody() && request.getNonBlockingBody().isForwardable()) {
                BodyDataSink dataSink = forward(request.getRequestHeader(), responseHandler);
                ((IForwardable) request.getNonBlockingBody()).forwardTo(dataSink);
                
            } else {
                ChainExchange chainExchange = new ChainExchange(rootExchange, chain, num + 1, request, responseHandler);
                chainExchange.handle();
            }
        }

        public BodyDataSink forward(IHttpRequestHeader requestHeader, IHttpResponseHandler responseHandler) throws IOException, ConnectException, IllegalStateException {
            if (requestHeader.getContentLength() != -1) {
                return forward(requestHeader, requestHeader.getContentLength(), responseHandler);
            }
                             
            if ((requestHeader.getTransferEncoding() == null)) {
                requestHeader.setHeader("Transfer-Encoding", "chunked");
            }
            
            InMemoryBodyDataSink dataSink = new InMemoryBodyDataSink(handler.toString(), requestHeader);
            IHttpRequest request = new HttpRequest(requestHeader, dataSink.getDataSource());
            
            forward(request, responseHandler);
            return dataSink;
        }
        

        public BodyDataSink forward(IHttpRequestHeader requestHeader) throws IOException, ConnectException, IllegalStateException {
            InMemoryBodyDataSink dataSink = new InMemoryBodyDataSink(handler.toString(), requestHeader);
            IHttpRequest request = new HttpRequest(requestHeader, dataSink.getDataSource());
            
            forward(request, responseHandler);
            
            return dataSink;
        }
        

        public BodyDataSink forward(IHttpRequestHeader requestHeader, int contentLength, IHttpResponseHandler responseHandler) throws IOException, ConnectException, IllegalStateException {
            InMemoryBodyDataSink dataSink = new InMemoryBodyDataSink(handler.toString(), requestHeader);
            IHttpRequest request = new HttpRequest(requestHeader, dataSink.getDataSource());
            
            forward(request, responseHandler);
            
            return dataSink;
        }

        
        public BodyDataSink forward(IHttpRequestHeader requestHeader, int contentLength) throws IOException, ConnectException, IllegalStateException {
            InMemoryBodyDataSink dataSink = new InMemoryBodyDataSink(handler.toString(), requestHeader);
            IHttpRequest request = new HttpRequest(requestHeader, dataSink.getDataSource());
            
            forward(request, responseHandler);
            
            return dataSink;
        }
        

        @Supports100Continue
        private final class DefaultResponseHandler implements IHttpResponseHandler {

            public void onResponse(IHttpResponse response) throws IOException {
                rootExchange.send(response);
            }
            
            public void onException(IOException ioe) throws IOException {
                rootExchange.sendError(ioe);                        
            }
        };
    }
}
