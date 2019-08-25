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



/**
 * WebSocket Message 
 * 
 * <br/><br/><b>This is an experimental implementation of the HTML5 draft and subject to change</b> 
 * 
 * @author grro
 */
abstract class WebSocketMessage implements IWebMessage {

    private final Data data;
    
    WebSocketMessage(ByteBuffer byteBufferData, String encoding) {
        this.data = new Data(byteBufferData, encoding);
    }

    final Data getData() {
        return data;
    }

    boolean isBinaryMessage() {
        return false;
    }

    
    boolean isTextMessage() {
        return false;
    }

    boolean isCloseMessage() {
        return false;
    }

    
    abstract int writeTo(WebSocketConnection connection, IWriteCompleteHandler completeHandler) throws IOException;
    
    
    
    static WebSocketMessage parse(ByteBuffer buffer) throws IOException {

        int savePos =  buffer.position();
        int saveLimit =  buffer.limit();
        

        WebSocketMessage msg = null;
        byte b = buffer.get();

        // text message
        if (TextMessage.isTextMessage(b)) {
            msg = TextMessage.parse(buffer);
        
        // binary message 
        } else if (BinaryMessage.isBinaryMessage(b)) {
            msg = BinaryMessage.parse(buffer);

        // close message 
        } else if (CloseMessage.isCloseMessage(b)) {
            msg = CloseMessage.parse(buffer);

        } else {
            buffer.position(savePos);        
            buffer.limit(saveLimit);
            throw new IOException("got invalid frame " + DataConverter.toTextAndHexString(new ByteBuffer[] { buffer }, "UTF-8", 4000));
        }
        
        if (msg == null) {
            buffer.position(savePos);        
            buffer.limit(saveLimit);
            return null;
            
        } else {
            return msg;
        }
    }
    
    
  
    public byte[] toBytes() {
        return data.toBytes();
    }

    @Override
    public String toString() {
        return data.toString();
    }
    
    public String toString(String charset) {
        return getData().toString(charset);
    }
    
    
    static final class Data {

        private final String encoding;
        
        private ByteBuffer byteBufferData;
        private byte[] byteData;
        
        
        public Data(ByteBuffer byteBufferData, String encoding) {
            this.byteBufferData = byteBufferData;
            this.encoding = encoding;
        }
        
        public Data(byte[] byteData, String encoding) {
            this.byteData = byteData;
            this.encoding = encoding;
        }
        

        public byte[] toBytes() {
            if (byteData == null) {
                return DataConverter.toBytes(byteBufferData);
            }
            
            return byteData;
        }
        
        ByteBuffer getByteBuffer() {
            if (byteBufferData == null) {
                byteBufferData = DataConverter.toByteBuffer(byteData);
            }
            
            return byteBufferData;
        }
        
   
        @Override
        public String toString() {
            if (encoding == null) {
                return DataConverter.toHexString(new ByteBuffer[] { getByteBuffer().duplicate() }, Integer.MAX_VALUE); 
            } else {
                return toString(encoding);
            }
        }
        
        public String toString(String charset) {
            try {
                return DataConverter.toString(getByteBuffer().duplicate(), charset);
            } catch (Exception e) {
                return "error occured by creating string representation: " + e.toString();
            }
        }
    }
}
	