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

import org.xsocket.DataConverter;
import org.xsocket.connection.IConnection.FlushMode;



/**
 * WebSocket Text Message 
 *
 * <br/><br/><b>This is an experimental implementation of the HTML5 draft and subject to change</b> 
 * 
 * @author grro
 */
public final class TextMessage extends WebSocketMessage {

    private static final byte START_BYTE_TEXTFRAME = (byte) 0x00;
    private static final byte END_BYTE = (byte) 0xFF;
 

    
    public TextMessage(String msg) {
        this(DataConverter.toByteBuffer(msg, "utf-8"));
    }
    
    TextMessage(ByteBuffer msg) {
        super(msg, "utf-8");
    }
    
    @Override
    public boolean isTextMessage() {
        return true;
    }
    
    @Override
    int writeTo(WebSocketConnection connection, IWriteCompleteHandler completeHandler) throws IOException {
        
        if (completeHandler == null) {
            connection.getUnderlyingTcpConnection().setFlushmode(FlushMode.SYNC);
        } else {
            connection.getUnderlyingTcpConnection().setFlushmode(FlushMode.ASYNC);
        }
        
        byte[] msgData = getData().toBytes();
        
        byte[] data = new byte[msgData.length + 2];
        data[0] = START_BYTE_TEXTFRAME;
        data[data.length - 1] = END_BYTE;
        System.arraycopy(msgData, 0, data, 1, msgData.length);
        
        int written;
        if (completeHandler == null) {
            written = connection.getUnderlyingTcpConnection().write(data);
        } else {
            written = data.length;
            connection.getUnderlyingTcpConnection().write(data, new WebSocketMessageCompleteHandlerAdapter(connection, completeHandler));
        }
        connection.getUnderlyingTcpConnection().flush();
        
        return written;
    }
    
    static TextMessage parse(ByteBuffer buffer) throws IOException {
        
        int savePos =  buffer.position();
        int saveLimit =  buffer.limit();

        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            if ((b & END_BYTE) == END_BYTE) {
                int pos = buffer.position();
                
                buffer.limit(buffer.position() - 1);
                buffer.position(savePos);
                
                ByteBuffer msg = buffer.slice();
                
                buffer.limit(saveLimit);
                buffer.position(pos);
                
                return new TextMessage(msg);
            }
         }
        
        buffer.position(savePos);        
        buffer.limit(saveLimit);
        
        return null;
    }
    
    static boolean isTextMessage(byte startByte) {
        return (startByte == START_BYTE_TEXTFRAME);
    }    
}
	