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

import java.io.File;


import java.io.IOException;


import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;


import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.xlightweb.client.HttpClient;
import org.xlightweb.client.HttpClientConnection;
import org.xlightweb.server.HttpServer;
import org.xsocket.DataConverter;
import org.xsocket.Execution;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;
import org.xsocket.connection.IConnection.FlushMode;

 

/**
*
* @author grro@xlightweb.org
*/
public final class UncompressingHttpProxyTest  {
  
	

    @Test
    public void testCompressesWithInterceptor() throws Exception {
        File file = QAUtil.copyToTempfile("compressedfile.zip");
        String path = file.getParent();
        
        HttpServer httpServer = new HttpServer(new FileServiceRequestHandler(path, true));
        httpServer.start();
        
        
        
        RequestHandlerChain reqHdl = new RequestHandlerChain();
        reqHdl.addLast(new LogFilter());
        HttpClient proxyHttpClient = new HttpClient();
        reqHdl.addLast(new HttpForwardHandler(proxyHttpClient, false, new HashMap<String, String>()));
            
        HttpServer proxyServer = new HttpServer(0, reqHdl, 1, 10);
        proxyServer.setIdleTimeoutMillis(15 * 60 * 1000L);
        proxyHttpClient.setWorkerpool(proxyServer.getWorkerpool());
        proxyServer.start();
        
    
        HttpClient httpClient = new HttpClient();
        httpClient.setProxyHost("localhost");
        httpClient.setProxyPort(proxyServer.getLocalPort());
        
        for (int i = 0; i < 500; i++) {
            GetRequest req = new GetRequest("http://localhost:" + httpServer.getLocalPort() + "/" + file.getName());
            req.setHeader("Accept-Encoding", "gzip");
            
            IHttpResponse resp = httpClient.call(req);
            Assert.assertEquals(5867, resp.getContentLength());
        }
        
        file.delete();
        httpClient.close();
        httpServer.close();
        proxyHttpClient.close();
        proxyServer.close();
    }

    

    @Supports100Continue
    private static final class HttpForwardHandler implements IHttpRequestHandler {

        
        private final HttpClient httpClient;
        private final boolean replaceUserAgent;
        private final String[] headersToAdd;
        private final Map<String, String> rewriteHost;
        
        public HttpForwardHandler(HttpClient httpClient, boolean replaceUserAgent, Map<String, String>  rewriteHost, String... headersToAdd) {
            this.httpClient = httpClient;
            this.replaceUserAgent = replaceUserAgent;
            this.headersToAdd = headersToAdd;
            this.rewriteHost = rewriteHost;
        }
        

        public void onRequest(IHttpExchange exchange) throws IOException {

            IHttpRequest req = exchange.getRequest();
            
            // connect request?
            if (req.getMethod().equalsIgnoreCase("CONNECT")) {
                try {
                    HttpUtils.establishTcpTunnel(exchange.getConnection(), req.getRequestURI());
                            
                    IHttpResponse response = new HttpResponse(200);
                    response.getResponseHeader().setReason("Connection established");
                    response.setHeader("Proxy-agent", "myProxy");
                    exchange.send(response);
                            
                } catch (IOException ioe) {
                    exchange.sendError(ioe);
                }
            
                
            // .. no
            } else {
                req.removeHopByHopHeaders();
            
                if (replaceUserAgent) {
                    req.setHeader("UserAgent", "xLightweb/" + HttpUtils.getImplementationVersion());
                }
                
                for (String headerToAdd : headersToAdd) {
                    req.addHeaderLine(headerToAdd);
                }
                
                String path = req.getRequestUrl().getFile();
                
                URL target = new URL(path);
                
                String host = rewriteHost.get(target.getHost());
                if (host != null) {
                    target = new URL(target.getProtocol() + "://" + host + target.getFile());
                }
                
                req.setRequestUrl(target);
                
                
                req.addHeader("Via", "myProxy");
            
                try {
                    httpClient.send(req, new HttpReverseHandler(exchange));
                } catch (Exception ce) {
                    exchange.sendError(502, ce.getMessage());
                }
            }
        }
    }

    
    
    @Supports100Continue
    private static final class HttpReverseHandler implements IHttpResponseHandler, IHttpSocketTimeoutHandler {
        
        private IHttpExchange exchange = null;
        
            
        public HttpReverseHandler(IHttpExchange exchange) {
            this.exchange = exchange;
        }


        @Execution(Execution.NONTHREADED)
        @InvokeOn(InvokeOn.HEADER_RECEIVED)
        public void onResponse(IHttpResponse resp) throws IOException {

            if (resp.getStatus() > 199) {
                resp.removeHopByHopHeaders();
                resp.addHeader("Via", "myProxy");
            }
            
            exchange.send(resp);
        }

        @Execution(Execution.NONTHREADED)
        public void onException(IOException ioe) {
            if (ioe instanceof ClosedChannelException) {
                exchange.destroy();
            } 
            exchange.sendError(ioe);
        }
        
        @Execution(Execution.NONTHREADED)
        public void onException(SocketTimeoutException stoe) {
            exchange.sendError(504, stoe.toString());
        }
    }

    
	
	private static final class LogFilter implements IHttpRequestHandler {
	    
	    private static final Logger LOG = Logger.getLogger(LogFilter.class.getName());
	    
	    private final long start = System.currentTimeMillis();
	    
	    private final AtomicInteger counter = new AtomicInteger();
	    
	    
	    public void onRequest(final IHttpExchange exchange) throws IOException {
	        final int id = counter.incrementAndGet();
	        final long sendTime = System.currentTimeMillis();
	        
	        
	        
	        final IHttpResponseHandler respHdl = new IHttpResponseHandler() {
	            
	            public void onResponse(final IHttpResponse response) throws IOException {
	                
	                final String txt = "\r\n --------- RESPONSE " + id + " " + " [elapsed=" + DataConverter.toFormatedDuration(System.currentTimeMillis() - sendTime) + "]";
	                
	                if (response.hasBody()) {
	                    if (isBodyPrintable(response)) {

	                        final boolean isPacked =isPacked(response);
	                        final boolean isDecodeable = isDecodeable(response);
	                        
	                        NonBlockingBodyDataSource source = response.getNonBlockingBody(); 
	                        final BodyDataSink sink = exchange.send(response.getResponseHeader());
	                        sink.setFlushmode(FlushMode.ASYNC);

	                        BodyForwarder bodyFowarder = new BodyForwarder(source, sink) {
	                        
	                            private final List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
	                            
	                            
	                            @Override
	                            public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException, IOException {
	                                
	                                int available = bodyDataSource.available();
	                                if (available > 0) {
	                                    ByteBuffer[] bufs = bodyDataSource.readByteBufferByLength(available) ;
	                                    for (ByteBuffer buffer : bufs) {
	                                        buffers.add(buffer.duplicate());
	                                    }
	                                    bodyDataSink.write(bufs);
	                                }
	                            }
	                            
	                            
	                            @Override
	                            public void onException(Exception e) {
	                                LOG.info("RESPONSE " + id + " BODY Exception: " + DataConverter.toString(e));
	                                System.out.println("\r\n --------- RESPONSE " + id + " " + " [ERROR] " + DataConverter.toString(e));
	                                exchange.destroy();
	                            }
	                            
	                            @Override
	                            public void onComplete() {
	                                try {
	                                    String body;
	                                    
	                                    String msg = txt;
	                                    if (isPacked) {
	                                        msg += " " + "(unpacked body)";
	                                        body = new String(HttpUtils.decompress(DataConverter.toBytes(buffers)), response.getCharacterEncoding());
	                                    } else {
	                                        body = DataConverter.toString(buffers, response.getCharacterEncoding());
	                                    }
	                                    
	                                    if (isDecodeable) {
	                                        msg += " " + "(decoded body)";
	                                        body = URLDecoder.decode(body, "UTF-8");
	                                    }
	    
	                                    System.out.println(msg + " ---\r\n" + response.getResponseHeader() + "\r\n" + body);
	                                    
	                                } catch (IOException ioe) {
	                                    LOG.info("RESPONSE BODY COMPLETE Exception: " + DataConverter.toString(ioe));
	                                    System.out.println(txt + "  ---\r\n" + response.getResponseHeader()  + "\r\n[EXCPETION] " + DataConverter.toString(ioe));
	                                }
	                            }
	                        };
	                        source.setDataHandler(bodyFowarder);
	                        return;
	                                        
	                    } else {
	                        System.out.println(txt + "  (Body is not printed) ---\r\n" + response.getResponseHeader());
	                    }
	                    
	                } else {
	                    System.out.println(txt + "  ---\r\n" + response);
	                }
	                
	                exchange.send(response);
	            }
	            
	            
	            public void onException(IOException ioe) {
	                LOG.info("RESPONSE " + id + " Exception: " + DataConverter.toString(ioe));
	                System.out.println("\r\n --------- RESPONSE " + id + " " + " [ERROR] " + DataConverter.toString(ioe));
	                exchange.sendError(ioe);
	            }
	        };

	        
	        final String txt = "\r\n --------- REQUEST " + id + " " + " [" + printCurrentDate() + "]";
	            
	        IHttpRequest request = exchange.getRequest();
	        if (request.hasBody()) {
	            
	            if (isBodyPrintable(request)) {
	                
	                final boolean isPacked =isPacked(request);
	                final boolean isDecodeable = isDecodeable(request);

	                
	                NonBlockingBodyDataSource source = request.getNonBlockingBody(); 
	                BodyDataSink sink = exchange.forward(request.getRequestHeader(), respHdl);
	                sink.setFlushmode(FlushMode.ASYNC);
	                
	                BodyForwarder bodyFowarder = new BodyForwarder(source, sink) {
	                
	                    private final List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
	                    
	                    @Override
	                    public void onData(NonBlockingBodyDataSource bodyDataSource, BodyDataSink bodyDataSink) throws BufferUnderflowException, IOException {
	                        
	                        int available = bodyDataSource.available();
	                        if (available > 0) {
	                            ByteBuffer[] bufs = bodyDataSource.readByteBufferByLength(available) ;
	                            for (ByteBuffer buffer : bufs) {
	                                buffers.add(buffer.duplicate());
	                            }
	                            bodyDataSink.write(bufs);
	                        }
	                    }
	                    
	                    @Override
	                    public void onException(Exception e) {
	                        LOG.info("REQUEST Exception: " + DataConverter.toString(e));
	                        System.out.println("\r\n --------- REQUEST " + id + " " + " [ERROR] " + DataConverter.toString(e));
	                        exchange.destroy();
	                    }

	                    
	                    @Override
	                    public void onComplete() {
	                        try {
	                            String body;
	    
	                            String msg = txt;
	                            IHttpRequest request = exchange.getRequest();
	                            
	                            if (isPacked) {
	                                msg += " " + "(unpacked body)";
	                                body = new String(HttpUtils.decompress(DataConverter.toBytes(buffers)), request.getCharacterEncoding());
	                            } else {
	                                body = DataConverter.toString(buffers, request.getCharacterEncoding());
	                            }
	                            
	                            if (isDecodeable) {
	                                msg += " " + "(decoded body)";
	                                body = URLDecoder.decode(body, "UTF-8");
	                            }
	    
	                            System.out.println(msg + " ---\r\n" + request.getRequestHeader() + "\r\n" + body);
	                            
	                        } catch (IOException ioe) {
	                            LOG.info("REQUEST ON COMPLETED " +  id + " Exception: " + DataConverter.toString(ioe));
	                            System.out.println("\r\n --------- REQUSST " + id + " " + " [ERROR] " + DataConverter.toString(ioe));
	                            exchange.destroy();
	                        }
	                    }
	                };
	                source.setDataHandler(bodyFowarder);
	                return;
	                                    
	            } else {
	                System.out.println(txt + " (Body is not printed) ---\r\n" + request.getRequestHeader());
	            }
	                
	        } else {
	            System.out.println(txt + " ---\r\n" + request);
	        }       

	        exchange.forward(exchange.getRequest(), respHdl);
	    }   


	    private synchronized String printCurrentDate() {
	        return "sinceStart=" +(System.currentTimeMillis() - start)+ " millis";
	    }
	    
	    

	    
	    private boolean isBodyPrintable(IHttpMessage message) {
	        if ((message.getCharacterEncoding() == null) || (message.getHeader("Accept-Ranges") != null) || (message.getContentType() == null)) {
	            return false;
	        }
	        
	        if (message.getHeader("Content-Encoding") != null) {
	            if (message.getHeader("Content-Encoding").equalsIgnoreCase("GZIP")) {
	                if (message.getContentLength() > 9000) {
	                    return false;
	                }
	            } else {
	                return false;
	            }
	        }
	        
	        
	        String contentType = message.getContentType().trim().toLowerCase();
	        if ((contentType.startsWith("text") || contentType.startsWith("application/json")  || contentType.startsWith("application/xml") || contentType.startsWith("application/x-www-form-urlencoded") || contentType.startsWith("application/x-javascript"))) {
	            return true;
	        }
	        
	        contentType = contentType.split(",")[0].trim();
	        if (contentType.startsWith("application/") && contentType.endsWith("+xml")) {
	            return true;
	        }
	        
	        return false;
	    }
	    
	    private static boolean isDecodeable(IHttpMessage message) {
	        String contentType = message.getContentType().trim().toLowerCase();
	        return (contentType.startsWith("application/x-www-form-urlencoded"));
	    }
	    
	    
	    
	    private static boolean isPacked(IHttpMessage message) {
	        if (message.getHeader("Content-Encoding") != null) {
	            if (message.getHeader("Content-Encoding").equalsIgnoreCase("GZIP")) {
	                return true; 
	            }
	        }
	        
	        return false;
	    }
	}   	
}
