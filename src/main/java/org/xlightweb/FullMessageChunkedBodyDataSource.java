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


import static org.xlightweb.HttpUtils.CR;
import static org.xlightweb.HttpUtils.HTAB;
import static org.xlightweb.HttpUtils.LF;
import static org.xlightweb.HttpUtils.SPACE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;



/**
 * a full body data source
 *  
 * @author grro@xlightweb.org
 */
final class FullMessageChunkedBodyDataSource extends AbstractNetworkBodyDataSource {

private static final char[] ISO_8859_1_ARRAY = HttpUtils.getISO_8859_1_Array();

    
    private static final int STATE_READ_LENGTH_FIELD = 0;
    private static final int STATE_READ_CONTENT = 1;
    private static final int STATE_READ_CONTENT_CRLF = 2;
    private static final int STATE_READ_TRAILER = 3;
    private static final int STATE_COMPLETE = 999;
    
    private int state = STATE_READ_LENGTH_FIELD;

    
    private static final int HL_READING_FIRST_CHAR_OF_LINE = 100;
    private static final int HL_READING_REMAINING_CHARS_OF_NAME = 110;
    private static final int HL_WAITING_UNTIL_VALUE_STARTS = 120;
    private static final int HL_READING_THE_VALUE = 130;

    private int trailerState = HL_READING_FIRST_CHAR_OF_LINE;
    
    
    private final StringBuilder stringBuilder = new StringBuilder(8); 

    private StringBuilder hlNameBuilder = new StringBuilder(26);
    private StringBuilder hlValueBuilder = new StringBuilder();
    private int hlPosLastCharNotSpaceOrHtab; 
    private boolean hlIsPreviousCharLF = true;

    private int sizeCurrentChunk;
    private int remainingCurrentChunk;
    private ArrayList<Integer> receivedChunks = new ArrayList<Integer>();
    
    
	

	public FullMessageChunkedBodyDataSource(AbstractHttpConnection httpConnection, HttpMessageHeader header) throws IOException {
		super(header, httpConnection);
		
		postCreate();
	}

	
	
	@Override
	void doParse(ByteBuffer[] rawData) throws IOException {
        
	    while (!HttpUtils.isEmpty(rawData)) {
            switch (state) {
                
                case STATE_READ_LENGTH_FIELD:
                    readLengthField(rawData);
                    break;
                        
                case STATE_READ_CONTENT:
                    readContent(rawData);
                    break;
                            
                case STATE_READ_CONTENT_CRLF:
                    readContentCRLF(rawData);
                    break;
                        
                case STATE_READ_TRAILER:
                    boolean isComplete = readTrailer(rawData);
                    if (isComplete) {
                        return;
                    }
                    break;
                        
                default:
                    // do nothing
            }
	    }
    }
    
    
    
    private void readLengthField(ByteBuffer[] rawData) throws IOException {
        for (ByteBuffer buffer : rawData) {
            if (buffer == null) {
                continue;
            }
            
            int remaining = buffer.remaining();
        
            for (int i = 0; i < remaining; i++) {
                
                byte b = buffer.get();
                switch (b) {
                    case CR:
                        break;
                        
                    case SPACE:
                        break;
                        
                    case HTAB:
                        break;
                        
                    case LF:
                        sizeCurrentChunk = parseLengtField(stringBuilder.toString());
                        remainingCurrentChunk = sizeCurrentChunk;
                        stringBuilder.setLength(0);
                        if (remainingCurrentChunk > 0) {
                            state = STATE_READ_CONTENT;
                            return;
                            
                        } else {
                            state = STATE_READ_TRAILER;
                            return;
                        }
                        
                    default:
                        stringBuilder.append((char) b);
                        break;
                }
            }
        }
    }
    

    
    private void readContent(ByteBuffer[] rawData) throws IOException {
        remainingCurrentChunk = readByteBufferByLength(rawData, remainingCurrentChunk);

        if (remainingCurrentChunk == 0) {
            receivedChunks.add(sizeCurrentChunk);
            sizeCurrentChunk = 0;
            state = STATE_READ_CONTENT_CRLF;
        } 
    }
    
    
    
    private void readContentCRLF(ByteBuffer[] rawData) throws IOException {
        for (ByteBuffer buffer : rawData) {
            if (buffer == null) {
                continue;
            }
            
            int remaining = buffer.remaining();
            
            for (int i = 0; i < remaining; i++) {
                
                byte b = buffer.get();
                switch (b) {
                    case CR:
                        break;
                    
                    case LF:
                        state = STATE_READ_LENGTH_FIELD;
                        return;
                        
                    default:
                        break;
                }
            }
        }
    }
    
    
    
    private boolean readTrailer(ByteBuffer[] rawData) throws IOException {
        for (ByteBuffer buffer : rawData) {
            if (buffer == null) {
                continue;
            }
            
            int remaining = buffer.remaining();
            
            for (int i = 0; i < remaining; i++) {
                
                byte b = buffer.get();      
                
                switch (trailerState) {
                    
                    case HL_READING_FIRST_CHAR_OF_LINE:
                        switch (b) {
                            case CR:
                                break;
                            
                            case LF:
                                // header end? 
                                if (hlIsPreviousCharLF) {
                                    addLine();
                                    state = STATE_COMPLETE;
                                    setComplete();
                                    return true;
                                } 
                                hlIsPreviousCharLF = true;
                                break;
                                
                        
                            case SPACE:
                                hlValueBuilder.append(" ");
                                state = HL_WAITING_UNTIL_VALUE_STARTS; 
                                break;
                                
                            case HTAB:
                                hlValueBuilder.append(" ");
                                state = HL_WAITING_UNTIL_VALUE_STARTS;
                                break;
                                
                                
                            default:
                                hlIsPreviousCharLF = false;
                                addLine();  // add previous read line (if exists) 
                            
                                hlNameBuilder.append((char) b);
                                state = HL_READING_REMAINING_CHARS_OF_NAME;
                                break;
                        }
                        break;
                        
                        
                    case HL_READING_REMAINING_CHARS_OF_NAME:
                        switch (b) {
                            case CR:
                                break;
                            
                            case LF:
                                state = HL_READING_FIRST_CHAR_OF_LINE;
                                hlIsPreviousCharLF = true;
                                break;
                                
                            case SPACE:
                                state = HL_WAITING_UNTIL_VALUE_STARTS; 
                                break;
                                
                            case HTAB:
                                state = HL_WAITING_UNTIL_VALUE_STARTS;
                                break;
                    
                                
                            case ':':
                                state = HL_WAITING_UNTIL_VALUE_STARTS;
                                break;
                                
                            default:
                                hlNameBuilder.append((char) b);
                                break;
                        }
                        break;
                    
                        
                    case HL_WAITING_UNTIL_VALUE_STARTS:
                        if ((b != SPACE) && (b != HTAB)) {
                            hlValueBuilder.append(toISO_8859_1_Char(b));
                            hlPosLastCharNotSpaceOrHtab = hlValueBuilder.length();
                            state = HL_READING_THE_VALUE;
                        }
                        break;
    
                        
                    case HL_READING_THE_VALUE:
                        switch (b) {
                            case CR:
                                break;
                            
                            case LF:
                                hlIsPreviousCharLF = true;
                                state = HL_READING_FIRST_CHAR_OF_LINE;
                                break;
                                
                            case SPACE:
                                hlValueBuilder.append(toISO_8859_1_Char(b)); 
                                break;
                                
                            case HTAB:
                                hlValueBuilder.append(toISO_8859_1_Char(b));
                                break;
                                
                            default:
                                hlValueBuilder.append(toISO_8859_1_Char(b));
                                hlPosLastCharNotSpaceOrHtab = hlValueBuilder.length();
                                break;
                        }
                        break;                  
                }
            }
        }
        
        return false;
    }
    
    

    private char toISO_8859_1_Char(byte b) {
        int i = b;
        i &= 0x000000FF;
        
        return ISO_8859_1_ARRAY[i]; 
    }
    
    
    
    
    private void addLine() {
        if ((hlNameBuilder.length() > 0) && (hlValueBuilder.length() > 0)) {
            hlValueBuilder.setLength(hlPosLastCharNotSpaceOrHtab);  // truncate trailing spaces
            getHeader().addHeader(hlNameBuilder.toString(), hlValueBuilder.toString());
            hlNameBuilder.setLength(0);
            hlValueBuilder.setLength(0);
        }
    }
    

    
    private int parseLengtField(String lengthField) throws ProtocolException {
        try {
            return Integer.parseInt(lengthField, 16);
            
        } catch (NumberFormatException nfe) {

            // chunk extension?
            if (lengthField.indexOf(";") != -1) {
                String length   = lengthField.substring(0, lengthField.indexOf(";"));
                return Integer.parseInt(length, 16);
                
            } else {
                throw new ProtocolException("[" + getId() + "] http protocol error. length field expected. " + chunkInfo() + " Got '" + lengthField + "'", getHeader());
            }
        }
    }   
    
    
    @Override
    void onClose() throws IOException {
	    if (state != STATE_COMPLETE) {
	        throw new ProtocolException("connection has been closed (by user?) while receiving body data. " + chunkInfo() + " (FullChunkedMessage)", getHeader());
	    }
    }


    @Override
    void performOnDisconnect() throws ProtocolException {
        if (state != STATE_COMPLETE) {
            throw new ProtocolException("connection has been closed (by peer?) while receiving body data. " + chunkInfo() +" (FullChunkedMessage)", getHeader()); 
        }
    }
    
    private String chunkInfo() {
        try {
            StringBuilder sb = new StringBuilder("reveiced complete chunksize: [");
            for (Integer size : receivedChunks) {
                sb.append(size + ", ");
            }
            sb.append("] ");
            
            if (state == STATE_READ_CONTENT) {
                sb.append(" state=reading chunk: current chunksize=" + sizeCurrentChunk + " already recevied=" + (sizeCurrentChunk - remainingCurrentChunk));
                
            } else if (state == STATE_READ_LENGTH_FIELD) {
                sb.append(" state=readLengthField");
                
            } else if (state == STATE_READ_TRAILER) {
                sb.append(" state=readingTrailer");
            
            } else if(state == STATE_READ_CONTENT_CRLF) {
                sb.append(" state=readingContentCRLF");
                
            } else {
                sb.append(" state=complete (?)");
            }
                      
            return sb.toString();
        } catch (Exception e) {
            return "no info available";
        }
    }	
}

  