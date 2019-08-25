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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xlightweb.AbstractHttpConnection.IMultimodeExecutor;
import org.xsocket.DataConverter;



/**
 * http request 
 *
 * @author grro@xlightweb.org
 */
public class HttpRequest extends AbstractHttpMessage implements IHttpRequest {
    
    private static final Logger LOG = Logger.getLogger(HttpRequest.class.getName());
    

	
	/**
	 * constructor
	 * 
	 * @param requestHeader  the request header
	 */
	public HttpRequest(IHttpRequestHeader requestHeader) {
		super(requestHeader);
		
		if (!isBodylessRequestMethod(requestHeader.getMethod())) {
			requestHeader.setContentLength(0);
		}
	}
	

	/**
	 * constructor
	 * 
	 * @param requestHeader    the request header 
	 * @param bodyDataSource   the body data source
	 * 
	 * @throws IOException if an exception occurs  
	 */
	public HttpRequest(IHttpRequestHeader requestHeader, NonBlockingBodyDataSource bodyDataSource) throws IOException {
		super(requestHeader);
		
		if ((bodyDataSource == null) || (bodyDataSource.availableSilence() == -1)) {
		    if (!isBodylessRequestMethod(requestHeader.getMethod())) {
		        setContentLength(0);
		    }
		    removeHeader("Content-Type");
		    removeHeader("Transfer-Encoding");
		    
		} else {
			setBody(bodyDataSource);
		} 
	}

	
	/**
	 * constructor
	 * 
	 * @param requestHeader    the request header 
	 * @param bodyData         the body data 
	 * @param compress         true, if the body should be compressed
	 * 
	 * @throws IOException if an exception occurs  
	 */
    public HttpRequest(IHttpRequestHeader requestHeader, ByteBuffer[] bodyData, boolean compress) throws IOException {
        this(requestHeader);
        setBody(requestHeader, bodyData, compress);
    }

	
	
 
    
    /**
     * constructor. The input stream will be read by a background thread. If the end of stream is reached, it will be closed 
     * 
     * @param requestHeader    the request header 
     * @param is         the body data 
     * 
     * @throws IOException if an exception occurs  
     */
    public HttpRequest(IHttpRequestHeader requestHeader, InputStream is) throws IOException {
        super(requestHeader);
        
        if (!isBodylessRequestMethod(requestHeader.getMethod()) && (requestHeader.getMethod().equalsIgnoreCase("PUT") || requestHeader.getMethod().equalsIgnoreCase("POST"))) {
            if ((is == null) || (is.available() == -1)) {
                requestHeader.setContentLength(0);
                    
            } else {
                requestHeader.removeHeader("Content-Length");
                requestHeader.setHeader("Transfer-Encoding", "chunked");

                NonBlockingBodyDataSource dataSource = new InMemoryBodyDataSource(requestHeader);
                setBody(dataSource);

                    
                if ((requestHeader.getContentType() != null) && (requestHeader.getContentType().startsWith("application/x-www-form-urlencoded"))) {
                    forwardInputStream(dataSource, is);
                        
                } else {
                    IMultimodeExecutor executor = HttpUtils.newMultimodeExecutor();
                    executor.processMultithreaded(new ForwardTask(dataSource, is));
                }
            }
        }
    }
    
    
    
    private static final class ForwardTask implements Runnable {
        
        private final InputStream is;
        private final NonBlockingBodyDataSource dataSource;
        
        public ForwardTask(NonBlockingBodyDataSource dataSource, InputStream is) {
            this.is = is;
            this.dataSource = dataSource;
        }
        
        public void run() {
            
            try {
                forwardInputStream(dataSource, is);
            } catch (Exception e) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("error occured by reading input stream " + e.toString());
                }
                dataSource.destroy("error occured by reading input stream " + e.toString());
            }
        }  
    }
    
	
    private static final void forwardInputStream(NonBlockingBodyDataSource dataSource, InputStream is) throws IOException {
        int read;
        do {
            byte[] transferBuffer = new byte[4096];
            read = is.read(transferBuffer);
                
            if (read != -1) {
                dataSource.append(ByteBuffer.wrap(transferBuffer, 0, read));
            }
        } while (read != -1);
            
        dataSource.setComplete();
        is.close();
    }
    
    
	/**
 	 * constructor
	 * 
	 * @param method        the method (GET, POST, ...)
	 * @param url           the url
	 * @param contentType   the content type
	 * @param body          the body 
	 * @param compress      true, if the body should be compressed
	 * @throws IOException if an exception occurs
	 */
    public HttpRequest(String method, String url, String contentType, ByteBuffer[] body, boolean compress) throws IOException {
        this(new HttpRequestHeader(method, url, contentType), body, compress);
    }

    
       
	
	/**
 	 * constructor
	 * 
	 * @param method        the method (GET, POST, ...)
	 * @param url           the url
	 * @param contentType   the content type
	 * @param body          the body 
	 * @param compress      true, if the body should be compressed
	 *
	 * @throws IOException if an exception occurs
	 */
    public HttpRequest(String method, String url, String contentType, byte[] body, boolean compress) throws IOException {
        this(new HttpRequestHeader(method, url, contentType), new ByteBuffer[] { DataConverter.toByteBuffer(body) }, compress);
    }
    
    
	
	
	
	/**
 	 * constructor
	 * 
	 * @param method        the method (GET, POST, ...)
	 * @param url           the url
	 * @param contentType   the content type
	 * @param encoding      the encoding
 	 * @param body          the body 
 	 * @param compress      true, if the body should be compressed
	 * 
	 * @throws IOException if an exception occurs
	 */
    public HttpRequest(String method, String url, String contentType, String encoding, String body, boolean compress) throws IOException {
        this(newRequestHeader(method, url, contentType, encoding), body, compress);
    }
    
   
    

	/**
 	 * constructor
	 * 
	 * @param method        the method (GET, POST, ...)
	 * @param url           the url
	 * @param contentType   the content type
	 * @param body          the body 
	 * @param compress      true, if the body should be compressed
	 * 
	 * @throws IOException if an exception occurs
	 */
    public HttpRequest(String method, String url, String contentType, String body, boolean compress) throws IOException {
        this(new HttpRequestHeader(method, url, contentType), body, compress);
    }
	
    
    

    /**
     * constructor
     * 
     * @param requestHeader    the request header 
     * @param body             the body
     * @param compress         true, if the body should be compressed
     * 
     * @throws IOException if an exception occurs  
     */
    public HttpRequest(IHttpRequestHeader requestHeader, String body, boolean compress) throws IOException {
        this(requestHeader, convert(requestHeader, body), compress);
    }

    
    private static ByteBuffer[] convert(IHttpRequestHeader requestHeader, String body) {
        if ((requestHeader.getContentType() != null) && (HttpUtils.isTextMimeType(requestHeader.getContentType()) && (HttpUtils.parseEncoding(requestHeader.getContentType()) == null))) {
            requestHeader.setContentType(requestHeader.getContentType() + "; charset=utf-8");
            
        }
        
        return new ByteBuffer[] { DataConverter.toByteBuffer(body, requestHeader.getCharacterEncoding()) };
    }
    

    /**
     * constructor
     * 
     * @param requestHeader    the request header 
     * @param body             the body
     * 
     * @throws IOException if an exception occurs  
     */
    public HttpRequest(IHttpRequestHeader requestHeader, String body) throws IOException {
        this(requestHeader, body, false);
    }
    
    


	/**
	 * constructor
	 * 
	 * @param requestHeader    the request header 
	 * @param bodyData         the body data 
	 * 
	 * @throws IOException if an exception occurs  
	 */
	public HttpRequest(IHttpRequestHeader requestHeader, ByteBuffer[] bodyData) throws IOException {
		this(requestHeader, bodyData, false);
	}

	
	
	   /**
     * constructor
     * 
     * @param requestHeader    the request header 
     * @param bodyData         the body data 
     * 
     * @throws IOException if an exception occurs  
     */
    public HttpRequest(IHttpRequestHeader requestHeader, byte[] bodyData) throws IOException {
        this(requestHeader, new ByteBuffer[] { DataConverter.toByteBuffer(bodyData) }, false);
    }

    

    

    /**
     * constructor
     * 
     * @param requestHeader    the request header 
     * @param bodyData         the body data 
     * 
     * @throws IOException if an exception occurs  
     */
    public HttpRequest(IHttpRequestHeader requestHeader, List<ByteBuffer> bodyData) throws IOException {
        this(requestHeader, bodyData, false);
    }

    
    /**
     * constructor
     * 
     * @param requestHeader    the request header 
     * @param bodyData         the body data
     * @param compress         true, if the data should be compressed       
     * 
     * @throws IOException if an exception occurs  
     */
    public HttpRequest(IHttpRequestHeader requestHeader, List<ByteBuffer> bodyData, boolean compress) throws IOException {
        this(requestHeader, toArray(bodyData), compress);
    }
    
  
   
    
	/**
     * constructor
     * 
     * @param requestHeader    the request header 
     * @param bodyData         the body data 
     * 
     * @throws IOException if an exception occurs  
     */
    HttpRequest(IHttpRequestHeader requestHeader, File bodyData) throws IOException {
        this(requestHeader, new FileDataSource(requestHeader, HttpUtils.newMultimodeExecutor(), bodyData));
        setContentLength((int) bodyData.length());
    }

    
	
	/**
 	 * constructor
	 * 
	 * @param method        the method (GET, POST, ...)
	 * @param url           the url
	 * @param contentType   the content type
	 * @param body          the body 
	 * 
	 * @throws IOException if an exception occurs
	 */
	public HttpRequest(String method, String url, String contentType, String body) throws IOException {
		this(method, url, contentType, body, false);
	}
	
	

	
	/**
 	 * constructor
	 * 
	 * @param method        the method (GET, POST, ...)
	 * @param url           the url
	 * @param contentType   the content type
	 * @param encoding      the encoding
 	 * @param body          the body 
	 * 
	 * @throws IOException if an exception occurs
	 */
	public HttpRequest(String method, String url, String contentType, String encoding, String body) throws IOException {
	    this(method, url, contentType, encoding, body, false);
	}
	
	

    
	
	/**
 	 * constructor
	 * 
	 * @param method        the method (GET, POST, ...)
	 * @param url           the url
	 * @param contentType   the content type
	 * @param body          the body 
	 *
	 * @throws IOException if an exception occurs
	 */
	public HttpRequest(String method, String url, String contentType, byte[] body) throws IOException {
		this(method, url, contentType, body, false);
	}
	
	
	

  
	
	/**
 	 * constructor
	 * 
	 * @param method        the method (GET, POST, ...)
	 * @param url           the url
	 * @param contentType   the content type
	 * @param body          the body 
	 * 
	 * @throws IOException if an exception occurs
	 */
	public HttpRequest(String method, String url, String contentType, ByteBuffer[] body) throws IOException {
		this(method, url, contentType, body, false);
	}

	
	
	/**
 	 * constructor
	 * 
	 * @param method        the method (GET, POST, ...)
	 * @param url           the url
	 * 
	 */
	public HttpRequest(String method, String url) {
	    this(new HttpRequestHeader(method, url));	
	}
	

	
	
 
    
    private static HttpRequestHeader newRequestHeader(String method, String url, String contentType, String encoding) {
        if ((HttpUtils.parseEncoding(contentType) == null) && (HttpUtils.parseMediaType(contentType).startsWith("text/"))) {
            contentType = contentType + "; charset=" + encoding;
        }

        return new HttpRequestHeader(method,url, contentType);
    }
    
    
    
    private static ByteBuffer[] toArray(List<ByteBuffer> buffers) {
        if (buffers == null) {
            return null;
        } else {
            return buffers.toArray(new ByteBuffer[buffers.size()]);
        }
    }
    
	
	

	/**
	 * returns the request header
	 *  
	 * @return the request header
	 */
	public IHttpRequestHeader getRequestHeader() {
		return (IHttpRequestHeader) getPartHeader();
	}
	

	
	/**
	 * {@inheritDoc}
	 */
	public String getRequestHandlerPath() {
		return getRequestHeader().getRequestHandlerPath();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void setRequestHandlerPath(String requestHandlerPath) {
		getRequestHeader().setRequestHandlerPath(requestHandlerPath);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public String getContextPath() {
		return getRequestHeader().getContextPath();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void setContextPath(String contextPath) {
	    getRequestHeader().setContextPath(contextPath);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void setMethod(String method) {
		getRequestHeader().setMethod(method);
	}
	

	/**
	 * {@inheritDoc}
	 */
	public String getMethod() {
		return getRequestHeader().getMethod();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public List<ContentType> getAccept() {
		return getRequestHeader().getAccept();
	}
	
	
    /**
     * {@inheritDoc}
     */
	public String getScheme() {
	    return getRequestHeader().getScheme();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void setHost(String host) {
		getRequestHeader().setHost(host);
	}
	

	/**
	 * {@inheritDoc}
	 */
	public String getServerName() {
		return getRequestHeader().getServerName();
	}
	

	/**
	 * {@inheritDoc}
	 */
	public int getServerPort() {
		return getRequestHeader().getServerPort();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public String getProtocol() {
		return getRequestHeader().getProtocol();
	}
	

	/**
	 * {@inheritDoc}
	 */
	public String getProtocolVersion() {
		return getRequestHeader().getProtocolVersion();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public URL getRequestUrl() {
		return getRequestHeader().getRequestUrl();
	}
	
	

	/**
	 * {@inheritDoc}
	 */
	public void setRequestUrl(URL url) {
		getRequestHeader().setRequestUrl(url);
	}
	
	

	/**
	 * {@inheritDoc}
	 */
	public String getRemoteHost() {
		return getRequestHeader().getRemoteHost();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public String getRemoteAddr() {
		return getRequestHeader().getRemoteAddr();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public int getRemotePort() {
		return getRequestHeader().getRemotePort();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public String getRequestURI() {
		return getRequestHeader().getRequestURI();
	}

	
	public void setRequestURI(String requestUri) {
		getRequestHeader().setRequestURI(requestUri);
	}

	
	/**
	 * {@inheritDoc}
	 */
	public String getPathInfo() {
	    return getRequestHeader().getPathInfo();
	}
 
		
	/**
	 * {@inheritDoc}
	 */
	public String getPathInfo(boolean removeSurroundingSlashs) {
	    return getRequestHeader().getPathInfo(removeSurroundingSlashs);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public String getHost() {
		return getRequestHeader().getHost();
	}
	
	

	
	/**
	 * {@inheritDoc}
	 */
	public String getUserAgent() {
		return getRequestHeader().getUserAgent();
	}
	
	public String getKeepAlive() {
	    return getRequestHeader().getKeepAlive();
	}
	
	public void setKeepAlive(String keepAlive) {
	    getRequestHeader().setKeepAlive(keepAlive);	    
	}
	
	public String getUpgrade() {
	    return getRequestHeader().getUpgrade();
	}
	
	public void setUpgrade(String upgrade) {
	    getRequestHeader().setUpgrade(upgrade);	    
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void setUserAgent(String userAgent) {
		getRequestHeader().setUserAgent(userAgent);
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getQueryString() {
		return getRequestHeader().getQueryString();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isSecure() {
		return getRequestHeader().isSecure();
	}
	

    
    /**
     * {@inheritDoc}
     */
    public String getMatrixParameter(String name) {
        return getRequestHeader().getMatrixParameter(name);
    }
    
    
    
    /**
     * {@inheritDoc}
     */
    public Set<String> getMatrixParameterNameSet() {
       return getRequestHeader().getMatrixParameterNameSet();
    }


    
    /**
     * {@inheritDoc}
     */
    public String[] getMatrixParameterValues(String name) {
        return getRequestHeader().getMatrixParameterValues(name);
    }

    
    /**
     * {@inheritDoc}
     */
    public void setMatrixParameter(String parameterName, String parameterValue) {
        getRequestHeader().setMatrixParameter(parameterName, parameterValue);
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void addMatrixParameter(String parameterName, String parameterValue) {
        getRequestHeader().addMatrixParameter(parameterName, parameterValue);
    }

    /**
     * {@inheritDoc}
     */
    public void removeMatrixParameter(String parameterName) {
        getRequestHeader().removeMatrixParameter(parameterName);        
    }
    
	
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public Enumeration getParameterNames() {
		return getRequestHeader().getParameterNames();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Set<String> getParameterNameSet() {
		return getRequestHeader().getParameterNameSet();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public String getParameter(String name) {
		return getRequestHeader().getParameter(name);
	}

	
    /**
     * {@inheritDoc}
     */	
	public String getParameter(String name, String defaultVal) {
	    return getRequestHeader().getParameter(name, defaultVal);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public String[] getParameterValues(String name) {
		return getRequestHeader().getParameterValues(name);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public String getRequiredStringParameter(String name) throws BadMessageException {
		return getRequestHeader().getRequiredStringParameter(name);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public Integer getIntParameter(String name) throws BadMessageException{
		return getRequestHeader().getIntParameter(name);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public int getRequiredIntParameter(String name) throws BadMessageException{
		return getRequestHeader().getRequiredIntParameter(name);
	}


	/**
	 * {@inheritDoc}
	 */
	public int getIntParameter(String name, int defaultVal) {
		return getRequestHeader().getIntParameter(name, defaultVal);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public Long getLongParameter(String name) throws BadMessageException {
		return getRequestHeader().getLongParameter(name);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public long getRequiredLongParameter(String name) throws BadMessageException {
		return getRequestHeader().getRequiredLongParameter(name);
	}

	
	
	/**
	 * {@inheritDoc}
	 */
	public long getLongParameter(String name, long defaultVal) {
		return getRequestHeader().getLongParameter(name, defaultVal);
	}

	
	/**
	 * {@inheritDoc}
	 */
	public Double getDoubleParameter(String name) throws BadMessageException {
		return getRequestHeader().getDoubleParameter(name);
	}

	
	/**
	 * {@inheritDoc}
	 */
	public double getRequiredDoubleParameter(String name) throws BadMessageException {
		return getRequestHeader().getRequiredDoubleParameter(name);
	}

	
	
	/**
	 * {@inheritDoc}
	 */
	public double getDoubleParameter(String name, double defaultVal) {
		return getRequestHeader().getDoubleParameter(name, defaultVal);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public Float getFloatParameter(String name) throws BadMessageException {
		return getRequestHeader().getFloatParameter(name);
	}
	

	/**
	 * {@inheritDoc}
	 */
	public float getRequiredFloatParameter(String name) throws BadMessageException {
		return getRequestHeader().getRequiredFloatParameter(name);
	}

	
	
	/**
	 * {@inheritDoc}
	 */
	public float getFloatParameter(String name, float defaultVal) {
		return getRequestHeader().getFloatParameter(name, defaultVal);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public Boolean getBooleanParameter(String name) {
		return getRequestHeader().getBooleanParameter(name);
	}

	
	/**
	 * {@inheritDoc}
	 */
	public boolean getRequiredBooleanParameter(String name) throws BadMessageException {
		return getRequestHeader().getRequiredBooleanParameter(name);
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean getBooleanParameter(String name, boolean defaultVal) {
		return getRequestHeader().getBooleanParameter(name, defaultVal);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void setParameter(String parameterName, String parameterValue) {
	    try {
    	    if ((getContentType() != null) && (getContentType().startsWith("application/x-www-form-urlencoded")) && getNonBlockingBody().isComplete()) {
    	        MultivalueMap body = new MultivalueMap(getBlockingBody());
    	        body.removeParameter(parameterName);
    	        body.setParameter(parameterName, parameterValue);

    	        removeHeader("Content-Length");
    	        setBody(getMessageHeader(), new ByteBuffer[] { DataConverter.toByteBuffer(body.toString(), getCharacterEncoding()) }, false);

    	    } else {
    	        getRequestHeader().setParameter(parameterName, parameterValue);
    	    }
	    } catch (IOException ioe) {
	        throw new RuntimeException(ioe);
	    }
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void removeParameter(String parameterName) {
	    try {
            if ((getContentType() != null) && (getContentType().startsWith("application/x-www-form-urlencoded")) && getNonBlockingBody().isComplete()) {
                MultivalueMap body = new MultivalueMap(getBlockingBody());
                body.removeParameter(parameterName);
                body.removeParameter(parameterName);
                
                removeHeader("Content-Length");
                setBody(getMessageHeader(), new ByteBuffer[] { DataConverter.toByteBuffer(body.toString(), getCharacterEncoding()) }, false);
            } else {
                getRequestHeader().removeParameter(parameterName);
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void addParameter(String parameterName, String parameterValue) {
	    try {
            if ((getContentType() != null) && (getContentType().startsWith("application/x-www-form-urlencoded")) && getNonBlockingBody().isComplete()) {
                MultivalueMap body = new MultivalueMap(getBlockingBody());
                body.addParameter(parameterName, parameterValue);

                removeHeader("Content-Length");
                setBody(getMessageHeader(), new ByteBuffer[] { DataConverter.toByteBuffer(body.toString(), getCharacterEncoding()) }, false);

            } else {
                getRequestHeader().addParameter(parameterName, parameterValue);
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
	}

	

	/**
	 * return true if the method indicated a body less message 
	 * 
	 * @param method the method name
	 * @return  true if the method indicated a body less message
	 */
	static boolean isBodylessRequestMethod(String method) {
		
		if (method.equals(IHttpMessage.GET_METHOD)) {
			return true;
		}
		
		if (method.equals(IHttpMessage.POST_METHOD)) {
			return false;
		}
		
		
		if (method.equals(IHttpMessage.CONNECT_METHOD) || 
			method.equals(IHttpMessage.HEAD_METHOD) || 
		    method.equals(IHttpMessage.TRACE_METHOD)|| 
		    method.equals(IHttpMessage.DELETE_METHOD)|| 
		    method.equals(IHttpMessage.OPTIONS_METHOD)) {
			
			return true;
		}
		
		return false;
	}
}
