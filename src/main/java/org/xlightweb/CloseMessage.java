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


import java.nio.ByteBuffer;

import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IConnection.FlushMode;



/**
 * WebSocket Close message
 * 
 * <br/><br/><b>This is an experimental implementation of the HTML5 draft and subject to change</b> 
 * 
 * @author grro
 */
final class CloseMessage extends WebSocketMessage {

    private static final byte START_BYTE_CLOSEFRAME = (byte) 0xFF;
    private static final byte END_BYTE = (byte) 0x00;
    private static final byte[] CLOSE_FRAME = new byte[] { START_BYTE_CLOSEFRAME, END_BYTE };
 
    
    public CloseMessage() {
        super(null, null);
    }

    @Override
    boolean isCloseMessage() {
        return true;
    }
    
    
    @Override
    int writeTo(WebSocketConnection connection, IWriteCompleteHandler completeHandler) throws IOException {
        
        if ((completeHandler == null) && !ConnectionUtils.isDispatcherThread()) {
            connection.getUnderlyingTcpConnection().setFlushmode(FlushMode.SYNC);
        } else {
            connection.getUnderlyingTcpConnection().setFlushmode(FlushMode.ASYNC);
        }
        
        if (completeHandler == null) {
            connection.getUnderlyingTcpConnection().write(CLOSE_FRAME);
        } else {
            connection.getUnderlyingTcpConnection().write(CLOSE_FRAME, new WebSocketMessageCompleteHandlerAdapter(connection, completeHandler));
        }
        connection.getUnderlyingTcpConnection().flush();
        
        return 2;
    }
    
    
    static CloseMessage parse(ByteBuffer buffer) throws IOException {
        
        int savePos =  buffer.position();
        int saveLimit =  buffer.limit();

        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            if ((b & END_BYTE) == END_BYTE) {
                int pos = buffer.position();
                
                buffer.limit(buffer.position() - 1);
                buffer.position(savePos);
                
                buffer.slice();
                
                buffer.limit(saveLimit);
                buffer.position(pos);
                
                return new CloseMessage();
            }
         }
        
        buffer.position(savePos);        
        buffer.limit(saveLimit);
        
        return null;
    }
    
    static boolean isCloseMessage(byte startByte) {
        return (startByte == START_BYTE_CLOSEFRAME);
    }            
}
	