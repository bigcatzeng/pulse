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





/**
 * @deprecated replaced by {@link MultivalueMap} 
 * 
 * @author grro@xlightweb.org
 */
public final class FormURLEncodedBody extends MultivalueMap {


    
    /**
     * constructor
     * 
     * @param decodedFormParameter   the decoded form parameters 
     */
    public FormURLEncodedBody(NameValuePair decodedFormParameter) {
    	super(decodedFormParameter);
    }
    
    
    /**
     * constructor
     * 
     * @param encoding               the encoding to use
     * @param decodedFormParameter   the decoded form parameters 
     */
    public FormURLEncodedBody(String encoding) {
    	super(encoding);
    }


    /**
     * constructor
     * 
     * @param decodedFormParameters   the decoded form parameters 
     */
    public FormURLEncodedBody(NameValuePair... decodedFormParameters) {
    	super(decodedFormParameters);
    }

    
    
    /**
     * constructor
     * 
     * @param encoding the encoding to use
     * @param decodedFormParameters   the decoded form parameters 
     */
    public FormURLEncodedBody(String encoding, NameValuePair... decodedFormParameters) {
        super(encoding, decodedFormParameters);
    }
    
    
    /**
     * constructor
     * 
     * @param encoding the encoding to use
     * @param decodedFormParameters   the decoded form parameters 
     */
    public FormURLEncodedBody(String encoding, String... decodedFormParameters) {
        super(encoding, decodedFormParameters);
    }
    
    

    
    /**
     * construcotr
     * 
     * @param bodyDataSource    the entity
     * 
     * @throws IOException if the body parsing fails
     */
    public FormURLEncodedBody(BodyDataSource bodyDataSource) throws IOException {
        super(bodyDataSource); 
    }
    

    
    FormURLEncodedBody(String encoding, String entity) throws IOException {
        super(encoding, entity);
    }
}

 