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
package org.xlightweb.server;




import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.xlightweb.Context;
import org.xlightweb.HttpUtils;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IWebHandler;
import org.xlightweb.RequestHandlerChain;
import org.xsocket.DataConverter;
import org.xsocket.IntrospectionBasedDynamicMBean;
import org.xsocket.Resource;
import org.xsocket.connection.IServer;




/** 
 * A HTTP utility class
 *
 * @author grro@xlightweb.org
 */
final class ServerUtils {
	
	private static final Logger LOG = Logger.getLogger(ServerUtils.class.getName());
	
	
	private static Map<String, String> mimeTypeMap;
	private static String componentInfo;

	
	
	private ServerUtils() { }
	
	

	/**
	 * get the component info
	 * 
	 * @return the component info
	 */
	static String getComponentInfo() {
		if (componentInfo == null) {
			componentInfo = "xLightweb/" + HttpUtils.getImplementationVersion();
		}

		return componentInfo;
	}

	
	
	static ObjectName exportMbean(MBeanServer mbeanServer, ObjectName objectname, Object handler) throws Exception {
			
		String namespace = objectname.getDomain();
		
		if (handler instanceof Context) {
			Context ctx = (Context) handler;
			objectname = new ObjectName(namespace + ":type=HttpContext" + ", name=[" + ctx.getContextPath() + "]");
			
			for (IWebHandler hdl : ctx.getHandlers()) {
				exportMbean(mbeanServer, objectname, hdl);
			}
			
		} else if (handler instanceof RequestHandlerChain) {
			RequestHandlerChain chain = (RequestHandlerChain) handler;
			objectname = new ObjectName(namespace + ":type=HttpRequestHandlerChain" + ", name=" + chain.hashCode());
			
			for (IHttpRequestHandler hdl : chain.getHandlers()) {
				exportMbean(mbeanServer, objectname, hdl);
			}
			
			
		} else {
		    
		    String name = handler.getClass().getSimpleName().trim();
		    if (name.length() < 1) {
		        name = Integer.toString(handler.hashCode());
		    }
			objectname = new ObjectName(namespace + ":type=HttpRequestHandler, name=" + name);
		}
		
		
		mbeanServer.registerMBean(new IntrospectionBasedDynamicMBean(handler), objectname);
		
		return objectname;
	}
	
	
	
	
	/**
	 * get the mime type file to extension map 
	 *
	 * @return the mime type file to extension map
	 */
	synchronized static Map<String, String> getMimeTypeMapping() {
		
		if (mimeTypeMap == null) {
			
			Map<String, String> map = new HashMap<String, String>();
			mimeTypeMap = Collections.unmodifiableMap(map); 
			
			InputStreamReader isr = null;
			LineNumberReader lnr = null;
			try {
				isr = new InputStreamReader(ServerUtils.class.getResourceAsStream("/org/xsocket/connection/http/server/mime.types"));
				if (isr != null) {
					lnr = new LineNumberReader(isr);
					String line = null;
					do {
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
						}
					} while (line != null);
		
					lnr.close();
				}
			} catch (IOException ioe) { 
				if (LOG.isLoggable(Level.FINE)) {
					LOG.fine("Error occured by reding version file " + ioe.toString());
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


	/**
	 * inject a protocol adapter
	 *
	 * @param handler   the handler
	 * @param adapter   the adapter to inject
	 */
	static void injectProtocolAdapter(Object handler, HttpProtocolAdapter adapter) {
		Field[] fields = handler.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (field.isAnnotationPresent(Resource.class)) {
				Resource res = field.getAnnotation(Resource.class);
				if ((field.getType() == HttpProtocolAdapter.class) || (res.type() == HttpProtocolAdapter.class)) {
					field.setAccessible(true);
					try {
						field.set(handler, adapter);
					} catch (IllegalAccessException iae) {
						LOG.warning("could not set HandlerContext for attribute " + field.getName() + ". Reason " + DataConverter.toString(iae));
					}
				}
			}
		}
	}
}
