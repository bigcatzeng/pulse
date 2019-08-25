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


import java.io.Closeable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;


import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.IDisconnectHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.Server;



/**
*
* @author grro@xlightweb.org
*/
public final class SimpleDualPortServer implements Runnable, Closeable {

    private final Server httpServer;
    private final Server tcpServer;

    
    public static void main(String[] args) throws IOException {
        SimpleDualPortServer server = new SimpleDualPortServer(Integer.parseInt(args[0]), Integer.parseInt(args[0]));
        server.run();
    }

    
    public SimpleDualPortServer(int httpListenport, int tcpListenport) throws IOException {
        Registry registry = new Registry();
        
        httpServer = new HttpServer(httpListenport, new HttpRequestHandler(registry));
        tcpServer = new Server(tcpListenport, new TcpConnectHandler(registry));
    }


    public void run() {
        try {
            httpServer.start();
            tcpServer.run();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    
    public void close() throws IOException {
        httpServer.close();
        tcpServer.close();
    }
    
    
    
    public int getHttpServerListenPort() {
        return httpServer.getLocalPort();
    }
    
    public int getTcpServerListenPort() {
        return tcpServer.getLocalPort();
    }
    
    
    private static final class HttpRequestHandler implements IHttpRequestHandler {
        
        private final Registry registry;

        
        public HttpRequestHandler(Registry registry) {
            this.registry = registry;
        }
        
        public void onRequest(IHttpExchange exchange) throws IOException, BadMessageException {
            
            IHttpRequest request = exchange.getRequest();
            String path = request.getRequestURI();
            
            int channelId;
            int idx = path.indexOf("/");
            int idx2 = path.indexOf("/", idx + 1);
            if (idx2 == -1) {
                channelId = Integer.parseInt(path.substring(idx + 1, path.length()));
            } else {
                channelId = Integer.parseInt(path.substring(idx + 1, idx2));
            }

            NonBlockingBodyDataSource ds = request.getNonBlockingBody();
            ds.addCompleteListener(new BodyCompleteListener(ds, registry, channelId, exchange));
            
        }
    }
    
    private static final class BodyCompleteListener implements IBodyCompleteListener {
        
        private final NonBlockingBodyDataSource ds;
        private final Registry registry;
        private final int channelId;
        private final IHttpExchange exchange; 
        
        public BodyCompleteListener(NonBlockingBodyDataSource ds, Registry registry, int channelId, IHttpExchange exchange) {
            this.ds = ds;
            this.registry = registry;
            this.channelId = channelId;
            this.exchange = exchange;
        }
        
        public void onComplete() throws IOException {
            try {
                int available = ds.available();
                ByteBuffer[] data = ds.readByteBufferByLength(available);
                
                INonBlockingConnection con = registry.retrieveTcpConnection(channelId, 1000);
                
                IDataHandler dh = new IDataHandler() {
                  public boolean onData(INonBlockingConnection connection) throws IOException {
                        connection.resetToReadMark();
                        connection.markReadPosition();
                        int length = connection.readInt();
                        ByteBuffer[] data = connection.readByteBufferByLength(length);
                        connection.removeReadMark();
                        
                        registry.addTcpConnection(channelId, connection);
                        exchange.send(new HttpResponse(200, "text/plain", data));
                        return false;
                    }  
                };
                con.setHandler(dh);
                
                con.write(available);
                con.write(data);
                
            } catch (SocketTimeoutException ste) {
                exchange.sendError(404);
            }
            
        }
    }
    
    
    
    
    private static final class TcpConnectHandler implements IConnectHandler, IDisconnectHandler {
        
        private final Registry registry;
        
        public TcpConnectHandler(Registry registry) {
            this.registry = registry;
        }
        

        public boolean onConnect(INonBlockingConnection connection) throws IOException {
            
            IDataHandler cannelIdReader = new IDataHandler() {
                
                public boolean onData(INonBlockingConnection connection) throws IOException {
                    Integer channelId = connection.readInt();
                    
                    registry.addTcpConnection(channelId, connection);

                    connection.setHandler(null);
                    return true;
                }
            };
            
            connection.setHandler(cannelIdReader);
            return true;
        }
        
        public boolean onDisconnect(INonBlockingConnection connection) throws IOException {
            registry.removeTcpConnection(connection);
            return true;
        }
    }
    
    
    
    private static final class Registry {
        
        private final Random random = new Random(); 
        
        private final HashMap<Integer, List<INonBlockingConnection>> openConnections = new HashMap<Integer, List<INonBlockingConnection>>();
        
        
        synchronized void addTcpConnection(int channelId, INonBlockingConnection nbc) {
            nbc.setAttachment(channelId);
            
            List<INonBlockingConnection> cons = openConnections.get(channelId);
            if (cons == null)  {
                cons = new ArrayList<INonBlockingConnection>();
                openConnections.put(channelId, cons);
            }
            
            cons.add(nbc);
            
            notifyAll();
        }
        
        
        synchronized void removeTcpConnection(INonBlockingConnection nbc) {
            Integer channelId = (Integer) nbc.getAttachment();
            List<INonBlockingConnection> cons = openConnections.get(channelId);
            cons.remove(nbc);
            if (cons.isEmpty()) {
                openConnections.remove(channelId);
            }
        }

        
        synchronized INonBlockingConnection retrieveTcpConnection(Integer channelId, long timeoutMillis) throws SocketTimeoutException {
            
            long start = System.currentTimeMillis();
            
            do {
                List<INonBlockingConnection> cons = openConnections.get(channelId);
                if (cons == null) {
                    try {
                        wait(timeoutMillis);
                    } catch (InterruptedException ignore) { }
                } else {
                    if (cons.isEmpty()) {
                        openConnections.remove(channelId);
                    } else {
                        INonBlockingConnection con = cons.remove(getRandomId(cons.size() - 1));
                        if (cons.isEmpty()) {
                            openConnections.remove(channelId);
                        }
                        return con;
                    }
                }
                
            } while (System.currentTimeMillis() < (start + timeoutMillis));
            
            throw new SocketTimeoutException("colud not get a connection of channel " + channelId);
        }

        private int getRandomId(int max) {
            if (max == 0) {
                return 0;
            }
            
            int i = 0;
            do {
                i = random.nextInt();
            } while (i < 0);
            
            i = i % max;
            return i;
        }
        
        @Override
        public synchronized String toString() {
            StringBuilder sb = new StringBuilder();
            for (Entry<Integer, List<INonBlockingConnection>> entry : openConnections.entrySet()) {
                sb.append(entry.getKey() + ": ");
                for (INonBlockingConnection con : entry.getValue()) {
                    sb.append(con.getId() + " ");
                }
                sb.append("\r\n");
            }
            
            return sb.toString();
        }
    }
    
}
