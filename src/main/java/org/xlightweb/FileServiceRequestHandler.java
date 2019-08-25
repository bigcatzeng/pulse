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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xsocket.DataConverter;
import org.xsocket.Execution;




/**
 * Handler implementation to handle file requests. If the requested file does not exists, the 
 * handler will forward the request. If the handler is embedded within {@link RequestHandlerChain}
 * the next handler will be called. If no successor exists, a 404 will be returned.
 * 
 * If the flag <i>isShowDirectoryTree</i> is set (default is false) the directory tree will be 
 * printed, in case the request file does not exists. See example:
 * 
 * <pre>
 *  String basePath = "public/files";
 *  IHttpRequestHandler handler = new FileServiceRequestHandler(basePath, true);  // will show the directory tree, if a directory is requested
 *  
 *  IServer server = new HttpServer(8080, handler);
 *  server.start();
 * </pre>  
 *
 *
 * @author grro@xlightweb.org
 */
@Execution(Execution.MULTITHREADED)
public class FileServiceRequestHandler implements IHttpRequestHandler {
	
	private static final Logger LOG = Logger.getLogger(FileServiceRequestHandler.class.getName());
	
	public static final boolean SHOW_DIRECTORY_TREE_DEFAULT = false;
	
	private final File fileBase;
	private final boolean isShowDirectoryTree;
	private final Integer expireSec;
	
	private int countFound = 0;
	private int countNotFound = 0;
	 
	
	/**
	 * constructor
	 * 
	 * @param fileBasepath   the base path
	 * 
	 * @throws FileNotFoundException  if the base path not exists
	 */
	public FileServiceRequestHandler(String fileBasepath) throws FileNotFoundException {
		this(fileBasepath, SHOW_DIRECTORY_TREE_DEFAULT);
	}
	
	

    /**
     * constructor
     * 
     * @param fileBasepath   the base path
     * @param expireSec      the expire time sec, which will be added to each response for caching or <code>null</code> 
     * 
     * @throws FileNotFoundException  if the base path not exists
     */
    public FileServiceRequestHandler(String fileBasepath, Integer expireSec) throws FileNotFoundException {
        this(fileBasepath, expireSec, SHOW_DIRECTORY_TREE_DEFAULT);
    }
	
	/**
	 * constructor
	 * 
	 * @param fileBasepath         the base path
	 * @param isShowDirectoryTree  true, if the directory tree will been shown, if the requests file is a directory
	 * 
	 * @throws FileNotFoundException  if the base path not exists
	 */
	public FileServiceRequestHandler(String fileBasepath, boolean isShowDirectoryTree) throws FileNotFoundException {
	    this(fileBasepath, null, isShowDirectoryTree);
	}

	
	/**
     * constructor
     * 
     * @param fileBasepath         the base path
     * @param expireSec            the expire time sec, which will be added to each response for caching or <code>null</code> 
     * @param isShowDirectoryTree  true, if the directory tree will been shown, if the requests file is a directory
     * 
     * @throws FileNotFoundException  if the base path not exists
     */
    public FileServiceRequestHandler(String fileBasepath, Integer expireSec, boolean isShowDirectoryTree) throws FileNotFoundException {
        this.fileBase = new File(fileBasepath);
        this.expireSec = expireSec;
        this.isShowDirectoryTree = isShowDirectoryTree;
        
        if (!new File(fileBasepath).exists()) {
            throw new FileNotFoundException("base path "+ fileBasepath + " does not exits");
        }
    }

	
	/**
	 * returns if the directory tree should be shown
	 * 
	 * @return true, if the directory tree should be shown
	 */
	boolean isShowDirectoryTree() {
		return isShowDirectoryTree;
	}
	
	
	/**
	 * return the base path
	 * 
	 * @return  the base path
	 */
	String getBasepath() {
		return fileBase.getAbsolutePath();
	}
	
	
	/**
	 * returns the number of found
	 * @return the number of found
	 */
	int getCountFound() {
		return countFound;
	}
	
	/**
	 * returns the number of not found
	 * @return the number of not found
	 */
	int getCountNotFound() {
		return countNotFound;
	}
	
	
	
	public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
		
		IHttpRequest request = exchange.getRequest();
		
		boolean isGET = request.getMethod().equalsIgnoreCase("GET");
		boolean isHEAD = request.getMethod().equalsIgnoreCase("HEAD");
		
		// only GET or HEAD is supported by this handler
		if (isGET || isHEAD) {
    		
    		String requestURI = URLDecoder.decode(request.getRequestURI(), "utf-8");
    		int ctxLength = request.getContextPath().length() + request.getRequestHandlerPath().length();
    		
    		
    		if (requestURI.length() > ctxLength) {
    
    			String filepath = requestURI.substring(ctxLength, requestURI.length());
    			
    			// file defined?
    			if (filepath.length() > 0) {
    				
    				// converting slash to file system's one 
    				filepath = filepath.replaceAll("[/\\\\]+", "\\" + File.separator);
    
    				// create native path
    				String path = fileBase.getAbsolutePath() + filepath;
    				
    				// removing tailing file separator  
    				if (path.endsWith(File.separator)) {
    					path = path.substring(0, path.length() - 1);
    				}
    				
    				File file = new File(path);
    				
    				// does file exits?
    				if (file.exists()) {
    					
    					// is file?
    					if (file.isFile()) {
    						String ifModifiedSinceRequestHeader = request.getHeader("If-Modified-Since");
    
    						if ((ifModifiedSinceRequestHeader != null) && (!HttpUtils.isAfter(ifModifiedSinceRequestHeader, file.lastModified()))) {
    							HttpResponse response = new HttpResponse(304);
    							enhanceFoundResponseHeader((HttpResponseHeader) response.getResponseHeader(), file.lastModified());
    							if (LOG.isLoggable(Level.FINE)) {
    								LOG.fine(filepath + " requested. returning not modified");
    							}
    								
    							exchange.send(response);
    							return;
    						}
    						
    												
    						HttpResponseHeader responseHeader = new HttpResponseHeader(200);
    						enhanceFoundResponseHeader(responseHeader, file.lastModified());
    						if (LOG.isLoggable(Level.FINE)) {
    							LOG.fine(filepath + " requested. returning data");
    						}
    						
    						
    						String range = request.getHeader("Range");
    						
    						HttpResponse response = new HttpResponse(responseHeader, file, range);
    						if (isHEAD) {
    						    response = new HttpResponse(response.getResponseHeader());
    						} 
    						
                            exchange.send(response);

    						countFound++;
    						
    						return;
    						
    					// ... on, it is a directory
    					} else {
    						handleNotFound(exchange, request, file);
    						return;
    					}
    					
    				// file does not exit 
    				} else {
    					handleNotFound(exchange, request, file);
    					return;
    				}
    			}
    			
    		// no file defined
    		} else {
    			exchange.sendError(404, request.getRequestURI() + " not found");
    			return;
    		}
		}
		
		exchange.forward(request, new HttpResponseHandler(exchange));
	}	
	

		
	private void enhanceFoundResponseHeader(HttpResponseHeader responseHeader, long lastModified) {
		responseHeader.setDate(System.currentTimeMillis());
		if (expireSec == null) {
		    responseHeader.setLastModifiedHeader(lastModified);
		} else {
		    responseHeader.setExpireHeaders(expireSec);
		}
	}
	
		
	private void handleNotFound(IHttpExchange exchange, IHttpRequest request, File file) throws IOException {
		
		countNotFound++;
		
		if ((isShowDirectoryTree) &&
			(file.isDirectory() && 
			(fileBase.getAbsolutePath().length() <= file.getAbsolutePath().length()))) { 
				String body = printDirectoryTree(request, file);
				exchange.send(new HttpResponse(200, "text/html", body));
				return;
		} 
		
		exchange.forward(request, new HttpResponseHandler(exchange));
	}
	
	
	private static final class HttpResponseHandler implements IHttpResponseHandler {
		
		private IHttpExchange exchange = null;
		
		public HttpResponseHandler(IHttpExchange exchange) {
			this.exchange = exchange;
		}
		
		public void onResponse(IHttpResponse response) throws IOException {
			exchange.send(response);
		}
		
		public void onException(IOException ioe) {
			exchange.sendError(500);
		}
	}
	
	

	private String printDirectoryTree(IHttpRequest request, File directory) throws IOException {
		StringBuilder sb = new StringBuilder();
		
		String requestResource = directory.getAbsolutePath();
		requestResource = requestResource.substring(fileBase.getAbsolutePath().length(), requestResource.length());
		
		if (request.getRequestHandlerPath().length() > 0) {
			requestResource = request.getRequestHandlerPath() + "/" + requestResource;
		}
		
		if (request.getContextPath().length() > 0) {
			requestResource = request.getContextPath() + "/" + requestResource;
		}
		
		requestResource = requestResource.replace("\\", "/");
		
		
		sb.append("<html>\r\n");
		sb.append("  <!-- This page is auto-generated by xLightweb (http://xLightweb.org) -->\r\n");
		sb.append("  <head>\r\n");
		sb.append("    <title>Index of " + requestResource + "</title>\r\n");
		sb.append("  </head>\r\n");
		sb.append("  <body>\r\n");
		sb.append("    <H1 style=\"color:#0a328c;font-size:1.5em;\">Index of " + requestResource + "</H1>\r\n");
		sb.append("    <p style=\"font-size:0.8em;\">\r\n");

		
		sb.append("    <table border=\"0\" style=\"color:#0a328c;font-size:1.0em;\">\r\n");


		
		for (File file : directory.listFiles()) {
			sb.append("      <tr>");
			
			sb.append("        <td align=\"right\">");
			
			if (file.isDirectory()) {
				sb.append("[DIR]");
			} else {
				sb.append("[TXT]");
			}
			
			sb.append("        </td>\r\n");

			
			sb.append("        <td>");
			
			
			sb.append("<a href=");
			
			String[] parts = requestResource.split("/");
			if (parts.length > 0) {
				sb.append(URLEncoder.encode(parts[parts.length - 1], "utf-8") + "/");
			}
			
			sb.append(URLEncoder.encode(file.getName(), "utf-8") + "> " + file.getName() + "</a>");
			sb.append("        </td>\r\n");
			
			sb.append("        <td>");
			sb.append(DataConverter.toFormatedDate(file.lastModified()));
			sb.append("        </td>\r\n");
			
			sb.append("        <td align=\"right\">");
			if (!file.isDirectory()) {
				sb.append(DataConverter.toFormatedBytesSize(file.length()));
			} else {
				sb.append("-");
			}
			sb.append("        </td>\r\n");
			
			sb.append("      </tr>");
		}
		
		sb.append("    </table>\r\n");
		
		sb.append("    </p>\r\n");
		sb.append("    <p style=\"font-size:0.8em;\">" + new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z").format(new Date()) + "    xLightweb (" + 
				  HttpUtils.getImplementationVersion() + ") at " + request.getServerName() + 
				  " Port " + request.getServerPort() + "</p>\r\n");
		sb.append("  </body>\r\n");
		sb.append("</html>\r\n");
		return sb.toString(); 
	}	
}
