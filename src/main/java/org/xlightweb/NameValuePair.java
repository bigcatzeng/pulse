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

import java.util.ArrayList;
import java.util.List;




/**
 * Name-Value-Pair
 * 
 * @author grro@xlightweb.org
 *
 */
public final class NameValuePair {

	private final String name;
	private final String value;
	
	
	
	
	/**
	 * constructor 
	 * 
	 * @param nvp  the name value pair "{name}={value}"
	 */
	public NameValuePair(String nvp) {
        int idx = nvp.indexOf("=");
        if (idx > 0) {
            name = nvp.substring(0, idx);
            value = nvp.substring(idx + 1, nvp.length());
        } else {
            throw new RuntimeException("illegal format: '" + nvp +  "' (usage: <key>=<value>)");
        }
    }

	
	
	/**
	 * constructor 
	 * 
	 * @param name  the name
	 * @param value the value
	 */
	public NameValuePair(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	
	static NameValuePair[] newPairs(String... nvps) {
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		
        for (String nvp : nvps) {
        	pairs.add(new NameValuePair(nvp));
        }
        
        return pairs.toArray(new NameValuePair[pairs.size()]);
	}
	
	
	/**
	 * return the name
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	
	/**
	 * return the value 
	 * 
	 * @return the value;
	 */
	public String getValue() {
		return value;
	}
}
	    
