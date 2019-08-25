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




import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


import org.xlightweb.AbstractHttpConnection.IMultimodeExecutor;
import org.xsocket.DataConverter;
import org.xsocket.Execution;
import org.xsocket.ILifeCycle;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.Resource;
import org.xsocket.SerializedTaskQueue;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.IDisconnectHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.IWriteCompletionHandler;
import org.xsocket.connection.NonBlockingConnection;
import org.xsocket.connection.NonBlockingConnectionPool;
import org.xsocket.connection.IConnection.FlushMode;


 

/** 
 * A HTTP utility class
 *
 * @author grro@xlightweb.org
 */
public final class HttpUtils {
    
    static final int MAX_HEADER_SIZE = 8192;
    
    static final byte CR = 13;
    static final byte LF = 10;
    static final byte SPACE = 32;
    static final byte HTAB = 9;
    static final byte AND = 38;
    static final byte SLASH = 47;
    static final byte COLON = 58;
    static final byte EQUALS = 61;
    static final byte QUESTION_MARK = 63;
    
    private static final Logger LOG = Logger.getLogger(HttpUtils.class.getName());
    
    
    private static final Bom BOM_UTF_8 = new Bom("UTF-8", new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
    private static final Bom BOM_UTF_32BE = new Bom("UTF-32BE", new byte[] {(byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0xFF});
    private static final Bom BOM_UTF_32LE = new Bom("UTF-32LE", new byte[] {(byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x00});
    private static final Bom BOM_UTF_16BE = new Bom("UTF-16BE", new byte[] {(byte) 0xFE, (byte) 0xFF}); 
    private static final Bom BOM_UTF_16LE = new Bom("UTF-16LE", new byte[] {(byte) 0xFF, (byte) 0xFE});
    private static final int BOM_MAX_LENGTH = 4;    
    private static final Bom[] BOMS = new Bom[] { BOM_UTF_8, BOM_UTF_32BE, BOM_UTF_32LE, BOM_UTF_16LE, BOM_UTF_16BE };
    

    private static final Timer TIMER = new Timer("xHttpTimer", true);

    
    private static final Executor DEFAULT_WORKERPOOL = Executors.newCachedThreadPool(new XLightwebThreadFactory()); 
    
    private static final String DATE_TIME_PATTERN_1 = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String DATE_TIME_PATTERN_2 = "yyyyMMdd'T'HHmmssz";
    private static final String DATE_TIME_PATTERN_3 = "yyyy-MM-dd'T'HH:mm:ss.S";

    private static final String RFC1123_TIME_PATTERN = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final String RFC1036_TIME_PATTERN = "EEEE, dd-MMM-yy HH:mm:ss zzz";
    
    
    @SuppressWarnings("rawtypes")
    private static final Map<Class, Boolean> bodyDataExecutionModeCache = ConnectionUtils.newMapCache(25);

    @SuppressWarnings("rawtypes")
    private static final Map<Class, Boolean> bodyCompleteListenerExecutionModeCache = ConnectionUtils.newMapCache(25);
    
    @SuppressWarnings("rawtypes")
    private static final Map<Class, Boolean> requestTimeoutHandlerExecutionModeCache = ConnectionUtils.newMapCache(25);

    @SuppressWarnings("rawtypes")
    private static final Map<Class, CompletionHandlerInfo> completionHandlerInfoCache = ConnectionUtils.newMapCache(25);
        
    @SuppressWarnings("rawtypes")
    private static final Map<Class, Boolean> bodyCloseListenerExecutionModeCache = ConnectionUtils.newMapCache(25);

    @SuppressWarnings("rawtypes")
    private static final Map<Class, String[]> mappingCache = ConnectionUtils.newMapCache(25);
    
    @SuppressWarnings("rawtypes")
    private static final Map<Class, Integer> bodyDataHandlerExecutionModeCache = ConnectionUtils.newMapCache(25);

    @SuppressWarnings("rawtypes")
    private static final Map<Class, Integer> listenerModeCache = ConnectionUtils.newMapCache(50);

    @SuppressWarnings("rawtypes")
    private static final Map<Class, RequestHandlerInfo> httpRequestHandlerInfoCache = ConnectionUtils.newMapCache(25);

    private static RequestHandlerInfo emptyHttpRequestHandlerInfo;

    @SuppressWarnings("unchecked")
    private static final Map<Class, PartHandlerInfo> partHandlerInfoCache = ConnectionUtils.newMapCache(25);
    private static PartHandlerInfo emptyPartHandlerInfo;
    
    @SuppressWarnings("unchecked")
    private static final Map<Class, ResponseHandlerInfo> httpResponseHandlerInfoCache = ConnectionUtils.newMapCache(25);
    private static ResponseHandlerInfo emptyResponseHandlerInfo;

    
    @SuppressWarnings("unchecked")
    private static final Map<Class, HttpConnectionHandlerInfo> httpConnectionHandlerInfoCache = ConnectionUtils.newMapCache(25);
    private static final HttpConnectionHandlerInfo EMPTY_HTTP_CONNECTION_HANDLER_INFO = new HttpConnectionHandlerInfo(null);
    
    
    @SuppressWarnings("unchecked")
    private static final Map<Class, WebSocketHandlerInfo> webSocketHandlerInfoCache = ConnectionUtils.newMapCache(25);
    private static final WebSocketHandlerInfo EMPTY_WEB_SOCKET_HANDLER_INFO = new WebSocketHandlerInfo(null);

    @SuppressWarnings("unchecked")
    private static final Map<Class, IEventHandlerInfo> webEventHandlerInfoCache = ConnectionUtils.newMapCache(25);
    private static final IEventHandlerInfo EMPTY_WEB_EVENT_HANDLER_INFO = new IEventHandlerInfo(null);


    
    private static String implementationVersion;
    private static String implementationDate;
    private static String xSocketImplementationVersion;
    
    
    private static Map<String, String> mimeTypeMap;

    private static boolean showDetailedError;

    private static Boolean isConnectHandlerWarningIsSuppressed = null;
    
    
    static {
        showDetailedError = Boolean.parseBoolean(System.getProperty(IHttpExchange.SHOW_DETAILED_ERROR_KEY, IHttpExchange.SHOW_DETAILED_ERROR_DEFAULT));
    }


    private static final char[] ISO_8859_1_Array = new char[256];

    static {
        for (int i = 0; i < ISO_8859_1_Array.length; i++) {
            try {
                ISO_8859_1_Array[i] = new String(new byte[] { (byte) i }, "ISO-8859-1").charAt(0);
            } catch (UnsupportedEncodingException use) {
                throw new RuntimeException(use);
            }
        }
    }
    

    private static final byte base64[] = {
                    (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D',
                    (byte) 'E', (byte) 'F', (byte) 'G', (byte) 'H',  
                    (byte) 'I', (byte) 'J', (byte) 'K', (byte) 'L',
                    (byte) 'M', (byte) 'N', (byte) 'O', (byte) 'P',  
                    (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T',
                    (byte) 'U', (byte) 'V', (byte) 'W', (byte) 'X',  
                    (byte) 'Y', (byte) 'Z', (byte) 'a', (byte) 'b',
                    (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f',       
                    (byte) 'g', (byte) 'h', (byte) 'i', (byte) 'j',
                    (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n',       
                    (byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r',
                    (byte) 's', (byte) 't', (byte) 'u', (byte) 'v',       
                    (byte) 'w', (byte) 'x', (byte) 'y', (byte) 'z',
                    (byte) '0', (byte) '1', (byte) '2', (byte) '3',       
                    (byte) '4', (byte) '5', (byte) '6', (byte) '7',
                    (byte) '8', (byte) '9', (byte) '+', (byte) '/',       
                    (byte) '='                          
    };

    
    static final Integer EXECUTIONMODE_UNSYNCHRONIZED = -1;
    
        
    private HttpUtils() { }
        
    
    
    /**
     * returns a ISO_8859_1 byte array
     * 
     * @return a ISO_8859_1 byte array
     */
    static char[] getISO_8859_1_Array() {
        return ISO_8859_1_Array; 
    }
    
    
    /**
     * returns true, if detailed error messages should been shown. See {@link IHttpExchange#SHOW_DETAILED_ERROR_KEY} 
     * 
     * @return true, if detailed error messages should been shown
     */
    public static boolean isShowDetailedError() {
        return showDetailedError;
    }
 

    /**
     * clears the internal caches
     * 
     * <br/><br/><b>This is a xSocket internal method and subject to change</b> 
     */
    public static void clearCaches() {
        bodyDataExecutionModeCache.clear();
        mappingCache.clear();
        bodyCompleteListenerExecutionModeCache.clear();;
        requestTimeoutHandlerExecutionModeCache.clear();
        completionHandlerInfoCache.clear();
        bodyCloseListenerExecutionModeCache.clear();
        httpRequestHandlerInfoCache.clear();
        partHandlerInfoCache.clear();
        httpResponseHandlerInfoCache.clear();
        httpConnectionHandlerInfoCache.clear();
    }

    
    
    /**
     * returns the reason text
     * 
     * @param statusCode the status code 
     * @return the reason text
     */
    public static String getReason(int statusCode) {
        switch (statusCode) {
        
        case 100:
            return "Continue";

        case 101:
            return "Switching Protocols";
            
        case 200:
            return "OK";
            
        case 201:
            return "Created";

        case 202:
            return "Accepted";
            
        case 203:
            return "Non-Authoritative Information";
            
        case 204:
            return "No Content";
            
        case 205:
            return "Reset Content";
            
        case 206:
            return "Partial Content";            
            
        case 300:
            return "Multiple Choices";
            
        case 301:
            return "Moved Permanently";
            
        case 302:
            return "Not Found";
            
        case 303:
            return "See other";
            
        case 304:
            return "Not Modifed";

        case 305:
            return "Use Proxy";
            
        case 307:
            return "Temporary Redirect";            

        case 400:
            return "Bad Request";
            
        case 401:
            return "Unauthorized";
            
        case 402:
            return "Payment Required";
            
        case 403:
            return "Forbidden";         

        case 404:
            return "Not found";
            
        case 405:
            return "Method Not Allowed";

        case 406:
            return "Not Acceptable";
            
        case 407:
            return "Proxy Authentication Required";
            
        case 408:
            return "Request Timeout";
            
        case 409:
            return "Conflict";
            
        case 410:
            return "Gone";
            
        case 411:
            return "Length Required";
            
        case 412:
            return "Precondition Failed";
            
        case 413:
            return "Request Entity Too Large";
            
        case 414:
            return "Request-URI Too Long";
            
        case 415:
            return "Unsupported Media Type";
            
        case 416:
            return "Requested Range Not Satisfiable";           
            
        case 417:
            return "Expectation Failed";
            
        case 500:
            return "Internal Server Error";
            
        case 501:
            return "Not Implemented";

        case 502:
            return "Bad Gateway";

        case 503:
            return "Service Unavailable";
            
        case 504:
            return "Gateway Timeout";
        
        case 505:
            return "HTTP Version Not Supported";

            
        default:
            return " ";
        }
    }

    
    

    

    /**
     * encodes the given byte array
     * 
     * @param bytes   byte array to encode
     * @return the encoded byte array 
     * @throws IOException if an exception occurs 
     */
    public static byte[] encodeBase64(byte bytes[]) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        byte buffer[] = new byte[1024];
        int got = -1;
        int off = 0;
        int count = 0;
        
        while ((got = in.read(buffer, off, 1024 - off)) > 0) {
        
            if (got >= 3) {
                got += off;
                off  = 0;
                
                while (off + 3 <= got) {
                    int c1 = (buffer[off] & 0xfc) >> 2;
                    int c2 = ((buffer[off]&0x3) << 4) | ((buffer[off+1]&0xf0) >>> 4);
                    int c3 = ((buffer[off+1] & 0x0f) << 2) | ((buffer[off+2] & 0xc0) >>> 6);
                    int c4 = buffer[off+2] & 0x3f;
                    
                    switch (count) {
                        case 73:
                           out.write(base64[c1]);
                           out.write(base64[c2]);
                           out.write(base64[c3]);
                           out.write ('\n') ;
                           out.write(base64[c4]);
                           count = 1 ;
                           break ;
                           
                         case 74:
                           out.write(base64[c1]);
                           out.write(base64[c2]);
                           out.write ('\n') ;
                           out.write(base64[c3]);
                           out.write(base64[c4]) ;
                           count = 2 ;
                           break ;
                           
                         case 75:
                           out.write(base64[c1]);
                           out.write ('\n') ;
                           out.write(base64[c2]);
                           out.write(base64[c3]);
                           out.write(base64[c4]) ;
                           count = 3 ;
                           break ;
                           
                         case 76:
                           out.write('\n') ;
                           out.write(base64[c1]);
                           out.write(base64[c2]);
                           out.write(base64[c3]);
                           out.write(base64[c4]);
                           count = 4;
                           break;
                           
                         default:
                           out.write(base64[c1]);
                           out.write(base64[c2]);
                           out.write(base64[c3]);
                           out.write(base64[c4]);
                           count += 4;
                           break;
                           
                    }
                    off += 3;
                }
                
                for ( int i = 0 ; i < 3 ;i++) {
                    buffer[i] = (i < got-off) ? buffer[off+i] : ((byte) 0);
                }
                off = got-off ;
                
             } else {
                off += got;
             }
        }


        switch (off) {
            case 1:
                out.write(base64[(buffer[0] & 0xfc) >> 2]);
                out.write(base64[((buffer[0]&0x3) << 4) | ((buffer[1]&0xf0) >>> 4)]);
                out.write('=');
                out.write('=');
                break ;
                
            case 2:
                out.write(base64[(buffer[0] & 0xfc) >> 2]);
                out.write(base64[((buffer[0]&0x3) << 4) | ((buffer[1]&0xf0) >>> 4)]);
                out.write(base64[((buffer[1] & 0x0f) << 2) | ((buffer[2] & 0xc0) >>> 6)]);
                out.write('=');
        }
    
        return out.toByteArray();
    }
    


    /**
     * decodes the given byte array
     * 
     * @param bytes   byte array to decode
     * @return the decoded byte array 
     * @throws IOException if an exception occurs 
     */
    public static byte[] decodeBase64(byte bytes[]) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        byte buffer[] = new byte[1024];
        byte chunk[] = new byte[4];
        int got = -1;
        int ready = 0;

        fill : while ((got = in.read(buffer)) > 0) {
            int skiped = 0;
            while (skiped < got) {
                while (ready < 4) {
                    if (skiped >= got) {
                        continue fill;
                    }

                    int ch = buffer[skiped++];

                    if ((ch >= 'A') && (ch <= 'Z')) {
                        ch = ch - 'A';
                    } else if ((ch >= 'a') && (ch <= 'z')) {
                        ch =  ch - 'a' + 26;
                    } else if ((ch >= '0') && (ch <= '9')) {
                        ch = ch - '0' + 52;
                    } else {
                        switch (ch) {
                            case '=':
                                ch = 65;
                                break;
                            
                            case '+':
                                ch = 62;
                                break;
                            
                            case '/':
                                ch = 63;
                                break;
                                
                            default:
                                ch = -1;
                        }
                    }
                    
                    if (ch >= 0) {
                        chunk[ready++] = (byte) ch;
                    }
                }
                
                if (chunk[2] == 65) {
                    out.write(((chunk[0] & 0x3f) << 2) | ((chunk[1] & 0x30) >>> 4));
                    out.flush();
                    return out.toByteArray();
                    
                } else if (chunk[3] == 65) {
                    out.write(((chunk[0] & 0x3f) << 2) | ((chunk[1] & 0x30) >>> 4));
                    out.write(((chunk[1] & 0x0f) << 4) | ((chunk[2] & 0x3c) >>> 2));
                    out.flush();
                    return out.toByteArray();
                    
                } else {
                    out.write(((chunk[0] & 0x3f) << 2) | ((chunk[1] & 0x30) >>> 4));
                    out.write(((chunk[1] & 0x0f) << 4) | ((chunk[2] & 0x3c) >>> 2));
                    out.write(((chunk[2] & 0x03) << 6) | (chunk[3] & 0x3f));
                    
                }
                ready = 0;
            }
        }
        if (ready != 0) {
            throw new IOException("invalid length");
        }
        
        out.flush();
        return out.toByteArray();
    }
    

    
    /**
     * validate, based on a leading int length field. The length field will be removed
     * 
     * @param connection     the connection
     * @return the length 
     * @throws IOException if an exception occurs
     * @throws BufferUnderflowException if not enough data is available
     */
    public static int validateSufficientDatasizeByIntLengthField(NonBlockingBodyDataSource stream) throws IOException, BufferUnderflowException {
        return validateSufficientDatasizeByIntLengthField(stream, true) ;
    }
        
    
    /**
     * validate, based on a leading int length field, that enough data (getNumberOfAvailableBytes() >= length) is available. If not,
     * an BufferUnderflowException will been thrown. 
     * 
     * @param connection         the connection
     * @param removeLengthField  true, if length field should be removed
     * @return the length 
     * @throws IOException if an exception occurs
     * @throws BufferUnderflowException if not enough data is available
     */
    public static int validateSufficientDatasizeByIntLengthField(NonBlockingBodyDataSource stream, boolean removeLengthField) throws IOException, BufferUnderflowException {

        stream.resetToReadMark();
        stream.markReadPosition();
        
        // check if enough data is available
        int length = stream.readInt();
        if (stream.available() < length) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("insufficient data. require " + length + " got "  + stream.available());
            }
            throw new BufferUnderflowException();
    
        } else { 
            // ...yes, remove mark
            if  (!removeLengthField) {
                stream.resetToReadMark();
            }
            stream.removeReadMark();
            return length;
        }
    }  
    
    
    
    /**
     * get the mime type file to extension map 
     *
     * @return the mime type file to extension map
     */
    public synchronized static Map<String, String> getMimeTypeMapping() {
        
        if (mimeTypeMap == null) {
            
            Map<String, String> map = new HashMap<String, String>();
            mimeTypeMap = Collections.unmodifiableMap(map); 
            
            InputStreamReader isr = null;
            LineNumberReader lnr = null;
            try {
                isr = new InputStreamReader(HttpUtils.class.getResourceAsStream("/org/xlightweb/mime.types"));
                if (isr != null) {
                    lnr = new LineNumberReader(isr);
                    String line = null;
                    while (true) {
                        line = lnr.readLine();
                        if (line != null) {
                            line = line.trim();
                            if (!line.startsWith("#")) {
                                StringTokenizer st = new StringTokenizer(line);
                                if (st.hasMoreTokens()) {
                                    String mimeType = st.nextToken();
                                    while (st.hasMoreTokens()) {
                                        String extension = st.nextToken();
                                        map.put(extension, mimeType);
                                            
                                        if (LOG.isLoggable(Level.FINER)) {
                                            LOG.finer("mapping " + extension + " -> " + mimeType + " added");
                                        }
                                    }
                                } else {
                                    if (LOG.isLoggable(Level.FINE)) {
                                        LOG.fine("line " + line + "ignored");
                                    }   
                                }
                            }
                        } else {
                            break;
                        }
                    } 
        
                    lnr.close();
                }
                
            } catch (Exception ioe) { 
                // eat and log exception
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("could not read mime.types. reason: " + ioe.toString());
                }               
                
            } finally {
                try {
                    if (lnr != null) {
                        lnr.close();
                    }
                        
                    if (isr != null) {
                        isr.close();
                    }
                } catch (IOException ioe) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("exception occured by closing mime.types file stream " + ioe.toString());
                    }
                }
            } 

            
            
        }
        
        return mimeTypeMap;
    }

    
    

    /**
     * injects a server field
     *
     * @param handler   the handler
     * @param server    the server to inject
     */
    static void injectServerField(Object handler, IServer server) {
        Field[] fields = handler.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Resource.class)) {
                Resource res = field.getAnnotation(Resource.class);
                if ((field.getType() == IServer.class) || (res.type() == IServer.class)) {
                    field.setAccessible(true);
                    try {
                        field.set(handler, server);
                    } catch (IllegalAccessException iae) {
                        LOG.warning("could not set HandlerContext for attribute " + field.getName() + ". Reason " + DataConverter.toString(iae));
                    }
                }
            }
        }
    }





    static boolean hasFormUrlencodedContentType(IHttpMessage message) {
        
        if (!message.hasBody()) {
            return false;
        }
        
        return hasContentType(message.getPartHeader(), "application/x-www-form-urlencoded");
    }

    
    /**
     * returns if the content type of the header match with the given one
     * 
     * @param header       the header
     * @param contentType  the content type
     * @return true, if the content type of the header match with the given one
     */
    public static boolean hasContentType(IHeader header, String contentType) {
        return (header.getContentType() != null) && (parseMediaType(header.getContentType()).equalsIgnoreCase(contentType));
    }
    
    
    
    
    /**
     * returns the encoding type of content typed FORM URL encoded message
     * 
     * @param message  the message 
     * @return the encoding type
     */
    static String getContentTypedFormUrlencodedEncodingType(IHttpMessage message) {
        
        String contentType = message.getContentType();
        if ((contentType != null) && (contentType.startsWith("application/x-www-form-urlencoded"))) {
            String[] parts = contentType.split(";");
            if (parts.length > 1) {
                for(int i = 1; i < parts.length; i++) {
                    String[] kvp = parts[i].split("=");
                    if (kvp[0].trim().toUpperCase().equals("CHARSET")) {
                        return kvp[1].trim();
                    }
                }
            }
        }
        
        return IHttpMessage.DEFAULT_ENCODING;
    }
    

    
    /**
     * creates a FORM URL encoded body message wrapper
     * 
     * @param request  the request 
     * @return a FORM URL encoded body message wrapper
     * @throws IOException  if an exception occurs 
     */
    static IHttpRequest newFormEncodedRequestWrapper(IHttpRequest request) throws IOException {
        if (request instanceof HttpRequestWrapper) {
            return request;
        }
        
        return new HttpRequestWrapper(new FormEncodedRequestHeaderWrapper(request), request);
    }
    
    
    

    static String[] retrieveMappings(IWebHandler handler) {
        String[] mappings = mappingCache.get(handler.getClass());
        
        if (mappings == null) {
            Mapping mappingAnnotation = handler.getClass().getAnnotation(Mapping.class);
            if (mappingAnnotation != null) {
                mappings = mappingAnnotation.value();
                mappingCache.put(handler.getClass(), mappings);
            }
        }

        return mappings;
    }   
    
    
    static CompletionHandlerInfo getCompletionHandlerInfo(IWriteCompletionHandler handler) {
        CompletionHandlerInfo completionHandlerInfo = completionHandlerInfoCache.get(handler.getClass());

        if (completionHandlerInfo == null) {
            completionHandlerInfo = new CompletionHandlerInfo(handler);
            completionHandlerInfoCache.put(handler.getClass(), completionHandlerInfo);
        }

        return completionHandlerInfo;
    }   

    /**
     * returns the part handler info 
     * 
     * @param partHandler the part handler 
     * @return the part handler info
     */
    @SuppressWarnings("unchecked")
    static PartHandlerInfo getPartHandlerInfo(IPartHandler partHandler) {
        if (partHandler == null) {
            if (emptyPartHandlerInfo == null) {
                emptyPartHandlerInfo = new PartHandlerInfo(null);
            }
            return emptyPartHandlerInfo;
        }

        PartHandlerInfo partHandlerInfo = partHandlerInfoCache.get(partHandler.getClass());

        if (partHandlerInfo == null) {
            partHandlerInfo = new PartHandlerInfo((Class<IPartHandler>) partHandler.getClass());
            partHandlerInfoCache.put(partHandler.getClass(), partHandlerInfo);
        }

        return partHandlerInfo;
    }
    
    
    static String getRequestURLWithoutQueryParams(IHttpRequestHeader header) {
        
        String url = header.getRequestUrl().toString();
        
        int idx = url.indexOf("?");
        if (idx != -1) {
            url = url.substring(0, idx); 
        }

        return url;
    }

    
    /**
     * returns the response handler info
     * 
     * @param httpResponseHandler  the response handler
     * @return the response handler info
     */
    @SuppressWarnings("unchecked")
    private static ResponseHandlerInfo getHttpResponseHandlerInfo(IHttpResponseHandler httpResponseHandler) {
        if (httpResponseHandler == null) {
            if (emptyResponseHandlerInfo == null) {
                emptyResponseHandlerInfo = new ResponseHandlerInfo(null);
            }
            return emptyResponseHandlerInfo;
        }

        ResponseHandlerInfo httpResponseHandlerInfo = httpResponseHandlerInfoCache.get(httpResponseHandler.getClass());

        if (httpResponseHandlerInfo == null) {
            httpResponseHandlerInfo = new ResponseHandlerInfo((Class<IHttpResponseHandler>) httpResponseHandler.getClass());
            httpResponseHandlerInfoCache.put(httpResponseHandler.getClass(), httpResponseHandlerInfo);
        }

        return httpResponseHandlerInfo;
    }
    

    /**
     * returns the connection handler info
     * 
     * @param httpConnectionHandler  the connection handler 
     * @return the connection handler info
     */
    @SuppressWarnings("unchecked")
    static HttpConnectionHandlerInfo getHttpConnectionHandlerInfo(IHttpConnectionHandler httpConnectionHandler) {
        if (httpConnectionHandler == null) {
            return EMPTY_HTTP_CONNECTION_HANDLER_INFO;
        }

        HttpConnectionHandlerInfo httpConnectionHandlerInfo = httpConnectionHandlerInfoCache.get(httpConnectionHandler.getClass());

        if (httpConnectionHandlerInfo == null) {
            httpConnectionHandlerInfo = new HttpConnectionHandlerInfo((Class<IHttpConnectionHandler>) httpConnectionHandler.getClass());
            httpConnectionHandlerInfoCache.put(httpConnectionHandler.getClass(), httpConnectionHandlerInfo);
        }

        return httpConnectionHandlerInfo;
    }

     /**
     * returns true, if the response is bodyless per definition
     * @param header  the status
     * @return true, if the response is bodyless per definition
     */
    public static boolean isBodylessStatus(int status) {
        if ((status == 304) ||      // not modified
            (status == 204) ||      // no content  
            (status == 205) ||      // reset content
            (status == 100) ||      // continue
            (status == 101)) {      // Switching Protocols
    
            return true;
        } else {
            return false;
        }
    }   
    
    
    
    static void addConnectionAttribute(IHttpResponseHeader header, IHttpConnection con) {
        if ((header.getAttribute("org.xlightweb.client.connection") == null) && (con != null)) {
            header.setAttribute("org.xlightweb.client.connection", con);
        }
    }
    
    static IHttpConnection getConnectionFromAttribute(IHttpResponseHeader header) {
        return (IHttpConnection) header.getAttribute("org.xlightweb.client.connection");
    }
    
    static Integer getExecutionMode(IBodyDataHandler bodyDataHandler) {
        
        Integer executionMode = bodyDataHandlerExecutionModeCache.get(bodyDataHandler.getClass());
        
        if (executionMode == null) {
            
            if (IUnsynchronized.class.isAssignableFrom(bodyDataHandler.getClass())) {
                executionMode = EXECUTIONMODE_UNSYNCHRONIZED;
                
            } else if (bodyDataHandler.getClass().getName().equals("org.xlightweb.client.DuplicatingBodyForwarder")) {
                executionMode = EXECUTIONMODE_UNSYNCHRONIZED;
                
            } else {
                executionMode = IBodyDataHandler.DEFAULT_EXECUTION_MODE;
                            
                Execution execution = bodyDataHandler.getClass().getAnnotation(Execution.class);
                if (execution != null) {
                    executionMode = execution.value();
                }
                        
                try {
                    Method meth = bodyDataHandler.getClass().getMethod("onData", new Class[] { NonBlockingBodyDataSource.class });
                    execution = meth.getAnnotation(Execution.class);
                    if (execution != null) {
                        executionMode = execution.value();
                    }
                            
                } catch (NoSuchMethodException nsme) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("shouldn't occure because body handler has to have such a method " + nsme.toString());
                    }
                }
            }
            
            bodyDataHandlerExecutionModeCache.put(bodyDataHandler.getClass(), executionMode);
        }
        
        return executionMode;
    }

    

    /**
     * <b>This is a xSocket internal method and subject to change</b>
     */
    @SuppressWarnings("unchecked")
    static RequestHandlerInfo getRequestHandlerInfo(IHttpRequestHandler requestHandler) {
        if (requestHandler == null) {
            if (emptyHttpRequestHandlerInfo == null) {
                emptyHttpRequestHandlerInfo = new RequestHandlerInfo(null);
            }
            return emptyHttpRequestHandlerInfo;
        }

        RequestHandlerInfo requestHandlerInfo = httpRequestHandlerInfoCache.get(requestHandler.getClass());

        // info found in cache?
        if (requestHandlerInfo != null) {
            return requestHandlerInfo;
         
        // .. no, it is not in cache
        } else {
            // create a new one 
            requestHandlerInfo = new RequestHandlerInfo((Class<IHttpRequestHandler>) requestHandler.getClass());
            httpRequestHandlerInfoCache.put(requestHandler.getClass(), requestHandlerInfo);
        }

        return requestHandlerInfo;
    }

    
    /**
     * <b>This is a xSocket internal method and subject to change</b>
     */
    static ResponseHandlerInfo getResponseHandlerInfo(IHttpResponseHandler responseHandler) {
        return getHttpResponseHandlerInfo(responseHandler);
    }

    
        
    public static boolean isAfter(String dateString, long referenceTime) {
        referenceTime = (referenceTime / 1000) * 1000;    // ignore millis
        return DataConverter.toDate(DataConverter.toFormatedRFC822Date(referenceTime)).after(DataConverter.toDate(dateString));
    }
    
    
    

    static boolean isContentTypeSupportsCharset(String contentType) {
        contentType = contentType.toLowerCase();
        return (contentType.startsWith("text") || contentType.startsWith("message/rfc822")); 
    }
    
    
       
    /**
     * returns the given media type string without parameters 
     * 
     * @param mediaType      the media type string
     * @return  the media type string without paramaters 
     */
    public static String removeMediaTypeParameters(String mediaType) {

        int idx = mediaType.indexOf(";");
        if (idx != -1) {
            return mediaType.substring(0, idx).trim();
        } else {
            return mediaType;
        }
    }

    
    

    

    static String parseMediaTypeParameter(String mediaType, String parameterName, boolean isRemoveSurroundingQuote, String dflt) {

        if (mediaType != null) {
            String[] parts = mediaType.split(";");
            if (parts.length > 1) {
                for (String part : parts) {
                    part = part.trim();
                    if (part.toLowerCase().startsWith(parameterName.toLowerCase() + "=")) {
                        String value= part.substring((parameterName.toLowerCase() + "=").length(), part.length());
                        value = value.trim();
                        if (isRemoveSurroundingQuote && value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                            value = value.substring(1, value.length() - 1);
                            value = value.trim();
                        }
                        return value;
                    }
                }
            }
        }
        
        return dflt;
    }

    
    static String parseMediaType(String mediaType) {
        if (mediaType == null) {
            return null;
        }
        String[] parts = mediaType.split(";");
        return parts[0].trim();
    }    
    



    
    /**
     * parse extracts the encoding parameter of the content type
     * @param contentType the content type 
     * @return the encoding or <null>
     */
    public static String parseEncoding(String contentType) {
        return parseMediaTypeParameter(contentType, "charset", true, null);
    }

    
    
    /**
     * parse extracts the encoding parameter of the content type
     * @param contentType the content type 
     * @return the encoding or <null>
     */
    public static String parseEncodingWithDefault(String contentType, String dflt) {
        if (contentType == null) {
            return dflt;
        }
        
        String encoding = parseMediaTypeParameter(contentType, "charset", true, null);
        
        if (encoding == null) {
            String mediaType = parseMediaType(contentType);
            if (mediaType.equalsIgnoreCase("text/xml")) {
                encoding = "us-ascii";
            } else if (mediaType.equalsIgnoreCase("application/json") || mediaType.equalsIgnoreCase("text/event-stream")) {
                encoding = "utf-8";
            } else {
                if (dflt == null) {
                    encoding = IHttpMessage.DEFAULT_ENCODING;
                } else {
                    encoding = dflt;
                }
            }
        }
        
        return encoding;
    }

    
    static String addEncodingIfNotPresent(String contentType) {
        if (contentType == null) {
            return null;
        }
        
        String encoding = parseMediaTypeParameter(contentType, "charset", true, null);
        
        if (encoding == null) {
            String mediaType = parseMediaType(contentType);
            if (mediaType.equalsIgnoreCase("text/xml")) {
                return contentType + "; charset=US-ASCII";
            } else if (mediaType.equalsIgnoreCase("application/json") || mediaType.equalsIgnoreCase("text/event-stream")) {
                return contentType + "; charset=utf-8";
            } else {
                return contentType + "; charset=" + IHttpMessage.DEFAULT_ENCODING;
            }
        } else {
            return contentType;
        }
    }
    

    static Integer getListenerExecutionMode(Object listener, String callbackMethodname) {
        Class<?> clazz = listener.getClass();
        
        Integer executionMode = listenerModeCache.get(clazz);

        if (executionMode == null) {
            if (clazz == null) {
                executionMode = Execution.MULTITHREADED;
                
            } else if (IUnsynchronized.class.isAssignableFrom(clazz)) {
                executionMode = EXECUTIONMODE_UNSYNCHRONIZED;
                
            } else {
                boolean isMultiThreaded = HttpUtils.isHandlerMultithreaded(clazz, true);
                isMultiThreaded = HttpUtils.isMethodMultithreaded(clazz, callbackMethodname, isMultiThreaded);
                
                if (isMultiThreaded) {
                    executionMode = Execution.MULTITHREADED;
                } else {
                    executionMode = Execution.NONTHREADED;
                }
            }
            
            listenerModeCache.put(clazz, executionMode);
        }

        return executionMode;

    }
    
    
    @SuppressWarnings("unchecked")
    static WebSocketHandlerInfo getWebSocketHandlerInfo(IWebSocketHandler webSocketHandler) {
        if (webSocketHandler == null) {
            return EMPTY_WEB_SOCKET_HANDLER_INFO;
        }

        WebSocketHandlerInfo webSocketHandlerInfo = webSocketHandlerInfoCache.get(webSocketHandler.getClass());

        if (webSocketHandlerInfo == null) {
            webSocketHandlerInfo = new WebSocketHandlerInfo((Class<IWebSocketHandler>) webSocketHandler.getClass());
            webSocketHandlerInfoCache.put(webSocketHandler.getClass(), webSocketHandlerInfo);
        }

        return webSocketHandlerInfo;
    }
    
    
    @SuppressWarnings("unchecked")
    static IEventHandlerInfo getWebEventHandlerInfo(IEventHandler webEventHandler) {
        if (webEventHandler == null) {
            return EMPTY_WEB_EVENT_HANDLER_INFO;
        }

        IEventHandlerInfo webEventHandlerInfo = webEventHandlerInfoCache.get(webEventHandler.getClass());

        if (webEventHandlerInfo == null) {
            webEventHandlerInfo = new IEventHandlerInfo((Class<IEventHandler>) webEventHandler.getClass());
            webEventHandlerInfoCache.put(webEventHandler.getClass(), webEventHandlerInfo);
        }

        return webEventHandlerInfo;
    }    
    
    
    
    /**
     * Copies a request. Only complete received message are supported
     *  
     * @param request   the request
     * @return the copy
     * @throws IOException if an exception occurs
     */
    public static IHttpRequest copy(IHttpRequest request) throws IOException {

        if (request.hasBody()) {
            if (!request.getNonBlockingBody().isCompleteReceived()) {
                throw new IOException("copy is only supported for complete received messages (hint: uses @InvokeOn(InvokeOn.MESSAGE_RECEIVED) annotation)");
            }
    
            return new HttpRequest(request.getRequestHeader().copy(), request.getNonBlockingBody().copyContent());
            
        } else {
            return new HttpRequest(request.getRequestHeader().copy());          
        }
    }
    
    

    /**
     * Copies a response. Only complete received message are supported 
     *  
     * @param response  the response
     * @return the copy
     * @throws IOException if an exception occurs
     */
    public static IHttpResponse copy(IHttpResponse response) throws IOException {

        if (response.hasBody()) {
            if (!response.getNonBlockingBody().isCompleteReceived()) {
                throw new IOException("copy is only supported for complete received messages (hint: uses @InvokeOn(InvokeOn.MESSAGE_RECEIVED) annotation)");
            }
    
            return new HttpResponse(response.getResponseHeader().copy(), response.getNonBlockingBody().copyContent());
            
        } else {
            return new HttpResponse(response.getResponseHeader().copy());
        }
    }
    
    
    static boolean isTextMimeType(String mimeType) {
        mimeType = mimeType.toLowerCase();
        mimeType = mimeType.trim();
        
        if (mimeType.startsWith("text")) {
            return true;
        }
        
        if (mimeType.startsWith("message/rfc822")) {
            return true;
        }
        
        return false;
    }
    
    
    static int[] computeFromRangePosition(String range, int length) throws BadMessageException {
        
        int[] positions = new int[2];
        
        int idx = range.indexOf("-");
        
        if (idx == -1) {
            throw new BadMessageException("invalid range header entry " + range);
        } 
        
        String from = range.substring(0, idx).trim();
        String to = range.substring(idx + 1, range.length()).trim();
        
        if (from.length() > 0) {
            positions[0] = Integer.parseInt(from);
            
            if (to.length() > 0) {
                // e.g. 500-1000
                positions[1] = Integer.parseInt(to);
            } else {
                // e.g. 500-
                positions[1] = length - 1;
            }
        } else {
            if (to.length() > 0) {
                // e.g. -3000
                positions[0] = length - Integer.parseInt(to); 
                positions[1] = length -1;
                
            } else {
                throw new BadMessageException("invalid range header entry " + range);
            }
        }
        
        if ((positions[0] < 0) || (positions[1] < 0) ||
            (positions[0] >= length) || (positions[1] >= length)) {
            throw new BadMessageException("invalid range header entry " + range);
        }
        
        return positions;  
    }

    
    static String detectEncoding(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);

            try {
                byte[] data = new byte[BOM_MAX_LENGTH];
                fis.read(data);
                
                return detectEncoding(data);
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
                
        } catch (IOException ioe) {
            return null;
        }
    }


    static String detectEncoding(byte[] data) throws BufferUnderflowException {
        if ((data != null) && (data.length > 0)) {
            for (Bom bom : BOMS) {
                if (bom.match(data)) {
                    return bom.getEncoding();
                }
            }
        }
      
        if ((data == null) || (data.length < BOM_MAX_LENGTH)) {
            throw new BufferUnderflowException();
        }
        return null;
    }   

    
    static boolean startsWithUTF8BOM(ByteBuffer buffer) throws BufferUnderflowException {
        return BOM_UTF_8.match(buffer);
    }  
    
    static boolean startsWithUTF16BEBOM(ByteBuffer buffer) throws BufferUnderflowException {
        return BOM_UTF_16BE.match(buffer);
    }  

    static boolean startsWithUTF16LEBOM(ByteBuffer buffer) throws BufferUnderflowException {
        return BOM_UTF_16LE.match(buffer);
    }  

    static boolean startsWithUTF32BEBOM(ByteBuffer buffer) throws BufferUnderflowException {
        return BOM_UTF_32BE.match(buffer);
    }  
    
    static boolean startsWithUTF32LEBOM(ByteBuffer buffer) throws BufferUnderflowException {
        return BOM_UTF_32LE.match(buffer);
    }  
    
    
    static int computeRemaining(ByteBuffer[] bufs) {
        int remaining = 0;
        
        if (bufs == null) {
            return 0;
        }
        
        for (ByteBuffer byteBuffer : bufs) {
            if (byteBuffer != null) {
                remaining += byteBuffer.remaining();
            }
        }
        
        return remaining;
    }
    

    static boolean isEmpty(ByteBuffer buffer) {
        if (buffer == null) {
            return true;
        }
        
        return (buffer.remaining() < 1);
    }

    

    static boolean isEmpty(ByteBuffer[] buffer) {
        if (buffer == null) {
            return true;
        }
        
        
        for (ByteBuffer byteBuffer : buffer) {
            if (byteBuffer != null) {
                if (byteBuffer.hasRemaining()) {
                    return false;
                }
            }
        }
        
        return true;
    }

    


    
    
    static ByteBuffer[] compact(ByteBuffer[] buffers) {
        if (buffers == null) {
            return null;
        }
 
        // just one buffer?
        if (buffers.length == 1) {
            
            if (buffers[0] == null) {
                return null;
                
            } else {
                if (buffers[0].remaining() == 0) {
                    return null;
                } else {
                    return buffers;
                }
            }
        }
        
        // more than one buffer
        ByteBuffer[] result = null;
        for (ByteBuffer buffer : buffers) {
            if (buffer != null) {
                result = merge(result, buffer);
            }
        }
        
        return result;
    }

    
    
    private static ByteBuffer[] merge(ByteBuffer[] buffers, ByteBuffer tailBuffer) {
        if (tailBuffer.remaining() == 0) {
            return buffers;
        }
        
        if ((buffers == null) || (buffers.length == 0)) {
            return new ByteBuffer[] { tailBuffer };
            
        } else {
            ByteBuffer[] result = new ByteBuffer[buffers.length + 1];
            System.arraycopy(buffers, 0, result, 0, buffers.length);
            result[buffers.length] = tailBuffer;
            
            return result;
        }
    }
    
    

    static ByteBuffer[] merge(ByteBuffer[] buffers, ByteBuffer[] tailBuffers) {
        if ((buffers == null) || (buffers.length == 0)) {
            return tailBuffers;
            
        } else {
            ByteBuffer[] result = new ByteBuffer[buffers.length + tailBuffers.length];
            System.arraycopy(buffers, 0, result, 0, buffers.length);
            System.arraycopy(tailBuffers, 0, result, buffers.length, tailBuffers.length);
            
            return result;
        }
    }
    
    
    static ByteBuffer duplicateAndMerge(ByteBuffer[] buffers) {

        if (buffers.length == 0) {
            return ByteBuffer.allocate(0);
            
        } else if (buffers.length == 1) {
            return buffers[0].duplicate();
            
        } else {
            int size = 0;
            for (ByteBuffer byteBuffer : buffers) {
                if (byteBuffer != null) {
                    size += byteBuffer.remaining();
                }
            }
            
            ByteBuffer buffer = ByteBuffer.allocate(size);
            for (ByteBuffer byteBuffer : buffers) {
                if (byteBuffer != null) {
                    int pos = byteBuffer.position();
                    int limit = byteBuffer.limit();
                    buffer.put(byteBuffer);
                    
                    byteBuffer.position(pos);
                    byteBuffer.limit(limit);
                }
            }
            
            buffer.flip();
            return buffer;
        }
    }
    

    
    static ByteBuffer merge(ByteBuffer buffer, ByteBuffer[] tailBuffers) {
        
        if ((buffer == null) || (buffer.remaining() == 0)) {
            return merge(tailBuffers);
        }
        
        
        int size = buffer.remaining();
        for (ByteBuffer byteBuffer : tailBuffers) {
            if (byteBuffer != null) {
                size += byteBuffer.remaining();
            }
        }
            
        ByteBuffer result = ByteBuffer.allocate(size);
        result.put(buffer);
        for (ByteBuffer byteBuffer : tailBuffers) {
            if (byteBuffer != null) {
                int pos = byteBuffer.position();
                int limit = byteBuffer.limit();
                result.put(byteBuffer);
                    
                byteBuffer.position(pos);
                byteBuffer.limit(limit);
            }
        }
            
        result.flip();
        return result;
    }
    

    static ByteBuffer merge(ByteBuffer[] buffers) {

        if ((buffers == null) || (buffers.length == 0)) {
            return ByteBuffer.allocate(0);
            
        } else if (buffers.length == 1) {
            return buffers[0];
            
        } else {
            int size = 0;
            for (ByteBuffer byteBuffer : buffers) {
                if (byteBuffer != null) {
                    size += byteBuffer.remaining();
                }
            }
            
            ByteBuffer buffer = ByteBuffer.allocate(size);
            for (ByteBuffer byteBuffer : buffers) {
                if (byteBuffer != null) {
                    int pos = byteBuffer.position();
                    int limit = byteBuffer.limit();
                    buffer.put(byteBuffer);
                    
                    byteBuffer.position(pos);
                    byteBuffer.limit(limit);
                }
            }
            
            buffer.flip();
            return buffer;
        }
    }


        
    static ByteBuffer merge(ByteBuffer buffer, ByteBuffer tailBuffer) {
        if ((buffer == null) || (buffer.remaining() == 0)) {
            return tailBuffer;
        }
        
        if ((tailBuffer == null) || (tailBuffer.remaining() == 0)) {
            return buffer;
        }
        
        ByteBuffer result = ByteBuffer.allocate(buffer.remaining() + tailBuffer.remaining());
        result.put(buffer);
        result.put(tailBuffer);
        result.flip();
            
        return result;          
    }
        

    
    static byte[] merge(byte[] bytes,  byte[] tailBytes) {
        byte[] b = new byte[bytes.length + tailBytes.length];
        
        System.arraycopy(bytes, 0, b, 0, bytes.length);
        System.arraycopy(tailBytes, 0, b, bytes.length, tailBytes.length);
        
        return b;
    }
    
    
    
    
    
    /**
     * copies the given buffer 
     * 
     * @param buffer  the buffer to copy
     * @return the copy
     */
    static ByteBuffer copy(ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        }
        
        return ByteBuffer.wrap(DataConverter.toBytes(buffer));
    }
    
    
    /**
     * wraps a exception with an io exception 
     * 
     * @param t   the exception
     * @return the ioe exception
     */
    public static IOException toIOException(Throwable t) {
        if (t instanceof IOException) {
            return (IOException) t;
        } else {
            IOException ioe = new IOException(t.getClass().getName() + ": " + t.toString());
            ioe.setStackTrace(t.getStackTrace());
            return ioe;
        }
    }
    
    
    /**
     * @deprecated
     */
    public static BlockingBodyDataSource compress(BlockingBodyDataSource dataSource) throws IOException {
        byte[] compressedData = compress(dataSource.readBytes());
        InMemoryBodyDataSource compressedDataSource = new InMemoryBodyDataSource(dataSource.getHeader(), compressedData); 
        return new BlockingBodyDataSource(compressedDataSource);
    }
    

       /**
     * compresses a body (using the GZIP compression)
     * 
     * @param dataSource   the body to compress
     * @return the compressed body 
     * @throws IOException if an excption occurs
     */
    public static BodyDataSource compress(BodyDataSource dataSource) throws IOException {
        byte[] compressedData = compress(dataSource.readBytes());
        InMemoryBodyDataSource compressedDataSource = new InMemoryBodyDataSource(dataSource.getHeader(), compressedData); 
        return new BodyDataSource(compressedDataSource);
    }
    
    
    /**
     * compresses a byte array (using the GZIP compression)
     * 
     * @param dataSource   the byte array to compress
     * @return the compressed byte array
     * @throws IOException if an excption occurs
     */
    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        GZIPOutputStream out = new GZIPOutputStream(bos);
        
        out.write(data);
        out.close();
        return bos.toByteArray();
    }
    

    static ByteBuffer[] readFile(File file) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel fc = raf.getChannel();

        ByteBuffer buffer = ByteBuffer.allocate((int) fc.size());
        fc.read(buffer);
        fc.close();
        raf.close();
        buffer.flip();
        
        return new ByteBuffer[] { buffer };
    }
    
    
    /**
     * @deprecated
     * 
     */
    public static BlockingBodyDataSource decompress(BlockingBodyDataSource dataSource) throws IOException {
        
        byte[] decompressedData = decompress(dataSource.readBytes());
        
        InMemoryBodyDataSource decompressedDataSource = new InMemoryBodyDataSource(dataSource.getHeader(), decompressedData); 

        return new BlockingBodyDataSource(decompressedDataSource);
    }

    
    /**
     * decompresses a body (using the GZIP decompression)
     * 
     * @param dataSource   the body to decompress
     * @return the decompressed body 
     * @throws IOException if an excption occurs
     */
    public static BodyDataSource decompress(BodyDataSource dataSource) throws IOException {
        
        byte[] decompressedData = decompress(dataSource.readBytes());
        
        InMemoryBodyDataSource decompressedDataSource = new InMemoryBodyDataSource(dataSource.getHeader(), decompressedData); 

        return new BodyDataSource(decompressedDataSource);
    }
    
    
    /**
     * decompresses a byte array (using the GZIP decompression)
     * 
     * @param compressedData
     * @return the deconmpressed byte array
     * @throws IOException
     */
    public static byte[] decompress(byte[] compressedData) throws IOException {
        
        ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
        GZIPInputStream in = new GZIPInputStream(bis);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        byte[] transferBuffer = new byte[4096];
        int read = 0;
        do {
            read = in.read(transferBuffer);
            if (read != -1) {
                bos.write(transferBuffer, 0, read);
            }
        } while (read != -1);
        in.close();
                
        in.close();
        bos.close();
        
        return bos.toByteArray();
    }
    
    
    static boolean isCompressable(File file) {
        return isCompressableMimeType(resolveContentTypeByFileExtension(file));
    }
    
    
    public static boolean isCompressableMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        } else {
            mimeType = parseMediaType(mimeType.toLowerCase());
            
            if (mimeType.startsWith("text/") && !mimeType.startsWith("text/event-stream")) {
                return true;
            }
            
            if (mimeType.startsWith("application/")) {
                
                if (mimeType.startsWith("application/json") || 
                    mimeType.startsWith("application/x-www-form-urlencoded") ||
                    mimeType.startsWith("application/x-javascript")) {
                    return true;
                }

                if (mimeType.endsWith("+xml")) {
                    return true;
                }
            }
            
            return false;
        }
    }

    
    /**
     * schedule a time task 
     * 
     * @param task    the timer task
     * @param delay   the delay 
     * @param period   the period
     */
    static void schedule(TimerTask task, long delay, long period) {
        TIMER.schedule(task, delay, period);
    }
    
    
    /**
     * schedule a time task 
     * 
     * @param task    the timer task
     * @param delay   the delay 
     */
    static void schedule(TimerTask task, long delay) {
        TIMER.schedule(task, delay);
    }
    
    static long computeRemainingTime(long start, int receiveTimeoutSec) {
        return (start + ((long) receiveTimeoutSec * 1000)) - System.currentTimeMillis();
    } 
    

    static boolean isPrintable(IHttpMessage message) {
        if (!message.hasBody()) {
            return true;
        }
        
        String contentType = message.getContentType();
        if (contentType == null) {
            return true;
        }
        
        contentType = contentType.toLowerCase();
        if (contentType.startsWith("text/") || 
            contentType.startsWith("application/json") || 
            contentType.startsWith("application/x-www-form-urlencoded") ||
            contentType.startsWith("application/x-javascript")) {
            return true;
        } 
        
        contentType = contentType.split(",")[0].trim();
        if (contentType.startsWith("application/") && contentType.endsWith("+xml")) {
            return true;
        }
        
        return false;
    }
    
     
    /**
     * set the caching expires headers of a response 
     * 
     * @param header     the header
     * @param expireSec  the expire time or 0 to set no-cache headers
     */
    public static void setExpireHeaders(IHttpResponseHeader header, int expireSec) {
        if (expireSec > 0) {
            header.setHeader("Expires", toRFC1123DateString(new Date(System.currentTimeMillis() + (expireSec * 1000L))));
            header.setHeader("Cache-Control", "public, max-age=" + expireSec);
            
        } else {
            header.setHeader("Expires", "Fri, 01 Jan 1990 00:00:00 GMT");
            header.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
            header.setHeader("Pragma", "no-cache");
        }
    }
    
    
    /**
     * set a last modified header of the response
     * 
     * @param header      the header
     * @param timeMillis  the last modified time in millis
     */
    public static void setLastModifiedHeader(IHttpResponseHeader header, long timeMillis) {
        header.setHeader("Last-Modified", DataConverter.toFormatedRFC822Date(timeMillis));
    }
   
    
    public static String toDateTimeString(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_PATTERN_1);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(date);
    }
    
    
    public static Date parseDateTimeString(String dateTimeString) throws ParseException {

        try {
            SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_PATTERN_1);
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            return formatter.parse(dateTimeString);

        } catch (ParseException pe) {
            
            try {
                SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_PATTERN_2);
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                return formatter.parse(dateTimeString);
                
            } catch (ParseException pe2) {
                SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_PATTERN_3);
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                return formatter.parse(dateTimeString);
            }
        }

    }
    
    
    static boolean isAcceptEncodingGzip(IHttpRequest request) {
        
        String acceptEncoding = request.getHeader("Accept-Encoding");
        if (acceptEncoding != null ) {
            for (String part : acceptEncoding.split(",")) {
                part = part.toLowerCase().trim();
                if (part.startsWith("gzip")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    
    
    /**
     * computes a  HMAC-SHA1 Signature
     * 
     * @param data  the data
     * @param key   the key 
     * @return the signature
     * @throws IOException if an error occurs
     */
    public static byte[] computeRFC2104HMAC(byte[] data, byte[] key) throws IOException {
        
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");
    
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
    
            byte[] rawHmac = mac.doFinal(data);
    
            return encodeBase64(rawHmac);
            
        } catch (NoSuchAlgorithmException nsae) {
            throw new IOException("required algorithm HmacSHA1 is not supported by environment: " + nsae.toString());
            
        } catch (InvalidKeyException ke) {
            throw new IOException("error occured by initializing mac: " + ke.toString());
        }
    }

    
    public static String toRFC1123DateString(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat(RFC1123_TIME_PATTERN, Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));

        String dateString = formatter.format(date);
        return dateString;
    }
    
    
    public static Date parseHttpDateString(String dateString) {

        try {
            SimpleDateFormat dateParser = new SimpleDateFormat(RFC1123_TIME_PATTERN, Locale.US);
            dateParser.setTimeZone(TimeZone.getTimeZone("GMT"));
            return dateParser.parse(dateString);
            
        } catch (ParseException pe) {

            try {
                SimpleDateFormat dateParser = new SimpleDateFormat(RFC1036_TIME_PATTERN, Locale.US);
                dateParser.setTimeZone(TimeZone.getTimeZone("GMT"));
                return dateParser.parse(dateString);
            } catch (ParseException pe2) {
                return null;
            }
        }

        
    }
    
    /**
     * (deep) copy of the byte buffer array
     *   
     * @param buffers the byte buffer array
     * @return the copy
     */
    static ByteBuffer[] copy(ByteBuffer[] buffers) {
        if (buffers == null) {
            return null;
        }
        
        ByteBuffer[] copy = new ByteBuffer[buffers.length];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = copy(buffers[i]);
        }
        return copy;
    }
    
    
    static Executor getDefaultWorkerPool() {
        return DEFAULT_WORKERPOOL;
    }
    
    static IMultimodeExecutor newMultimodeExecutor() {
        return newMultimodeExecutor(DEFAULT_WORKERPOOL);
    }
    
    
    static IMultimodeExecutor newMultimodeExecutor(Executor workerpool) {
        return new MultimodeExecutor(workerpool);
    }
    
       
    private static final class MultimodeExecutor implements IMultimodeExecutor  {
        
        private final SerializedTaskQueue taskQueue = new SerializedTaskQueue();

        private Executor workerpool = null;
        
        public MultimodeExecutor(Executor workerpool) {
            this.workerpool = workerpool;
        }

        public void processMultithreaded(Runnable task) {
            taskQueue.performMultiThreaded(task, workerpool);
        }

        public void processNonthreaded(Runnable task) {
            taskQueue.performNonThreaded(task, workerpool);
        }
        
        public Executor getWorkerpool() {
            return workerpool;
        }
    }
    
    
    /**
     * returns true if the header contains a Expect: 100-continue header
     * 
     * @param header the request header 
     * @return true if the header contains a Expect: 100-continue header
     */
    public static boolean isContainExpect100ContinueHeader(IHttpMessageHeader header)  {
        
        Boolean isExpectContinue = (Boolean) header.getAttribute("org.xlightweb.containsExpect-100-Continue-header");
        if (isExpectContinue == null) {
            isExpectContinue = isContainExpect100ContinueHeader((IHeader) header);
            header.setAttribute("org.xlightweb.containsExpect-100-Continue-header", isExpectContinue);
        } 

        return isExpectContinue;
    }
     
    static boolean isContainExpect100ContinueHeader(IHeader header)  {
        return ((header.getHeader("Expect") != null)) && (header.getHeader("Expect").equalsIgnoreCase("100-Continue"));
    }
    
    
    public static boolean isContainsExpect100ContinueHeader(IHttpRequest request) {
        return isContainExpect100ContinueHeader(request.getRequestHeader());
    }


    
    public static String removeSurroundingSlashes(String path) {
        path = path.trim();
        
        if (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }
        
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() -1);
        }
        
        return path;
    }
    
    
    
    
    /**
     * returns true if the location is absolute 
     * 
     * @param location  the location
     * @return true if the location is absolute
     */
    static boolean isAbsoluteURL(String location) {
        String locationLowerCase = location.toLowerCase();
        return ((locationLowerCase.startsWith("http://") || locationLowerCase.startsWith("https://")));
    }
    
    
    /**
     * returns the execution mode for request timeout handler 
     * 
     * @param requestTimeoutHandler  the request timeout handler
     * @return the execution mode
     */
    static boolean isRequestTimeoutHandlerMultithreaded(Class<IHttpRequestTimeoutHandler> clazz) {
        Boolean isMultithreaded = requestTimeoutHandlerExecutionModeCache.get(clazz);
         
        if (isMultithreaded == null) {
            int mode = IHttpRequestTimeoutHandler.DEFAULT_EXECUTION_MODE;
            
            Execution execution = clazz.getAnnotation(Execution.class);
            if (execution != null) {
                mode = execution.value();
            }
            
            try {
                Method meth = clazz.getMethod("onRequestTimeout", new Class[] { IHttpConnection.class });
                execution = meth.getAnnotation(Execution.class);
                if (execution != null) {
                    mode = execution.value();
                }
                
            } catch (NoSuchMethodException nsme) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("shouldn't occure because request timeout handlerr has to have such a method " + nsme.toString());
                }
            }
            
            isMultithreaded = (mode == Execution.MULTITHREADED);
            
            bodyCompleteListenerExecutionModeCache.put(clazz, isMultithreaded);
        }

        return isMultithreaded;
    }
    
    

    
    static Map<String, List<String>> parseParamters(String txt, String encoding) {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        
        try {
            String[] params = txt.split("&");
            for (String param : params) {
                String[] kv = param.split("=");
                if (kv.length > 1) {
                    String name = URLDecoder.decode(kv[0], encoding);
                    List<String> values = result.get(name);
                    if (values == null) {
                        values = new ArrayList<String>();
                        result.put(name, values);
                    }
                
                    values.add(URLDecoder.decode(kv[1], encoding));
                }
            }
            
            return result;
        
        } catch (UnsupportedEncodingException use) {
            throw new RuntimeException(use.toString());
        }
    }
    
    
    
    
    static Map<String, List<String>> parseMatrixParamters(String uri, String encoding) {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        
        
        try {
            String[] parts = uri.split(";");
            for (String part : parts) {
                
                String[] kv = part.split("=");
                if (kv.length > 1) {
                    String name = URLDecoder.decode(kv[0], encoding);
                    List<String> values = result.get(name);
                    if (values == null) {
                        values = new ArrayList<String>();
                        result.put(name, values);
                    }
                    
                    values.add(URLDecoder.decode(kv[1], encoding));
                }
            }
            
            return result;
        
        } catch (UnsupportedEncodingException use) {
            throw new RuntimeException(use.toString());
        }
    }
    
    
    
    static String removeMatrixParamter(String uri, String name) {
        
        if (uri == null) {
            return null;
        }
        
        int idx = uri.indexOf(";" + name);
        
        if (idx == -1) {
            return uri;
            
        } else {
            int end = uri.indexOf(";", idx + 1);
            if (end == -1) {
                return uri.substring(0, idx);
            } else {
                return uri.substring(0, idx) + uri.substring(end, uri.length());
            }
        }
    }
    
    
    
    
    
    static String addMatrixParamter(String uri, String name, String value, String encoding) {
            
        try {
            if (uri == null) {
                uri = "";
            } 

            uri += ";" + URLEncoder.encode(name, encoding) + "=" + URLEncoder.encode(value, encoding);
            
            return uri;
        } catch (UnsupportedEncodingException use) {
            throw new RuntimeException(use.toString());
        }
    }
        
    
    
       
    /**
     * returns the content type based on the file extension
     * 
     * @param file the file
     */
    public static final String resolveContentTypeByFileExtension(File file) {

        String mimeType = null;
        
        String name = file.getName();
        int pos = name.lastIndexOf(".");
        if (pos != -1) {
            String extension = name.substring(pos + 1, name.length());
            mimeType = HttpUtils.getMimeTypeMapping().get(extension);
            if (mimeType != null) {
                if (isTextMimeType(mimeType)) {
                    String encoding = HttpUtils.detectEncoding(file);
                    if (encoding != null) {
                        mimeType = mimeType + "; charset=" + encoding;
                    }
                } 
            }
        }
        
        return mimeType;
    }
    
    
    static int convertMillisToSec(long millis) {
        
        if (millis > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
            
        } else if (millis >= 1000) { 
            return ((int) millis) / 1000;

        } else if (millis == 0) {
            return 0;
                        
        } else {
            return 1;
        }
    }
    
    
    /**
     * get the implementation version
     * 
     * @return the implementation version
     */
    public static String getImplementationVersion() {
        
        if (implementationVersion == null) {
            readVersionFile();
        }
        
        return implementationVersion;
    }
    
    
       
    /**
     * get the xSocket implementation version
     * 
     * @return the xSocket implementation version
     */
    static String getXSocketImplementationVersion() {
        
        if (xSocketImplementationVersion == null) {
            readVersionFile();
        }
        
        return xSocketImplementationVersion;
    }

    
    /**
     * <br/><br/><b>This is a xSocket internal method and subject to change</b> 
     */
    public static long parseLong(String longString, long dflt) {
        if (longString == null) {
            return dflt;
        }
        
        try {
            return Long.parseLong(longString);
        } catch (NumberFormatException nfe) {
            return dflt;
        }
    }
    
    
    /**
     * get the implementation date
     * 
     * @return the implementation date
     */
    public static String getImplementationDate() {
        
        if (implementationDate== null) {
            readVersionFile();
        }
        
        return implementationDate;
    }

    
    private static void readVersionFile() {
        
        implementationVersion = "<unknown>";
        implementationDate = "<unknown>";
            
        InputStreamReader isr = null;
        LineNumberReader lnr = null;
        
        try {
            isr = new InputStreamReader(HttpUtils.class.getResourceAsStream("/org/xlightweb/version.txt"));
            if (isr != null) {
                lnr = new LineNumberReader(isr);
                String line = null;
        
                do {
                    line = lnr.readLine();
                    if (line != null) {
                        if (line.startsWith("Implementation-Version=")) {
                            implementationVersion = line.substring("Implementation-Version=".length(), line.length()).trim();
                            
                        } else if (line.startsWith("Implementation-Date=")) {
                            implementationDate = line.substring("Implementation-Date=".length(), line.length()).trim();
                            
                        } else if (line.startsWith("Dependency.xSocket.Implementation-Version=")) {
                            xSocketImplementationVersion = line.substring("Dependency.xSocket.Implementation-Version=".length(), line.length()).trim();
                        }
                    }
                } while (line != null);
                
                lnr.close();
            }
        } catch (Exception ioe) { 
            
            implementationDate = "<unknown>";
            implementationVersion  = "<unknown>";
            xSocketImplementationVersion  = "<unknown>";
                        
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("could not read version file. reason: " + ioe.toString());
            }               
            
        } finally {
            try {
                if (lnr != null) {
                    lnr.close();
                }
                    
                if (isr != null) {
                    isr.close();
                }
            } catch (IOException ioe) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("exception occured by closing version.txt file stream " + ioe.toString());
                }
            }
        } 
    }
    
    
    /**
     * returns if handler is multi threaded 
     * 
     * <br/><br/><b>This is a xLightweb internal method and subject to change</b> 
     * 
     * @param clazz   the handler class 
     * @param dflt    the default value
     * @return true, if multi threaded
     */
    static boolean isHandlerMultithreaded(Class<? extends Object> clazz, boolean dflt) {
        Execution execution = clazz.getAnnotation(Execution.class);
        if (execution != null) {
            if(execution.value() == Execution.NONTHREADED) {
                return false;
                
            } else {
                return true;
            }

        } else {
            return dflt;
        }
    }
    

    /**
     * returns if the handler method is multi threaded 
     * 
     * <br/><br/><b>This is a xSocket internal method and subject to change</b> 
     * 
     * @param clazz       the handler class 
     * @param methodname  the method name
     * @param dflt        the default value
     * @param paramClass  the method parameter classes 
     * @return true, if multi threaded
     */
    @SuppressWarnings("unchecked")
    public static boolean isMethodMultithreaded(Class clazz, String methodname, boolean dflt, Class... paramClass) {
        try {
            Method meth = clazz.getMethod(methodname, paramClass);
            Execution execution = meth.getAnnotation(Execution.class);
            if (execution != null) {
                if(execution.value() == Execution.NONTHREADED) {
                    return false;
                } else {
                    return true;
                }
            } else {
                return dflt;
            }
            
        } catch (NoSuchMethodException nsme) {
            return dflt;
        }
    }
    
    
   
    
    static boolean isSynchronizedOnSession(Class<? extends Object> clazz, boolean dflt) {
        
        SynchronizedOn synchronizedOn = clazz.getAnnotation(SynchronizedOn.class);
          if (synchronizedOn != null) {
              if(synchronizedOn.value() == SynchronizedOn.SESSION) {
                  return true;
                    
              } else {
                  return false;
              }

          } else {
              return dflt;
          }
    }
    
    
    /**
     * returns if the connect handler warning is suppressed
     * 
     * <br/><br/><b>This is a xSocket internal method and subject to change</b>  
     * 
     * @return true, if the connect handler warning is suppressed
     */
    public static boolean isConnectHandlerWarningIsSuppressed() {
        if (isConnectHandlerWarningIsSuppressed == null) {
            isConnectHandlerWarningIsSuppressed = Boolean.parseBoolean(System.getProperty("org.xlightweb.httpConnectHandler.suppresswarning", "false"));
        }
        
        return isConnectHandlerWarningIsSuppressed;
    }
     
    @SuppressWarnings("unchecked")
    static boolean isSynchronizedOnSession(Class clazz, String methodname, boolean dflt, Class... paramClass) {
        try {
            Method meth = clazz.getMethod(methodname, paramClass);
            SynchronizedOn synchronizedOn = meth.getAnnotation(SynchronizedOn.class);
            if (synchronizedOn != null) {
                if(synchronizedOn.value() == SynchronizedOn.SESSION) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return dflt;
            }
            
        } catch (NoSuchMethodException nsme) {
            return dflt;
        }
    }

    
    static boolean isInvokeOnMessageReceived(Class<? extends Object> clazz, boolean dflt) {
         InvokeOn invokeOn = clazz.getAnnotation(InvokeOn.class);
         if (invokeOn != null) {
             if(invokeOn.value() == InvokeOn.MESSAGE_RECEIVED) {
                 return true;
                 
             } else {
                 return false;
             }

         } else {
             return dflt;
         }
    }
    

    @SuppressWarnings("unchecked")
    static boolean isInvokeOnMessageReceived(Class clazz, String methodname, boolean dflt, Class... paramClass) {
        try {
            Method meth = clazz.getMethod(methodname, paramClass);
            InvokeOn invokeOn = meth.getAnnotation(InvokeOn.class);
            if (invokeOn != null) {
                if(invokeOn.value() == InvokeOn.MESSAGE_RECEIVED) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return dflt;
            }
            
        } catch (NoSuchMethodException nsme) {
            return dflt;
        }
    }
    
    
    @SuppressWarnings("unchecked")
    static boolean isContinueHandler(Class<? extends Object> clazz, String methodname, Class... params) {
         
        try {
            Supports100Continue handles100Continue = clazz.getAnnotation(Supports100Continue.class);
            if (handles100Continue != null) {
                return true;
                
            } else {
                Method meth = clazz.getMethod(methodname, params);
                handles100Continue = meth.getAnnotation(Supports100Continue.class);
                if (handles100Continue != null) {
                    return true;
                }
            }
                
        } catch (NoSuchMethodException ignore) { }
        
        return false;
    }


    static boolean isGzipEncoded(IHttpMessageHeader responseHeader) {
        return (responseHeader.getHeader("Content-Encoding") != null) && 
               (responseHeader.getHeader("Content-Encoding").toLowerCase().indexOf("gzip") != -1);
    }
    

    public static boolean isAcceptEncdoingGzip(IHttpRequestHeader requestHeader) {
        return (requestHeader.getHeader("Accept-Encoding") != null) && (requestHeader.getHeader("Accept-Encoding").toLowerCase().indexOf("gzip") != -1);
    }

    
    /**
     * establishs a tcp tunnel 
     * @param httpConnection   the http connection
     * @param target           the target address (e.g. www.gmx.com, www.gmx.com:443)
     * @throws IOException if an exception occurs
     */
    public static void establishTcpTunnel(IHttpConnection httpConnection, String target) throws IOException {
        String targetHost = null;
        int targetPort = 443;
                
        int idx = target.lastIndexOf(":");
        if (idx == -1) {
            targetHost = target;
        } else {
            targetHost = target.substring(0, idx);
            targetPort = Integer.parseInt(target.substring(idx + 1, target.length()));
        }
            
        establishTcpTunnel(httpConnection, targetHost, targetPort);
    }
    
    
    /**
     * establishs a tcp tunnel 
     * @param httpConnection   the http connection
     * @param targetHost       the target host
     * @param targetPort       the target port
     * @throws IOException if an exception occurs
     */
    public static void establishTcpTunnel(IHttpConnection httpConnection, String targetHost, int targetPort) throws IOException {
        INonBlockingConnection forwardCon = new NonBlockingConnection(targetHost, targetPort);
        
        INonBlockingConnection tcpCon = httpConnection.getUnderlyingTcpConnection();

        forwardCon.setAttachment(tcpCon);
        tcpCon.setAttachment(forwardCon);
            
        forwardCon.setFlushmode(FlushMode.ASYNC);
        forwardCon.setAutoflush(false);
        tcpCon.setFlushmode(FlushMode.ASYNC);
        tcpCon.setAutoflush(false);

        forwardCon.setHandler(new TcpProxyHandler());
        tcpCon.setHandler(new TcpProxyHandler());
    }
    
    
    private static class TcpProxyHandler implements IDataHandler, IDisconnectHandler {
        
        public boolean onDisconnect(INonBlockingConnection connection) throws IOException {
            INonBlockingConnection reverseConnection = (INonBlockingConnection) connection.getAttachment();
            if (reverseConnection != null) {
                connection.setAttachment(null);
                NonBlockingConnectionPool.destroy(reverseConnection);
            }
            return true;
        }
        
        
        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            INonBlockingConnection forwardConnection = (INonBlockingConnection) connection.getAttachment();
                
            int available = connection.available();
            if (available > 0) {
                ByteBuffer[] data = connection.readByteBufferByLength(connection.available());
                forwardConnection.write(data);
                forwardConnection.flush();
                
            } else if (available == -1) {
                NonBlockingConnectionPool.destroy(connection);
            }
            
            return true;
        }
    }   

   
    
    static class HttpConnectionHandlerInfo {
        
        private boolean isConnectHandler = false;
        private boolean isConnectHandlerMultithreaded = true;
        
        private boolean isDisconnectHandler = false;
        private boolean isDisconnectHandlerMultithreaded = true;
        
    
        @SuppressWarnings("unchecked")
        public HttpConnectionHandlerInfo(Class clazz) {

            if (clazz == null) {
                return;
            }
            
            
            if (IHttpConnectHandler.class.isAssignableFrom(clazz)) {
                isConnectHandler = true;
                isConnectHandlerMultithreaded = isOnConnectMultithreaded(clazz);
            }

            if (IHttpDisconnectHandler.class.isAssignableFrom(clazz)) {
                isDisconnectHandler = true;
                isDisconnectHandlerMultithreaded = isOnDisconnectMultithreaded(clazz);
            }
        }



        static boolean isOnConnectMultithreaded(Class<IHttpRequestHandler> serverHandlerClass) {
            int mode = IHttpRequestHandler.DEFAULT_EXECUTION_MODE;

            Execution execution = serverHandlerClass.getAnnotation(Execution.class);
            if (execution != null) {
                mode = execution.value();
                return (mode == Execution.MULTITHREADED);
            }

            try {
                Method meth = serverHandlerClass.getMethod("onConnect", new Class[] { IHttpConnection.class });
                execution = meth.getAnnotation(Execution.class);
                if (execution != null) {
                    mode = execution.value();
                }

            } catch (NoSuchMethodException nsme) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("shouldn't occure because body handler has to have such a method " + nsme.toString());
                }
            }

            return (mode == Execution.MULTITHREADED);
        }


        static boolean isOnDisconnectMultithreaded(Class<IHttpRequestHandler> serverHandlerClass) {
            int mode = IHttpRequestHandler.DEFAULT_EXECUTION_MODE;

            Execution execution = serverHandlerClass.getAnnotation(Execution.class);
            if (execution != null) {
                mode = execution.value();
                return (mode == Execution.MULTITHREADED);
            }

            try {
                Method meth = serverHandlerClass.getMethod("onDisconnect", new Class[] { IHttpConnection.class });
                execution = meth.getAnnotation(Execution.class);
                if (execution != null) {
                    mode = execution.value();
                }

            } catch (NoSuchMethodException nsme) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("shouldn't occure because body handler has to have such a method " + nsme.toString());
                }
            }

            return (mode == Execution.MULTITHREADED);
        }

        public boolean isConnectHandler() {
            return isConnectHandler;
        }

        public boolean isConnectHandlerMultithreaded() {
            return isConnectHandlerMultithreaded;
        }

        public boolean isDisconnectHandler() {
            return isDisconnectHandler;
        }

        public boolean isDisconnectHandlerMultithreaded() {
            return isDisconnectHandlerMultithreaded;
        }
    }
    
    
    

    
    private static final class FormEncodedRequestHeaderWrapper extends HttpRequestHeaderWrapper {
        
        private static final Boolean NULL_BOOLEAN = null;
        
        private final IHttpRequest request;
        private final Map<String, List<String>> bodyParamsMap = new HashMap<String, List<String>>();
        
        private boolean isBodyParsed = false;

        
        
        FormEncodedRequestHeaderWrapper(IHttpRequest request) throws IOException {
            super(request.getRequestHeader());
            this.request = request;
        }
            
        

        public Integer getIntParameter(String name) {
            String s = getParameter(name);
            if (s != null) {
                return Integer.parseInt(s);
            } else {
                return null;
            }
        }
        


        public int getIntParameter(String name, int defaultVal) {
            String s = getParameter(name);
            if (s != null) {
                try {
                    return Integer.parseInt(s);
                } catch (Exception e) {
                    return defaultVal;
                }
            } else {
                return defaultVal;
            }
        }
        

        public Long getLongParameter(String name) {
            String s = getParameter(name);
            if (s != null) {
                return Long.parseLong(s);
            } else {
                return null;
            }
        }
        

        public long getLongParameter(String name, long defaultVal) {
            String s = getParameter(name);
            if (s != null) {
                try {
                    return Long.parseLong(s);
                } catch (Exception e) {
                    return defaultVal;
                }
            } else {
                return defaultVal;
            }
        }


        public Double getDoubleParameter(String name) {
            String s = getParameter(name);
            if (s != null) {
                return Double.parseDouble(s);
            } else {
                return null;
            }
        }
        

        public double getDoubleParameter(String name, double defaultVal) {
            String s = getParameter(name);
            if (s != null) {
                try {
                    return Double.parseDouble(s);
                } catch (Exception e) {
                    return defaultVal;
                }
            } else {
                return defaultVal;
            }
        }
        
        
        public Float getFloatParameter(String name) {
            String s = getParameter(name);
            if (s != null) {
                return Float.parseFloat(s);
            } else {
                return null;
            }
        }
        
        
        public float getFloatParameter(String name, float defaultVal) {
            String s = getParameter(name);
            if (s != null) {
                try {
                    return Float.parseFloat(s);
                } catch (Exception e) {
                    return defaultVal;
                }
            } else {
                return defaultVal;
            }
        }
        
        public Boolean getBooleanParameter(String name) {
            String s = getParameter(name);
            if (s != null) {
                return Boolean.parseBoolean(s);
            } else {
                return NULL_BOOLEAN;
            }
        }
        
        
        
        public boolean getBooleanParameter(String name, boolean defaultVal) {
            String s = getParameter(name);
            if (s != null) {
                try {
                    return Boolean.parseBoolean(s);
                } catch (Exception e) {
                    return defaultVal;
                }
            } else {
                return defaultVal;
            }
        }
                

        
        private void parseBodyIfNecessary() {
            
            if (!isBodyParsed) {
                isBodyParsed = true;
                try {
                    if (HttpUtils.hasFormUrlencodedContentType(request) && request.hasBody()) {
                        bodyParamsMap.putAll(parseParamters(request.getNonBlockingBody().toString(), HttpUtils.getContentTypedFormUrlencodedEncodingType(request)));
                    } else {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("warning request does not contain a FORM-URLENCODED body: " + request);
                        }
                    }
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        }
        


        public String getParameter(String name) {
            parseBodyIfNecessary();
            
            if (bodyParamsMap.containsKey(name)) {
                return bodyParamsMap.get(name).get(0);
            }
            
            return getWrappedRequestHeader().getParameter(name);
        }
        
        
        
        public String[] getParameterValues(String name) {
            parseBodyIfNecessary();
            
            ArrayList<String> result = new ArrayList<String>();
            
            if (bodyParamsMap.containsKey(name)) {
                result.addAll(bodyParamsMap.get(name));
            }
            
            String[] v = getWrappedRequestHeader().getParameterValues(name);
            result.addAll(Arrays.asList(v));
            
            return result.toArray(new String[result.size()]);
        }
        
        
        public void setParameter(String parameterName, String parameterValue) {
            parseBodyIfNecessary();
            
            if (bodyParamsMap.containsKey(parameterName)) {
                throw new RuntimeException("parameter is contained in body and can not be modified");
            }
            
            getWrappedRequestHeader().setParameter(parameterName, parameterValue);
        }

        
        public Set<String> getParameterNameSet() {
            parseBodyIfNecessary();
            
            Set<String> result = new HashSet<String>();
            
            result.addAll(getWrappedRequestHeader().getParameterNameSet());
            result.addAll(bodyParamsMap.keySet());
            
            return result;
        }
        
        @SuppressWarnings("unchecked")
        public Enumeration getParameterNames() {
            return Collections.enumeration(getParameterNameSet());
        }
    }

    static final class CompletionHandlerInfo {
        
        private boolean isUnsynchronized = false;
        private boolean isOnWrittenMultithreaded = false;
        private boolean isOnExceptionMultithreaded = false;

        public CompletionHandlerInfo(IWriteCompletionHandler handler) {
            
            boolean isHandlerMultithreaded = isHandlerMultithreaded(handler.getClass(), true);
            
            isOnWrittenMultithreaded = isMethodMultithreaded(handler.getClass(), "onWritten", isHandlerMultithreaded, int.class);
            isOnExceptionMultithreaded = isMethodMultithreaded(handler.getClass(), "onException", isHandlerMultithreaded, IOException.class);
            
            isUnsynchronized = (handler instanceof IUnsynchronized);
        }

        
        public boolean isUnsynchronized() {
            return isUnsynchronized;
        }

        public boolean isOnWrittenMultithreaded() {
            return isOnWrittenMultithreaded;
        }

        public boolean isOnExceptionMutlithreaded() {
            return isOnExceptionMultithreaded;
        }
    }   
    
    
    static final class ResponseHandlerInfo {
        
        private static final List<String> SYSTEM_HANDLERS = Arrays.asList(new String[] { "org.xlightweb.client.CookieHandler$ResponseHandler", 
                                                                                         "org.xlightweb.client.RedirectHandler$BodyRedirectResponseHandler",
                                                                                         "org.xlightweb.client.RedirectHandler$BodylessRedirectResponseHandler",
                                                                                         "org.xlightweb.client.RetryHandler$BodyRetryResponseHandler",
                                                                                         "org.xlightweb.client.RetryHandler$BodylessRetryResponseHandler"});

        
        private boolean isResponseHandler = false;
        private boolean isUnsynchronized = false;
        private boolean isResponseHandlerInvokeOnMessageReceived = false;
        private boolean isResponseHandlerMultithreaded = true;
        private boolean isResponseExeptionHandlerMultithreaded = true;
        
        private boolean isSocketTimeoutHandler = false;
        private boolean isSocketTimeoutHandlerMultithreaded = true;
        
        private boolean isContinueHandler = true;

        
        @SuppressWarnings("unchecked")
        ResponseHandlerInfo(Class clazz) {

            if (clazz == null) {
                return;
            }
            
            isContinueHandler = HttpUtils.isContinueHandler(clazz, "onResponse", IHttpResponse.class);

            
            
            if (IHttpResponseHandler.class.isAssignableFrom(clazz)) {
                isResponseHandler = true;
                isResponseHandlerMultithreaded = isOnResponseMultithreaded((Class<IHttpResponseHandler>) clazz);
                isResponseHandlerInvokeOnMessageReceived = isOnResponseInvokeOnMessageReceived((Class<IHttpResponseHandler>) clazz);
                
                isResponseExeptionHandlerMultithreaded = isOnResponseExceptionMultithreaded((Class<IHttpResponseHandler>) clazz);
            }
            
            isSocketTimeoutHandlerMultithreaded = isResponseExeptionHandlerMultithreaded;

            if (IHttpSocketTimeoutHandler.class.isAssignableFrom(clazz)) {
                isSocketTimeoutHandler = true;
                isSocketTimeoutHandlerMultithreaded = isOnResponseTimeoutMultithreaded((Class<IHttpResponseHandler>) clazz);
            }
            
            isUnsynchronized = IUnsynchronized.class.isAssignableFrom(clazz) || SYSTEM_HANDLERS.contains(clazz.getName());
        }


        
        static boolean isOnResponseMultithreaded(Class<IHttpResponseHandler> handlerClass) {
            boolean isMultithreaded = HttpUtils.isHandlerMultithreaded(handlerClass, (IHttpResponseHandler.DEFAULT_EXECUTION_MODE == Execution.MULTITHREADED));
            return HttpUtils.isMethodMultithreaded(handlerClass, "onResponse", isMultithreaded, IHttpResponse.class);
        }

        
        static boolean isOnResponseExceptionMultithreaded(Class<IHttpResponseHandler> handlerClass) {
            boolean isMultithreaded = HttpUtils.isHandlerMultithreaded(handlerClass, (IHttpResponseHandler.DEFAULT_EXECUTION_MODE == Execution.MULTITHREADED));
            return HttpUtils.isMethodMultithreaded(handlerClass, "onException", isMultithreaded, IOException.class);
        }
        

        static boolean isOnResponseTimeoutMultithreaded(Class<IHttpResponseHandler> handlerClass) {
            boolean isMultithreaded = HttpUtils.isHandlerMultithreaded(handlerClass, (IHttpResponseHandler.DEFAULT_EXECUTION_MODE == Execution.MULTITHREADED));
            return HttpUtils.isMethodMultithreaded(handlerClass, "onException", isMultithreaded, SocketTimeoutException.class);
        }


        static boolean isOnResponseInvokeOnMessageReceived(Class<IHttpResponseHandler> handlerClass) {
            boolean invokeOnMessageReceived = HttpUtils.isInvokeOnMessageReceived(handlerClass, (IHttpResponseHandler.DEFAULT_INVOKE_ON_MODE == InvokeOn.MESSAGE_RECEIVED));
            return HttpUtils.isInvokeOnMessageReceived(handlerClass, "onResponse", invokeOnMessageReceived, IHttpResponse.class);   
        }


        
        public boolean isResponseHandler() {
            return isResponseHandler;
        }
        
        public boolean isResponseHandlerInvokeOnMessageReceived() {
            return  isResponseHandlerInvokeOnMessageReceived;
        }

        public boolean isResponseHandlerMultithreaded() {
            return isResponseHandlerMultithreaded;
        }
        
        public boolean isSocketTimeoutHandler() {
            return isSocketTimeoutHandler;
        }

        public boolean isResponseExeptionHandlerMultithreaded() {
            return isResponseExeptionHandlerMultithreaded;
        }
        
        public boolean isContinueHandler() {
            return isContinueHandler;
        }

        public boolean isSocketTimeoutHandlerMultithreaded() {
            return isSocketTimeoutHandlerMultithreaded;
        }

        
        public boolean isUnsynchronized() {
            return isUnsynchronized;
        }
    }

   
 
    static final class RequestHandlerInfo  {
        
        private static final Logger LOG = Logger.getLogger(RequestHandlerInfo.class.getName());
        
        private static final List<String> SYSTEM_HANDLERS = Arrays.asList(new String[] { "org.xlightweb.client.CookieHandler", 
                                                                                         "org.xlightweb.client.CacheHandler",
                                                                                         "org.xlightweb.client.RedirectHandler", 
                                                                                         "org.xlightweb.client.ProxyHandler",
                                                                                         "org.xlightweb.client.RetryHandler"});
        
        private String clazzName;
        private boolean isUnsynchronized = false;
        private boolean isRequestHandlerSynchronizedOnSession = false;
        private boolean isRequestHandlerInvokeOnMessageReceived = false;
        private boolean isRequestHandlerMultithreaded = true;

        private boolean isLifeCycle = false;
        
        private boolean isRequestTimoutHandler = false;
        private boolean isRequestTimoutHandlerMultithreaded = true;

        private boolean isContinueHandler = true;
        
        

        @SuppressWarnings("unchecked")
        RequestHandlerInfo(Class clazz) {
            if (clazz == null) {
                return;
            }
            
            clazzName = clazz.getName();            
            isLifeCycle = ILifeCycle.class.isAssignableFrom(clazz);
            
            isContinueHandler = HttpUtils.isContinueHandler(clazz, "onRequest", IHttpExchange.class);

            if (IHttpRequestHandler.class.isAssignableFrom(clazz)) {
                isRequestHandlerMultithreaded = isOnRequestMultithreaded((Class<IHttpRequestHandler>) clazz);
                isRequestHandlerInvokeOnMessageReceived = isOnRequestInvokeOnMessageReceived((Class<IHttpRequestHandler>) clazz);
                isRequestHandlerSynchronizedOnSession = isOnSession((Class<IHttpRequestHandler>) clazz);
                
                if (isRequestHandlerSynchronizedOnSession && !isRequestHandlerMultithreaded) {
                    LOG.warning("request handler " + clazz.getName() + " is annotated as session scope and non-threaded. " +
                                "Session scope have to be multi-threaded. Updating execution mode");
                    isRequestHandlerMultithreaded = true;
                }
            }
            
            if (IHttpRequestTimeoutHandler.class.isAssignableFrom(clazz)) {
                isRequestTimoutHandler = true;
                isRequestTimoutHandlerMultithreaded = HttpUtils.isRequestTimeoutHandlerMultithreaded((Class<IHttpRequestTimeoutHandler>) clazz);
            }
            
            isUnsynchronized = IUnsynchronized.class.isAssignableFrom(clazz) || SYSTEM_HANDLERS.contains(clazz.getName());
            
        }


        static boolean isOnRequestMultithreaded(Class<IHttpRequestHandler> serverHandlerClass) {
            boolean isMultithreaded = HttpUtils.isHandlerMultithreaded(serverHandlerClass, (IHttpRequestHandler.DEFAULT_EXECUTION_MODE == Execution.MULTITHREADED));
            return HttpUtils.isMethodMultithreaded(serverHandlerClass, "onRequest", isMultithreaded, IHttpExchange.class);
        }

        
        static boolean isOnRequestInvokeOnMessageReceived(Class<IHttpRequestHandler> handlerClass) {
            boolean invokeOnMessageReceived = HttpUtils.isInvokeOnMessageReceived(handlerClass, (IHttpRequestHandler.DEFAULT_INVOKE_ON_MODE == InvokeOn.MESSAGE_RECEIVED));
            return HttpUtils.isInvokeOnMessageReceived(handlerClass, "onRequest", invokeOnMessageReceived, IHttpExchange.class);    
        }

        
        static boolean isOnSession(Class<IHttpRequestHandler> handlerClass) {
            boolean isSynchronizedOnSession = HttpUtils.isSynchronizedOnSession(handlerClass, (IHttpRequestHandler.DEFAULT_SYNCHRONIZED_ON_MODE == SynchronizedOn.SESSION));
            return HttpUtils.isSynchronizedOnSession(handlerClass, "onRequest", isSynchronizedOnSession, IHttpExchange.class);
        }

        
        
        public boolean isLifeCycle() {
            return isLifeCycle;
        }
        
        public boolean isRequestHandlerInvokeOnMessageReceived() {
            return  isRequestHandlerInvokeOnMessageReceived;
        }

        
        public boolean isRequestHandlerSynchronizedOnSession() {
            return isRequestHandlerSynchronizedOnSession;
        }

        public boolean isRequestHandlerMultithreaded() {
            return isRequestHandlerMultithreaded;
        }
        
        public boolean isRequestTimeoutHandler() {
            return isRequestTimoutHandler;
        }
        
        public boolean isRequestTimeoutHandlerMultithreaded() {
            return isRequestTimoutHandlerMultithreaded;
        }
        
        public boolean isContinueHandler() {
            return isContinueHandler;
        }
        
        public boolean isUnsynchronized() {
            return isUnsynchronized;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(super.toString() + " (");
            sb.append("classname=" + clazzName + " isLifeCycle=" + isLifeCycle + " " +
                      "isUnsynchronized=" + isUnsynchronized + " " +
                      "isContinueHandler=" + isContinueHandler + " " +
                      "isRequestHandlerMultithreaded=" + isRequestHandlerMultithreaded + " " +
                      "isRequestHandlerInvokeOnMessageReceived=" + isRequestHandlerInvokeOnMessageReceived + " " +
                      "isRequestHandlerSynchronizedOnSession=" + isRequestHandlerSynchronizedOnSession + " " + 
                      "isRequestTimoutHandler=" + isRequestTimoutHandler + " " +
                      "isRequestTimoutHandlerMultithreaded=" + isRequestTimoutHandlerMultithreaded + ")");
            return sb.toString();
        }
    }
    
    
    private static class XLightwebThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        XLightwebThreadFactory() {
            namePrefix = "xLightwebPool-" + poolNumber.getAndIncrement() + "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            if (!t.isDaemon()) {
                t.setDaemon(true);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    
    private static final class Bom {
        
        private String encoding;
        private  byte[] bom;
        
        public Bom(String encoding, byte[] bom) {
            this.encoding = encoding;
            this.bom = bom;
        }
        
        public String getEncoding() {
            return encoding;
        }
        
        public boolean match(byte[] data) {
            for (int i = 0; i < bom.length; i++) {
                if (bom[i] != data[i]) {
                    return false;
                }
            }
            
            return true;
        }
        
        public boolean match(ByteBuffer buffer) {
            if (buffer.remaining() < bom.length) {
                return false;
            }
            
            int pos = buffer.position();
            try {
                for (int i = 0; i < bom.length; i++) {
                    if (bom[i] != buffer.get()) {
                        return false;
                    }
                }
                
                return true;    
            } finally {
                buffer.position(pos);
            }
        }
    }
}
