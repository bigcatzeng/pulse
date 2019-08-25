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
import java.util.Enumeration;
import java.util.List;
import java.util.Set;






/**
 * Http Request Header
 * 
 * @author grro@xlightweb.org
 */
public interface IHttpRequestHeader extends IHttpMessageHeader {
	 

	/**
	 * Returns the fully qualified name of the client 
	 * or the last proxy that sent the request. If the 
	 * engine cannot or chooses not to resolve the hostname
	 * (to improve performance), this method returns the 
	 * dotted-string form of the IP address.
	 * 
	 * @return a String containing the fully qualified name of the client
	 */
	String getRemoteHost();
	
	
	/**
	 * Returns the Internet Protocol (IP) source 
	 * port of the client or last proxy that sent the request.
	 * 
	 * @return an integer specifying the port number
	 */
	int getRemotePort();
	

	/**
	 * Returns the Internet Protocol (IP) address of the client or last proxy that
	 * sent the request.
	 * 
	 * @return a String containing the IP address of the client that sent the request
	 */
	String getRemoteAddr();
	
	
	
	/**
	 * Returns the query string that is contained in the request URL 
	 * after the path. This method returns <code>null</code> 
	 * if the URL does not have a query string.
	 * 
	 * @return a String containing the query string or null if the URL contains no query string. 
	 */
	String getQueryString();
	
	
	  
    /**
     * Returns the portion of the request URI that indicates 
     * the context of the request. The context path always comes first 
     * in a request URI. For the default (root) context, this method returns "".
     *  
     * 
     * @see IHttpRequest#getRequestHandlerPath()
     * 
     * @return the context path
     */
    String getContextPath();
    
    
    /**
     * sets the context path. 
     *  
     * @param contextPath the context path
     */
    void setContextPath(String contextPath);
 
    
    /**
     * Returns any extra path information associated with the URL.
     * 
     * @return a String specifying extra path information that comes after the request handler
     *         and context path but before the query string in the request URL; 
     *         or <code>null</code> if the URL does not have any extra path information
     */
    String getPathInfo(); 
    
    
    /**
     * Returns any extra path information associated with the URL. 
     * 
     * @param removeSurroundingSlashs  true, if surrounding slashs wil lbe removed 
     * @return a String specifying extra path information that comes after the request handler
     *         and context path but before the query string in the request URL; 
     *         or <code>null</code> if the URL does not have any extra path information
     */
    String getPathInfo(boolean removeSurroundingSlashs); 

	
	
	/**
	 * Returns the part of this request's URL from the protocol
	 * name up to the query string in the first line of the HTTP request.
	 * 
	 * @return a String containing the part of the URL from the protocol 
	 *         name up to the query string
	 */
	String getRequestURI();
	
	
	/**
     * returns the request handler path. By default this method returns "".<br><br>
     * 
       * 
     * @return the request handler path
     */
    String getRequestHandlerPath();

    
    
    /**
     * sets the request handler path
     * 
     * @param requestHandlerPath  the request handler path
     */
    void setRequestHandlerPath(String requestHandlerPath);
    


	/**
	 * set the request uri part of this request's URL from the protocol
	 * name up to the query string in the first line of the HTTP request.
	 * 
	 * @param requestUri  the request uri
	 */
	void setRequestURI(String requestUri);
	
	
	/**
	 * Reconstructs the URL the client used to make the request.
	 * 
	 * @return the URL
	 */
	URL getRequestUrl();
	
	
	/**
	 * set the request url
	 * 
	 * @param url the request url
	 */
	void setRequestUrl(URL url);
	
	
	
	/**
	 * Returns a boolean indicating whether this request 
	 * was made using a secure channel, such as HTTPS.
	 *   
	 * @return a boolean indicating if the request was made using a secure channel
	 */
	boolean isSecure();
	
	
	
	/**
	 * Returns the name of the HTTP method with
	 * which this request was made, for example, GET, POST, or PUT.
	 * 
	 * @return a String specifying the name of the method 
	 */
	String getMethod();
	
	
	
	/**
	 * Sets the name of the HTTP method 
	 * 
	 * @param method a String specifying the name of the method
	 */
	void setMethod(String method);

	
	/**
	 * returns the Host header parameter or <code>null</code> if the header is not set
     *
     * @return the Host header parameter or <code>null</code> if the header is not set
	 */
	String getHost();

	
	/**
	 * sets the Host header (e.g. www.gmx.com or www.gmx.com:9900). 
	 * If the port value is -1, it will be removed, automatically
	 * 
	 * @param host the Host header
	 */
	void setHost(String host);
	
	
	/**
	 * returns the User-Agent header parameter or <code>null</code> if the header is not set
     *
     * @return the User-Agent header parameter or <code>null</code> if the header is not set
	 */
	String getUserAgent();
	
	
	/**
	 * sets the User-Agent header
	 * @param userAgent the User-Agent header
	 */
	void setUserAgent(String userAgent);
	
	
	/**
	 * Returns the host name of the server to which the request was sent. 
	 * It is the value of the part before ":" in the Host  header value, 
	 * if any, or the resolved server name, or the server IP address.
	 * 
	 * @return the server name
	 */
	String getServerName();
	
	
	/**
	 * Returns the port number to which the request was sent. It is the 
	 * value of the part after ":" in the Host  header value, if any, 
	 * or the server port where the client connection was accepted on.
	 * 
	 * @return the server port
	 */
	int getServerPort();	
		
	
	/**
     * returns the list of the accepted content types, ordered by the quality factory
     *  
     * @return the accepted content types
     */
    List<ContentType> getAccept();

    

    /**
     * @deprecated
     */
    void setKeepAlive(String keepAlive);
    
    
    /**
     * @deprecated
     */
    String getKeepAlive();
    
    

    /**
     * @deprecated
     */
    String getUpgrade();
    
    
    /**
     * @deprecated
     */
    void setUpgrade(String upgrade);
    
 
    
    /**
     * Returns the name of the scheme used to make this request, for example, http or https.
     * 
     * @return a String containing the name of the scheme
     */
    String getScheme();
    

    /**
     * returns the matrix parameter name set
     *  
     * @return the matrix parameter name set
     */
    Set<String> getMatrixParameterNameSet();
       
       
    
    /**
     * Returns the value of a request matrix parameter as a String, or null 
     * if the parameter does not exist. Request parameters are extra 
     * information sent with the request. 
     * 
     * @param name a String specifying the name of the matrix parameter
     * @return a String representing the single value of the matrix parameter 
     */
    String getMatrixParameter(String name);
    
    
    
    /**
     * Returns an array of String objects containing all of the values 
     * the given request matrix parameter has, or null if the parameter does not exist.
     * If the natrix parameter has a single value, the array has a length of 1.
     *  
     * @param name a String specifying the name of the matrix parameter
     * @return an array of String objects containing the matrix parameter's values
     */
    String[] getMatrixParameterValues(String name);

    
    /**
     * sets a matrix parameter 
     * 
     * @param parameterName   the parameter name 
     * @param parameterValue  the parameter value
     */
    void setMatrixParameter(String parameterName, String parameterValue);
   
    
    /**
     * adds a matrix parameter 
     * 
     * @param parameterName   the parameter name 
     * @param parameterValue  the parameter value
     */
    void addMatrixParameter(String parameterName, String parameterValue);
    
    
    
    /**
     * removes a matrix parameter 
     * 
     * @param parameterName   the parameter name 
     */
    void removeMatrixParameter(String parameterName);


    
    /**
     * remove a parameter 
     * 
     * @param parameterName   the parameter name 
     * @param parameterValue  the parameter value
     */
    void removeParameter(String parameterName);

	
	
	/**
	 * sets a parameter 
	 * 
	 * @param parameterName   the parameter name 
	 * @param parameterValue  the parameter value
	 */
	void setParameter(String parameterName, String parameterValue);


   /**
     * adds a parameter 
     * 
     * @param parameterName   the parameter name 
     * @param parameterValue  the parameter value
     */
    void addParameter(String parameterName, String parameterValue);
	
	
	/**
	 * Returns an Enumeration of String  objects containing the names 
	 * of the parameters contained in this request. If the request has no 
	 * parameters, the method returns an empty Enumeration. 
	 * 
	 * @return an Enumeration of String  objects, each String containing 
	 *         the name of a request parameter; or an empty Enumeration if the request has no parameters
	 */
	@SuppressWarnings("unchecked")
	Enumeration getParameterNames();
	
	
	/**
	 * returns the parameter name set
	 *  
	 * @return the parameter name set
	 */
	Set<String> getParameterNameSet();
	
	
	
	/**
	 * Returns the value of a request parameter as a String, or null 
	 * if the parameter does not exist. Request parameters are extra 
	 * information sent with the request. 
	 * 
	 * @param name a String specifying the name of the parameter
	 * @return a String representing the single value of the parameter 
	 */
	String getParameter(String name);
	

    /**
     * Get an String parameter, with a fallback value.  
     * 
     * @param name        the name of the parameter
     * @param defaultVal  the default value to use as fallback
     * @return the value
     */
    String getParameter(String name, String defaultVal);

	
	

	/**
	 * Returns an array of String objects containing all of the values 
	 * the given request parameter has, or null if the parameter does not exist.
	 * If the parameter has a single value, the array has a length of 1.
	 *  
	 * @param name a String specifying the name of the parameter
	 * @return an array of String objects containing the parameter's values
	 */
	String[] getParameterValues(String name);
	
	
	
	/**
     * Get an string parameter or throws an exception if parameter is not present
	 * 
	 * @param name    the parameter name
	 * @return the value
	 * @throws BadMessageException if the parameter is not present
	 */
	String getRequiredStringParameter(String name) throws BadMessageException;

	
	/**
	 * Get an Integer parameter, or null if not present.  
	 * 
	 * @param name the name of the parameter
	 * @return the value, or <code>null</code>
	 * @throws BadMessageException  if the parameter value is not a number
	 */
	Integer getIntParameter(String name) throws BadMessageException;
	
	
	
	/**
     * Get an int parameter or throws an exception if parameter is not present
	 * 
	 * @param name    the parameter name
	 * @return the value
	 * @throws BadMessageException if the parameter is not present or the parameter is not a number
	 */
	int getRequiredIntParameter(String name) throws BadMessageException;
	
	
	/**
	 * Get an int parameter, with a fallback value.  
	 * 
	 * @param name        the name of the parameter
	 * @param defaultVal  the default value to use as fallback
	 * @return the value
	 */
	int getIntParameter(String name, int defaultVal);
	
	
	
	/**
	 * Get an Long parameter, or null if not present.  
	 * 
	 * @param name the name of the parameter
	 * @return the value, or <code>null</code>
	 * @throws BadMessageException  if the parameter value is not a number 
	 */
	Long getLongParameter(String name) throws BadMessageException;
	
	
	/**
     * Get an long parameter or throws an exception if parameter is not present
	 * 
	 * @param name    the parameter name
	 * @return the value
	 * @throws BadMessageException if the parameter is not present or the parameter is not a number
	 */
	long getRequiredLongParameter(String name) throws BadMessageException;

	
	
	/**
	 * Get an long parameter, with a fallback value.  
	 * 
	 * @param name        the name of the parameter
	 * @param defaultVal  the default value to use as fallback
	 * @return the value
	 */
	long getLongParameter(String name, long defaultVal);
	
	

	
	/**
	 * Get an Double parameter, or null if not present.  
	 * 
	 * @param name the name of the parameter
	 * @return the value, or <code>null</code>
	 * @throws BadMessageException if the parameter is not a number 
	 */
	Double getDoubleParameter(String name) throws BadMessageException;
	
	
	/**
     * Get an double parameter or throws an exception if parameter is not present
	 * 
	 * @param name    the parameter name
	 * @return the value
	 * @throws BadMessageException if the parameter is not present or the parameter value is not a number
	 */
	double getRequiredDoubleParameter(String name) throws BadMessageException;

	
	/**
	 * Get an double parameter, with a fallback value.  
	 * 
	 * @param name        the name of the parameter
	 * @param defaultVal  the default value to use as fallback
	 * @return the value
	 */
	double getDoubleParameter(String name, double defaultVal);
	
	
	
	
	/**
	 * Get an Float parameter, or null if not present.  
	 * 
	 * @param name the name of the parameter
	 * @return the value, or <code>null</code>
	 * @throws BadMessageException  if the parameter value is not a number 
	 */
	Float getFloatParameter(String name) throws BadMessageException;
	
	
	/**
     * Get an float parameter or throws an exception if parameter is not present
	 * 
	 * @param name    the parameter name
	 * @return the value
	 * @throws BadMessageException if the parameter is not present or the parameter value is not a number
	 */
	float getRequiredFloatParameter(String name) throws BadMessageException;

	
	
	/**
	 * Get an float parameter, with a fallback value.  
	 * 
	 * @param name        the name of the parameter
	 * @param defaultVal  the default value to use as fallback
	 * @return the value
	 */
	float getFloatParameter(String name, float defaultVal);
	

	
	
	/**
	 * Get an Boolean parameter, or null if not present.  
	 * 
	 * @param name the name of the parameter
	 * @return the value, or <code>null</code>
	 */
	Boolean getBooleanParameter(String name);
	
	
	/**
     * Get an boolean parameter or throws an exception if parameter is not present
	 * 
	 * @param name    the parameter name
	 * @return the value
	 * @throws BadMessageException if the parameter is not present
	 */
	boolean getRequiredBooleanParameter(String name) throws BadMessageException;

	
	
	
	/**
	 * Get an boolean parameter, with a fallback value.  
	 * 
	 * @param name        the name of the parameter
	 * @param defaultVal  the default value to use as fallback
	 * @return the value
	 */
	boolean getBooleanParameter(String name, boolean defaultVal);

	
	/**
	 * creates a copy of this header 
	 * 
	 * @return return the copy
	 */
	IHttpRequestHeader copy();
}

 