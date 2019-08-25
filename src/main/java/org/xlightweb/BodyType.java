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



/**
 * Body type
 * 
 * @author grro@xlightweb.org
 */
final class BodyType  {
	
	static final BodyType IN_MEMORY = new BodyType("InMemory");
	static final BodyType FULL_MESSAGE = new BodyType("FullMessage");
	static final BodyType FULL_MESSAGE_CHUNKED = new BodyType("FullMessageChunked");
	static final BodyType SIMPLE_MESSAGE = new BodyType("SimpleMessage");
	static final BodyType NO_BODY = new BodyType("NoBody");

	
	
	private final String name;
	
	private BodyType(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}	
}