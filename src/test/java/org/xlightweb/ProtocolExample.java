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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;





/**  
*
* @author grro@xlightweb.org
*/
public final class ProtocolExample  {
	

    
    public static void main(String[] args) throws IOException {

        HttpServer server;
        HttpClient httpClient;

        String wsPrefix;
        if ((args.length > 0) && (args[0].equals("ssl"))) {
            server = new HttpServer(0, new NotificationHandler(new Broker(new TopicListener())), SSLTestContextFactory.getSSLContext(), true);
            httpClient = new HttpClient(SSLTestContextFactory.getSSLContext());
            wsPrefix = "wss";
        } else {
            server = new HttpServer(0, new NotificationHandler(new Broker(new TopicListener())));
            httpClient = new HttpClient();
            wsPrefix = "ws";
        }
        
        server.start();
        
        IWebSocketConnection webSocketConnection = httpClient.openWebSocketConnection(wsPrefix + "://localhost:" + server.getLocalPort(), "com.example.mnp");
        
        Event event = new Event("REGISTER", "messagebox/2234/folder/45");
        webSocketConnection.writeMessage(new TextMessage(event.toString()));
        
        for (int i = 0; i < 100; i++) {
            WebSocketMessage msg = webSocketConnection.readMessage();
            event = Event.parse(msg.toString()); 
            System.out.println(event);
        }
        
        httpClient.close();
        server.close();
    } 

    
    
    private static final class NotificationHandler implements IWebSocketHandler {
        
        private final Broker broker;
        
        public NotificationHandler(Broker broker) {
            this.broker = broker;
        }
        
        public void onConnect(IWebSocketConnection con) throws IOException {
            if (!con.getProtocol().equalsIgnoreCase("tcom.example.mnp")) {
                throw new UnsupportedProtocolException("protocol " + con.getProtocol() + " is not supported");
            }
            
        }
        
        public void onDisconnect(IWebSocketConnection con) throws IOException {
            broker.deregisterAll(con);
        }
        
        public void onMessage(IWebSocketConnection con) throws IOException {
            
            WebSocketMessage msg = con.readMessage();
            Event event = Event.parse(msg.toString());
            
            System.out.println(event);
            if (event.getName().equalsIgnoreCase("REGISTER")) {
                broker.register(event.getTopic(), con);
                
            } else if (event.getName().equalsIgnoreCase("DEREGISTER")) {
                broker.deregister(event.getTopic(), con);
            }
        }
    }
    
    
    public static class Event {
        
        private final String name;
        private final String topic;
        private final String[] params;
        
        public Event(String name, String topic, String... params) {
            this.name = name;
            this.topic = topic;
            this.params = params;
        }

        public String getName() {
            return name;
        }

        public String getTopic() {
            return topic;
        }

        public String[] getParams() {
            return params;
        }
        
        @Override
        public String toString() {
            
            try {
                StringBuilder sb = new StringBuilder(URLEncoder.encode(name, "UTF-8") + ":" + URLEncoder.encode(topic, "UTF-8"));
                for (String param : params) {
                    sb.append(":" + param);
                }
                return sb.toString();
            } catch (UnsupportedEncodingException use) {
                throw new RuntimeException(use);
            }
        }
        
        public static Event parse(String s) throws IOException {

            try {
                String[] parts = s.split(":");
                if (parts.length >= 2) {
                    String[] params = new String[parts.length - 2];
                    System.arraycopy(parts, 2, params, 0, params.length);
                    return new Event(URLDecoder.decode(parts[0], "UTF-8"), URLDecoder.decode(parts[1], "UTF-8"), params);
                } else {
                    throw new IOException("invalid event format " + s); 
                }
            } catch (UnsupportedEncodingException use) {
                throw new RuntimeException(use);
            }
        }
    }
    
    
    public static final class TopicListener implements ITopicListener {
        
        private final Map<String, TimerTask> openTasks = new HashMap<String, TimerTask>();
        private final Timer timer = new Timer(true);
        
        private Broker broker;
        
        public void setBroker(Broker broker) {
            this.broker = broker;
        }
        
        public void onTopicAppears(final String topic) {
            
            TimerTask task = new TimerTask() {
            
                @Override
                public void run() {
                    Set<IWebSocketConnection> cons = broker.retrieve(topic);
                    for (IWebSocketConnection con : cons) {
                        try {
                            Event event = new Event("REVISION_CHANGED", topic, "old=35", "new=65");
                            con.writeMessage(new TextMessage(event.toString()));
                        } catch (IOException ioe) {
                            broker.deregister(topic, con);
                            con.closeQuitly();
                        }
                    }
                }
            };
            
            TimerTask oldTask;
            synchronized (openTasks) {
                oldTask = openTasks.put(topic, task);
            }
            
            if (oldTask != null) {
                oldTask.cancel();
            }
            
            timer.schedule(task, 1000, 1000);
        }
        
        
        public void onTopicDisappears(String topic) {

            TimerTask task;
            synchronized (openTasks) {
                task = openTasks.remove(topic);
            }
            
            if (task != null) {
                task.cancel();
            }
        }
    }

    
    public static final class Broker {

        private final Map<String, Set<IWebSocketConnection>> registered = new HashMap<String, Set<IWebSocketConnection>>();
        
        private final ITopicListener topicListener;
        
        public Broker() {
            this(null);
        }
        
        public Broker(ITopicListener topicListener) {
            this.topicListener = topicListener;
            if (topicListener != null) {
                topicListener.setBroker(this);
            }
        }
        
        
        public synchronized void register(String topic, IWebSocketConnection con) {
            Set<IWebSocketConnection> cons = registered.get(topic);
            if (cons == null) {
                cons = new HashSet<IWebSocketConnection>();
                registered.put(topic, cons);
                
                if (topicListener != null) {
                    topicListener.onTopicAppears(topic);
                }
            }
            
            cons.add(con);
        }
        
        public synchronized Set<IWebSocketConnection> retrieve(String topic) {
            Set<IWebSocketConnection> cons = registered.get(topic);
            
            if (cons == null) {
                return new HashSet<IWebSocketConnection>();
            } else {
                return Collections.unmodifiableSet(cons);
            }
        }

        
        public boolean deregister(String topic, IWebSocketConnection con) {
        
            Set<IWebSocketConnection> cons = registered.get(topic);
            if (cons != null) {
                cons.remove(con);
                if (cons.isEmpty()) {
                    registered.remove(topic);

                    if (topicListener != null) {
                        topicListener.onTopicDisappears(topic);
                    }
                }
                return true;
            }
            
            return false;
        }
        
        
        public Set<String> deregisterAll(IWebSocketConnection con) {
            Set<String> topics = new HashSet<String>();
            
            for (String topic : registered.keySet()) {
                if (registered.values().contains(con)) {
                    topics.add(topic);
                }
            }

            for (String topic : topics) {
                deregister(topic, con);
            }
            
            return topics;
        }        
    }
    
    
    public static interface ITopicListener {
        
        void setBroker(Broker broker);
     
        void onTopicAppears(String topic);
        
        void onTopicDisappears(String topic);
    }

}