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
import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xlightweb.AbstractHttpConnection.IMessageHandler;
import org.xlightweb.AbstractHttpConnection.IMessageHeaderHandler;
import org.xsocket.DataConverter;



/**
 * protocol handler definition
 *  
 * @author grro@xlightweb.org
 */
abstract class AbstractHttpProtocolHandler extends AbstractParser {
    
    private static final Logger LOG = Logger.getLogger(AbstractHttpProtocolHandler.class.getName());
    
    
	static final int BODY_TYPE_EMTPY = 0;
	static final int BODY_TYPE_FULL_MESSAGE = 1;
	static final int BODY_TYPE_CHUNKED = 2;
	static final int BODY_TYPE_SIMPLE = 3;
	static final int BODY_TYPE_MULTIPART_BYTERANGE = 4;
	static final int BODY_TYPE_WEBSOCKET = 5;
	
	
	static final int RECEIVING_HEADER = 5;
	static final int RECEIVING_BODY = 10;

	private int state = RECEIVING_HEADER;

	private AbstractNetworkBodyDataSource dataSource = null;
    private IMessageHandler messageHandler = null;
    
    
    // statisitics
    int received = 0;
    
    
    final void incReveived(int addSize) {
        received += addSize;
    }
    
    final int getReceived() {
        return received;
    }
    
    
	final void setState(int state) {
		this.state = state;
	}
	
	final int getState() {
		return state;
	}
    
    final void setBodyDataSource(AbstractNetworkBodyDataSource dataSource) {
        this.dataSource = dataSource;
    }
	
    final AbstractNetworkBodyDataSource getBodyDataSource() {
        return dataSource;
    }
    
	final void setMessageHandler(IMessageHandler messageHandler) {
	    this.messageHandler = messageHandler;
	}
	
	final IMessageHandler getMessageHandler() {
        return messageHandler;
    }
	

	
	
	
	public final ByteBuffer[] onData(AbstractHttpConnection httpConnection, ByteBuffer[] rawData) throws BadMessageException, IOException {
		
		try {
			switch (state) {
			
				case RECEIVING_HEADER:
				    if (LOG.isLoggable(Level.FINE)) {
				        LOG.fine("parsing header");
				    }
					return parseHeader(httpConnection, rawData);
										
				default:  // RECEIVING_BODY
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("parsing body (" + this.getClass().getName() + "#" + this.hashCode() + ")");
                    }
					rawData = parserBody(httpConnection, rawData);
					if (rawData != null) {
						return onData(httpConnection, rawData);
					}
			}
			
		} catch (BufferUnderflowException ignore) { 
		    
		} catch (Exception e) {
		    IOException ioe = null;
		    if (e instanceof IOException) {
		        ioe = (IOException) e;
		    } else {
		        ioe = HttpUtils.toIOException(e);
		    }
		    if (messageHandler != null) {
		        messageHandler.onBodyException(ioe, rawData);
		    }
            throw ioe;
		}
		
		return HttpUtils.compact(rawData);
	}
	
	
	
    final ByteBuffer[] parserBody(AbstractHttpConnection httpConnection, ByteBuffer[] rawData) throws IOException {

        dataSource.parse(rawData);
        if (dataSource.isComplete()) {
            IMessageHandler mh = messageHandler;
            reset();
            
            if (mh != null) {
                mh.onMessageReceived();
            } 
        }
        
        rawData = HttpUtils.compact(rawData);
        
        // next response? (-> pipelining) 
        if (rawData != null) {
            return this.onData(httpConnection, rawData);
            
        } else {
            return rawData;
        } 
    }	   

    void reset() {
        setState(RECEIVING_HEADER);
        messageHandler = null;
        dataSource = null;
    }
    

    abstract ByteBuffer[] parseHeader(AbstractHttpConnection httpConnection, ByteBuffer[] rawData) throws BadMessageException, IOException; 
    
 	
	
	
	
	final void onDisconnect(AbstractHttpConnection httpConnection, ByteBuffer[] rawData) {

	    // state receiving header
        if (getState() == RECEIVING_HEADER) {
            if (!httpConnection.isClosing()) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + httpConnection.getId() + "] connection disconnected while receving header");
                }
            }

            // already data received?
            if (!HttpUtils.isEmpty(rawData)) {
                try {
                    onDisconnectInHeader(httpConnection, rawData);
                } catch (IOException ioe) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + httpConnection.getId() + "] Error occured by calling on disconnect while receiving header " + ioe.toString());
                    }
                    IMessageHeaderHandler messageHeaderHandler = httpConnection.getMessageHeaderHandler();
                    if (messageHeaderHandler != null) {
                        messageHeaderHandler.onHeaderException(ioe, rawData);
                    }
                }

            // ... no
            } else {
                if (!httpConnection.isClosing()) {
                    onDisconnectInHeaderNothingReceived(httpConnection, rawData);
                }
            }

            
        // state receiving body   
        } else {
            AbstractNetworkBodyDataSource ds = getBodyDataSource();
            if (ds != null) {                
                // force calling data handler if data is available 
                try {
                    ds.append((ByteBuffer) null);
                } catch (IOException ioe) {
                    ds.setException(ioe);
                }
                
                try {
                    ds.onDisconnect();
                } catch (IOException ioe) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + httpConnection.getId() + "] Error occured by calling on disconnect while receiving header  " + ioe.toString());
                    }
                    if (messageHandler != null) {
                        messageHandler.onBodyException(ioe, rawData);
                    }
                }
            }
        }
	}
	
	
    void onDisconnectInHeader(AbstractHttpConnection httpConnection, ByteBuffer[] rawData) throws IOException {
        ByteBuffer[] received = HttpUtils.copy(rawData);
        throw new ProtocolException("connection " + httpConnection.getId() + " has been disconnected while receiving header. Already received: " + DataConverter.toString(received, IHttpMessageHeader.DEFAULT_ENCODING), null);
    }

    
    void onDisconnectInHeaderNothingReceived(AbstractHttpConnection httpConnection, ByteBuffer[] rawData) {

    	String detailedReason;
    	if (httpConnection.isOpen()) {
    		if (httpConnection.getCountMessagesSent() > 0) {
    			detailedReason = httpConnection.getCountMessagesSent() + " messages sent, " + httpConnection.getCountMessagesReceived() + " messages recieved (closed by peer?)";
    		} else {
    			detailedReason = "no messages sent (closed by peer?)";
    		}
    		
    	} else if (httpConnection.isClosing()) {
    		detailedReason = "connection is closing " + httpConnection.getCountMessagesSent() + " messages sent, " + httpConnection.getCountMessagesReceived() + " messages recieve";
    	} else {
    		detailedReason = "closeReason=" + httpConnection.getCloseReason() + ")";
    	}
        
    	
    	ProtocolException pe = new ProtocolException("connection " + httpConnection.getId() + " has been disconnected while receiving header. no data received. " + detailedReason, null);
    	
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("[" + httpConnection.getId() + "] error occured " + pe.toString());
        }        
        
        IMessageHeaderHandler messageHeaderHandler = httpConnection.getMessageHeaderHandler();
        if (messageHeaderHandler != null) {
            messageHeaderHandler.onHeaderException(pe, rawData);
        }
    }
 
    
    
    
    /**
     * call back if a exception is occured
     * 
     * @param  httpConnection   the underlying http conection
     * @param  ioe              the exception
     * @param  rawData          the unready networkdata 
     */
    public final void onException(AbstractHttpConnection httpConnection, IOException ioe, ByteBuffer[] rawData) {
        
        switch (getState()) {
        
            case RECEIVING_HEADER:
                if (LOG.isLoggable(Level.FINE)) {
                    try {
                        LOG.info("error occured by receiving header. Reason: " + ioe.toString() + " received" + DataConverter.toString(HttpUtils.copy(rawData)));
                    } catch (UnsupportedEncodingException use) {
                        LOG.info("error occured by receiving header. Reason: " + ioe.toString());
                    }
                }
                break;
    
                
            default:
                if (dataSource != null) {
                    dataSource.onException(ioe, rawData);
                }
        }
    } 
}

 