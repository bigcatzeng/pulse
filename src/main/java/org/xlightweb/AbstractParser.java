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

import static org.xlightweb.HttpUtils.*;

import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;



/**
 * parser base class
 *  
 * @author grro@xlightweb.org
 */
abstract class AbstractParser {
    
	
	static void skipPositions(int length, ByteBuffer[] rawBuffer) {
        for (ByteBuffer buf : rawBuffer) {
            int remaining = buf.remaining();
            if (remaining < length) {
                buf.position(buf.limit());
                length -= remaining;
            } else {
                buf.position(buf.position() + length);
                break;
            }
        }
    }



	static String extractString(int posStart, int length, ByteBuffer buffer) {
        byte[] data = new byte[length];
        
        buffer.position(posStart);
        buffer.get(data);

        try {
            return new String(data, "ISO-8859-1");
        } catch (UnsupportedEncodingException usee) {
            throw new RuntimeException(usee);
        }
    }
    
    
    static int extractInt(int posStart, int length, ByteBuffer buffer) {
        String s = extractString(posStart, length, buffer);
        return Integer.parseInt(s);
    }


    

    static String extractStringWithoutTailingCR(int posStart, int length, ByteBuffer buffer) {
        buffer.position(posStart + length - 1);
        if (buffer.get() == CR) {
            length -= 1;
            if (length == -1) { // no value?
                return "";
            }
        }
        
        byte[] data = new byte[length];
        
        buffer.position(posStart);
        buffer.get(data);

        try {
            return new String(data, "ISO-8859-1");
        } catch (UnsupportedEncodingException usee) {
            throw new RuntimeException(usee);
        }
    }
    
 
    
    static String consumeStringWithoutTailingCR(int offset, int length, ByteBuffer buffer) {
    	int size = length;
    	
        buffer.position(offset + length - 1);
        if (buffer.get() == CR) {
        	size -= 1;
            if (size == -1) { // no value?
                return "";
            }
        }
        
        byte[] data = new byte[size];
        
        buffer.position(offset);
        buffer.get(data);
        
        buffer.position(offset + length);

        try {
            return new String(data, "ISO-8859-1");
        } catch (UnsupportedEncodingException usee) {
            throw new RuntimeException(usee);
        }
    }
    
    

    
    static void removeTailingCRLF(ByteBuffer buffer) {
    	int pos = buffer.position();
    	
    	if (buffer.get(buffer.limit() - 1) == LF) {
    		
    		if (buffer.get(buffer.limit() - 2) == CR) {
    			buffer.limit(buffer.limit() - 2);
    		} else {
        		buffer.limit(buffer.limit() - 1);
    		}
    	}
    	
    	buffer.position(pos);
    }
    

    static void parseHeaderLines(ByteBuffer buffer, IHeader header) throws BufferUnderflowException {
    	
        String name = null;
        String value = null;

        int posLF = buffer.position();

        outer : while (true) {
            
            // is first char not printable?
            byte firstCharOfLine = buffer.get(); 
            if (firstCharOfLine < 33) {
                if (firstCharOfLine == CR) {
                    byte secondCharOfLine = buffer.get();
                    if (secondCharOfLine == LF) {
                        if (name != null) {
                            header.addHeader(name, value);
                        }
                        
                        return;
                    }
                    
                } else if (firstCharOfLine == LF) {
                    if (name != null) {
                        header.addHeader(name, value);
                    }
                    
                    return;
                    
                    
                // Folding 
                } else if ((firstCharOfLine == SPACE) || (firstCharOfLine == HTAB)) {
                    
                    // read first non WSP char
                    int pos = 0;
                    while (true) {
                        byte c = buffer.get();
                        if ((c != SPACE) && (c != HTAB)) {
                            pos = buffer.position();
                            break;
                        }
                    }
                    
                    // looking for LF
                    while (true) {
                        if (buffer.get() == LF) {
                            posLF = buffer.position();
                            String v2 = extractStringWithoutTailingCR(pos - 1, posLF - pos, buffer);
                            if (value == null) {
                                value = v2;
                            } else {
                                value = value + " " + v2;
                            }
                            value = value.trim();
                            
                            buffer.position(posLF);
                            continue outer;
                        }
                    }
                }
                
            } else {
                if (name != null) {
                    header.addHeader(name, value);
                }
            }
            
            
            // looking for colon -> Content-Length:
            int posColon;
            while (true) {
                if (buffer.get() == COLON) {
                    posColon = buffer.position();
                    name = extractString(posLF, posColon - (posLF + 1), buffer);
                    buffer.position(posColon);
                    break;
                }
            }
            
            // looking for LF  -> Content-Length: 143
            while (true) {
                if (buffer.get() == LF) {
                    posLF = buffer.position();
                    value = extractStringWithoutTailingCR(posColon, posLF - (posColon + 1), buffer);
                    value = value.trim();
                    
                    buffer.position(posLF);
                    break;
                }
            }
        }
    }
}

 