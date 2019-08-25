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

import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;




 
/**
 * Provides a convenient implementation of the HttpRequestHeader interface that can 
 * be subclassed by developers wishing to adapt the request header. This class implements 
 * the Wrapper or Decorator pattern. Methods default to calling through to the 
 * wrapped request header object.   
 * 
 * @author grro@xlightweb.org
 */
public class HttpRequestHeaderWrapper extends HeaderWrapper implements IHttpRequestHeader {

	/**
	 * constructor 
	 * 
	 * @param request the request header to wrap
	 */
	public HttpRequestHeaderWrapper(IHttpRequestHeader requestHeader) {
	    super(requestHeader);
	}
	
	
	protected IHttpRequestHeader getWrappedRequestHeader() {
	    return (IHttpRequestHeader) getWrappedHeader();
	}
	

	/**
	 * {@inheritDoc}
	 */
	public Boolean getBooleanParameter(String name) {
		return getWrappedRequestHeader().getBooleanParameter(name);
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean getBooleanParameter(String name, boolean defaultVal) {
		return getWrappedRequestHeader().getBooleanParameter(name, defaultVal);
	}


	/**
	 * {@inheritDoc}
	 */
	public Double getDoubleParameter(String name) throws BadMessageException {
		return getWrappedRequestHeader().getDoubleParameter(name);
	}

	/**
	 * {@inheritDoc}
	 */
	public double getDoubleParameter(String name, double defaultVal) {
		return getWrappedRequestHeader().getDoubleParameter(name, defaultVal);
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getFloatParameter(String name) throws BadMessageException {
		return getWrappedRequestHeader().getFloatParameter(name);
	}

	/**
	 * {@inheritDoc}
	 */
	public float getFloatParameter(String name, float defaultVal) {
		return getWrappedRequestHeader().getFloatParameter(name, defaultVal);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getIntParameter(String name) throws BadMessageException {
		return getWrappedRequestHeader().getIntParameter(name);
	}


	/**
	 * {@inheritDoc}
	 */
	public int getIntParameter(String name, int defaultVal) {
		return getWrappedRequestHeader().getIntParameter(name, defaultVal);
	}


	/**
	 * {@inheritDoc}
	 */
	public Long getLongParameter(String name) throws BadMessageException {
		return getWrappedRequestHeader().getLongParameter(name);
	}


	/**
	 * {@inheritDoc}
	 */
	public long getLongParameter(String name, long defaultVal) {
		return getWrappedRequestHeader().getLongParameter(name, defaultVal);
	}


	/**
	 * {@inheritDoc}
	 */
	public String getMethod() {
		return getWrappedRequestHeader().getMethod();
	}


	
    /**
     * {@inheritDoc}
     */
	public String getMatrixParameter(String name) {
	    return getWrappedRequestHeader().getMatrixParameter(name);
	}
	
	
	
    /**
     * {@inheritDoc}
     */
	public Set<String> getMatrixParameterNameSet() {
	   return getWrappedRequestHeader().getMatrixParameterNameSet();
	}


	
    /**
     * {@inheritDoc}
     */
	public String[] getMatrixParameterValues(String name) {
	    return getWrappedRequestHeader().getParameterValues(name);
	}

	
    /**
     * {@inheritDoc}
     */
	public void setMatrixParameter(String parameterName, String parameterValue) {
	    getWrappedRequestHeader().setMatrixParameter(parameterName, parameterValue);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void addMatrixParameter(String parameterName, String parameterValue) {
	    getWrappedRequestHeader().addMatrixParameter(parameterName, parameterValue);
	}
	
	
	/**
     * {@inheritDoc}
     */
	public void removeMatrixParameter(String parameterName) {
	    getWrappedRequestHeader().removeMatrixParameter(parameterName);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void removeParameter(String parameterName) {
	    getWrappedRequestHeader().removeParameter(parameterName);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getParameter(String name) {
		return getWrappedRequestHeader().getParameter(name);
	}

	/**
     * {@inheritDoc}
     */
	public String getParameter(String name, String defaultVal) {
	    return getWrappedRequestHeader().getParameter(name, defaultVal);
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<String> getParameterNameSet() {
		return getWrappedRequestHeader().getParameterNameSet();
	}


	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public Enumeration getParameterNames() {
		return Collections.enumeration(getParameterNameSet());
	}

	
	/**
	 * {@inheritDoc}
	 */
	
	public String[] getParameterValues(String name) {
		return getWrappedRequestHeader().getParameterValues(name);
	}

	
	/**
	 * {@inheritDoc}
	 */
	public String getQueryString() {
		return getWrappedRequestHeader().getQueryString();
	}


	/**
	 * {@inheritDoc}
	 */
	public String getRemoteAddr() {
		return getWrappedRequestHeader().getRemoteAddr();
	}


	/**
	 * {@inheritDoc}
	 */
	public String getRemoteHost() {
		return getWrappedRequestHeader().getRemoteHost();
	}


	/**
	 * {@inheritDoc}
	 */
	public int getRemotePort() {
		return getWrappedRequestHeader().getRemotePort();
	}


	/**
	 * {@inheritDoc}
	 */
	public String getRequestURI() {
		return getWrappedRequestHeader().getRequestURI();
	}

	
	/** 
	 * {@inheritDoc}
	 */ 
	public void setRequestURI(String requestUri) {
	    getWrappedRequestHeader().setRequestURI(requestUri);
	}

	/**
	 * {@inheritDoc}
	 */
	public URL getRequestUrl() {
		return getWrappedRequestHeader().getRequestUrl();
	}

    /**
     * {@inheritDoc}
     */
	public String getPathInfo() {
	    return getWrappedRequestHeader().getPathInfo();
	}
	
	
    /**
     * {@inheritDoc}
     */
	public String getPathInfo(boolean removeSurroundingSlashs) {
	    return getWrappedRequestHeader().getPathInfo(removeSurroundingSlashs);
	}
	
	
    /**
     * {@inheritDoc}
     */	
	public String getRequestHandlerPath() {
	    return getWrappedRequestHeader().getRequestHandlerPath();
	}
	
    /**
     * {@inheritDoc}
     */
	public void setRequestHandlerPath(String requestHandlerPath) {
	    getWrappedRequestHeader().setRequestHandlerPath(requestHandlerPath);
	}
	
	
    /**
     * {@inheritDoc}
     */	
	public String getContextPath() {
	    return getWrappedRequestHeader().getContextPath();
	}
	
	/**
     * {@inheritDoc}
     */
	public void setContextPath(String contextPath) {
	    getWrappedRequestHeader().setContextPath(contextPath);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean getRequiredBooleanParameter(String name) throws BadMessageException {
		return getWrappedRequestHeader().getRequiredBooleanParameter(name);
	}


	/**
	 * {@inheritDoc}
	 */
	public double getRequiredDoubleParameter(String name) throws BadMessageException {
		return getWrappedRequestHeader().getRequiredDoubleParameter(name);
	}


	/**
	 * {@inheritDoc}
	 */
	public float getRequiredFloatParameter(String name) throws BadMessageException {
		return getWrappedRequestHeader().getRequiredFloatParameter(name);
	}


	/**
	 * {@inheritDoc}
	 */
	public int getRequiredIntParameter(String name) throws BadMessageException {
		return getWrappedRequestHeader().getRequiredIntParameter(name);
	}


	/**
	 * {@inheritDoc}
	 */
	public long getRequiredLongParameter(String name) throws BadMessageException {
		return getWrappedRequestHeader().getRequiredLongParameter(name);
	}


	/**
	 * {@inheritDoc}
	 */
	public String getRequiredStringParameter(String name) throws BadMessageException {
		return getWrappedRequestHeader().getRequiredStringParameter(name);
	}


	/**
	 * {@inheritDoc}
	 */
	public String getServerName() {
		return getWrappedRequestHeader().getServerName();
	}


	/**
	 * {@inheritDoc}
	 */
	public int getServerPort() {
		return getWrappedRequestHeader().getServerPort();
	}
	

    /**
     * {@inheritDoc}
     */
	public int getContentLength() {
	    return getWrappedRequestHeader().getContentLength();
	}

	
	/**
	 * {@inheritDoc}
	 */
	public boolean isSecure() {
		return getWrappedRequestHeader().isSecure();
	}


	/**
	 * {@inheritDoc}
	 */
	public void setMethod(String method) {
	    getWrappedRequestHeader().setMethod(method);		
	}


	/**
	 * {@inheritDoc}
	 */
	public void setParameter(String parameterName, String parameterValue) {
	    getWrappedRequestHeader().setParameter(parameterName, parameterValue);
	}


	/**
     * {@inheritDoc}
     */
	public void addParameter(String parameterName, String parameterValue) {
	    getWrappedRequestHeader().addParameter(parameterName, parameterValue);
	}
	

	/**
	 * {@inheritDoc}
	 */
	public void setRequestUrl(URL url) {
	    getWrappedRequestHeader().setRequestUrl(url);		
	}


	/**
	 * {@inheritDoc}
	 */
	public Object getAttribute(String name) {
		return getWrappedRequestHeader().getAttribute(name);
	}


	/**
	 * {@inheritDoc}
	 */
	public Set<String> getAttributeNameSet() {
		return getWrappedRequestHeader().getAttributeNameSet();
	}


	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public Enumeration getAttributeNames() {
		return getWrappedRequestHeader().getAttributeNames();
	}



	/**
	 * {@inheritDoc}
	 */
	public List<ContentType> getAccept() {
		return getWrappedRequestHeader().getAccept();
	}
	
    /**
     * {@inheritDoc}
     */	
	public void setKeepAlive(String keepAlive) {
	    getWrappedRequestHeader().setKeepAlive(keepAlive);
	}
	
    /**
     * {@inheritDoc}
     */	
	public String getKeepAlive() {
	    return getWrappedRequestHeader().getKeepAlive();
	}
	
    /**
     * {@inheritDoc}
     */	
	public void setUpgrade(String upgrade) {
	    getWrappedRequestHeader().setUpgrade(upgrade);
	}

	/**
     * {@inheritDoc}
     */
	public String getUpgrade() {
	    return getWrappedRequestHeader().getUpgrade();
	}

	/**
     * {@inheritDoc}
     */
	public String getScheme() {
	    return getWrappedRequestHeader().getScheme();
	}
	

	
	/**
	 * {@inheritDoc}
	 */
	public String getHost() {
		return getWrappedRequestHeader().getHost();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public String getUserAgent() {
		return getWrappedRequestHeader().getUserAgent();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void setHost(String host) {
	    getWrappedRequestHeader().setHost(host);
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public void setUserAgent(String userAgent) {
	    getWrappedRequestHeader().setUserAgent(userAgent);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public IHttpRequestHeader copy() {
		return new HttpRequestHeaderWrapper(this);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void copyHeaderFrom(IHttpMessageHeader otherHeader, String... upperExcludenames) {
	    getWrappedRequestHeader().copyHeaderFrom(otherHeader, upperExcludenames);
	}
	


	/**
	 * {@inheritDoc}
	 */
	public String getProtocol() {
		return getWrappedRequestHeader().getProtocol();
	}


	/**
	 * {@inheritDoc}
	 */
	public String getProtocolVersion() {
		return getWrappedRequestHeader().getProtocolVersion();
	}


	/**
	 * {@inheritDoc}
	 */
	public String getTransferEncoding() {
		return getWrappedRequestHeader().getTransferEncoding();
	}


	/**
	 * {@inheritDoc}
	 */
	public void removeHopByHopHeaders() {
	    getWrappedRequestHeader().removeHopByHopHeaders();
	}


	/**
	 * {@inheritDoc}
	 */
	public void setAttribute(String name, Object o) {
	    getWrappedRequestHeader().setAttribute(name, o);		
	}


	/**
	 * {@inheritDoc}
	 */
	public void setContentLength(int length) {
	    getWrappedRequestHeader().setContentLength(length);		
	}


	/**
	 * {@inheritDoc}
	 */
	public void setContentType(String type) {
	    getWrappedRequestHeader().setContentType(type);		
	}



	/**
	 * {@inheritDoc}
	 */
	public void setTransferEncoding(String transferEncoding) {
	    getWrappedRequestHeader().setTransferEncoding(transferEncoding);		
	}	
	
	@Override
	public String toString() {
		return getWrappedRequestHeader().toString();
	}
}
