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


import static org.xlightweb.HttpUtils.AND;
import static org.xlightweb.HttpUtils.CR;
import static org.xlightweb.HttpUtils.EQUALS;
import static org.xlightweb.HttpUtils.HTAB;
import static org.xlightweb.HttpUtils.LF;
import static org.xlightweb.HttpUtils.MAX_HEADER_SIZE;
import static org.xlightweb.HttpUtils.QUESTION_MARK;
import static org.xlightweb.HttpUtils.SLASH;
import static org.xlightweb.HttpUtils.SPACE;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.xlightweb.AbstractHttpConnection.IMessageHandler;
import org.xlightweb.AbstractHttpConnection.IMessageHeaderHandler;
import org.xsocket.DataConverter;
import org.xsocket.connection.INonBlockingConnection;



/**
 * server side http protocol handler
 *  
 * @author grro@xlightweb.org
 */
final class HttpProtocolHandlerServerSide extends AbstractHttpProtocolHandler {
    
 
	
	ByteBuffer[] parseHeader(AbstractHttpConnection httpConnection, ByteBuffer[] rawData) throws BadMessageException, IOException {
		HttpRequestHeader requestHeader = parse(httpConnection.getUnderlyingTcpConnection(), rawData);
		
		if (requestHeader != null) {
		    requestHeader.setBodyDefaultEncoding(httpConnection.getBodyDefaultEncoding());
		    
			httpConnection.setLastTimeHeaderReceivedMillis(System.currentTimeMillis());
			httpConnection.incCountMessageReceived();

			IMessageHeaderHandler messageHeaderHandler = httpConnection.getMessageHeaderHandler();
			if (messageHeaderHandler == null) {
				throw new IOException("no message handler set");
			}
			
			
			HttpRequest request = null;
			
			switch (getBodyType(requestHeader)) {
				
				case BODY_TYPE_EMTPY:
					request = new HttpRequest(requestHeader);
					IMessageHandler messageHandler = messageHeaderHandler.onMessageHeaderReceived(request);
					messageHandler.onHeaderProcessed();
					
					messageHandler.onMessageReceived();
					httpConnection.onMessageCompleteReceived(requestHeader);
					
					rawData = HttpUtils.compact(rawData);
					reset();
					
					// next request? (-> pipelining)
					if (rawData != null) {
						return onData(httpConnection, rawData);
					}								
					
					break;
					
				case BODY_TYPE_FULL_MESSAGE:
					AbstractNetworkBodyDataSource dataSource = new FullMessageBodyDataSource(requestHeader, requestHeader.getContentLength(), httpConnection);
					setBodyDataSource(dataSource);
					
					request = new HttpRequest(requestHeader, dataSource);
					messageHandler = messageHeaderHandler.onMessageHeaderReceived(request);
					try {
						setMessageHandler(messageHandler);
						
						setState(RECEIVING_BODY);
						
						request.getNonBlockingBody().setBodyDataReceiveTimeoutMillisSilence(httpConnection.getBodyDataReceiveTimeoutMillis());
						rawData = parserBody(httpConnection, rawData);
					} finally {
						messageHandler.onHeaderProcessed();
					}
					
					return rawData;
					
					
                case BODY_TYPE_WEBSOCKET:
                    dataSource = new FullMessageBodyDataSource(requestHeader, 8, httpConnection);
                    setBodyDataSource(dataSource);
                    
                    request = new HttpRequest(requestHeader, dataSource);
                    request.removeHeader("Content-Length");
                    messageHandler = messageHeaderHandler.onMessageHeaderReceived(request);
                    try {
                        setMessageHandler(messageHandler);
                        
                        setState(RECEIVING_BODY);
                        
                        request.getNonBlockingBody().setBodyDataReceiveTimeoutMillisSilence(httpConnection.getBodyDataReceiveTimeoutMillis());
                        rawData = parserBody(httpConnection, rawData);
                    } finally {
                        messageHandler.onHeaderProcessed();
                    }
                    
                    return rawData;
					
					
				default:  // BODY_TYPE_CHUNKED
					dataSource = new FullMessageChunkedBodyDataSource(httpConnection, requestHeader);
				    setBodyDataSource(dataSource);
                
				    request = new HttpRequest(requestHeader, dataSource);

					messageHandler = messageHeaderHandler.onMessageHeaderReceived(request);
					try {
						setMessageHandler(messageHandler);
	
						setState(RECEIVING_BODY);
	
						request.getNonBlockingBody().setBodyDataReceiveTimeoutMillis(httpConnection.getBodyDataReceiveTimeoutMillis());
						rawData = parserBody(httpConnection, rawData);
					} finally {
						messageHandler.onHeaderProcessed();
					}
					
					return rawData;
			}						
			
		}
		return rawData;		
	}
	
	
	
	private static int getBodyType(HttpRequestHeader requestHeader) throws BadMessageException {
		
		String method = requestHeader.getMethod();

	
		// GET request?
		if (method.equals(IHttpMessage.GET_METHOD)) {
		    if (requestHeader.getHeader("Sec-WebSocket-Key1") != null) {
		        return BODY_TYPE_WEBSOCKET;
		    } else {
		        return BODY_TYPE_EMTPY;
		    }
		}

		
		// POST request?
		if (method.equals(IHttpMessage.POST_METHOD)) {
			return getBodyTypeByHeader(requestHeader);
		}
		
		
		// other bodyless request?
		if (method.equals(IHttpMessage.CONNECT_METHOD) || 
			method.equals(IHttpMessage.HEAD_METHOD) || 
		    method.equals(IHttpMessage.TRACE_METHOD)|| 
		    method.equals(IHttpMessage.DELETE_METHOD)|| 
		    method.equals(IHttpMessage.OPTIONS_METHOD)) {
			
			return BODY_TYPE_EMTPY;
		}
				
		// ... no, determine if body by header
		return getBodyTypeByHeader(requestHeader);
	}
	
	
	private static int getBodyTypeByHeader(HttpRequestHeader requestHeader) throws BadMessageException {
		
		// contains a non-zero Content-Length header  -> bound body 
		if ((requestHeader.getContentLength() != -1)) {
			if (requestHeader.getContentLength() > 0) {
				return BODY_TYPE_FULL_MESSAGE;
			}
			return BODY_TYPE_EMTPY;
		}
		
		//  transfer encoding header is set with chunked -> chunked body
		String transferEncoding = requestHeader.getTransferEncoding();
		if ((transferEncoding != null) && (transferEncoding.equalsIgnoreCase("chunked"))) {
			return BODY_TYPE_CHUNKED;
		}
		
		throw new BadMessageException(requestHeader.toString());
	}
	
	

    private static HttpRequestHeader parse(INonBlockingConnection connection, ByteBuffer[] rawData) throws BadMessageException, IOException {

        // filter CR:
        // RFC 2616 (19.3 Tolerant Applications)
        // ... The line terminator for message-header fields is the sequence CRLF.
        // However, we recommend that applications, when parsing such headers,
        // recognize a single LF as a line terminator and ignore the leading CR...

        HttpRequestHeader requestHeader = new HttpRequestHeader(connection);

        
        if (rawData == null) {
            return null;
        }

        ByteBuffer workBuffer = HttpUtils.duplicateAndMerge(rawData);
        int savePosition = workBuffer.position();
        int saveLimit = workBuffer.limit();
        
        
        try {
            
            // looking for first char of method -> P
            int posStart;
            while (true) {
                byte b = workBuffer.get();
                if (b > SPACE) {
                    posStart = workBuffer.position();
                    break;
                    
                } else if ((b != CR) && (b != LF)) {
                    if (HttpUtils.isShowDetailedError()) {
                        workBuffer.position(savePosition);
                        workBuffer.limit(saveLimit);
                        throw new BadMessageException("bad request (by parsing method name): " + DataConverter.toString(HttpUtils.copy(workBuffer)));
                    } else {
                        throw new BadMessageException("bad request");
                    }
                }
            }
    

            // looking for last char of method -> POST
            while (true) {
                if (workBuffer.get() == SPACE) {
                    int posEnd = workBuffer.position();
                    
                    String method = extractString(posStart - 1, posEnd - posStart, workBuffer);
                    requestHeader.setMethodSilence(method);
                    workBuffer.position(posEnd);
                    break;
                }
            }

            
            // looking for first char of path -> POST /
            while (true) {
                if (workBuffer.get() > SPACE) {
                    posStart = workBuffer.position();
                    break;
                }
            }

            

            // looking for last char of path or question mark -> POST /test/resource
            while (true) {
                byte b = workBuffer.get();
                
                if (b < 64) {  // performance optimization: check only char equals or lower than '?'                
                    int posEnd = workBuffer.position();

                    if (b == SPACE) {
                        String path = extractString(posStart - 1, posEnd - posStart, workBuffer);
                        requestHeader.setPathSilence(path);
                        workBuffer.position(posEnd);
                        break;


                    } else if (b == QUESTION_MARK) {
                        String path = extractString(posStart - 1, posEnd - posStart, workBuffer);
                        requestHeader.setPathSilence(path);
                        workBuffer.position(posEnd);

                        parserQueryParams(workBuffer, posEnd, requestHeader);
                        break;
                        
                        
                    // printable char
                    } else if (b > SPACE) {
                        continue;  // optimization: printable char: continue 
                        
                        
                    // HTTP 0.9 request handling
                    } else if (b == LF) {
                        throw new BadMessageException("simple request messages are not supported");
                        
                    } else if (b == CR) {
                        b = workBuffer.get();
                        if (b == LF) {
                            throw new BadMessageException("simple request messages are not supported");
                        } else {
                            workBuffer.position(workBuffer.position() - 1);
                        }
                    }
                } 
            }


            // looking for slash  -> POST /test/resource HTTP/1.1
            while (true) {
                byte b = workBuffer.get();
                if (b < 48) {  // performance optimization: check only char equals or lower than '/'
                    if (b == SLASH) {
                        int posSlash = workBuffer.position();
                        
                        String protocolScheme = extractString(posSlash - 5, 4, workBuffer);
                        requestHeader.setProtocolSchemeSilence(protocolScheme);
                        
                        String protocolVersion = extractString(posSlash, 3, workBuffer);
                        requestHeader.setProtocolVersionSilence(protocolVersion);
                        
                        workBuffer.position(posSlash + 3);
                        break;
                        
                    } else if (b >= SPACE) {
                        continue;
                        
                    } else if (b != HTAB) {
                        if (HttpUtils.isShowDetailedError()) {
                            workBuffer.position(savePosition);
                            workBuffer.limit(saveLimit);
                            throw new BadMessageException("bad request (by parsing protocol): " + DataConverter.toString(HttpUtils.copy(workBuffer)));
                        } else {
                            throw new BadMessageException("bad message");
                        }
                            
                    }
                }
            }
            
            
            // looking for LF  -> POST /test/resource HTTP/1.1
            while (true) {
                if (workBuffer.get() == LF) {
                    break;
                }
            }

            // parse header lines
            parseHeaderLines(workBuffer, requestHeader);

            skipPositions(workBuffer.position() - savePosition, rawData);

            return requestHeader;


        } catch (BadMessageException bme) {
            throw bme;
            
        } catch (Exception e) {
            workBuffer.position(savePosition);
            workBuffer.limit(saveLimit);
            
            if (workBuffer.remaining() > MAX_HEADER_SIZE) {
                throw new BadMessageException("max header size reached");
            } else {
                return null;
            }
        }
    }
    
    
    private static void parserQueryParams(ByteBuffer buffer, int pos, HttpRequestHeader requestHeader) throws BadMessageException, IOException {
        
        String paramName = null;
        String paramValue = "";
        
        boolean isQueryParameterRead = false;
        
        while (true) {
            byte b = buffer.get();
            
            if (b < 62) { // performance optimization: check only char equals or lower than '='                 

                // looking for equals -> POST /test/resource?param1=
                if (b == EQUALS) {
                    isQueryParameterRead = true;
                    paramName = extractString(pos, (buffer.position() - 1)  - pos, buffer);
                    pos = buffer.position() + 1;
                    
                    // parse value
                    while (true) {
                        b = buffer.get();

                        if (b < 39) { // performance optimization: check only char equals or lower than '&'
                            
                            // looking for & -> POST /test/resource?param1=value1&
                            if (b == AND) {
                                paramValue = extractString(pos, (buffer.position() - 1)  - pos, buffer);
                                pos = buffer.position() + 1;
                                buffer.position(pos);

                                requestHeader.addRawQueryParameterSilence(paramName, paramValue);
                                paramName = null;
                                paramValue = "";
                                break;
                                
                            // or for space -> POST /test/resource?param1=value1                                
                            } else if (b == SPACE) {
                                paramValue = extractString(pos, (buffer.position() - 1)  - pos, buffer);
                                buffer.position(buffer.position() + 1);
                                
                                requestHeader.addRawQueryParameterSilence(paramName, paramValue);
                                return;
                                
                            // printable char
                            } else if (b > SPACE) {
                                continue;  // optimization: printable char: continue
                                
                                
                            // HTTP 0.9 request handling
                            } else if (b == LF) {
                                throw new BadMessageException("simple request messages are not supported");
                                
                            } else if (b == CR) {
                                b = buffer.get();
                                if (b == LF) {
                                    throw new BadMessageException("simple request messages are not supported");
                                } else {
                                    buffer.position(buffer.position() - 1);
                                }
                            }
                        }
                    }
                    
                } else if (b == SPACE) {
                    if (!isQueryParameterRead) {
                        String queryString = extractString(pos, (buffer.position() - 1)  - pos, buffer);
                        requestHeader.setQueryString(queryString);
                    }
                    buffer.position(buffer.position() + 1);
                    
                    return;
                    
                // printable char
                } else if (b > SPACE) {
                    continue;
                    
                    
                // HTTP 0.9 request handling
                } else if (b == LF) {
                    throw new BadMessageException("simple request messages are not supported");
                    
                } else if (b == CR) {
                    b = buffer.get();
                    if (b == LF) {
                        throw new BadMessageException("simple request messages are not supported");
                    } else {
                        buffer.position(buffer.position() - 1);
                    }
                }
            }
        }
    }
    
    @Override
    void onDisconnectInHeaderNothingReceived(AbstractHttpConnection httpConnection, ByteBuffer[] rawData) {
        // do nothing 
    }
}

 