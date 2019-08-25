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

import java.io.IOException;
import java.util.logging.Logger;



/**
 * Checked exception thrown when a if a bad message occurs
 *
 * @author grro@xlightweb.org
 */
public final class BadMessageException extends IOException {
	
	private static final Logger LOG = Logger.getLogger(BadMessageException.class.getName());
	
 
	private static final long serialVersionUID = -8030258963989509537L;

	private final int statusCode; 
	
	/**
	 * constructor 
	 * 
	 * @param reason  the reason
	 */
	public BadMessageException(String reason) {
		this(400, reason);
	}
	
	/**
	 * constructor 
	 * 
	 * @param statusCode the status code
	 */
	public BadMessageException(int statusCode) {
		this(statusCode, HttpUtils.getReason(statusCode));
	}
	
	
	
	/**
	 * constructor 
	 * 
	 * @param statusCode the status code
	 * @param reason     the reason
	 */
	public BadMessageException(int statusCode, String reason) {
		super(reason);
		
		this.statusCode = statusCode;
		
		if ((statusCode < 400)) {
			LOG.warning("status code should be 4xx");
		}
	}
	
	
	/**
	 * get the status code
	 * 
	 * @return the status code
	 */
	public int getStatus() {
		return statusCode;
	}
}	