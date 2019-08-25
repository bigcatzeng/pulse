/*
 *  Copyright (c) xlightweb.org, 2006 - 2009. All rights reserved.
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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;






/**
 *
 * @author grro@xlightweb.org
 */
public final class RESTHandlerTest {
			
	
	@Test 
	public void testSimple() throws Exception {
		
		System.setProperty("org.xlightweb.showDetailedError", "true");
		
		Context rootCtx = new Context("/mailsystem");
		rootCtx.addHandler("/quota/*", new QuotaResourceHandler());
		
	    HttpServer server = new HttpServer(rootCtx);
	    server.start();

	    HttpClient httpClient = new HttpClient();
	    
	    
	    
	    // insert resource 
	    IHttpRequest request = new PutRequest("http://localhost:" + server.getLocalPort() + "/mailsystem/quota/44545/", new NameValuePair("resource", "storage"), new NameValuePair("limitSoft", "5000"), new NameValuePair("limitHard", "6500"), new NameValuePair("usage", "0"));
	    request.setHeader("Accept", "*/*");
	    IHttpResponse response = httpClient.call(request);
	    Assert.assertEquals(200, response.getStatus());

	    
	    // insert another resource 
	    request = new PutRequest("http://localhost:" + server.getLocalPort() + "/mailsystem/quota/44549/", "application/json", "{\"limitHard\":67500,\"limitSoft\":2200,\"resource\":\"storage\",\"usage\":445}");
	    request.setHeader("Accept", "*/*");
	    response = httpClient.call(request);
	    Assert.assertEquals(200, response.getStatus());

	    
	    // request complete resource 
	    request = new GetRequest("http://localhost:" + server.getLocalPort() + "/mailsystem/quota/44545/");
	    request.setHeader("Accept", "application/*");
	    response = httpClient.call(request);
	    Assert.assertEquals(200, response.getStatus());


	    // request complete resource 
	    request = new GetRequest("http://localhost:" + server.getLocalPort() + "/mailsystem/quota/44545/");
	    request.setHeader("Accept", "application/x-www-form-urlencoded");
	    response = httpClient.call(request);
	    Assert.assertEquals(200, response.getStatus());

	    
	    // request complete resource as JSON 
	    request = new GetRequest("http://localhost:" + server.getLocalPort() + "/mailsystem/quota/44545/");
	    request.setHeader("Accept", "application/json");
	    response = httpClient.call(request);
	    Assert.assertEquals(200, response.getStatus());

	    
	    // request a several attributes 
	    request = new GetRequest("http://localhost:" + server.getLocalPort() + "/mailsystem/quota/44545?attribute=limitSoft&attribute=usage");
	    request.setHeader("Accept", "application/x-www-form-urlencoded");
		response = httpClient.call(request);
		Assert.assertEquals(200, response.getStatus());
    

		// simulate browser request
	    request = new GetRequest("http://localhost:" + server.getLocalPort() + "/mailsystem/quota/44545");
	    request.setHeader("KeepAlive", "300");
	    request.setUserAgent("Mozilla/5.0 (Windows; U; Windows NT 5.1; de; rv:1.9.0.5) Gecko/2008120122 Firefox/3.0.5");
	    request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
	    request.setHeader("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
	    request.setHeader("Accept-Encoding", "gzip,deflate");
	    request.setHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		response = httpClient.call(request);
		Assert.assertEquals(200, response.getStatus());
		
		
	    // delete resource 
	    request = new DeleteRequest("http://localhost:" + server.getLocalPort() + "/mailsystem/quota/44545/");
	    response = httpClient.call(request);
	    Assert.assertEquals(200, response.getStatus());

	    
	    // request complete (deleted) resource 
	    request = new GetRequest("http://localhost:" + server.getLocalPort() + "/mailsystem/quota/44545/");
	    request.setHeader("Accept", "application/x-www-form-urlencoded");
	    response = httpClient.call(request);
	    Assert.assertEquals(404, response.getStatus());

	    
	    httpClient.close();
	    server.close();
	}
	
	
	@Test 
	public void testSimple2() throws Exception {
		System.setProperty("org.xlightweb.showDetailedError", "true");
		
		Context rootCtx = new Context("/mailsystem");
		rootCtx.addHandler("/quota/*", new QuotaResourceHandler());
		
	    HttpServer server = new HttpServer(rootCtx);
	    server.start();

	    HttpClient httpClient = new HttpClient();
	    
	    
	    
	    // insert resource 
	    IHttpRequest request = new PutRequest("http://localhost:" + server.getLocalPort() + "/mailsystem/quota/44545/", new NameValuePair("resource", "storage"), new NameValuePair("limitSoft", "5000"), new NameValuePair("limitHard", "6500"), new NameValuePair("usage", "0"));
	    request.setHeader("Accept", "*/*");
	    IHttpResponse response = httpClient.call(request);
	    Assert.assertEquals(200, response.getStatus());

		// simulate browser request
	    request = new GetRequest("http://localhost:" + server.getLocalPort() + "/mailsystem/quota/44545");
	    request.setHeader("KeepAlive", "300");
	    request.setUserAgent("Mozilla/5.0 (Windows; U; Windows NT 5.1; de; rv:1.9.0.5) Gecko/2008120122 Firefox/3.0.5");
	    request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
	    request.setHeader("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
	    request.setHeader("Accept-Encoding", "gzip,deflate");
	    request.setHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		response = httpClient.call(request);
		Assert.assertEquals(200, response.getStatus());
		

	    
	    httpClient.close();
	    server.close();
	}	
	
	
	
	private static final class QuotaResourceHandler extends HttpRequestHandler {
		
		private final Map<String, Quota> quotas = new HashMap<String, Quota>();

		
		@Override
		@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
		public void doPut(IHttpExchange exchange) throws IOException, BadMessageException {
			IHttpRequest request = exchange.getRequest();
			
			String id = request.getPathInfo(true);
			
			try {
				Quota quota = Quota.parse(request.getBody().readString(), new ContentType(request.getContentType()));
				quotas.put(id, quota);

				IHttpResponse response = newResponse(request.getAccept(), quota);
				response.setHeader("Location", request.getRequestUrl().toExternalForm());
				exchange.send(response);
			
			} catch (UnsupportedMimeTypeException use) {
				exchange.sendError(415, "Unsupported Media Type");				
			}
		}
		
	
		@Override
		public void doGet(IHttpExchange exchange) throws IOException, BadMessageException {
			IHttpRequest request = exchange.getRequest();
			 
			String id = request.getPathInfo(true);
			Quota quota = quotas.get(id);
			
			if (quota == null) {
				exchange.sendError(404, "Not found (quota " + id + " does not exist");
				
			} else {
				IHttpResponse response = newResponse(request.getAccept(), quota, request.getParameterValues("attribute"));
				exchange.send(response);
			}
		}
		
		
		
		@Override
		public void doDelete(IHttpExchange exchange) throws IOException, BadMessageException {
			IHttpRequest request = exchange.getRequest();
			 
			String id = request.getPathInfo(true);
			Quota quota = quotas.get(id);
			
			if (quota == null) {
				exchange.sendError(404, "Not found (quota " + id + " does not exist");
				
			} else {
				quotas.remove(id);
				exchange.send(new HttpResponse(200));				
			}
		}
		
		
		@Override
		@Execution(Execution.NONTHREADED)
		public void doOptions(IHttpExchange exchange) throws IOException, BadMessageException {
			IHttpResponse response = new HttpResponse(200);
			response.setHeader("Allow", "GET, HEAD, OPTIONS, PUT");
			exchange.send(response);
		}
		
		
		

		
		private IHttpResponse newResponse(List<ContentType> contentTypes, Quota quota, String... attributes) throws IOException {

			
			try {
				for (ContentType contentType : contentTypes) {
					if (contentType.getPrimaryType().equals("*")) {
						contentType.setPrimaryType("application");
					}
					if (contentType.getSubType().equals("*")) {
						contentType.setSubType("x-www-form-urlencoded");
					}
					
					String body = quota.serialize(contentType, attributes);
					if (body != null) {
						return new HttpResponse(200, contentType.toString(), body);
					}
				}
				throw new UnsupportedMimeTypeException("");
				
			} catch (UnsupportedMimeTypeException ust) {
				return new HttpResponse(415, "Unsupported Media Type");
			}
		}
	}
	
	
	
	
	public static final class Quota {
		
		private Integer limitSoft;
		private Integer limitHard;
		private Integer usage;
		private String resource;

		public Quota() {

		}
		
		public String getResource() {
			return resource;
		}

		public void setResource(String resource) {
			this.resource = resource;
		}

		public Integer getLimitSoft() {
			return limitSoft;
		}

		public void setLimitSoft(Integer limitSoft) {
			this.limitSoft = limitSoft;
		}

		public Integer getLimitHard() {
			return limitHard;
		}

		public void setLimitHard(Integer limitHard) {
			this.limitHard = limitHard;
		}

		public Integer getUsage() {
			return usage;
		}

		public void setUsage(Integer usage) {
			this.usage = usage;
		}
		
		
		
		public static Quota parse(String s, ContentType mediaType) throws UnsupportedMimeTypeException {
			
			if (mediaType.getPrimaryType().equalsIgnoreCase("application")) {
				
				if (mediaType.getSubType().equalsIgnoreCase("x-www-form-urlencoded")) {
					Quota quota = new Quota();
					String[] kvps = s.split("&");
					for (String kvp : kvps) {
						String[] keyValue = kvp.split("=");
						if (keyValue[0].equalsIgnoreCase("limitHard")) {
							quota.setLimitHard(Integer.parseInt(keyValue[1]));
						} else if (keyValue[0].equalsIgnoreCase("limitSoft")) {
							quota.setLimitSoft(Integer.parseInt(keyValue[1]));
						} else if (keyValue[0].equalsIgnoreCase("usage")) {
							quota.setUsage(Integer.parseInt(keyValue[1]));
						} else if (keyValue[0].equalsIgnoreCase("resource")) {
							quota.setResource(decode(keyValue[1]));								
						}
					}
						
					return quota;
					
					
				} else if (mediaType.getSubType().equalsIgnoreCase("json")) {
					return JSONObject.parseObject(s, Quota.class); // JSONObject.toBean(JSONObject.fromObject(s), );
				}
			}
			

			throw new UnsupportedMimeTypeException("unsupported mime type " + mediaType.toString()); 
		}
		
		
		public String serialize(ContentType mimeType, String... attributes) throws IOException {
			
			if (mimeType == null) {
				return null;
			}
			
			if (mimeType.getPrimaryType().equalsIgnoreCase("application")) {
				
				if (mimeType.getSubType().equalsIgnoreCase("json")) {
					return JSONObject.toJSONString(this); //.fromObject(this).toString();
					
				} else if (mimeType.getSubType().equalsIgnoreCase("x-www-form-urlencoded")) {
					if (attributes.length == 0) {
						return "resource=" + resource + "&limitSoft=" + limitSoft + "&limitHard=" + limitHard + "&usage=" + usage;
						
					} else {
						StringBuilder sb = new StringBuilder();
						for (String attribute : attributes) {
							String kvp = null;
							if (attribute.equalsIgnoreCase("usage")) {
								kvp = "usage=" + Integer.toString(usage);
							} else if (attribute.equalsIgnoreCase("limitSoft")) {
								kvp = "limitSoft=" + Integer.toString(limitSoft);
							} else if (attribute.equalsIgnoreCase("limitHard")) {
								kvp = "limitHard=" + Integer.toString(limitHard);
							} else if (attribute.equalsIgnoreCase("resource")) {
								kvp = "resource=" + encode(Integer.toString(limitHard));
							}
							
							if (kvp != null) {
								if (sb.length() != 0) {
									sb.append("&");
								} 
								sb.append(kvp);
							}
						}
						
						return sb.toString();
					}
					
				} 
				
				
			} 
			
			return null;
		}
		
		
		private static String encode(String s) {
			if (s == null) {
				return null;
			}
			
			try {
				return URLEncoder.encode(s, "UTF-8");
			} catch (UnsupportedEncodingException usee) {
				throw new RuntimeException(usee);
			}
		}
		
		
		private static String decode(String s) {
			if (s == null) {
				return null;
			}
			
			try {
				return URLDecoder.decode(s, "UTF-8");
			} catch (UnsupportedEncodingException usee) {
				throw new RuntimeException(usee);
			}
		}
	}
	
	
	private static final class UnsupportedMimeTypeException extends Exception {
		
		private static final long serialVersionUID = 7902658899514661293L;

		public UnsupportedMimeTypeException(String msg) {
			super(msg);
		}
	}
	
	
}
