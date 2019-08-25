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
 * Web stream
 * 
 * <br/><br/><b>This is an experimental implementation and subject to change</b> 
 * 
 * @author grro
 */
interface IReadWriteableWebStream<T extends IWebMessage> extends IReadableWebStream<T> {
    
    /**
     * write the message synchronously
     * @param msg the message to write
     * @return the message size
     * @throws IOException if a write error occurs
     */
    int writeMessage(T msg) throws IOException;    
    
    
    
    /**
     * write the message asynchronously 
     * @param msg              the message
     * @param completeHandler  the complete handler
     * @throws IOException if a write error occurs
     */
    void writeMessage(T msg, IWriteCompleteHandler completeHandler) throws IOException;
 
    
}
	