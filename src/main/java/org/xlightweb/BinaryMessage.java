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
 * WebSocket Binary Message 
 * 
 * <br/><br/><b>This is an experimental implementation of the HTML5 draft and subject to change</b> 
 * 
 * @author grro
 */
final class BinaryMessage extends WebSocketMessage {

    private static final byte START_BYTE_BINARYFRAME = (byte) 0x80;

    
    public BinaryMessage(byte[] bytes) {
        this(DataConverter.toByteBuffer(bytes));
    }
    
    BinaryMessage(ByteBuffer msg) {
        super(msg, null);
    }
    
    @Override
    public boolean isBinaryMessage() {
        return true;
    }
    
    
    @Override
    int writeTo(WebSocketConnection connection, IWriteCompleteHandler completeHandler) throws IOException {
        
        if (completeHandler == null) {
            connection.getUnderlyingTcpConnection().setFlushmode(FlushMode.SYNC);
        } else {
            connection.getUnderlyingTcpConnection().setFlushmode(FlushMode.ASYNC);
        }
        
        ByteBuffer msgData = getData().getByteBuffer();
        int length = msgData.remaining();
        
        byte[] header;
        if (length <=128) {
            header = new byte[1 + 1];
            header[1] = (byte) (0x7F & length);
            
        } else if (length <= 16384) {
            header = new byte[1 + 2];
            header[1] = (byte) (0x80 | (0x7f & (length>>7)));
            header[2] = (byte) (0x7F & length);
            
        } else {
            header = new byte[1 + 3];
            header[1] = (byte) (0x80 | (length>>14));
            header[2] = (byte) (0x80 | (0x7F & (length>>7)));
            header[3] = (byte) (0x7F & length);
        }
        header[0] = (byte) 0x80;
        
        int written = connection.getUnderlyingTcpConnection().write(header);
        if (completeHandler == null) {
            written += connection.getUnderlyingTcpConnection().write(msgData);
        } else {
            written += msgData.remaining(); 
            connection.getUnderlyingTcpConnection().write(msgData, new WebSocketMessageCompleteHandlerAdapter(connection, completeHandler));
        }
        connection.getUnderlyingTcpConnection().flush();

        return written;
    }    

    
    static BinaryMessage parse(ByteBuffer buffer) throws IOException {
        
        int savePos =  buffer.position();
        int saveLimit =  buffer.limit();

        // read length
        int length = 0;
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            
            length = length << 7;
            length = length | (0x7F & b);
            if (b >= 0) {
                break;
            }
        }
 
        if (buffer.remaining() >= length) {
            ByteBuffer msgBuffer = buffer.slice();
            buffer.position(buffer.position() + length);

            return new BinaryMessage(msgBuffer);
        }
        
        buffer.position(savePos);        
        buffer.limit(saveLimit);
        
        return null;
    }
    
    static boolean isBinaryMessage(byte startByte) {
        return (startByte == START_BYTE_BINARYFRAME);
    }    
}
	