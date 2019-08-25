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



import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;




/**
 * Annotation which defines the URL-pattern mapping. Currently the mapping annotation
 * is only supported by {@link Context#addHandler(IHttpRequestHandler)} 
 * 
 * 
 * Example:
 * 
 * <pre>
 * class MyRequestHandler implements IHttpRequestHandler {
 * 
 *    &#064Mapping("/rpc/*")
 *    public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
 *              
 *       //...
 *    }
 *    
 * }
 * </pre>
 * 
 * @author grro@xlightweb.org
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Mapping {
	
	String[] value();
}
