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
import java.net.MalformedURLException;







/**
 * a GET request 
 * 
 * @author grro@xlightweb.org
 */
public class GetRequest extends HttpRequest  {
	
	
	/**
	 * constructor 
	 * 
	 * @param url  the url string
	 */
	public GetRequest(String url) throws MalformedURLException {
		super(new HttpRequestHeader("GET", url));
	}
	

    
    /**
     * constructor 
     * 
     * @param url   the url string
     * @param body  the body
     */
    GetRequest(String url, byte[] data) throws MalformedURLException, IOException {
        super(new HttpRequestHeader("GET", url), data);
    }
    
    
    /**
     * constrcutor 
     * 
     * @param url              the url string
     * @param queryParameters  the query parameters
     */
    public GetRequest(String url, NameValuePair... queryParameters) throws MalformedURLException {
        super(new HttpRequestHeader("GET", enhanceUrl(url, queryParameters)));
    }
    
    static String enhanceUrl(String url, NameValuePair[] queryParameters) {
        if ((queryParameters == null) || (queryParameters.length == 0)) {
            return url;
        }
        
        String encodedQueryParams = new MultivalueMap(IHttpMessageHeader.DEFAULT_ENCODING, queryParameters).toString();
        
        int idx = url.indexOf("?");
        if (idx != -1) {
            String queryString = url.substring(idx, url.length()).trim();
            if (queryString.indexOf("=") != -1) {
                return url + encodedQueryParams; 
            } 
            
            return url + encodedQueryParams;
        }
        
        return url + "?" + encodedQueryParams;
    }
    
    

    /**
     * @deprecated
     */
    public GetRequest(String url, String... queryParameters) throws MalformedURLException {
        super(new HttpRequestHeader("GET", enhanceUrl(url, queryParameters)));
    }
    
    private static String enhanceUrl(String url, String[] queryParameters) {
        if ((queryParameters == null) || (queryParameters.length == 0)) {
            return url;
        }
        
        String encodedQueryParams = new MultivalueMap(IHttpMessageHeader.DEFAULT_ENCODING, queryParameters).toString();
        
        int idx = url.indexOf("?");
        if (idx != -1) {
            String queryString = url.substring(idx, url.length()).trim();
            if (queryString.indexOf("=") != -1) {
                return url + encodedQueryParams; 
            } 
            
            return url + encodedQueryParams;
        }
        
        return url + "?" + encodedQueryParams;
    }
}
