/*
 *  Copyright (c) xlightweb.org, 2006 - 2009. All rights reserved.
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
public class HttpRequestTest {
	

	
	@Test
	public void testContentDisposition() throws Exception {
	    
	    HttpRequestHeader header = new HttpRequestHeader("POST", "http://xlightweb.org/upload", "text/plain");
	    header.setHeader("Content-Disposition", "attachment; filename=genome.jpeg; modification-date=\"Wed, 12 Feb 1997 16:29:51 -0500\"");
	    HttpRequest request = new HttpRequest(header, "dummy content");
	    
	    Assert.assertEquals("attachment; filename=genome.jpeg; modification-date=\"Wed, 12 Feb 1997 16:29:51 -0500\"", request.getDisposition());
	    Assert.assertEquals("attachment", request.getDispositionType());
	    Assert.assertEquals("genome.jpeg", request.getDispositionParam("filename"));
	}
	
	
    @Test
    public void testContentDispositionMissing() throws Exception {
        
        HttpRequestHeader header = new HttpRequestHeader("POST", "http://xlightweb.org/upload", "text/plain");
        HttpRequest request = new HttpRequest(header, "dummy content");
        
        Assert.assertEquals(null, request.getDisposition());
        Assert.assertEquals(null, request.getDispositionType());
        Assert.assertEquals(null, request.getDispositionParam("filename"));
    }	
}	
	
	
