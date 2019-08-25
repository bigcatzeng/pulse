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
import java.net.MalformedURLException;
import java.nio.ByteBuffer;




/**
 * POST request
 * 
 * @author grro@xlightweb.org
 */
public class PostRequest extends HttpRequest  {
	
	
	
	
	/**
	 * constructor 
	 * 
	 * @param url   the url string
	 * @throws MalformedURLException if the url is malformed
	 */
	public PostRequest(String url) throws MalformedURLException {
		super(new HttpRequestHeader("POST", url));
	}
	
	/**
	 * constructor 
	 * 
	 * @param url           the url string
	 * @param contentType   the content type
	 * @throws MalformedURLException if the url is malformed
	 * @throws IOException if an exception occurs
	 */
	public PostRequest(String url, String contentType) throws IOException, MalformedURLException {
		super("POST", url);
		setContentType(contentType);
	}

	
	/**
	 * constructor 
	 * 
	 * @param url           the url string
	 * @param contentType   the content type
	 * @param body          the body
	 * @throws MalformedURLException if the url is malformed
	 * @throws IOException if an exception occurs  
	 */
	public PostRequest(String url, String contentType, String body) throws IOException, MalformedURLException {
		this(url, contentType, body, false);		
	}
	
	/**
     * constructor 
     * 
     * @param url           the url string
     * @param contentType   the content type
     * @param body          the body
     * @param compress      true, if the body should be compressed 
     * @throws MalformedURLException if the url is malformed
     * @throws IOException if an exception occurs  
     */
    public PostRequest(String url, String contentType, String body, boolean compress) throws IOException, MalformedURLException {
        super("POST", url, HttpUtils.addEncodingIfNotPresent(contentType), body, compress);      
    }

   
	
	
	/**
	 * constructor 
	 * 
	 * @param url           the url string
	 * @param file          the file 
	 * @throws MalformedURLException if the url is malformed
	 * @throws IOException if an exception occurs 
	 */
	public PostRequest(String url, File file) throws IOException, MalformedURLException {
		super(new HttpRequestHeader("POST", url, HttpUtils.resolveContentTypeByFileExtension(file)), file);
	}
	
	

    /**
     * constructor 
     * 
     * @param url           the url string
     * @param file          the file
     * @param compress      true, if the body should be compressed
     * @throws MalformedURLException if the url is malformed
     * @throws IOException if an exception occurs 
     */
    public PostRequest(String url, File file, boolean compress) throws IOException, MalformedURLException {
        super(new HttpRequestHeader("POST", url, HttpUtils.resolveContentTypeByFileExtension(file)), HttpUtils.readFile(file), compress);
    }
	
	
	/**
	 * constructor 
	 * 
	 * @param url           the url string
	 * @param encoding      the encoding
	 * @param contentType   the content type
	 * @param body          the body 
	 * @throws MalformedURLException if the url is malformed
	 * @throws IOException if an exception occurs 
	 */
	public PostRequest(String url, String contentType, String encoding, String body) throws IOException, MalformedURLException {
		this(url, contentType, encoding, body, false);		
	}
	
	
	/**
     * constructor 
     * 
     * @param url           the url string
     * @param encoding      the encoding
     * @param contentType   the content type
     * @param body          the body 
     * @param compress      true, if the body should be compressed 
     * @throws MalformedURLException if the url is malformed
     * @throws IOException if an exception occurs 
     */
    public PostRequest(String url, String contentType, String encoding, String body, boolean compress) throws IOException, MalformedURLException {
        super("POST", url, contentType, encoding, body, compress);        
    }
	
	
	/**
	 * constructor 
	 * 
	 * @param url           the url string 
	 * @param contentType   the content type
	 * @param body          the body 
	 * @throws MalformedURLException if the url is malformed
	 * @throws IOException if an exception occurs 
	 */
	public PostRequest(String url, String contentType, byte[] body) throws IOException, MalformedURLException {
		this(url, contentType, body, false);
	}
	
	
	/**
     * constructor 
     * 
     * @param url           the url string 
     * @param contentType   the content type
     * @param body          the body 
     * @param compress      true, if the body should be compressed 
     * @throws MalformedURLException if the url is malformed
     * @throws IOException if an exception occurs 
     */
    public PostRequest(String url, String contentType, byte[] body, boolean compress) throws IOException, MalformedURLException {
        super("POST", url, contentType, body, compress);
    }
    
    

	
	/**
	 * constructor 
	 * 
	 * @param url           the url string
	 * @param contentType   the content type
	 * @param body          the body
	 * @throws MalformedURLException if the url is malformed
	 * @throws IOException if an exception occurs  
	 */
	public PostRequest(String url, String contentType, ByteBuffer[] body) throws IOException, MalformedURLException {
		this(url, contentType, body, false);
	}
	
	

    /**
     * constructor 
     * 
     * @param url           the url string
     * @param contentType   the content type
     * @param body          the body
     * @param compress      true, if the body should be compressed 
     * @throws MalformedURLException if the url is malformed
     * @throws IOException if an exception occurs  
     */
    public PostRequest(String url, String contentType, ByteBuffer[] body, boolean compress) throws IOException, MalformedURLException {
        super("POST", url, contentType, body, compress);
    }
    
	
	
	/**
	 * constructor
	 * <pre>
     * PostRequest req = new PostRequest(urlBase + "/login", new NameValuePair("name", "ItsMe"));
     * ...
     * </pre>

	 *    
	 * @param url             the url string 
	 * @param formParameters  an Array of  FORM parameter
	 * @throws MalformedURLException if the url is malformed
	 * @throws IOException if an exception occurs 
	 */
	public PostRequest(String url, NameValuePair... formParameters) throws IOException, MalformedURLException {
	    super("POST", url, "application/x-www-form-urlencoded; charset=utf-8", new MultivalueMap("utf-8", formParameters).toString());
	}
	
	
	/**
     * @deprecated
     */
    public PostRequest(String url, String[] formParameters) throws IOException, MalformedURLException {
        super("POST", url, "application/x-www-form-urlencoded; charset=utf-8", new MultivalueMap("utf-8", formParameters).toString());
    }
}
