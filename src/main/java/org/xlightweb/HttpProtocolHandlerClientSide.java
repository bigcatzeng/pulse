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
import static org.xlightweb.HttpUtils.MAX_HEADER_SIZE;
import static org.xlightweb.HttpUtils.SLASH;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xlightweb.AbstractHttpConnection.IMessageHandler;
import org.xlightweb.AbstractHttpConnection.IMessageHeaderHandler;
import org.xsocket.DataConverter;
import org.xsocket.connection.INonBlockingConnection;



/**
 * client side http protocol handler 
 * 
 * @author grro@xlightweb.org
 */
final class HttpProtocolHandlerClientSide extends AbstractHttpProtocolHandler {
    
    private static final Logger LOG = Logger.getLogger(HttpProtocolHandlerClientSide.class.getName());
    
	
	ByteBuffer[] parseHeader(AbstractHttpConnection httpConnection, ByteBuffer[] rawData) throws BadMessageException, IOException {
		HttpResponseHeader responseHeader = null;
		
		responseHeader = parse(httpConnection.getUnderlyingTcpConnection(), rawData);

		if (responseHeader != null) {
		    
		    if (LOG.isLoggable(Level.FINE)) {
		        LOG.fine("[" + httpConnection.getId() + "] request header parsed (rawData: " + HttpUtils.computeRemaining(rawData) + ")");
		    }

		    responseHeader.setBodyDefaultEncoding(httpConnection.getBodyDefaultEncoding());
		    
            setState(RECEIVING_BODY);

			httpConnection.setLastTimeHeaderReceivedMillis(System.currentTimeMillis());
			httpConnection.incCountMessageReceived();
				
			IMessageHeaderHandler messageHeaderHandler = httpConnection.getMessageHeaderHandler();
			if (messageHeaderHandler == null) {
				throw new IOException("no message handler set");
			}
			
			try {			
                int bodyType = getBodyType(httpConnection, responseHeader, (IHttpRequestHeader) messageHeaderHandler.getAssociatedHeader());
				switch (bodyType) {
					
					case BODY_TYPE_EMTPY:
						HttpResponse response = new HttpResponse(responseHeader);
						
						IMessageHandler messageHandler = messageHeaderHandler.onMessageHeaderReceived(response);
						messageHandler.onHeaderProcessed();
						
						httpConnection.onMessageCompleteReceived(responseHeader);
						
						setMessageHandler(messageHandler);
						reset();
						
						
						// next response? (-> pipelining)
						if (rawData != null) {
							return onData(httpConnection, rawData);
						}		
						break;

						
					case BODY_TYPE_FULL_MESSAGE:
					    AbstractNetworkBodyDataSource dataSource = new FullMessageBodyDataSource(responseHeader, responseHeader.getContentLength(), httpConnection);
					    setBodyDataSource(dataSource);
						
						response = new HttpResponse(responseHeader, dataSource);

	                    messageHandler = messageHeaderHandler.onMessageHeaderReceived(response);
	                    try {
		                    setMessageHandler(messageHandler);
							
							response.getNonBlockingBody().setBodyDataReceiveTimeoutMillis(httpConnection.getBodyDataReceiveTimeoutMillis());
							rawData = parserBody(httpConnection, rawData);
	                    } finally {
							messageHandler.onHeaderProcessed();
						}							
						
						break;

						
                    case BODY_TYPE_WEBSOCKET:
                        dataSource = new FullMessageBodyDataSource(responseHeader, 16, httpConnection);
                        setBodyDataSource(dataSource);
                        
                        response = new HttpResponse(responseHeader, dataSource);
                        response.removeHeader("Content-Length");

                        messageHandler = messageHeaderHandler.onMessageHeaderReceived(response);
                        try {
                            setMessageHandler(messageHandler);
                            
                            response.getNonBlockingBody().setBodyDataReceiveTimeoutMillis(httpConnection.getBodyDataReceiveTimeoutMillis());
                            rawData = parserBody(httpConnection, rawData);
                        } finally {
                            messageHandler.onHeaderProcessed();
                        }                           
                        
                        break;
						
						
					case BODY_TYPE_SIMPLE:
					    dataSource = new SimpleMessageBodyDataSource(responseHeader, httpConnection);
                        setBodyDataSource(dataSource);
						
						response = new HttpResponse(responseHeader, dataSource);
						
						messageHandler = messageHeaderHandler.onMessageHeaderReceived(response);
						try {
				            setMessageHandler(messageHandler);
							
							response.getNonBlockingBody().setBodyDataReceiveTimeoutMillis(httpConnection.getBodyDataReceiveTimeoutMillis());
							rawData = parserBody(httpConnection, rawData);
						} finally {
							messageHandler.onHeaderProcessed();
						}
						break;
						
					case BODY_TYPE_MULTIPART_BYTERANGE:					    
					    dataSource = new MultipartByteRangeMessageBodyDataSource(httpConnection, responseHeader);
					    setBodyDataSource(dataSource);
						
						response = new HttpResponse(responseHeader, dataSource);

						messageHandler = messageHeaderHandler.onMessageHeaderReceived(response);
						try {
							setMessageHandler(messageHandler);
							
							response.getNonBlockingBody().setBodyDataReceiveTimeoutMillis(httpConnection.getBodyDataReceiveTimeoutMillis());
							rawData = parserBody(httpConnection, rawData);
						} finally {
							messageHandler.onHeaderProcessed();
						}
						break;
						
						
					default:  // BODY_TYPE_CHUNKED
						dataSource = new FullMessageChunkedBodyDataSource(httpConnection, responseHeader);
					    setBodyDataSource(dataSource);

						response = new HttpResponse(responseHeader, dataSource);
						
						messageHandler = messageHeaderHandler.onMessageHeaderReceived(response);
						try {
							setMessageHandler(messageHandler);
							
							response.getNonBlockingBody().setBodyDataReceiveTimeoutMillis(httpConnection.getBodyDataReceiveTimeoutMillis());
							rawData = parserBody(httpConnection, rawData);
						} finally {
							messageHandler.onHeaderProcessed();
						}
						
						break;
				}				

			
			} catch (BadMessageException bme) {
				throw bme;				
			} 
		}
		
		return rawData;
	}
	
	



	/**
	 * {@inheritDoc}
     */
	@Override
	void onDisconnectInHeader(AbstractHttpConnection httpConnection, ByteBuffer[] rawData) throws IOException {
        ByteBuffer buffer = HttpUtils.duplicateAndMerge(rawData); 

        int maxReadSize = 8;
        if (buffer.remaining() < maxReadSize) {
            maxReadSize = buffer.remaining();
        }
        
        // looking for slash of protocol -> HTTP/
        for (int i = 0; i < maxReadSize; i++) {
            if (buffer.get() == SLASH) {
                int posSlash = buffer.position();
                    
                String protocolScheme = extractString(posSlash - 5, 4, buffer);
                if (protocolScheme.equalsIgnoreCase("HTTP")) {
                    throw new ProtocolException("connection " + httpConnection.getId() + " has been disconnected while receiving header. Already received: " + DataConverter.toString(HttpUtils.copy(rawData), IHttpMessageHeader.DEFAULT_ENCODING), null);
                }
            }
        }
        
        
        // HTTP 0.9 response
        httpConnection.setLastTimeHeaderReceivedMillis(System.currentTimeMillis());
        httpConnection.incCountMessageReceived();
        
        HttpResponseHeader responseHeader = new HttpResponseHeader(200);
        responseHeader.setProtocolVersionSilence("0.9");
        responseHeader.setHeader("Connection", "close");
        
        AbstractNetworkBodyDataSource dataSource = new SimpleMessageBodyDataSource(responseHeader, httpConnection);
        setBodyDataSource(dataSource);

        setState(RECEIVING_BODY);
        
        HttpResponse response = new HttpResponse(responseHeader, dataSource, true);

        
        IMessageHeaderHandler messageHeaderHandler = httpConnection.getMessageHeaderHandler(); // get it before disconnecting dataSource! 
        if (messageHeaderHandler == null) {
            throw new IOException("no message handler set");
        }
        
        dataSource.parse(rawData);
        dataSource.onDisconnect();   
        
        IMessageHandler messageHandler = messageHeaderHandler.onMessageHeaderReceived(response);
        setMessageHandler(messageHandler);
        messageHandler.onHeaderProcessed();
	}

	
    
    @Override
    void onDisconnectInHeaderNothingReceived(AbstractHttpConnection httpConnection, ByteBuffer[] rawData) {
        if (httpConnection.getNumOpenTransactions() == 0) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("no open transactions running. ignore disconnect");
            }
            // do nothing
        } else {
            super.onDisconnectInHeaderNothingReceived(httpConnection, rawData);
        }
    }
	
	
	private static int getBodyType(AbstractHttpConnection httpConnection, HttpResponseHeader responseHeader, IHttpRequestHeader requestHeader) {

	    int status = responseHeader.getStatus();

	    if ((status == 101) && (responseHeader.getHeader("Sec-WebSocket-Origin") != null)) {
	        return BODY_TYPE_WEBSOCKET;
	    }
	    
		if (HttpUtils.isBodylessStatus(status) || (requestHeader.getMethod().equals(IHttpMessage.HEAD_METHOD))) {
			return BODY_TYPE_EMTPY;
		}

		if (((status / 100) == 2) && requestHeader.getMethod().equals(IHttpMessage.CONNECT_METHOD)) {
		    return BODY_TYPE_EMTPY;
	    }
            
		
		
		// contains a non-zero Content-Length header  -> bound body 
		if ((responseHeader.getContentLength() != -1)) {
			if (responseHeader.getContentLength() > 0) {
				return BODY_TYPE_FULL_MESSAGE;
			}
			return BODY_TYPE_EMTPY;
		}
	
		
		//  transfer encoding header is set with chunked -> chunked body
		String transferEncoding = responseHeader.getTransferEncoding();
		if ((transferEncoding != null) && (transferEncoding.equalsIgnoreCase("chunked"))) {
			return BODY_TYPE_CHUNKED;
		}
		
	
		  
        // is connection header set with close?
        if ((responseHeader.getHeader("Connection") != null) && (responseHeader.getHeader("Connection").equalsIgnoreCase("close"))) {
            httpConnection.setPersistent(false);
            return BODY_TYPE_SIMPLE;
        }
        
        
		// multipart/byteranges response?
		if ((responseHeader.getStatus() == 206) && (responseHeader.getContentType() != null) && (responseHeader.getContentType().toLowerCase().startsWith("multipart/byteranges"))) {
			return BODY_TYPE_MULTIPART_BYTERANGE;
		}
		
		
		  
        if (!responseHeader.getProtocolVersion().equalsIgnoreCase("0.9") && (status >= 300)) {
            httpConnection.setPersistent(false);
            return BODY_TYPE_EMTPY;
        }
            
	
		// assume 0.9 response
		httpConnection.setPersistent(false);
		return BODY_TYPE_SIMPLE;
	}
	
	
	private static HttpResponseHeader parse(INonBlockingConnection connection, ByteBuffer[] rawData) throws IOException {

        // filter CR:
        // RFC 2616 (19.3 Tolerant Applications)
        // ... The line terminator for message-header fields is the sequence CRLF.
        // However, we recommend that applications, when parsing such headers,
        // recognize a single LF as a line terminator and ignore the leading CR...

        HttpResponseHeader responseHeader = new HttpResponseHeader();

        
        if (rawData == null) {
            return null;
        }

        ByteBuffer workBuffer = HttpUtils.duplicateAndMerge(rawData);
        int savePosition = workBuffer.position();
        
        try {
    
            // looking for slash of protocol -> HTTP/
            while (true) {
                if (workBuffer.get() == SLASH) {
                    int posSlash = workBuffer.position();
                    
                    if (LOG.isLoggable(Level.FINE)) {
                        if (posSlash != 5) {
                            LOG.fine("leading data " + DataConverter.toString(HttpUtils.copy(workBuffer)));
                        }
                    }
                    
                    String protocolScheme = extractString(posSlash - 5, 4, workBuffer);
                    responseHeader.setProtocolSchemeSilence(protocolScheme);
                    
                    String protocolVersion = extractString(posSlash, 3, workBuffer);
                    responseHeader.setProtocolVersionSilence(protocolVersion);
                    
                    workBuffer.position(posSlash + 4);
                    break;
                }
            }
    
            
            // looking for first char of status  -> HTTP/1.1 2
            int posStatusCode;
            while (true) {
                if (workBuffer.get() > 47) {
                    posStatusCode = workBuffer.position();
                    
                    int statusCode = extractInt(posStatusCode - 1, 3, workBuffer); 
                    responseHeader.setStatus(statusCode);
                    
                    workBuffer.position(posStatusCode + 3);
    
                    break;
                }
            }

            
            // looking for LF -> HTTP/1.1 200 OK
            int posLF;
            while (true) {
                if (workBuffer.get() == LF) {
                    posLF = workBuffer.position();
                    
                    String reason = extractStringWithoutTailingCR(posStatusCode + 3, posLF - (posStatusCode + 4), workBuffer);
                    responseHeader.setReason(reason);
    
                    workBuffer.position(posLF);
                    break;
                }
            }

            parseHeaderLines(workBuffer, responseHeader);

            skipPositions(workBuffer.position() - savePosition, rawData);

            return responseHeader;

        } catch (Exception e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("header parsing exception: " + e.toString());
            }
            
            if (workBuffer.position(savePosition).remaining() > MAX_HEADER_SIZE) {
                throw new BadMessageException("max header size reached");
            } else {
                return null;
            }
        }
	}
}

 