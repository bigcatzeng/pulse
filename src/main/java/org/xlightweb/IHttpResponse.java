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
 * Http response
 * 
 * @author grro@xlightweb.org
 */
public interface IHttpResponse extends IHttpMessage {
	
    
    

	/**
	 * returns the status 
	 * 
	 * @return the status 
	 */
	int getStatus();
	

	/**
	 * sets the status 
	 * 
	 * @param status the status 
	 */
	void setStatus(int status);
	
	
	/**
	 * returns the reason
	 * 
	 * @return the reason 
	 */
	String getReason();
	
	
	/**
	 * sets the reason 
	 * 
	 * @param reason the reason
	 */
	void setReason(String reason);
	
	
	/**
	 * returns the protocol 
	 * 
	 * @return the protocol
	 */
	String getProtocol();
	
	
	
	/**
	 * sets the protocol
	 *  
	 * @param protocol the protocol
	 */
	void setProtocol(String protocol);
	
	
	/**
	 * sets the Server header parameter
	 * 
	 * @param server the Server header parameter
	 */
	void setServer(String server);

	
	/**
	 * gets the Server header parameter
	 * 
	 * @return the Server header parameter
	 */
	String getServer();

	
    
    /**
	 * returns the Date header parameter or <code>null</code> if the header is not set
     *
     * @return the Date header parameter or <code>null</code> if the header is not set
	 */
	String getDate();
	
	
	/**
	 * sets the Date header parameter
	 * 
	 * @param date the Date header parameter
	 */
	void setDate(String date);
	
	
	
	/**
	 * returns the response header
	 *  
	 * @return the response header
	 */
	IHttpResponseHeader getResponseHeader();
}
