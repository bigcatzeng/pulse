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


 
/**
 * Provides a convenient implementation of the HttpRequest interface that can 
 * be subclassed by developers wishing to adapt the request. This class implements 
 * the Wrapper or Decorator pattern. Methods default to calling through to the 
 * wrapped request object.   
 * 
 * @author grro@xlightweb.org
 */
public class HttpRequestWrapper extends HttpRequestHeaderWrapper implements IHttpRequest {

	private final IHttpRequest request;
	

	/**
	 * constructor 
	 * 
	 * @param request the request to wrap
	 */
	public HttpRequestWrapper(IHttpRequest request) {
		this(request.getRequestHeader(), request);
	}
	
	
	/**
	 * constructor 
	 * 
	 * @param requestHeader  the  header
	 * @param request the delegate message
	 */
	HttpRequestWrapper(IHttpRequestHeader requestHeader, IHttpRequest request) {
		super(requestHeader);
		this.request = request;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getContextPath() {
		return request.getContextPath();
	}



	/**
	 * {@inheritDoc}
	 */
	public String getPathInfo() {
		return request.getPathInfo();
	}
	

	/**
	 * {@inheritDoc}
	 */
	public String getPathInfo(boolean removeSurroundingSlashs) {
		return request.getPathInfo(removeSurroundingSlashs);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getRequestHandlerPath() {
		return request.getRequestHandlerPath();
	}


	/**
	 * {@inheritDoc}
	 */
	public IHttpRequestHeader getRequestHeader() {
		return request.getRequestHeader();
	}


	
	/**
	 * {@inheritDoc}
	 */
	public String getHost() {
		return request.getHost();
	}
	

	/**
	 * {@inheritDoc}
	 */
	public String getUserAgent() {
		return request.getUserAgent();
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public void setHost(String host) {
		request.setHost(host);
	}

	
	/**
	 * {@inheritDoc}
	 */
	public void setUserAgent(String userAgent) {
		request.setUserAgent(userAgent);
	}
	

	/**
	 * {@inheritDoc}
	 */
	public void setContextPath(String contextPath) {
		request.setContextPath(contextPath);		
	}

	
   /**
     * {@inheritDoc}
     */
	@Override
	public void setParameter(String parameterName, String parameterValue) {
	    request.setParameter(parameterName, parameterValue);
	}

	
    /**
     * {@inheritDoc}
     */
	@Override
	public void removeParameter(String parameterName) {
	    request.removeParameter(parameterName);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRequestHandlerPath(String requestHandlerPath) {
		request.setRequestHandlerPath(requestHandlerPath);	
	}

	

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("deprecation")
    public BlockingBodyDataSource getBlockingBody() throws IOException {
		return request.getBlockingBody();
	}
	
	public BodyDataSource getBody() throws IOException {
	    return request.getBody();
	}


	/**
	 * {@inheritDoc}
	 */
	public IHttpMessageHeader getMessageHeader() {
		return request.getMessageHeader();
	}


	/**
	 * {@inheritDoc}
	 */
	public IHeader getPartHeader() {
		return request.getPartHeader();
	}

	/**
	 * {@inheritDoc}
	 */
	public NonBlockingBodyDataSource getNonBlockingBody() throws IOException {
		return request.getNonBlockingBody();
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean hasBody() {
		return request.hasBody();
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return request.toString();
	}
}
