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
import java.net.SocketTimeoutException;


import org.xsocket.connection.IConnection;




/**
 * WebSocketConnection
 * 
 * <br/><br/><b>This is an experimental implementation of the HTML5 draft and subject to change</b>
 * 
 * @author grro@xlightweb.org
 */
public interface IWebSocketConnection extends IReadWriteableWebStream<WebSocketMessage>, IConnection {
    
    
    
    /**
     * write the message synchronously
     * @param msg the message to write
     * @return the message size
     * @throws IOException if a write error occurs
     */
    int writeMessage(WebSocketMessage msg) throws IOException;


    /**
     * write the text message synchronously
     * @param msg the message to write
     * @return the message size
     * @throws IOException if a write error occurs
     */
    int writeMessage(TextMessage msg) throws IOException;

    
    
    /**
     * write the message asynchronously 
     * @param msg              the message
     * @param completeHandler  the complete handler
     * @throws IOException if a write error occurs
     */
    void writeMessage(WebSocketMessage msg, IWriteCompleteHandler completeHandler) throws IOException;
 

    /**
     * write the text message asynchronously 
     * @param msg              the message
     * @param completeHandler  the complete handler
     * @throws IOException if a write error occurs
     */
    void writeMessage(TextMessage msg, IWriteCompleteHandler completeHandler) throws IOException;


    /**
     * returns the protocol
     * @return the protocol or <code>null</code> 
     */
    String getProtocol();
    
    
    /**
     * returns the web socket origin
     * @return the web socket origin
     */
    String getWebSocketOrigin();
    
    
    
    /**
     * returns the web socket location
     * @return the web socket location
     */
    String getWebSocketLocation();

    
    
    /**
     * returns the upgrade request header  
     * @return the upgrade request header
     */
    IHttpRequestHeader getUpgradeRequestHeader();
    
    
    /**
     * returns the upgrade response header  
     * @return the upgrade response header
     */
    IHttpResponseHeader getUpgradeResponseHeader();
    
    
    /**
     * returns the received message or <code>null</code> if no message is available. This method never blocks
     * 
     * @return the received message or <code>null</code> if no message is available
     * @throws SocketTimeoutException if a read timeout occurs
     * @throws IOException  if another exception occurs 
     */
    WebSocketMessage readMessage() throws IOException, SocketTimeoutException;
    
    
    /**
     * returns the received text message or <code>null</code> if no message is available. This method never blocks
     * 
     * @return the received message or <code>null</code> if no message is available
     * @throws SocketTimeoutException if a read timeout occurs
     * @throws IOException  if the received message is not a text message or another exception occurs 
     */
    TextMessage readTextMessage() throws IOException, SocketTimeoutException;
    
}
