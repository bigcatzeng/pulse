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
import org.xlightweb.GetRequest;


/**
*
* @author grro@xlightweb.org
*/
public final class RequestURLTest {


	@Test
	public void testFullURL() throws Exception {
		
		GetRequest req = new GetRequest("https://username:password@example.com:8042/over/there/index.dtb;type=animal?name=ferret#nose");
		
		Assert.assertEquals(true, req.isSecure());
		Assert.assertEquals("example.com", req.getServerName());
		Assert.assertEquals(8042, req.getServerPort());
		Assert.assertEquals("/over/there/index.dtb;type=animal", req.getRequestURI());
		Assert.assertEquals("name=ferret", req.getQueryString());
		
		Assert.assertEquals("https://example.com:8042/over/there/index.dtb;type=animal?name=ferret", req.getRequestUrl().toString());
	}
	
	
	@Test
	public void testMinimalURL() throws Exception {
		
		GetRequest req = new GetRequest("/");
		
		Assert.assertEquals(false, req.isSecure());
		Assert.assertNull(req.getServerName());
		Assert.assertEquals(-1, req.getServerPort());
		Assert.assertEquals("/", req.getRequestURI());
		Assert.assertNull(req.getQueryString());
		
		Assert.assertEquals("http:/", req.getRequestUrl().toString());
	}
}
