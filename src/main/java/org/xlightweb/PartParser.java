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


import static org.xlightweb.HttpUtils.LF;







import static org.xlightweb.HttpUtils.CR;


import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.xsocket.Execution;




/**
 * part parser
 *  
 * @author grro@xlightweb.org
 */
@Execution(Execution.NONTHREADED)
final class PartParser extends AbstractParser implements IBodyDataHandler {
    

	private static final int READING_BEGIN_BOUNDARY = 0;
	private static final int READING_HEADER = 5;
	private static final int READING_CONTENT = 9;
	private static final int IS_COMPLETE = 15;
	private int state = READING_BEGIN_BOUNDARY;

	private final List<IPart> parts = new ArrayList<IPart>();
	
	private final NonBlockingBodyDataSource dataSource;
	private final byte[] endBoundary;
	private IPartHandler partHandler;
	private final AtomicBoolean isTotalComplete = new AtomicBoolean(false);

	private ByteBuffer rawData = null;
	private IPart part = null;

	
	public PartParser(IPartHandler partHandler, final NonBlockingBodyDataSource dataSource, String dashBoundaryString, ByteBuffer[] rawData) {
		this.endBoundary = (dashBoundaryString + "--").getBytes();
		this.dataSource = dataSource;
		this.rawData = HttpUtils.merge(rawData);
		this.partHandler = partHandler;
	}
	
	
	void setPartHandler(IPartHandler partHandler) throws IOException {
		synchronized (parts) {
			this.partHandler = partHandler;
			
			for (int i = 0; i < parts.size(); i++) {
				partHandler.onPart(dataSource);
			}
		}
	}
	
	IPart readPart() throws ClosedChannelException {
		synchronized (parts) {
			if (parts.isEmpty()) {
				if (isTotalComplete.get()) {
					throw new ClosedChannelException();
				} else {
					throw new BufferUnderflowException();
				}
			} else {
				return parts.remove(0);
			}
		}
	}
	
	
	int availableParts() {
		synchronized (parts) {
			return parts.size();	
		}
	}
	
	public boolean onData(NonBlockingBodyDataSource bodyDataSource) {
		try {
			int available = bodyDataSource.available();
			
			if (available > 0) {
				rawData = HttpUtils.merge(rawData, bodyDataSource.readByteBufferByLength(available));
			}
			
			while (true) {
                int previousSize = rawData.remaining();
                parse(rawData);
                
                int currentSize = rawData.remaining();
                if ((currentSize == 0) || (currentSize == previousSize)) {
                    break;
                }
            }
    			
			if (available == -1) {
			    if (isTotalComplete.get() == false) {
			        throw new IOException("incomplete multpart received");
			    }
            }

			
			return true;
			
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	
	
	private void parse(ByteBuffer rawData) throws IOException {
		switch (state) {
		
		case READING_BEGIN_BOUNDARY:
			boolean found = parseBoundary(rawData);
			if (found) {
				state = READING_HEADER;
			} 
			break;

		case READING_HEADER:
			Header header = parseHeader(rawData);
			if (header != null) {
				InMemoryBodyDataSource body = new InMemoryBodyDataSource(header);
				part = new Part(header, body);
				state = READING_CONTENT;
			}
			break;
			
		case READING_CONTENT: 
			int result = readContent(rawData);
			
			if (result > 0) {
				part.getNonBlockingBody().setComplete();

				synchronized (parts) {
					parts.add(part);

					if (result == 2) {
						state = IS_COMPLETE;
						isTotalComplete.set(true);
					} else {
						state = READING_HEADER;
					}

					if (partHandler != null) {
						partHandler.onPart(dataSource);
						
						if (isTotalComplete.get()) {
							partHandler.onPart(dataSource);
						}
 					}
				}
				part = null;
			}
			break;
			
			
		default:  // is complete
			// do nothing
			break;
	}

	}
	
	
	public boolean parseBoundary(ByteBuffer rawData) throws IOException {

        int savePosition = rawData.position();
        int idx = 0;
        boolean crFound = false;
        

        try {
            // looking for LF 
            while (true) {
            	byte b = rawData.get();

                if (b == LF) {
            		if (idx == (endBoundary.length - 2)) {
            			return true;

            		} else {
            			idx = 0;
            			crFound = false;
            		}
                    
                } else if (b == CR) {
                	if (crFound) {
                		idx = 0;
                		crFound = false;
                	} else {
                		crFound = true;
                	}
                	
                } else {
                	if (b == endBoundary[idx]) {
                		idx++;
                	} else {
                		crFound = false;
                		idx = 0;
                	}
                }
            }

        } catch (BufferUnderflowException bue) {
        	// do nothing
        }
        
        // not found restore buffer
    	rawData.position(savePosition);
        return false;
	}
	


	public Header parseHeader(ByteBuffer rawData) throws IOException {
		

        int savePosition = rawData.position();
        Header header = new Header();

        try {
        	 // parse header lines
            parseHeaderLines(rawData, header);
            return header;
            
        } catch (BufferUnderflowException bue) {
        	// no enough data -> restore buffer
        	rawData.position(savePosition);
        	return null;
        }
	}
	
	
	
	public int readContent(ByteBuffer rawData) throws IOException {
		
        int savePosition = rawData.position();
        int idx = 0;
        int startPos = 0;
        boolean crFound = false;
                
        try {
            // looking for LF 
            while (true) {
            	byte b = rawData.get();

                if (b == LF) {
                	boolean isDashBoundary  = (idx == (endBoundary.length - 2));
                	boolean isEndBoundary  = (idx == (endBoundary.length));
                	
            		if (isEndBoundary || isDashBoundary) {
            			int pos = rawData.position();
            			consumeStringWithoutTailingCR(savePosition, rawData.position() - savePosition, rawData);
            			
            			ByteBuffer buf = rawData.duplicate();
            			buf.position(savePosition);
            			buf.limit(startPos);
            			
            			removeTailingCRLF(buf);
            			
            			((InMemoryBodyDataSource ) part.getNonBlockingBody()).append(buf);
            			rawData.position(pos);
            			
            			if (isEndBoundary) {
            				return 2;
            			} else {
            				return 1;
            			}

            		} else {	
            			idx = 0;
            			crFound = false;
            			startPos = rawData.position();
            		}
                    
                } else if (b == CR) {
                	if (crFound) {
                		idx = 0;
                		startPos = rawData.position();
                		crFound = false;
                	} else {
                		crFound = true;
                	}
                	
                } else {
                	if (b == endBoundary[idx]) {
                		idx++;
                	} else {
                		idx = 0;
                		crFound = false;
                		startPos = rawData.position();
                	}
                }
            }
            
        } catch (BufferUnderflowException bue) {
        	// do nothing
        }
      
        
        // reading boundary? 
        if (idx != 0) {
        	rawData.position(savePosition);
        	

        // .. no
        } else {
        	rawData.position(savePosition);
        	
        	((InMemoryBodyDataSource ) part.getNonBlockingBody()).append(rawData.duplicate());
        	rawData.position(rawData.limit());
        }
        
        return 0;
	}
}
