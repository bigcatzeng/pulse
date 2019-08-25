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





/**
 * A multipart/form-data request, which supports file uploads. Example:
 * 
 * <pre>
 *   MultipartFormDataRequest req = new MultipartFormDataRequest(url);
 *   req.addPart("file", file);
 *   req.addPart("description", "text/plain", "A unsigned ...");
 *   
 *   IHttpResponse resp = httpClient.call(req);
 *   // ...
 * </pre> 
 * 
 * see <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a> 
 * 
 * @author grro@xlightweb.org
 */
public class MultipartFormDataRequest extends MultipartRequest  {
	
 
	/**
	 * constructor 
	 * 
	 * @param url   the url string
	 * @throws MalformedURLException if the url is malformed
	 */
	public MultipartFormDataRequest(String url) throws MalformedURLException, IOException {
	    super("POST", url);
	}

	
    
    /**
     * @deprecated
     */
    public void addPart(String name, String content) throws IOException {
        addPart(name,  "text/plain", content);
    }
    
    
    
    /**
     * adds a part
     * 
     * @param name           the part name 
     * @param content        the content 
     * @throws IOException   if an exception occurs
     */
    public void addPart(String name, String contentType, String content) throws IOException {
        IPart part = new Part(contentType, content);
        part.setHeader("Content-Disposition", "form-data; name=\"" + name + "\"");
        
        addPart(part);
    }
    
    
    
    /**
     * adds a part
     *  
     * @param name           the part name
     * @param file           the file
     * @throws IOException   if an exception occurs
     */
    public void addPart(String name, File file) throws IOException {
        IPart part = new Part(file);
        part.setHeader("Content-Disposition", "form-data; name=\"" + name + "\"; filename=\"" + file.getName() + "\"");
        
        addPart(part);
    }   
}
