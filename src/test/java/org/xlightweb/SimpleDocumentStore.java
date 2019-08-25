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


import java.io.FileFilter;
import java.io.IOException;
import java.util.UUID;


import org.xlightweb.NonBlockingBodyDataSource.ITransferResultHandler;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClient.FollowsRedirectMode;
import org.xlightweb.server.HttpServer;
import org.xsocket.ILifeCycle;





/**
*
* @author grro@xlightweb.org
*/
public final class SimpleDocumentStore extends HttpServer {

	public SimpleDocumentStore(int port, String fileBasepath) throws IOException {
		this(port, fileBasepath, Integer.MAX_VALUE);
	}
	
	
	public SimpleDocumentStore(int port, String fileBasepath, int quotaLimit) throws IOException {
		super(port, new RequestHandler(new TestStore(fileBasepath), quotaLimit));
	}
	
	
	
	private static final class RequestHandler extends HttpRequestHandler implements ILifeCycle  {
		
		private HttpClient httpClient;
		
		private final IStore store;
		private final int quotaLimit;
		
		
		
		public RequestHandler(IStore store, int quotaLimit) {
			this.store = store;
			this.quotaLimit = quotaLimit;
		}
		
		public void onInit() {
			httpClient = new HttpClient();
	        httpClient.setFollowsRedirectMode(FollowsRedirectMode.ALL);
			httpClient.setAutoHandleCookies(false);
		}
		
		public void onDestroy() throws IOException {
			httpClient.close();
		}
		
		
		

		@Override
		public void doPost(final IHttpExchange exchange) throws IOException, BadMessageException {
			final IHttpRequest request = exchange.getRequest();
			
			
			// direct upload?
			if (request.hasBody()) {
				if (request.getContentLength() > quotaLimit) {
					throw new BadMessageException(413);
				}
					
				IFileInfo fileInfo = store.newFile(request.getContentType(), request.getIntParameter("cacheExpiresSec", -1));
				request.getNonBlockingBody().transferTo(fileInfo.getFile(), new TransferResultHandler(exchange, fileInfo));
				
				
			// .. no, retrieve the document by the given URI and store it  					
			} else {
				IHttpResponse response = httpClient.call(new GetRequest(request.getParameter("sourceURI")));
				
				if (response.getStatus() != 200) {
					throw new BadMessageException("could not retrieve doc " + request.getParameter("sourceURI"));
					
				} else {
					IFileInfo fileInfo = store.newFile(response.getContentType(), request.getIntParameter("cacheExpiresSec", -1));
					response.getNonBlockingBody().transferTo(fileInfo.getFile(), new TransferResultHandler(exchange, fileInfo));
				}
			}
		}
		
		
		private static final class TransferResultHandler implements ITransferResultHandler {
			
			private final IHttpExchange exchange;
			private final IFileInfo fileInfo;
			
			public TransferResultHandler(IHttpExchange exchange, IFileInfo fileInfo) {
				this.exchange = exchange;
				this.fileInfo = fileInfo;
			}
			 
			public void onComplete() throws IOException {
				IHttpRequest request = exchange.getRequest();
				
				IHttpResponse response = new HttpResponse(201);
				response.setHeader("Location", request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getRequestHandlerPath() + "/"  + fileInfo.getId());
				
				exchange.send(response);		
			}
			
			public void onException(IOException ioe) throws IOException {
				exchange.sendError(ioe);
			}
		}
		
		
		@Override
		public void doPut(final IHttpExchange exchange) throws IOException, BadMessageException {
			IHttpRequest request = exchange.getRequest();
			
			if (request.getContentLength() > quotaLimit) {
				throw new BadMessageException(413);
			}
			
			exchange.sendContinueIfRequested(); 
			
			final String id = HttpUtils.removeSurroundingSlashes(request.getRequestURI());
			final IFileInfo fileInfo = store.replaceFile(id, request.getContentType(), request.getIntParameter("cacheExpiresSec", -1));

			
			ITransferResultHandler hdl = new ITransferResultHandler() {
				 
				public void onComplete() throws IOException {
					IHttpResponse response = new HttpResponse(204);
					exchange.send(response);		
				}
				
				public void onException(IOException ioe) throws IOException {
					exchange.sendError(ioe);
				}
			};
			
			request.getNonBlockingBody().transferTo(fileInfo.getFile(), hdl);
		}
		
		

		@Override
		public void doGet(IHttpExchange exchange) throws IOException, BadMessageException {
			IHttpRequest request = exchange.getRequest();
			String id = HttpUtils.removeSurroundingSlashes(request.getRequestURI());
			IFileInfo fileInfo = store.retrieveFile(id);
		
			// file found?
			if (fileInfo != null) {
				HttpResponse response;
				
				// conditional GET?
				String lastModifiedSince = request.getHeader("If-Modified-Since");
				if ((lastModifiedSince != null) && !HttpUtils.isAfter(lastModifiedSince, fileInfo.lastModified())) {
					response = new HttpResponse(304);
				
				// standard GET
				} else {
					response = new HttpResponse(200, fileInfo.getMimeType(), fileInfo.getFile(), request.getHeader("Range"));
					
					// is response (expire-based) cacheable?
					if (fileInfo.getCacheExpireTimeSec() > 0) {
						response.setExpireHeaders(fileInfo.getCacheExpireTimeSec());
					}
				}

				response.setDate(System.currentTimeMillis());
				response.setLastModifiedHeader(fileInfo.lastModified());
				
				exchange.send(response);

				
		    // .. no
			} else {
				exchange.sendError(404);
			}
		}
		
		
		@Override
		public void doDelete(IHttpExchange exchange) throws IOException, BadMessageException {
			IHttpRequest request = exchange.getRequest();
			
			String id = HttpUtils.removeSurroundingSlashes(request.getRequestURI());
			store.deleteFile(id);
			
			exchange.send(new HttpResponse(204));
		}
	}	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public static interface IStore {
		
		IFileInfo newFile(String mimeType, int cacheExpireTimeSec) throws IOException;
		
		IFileInfo replaceFile(String id, String mimeType, int cacheExpireTimeSec) throws IOException;
		
		IFileInfo retrieveFile(String id) throws IOException;
		
		boolean deleteFile(String id) throws IOException;
	}
	
	
	
	
	public static interface IFileInfo {
		
		int getCacheExpireTimeSec();
		
		File getFile();
		
		String getMimeType();
		
		String getId();
		
		long lastModified();
	}
	
	
	public static final class TestStore implements IStore {
		
		
		
		private final String fileBasepath;
		
		public TestStore(String fileBasepath) {
			this.fileBasepath = fileBasepath; 
			
			if (!new File(fileBasepath).exists()) {
				System.out.println("creating dir "+ fileBasepath);
				new File(fileBasepath).mkdirs();
			}
		}

		public IFileInfo newFile(String mimeType, int cacheExpireTime) throws IOException {
			return newFile(UUID.randomUUID().toString(), mimeType, cacheExpireTime);
		}

		
		public IFileInfo newFile(String id, String mimeType, int cacheExpireTime) throws IOException {
			File file = new File(fileBasepath + File.separator + id + "." + encode(mimeType) + "." + encode(Integer.toString(cacheExpireTime)));
			file.createNewFile();

			return new FileInfo(file, mimeType, id, file.lastModified(), cacheExpireTime);
		}

		
		public IFileInfo replaceFile(String id, String mimeType, int cacheExpireTimeSec) throws IOException {
			
			// HACK!! not transactional
			retrieveFile(id).getFile().delete();
			return newFile(id, mimeType, cacheExpireTimeSec);
		}
		

		public IFileInfo retrieveFile(final String id) throws IOException {

			FileFilter filter = new FileFilter() {
				public boolean accept(File file) {
					return file.getName().startsWith(id + ".");
				}
			};
			
			
			File[] files = new File(fileBasepath).listFiles(filter);
			if (files.length == 0) {
				return null;
			} else {
				String[] parts = files[0].getName().split("\\.");
				return new FileInfo(files[0], decode(parts[1]), parts[0], files[0].lastModified(), Integer.parseInt(decode(parts[2])));
			}
		}
		
		
		public boolean deleteFile(String id) throws IOException {
			return retrieveFile(id).getFile().delete();
		}
		
		
		private String encode(String txt) throws IOException {
			return new String(HttpUtils.encodeBase64(txt.getBytes("UTF-8")), "UTF-8");
		}
		
		private String decode(String txt) throws IOException {
			return new String(HttpUtils.decodeBase64(txt.getBytes("UTF-8")), "UTF-8");
		}
	}

	public static final class FileInfo implements IFileInfo {
		
		private final File file;
		private final int cacheExpireTimeSec;
		private final String mimeType;
		private final String id;
		private final long lastModified;
		
		
		public FileInfo(File file, String mimeType, String id, long lastModified, int cacheExpireTimeSec) {
			this.file = file;
			this.mimeType = mimeType;
			this.id = id;
			this.lastModified = lastModified;
			this.cacheExpireTimeSec = cacheExpireTimeSec;
		}
		
		public int getCacheExpireTimeSec() {
			return cacheExpireTimeSec;
		}
		
		public File getFile() {
			return file;
		}
		
		public String getMimeType() {
			return mimeType;
		}
		
		public String getId() {
			return id;
		}
		
		public long lastModified() {
			return lastModified;
		}
	}

	
}