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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.connection.IConnection.FlushMode;




/**
*
* @author grro@xlightweb.org
*/
public final class ChatExample  {
	
	
	public static void main(String... args) throws IOException {
		Context rootCtx = new Context("");
		rootCtx.addHandler("/chat/service/*", new ChatServerHandler());
		
		String basePath = ChatExample.class.getResource("").getFile();
		rootCtx.addHandler("/chat/*", new FileServiceRequestHandler(basePath, true));
		
		HttpServer server = new HttpServer(9090, rootCtx);
		server.run();
	}

	
	public static final class MessageBroker {
		
		private static final Map<String, MessageBroker> BROKERS = new HashMap<String, MessageBroker>();
		
		private String topic;
		private final Set<MessageSender> receivers = new HashSet<MessageSender>(); 
		
		public static synchronized MessageBroker getBroker(String topic) {
			MessageBroker broker = BROKERS.get(topic);
			if (broker == null) {
				broker = new MessageBroker(topic);
				BROKERS.put(topic, broker);
			}
			
			return broker;
		}
		
		
		private MessageBroker(String topic) {
			this.topic = topic;
		}
		
		
		public synchronized void addMessage(String user, String msg) throws IOException {
			
			Set<MessageSender> recevierToRemove = null; 
			
			for (MessageSender receiver : receivers) {
				try {
					receiver.sendMessage("[" + user + "] " + msg + "<br/>");
				} catch (IOException ioe) {
					if (recevierToRemove == null) {
						recevierToRemove = new HashSet<MessageSender>();
					}
					recevierToRemove.add(receiver);
 				}
			}
			
			
			if (recevierToRemove != null) {
				for (MessageSender receiver : recevierToRemove) {
					receivers.remove(receiver);
				}
			}
		}
		
		public synchronized void registerReceiver(String user, MessageSender receiver) throws IOException {
			receivers.add(receiver);
			addMessage(user, "entered chat");
		}
		
		public synchronized void deregisterReceiver(String user, MessageSender receiver) throws IOException {
			receivers.add(receiver);
			addMessage(user, "left chat");
		}
	}
	
	

	
	
	public static final class ChatServerHandler implements IHttpRequestHandler {
		
		@Execution(Execution.NONTHREADED)
		@InvokeOn(InvokeOn.MESSAGE_RECEIVED)
		public void onRequest(IHttpExchange exchange) throws IOException {
			IHttpRequest request = exchange.getRequest();
			String requestURI = request.getRequestURI();
			
			String[] parts = requestURI.split("/");
			String user = parts[parts.length - 1];
			String operation = parts[parts.length - 2];
			String channel = parts[parts.length - 3];
				
			if (operation.equals("receiveMessages")) {
				int type = Integer.parseInt(request.getParameter("cometType"));  // pi framework
				
				String contentType = null;
				if (type == 2) {
					contentType = "application/x-dom-event-stream";

				} else {
					contentType = "text/html";
				}
				BodyDataSink outChannel = exchange.send(new HttpResponseHeader(200, contentType));
				outChannel.setFlushmode(FlushMode.ASYNC);
				
				MessageBroker.getBroker(channel).registerReceiver(user, new MessageSender(outChannel, user, type));		

				
			} else if (operation.equals("addMessage")) {
				MessageBroker.getBroker(channel).addMessage(user, request.getBody().readString());
				exchange.send(new HttpResponse(200,  "text/html", "OK"));				

			} else {
				exchange.sendError(404);
			}
		}
	}

	
	public static interface IMessageSender {
	
		void sendMessage(String message) throws IOException;		
	}
	
	
	public static final class MessageSender implements IMessageSender {
	
		private String user = null;
		private int type = 1;
		private BodyDataSink bodyDataSink = null;
		
		public MessageSender(BodyDataSink bodyDataSink, String user, int type) {
			this.bodyDataSink = bodyDataSink;
			this.user = user;
			this.type = type;
		}
		
		public void sendMessage(String message) throws IOException {
			
			try {
				if (type == 1) {
					bodyDataSink.write("<end />" + message);
				
				} else if (type == 3) {
					bodyDataSink.write("<script>parent._cometObject.event.push(\"" + message + "\")</script>");
				}
				
				bodyDataSink.flush();
				
			} catch (IOException ioe) {
				bodyDataSink.destroy();
				throw ioe;
			}
		}
	}
	
}