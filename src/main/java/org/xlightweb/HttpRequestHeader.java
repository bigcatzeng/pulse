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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.xsocket.connection.INonBlockingConnection;




/**
* http request header
* 
 * @author grro@xlightweb.org
 */
public class HttpRequestHeader extends HttpMessageHeader implements IHttpRequestHeader {
	
    private static final Logger LOG = Logger.getLogger(HttpRequestHeader.class.getName());
    
    
	private static final Boolean NULL_BOOLEAN = null;
	
    private static final boolean IS_UPDATE_HOSTHEADER = Boolean.parseBoolean(System.getProperty("org.xlightweb.requestheader.autoupdatehost", "true"));

	
	private final INonBlockingConnection tcpConnection;
	
	
	private String method;
	private String path;
	private String queryString;

	private ArrayList<Parameter> queryParameters;
	private boolean isQueryParamMapModified = false;
	private boolean queryParameterResolveRequired = false;

	private ArrayList<Parameter> matrixParameters;
    private boolean matrixParameterResolveRequired = true;
	
	
	private boolean isSecure = false; 
	
	
    private String requestHandlerPath = "";
    private String contextPath = ""; 
    
	
	
	// cache
	private String servername;
	private Integer serverport;
	
	private String host;
	private String userAgent;

	

	/**
	 * constructor
	 *  
	 * @param connection  the underlying connection
	 */
	HttpRequestHeader(INonBlockingConnection tcpConnection) {
		this.tcpConnection = tcpConnection;
		isSecure = tcpConnection.isSecure();
		setProtocolSchemeSilence("HTTP");
		setProtocolVersionSilence("0.9");
	}


	
	/**
	 * constructor 
	 * 
	 * @param method   the method 
	 * @param url      the url string
	 */
	public HttpRequestHeader(String method, String url) {
		this(method, url, null, "1.1");
	}
	

	/**
	 * constructor
	 * 
	 * @param method       the method
	 * @param url          the url string 
	 * @param contentType  the content type
	 */
	public HttpRequestHeader(String method, String url, String contentType) {
		this(method, url, contentType, "1.1");
	}
	
	
	/**
	 * constructor 
	 * 
	 * @param method            the method
	 * @param url               the url string
	 * @param contentType       the content type 
	 * @param protocolVersion   the protocol version
	 */
	HttpRequestHeader(String method, String url, String contentType, String protocolVersion) {
	    tcpConnection = null;
		queryParameterResolveRequired = true;
		
		this.method = method;
		
		if (method.toLowerCase().startsWith("http")) {
		    throw new RuntimeException("method " + method + " is not supported");
		}
		
		setProtocolSchemeSilence("HTTP");
		setProtocolVersionSilence(protocolVersion);
		
		if (contentType != null) {
			addHeader(CONTENT_TYPE, contentType);
		}

		if (method.equalsIgnoreCase("CONNECT")) {
			path = url.trim();
				
		} else {
			int port = -1;
			
			String scheme;
			try {
				URI uri = new URI(url.trim());

				path = uri.getRawPath();
				queryString = uri.getRawQuery();
				scheme = uri.getScheme(); 
				servername = uri.getHost();
				port = uri.getPort(); 
				
				
			} catch (URISyntaxException ue) {
				
				try {
					URL u = new URL(url.trim());
	
					path = u.getPath();
					queryString = u.getQuery();
					scheme = u.getProtocol();
					
					servername = u.getHost();
					port = u.getPort();
				} catch (MalformedURLException urle) {
					throw new RuntimeException(urle.toString());
				}
			}

			if ((scheme != null) && (scheme.equals("https") || (scheme.equals("wss")))) {
				isSecure = true;
			}
			
			if (port != -1) {
				serverport = port;
				setHost(servername + ":" + serverport);
			
			} else {
				setHost(servername);
			}

			
			if (path.length() == 0) {
				path = "/";
			}

		}
	}
	
	
	/**
     * {@inheritDoc}
     */
    public String getRequestHandlerPath() {
        return requestHandlerPath;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void setRequestHandlerPath(String requestHandlerPath) {
        this.requestHandlerPath = requestHandlerPath;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String getContextPath() {
        return contextPath;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }


	
	/**
	 * returns the underlying tcp connection
	 * @return
	 */
	final INonBlockingConnection getUnderylingConnection() {
		return tcpConnection;
	}
	

	/**
	 * sets the server name silence 
	 * @param servername the server name to set
	 */
	final void setServernameSilence(String servername) {
		this.servername = servername;
	}
	
	/**
	 * sets the server port silence
	 * @param serverport the server port to set
	 */
	
	final void setServerportPortSilence(int serverport) {
		this.serverport = serverport;
	}
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	final protected boolean onHeaderAdded(String headername, String headervalue) {
	
		if (headername.equalsIgnoreCase(HOST)) {
			host = headervalue;
			return true;
		} 

		return super.onHeaderAdded(headername, headervalue);
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	final protected boolean onHeaderRemoved(String headername) {
	
		if (headername.equalsIgnoreCase(HOST)) {
			host = null;
			return true;
		} 

		if (headername.equalsIgnoreCase(USER_AGENT)) {
			userAgent = null;
			return true;
		} 
		
		return super.onHeaderRemoved(headername);
	}
	

	
	/**
	 * {@inheritDoc}
	 */
	final public String getHost() {
		return host;	 		
	}
	

	/**
     * {@inheritDoc}
     */
    final public String getPathInfo() {
        return getPathInfo(false);
    }
 
        
    /**
     * {@inheritDoc}
     */
    final public String getPathInfo(boolean removeSurroundingSlashs) {
        String uri = getRequestURI();
        uri = uri.substring(getContextPath().length(), uri.length());
        uri = uri.substring(getRequestHandlerPath().length(), uri.length()).trim();
        
        if (removeSurroundingSlashs) {
            if (uri.startsWith("/")) {
                uri = uri.substring(1,uri.length());
            }
            
            if (uri.endsWith("/")) {
                uri = uri.substring(0, uri.length() - 1).trim();
            }
            
        } else {
            if (!uri.startsWith("/")) {
                uri = "/"  + uri.substring(1,uri.length());
            }
        }

        return uri;
    }
    
	
	/**
	 * {@inheritDoc}
	 */
	final public void setHost(String host) {
	    
	    if ((host != null) && (host.endsWith(":-1"))) {
	        host = host.substring(0, host.length() - ":-1".length());
	    }
	    
		this.host = host;
	}
    
	
	/**
	 * {@inheritDoc}
	 */
	final public String getUserAgent() {
		if (userAgent != null) {
			return userAgent;
		} else {
			return getHeader(USER_AGENT);
		}
	}
	

	/**
	 * {@inheritDoc}
	 */
	final public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

    /**
     * @deprecated
     */	
	public void setUpgrade(String upgrade) {
	    setHeader("Upgrade", upgrade);   
	}

    /**
     * @deprecated
     */
	public String getUpgrade() {
	    return getHeader("Upgrade");
	}
	
    /**
     * @deprecated
     */	
	public void setKeepAlive(String keepAlive) {
	    setHeader("Keep-Alive", keepAlive);
	}
	
	
	/**
	 * @deprecated
	 */
	public String getKeepAlive() {
        return getHeader("Keep-Alive");
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	final public Set<String> getHeaderNameSet() {
		
		Set<String> headerNames = super.getHeaderNameSet();
					
		if (host != null) {
			headerNames.add(HOST);
		}
		
		if (userAgent != null) {
			headerNames.add(USER_AGENT);
		}
		
		
		return headerNames;
	}
	

	/**
	 * {@inheritDoc}
	 */
	final public List<String> getHeaderList(String headername) {
		if ((headername.equalsIgnoreCase(HOST)) && (host != null)) {
			List<String> result = new ArrayList<String>();
			result.add(host);
			return Collections.unmodifiableList(result);
		} 
		
		
		return super.getHeaderList(headername);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	final public String getHeader(String headername) {
		
		if ((headername.equalsIgnoreCase(HOST)) && (host != null)) {
			return host;
		} 
		
		if ((headername.equalsIgnoreCase(USER_AGENT)) && (userAgent != null)) {
			return userAgent;
		} 
		
		return super.getHeader(headername);
	} 

	

	/**
	 * {@inheritDoc}
	 */
	final public boolean containsHeader(String headername) {
		
		if ((headername.equalsIgnoreCase(HOST)) && (host != null)) {
			return true;
		}
		
		if ((headername.equalsIgnoreCase(USER_AGENT)) && (userAgent != null)) {
			return true;
		}
		
		return super.containsHeader(headername);
	}
		
	
	
	/**
	 * {@inheritDoc}
	 */
	final public String getRemoteHost() {
		if (tcpConnection != null) {
			return tcpConnection.getRemoteAddress().getHostName();
		} else {
			return null;
		}
	}

	
	/**
	 * {@inheritDoc}
	 */
	final public int getRemotePort() {
		if (tcpConnection != null) {
			return tcpConnection.getRemotePort();
		} else {
			return -1;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	final public String getRemoteAddr() {
		if (tcpConnection != null) {
			return tcpConnection.getRemoteAddress().getHostAddress();
		} else {
			return null;
		}
	}
	

	
	/**
	 * {@inheritDoc}
	 */
	final public String getQueryString() {
		
		if ((queryString == null) && (queryParameters != null) && (queryParameters.size() > 0)) {
			StringBuilder sb = new StringBuilder();
			for (Parameter parameter : queryParameters) {
				sb.append(parameter.getEncodedName() + "=" + parameter.getEncodedValue() + "&");
			}
			
			sb.setLength(sb.length() - 1);
			queryString = sb.toString();
		}
		
		return queryString;
	}

	
	/**
	 * {@inheritDoc}
	 */
	final public String getRequestURI() {
		return path;
	}
	
 
	/**
	 * {@inheritDoc}
	 */
	final public void setRequestURI(String requestUri) {
		this.path = requestUri; 
	}
	
	 
	/**
	 * sets the path silence
	 * @param path the path
	 */
	final void setPathSilence(String path) {
		this.path = path;
	}

	
	
	/**
	 * {@inheritDoc}
	 */
	final public URL getRequestUrl() {
		try {
			String p = path;
			String queryString = getQueryString(); 
			if (queryString != null) {
				p += "?" + queryString;
			}
			
			String srvName = getServerName();  // resolves implicit host field & serverport; 
			if (isSecure) {
				if (serverport != null) {
					return new URL("https", srvName, serverport, p);
				} else {
					return new URL("https", srvName, p);
				}
			} else {
				if (serverport != null) {
					return new URL("http", srvName, serverport, p);
				} else {
					return new URL("http", srvName, p);
				}
			}
			
		} catch (MalformedURLException use) {
			throw new RuntimeException(use.toString()); 
		}
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	final public void setRequestUrl(URL url) {

		String protocol = url.getProtocol();
		if (protocol != null) {
			if (protocol.equalsIgnoreCase("https")) {
				isSecure = true;
			} else {
				isSecure = false;
			}
		}
		
		path = url.getPath();
		if (path.trim().length() == 0) {
			path = "/";
		}
		queryString = url.getQuery();
		
		if (queryParameters != null) {
			queryParameters.clear();
		}

		if (IS_UPDATE_HOSTHEADER && (url.getHost() != null)) {
		    servername = url.getHost();
            int port = url.getPort();
            if (port != -1) {
                serverport = port;
            } else {
                serverport = null;
            }

            if (serverport != null) {
                host = servername + ":" + serverport;
            } else {
                host = servername;
            }
                       
            setHost(host);    				
		}
	}


	
	/**
	 * {@inheritDoc}
	 */
	final public boolean isSecure() {
		return isSecure;
	}
	
	/**
     * {@inheritDoc}
     */
	final public String getScheme() {
	    if (isSecure()) {
	        return "https";
	    } else {
	        return "http";
	    }
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	final public String getMethod() {
		return method;
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	final public void setMethod(String method) {
		setMethodSilence(method);
	}

	
	/**
	 * sets the method silence
	 * @param method the method 
	 */
	final void setMethodSilence(String method) {
		this.method = method;
	}

		
	
	/**
	 * {@inheritDoc}
	 */
	final public String getServerName() {
		
		if (servername == null) {
			resolveHostField();
			
			if ((servername == null) && (tcpConnection != null)) {
				return tcpConnection.getLocalAddress().getHostName();
			}
		}
			
		return servername;
	}

	
	
	
	/**
	 * {@inheritDoc}
	 */
	final public int getServerPort() {
		
		if (serverport == null) {
			resolveHostField();
			
			if (serverport == null) {
			
				if (servername != null) {
					if (isSecure()) {
						return 443;
					} else {
						return 80;
					}
				}
				
				if (tcpConnection != null) {
					return tcpConnection.getLocalPort();
					
				} else {
					serverport = -1;
				}
			}
		}

		return serverport;
	}
	
	
	
	private void resolveHostField() {
		if (servername == null) {
			String host = getHeader("HOST");
				
			if (host != null) {
				int pos = host.lastIndexOf(":");
					
				if (pos == -1) {
					servername = host;
						
				} else {
					servername =  host.substring(0, pos);
					serverport = Integer.parseInt(host.substring(pos + 1, host.length()).trim());
				}
			} 
		}
	}


	


	/**
	 * {@inheritDoc}
	 */
	final public void removeParameter(String parameterName) {
       if ((getContentType() != null) && (getContentType().startsWith("application/x-www-form-urlencoded"))) {
            LOG.warning("parameter will be removed from URI even though request is from url encoded (if prameter is contained in body this operation will be ignored");
        }

	    
		isQueryParamMapModified = true;
		
		List<Parameter> paramsToRemove = new ArrayList<Parameter>(); 
		
		for (Parameter parameter : getQueryParamList()) {
			if (parameter.isDecodedNameSame(parameterName)) {
				paramsToRemove.add(parameter);
			}
		}

		queryParameters.removeAll(paramsToRemove);
	}

	
	
	
	/**
	 * {@inheritDoc}
	 */
	final public void setParameter(String parameterName, String parameterValue) {
	    if ((getContentType() != null) && (getContentType().startsWith("application/x-www-form-urlencoded"))) {
	        LOG.warning("parameter will be set in URI even though request is form url encoded (other parameter can be contained in body");
	    }
	    
	    if (queryParameterResolveRequired) {
	        queryParameterResolveRequired = false;
	        resolveParameters();
	    }
	    
		removeParameter(parameterName);
		getQueryParamList().add(new Parameter(parameterName, parameterValue, true));
	}

	
	/**
     * {@inheritDoc}
     */
	final public void addParameter(String parameterName, String parameterValue) {
	       if ((getContentType() != null) && (getContentType().startsWith("application/x-www-form-urlencoded"))) {
	            LOG.warning("parameter will be added to URI even though request is form url encoded (other parameter can be contained in body)");
	        }

        if (queryParameterResolveRequired) {
            queryParameterResolveRequired = false;
            resolveParameters();
        }
        
        getQueryParamList().add(new Parameter(parameterName, parameterValue, true));
        isQueryParamMapModified = true;
	}

	
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	final public Enumeration getParameterNames() {
		return Collections.enumeration(getParameterNameSet());
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	final public Set<String> getParameterNameSet() {
		
		Set<String> result = new HashSet<String>();
		
		
		if (queryParameterResolveRequired) {
			queryParameterResolveRequired = false;
			resolveParameters();
		}
		
		for (Parameter param : getQueryParamList()) {
			result.add(param.getDecodedName());
		}
		
		return result;
	}
	
	
	/**
     * {@inheritDoc}
     */
	final public String getParameter(String name, String defaultVal) {
        
        String param = getParameter(name);
        if (param == null) {
            param = defaultVal;
        }
        
        return param;
    }
	
	
	/**
	 * {@inheritDoc}
	 */
	final public String getParameter(String name) {
		
		if (queryParameterResolveRequired) {
			queryParameterResolveRequired = false;
			resolveParameters();
		}
		
		for (Parameter param : getQueryParamList()) {
			if (param.isDecodedNameSame(name)) {
				return param.getDecodedValue();
			}
		}
		
		return null;
	}
	
	
	private void resolveParameters() {
		if (queryString != null) {
			Map<String, List<String>> p = HttpUtils.parseParamters(queryString, DEFAULT_ENCODING);
			for (Entry<String, List<String>> entry  : p.entrySet()) {
				for (String value : entry.getValue()) {
					getQueryParamList().add(new Parameter(entry.getKey(), value, true));
				}
			}
		}
	}

	
    /**
     * {@inheritDoc}
     */
	final public void setMatrixParameter(String parameterName, String parameterValue) {
	    matrixParameterResolveRequired = true;
	    getMatrixParamList().clear();
        
        removeMatrixParameter(parameterName);
        getMatrixParamList().add(new Parameter(parameterName, parameterValue, true));
        
        path = HttpUtils.addMatrixParamter(path, parameterName, parameterValue, DEFAULT_ENCODING);
	}


    /**
     * {@inheritDoc}
     */
	final public void addMatrixParameter(String parameterName, String parameterValue) {
        matrixParameterResolveRequired = true;
        getMatrixParamList().clear();
        
        getMatrixParamList().add(new Parameter(parameterName, parameterValue, true));
        
        path = HttpUtils.addMatrixParamter(path, parameterName, parameterValue, DEFAULT_ENCODING);
    }
	
	
	
	/**
     * {@inheritDoc}
     */
	final public void removeMatrixParameter(String parameterName) {

	    if (matrixParameterResolveRequired) {
            matrixParameterResolveRequired = false;
            resolveMatrixParameters();
        }
	    
	    List<Parameter> paramsToRemove = new ArrayList<Parameter>(); 
        
        for (Parameter parameter : getMatrixParamList()) {
            if (parameter.isDecodedNameSame(parameterName)) {
                paramsToRemove.add(parameter);
            }
        }

        matrixParameters.removeAll(paramsToRemove);
        path = HttpUtils.removeMatrixParamter(path, parameterName);
   }

	
	
	
	/**
	 * {@inheritDoc}
	 */
	final public String[] getMatrixParameterValues(String name) {
	    List<String> result = new ArrayList<String>();
        
        
        if (matrixParameterResolveRequired) {
            matrixParameterResolveRequired = false;
            resolveMatrixParameters();
        }
        
        for (Parameter param : getMatrixParamList()) {
            if (param.isDecodedNameSame(name)) {
                result.add(param.getDecodedValue());
            }
        }
        
        return result.toArray(new String[result.size()]);
	}
	
	
	/**
     * {@inheritDoc}
     */
	final public Set<String> getMatrixParameterNameSet() {
        
        Set<String> result = new HashSet<String>();
        
        
        if (matrixParameterResolveRequired) {
            matrixParameterResolveRequired = false;
            resolveMatrixParameters();
        }
        
        for (Parameter param : getMatrixParamList()) {
            result.add(param.getDecodedName());
        }
        
        return result;
    }
	
	
	
	/**
	 * {@inheritDoc}
	 */
	final public String getMatrixParameter(String name) {
	    if (matrixParameterResolveRequired) {
            matrixParameterResolveRequired = false;
            resolveMatrixParameters();
        }
        
        for (Parameter param : getMatrixParamList()) {
            if (param.isDecodedNameSame(name)) {
                return param.getDecodedValue();
            }
        }
        
        return null;
	}

	
	   private void resolveMatrixParameters() {
	        if (path != null) {
	            Map<String, List<String>> p = HttpUtils.parseMatrixParamters(path, getCharacterEncoding());
	            for (Entry<String, List<String>> entry  : p.entrySet()) {
	                for (String value : entry.getValue()) {
	                    getMatrixParamList().add(new Parameter(entry.getKey(), value, true));
	                }
	            }
	        }
	    }
	
	
	
	
	/**
	 * {@inheritDoc}
	 */
	final public String[] getParameterValues(String name) {
		List<String> result = new ArrayList<String>();
		
		
		if (queryParameterResolveRequired) {
			queryParameterResolveRequired = false;
			resolveParameters();
		}
		
		for (Parameter param : getQueryParamList()) {
			if (param.isDecodedNameSame(name)) {
				result.add(param.getDecodedValue());
			}
		}
		
		return result.toArray(new String[result.size()]);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	final public String getRequiredStringParameter(String name) throws BadMessageException {
		String s  = getParameter(name);
		if (s != null) {
			return s;
		} else {
			throw new BadMessageException("mandatory parameter '" + name + "' is not set");
		}
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	final public Integer getIntParameter(String name) throws BadMessageException {
		String s = getParameter(name);
		if (s != null) {
			try {
				return Integer.parseInt(s);
			} catch(NumberFormatException nfe) {
				throw new BadMessageException("parameter '" + name + "' is not a number");
			}
		} else {
			return null;
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	final public int getRequiredIntParameter(String name) throws BadMessageException {
		return Integer.parseInt(getRequiredStringParameter(name));
	}
	
	/**
	 * {@inheritDoc}
	 */
	final public int getIntParameter(String name, int defaultVal) {
		String s = getParameter(name);
		if (s != null) {
			try {
				return Integer.parseInt(s);
			} catch (Exception e) {
				return defaultVal;
			}
		} else {
			return defaultVal;
		}
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	final public Long getLongParameter(String name) throws BadMessageException {
		String s = getParameter(name);
		if (s != null) {
			try {
				return Long.parseLong(s);
			} catch(NumberFormatException nfe) {
				throw new BadMessageException("parameter '" + name + "' is not a number");
			}
		} else {
			return null;
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	final public long getRequiredLongParameter(String name) throws BadMessageException {
		return Long.parseLong(getRequiredStringParameter(name));
	}

	
	/**
	 * {@inheritDoc}
	 */
	final public long getLongParameter(String name, long defaultVal) {
		String s = getParameter(name);
		if (s != null) {
			try {
				return Long.parseLong(s);
			} catch (Exception e) {
				return defaultVal;
			}
		} else {
			return defaultVal;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	final public Double getDoubleParameter(String name) throws BadMessageException {
		String s = getParameter(name);
		if (s != null) {
			try {
				return Double.parseDouble(s);
			} catch(NumberFormatException nfe) {
				throw new BadMessageException("parameter '" + name + "' is not a number");
			}
		} else {
			return null;
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	final public double getRequiredDoubleParameter(String name) throws BadMessageException {
		return Double.parseDouble(getRequiredStringParameter(name));
	}

	
	/**
	 * {@inheritDoc}
	 */
	final public double getDoubleParameter(String name, double defaultVal) {
		String s = getParameter(name);
		if (s != null) {
			try {
				return Double.parseDouble(s);
			} catch (Exception e) {
				return defaultVal;
			}
		} else {
			return defaultVal;
		}
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	final public Float getFloatParameter(String name) throws BadMessageException {
		String s = getParameter(name);
		if (s != null) {
			try {
				return Float.parseFloat(s);
			} catch(NumberFormatException nfe) {
				throw new BadMessageException("parameter '" + name + "' is not a number");
			}
		} else {
			return null;
		}
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	final public float getRequiredFloatParameter(String name) throws BadMessageException {
		return Float.parseFloat(getRequiredStringParameter(name));
	}
	

	/**
	 * {@inheritDoc}
	 */
	final public float getFloatParameter(String name, float defaultVal) {
		String s = getParameter(name);
		if (s != null) {
			try {
				return Float.parseFloat(s);
			} catch (Exception e) {
				return defaultVal;
			}
		} else {
			return defaultVal;
		}
	}
	
	

	/**
	 * {@inheritDoc}
	 */
	final public Boolean getBooleanParameter(String name) {
		String s = getParameter(name);
		if (s != null) {
			return Boolean.parseBoolean(s);
		} else {
			return NULL_BOOLEAN;
		}
	}
	

	
	/**
	 * {@inheritDoc}
	 */
	final public boolean getRequiredBooleanParameter(String name) throws BadMessageException {
		return Boolean.parseBoolean(getRequiredStringParameter(name));
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	final public boolean getBooleanParameter(String name, boolean defaultVal) {
		String s = getParameter(name);
		if (s != null) {
			try {
				return Boolean.parseBoolean(s);
			} catch (Exception e) {
				return defaultVal;
			}
		} else {
			return defaultVal;
		}
	}
	
	
	/**
	 * add a raw query parameter silence
	 * 
	 * @param paramName       the param name
	 * @param paramValue      the param value
	 * @throws IOException if an exception occurs
	 */
	final void addRawQueryParameterSilence(String paramName, String paramValue) throws IOException {
		getQueryParamList().add(new Parameter(paramName, paramValue, false));
	}
	
	
	/**
     * set the query string silence
     * 
     * @param queryString  the query string
     */
	final void setQueryString(String queryString) {
        this.queryString = queryString;
    }
	
    
	private List<Parameter> getQueryParamList() {
		if (queryParameters == null) {
			queryParameters = new ArrayList<Parameter>();	
		}
		
		return queryParameters;
	}
	
	
	 
    private List<Parameter> getMatrixParamList() {
        if (matrixParameters == null) {
            matrixParameters = new ArrayList<Parameter>();   
        }
        
        return matrixParameters;
    }


	
	
	private void writeRequestLineTo(StringBuilder sb) {
		
		sb.append(getMethod() + " " + path);

		
		
		if (isQueryParamMapModified) {

			boolean isFirstEntry = true; 
			for (Parameter param : getQueryParamList()) {
				
				if (isFirstEntry) {
					isFirstEntry = false;
					sb.append("?" + param.getEncodedName() + "=" + param.getEncodedValue());
				} else {
					sb.append("&" + param.getEncodedName() + "=" + param.getEncodedValue());
				}
			} 
			
		} else {
			String queryString = getQueryString(); 
			if (queryString != null) {
				sb.append("?");
				sb.append(queryString);
			} 			
		}

		
		if (getProtocolScheme() != null) {
			 sb.append(" ");
			 sb.append(getProtocolScheme());
		}
		
		if (getProtocolVersion() != null) {
			sb.append("/");
			sb.append(getProtocolVersion());
		}
		
		sb.append("\r\n");
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		writeRequestLineTo(sb);
		
		if (host != null) {
			sb.append("Host: ");
			sb.append(host);
			sb.append("\r\n");
		} 
		
		if (userAgent != null) {
			sb.append("User-Agent: ");
			sb.append(userAgent);
			sb.append("\r\n");
		}
		
		writeHeadersTo(sb);
		
		return sb.toString();
	}
		
	
	
	/**
	 * {@inheritDoc}
	 */
	final public IHttpRequestHeader copy() {
		try {
			return (IHttpRequestHeader) this.clone();
		} catch (CloneNotSupportedException cnse) {
			throw new RuntimeException(cnse.toString());
		}
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Object clone() throws CloneNotSupportedException {
		HttpRequestHeader copy = (HttpRequestHeader) super.clone();
		
		if (this.queryParameters != null) {
			copy.queryParameters = (ArrayList<Parameter>) queryParameters.clone();
		}
		
		return copy;
	}

	
	
	private static class Parameter {
		
		private String encodedName = null;
		private String decodedName = null;
		
		private String encodedValue = null;
		private String decodedValue = null;
		
		public Parameter(String name, String value, boolean isDecoded) {
			if (isDecoded) {
				this.decodedName = name;
				this.decodedValue = value;
			} else {
				this.encodedName = name;
				this.encodedValue = value;				
			}
		}
		
		String getDecodedName() {
			if (decodedName == null) {
				decodedName = decode(encodedName);
			}
			
			return decodedName;
		}
		
		String getEncodedName() {
			if (encodedName == null) {
				encodedName = encode(decodedName);
			}
			
			return encodedName;
		}
		
		
		String getDecodedValue() {
			if (decodedValue == null) {
				decodedValue = decode(encodedValue);
			}
			
			return decodedValue;
		}

		String getEncodedValue() {
			if (encodedValue == null) {
				encodedValue = encode(decodedValue);
			}
			
			return encodedValue;
		}

		
		
		boolean isDecodedNameSame(String decodedname) {
			return getDecodedName().equals(decodedname);
		}
		
		
		private static String decode(String encoded) {
			
			if (encoded == null) {
				return null;
				
			} else {
				try {
					return URLDecoder.decode(encoded, "UTF-8");
				} catch (UnsupportedEncodingException use) {
					throw new RuntimeException(use.toString());
				}
			}
		}
		
		
		private static String encode(String decoded) {
			
			if (decoded == null) {
				return null;
				
			} else {
				try {
					return URLEncoder.encode(decoded, "UTF-8");
				} catch (UnsupportedEncodingException use) {
					throw new RuntimeException(use.toString());
				}
			}
		}
		
		
		@Override
		public String toString() {
			return getDecodedName() + "=" + getDecodedValue();
		}
	} 
}

 