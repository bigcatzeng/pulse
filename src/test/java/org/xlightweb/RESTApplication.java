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




import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;





/**
*
* @author grro@xlightweb.org
*/
public final class RESTApplication extends Application {

	private Set<Object> singletons = new HashSet<Object>();
	
	public RESTApplication() {
	    singletons.add(new MyResource());
	}

	@Override
	public Set<Class<?>> getClasses() {
		return new HashSet<Class<?>>();
	}

	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}
	
	
	

	@Path("/MyResource")
	public static class MyResource {
		
		
		@POST
		@Consumes("multipart/*")
		public void createProcess(MultipartInput input) {
			for (InputPart part : input.getParts()) {
				System.out.println("OK");
			}
		}
		
	}	
}