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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;





/**
 * This class represents a content type
 *
 * @author grro@xlightweb.org
 */
public final class ContentType {
	
	private String contentTypeString = null; 
	private String primaryType = ""; 
	private String subType = "";
	private String parameterString = null;
	private Map<String, String> parameters = null;

	

	/**
	 * Constructor that takes a Content-Type string. The String is parsed into its
	 * constituents: primaryType, subType and parameters.
	 *  
	 * @param contentTypeString the Content-Type string
	 */
	public ContentType(String contentTypeString) {
		contentTypeString = contentTypeString.trim();
		
		if (contentTypeString.endsWith(";")) {
			contentTypeString = contentTypeString.substring(0, contentTypeString.length() - 1);
		}
		
		int idx = contentTypeString.indexOf(";");
		if (idx == -1) {
			parseType(contentTypeString);
		} else {
			parseType(contentTypeString.substring(0, idx));
			parameterString = contentTypeString.substring(idx + 1, contentTypeString.length());
		} 
	}
	
	
	/**
	 * Constructor that takes the primary type and the sub type
	 * 
	 * @param primaryType   the primary type
	 * @param subType       the sub type
	 */
	public ContentType(String primaryType, String subType) {
		this.primaryType = primaryType;
		this.subType = subType;
	}

	
	/**
	 * Constructor that takes the primary type, the sub type and parameter
	 * 
	 * @param primaryType    the primary type
	 * @param subType        the sub type
	 * @param parameterName  the parameter name
	 * @param parameterValue the parameter value
	 */
	public ContentType(String primaryType, String subType, String parameterName, String parameterValue) {
		this(primaryType, subType);
		setParameter(parameterName, parameterValue);
	}

	
	private void parseType(String type) {
		int idx = type.indexOf("/");
		if (idx == -1) {
			primaryType = type.trim();
		} else {
			primaryType = type.substring(0, idx).trim();
			subType = type.substring(idx + 1, type.length()).trim();
		}
	}
	

	
	/**
	 * Return the primary type
	 * 
	 * @return the primary type
	 */
	public String getPrimaryType() {
		return primaryType;
	}
	
	
	/**
	 * Set the primary type
	 * 
	 * @param primaryType the primary type
	 */
	public void setPrimaryType(String primaryType) {
		this.primaryType = primaryType;
		contentTypeString = null;
	}
	
	
	/**
	 * Return the subType 
	 * 
	 * @return the subType
	 */
	public String getSubType() {
		return subType;
	}

	
	/**
	 * Set the subType
	 * 
	 * @param subType  the subType
	 */
	public void setSubType(String subType) {
		this.subType = subType;
		contentTypeString = null;
	}

	
	/**
	 * Return the specified parameter value. Returns<code>null</code> if this parameter is absent 
	 * 
	 * @param name the parameter name
	 * @return parameter value
	 */
	public String getParameter(String name) {
		return getParameterMap().get(name);
	}	
	
	
	/**
	 * Set a specified parameter 
	 * 
	 * @param name       the parameter name
	 * @return parameter the parameter value
	 */
	public void setParameter(String name, String value) {
		getParameterMap().put(name, value);
		contentTypeString = null;
	}	
	

	private synchronized Map<String, String> getParameterMap() {
		if (parameters == null) {
			parameters = new HashMap<String, String>();
			if (parameterString != null) {
				String[] ps = parameterString.split(";");
				for (String p : ps) {
					String[] kvp = p.split("=");
					if (kvp.length == 2) {
						parameters.put(kvp[0], kvp[1]);
					} else {
						parameters.put(kvp[0], "");	
					}
				}
			}
		}
		
		return parameters;
	}
	
	
	/**
	 * returns true, if the primary and sub type is equals ignoring parameters    
	 * 
	 * @param other the Content type to compare
	 * @return true, if the primary and sub type is equals ignoring parameters
	 */
	public boolean equalsIgnoreParameters(Object other) {
	        
	    if (other instanceof ContentType) {
	        ContentType otherContentType = (ContentType) other;
	        return otherContentType.getPrimaryType().equals(this.getPrimaryType()) && 
	               otherContentType.getSubType().equals(this.getSubType());
	    }
	        
	    return false;
	}
	    

	
	@Override
	public boolean equals(Object other) {
		
		if (other instanceof ContentType) {
			ContentType otherContentType = (ContentType) other;
			return otherContentType.toString().equals(this.toString());
		}
		
		return false;
	}
	
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		if (contentTypeString == null) {
			contentTypeString = getPrimaryType() + "/" + getSubType();
			for (Entry<String, String> entry : getParameterMap().entrySet()) {
				contentTypeString = contentTypeString + ";" + entry.getKey() + "=" + entry.getValue() + " ";
			}
		}
		
		return contentTypeString;
	}
}
