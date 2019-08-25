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




import junit.framework.Assert;

import org.junit.Test;




/**
*
* @author grro@xlightweb.org
*/
public final class CompressTest  {

    
    @Test
    public void testComressAndUncompress() throws Exception {
        
        String txt = "test 1234567";
        byte[] compressed = HttpUtils.compress(txt.getBytes("US-ASCII"));
        byte[] plainData = HttpUtils.decompress(compressed);
        
        Assert.assertEquals("test 1234567", new String(plainData, "US-ASCII"));
    }

    
    
    @Test
    public void testComressAndUncompress2() throws Exception {
        
        PostRequest request = new PostRequest("http://localhost:8080/", "text/plain", "test 1234567", true);
        byte[] data  = request.getBody().readBytes();
        
        byte[] plainData = HttpUtils.decompress(data);
        String txt = new String(plainData, request.getCharacterEncoding());
        
        Assert.assertEquals("test 1234567", txt);
    }
	
	

    @Test
    public void testComressAndUncompress3() throws Exception {
        
        PostRequest request = new PostRequest("http://localhost:8080/", "text/plain", "test 1234567", true);
        
        BodyDataSource dataSource = HttpUtils.decompress(request.getBody());
        Assert.assertEquals("test 1234567", dataSource.readString());
    }
    
    

    @Test
    public void testComressAndUncompress4() throws Exception {
        
        PostRequest request = new PostRequest("http://localhost:8080/", "text/plain", "test 1234567", true);
        
        IHttpRequest requestCopy = HttpUtils.copy(request);
        BodyDataSource dataSource = HttpUtils.decompress(requestCopy.getBody());
        Assert.assertEquals("test 1234567", dataSource.readString());
    }    
}