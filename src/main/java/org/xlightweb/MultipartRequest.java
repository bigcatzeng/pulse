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
import java.util.UUID;




/**
 * A multipart request.
 * 
 * <pre>
 *   MultipartRequest req = new MultipartRequest("POST", uri);
 *   
 *   Part part = new Part(myFile);
 *   req.addPart(part);
 *   
 *   Part part2 = new Part("text/plain", myText);
 *   req.addPart(part2);
 *   
 *   IHttpResponse resp = httpClient.call(req);
 *   // ...
 * </pre> 
 *  
 * @author grro@xlightweb.org
 */
public class MultipartRequest extends HttpRequest  {
	
   
 
	/**
	 * constructor 
	 * 
	 * @param method  the method
	 * @param url     the url string
	 * @throws MalformedURLException if the url is malformed
	 */
	public MultipartRequest(String method, String url) throws MalformedURLException, IOException {
		this(method, url, "multipart/mixed", null);
	}
	
	
	 /**
     * constructor 
     * 
     * @param method       the method
     * @param url          the url string
     * @param contentType  the contenttype
     * @throws MalformedURLException if the url is malformed
     */
    public MultipartRequest(String method, String url, String contentType) throws MalformedURLException, IOException {
        this(method, url, contentType, HttpUtils.parseMediaTypeParameter(contentType, "boundary", true, null));
    }
	
    
    private MultipartRequest(String method, String url, String contentType, String boundary) throws MalformedURLException, IOException {
        super(new HttpRequestHeader(method, url));
        
        if (boundary == null) {
            boundary = UUID.randomUUID().toString();
            contentType = contentType + "; boundary=" + boundary;
        }
        
        setBody(new MultipartDataSource(getRequestHeader(), null, "--" + boundary));
        
        setContentType(contentType);
        removeHeader("Content-Length");
        setHeader("Transfer-Encoding", "chunked");
    }


    MultipartDataSource getMultipartDataSource() throws IOException {
        return (MultipartDataSource) getNonBlockingBody();
    }
	
    /**
     * add a part
     * 
     * @param part   the part 
     * @throws IOException  if an exception occurs
     */
	public final void addPart(IPart part) throws IOException {
	    getMultipartDataSource().addPart(part);
	}
}
