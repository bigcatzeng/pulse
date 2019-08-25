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
import java.nio.ByteBuffer;




/**
 * OPTIONS request
 * 
 * @author grro@xlightweb.org
 */
public class OptionsRequest extends HttpRequest  {
	
	
	/**
	 * constructor 
	 * 
	 * @param url   the url string 
	 * @throws MalformedURLException if the url is malformed   
	 */
	public OptionsRequest(String url) throws MalformedURLException {
		super(new HttpRequestHeader("OPTIONS", url));
	}
	
	/**
	 * constructor 
	 * 
	 * @param url           the url string
	 * @param contentType   the content type
	 * @throws MalformedURLException if the url is malformed
	 * @throws IOException if an exception occurs   
	 */
	public OptionsRequest(String url, String contentType) throws IOException, MalformedURLException {
		super("OPTIONS", url);
		setContentType(contentType);
	}

	
	
	
	/**
	 * constructor 
	 * 
	 * @param url           the url
	 * @param contentType   the content type
	 * @param body          the body 
	 */
	public OptionsRequest(String url, String contentType, String body) throws IOException, MalformedURLException {
		this(url, contentType, body, false);	
	}
	
	
	
	   
    /**
     * constructor 
     * 
     * @param url           the url
     * @param contentType   the content type
     * @param body          the body 
     * @param compress      true, if the body should be compressed 
     */
    public OptionsRequest(String url, String contentType, String body, boolean compress) throws IOException, MalformedURLException {
        super("PUT", url, contentType, body, compress);       
    }
    
	
	
	/**
	 * constructor 
	 * 
	 * @param url           the url
	 * @param encoding      the encoding
	 * @param contentType   the content type
	 * @param body          the body 
	 */
	public OptionsRequest(String url, String contentType, String encoding, String body) throws IOException, MalformedURLException {
		this(url, contentType, encoding, body, false);		
	}
	
	
	/**
     * constructor 
     * 
     * @param url           the url
     * @param encoding      the encoding
     * @param contentType   the content type
     * @param body          the body
     * @param compress      true, if the body should be compressed  
     */
    public OptionsRequest(String url, String contentType, String encoding, String body, boolean compress) throws IOException, MalformedURLException {
        super("OPTIONS", url, contentType+ ";" + encoding, body, compress);       
    }


	
	/**
	 * constructor 
	 * 
	 * @param url           the url
	 * @param contentType   the content type
	 * @param body          the body 
	 */
	public OptionsRequest(String url, String contentType, byte[] body) throws IOException, MalformedURLException {
		this(url, contentType, body, false);
	}
	
	
	/**
     * constructor 
     * 
     * @param url           the url
     * @param contentType   the content type
     * @param body          the body 
     * @param compress      true, if the body should be compressed 
     */
    public OptionsRequest(String url, String contentType, byte[] body,boolean compress) throws IOException, MalformedURLException {
        super("OPTIONS", url, contentType, body, compress);
    }
    
    

	
	/**
	 * constructor 
	 * 
	 * @param url           the url
	 * @param contentType   the content type
	 * @param body          the body 
	 */
	public OptionsRequest(String url, String contentType, ByteBuffer[] body) throws IOException, MalformedURLException {
		this(url, contentType, body, false);
	}
	
	
	   
    /**
     * constructor 
     * 
     * @param url           the url
     * @param contentType   the content type
     * @param body          the body
     * @param compress      true, if the body should be compressed  
     */
    public OptionsRequest(String url, String contentType, ByteBuffer[] body, boolean compress) throws IOException, MalformedURLException {
        super("OPTIONS", url, contentType, body, compress);
    }
}
