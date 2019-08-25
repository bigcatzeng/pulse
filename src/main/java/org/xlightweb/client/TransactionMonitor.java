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
package org.xlightweb.client;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xlightweb.IBodyCompleteListener;
import org.xlightweb.IBodyDestroyListener;
import org.xlightweb.IHttpRequestHeader;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHeader;
import org.xlightweb.NonBlockingBodyDataSource;
import org.xsocket.DataConverter;




/**
 * Transaction Monitor
 * @author grro@xlightweb.org
 */
final class TransactionMonitor {
	
	private static final Logger LOG = Logger.getLogger(TransactionMonitor.class.getName());
	

	private final Map<IHttpRequestHeader, Transaction> pendingTransactions = new HashMap<IHttpRequestHeader, Transaction>();
	private final AtomicInteger pending = new AtomicInteger(0);
	
	private final TransactionLog transactionLog;

	public TransactionMonitor(TransactionLog transactionLog) {
		this.transactionLog = transactionLog;
	}
	
	
	public void register(IHttpRequestHeader requestHeader) {
		Transaction transaction = new Transaction(this);
		transaction.register(requestHeader);
			
		pendingTransactions.put(requestHeader, transaction);
		transactionLog.add(transaction);
	}
	
	public void register(HttpClientConnection con, IHttpRequestHeader requestHeader, IHttpResponse response) {
		Transaction transaction = pendingTransactions.remove(requestHeader);
		if (transaction != null) {
			transaction.register(con, response);
		}
	}
	
	void incPending() {
		pending.incrementAndGet();
	}
	
	void decPending() {
		pending.decrementAndGet();
	}
	
	int getPendingTransactions() {
		return pending.get();
	}




	@SuppressWarnings("unchecked")
	static final class TransactionLog {
		
		private LinkedList<Transaction> transactions = new LinkedList<Transaction>();
		private int maxSize = 0;
		
		TransactionLog(int maxSize) {
			this.maxSize = maxSize;
		}
		
		void setMaxSize(int maxSize) {
			this.maxSize = maxSize;
			removeOddEntries();
		}
		
		int getMaxSize() {
			return maxSize;
		}
		
		void add(Transaction transaction) {
			transactions.add(transaction);
			removeOddEntries();
		}
		
		private void removeOddEntries() {
			while (transactions.size() > maxSize) {
				try {
					transactions.removeFirst();
				} catch (Exception e) {
				    // eat and log exception
					if (LOG.isLoggable(Level.FINE)) {
						LOG.fine("error occured by removing list entry " + e.toString());
					}
				}
			}
		}
		
		
		public List<Transaction> getTransactions() {
			return (List<Transaction>) transactions.clone();
		}
	}



	static final class Transaction {
		
		private final TransactionMonitor monitor;
		
		private String requestHeaderInfo = "";
		private String requestBodyInfo = "";
		private String responseHeaderInfo = "";
		private String responseBodyInfo = "";
		private String conInfo = "";
		
		private boolean isHeaderReceived = false;
		private boolean isBodyReceived = false;
		
		private Long requestHeaderSend = null; 
		private Long responseBodyReceived = null;
		
		private boolean isErrorOccuredByReceivingBody = false;
		
		private String reveivedBodySize = "0 byte";
		private NonBlockingBodyDataSource responseBody = null;
		
		private HttpClientConnection con = null;
		

		private SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
		
		
		public Transaction(TransactionMonitor monitor) {
			this.monitor = monitor;
		}
		
		
		public void register(IHttpRequestHeader requestHeader) {
			monitor.incPending();
			
			requestHeaderSend = System.currentTimeMillis();
			
			if (requestHeader.getQueryString() != null) {
				requestHeaderInfo = "[" + df.format(new Date()) + "] " + requestHeader.getServerName() + ":" + requestHeader.getServerPort() + 
				       " " + requestHeader.getMethod() + " " + requestHeader.getRequestURI() + requestHeader.getQueryString();
			} else {
				requestHeaderInfo = "[" + df.format(new Date()) + "] " + requestHeader.getServerName() + ":" + requestHeader.getServerPort() +
				       " " + requestHeader.getMethod() + " " + requestHeader.getRequestURI();
			}
		}
		
		public void register(HttpClientConnection con, final IHttpResponse response) {
		    this.con = con;
			isHeaderReceived = true;

			IHttpResponseHeader responseHeader = response.getResponseHeader();
			responseHeaderInfo = responseHeader.getStatus() + " " + responseHeader.getReason();
			if (responseHeader.containsHeader("connection")) {
				responseHeaderInfo = responseHeaderInfo + " (connection: " + responseHeader.getHeader("connection") + ")";
			}
			
			
			if (response.hasBody()) {
				try {
	                responseBody = response.getNonBlockingBody();
					responseBodyInfo = "(" + response.getNonBlockingBody().getClass().getSimpleName() + ")";

					BodyListener bodyListener = new BodyListener();
					NonBlockingBodyDataSource ds = response.getNonBlockingBody();
					ds.addCompleteListener(bodyListener);
					ds.addDestroyListener(bodyListener);
					
				} catch (IOException ioe) {
					if (LOG.isLoggable(Level.FINE)) {
						LOG.fine("error occured by registering complete listener " + ioe.toString());
					}
				}
				
			} else {
				responseBodyInfo = "(NO BODY)";
				registerCompleteResponseReceived();
			}
			
			conInfo = "id=" + con.getId();
		}
		
		
		private final class BodyListener implements IBodyCompleteListener, IBodyDestroyListener {
		 
		    public void onComplete() throws IOException {
                reveivedBodySize = DataConverter.toFormatedBytesSize(HttpClientConnection.getDataReceived(responseBody));
                registerCompleteResponseReceived();
            }
		    
		    public void onDestroyed() throws IOException {
		        isErrorOccuredByReceivingBody = true;
                registerCompleteResponseReceived();
		    }
		}
		
		
		private void registerCompleteResponseReceived() {
			monitor.decPending();
			responseBody = null;
			con = null;
			isBodyReceived = true;
			responseBodyReceived = System.currentTimeMillis();
		}
		
		
		private String getConInfo() {
		    HttpClientConnection c = con;
		    if (c == null) {
		        return conInfo;
		    } else {
		        return conInfo + " isOpen=" + con.isOpen();
		    }
		}
		
		
		@Override
		public String toString() {
			String elapsed = "";
			if (responseBodyReceived != null) {
				elapsed = "elapsed=" + DataConverter.toFormatedDuration(responseBodyReceived - requestHeaderSend);
			} else {
				elapsed = "elapsed=" + DataConverter.toFormatedDuration(System.currentTimeMillis() - requestHeaderSend);
			}
						
			if (isBodyReceived) {
			    if (isErrorOccuredByReceivingBody) {
			        return requestHeaderInfo + " " + requestBodyInfo + "-> " + responseHeaderInfo + " " + responseBodyInfo + " error occured by receiving body [" + elapsed + " " + " body=" + reveivedBodySize + " " + getConInfo() + "]";
			    } else {
			        return requestHeaderInfo + " " + requestBodyInfo + "-> " + responseHeaderInfo + " " + responseBodyInfo + " [" + elapsed + " " + " body=" + reveivedBodySize + " " + getConInfo() + "]";
			    }    
				
			} else if (isHeaderReceived) {
			    NonBlockingBodyDataSource rb = responseBody;
			    if (rb == null) {
			        return requestHeaderInfo + " " + requestBodyInfo + "-> " + responseHeaderInfo + " " + responseBodyInfo + " [READING BODY " + elapsed + " " + getConInfo() + "]";
			    } else {
			        return requestHeaderInfo + " " + requestBodyInfo + "-> " + responseHeaderInfo + " " + responseBodyInfo + " [READING BODY " + elapsed + " " + " body=" + DataConverter.toFormatedBytesSize(HttpClientConnection.getDataReceived(rb)) + " " + getConInfo() + "]";
			    }
				
			} else {
				return requestHeaderInfo + " " + requestBodyInfo + "-> " + responseHeaderInfo + " " + responseBodyInfo + " [WAITING FOR HEADER " + elapsed + " " + getConInfo() + "]";
			}
		}
	}

}
