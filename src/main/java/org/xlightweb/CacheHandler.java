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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xlightweb.HttpCache.IValidationHandler;
import org.xlightweb.IHttpCache.ICacheEntry;
import org.xsocket.ILifeCycle;



/**
 * Cache handler
 *  
 * Example:
 * <pre>
 *   RequestHandlerChain chain = new RequestHandlerChain();
 *   chain.addLast(new CacheHandler(500));  // add a cache handler with max size 500 KB 
 *   chain.addLast(new FileServiceRequestHandler(basepath));
 *
 *   HttpServer server = new HttpServer(chain);   
 *   //...
 * </pre>  
 * 
 *  HttpClient will add a cache handler automaically, if the cacheMaxSizeKB is set larger than 0  
 *  
 * @author grro@xlightweb.org
 */
@Supports100Continue
public final class CacheHandler implements IHttpRequestHandler, ILifeCycle, IUnsynchronized {

    
	private static final Logger LOG = Logger.getLogger(CacheHandler.class.getName());

	static final String XHEADER_NAME = "X-Cache"; 
	static final String SKIP_CACHE_HANDLING = "org.xlighhtweb.client.cachehandler.skipcachehandling";

	static final String CACHE_HIT = "org.xlighhtweb.client.cachehandler.chachehit";
	
	private final IHttpCache cache;
	
	   
    // statistics
	private final HitStatistics statistics = new HitStatistics();
    private int countCacheHit = 0;
    private int countCacheMiss = 0;
    private long countCacheableResponse = 0;
    private long countNonCacheableResponse = 0;
	
	
	
    /**
     * constructor 
     * 
     * @param maxSizeKB  the max cache size
     */
	public CacheHandler(int maxSizeByteKB) {
	    cache = new HttpCache();
	    cache.setMaxSizeKB(maxSizeByteKB);   
    }
	
	
	/**
	 * {@inheritDoc}
	 */
	public void onInit() {
	}


   /**
     * {@inheritDoc}
     */
	public void onDestroy() throws IOException {
		cache.close();		
	}

	
	/**
	 * set true, if cache is shared
	 * @param isSharedCache true, if cache is shared
	 */
	public void setSharedCache(boolean isSharedCache) {
	    cache.setSharedCache(isSharedCache);
	}
	
	/**
	 * returns true, if cache is shared
	 * @return true, if cache is shared
	 */
	public boolean isSharedCache() {
	    return cache.isSharedCache();
	}
	    
	
	/**
	 * set the max cache size
	 * @param sizeBytes the max cache size
	 */
	public void setMaxCacheSizeKB(int sizeByteKB) {
	    cache.setMaxSizeKB(sizeByteKB);
	}
	
	/**
	 * return the max cache size
	 * @return the max cache size
	 */
	public int getMaxCacheSizeKB() {
	    return cache.getMaxSizeKB(); 
	}
	
	
	/**
	 * return the current cache size
	 * @return the current cache size
	 */
	public int getCurrentCacheSizeBytes() {
        return cache.getCurrentSize(); 
    }
	
	
	/**
	 * returns the number of cache hits
	 * @return the number of cache hits
	 */
	public int getCountCacheHit() {
	    return countCacheHit;
	}
		
	/**
	 * returns the current hit ratio 
	 * @return the current hit ratio
	 */
	public double getCurrentHitRatio() {
	    return statistics.getHitRate();
	}
	
	

	/**
	 * returns the number of cache misses
	 * @return the number of cache misses
	 */
	public int getCountCacheMiss() {
        return countCacheMiss;
    }
	
	long getCountCacheableResponse() {
	    return countCacheableResponse;
	}
	
	long getNonCountCacheableResponse() {
        return countNonCacheableResponse;
    }
	
	
	/**
	 * return the cache info
	 * @return the cache info
	 */
	public List<String> getCacheInfo() {
	    List<String> result = new ArrayList<String>();
	    for (ICacheEntry entry : cache.getEntries()) {
	        result.add(entry.toString());
	    }
	    
	    return result;
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public void onRequest(final IHttpExchange exchange) throws IOException {

	    final IHttpRequest request = exchange.getRequest();

	    
	    // skip cache handling?
	    if ((request.getAttribute(SKIP_CACHE_HANDLING) != null) && (request.getAttribute(SKIP_CACHE_HANDLING).equals("true"))) {
	       exchange.forward(request); 
	       return;
	    }

	    // is request not cacheable?
	    if (!HttpCache.isCacheable(request, isSharedCache())) {
	        
	        exchange.forward(request);
	        
	        // TODO: remove existing entry if PUT, POST or DELETE
	        return;
	    }

	    
	    Date minFresh = new Date();
        Date maxOld = null;
        boolean isOnlyIfCached = false;
	    
	    // handle requests cache control directive
	    String cacheControl = request.getHeader("Cache-Control");
	    
	    if (cacheControl != null) {
	        for (String directive : cacheControl.split(",")) {
                directive = directive.trim();
                String directiveLower = directive.toLowerCase();
                
                if (directive.equalsIgnoreCase("no-cache") || directive.equalsIgnoreCase("no-store")) {
                    exchange.forward(request);
                    return;
                } 
   
                
                if (directiveLower.startsWith("min-fresh=")) {
                    String minRefresh = directive.substring("min-fresh=".length(), directive.length()).trim();
                    minFresh = new Date(System.currentTimeMillis() + HttpUtils.parseLong(minRefresh, 0));
                }       
                
                if (directiveLower.startsWith("max-stale")) {
                    if (directive.length() > "max-stale=".length()) {
                        String maxStale = directive.substring("max-stale=".length(), directive.length()).trim();
                        minFresh = new Date(System.currentTimeMillis() - (1000L * (HttpUtils.parseLong(maxStale, 365 * 24 * 60 * 60))));
                    } else {
                        minFresh = new Date(System.currentTimeMillis() - (1000L * (365L * 24L * 60L * 60L)));
                    }
                }
                
                if (directiveLower.startsWith("max-age=")) {
                    String maxAge = directive.substring("max-age=".length(), directive.length()).trim();
                    maxOld = new Date(System.currentTimeMillis() - (1000L * HttpUtils.parseLong(maxAge, 0)));
                }

                
                if (directive.equalsIgnoreCase("only-if-cached")) {
                    isOnlyIfCached = true;
                }
	        }
	    }
	    
	       
	    // handle caching
	    try {
            ICacheEntry ce = cache.get(request, minFresh);
 
            if ((ce != null) && !(ce.isAfter(maxOld))) {
                
                // must revalidate?
                if (ce.mustRevalidate(minFresh)) {
                    
                    if (isOnlyIfCached) {
                        countCacheMiss++;
                        statistics.addMiss();
                        
                        exchange.sendError(504);
                        
                    } else {
                        IValidationHandler validationHdl = new IValidationHandler() {
                            
                            public void onRevalidated(boolean isNotModified, HttpCache.AbstractCacheEntry ce) {
                                
                                if (isNotModified) {
                                    try {
                                        countCacheHit++;
                                        statistics.addHit();
                                        
                                        IHttpResponse resp = ce.newResponse();
                                        resp.setHeader(XHEADER_NAME, "HIT - revalidated (xLightweb)");
                                        resp.setAttribute(CACHE_HIT, "HIT (revalidated)");
                                        exchange.send(resp);
                                    } catch (IOException ioe) {
                                        exchange.sendError(ioe);
                                    }
                                } else  {
                                    try {
                                        countCacheMiss++;
                                        statistics.addMiss();
                                        
                                        IHttpResponse resp = ce.newResponse();
                                        exchange.send(resp);
                                    } catch (IOException ioe) {
                                        exchange.sendError(ioe);
                                    }
                                }
                            }
                            
                            public void onException(IOException ioe) {
                                exchange.sendError(ioe);
                            }
                            
                        };

                        ce.revalidate(exchange, validationHdl);
                    }
                  
                // .. revalidation is not required 
                } else {
                    countCacheHit++;
                    statistics.addHit();
                    IHttpResponse resp = ce.newResponse();
                    resp.setHeader(XHEADER_NAME, "HIT  (xLightweb)");
                    resp.setAttribute(CACHE_HIT, "HIT");

                    exchange.send(resp);
                }
                
                
            // no, forward request and intercept response
            } else {  
                countCacheMiss++;
                statistics.addMiss();

                if (isOnlyIfCached) {
                    exchange.sendError(504);
                } else {
                    forwardForCache(exchange);
                }
            }
            
        } catch (IOException ioe) {
            exchange.sendError(ioe);
        }
	}	
	
	
	
	private void forwardForCache(final IHttpExchange exchange) throws IOException {
	
	    IHttpRequest request = exchange.getRequest();
	    IHttpRequestHeader headerCopy = request.getRequestHeader().copy();	    	    
	    	    
	    ForwarderResponseHandler forwardResponseHandler = new ForwarderResponseHandler(exchange, headerCopy);
	    exchange.forward(exchange.getRequest(), forwardResponseHandler);
	}
	
	
	
    
    private final class ForwarderResponseHandler implements IHttpResponseHandler, IUnsynchronized {
        
        private final IHttpExchange exchange;
        private final IHttpRequest request;
        private final long startTime;
        
        
        public ForwarderResponseHandler(IHttpExchange exchange, IHttpRequestHeader headerCopy) {
            assert (HttpCache.isCacheable(exchange.getRequest(), isSharedCache()));
            
            this.exchange = exchange;
            this.request = new HttpRequest(headerCopy);
            this.startTime = System.currentTimeMillis();
        }
        
            
        @InvokeOn(InvokeOn.HEADER_RECEIVED)
        public void onResponse(IHttpResponse response) throws IOException {

            // is response cacheable?
            if (HttpCache.isCacheable(response, isSharedCache())) {
                final IHttpResponseHeader responseHeaderCopy = response.getResponseHeader().copy();
                responseHeaderCopy.removeHopByHopHeaders();
                responseHeaderCopy.removeHeader("Content-Length");
                responseHeaderCopy.removeHeader("Transfer-Encoding");
                
                if (response.hasBody()) {
                    final NonBlockingBodyDataSource dataSource = response.getNonBlockingBody();
                    BodyDataSink dataSink = exchange.send(response.getResponseHeader());

                    BodyForwarder bodyForwarder = new BodyForwarder(response.getMessageHeader(), dataSource, dataSink, null) {

                        private final List<ByteBuffer> responseBodyCopy = new ArrayList<ByteBuffer>();
                        
                        @Override
                        public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException, IOException {
                            
                            int available = bodyDataSource.available();
                            if (available > 0) {
                                ByteBuffer[] data = bodyDataSource.readByteBufferByLength(bodyDataSource.available());
                               
                                
                                for (ByteBuffer buf : data) {
                                    responseBodyCopy.add(buf.duplicate());
                                }
                                
                                bodyDataSink.write(data);
                            }
                        }
                        
                        
                        @Override
                        public void onComplete() {
                            try {
                                IHttpResponse responseCopy = new HttpResponse(responseHeaderCopy, responseBodyCopy);
                                addToCache(responseCopy);
                                
                            } catch (IOException ioe) {
                                if (LOG.isLoggable(Level.FINE)) {
                                    LOG.fine("error occured by creating/registering cachedResponse " + ioe.toString());
                                }
                            }
                        }
                    };

                    dataSource.setDataHandler(bodyForwarder);
                    
                } else {
                    IHttpResponse responseCopy = new HttpResponse(responseHeaderCopy);
                    addToCache(responseCopy);
                    exchange.send(response);
                }


            // response is not cacheable 
            } else {
                countNonCacheableResponse++;
                exchange.send(response);
                return;                
            }
        }
        
       
        private void addToCache(IHttpResponse response) {        
            try {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("adding interaction " + request.getRequestUrl().toString() +  " - " + response.getStatus() + " to cache");
                }
                countCacheableResponse++;
                cache.register(request, System.currentTimeMillis() - startTime, response);
            
            } catch (IOException ioe) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("error occured by adding interaction to cache " + ioe.toString());
                }
            }
        }       

        
            
        public void onException(IOException ioe) throws IOException {
            exchange.sendError(ioe);
        }
    }
    
    
    
    private static final class HitStatistics {
        private static final int WINDOWS_SIZE = 500;
        private static final int MAX_POINTER_VALUE = WINDOWS_SIZE - 2;
        
        private final Boolean[] measures = new Boolean[WINDOWS_SIZE];
        private int pointer = 0;
        
        void addHit() {
            try {
                measures[pointer] = true;
                incPointer();
            } catch (Exception ignore) { }
        }
        
        
        void addMiss() {
            try {
                measures[pointer] = false;
                incPointer();
            } catch (Exception ignore) { }
        }
        
        
        private void incPointer() {
            if (pointer < MAX_POINTER_VALUE) {
                pointer++;
            }
        }

        double getHitRate() {
            int hits = 0;
            int misses = 0;
         
            for (int i = 0; i < measures.length; i++) {
                Boolean b = measures[i];
                if (b != null) {
                    if (b) {
                        hits++;
                    } else {
                        misses++;
                    }
                }
            }
            
            return ratio(hits, misses);
        }
        
        
        
        private double ratio(int hits, int misses) {
            if (misses == 0) {
                return 100;
            } 
            
            if (hits == 0) {
                return 0;
            }
            
            return ((double) hits / (double) misses); 
        }
    }
}
