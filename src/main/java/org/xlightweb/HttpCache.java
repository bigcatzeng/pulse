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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xsocket.DataConverter;
import org.xsocket.Execution;




/**
 * cache 
 *  
 * @author grro@xlightweb.org
 */
final class HttpCache implements IHttpCache {
    
    private static final Logger LOG = Logger.getLogger(HttpCache.class.getName());

    private static final long TIMERTASK_PERIOD_MILLIS = Long.parseLong(System.getProperty("org.xlightweb.httpchache.checkperiodMillis", "60000"));
    private final TimerTask timerTask; 


    private final Cache cache = new Cache(); 


    private boolean isSharedCache = true;
    
    
    
    
    
     
    public HttpCache() {
        timerTask = new TimerTask() {
            public void run() {
                cache.periodicChecks();
            }
        };
                
        AbstractHttpConnection.schedule(timerTask, TIMERTASK_PERIOD_MILLIS, TIMERTASK_PERIOD_MILLIS);            
    }
    
    
    public void setSharedCache(boolean isSharedCache) {
        this.isSharedCache = isSharedCache;
        cache.clear();
    }
    
    public boolean isSharedCache() {
        return isSharedCache;
    }
	    
    public void setMaxSizeKB(int sizeBytesKB) {
        cache.setMaxSizeBytes(sizeBytesKB * 1024);
    }
    
    public int getMaxSizeKB() {
        int size = cache.getMaxSizeBytes();
        if (size > 0) {
            size = size / 1024;
        }
        return size;
    }
    
    
    public int getMaxSizeCacheEntry() {
        return cache.getMaxSizeCacheEntry();
    }

    public int getCurrentSize() {
        return cache.getCurrentSize();
    }
    
    public Collection<ICacheEntry> getEntries() {
        return cache.getEntries();
    }
    
    public void close() throws IOException {
        cache.close();
        timerTask.cancel();
    }
    
    
    public static boolean isCacheable(IHttpRequest request, boolean isSharedCache) {
        
        try {
            // do not cache secured (SSL) transactions for shared caches
            if (isSharedCache && request.isSecure()) {
                return false;
            }
            
            // cache http/1.1 transactions only
            if (!request.getProtocolVersion().equals("1.1")) {
                return false;
            }
            
            // If the request contains an "Authorization:" header, the response will not be cached if shared
            if (isSharedCache && (request.getHeader("Authorization") != null)) {
                return false;
            }
            
            // If the request contains an "Cookie:" header, the response will not be cached if shared
            if (isSharedCache && (request.getHeader("Cookie") != null)) {
                return false;
            }

            
            // only GET is supported
            if (!request.getMethod().equalsIgnoreCase("GET")) {
               return false;
            }
    
            // do not handle validation based cache request (ETag, Modification-Date) -> could be done in the future
            if ((request.getHeader("If-None-Match") != null) || (request.getHeader("If-Modified-Since") != null)) {
                return false;         
            }
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean isCacheable(IHttpResponse response, boolean isSharedCache) {
        
        try {
            // cache only specific response status
            if (!isCacheableSuccess(response.getStatus()) && !isCacheableRedirect(response.getStatus())) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("non-cacheable response received (status: " + response.getStatus() + ")");
                }
    
                return false;
            }
    
            
            if (!(response.getProtocolVersion().equals("1.0") || response.getProtocolVersion().equals("1.1"))) {
                return false;
            }

            
            // do not cache pragma header 'no-cache'
            String pragmaHeader = response.getHeader("Pragma");
            if ((pragmaHeader != null) && (pragmaHeader.equalsIgnoreCase("no-cache"))) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("non-cacheable response received (Pragma: no-cache)");
                }
    
                return false;
            }
    

            // do not cache Set-cookie response 
            if (response.getHeader("Set-Cookie") != null) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("non-cacheable response received (Set-Cookie)");
                }
                return false;
            }
            
            
            // check expired based            
            String expires = response.getHeader("Expires");
            if (expires != null) {
                Date date = HttpUtils.parseHttpDateString(expires);
                if (date == null) {
                    return false; 
                } else if (date.getTime() < System.currentTimeMillis()) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("non-cacheable response received (Expires: " + expires + ")");
                    }
    
                    return false;
                }
            }
            
            
            // check cache control header            
            String cacheControl = response.getHeader("Cache-Control");
            if (cacheControl != null) {
                for (String directive : cacheControl.split(",")) {
                    directive = directive.trim();
                    if (directive.equalsIgnoreCase("no-cache") || directive.equalsIgnoreCase("no-store")) {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("non-cacheable response received (Cache-Control: " + cacheControl + ")");
                        }
    
                        return false;
                    } 
                    
                    if (isSharedCache && directive.equalsIgnoreCase("private")) {
                        return false;
                    }
                }
            }
            
            
            // do not cache response contains vary 
            if (response.getHeader("Vary") != null) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("non-cacheable response received (includes Vary header)");
                }
    
                return false;
            }
            
            
            // do not cache text/event-stream
            if (HttpUtils.hasContentType(response.getPartHeader(), "text/event-stream")) {
                return false;
            }
            
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    
    
    private static boolean isCacheableSuccess(int statusCode) {
        return ((statusCode >= 200) && (statusCode <= 201));
    }
    
    private static boolean isCacheableRedirect(int statusCode) {
        return ((statusCode >= 301) && (statusCode <= 302));
    }

    
	    
    public ICacheEntry get(IHttpRequest request, Date minFresh) throws IOException {
        ICacheEntry ce = null;
        
        synchronized (this) {
            ce = cache.getEntry(request);
        }
        
        if (ce != null) {
            if (ce.isExpired(minFresh)) {
                return null;
                
            } else {                
                return ce;
            }
        } else {
            return null;
        }
            
    }

        
    
      
    private ICacheEntry newCacheEntry(IHttpRequest request, long networkLatency, IHttpResponse response) {
        try {
            
            ///////////////////////////////
            // CHECK EXPIRED BASED 
            String cacheControl = response.getHeader("Cache-Control");
            
            // handle cache-control header 
            if (cacheControl != null) {
                ICacheEntry cacheEntry = new CacheControlBasedCacheEntry(request, response, networkLatency, cacheControl, isSharedCache); 
                if (cacheEntry.isExpired(new Date())) {
                    return null;
                } else {
                    return cacheEntry;
                }
            }

            
            // handler expires header 
            String expire = response.getHeader("Expires");
            if (expire != null) {
                ICacheEntry cacheEntry = new ExpiresBasedCacheEntry(request, response, networkLatency, expire);
                if (cacheEntry.isExpired(new Date())) {
                    return null;
                } else {
                    return cacheEntry;
                }
            }

           
            ///////////////////////////////
            // CHECK VALIDATION BASED
            String eTag = response.getHeader("ETag");
            String lastModified = response.getHeader("Last-Modified");
            if ((eTag != null) || (lastModified != null)) {
                ValidationBasedCacheEntry cacheEntry = new ValidationBasedCacheEntry(request, response);
                if (cacheEntry.isExpired(new Date())) {
                    return null;
                } else {
                    return cacheEntry;
                }
            }

            
            //////////////////////////////////
            // DEFAULT Handling
            
            
            // handle permantent redirect
            if (response.getStatus() == 301)  {
                return new ExpiresBasedCacheEntry(request, response, new Date(System.currentTimeMillis() + (30L * 24L * 60L * 60L * 1000L)));
            }

            
        } catch (Exception e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("error occured by checking if cacheable" + e.toString());
            }
        }
        
        return null;
    }
    
    
    public void register(IHttpRequest request, long networkLatency, IHttpResponse response) throws IOException {
        ICacheEntry ce = newCacheEntry(request, networkLatency, response);
        register(ce);
    }
    
    
    private void register(ICacheEntry ce) throws IOException {
        if (ce == null) {
            return;
        }
        
        cache.putEntry(ce);
    }
    
    
    public void deregister(IHttpRequest request) throws IOException {
        cache.removeEntry(request);
    }

    
    @Override
    public String toString() {
        return cache.toString();
    }
    
    
    private static Date computeExpireDate(String expire, long networkLatency) {
        if ((HttpUtils.parseHttpDateString(expire).getTime() - networkLatency) > 0) { 
            return new Date(HttpUtils.parseHttpDateString(expire).getTime() - networkLatency);
        } else {
            return new Date(0);
        }
    }

  
    
    private static final class Cache extends LinkedHashMap<String, ICacheEntry> {

        private static final long serialVersionUID = -4920963130585126603L;

        private static final ICacheEntry DUMMY_CACHE_ENTRY = new DummyCacheEntry(5000);
        
        private static final String SEPARATOR = "*"; 

        private static final int DEFAULT_ENTRY_SIZE_THRESHOLD_PERCENT = 10;  
        private int entrySizeThresholdPercent = DEFAULT_ENTRY_SIZE_THRESHOLD_PERCENT; 

        
        private int currentSize = 0;
        private int maxSize = 0;


        public synchronized ICacheEntry putEntry(ICacheEntry entry) throws IOException {
            assert (entry.getRequest().getMethod().equalsIgnoreCase("GET"));
            
            int size = entry.getSize();
            
            // very large entries will not be cached 
            if (size > getMaxSizeCacheEntry()) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("entry " + entry.getRequest().getRequestUrl().toString() + " will not be cached (is to large: " + entry.getSize() + " bytes; supproted max entry size: " + getMaxSizeCacheEntry() + " bytes)");
                }
                return null;
            }

            ICacheEntry removed = super.put(computeFingerprint(entry.getRequest()), entry);
            currentSize += size;
          
            
            // if a larger entry is added and a smaller one is removed (eldest), the current size can be higher than the max size
            while(currentSize > maxSize) {
                try {
                    ICacheEntry rem = super.put("DUMMY_ENTRY", DUMMY_CACHE_ENTRY);  // put dummy entry which cause removing the oldest entry
                    if (rem != null) {
                        // should not happen. Anyway, break loop
                        break;
                    }
                } finally {
                    super.remove("DUMMY_ENTRY");
                }
            }
            
            
            return removed;
        }
        
        
        public synchronized ICacheEntry getEntry(IHttpRequest request) throws IOException {
            return super.get(computeFingerprint(request));
        }
        
        
        public synchronized ICacheEntry removeEntry(IHttpRequest request) throws IOException {
            ICacheEntry removed = super.remove(computeFingerprint(request));
            if (removed != null) {
                currentSize -= removed.getSize();
            }
            
            return removed;
        }
        
        
        
        public void setMaxSizeBytes(int sizeBytes) {
            maxSize = sizeBytes;
        }
        
        public int getMaxSizeBytes() {
            return maxSize;
        }

        
        int getMaxSizeCacheEntry() {
            return (maxSize / entrySizeThresholdPercent);
        }
        
        
        public synchronized int getCurrentSize() {
            return currentSize;
        }

        private synchronized void setCurrentSize(int currentSize) {
            this.currentSize = currentSize;
        }

        
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, ICacheEntry> eldest) {
            
            if (currentSize > maxSize) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("removing eldest entry (current size " + currentSize + " is larger than max size " + maxSize + ")");
                }
                
                currentSize -= eldest.getValue().getSize();
                return true;
            }
            
            return false;
        }
        
        
        @SuppressWarnings("unchecked")
        public Collection<ICacheEntry> getEntries() {
            LinkedHashMap<String, ICacheEntry> copy;

            synchronized (this) {
                copy = (LinkedHashMap<String, ICacheEntry>) super.clone();
            }
            
            return copy.values();
        }
        
        
        public synchronized void close() throws IOException {
            clear();
        }
        
        
        
        private String computeFingerprint(IHttpRequest request) throws IOException {

            StringBuilder sb = new StringBuilder(request.getRequestUrl().toString());

            List<String> headers = new ArrayList<String>(request.getHeaderNameSet());
            Collections.sort(headers);

            for (String header : headers) {
                if (header.equalsIgnoreCase("User-Agent") ||
                    header.equalsIgnoreCase("Referer") ||
                    header.equalsIgnoreCase("Cache-Control") ||
                    header.equalsIgnoreCase("Connection") ||
                    header.equalsIgnoreCase("Keep-Alive") ||
                    header.equalsIgnoreCase("Proxy-Authenticate") ||
                    header.equalsIgnoreCase("Proxy-Authorization") ||
                    header.equalsIgnoreCase("TE"))  {
                    continue;
                }
                
                
                sb.append(header + SEPARATOR);
                
                for (String value : request.getHeaderList(header)) {
                    sb.append(value + SEPARATOR);
                }
            }

            if (request.hasBody()) {
                sb.append(request.getNonBlockingBody().toString());
            }
            
            return sb.toString();
        }
        
        
        void periodicChecks() {
            Date currentDate = new Date();
            
            int size = 0;
            for (ICacheEntry cacheEntry : getEntries()) {
                try {
                    if (cacheEntry.isExpired(currentDate)) {
                        removeEntry(cacheEntry.getRequest());
                    } else {
                        size += cacheEntry.getSize();
                    }
                } catch (IOException ioe) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("could not vaildate/remove cache entry " + cacheEntry.getRequest().getRequestUrl().toString() + " " + ioe.toString());
                    }
                }
            }
            
            setCurrentSize(size);
        }
        
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            
            for (ICacheEntry entry : getEntries()) {
                sb.append(entry.toString() + "\r\n");
            }
            return sb.toString();
        }
    };
    
   
    
   


    
  
    interface IValidationHandler {
        
        void onRevalidated(boolean isNotModified, AbstractCacheEntry ce);
        
        void onException(IOException ioe);
    }
	
    
    
    
    abstract class AbstractCacheEntry implements ICacheEntry {
        
        private final Date cacheDate;

        private final IHttpRequest request;
        private final int sizeRequest;
        
        private IHttpResponse response;
        private int sizeResponse;

        
        
        public AbstractCacheEntry(IHttpRequest request, IHttpResponse response) throws IOException {
            this.cacheDate = new Date();
            
            this.request = request;

            int i = request.getRequestHeader().toString().length();
            if (request.hasBody()) {
                i += request.getNonBlockingBody().available();
            } 
            this.sizeRequest = i; 
                
            setHttpResponse(response);
        }
        
        
        public final IHttpRequest getRequest() {
            return request;
        }
        
    
        public final IHttpResponse getResponse() {
            return response;
        }
    
        public final void setHttpResponse(IHttpResponse response) throws IOException {
            this.response = response;
                        
            int i = response.getResponseHeader().toString().length();
            if (response.hasBody()) {
                i += response.getNonBlockingBody().available();
            } 
            this.sizeResponse = i; 
        }
        
        public final int getSize() {
            return sizeRequest + sizeResponse;
        }
    
        public final Date getCacheDate() {
            return cacheDate;
        }
        
        public final long getAgeMillis() {
            return (System.currentTimeMillis() - getCacheDate().getTime());
        }
        
        
        public final boolean isAfter(Date date) {
            if (date == null) {
                return false;
            }
            
            if (date.after(cacheDate)) {
                return true;
            }
            
            return false;
        }
        
        
        public final IHttpResponse newResponse() throws IOException {
            IHttpResponse response = HttpUtils.copy(getResponse());
            enhanceCachedResponse(response);
            return response;
        }
        
        
        @Override
        public String toString() {
            return getRequest().getRequestUrl().toString() + " - " + getResponse().getStatus() +
                   " (" + getType() + ", size: " + DataConverter.toFormatedBytesSize(getSize()) + 
                   ", age: " + DataConverter.toFormatedDuration(getAgeMillis()) + ")";
        }
        
        abstract void enhanceCachedResponse(IHttpResponse response);
    }
    
    
    

    private final class ExpiresBasedCacheEntry extends AbstractCacheEntry {
            
        private final Date expireDate;

        
        public ExpiresBasedCacheEntry(IHttpRequest request, IHttpResponse response, long networkLatency, String expire) throws IOException {
            this(request, response, computeExpireDate(expire, networkLatency));
        }
        
        public ExpiresBasedCacheEntry(IHttpRequest request, IHttpResponse response, Date expireDate) throws IOException {
            super(request, response);
            this.expireDate = expireDate;
        }

        
        public String getType() {
            return "ExpiredBased - " + DataConverter.toFormatedDuration(expireDate.getTime() - getCacheDate().getTime());
        }
        
        
        @Override
        void enhanceCachedResponse(IHttpResponse response) {
            

        }
        

        public boolean isExpired(Date currentDate) {
            if (currentDate.after(expireDate)) {
                return true;
            }
                
            return false;
        }        
        
        public boolean mustRevalidate(Date currentDate) {
            return false;
        }
        
        public void revalidate(IHttpExchange exchange, IValidationHandler hdl) throws IOException {
            throw new IOException("illegal state");
        }
    }
    
    
    
    
    private final class CacheControlBasedCacheEntry extends AbstractCacheEntry {
       
        private Date expireDate = new Date(0);
        private final boolean isShared;
        
        
        public CacheControlBasedCacheEntry(IHttpRequest request, IHttpResponse response, long networkLatency, String cacheControl, boolean isShared) throws IOException {
            super(request, response);
            
            this.isShared = isShared;
            
            for (String directive : cacheControl.split(",")) {
                directive = directive.trim();
                if (directive.equalsIgnoreCase("no-cache") || directive.equalsIgnoreCase("no-store")) {
                    expireDate = new Date(0);
                    return;
                } 
                
                if (isShared && directive.equalsIgnoreCase("private")) {
                    expireDate = new Date(0);
                    return;
                }
                
                if (directive.toLowerCase().startsWith("max-age=")) {
                    long maxAgeMillis = Long.parseLong(directive.substring("max-age=".length(), directive.length())) * 1000L;
                    
                    if ((maxAgeMillis - networkLatency) > 0) {
                        expireDate = new Date(System.currentTimeMillis() + (maxAgeMillis - networkLatency));
                    } else {
                        expireDate = new Date(0);
                    }
                }
                

                // revalidation isnot supported
                if (directive.equalsIgnoreCase("must-revalidate") || (directive.equalsIgnoreCase("proxy-revalidate"))) {
                    expireDate = new Date(0);
                }
            }
        }

        
        public String getType() {
            return "CacheControl - " + DataConverter.toFormatedDuration(expireDate.getTime() - getCacheDate().getTime());
        }
        
        
        @Override
        void enhanceCachedResponse(IHttpResponse response) {
            String cacheControl = response.getHeader("Cache-Control");
            if (cacheControl != null) {
                StringBuilder sb = new StringBuilder();
                for (String entry : cacheControl.split(",")) {
                    entry = entry.trim();
                    
                    if (isShared) {
                        if (entry.toLowerCase().startsWith("max-age=")) {
                            entry = "max-age=" + ((expireDate.getTime() - System.currentTimeMillis()) / 1000);
                        }
                        
                    } else {
                        if (entry.toLowerCase().startsWith("s-maxage=")) {
                            entry = "s-maxage=" + ((expireDate.getTime() - System.currentTimeMillis()) / 1000);
                        }
                    }

                    
                    sb.append(entry + ", ");
                }
                if (sb.length() > 0) {
                    sb.setLength(sb.length() - 2);
                }
                response.setHeader("Cache-Control", sb.toString());
            }            

        }
        

        public boolean isExpired(Date currentDate) {
            if (currentDate.after(expireDate)) {
                return true;
            }
                
            return false;
        }        
        
        public boolean mustRevalidate(Date currentDate) {
            return false;
        }
        
        
        public void revalidate(IHttpExchange exchange, IValidationHandler hdl) throws IOException {
            throw new IOException("illegal state");
        }
    }
    
    
    
    private final class ValidationBasedCacheEntry extends AbstractCacheEntry {
        
        public ValidationBasedCacheEntry(IHttpRequest request, IHttpResponse response) throws IOException {
            super(request, response);
        }
        
        
        public String getType() {
            StringBuilder sb = new StringBuilder("ValidationBased -");
            if (getResponse().getHeader("Etag") != null) {
                sb.append(" Etag");
            }
            if (getResponse().getHeader("Last-Modified") != null) {
                sb.append(" Last-Modified");
            }
            
            return sb.toString();
        }
        
        
        public boolean isExpired(Date currentDate) {
            return false;
        }
        
        public boolean mustRevalidate(Date currentDate) {
            return true;
        }
        
        @Override
        void enhanceCachedResponse(IHttpResponse response) {
         
        }
        
        
        public void revalidate(final IHttpExchange exchange, final IValidationHandler hdl) throws IOException {
            
            IHttpRequest requestCopy = HttpUtils.copy(getRequest());
            
            if (getResponse().getHeader("Etag") != null) {
                requestCopy.setHeader("IF-None-Match", getResponse().getHeader("Etag"));
                
            } else {
                requestCopy.setHeader("If-Modified-Since", getResponse().getHeader("Last-Modified"));
            }            
            requestCopy.setAttribute(CacheHandler.SKIP_CACHE_HANDLING, "true");

            
            IHttpResponseHandler respHdl = new IHttpResponseHandler() {
                
                @Execution(Execution.NONTHREADED)
                @InvokeOn(InvokeOn.MESSAGE_RECEIVED)
                public void onResponse(IHttpResponse resp) throws IOException {
                                        
                    if (resp.getStatus() == 304) {
                        
                        /* RFC 2616:
                         * If a cache uses a received 304 response to update a cache entry, the
                         * cache MUST update the entry to reflect any new field values given in
                         * the response.
                         */
                        
                        resp.removeHopByHopHeaders();
                        resp.removeHeader("Transfer-Encoding");
                        resp.removeHeader("Content-Length");
                        resp.removeHeader("Content-Type");
                        
                        for (String headername : resp.getHeaderNameSet()) {
                            getResponse().removeHeader(headername);
                            
                            for (String headervalue : resp.getHeaderList(headername)) {
                                getResponse().addHeader(headername, headervalue);
                            }
                        }
                        
                        hdl.onRevalidated(true, ValidationBasedCacheEntry.this);
                        
                    } else {
                        
                        if (isCacheableSuccess(resp.getStatus())) {
                            setHttpResponse(resp);
                        } else {
                            deregister(getRequest());
                        }
                        
                        hdl.onRevalidated(false, ValidationBasedCacheEntry.this);
                    }
                }
                
                
                public void onException(IOException ioe) throws IOException {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("got exception by revalidating "+ ioe.toString());
                    }
                    deregister(getRequest());
                    
                    hdl.onException(ioe);
                }
            };

            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("revalidating request " + requestCopy.getRequestUrl().toString());
            }
            
            exchange.forward(requestCopy, respHdl);
        }
    }
    
    
    private static final class DummyCacheEntry implements ICacheEntry {

        private final int size;
        
        public DummyCacheEntry(int size) {
            this.size = size;
        }
        
        public int getSize() {
            return size;
        }
        
        public long getAgeMillis() {
            return 0;
        }
        
        public IHttpRequest getRequest() {
            return null;
        }
        
        public IHttpResponse getResponse() {
            return null;
        }
        
        public String getType() {
            return null;
        }
        
        public boolean isAfter(Date data) {
            return false;
        }
        
        public boolean isExpired(Date currentDate) {
            return false;
        }
        
        public boolean mustRevalidate(Date currentDate) {
            return false;
        }
        
        public IHttpResponse newResponse() throws IOException {
            return null;
        }
        
        public void revalidate(IHttpExchange exchange, IValidationHandler hdl) throws IOException {
        }
    }
}
