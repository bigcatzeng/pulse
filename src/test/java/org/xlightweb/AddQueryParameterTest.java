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





import org.junit.Assert;
import org.junit.Test;





/**
*
* @author grro@xlightweb.org
*/
public final class AddQueryParameterTest  {


	@Test
	public void testSimple() throws Exception {
	    GetRequest request = new GetRequest("http://www.gmx.com/", new NameValuePair("param1", "1"), new NameValuePair("param2", "2"));
	    request.addParameter("param3", "3");
	    
	    String s = request.toString();
	    Assert.assertTrue(s.indexOf("param1=1") != -1);
	    Assert.assertTrue(s.indexOf("param2=2") != -1);
	    Assert.assertTrue(s.indexOf("param3=3") != -1);
	}	
	
	
	@Test
	public void testSimple2() throws Exception {
	        
	    GetRequest request = new GetRequest("http://www.gmx.com/?param1=1&param2=2");
	    request.addParameter("param3", "3");
	        
	    String s = request.toString();
	    Assert.assertTrue(s.indexOf("param1=1") != -1);
	    Assert.assertTrue(s.indexOf("param2=2") != -1);
	    Assert.assertTrue(s.indexOf("param3=3") != -1);
	}
}