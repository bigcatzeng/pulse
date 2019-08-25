package org.xlightweb;




import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;


import org.apache.commons.codec.binary.Base64;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.IHttpSocketTimeoutHandler;
import org.xlightweb.InvokeOn;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.Execution;
import org.xsocket.ILifeCycle;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.IDisconnectHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.NonBlockingConnection;
import org.xsocket.connection.IConnection.FlushMode;


 
public class ContinueableHttpProxy2 extends HttpServer { 

	public ContinueableHttpProxy2(boolean isAuthRequired) throws IOException {
		this(0, isAuthRequired);
	}
	
	public ContinueableHttpProxy2(int port, boolean isAuthRequired) throws IOException {
		super(port, new HttpForwardHandler(isAuthRequired));
	}
	
	
	@Supports100Continue
	private static final class HttpForwardHandler implements IHttpRequestHandler, ILifeCycle {

		private HttpClient httpClient = null;
	
		private boolean isAuthRequired = false;
		
		public HttpForwardHandler(boolean isAuthRequired) {
			this.isAuthRequired = isAuthRequired;
		}
		
		public void onInit() {
			httpClient = new HttpClient();
		}
		
		public void onDestroy() throws IOException {
			httpClient.close();
		}
		

		public void onRequest(IHttpExchange exchange) throws IOException {

			IHttpRequest req = exchange.getRequest();

			if (isAuthRequired) {
				String s = req.getHeader("Proxy-Authorization");
				if (s != null) {
					int idx = s.indexOf(" ");
					String algorithm = s.substring(0, idx);
					if (algorithm.equalsIgnoreCase("Basic")) {
						String decoded = new String(Base64.decodeBase64(s.substring(idx + 1, s.length()).getBytes()));
						String[] upp = decoded.split(":");
						if (!upp[0].equals(upp[1])) {
							exchange.sendError(401);
							return;
						}
					} 
				}
			}
			
			
			
			if (req.getMethod().equalsIgnoreCase("CONNECT")) {
				establishTunnel(exchange);
				return;
			}
			
			
			String path = req.getRequestUrl().getFile();
			URL target = new URL(path);
			
			req.setRequestUrl(target);
			
			
			// add via header
			req.addHeader("Via", "myProxy");
		
			
				
			// .. and forward the request
			try {
				httpClient.send(req, new HttpReverseHandler(exchange));
			} catch (ConnectException ce) {
				exchange.sendError(502, ce.getMessage());
			}
		}
		

		private void establishTunnel(IHttpExchange exchange) {
			IHttpRequest req = exchange.getRequest();
			
			String forwardHost = null;
			int forwardPort = 443;
			
			String uri = req.getRequestURI();
			int idx = uri.lastIndexOf(":");
			if (idx == -1) {
				forwardHost = uri;
			} else {
				forwardHost = uri.substring(0, idx);
				forwardPort = Integer.parseInt(uri.substring(idx + 1, uri.length()));
			}
			
			try {
				INonBlockingConnection forwardCon = new NonBlockingConnection(forwardHost, forwardPort);
				INonBlockingConnection tcpCon = exchange.getConnection().getUnderlyingTcpConnection();

				forwardCon.setAttachment(tcpCon);
				tcpCon.setAttachment(forwardCon);
				
				forwardCon.setFlushmode(FlushMode.ASYNC);
				forwardCon.setAutoflush(true);
				tcpCon.setFlushmode(FlushMode.ASYNC);
				tcpCon.setAutoflush(true);

				forwardCon.setHandler(new TcpProxyHandler());
				tcpCon.setHandler(new TcpProxyHandler());
				
				IHttpResponse response = new HttpResponse(200);
				response.getResponseHeader().setReason("Connection established");
				response.setHeader("Proxy-agent", "myProxy");
				exchange.send(response);
				
			} catch (IOException ioe) {
				exchange.sendError(ioe);
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
    			// add via header
    			resp.addHeader("Via", "myProxy");
		    }
			
			// 	return the response 
			exchange.send(resp);
		}

		@Execution(Execution.NONTHREADED)
		public void onException(IOException ioe) {
			exchange.sendError(500, ioe.toString());
		}
		
		@Execution(Execution.NONTHREADED)
		public void onException(SocketTimeoutException stoe) {
			exchange.sendError(504, stoe.toString());
		}
	}
	
	

	private static class TcpProxyHandler implements IDataHandler, IDisconnectHandler {
		
	
		public boolean onDisconnect(INonBlockingConnection connection) throws IOException {
			INonBlockingConnection reverseConnection = (INonBlockingConnection) connection.getAttachment();
			if (reverseConnection != null) {
				connection.setAttachment(null);
				reverseConnection.close();
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
				connection.close();
			}
			
			return true;
		}
	}	
}
