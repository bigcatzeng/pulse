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
public final class EventParserTest   {
	
    
    @Test
    public void testSimple() throws Exception {
        
        String s = "id: 1269854853\r\n" +
                   "data: MAILBOX_MODIFIED@http://localhost:4780/service/Mailbox/21103@1269854853@-1\r\n" +
                   "\r\n";
        
        Event event = Event.parse(s);
        Assert.assertEquals("1269854853", event.getId());
        Assert.assertEquals("MAILBOX_MODIFIED@http://localhost:4780/service/Mailbox/21103@1269854853@-1", event.getData());
    }
    
    
    @Test
    public void testSimple2() throws Exception {
        
        String s = "id: 1269854853  \r\n" +
                   "data: MAILBOX_MODIFIED@http://localhost:4780/service/Mailbox/21103@1269854853@-1\r\n" +
                   "\r\n";
        
        Event event = Event.parse(s);
        Assert.assertEquals("1269854853  ", event.getId());
        Assert.assertEquals("MAILBOX_MODIFIED@http://localhost:4780/service/Mailbox/21103@1269854853@-1", event.getData());
    }    
}