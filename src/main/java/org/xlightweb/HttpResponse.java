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

import java.io.File;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xsocket.DataConverter;





/**
 * http response
 *
 * @author grro@xlightweb.org
 */
public class HttpResponse extends AbstractHttpMessage implements IHttpResponse {
    
    private static final Logger LOG = Logger.getLogger(HttpResponse.class.getName());
    
	
    private final boolean isSimpleResponse;


	/**
	 * constructor 
	 * 
	 * @param responseHeader   the response header
	 */
	public HttpResponse(IHttpResponseHeader responseHeader) {
		super(responseHeader);
		isSimpleResponse = false;
	}	
	
	
	 
    /**
     * constructor 
     * 
     * @param status       the status 
     */
    public HttpResponse(int status)  {
        this(new HttpResponseHeader(status));
    }   

	
    

    /**
     * constructor 
     * 
     * @param exception  the exception
     */
    HttpResponse(Exception exception) throws IOException {
        this(500, "text/plain", DataConverter.toString(exception));
    }   
    
    
    /**
     * constructor 
     * 
     * @param responseHeader      the response header
     * @param bodyDataSource      the response body
     * 
     * @throws IOException if an io exception occurs
     */
    HttpResponse(IHttpResponseHeader responseHeader, NonBlockingBodyDataSource bodyDataSource, boolean isSimpleResponse) throws IOException {
        super(responseHeader);
        this.isSimpleResponse = isSimpleResponse;
        
        if ((bodyDataSource != null) && (bodyDataSource.available() != -1)) {
            setBody(bodyDataSource);
        } else {
            setContentLength(0);
        }
    }   
    
    
    /**
     * constructor 
     * 
     * @param responseHeader      the response header
     * @param bodyDataSource      the response body
     * @param compress            true, if the data should be compressed
     * 
     * @throws IOException if an io exception occurs
     */	 
    public HttpResponse(IHttpResponseHeader responseHeader, ByteBuffer[] body, boolean compress) throws IOException {
        this(responseHeader);
        setBody(responseHeader, body, compress);
    }

    
    
	
	/**
	 * constructor 
	 * 
	 * @param responseHeader      the response header
	 * @param bodyDataSource      the response body
	 * 
	 * @throws IOException if an io exception occurs
	 */
	public HttpResponse(IHttpResponseHeader responseHeader, NonBlockingBodyDataSource bodyDataSource) throws IOException {
	    this(responseHeader, bodyDataSource, false);
	}	

	
   
	
	
    
    /**
     * constructor 
     * 
     * @param status         the status 
     * @param contentType    the content type
     * @param contentLength  the content length
     * @param body           the body
     * @throws IOException if an io exception occurs 
     */
    public HttpResponse(int status, String contentType, int contentLength, BodyDataSource body) throws IOException {
        this(status, contentType, contentLength, body.getUnderliyingBodyDataSource());
    }   

	

    /**
     * constructor 
     * 
     * @param status         the status 
     * @param contentType    the content type
     * @param contentLength  the content length
     * @param body           the body
     * @throws IOException if an io exception occurs 
     */
    public HttpResponse(int status, String contentType, int contentLength, NonBlockingBodyDataSource body) throws IOException {
        this(new HttpResponseHeader(status, contentType), body);
        setContentLength(contentLength);
    }   
    
	
	
	/**
	 * constructor 
	 * 
	 * @param responseHeader      the response header
	 * @param bodyDataSource      the response body
	 * 
	 * @throws IOException if an io exception occurs 
	 */
	public HttpResponse(String contentType, NonBlockingBodyDataSource bodyDataSource) throws IOException {
		this(newResponseHeader(200, contentType, bodyDataSource), bodyDataSource);
	}	

  
    	
	
	/**
	 * constructor. The status will be set to 200 
	 * 
	 * @param responseHeader      the response header
	 * @param bodyDataSource      the response body
	 * 
	 * @throws IOException if an io exception occurs 
	 */
	public HttpResponse(String contentType, BodyDataSource bodyDataSource) throws IOException {
		this(new HttpResponseHeader(200, contentType), bodyDataSource.getUnderliyingBodyDataSource());
	}	
	

	/**
	 * constructor. The status will be set to 200
	 * 
	 * @param contentType    the content type 
	 * @param body           the body
	 * @throws IOException if an io exception occurs 
	 */
	public HttpResponse(String contentType, String body) throws IOException {
		this(200, contentType, body);
	}	

	
	
	/**
	 * constructor. The status will be set to 200
	 * 
	 * @param body           the body
	 * @throws IOException if an io exception occurs 
	 */
	public HttpResponse(String body) throws IOException {
		this(new HttpResponseHeader(200), body);
	}	


	/**
	 * constructor. The status will be set to 200
	 * 
	 * @param file           the file
	 * @throws IOException if an io exception occurs 
	 */
	public HttpResponse(File file) throws IOException {
		this(200, file);
	}		
	
	
	/**
	 * constructor 
	 * @param status     the status
	 * @param file       the file
     * @throws IOException if an io exception occurs
	 */
    public HttpResponse(int status, File file) throws IOException { 
        this(status, HttpUtils.resolveContentTypeByFileExtension(file), file);
    }       
    

	/**
	 * constructor 
	 * @param status      the status
	 * @param contentType the contentType
	 * @param file        the file
     * @throws IOException if an io exception occurs
	 */
    public HttpResponse(int status, String contentType, File file) throws IOException { 
        this(new HttpResponseHeader(status, contentType), file);
    }       

    

	/**
	 * constructor 
	 * @param status      the status
	 * @param contentType the contentType
	 * @param file        the file
     * @throws IOException if an io exception occurs
	 */
    public HttpResponse(int status, String contentType, File file, String range) throws IOException {
    	this(new HttpResponseHeader(status, contentType), file, range);
    }       


	/**
	 * constructor 
	 * @param status      the status
	 * @param nvp         he name value pairs
     * @throws IOException if an io exception occurs
	 */
    public HttpResponse(int status, NameValuePair... nvp) throws IOException {
    	this(status, "application/x-www-form-urlencoded; charset=utf-8", new MultivalueMap("utf-8", nvp).toString());
    }       
    
    
    /**
     * constructor 
     * @param header     the response header
     * @param file       the file
     * @throws IOException if an io exception occurs
     */
    public HttpResponse(IHttpResponseHeader header, File file) throws IOException {
        this(header, file, null);
    }       


    
    
    
   
    /**
     * constructor 
     * 
     * @param header        the header
     * @param file          the file 
     * @param range         the range
     * @throws IOException if an exception occurs
     */
    public HttpResponse(IHttpResponseHeader header, File file, String range) throws IOException { 
        this(header);

    	header.setHeader("Accept-Ranges", "bytes");
        
        String contentType = HttpUtils.resolveContentTypeByFileExtension(file);
        if (contentType != null) {
            header.setContentType(contentType);
        }	

    	
        if ((range == null) || (range.split(",").length == 0)) {
        	setContentLength((int) file.length());
        	setBody(new FileDataSource(header, HttpUtils.newMultimodeExecutor(), file, range));
        	
        	
        } else {
        	if (!range.toLowerCase().startsWith("bytes=")) {
        		throw new BadMessageException("invalid range header " + range);
        	} 
        	range = range.substring("bytes=".length(), range.length());
        	
        	String[] ranges = range.split(",");
        	
        	if (ranges.length == 1) {
        		header.setStatus(206);
        		header.setReason(HttpUtils.getReason(header.getStatus()));
        		
        		int length = (int) file.length();
        		int[] positions = HttpUtils.computeFromRangePosition(range, length);
        		header.setHeader("Content-range", "bytes " + positions[0] + "-" + positions[1] + "/" + length);
                	
        		length = positions[1] - positions[0] + 1;
        		
        		setContentLength(length);
                setBody(new FileDataSource(header, HttpUtils.newMultimodeExecutor(), file, ranges[0]));
        		
        	} else {
        		header.setStatus(206);
        		header.setReason(HttpUtils.getReason(header.getStatus()));

        		String boundary = UUID.randomUUID().toString();
        		setContentType("multipart/byteranges; boundary=" + boundary);
        		setBody(new MultipartByteRangeFileDataSource(header, HttpUtils.newMultimodeExecutor(), file, ranges, boundary));
        	}
        }
    }      
    
	
	/**
	 * constructor 
	 * 
	 * @param status       the status 
	 * @param contentType  the content type
	 * @param body         the body
	 * @throws IOException if an io exception occurs 
	 */
	public HttpResponse(int status, String contentType, String body) throws IOException {
		this(new HttpResponseHeader(status, contentType), body);
	}	


	
	/**
	 * constructor 
	 * 
	 * @param status       the status 
	 * @param contentType  the content type
	 * @param body         the body
	 * @param compress     true, if the data should be compressed
	 * @throws IOException if an io exception occurs 
	 */
	public HttpResponse(int status, String contentType, String body, boolean compress) throws IOException {
		this(new HttpResponseHeader(status, contentType), body, compress);
	}	

	/**
	 * constructor 
	 * 
	 * @param status       the status 
	 * @param body         the body
	 * @throws IOException if an io exception occurs 
	 */
	public HttpResponse(int status, String body) throws IOException {
		this(new HttpResponseHeader(status), new ByteBuffer[] { DataConverter.toByteBuffer(body, IHttpMessageHeader.DEFAULT_ENCODING)});
	}	

	/**
	 * constructor. The status will be set to 200
	 *  
	 * @param contentType    the content type
	 * @param body           the body
	 * @throws IOException if an io exception occurs 
	 */
	public HttpResponse(String contentType, byte[] body) throws IOException {
		this(200, contentType, body);
	}	



	
	
	/**
	 * constructor 
	 * 
	 * @param status         the status 
	 * @param contentType    the content type
	 * @param body           the body
	 * @throws IOException if an io exception occurs 
	 */
	public HttpResponse(int status, String contentType, NonBlockingBodyDataSource body) throws IOException {
	    this(newResponseHeader(status, contentType, body), body);
	}	
	
	
	private static HttpResponseHeader newResponseHeader(int status, String contentType, NonBlockingBodyDataSource body) throws IOException {
	    
	    HttpResponseHeader responseHeader;
	    
	    if (body.isComplete()) {
	        responseHeader = new HttpResponseHeader(status, contentType);
	        responseHeader.setContentLength(body.available());
	        
	    } else {
	        if (LOG.isLoggable(Level.FINE)) {
	            LOG.fine("body is not complete. Set transfer-enncoding header with chunked");
	        }
	        responseHeader = new HttpResponseHeader(status, contentType);
            responseHeader.setHeader("Transfer-encoding", "chunked");
	    }
	    
	    return responseHeader;
	}
	
	
	
	/**
	 * constructor 
	 * 
	 * @param status         the status 
	 * @param contentType    the content type
	 * @param body           the body
	 * @throws IOException if an io exception occurs 
	 */
	public HttpResponse(int status, String contentType, BodyDataSource body) throws IOException {
	    this(status, contentType, body.getUnderliyingBodyDataSource());
	}	
	
	
	
	/**
	 * constructor 
	 * 
	 * @param status       the status 
	 * @param contentType  the content type
	 * @param body         the body
	 * @throws IOException if an io exception occurs 
	 */
	public HttpResponse(int status, String contentType, byte[] body) throws IOException {
		this(status, contentType, body, false);
	}	

	
	/**
	 * constructor 
	 * 
	 * @param status       the status 
	 * @param contentType  the content type
	 * @param body         the body
     * @param compress     true, if the data should be compressed 
	 * @throws IOException if an io exception occurs 
	 */
    public HttpResponse(int status, String contentType, byte[] body, boolean compress) throws IOException {
        this(new HttpResponseHeader(status, contentType), new ByteBuffer[] { DataConverter.toByteBuffer(body) }, compress);
    }   

	

	/**
	 * constructor. The status will be set to 200 
	 * 
	 * @param contentType    the content type
	 * @param body           the body
	 * @throws IOException if an io exception occurs  
	 */
	public HttpResponse(String contentType, ByteBuffer[] body) throws IOException {
		this(200, contentType, body);
	}	
	
	
	/**
	 * constructor 
	 * 
	 * @param status       the status 
	 * @param contentType  the content type 
	 * @param body         the body 
	 * @throws IOException if an io exception occurs 
	 */
	public HttpResponse(int status, String contentType, ByteBuffer[] body) throws IOException {
		this(status, contentType, body, HttpUtils.parseEncodingWithDefault(contentType, IHttpMessage.DEFAULT_ENCODING));			
	}	

	
	   
    /**
     * constructor 
     * 
     * 
     * @param status        the status 
     * @param contentType   the content type
     * @param body          the body 
     * @param encoding      the encoding
     * @throws IOException if an io exception occurs
     */
    HttpResponse(int status, String contentType, ByteBuffer[] body, String encoding) throws IOException {
       this(status, contentType, body, encoding, false);
    }   
    
    
	
    /**
     * constructor 
     * 
     * 
     * @param status        the status 
     * @param contentType   the content type
     * @param body          the body 
     * @param encoding      the encoding
     * @param compress      true, if the data should be compressed
     * @throws IOException if an io exception occurs
     */
	HttpResponse(int status, String contentType, ByteBuffer[] body, String encoding, boolean compress) throws IOException {
		this(newResponseHeader(status, contentType, encoding), body, compress);
	}	
	
		
	/**
	 * constructor 
	 * 
	 * @param responseHeader   the response header 
	 * @param body             the body
	 * @throws IOException if an io exception occurs  
	 */
	public HttpResponse(IHttpResponseHeader responseHeader, String body) throws IOException {		
		this(responseHeader, body, false);
	}

	
	   
	/**
	 * constructor 
	 * 
	 * @param responseHeader   the response header 
	 * @param body             the body
	 * @param compress         true, if the data whould be compressed
	 * @throws IOException if an io exception occurs  
	 */
    public HttpResponse(IHttpResponseHeader responseHeader, String body, boolean compress) throws IOException {       
        this(responseHeader, convert(responseHeader, body), compress);
    }

    
    private static ByteBuffer[] convert(IHttpResponseHeader responseHeader, String body) {
        if ((responseHeader.getContentType() != null) && (HttpUtils.isTextMimeType(responseHeader.getContentType()) && (HttpUtils.parseEncoding(responseHeader.getContentType()) == null))) {
            responseHeader.setContentType(responseHeader.getContentType() + "; charset=utf-8");
            
        }
        
        return new ByteBuffer[] { DataConverter.toByteBuffer(body, responseHeader.getCharacterEncoding()) };
    }
	
	
	/**
     * constructor 
     * 
     * @param responseHeader   the response header
     * @param body             the body
     * @throws IOException if an io exception occurs  
     */
    public HttpResponse(IHttpResponseHeader responseHeader, byte[] body) throws IOException {
        this(responseHeader, body, false);     
    }
    
	
	/**
     * constructor 
     * 
     * @param responseHeader   the response header
     * @param body             the body
	 * @param compress         true, if the data should be compressed
     * @throws IOException if an io exception occurs  
     */
	public HttpResponse(IHttpResponseHeader responseHeader, byte[] body, boolean compress) throws IOException {
	    this(responseHeader, new ByteBuffer[] { DataConverter.toByteBuffer(body) }, compress);
	}
	
	
	/**
     * constructor 
     * 
     * @param responseHeader   the response header
     * @param body             the body
     * @throws IOException if an io exception occurs  
     */
    public HttpResponse(IHttpResponseHeader responseHeader, List<ByteBuffer> body) throws IOException {
        this(responseHeader, body, false);
    }
    
	/**
     * constructor 
     * 
     * @param responseHeader   the response header
     * @param body             the body
     * @param compress         true, if the data should be compressed 
     * @throws IOException if an io exception occurs  
     */
    public HttpResponse(IHttpResponseHeader responseHeader, List<ByteBuffer> body, boolean compress) throws IOException {
        this(responseHeader, toArray(body), compress);
    }

    
	
	/**
	 * constructor 
	 * 
	 * @param responseHeader   the response header
	 * @param body             the body
	 * @throws IOException if an io exception occurs  
	 */
	public HttpResponse(IHttpResponseHeader responseHeader, ByteBuffer[] body) throws IOException {
		this(responseHeader, body, false);
	}

	

    
    private static HttpResponseHeader newResponseHeader(int status, String contentType, String encoding) {
        if ((HttpUtils.parseEncoding(contentType) == null) &&  HttpUtils.isContentTypeSupportsCharset(contentType)){
            contentType = contentType + "; charset=" + encoding;
        }

        return new HttpResponseHeader(status, contentType);
    }

    
    private static ByteBuffer[] toArray(List<ByteBuffer> buffers) {
        if (buffers == null) {
            return null;
        } else {
            return buffers.toArray(new ByteBuffer[buffers.size()]);
        }
    }
    
	
	
    
			
	/**
	 * {@inheritDoc}
	 */
	public final int getStatus() {
		return getResponseHeader().getStatus();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public final void setStatus(int status) {
		getResponseHeader().setStatus(status);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public String getServer() {
		return getResponseHeader().getServer();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void setServer(String server) {
		getResponseHeader().setServer(server);
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public void setDate(String date) {
		getResponseHeader().setDate(date);
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public String getDate() {
		return getResponseHeader().getDate();
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public final String getReason() {
		return getResponseHeader().getReason();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void setReason(String reason) {
		getResponseHeader().setReason(reason);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public String getProtocol() {
		return getResponseHeader().getProtocol();
	}

	
	/**
	 * {@inheritDoc}
	 */
	public String getProtocolVersion() {
		return getResponseHeader().getProtocolVersion();
	}
	
	
    /**
     * set the date header of the response
     * 
     * @param timeMillis  the last modified time in millis
     */
    public void setDate(long timeMillis) {
    	setDate(DataConverter.toFormatedRFC822Date(timeMillis));
    }
	
	
    /**
     * set the caching expires headers of a response 
     * 
     * @param expireSec  the expire time or 0 to set no-cache headers
     */
    public void setExpireHeaders(int expireSec) {
    	HttpUtils.setExpireHeaders(this.getResponseHeader(), expireSec);
    }
    
    
    /**
     * set a last modified header of the response
     * 
     * @param timeMillis  the last modified time in millis
     */
    public void setLastModifiedHeader(long timeMillis) {
    	HttpUtils.setLastModifiedHeader(this.getResponseHeader(), timeMillis);
    }
	
	/**
	 * {@inheritDoc}
	 */
	public final void setProtocol(String protocol) {
		getResponseHeader().setProtocol(protocol);
	}
	

	/**
	 * returns the response header
	 *  
	 * @return the response header
	 */
	public final IHttpResponseHeader getResponseHeader() {
		return (IHttpResponseHeader) getPartHeader();
	}
	
	
	
	@Override
	public String toString() {
	    if (isSimpleResponse) {
	        try {
	            return getNonBlockingBody().toString();
	        } catch (IOException ioe) {
	            return "[body can not be printed: " + ioe.toString() + "]"; 
	        }
	    } else {
	        return super.toString();
	    }
	}
}
