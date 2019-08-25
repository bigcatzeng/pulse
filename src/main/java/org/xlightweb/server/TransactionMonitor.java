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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xlightweb.BodyDataSink;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHeader;
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
	private final TransactionLog transactionLog;

	
	public TransactionMonitor(TransactionLog transactionLog) {
		this.transactionLog = transactionLog;
	}
	
	
	public void registerMessageHeaderReceived(HttpServerConnection con, IHttpRequestHeader requestHeader) {
		Transaction transaction = new Transaction();
		transaction.registerMessageHeaderReceived(con, requestHeader);
			
		pendingTransactions.put(requestHeader, transaction);
		transactionLog.add(transaction);
	}
	
	
	   
    public void registerMessageReceived(HttpServerConnection con, IHttpRequest request) {
        Transaction transaction = pendingTransactions.get(request.getRequestHeader());
        if (transaction != null) {
            transaction.registerMessageReceived(con, request);
        }
    }
    
    
    public void registerMessageReceivedException(HttpServerConnection con, IHttpRequest request, IOException ioe) {
        Transaction transaction = pendingTransactions.get(request.getRequestHeader());
        if (transaction != null) {
            transaction.registerMessageExceptionReceived(con, request, ioe);
        }
    }
    
    
    public void registerMessageHeaderSent(IHttpRequest request, IHttpResponseHeader responseHeader, BodyDataSink body) {
        Transaction transaction = pendingTransactions.get(request.getRequestHeader());
        if (transaction != null) {
            transaction.registerMessageHeaderSent(request, responseHeader, body);
        }
    }
    
    public void registerMessageSent(IHttpRequest request) {
        Transaction transaction = pendingTransactions.remove(request.getRequestHeader());
        if (transaction != null) {
            transaction.registerMessageSent(request);
        }
    }

    
    public void registerMessageBodySentError(IHttpRequest request) {
        Transaction transaction = pendingTransactions.remove(request.getRequestHeader());
        if (transaction != null) {
            transaction.registerMessageBodySentError(request);
        }
    }
    
    
	int getPendingTransactions() {
		return pendingTransactions.size();
	}


 

	@SuppressWarnings("unchecked")
	static final class TransactionLog {
		
		private final LinkedList<Transaction> transactions = new LinkedList<Transaction>();
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
		
		private static final int WAITING_FOR_REQUEST_BODY = 1;
		private static final int WAITING_FOR_RESPONSE_HEADER = 5;
		private static final int WAITING_FOR_RESPONSE_BODY = 9;
		private static final int COMPLETE = 15;
		
		private int state = WAITING_FOR_REQUEST_BODY;

		
		private long startDate = 0;
		private long endDate = 0;

		
		private boolean isErrorOccuredBySendingBody = false;
		
		private String requestHeaderInfo = "";
		private NonBlockingBodyDataSource requestBody = null;
		private String responseHeaderInfo = "";
		private BodyDataSink responseBody = null;
		private String conInfo = "";
	

		private SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
		
		
		
		public void registerMessageHeaderReceived(HttpServerConnection con, IHttpRequestHeader requestHeader) {
		    startDate = System.currentTimeMillis();
			
            conInfo = "id=" + con.getId();
			
			if (requestHeader.getQueryString() != null) {
				requestHeaderInfo = "[" + df.format(new Date()) + "] " + 
				                    con.getUnderlyingTcpConnection().getRemoteAddress() + ":" + con.getUnderlyingTcpConnection().getRemotePort() + 
				                    " " + requestHeader.getMethod() + " " + requestHeader.getRequestURI() + requestHeader.getQueryString();
			} else {
				requestHeaderInfo = "[" + df.format(new Date()) + "] " + 
				                    con.getUnderlyingTcpConnection().getRemoteAddress() + ":" + con.getUnderlyingTcpConnection().getRemotePort() +
				                    " " + requestHeader.getMethod() + " " + requestHeader.getRequestURI();
			}
		}
		
		
		
		public void registerMessageReceived(HttpServerConnection con, IHttpRequest request) {
		    state = WAITING_FOR_RESPONSE_HEADER;
		    
		    if (request.hasBody()) {
		        try {
		            this.requestBody = request.getNonBlockingBody();
		        } catch (IOException ioe) {
		            if (LOG.isLoggable(Level.FINE)) {
		                LOG.fine("error occured by getting body " + ioe.toString());
		            }
		        }
		    }
		}
		
		
		
		public void registerMessageExceptionReceived(HttpServerConnection con, IHttpRequest request, IOException ioe) {
		    state = WAITING_FOR_RESPONSE_HEADER;
        }
        
		
		
		public void registerMessageHeaderSent(IHttpRequest request, IHttpResponseHeader responseHeader, BodyDataSink responseBody) {
		    state = WAITING_FOR_RESPONSE_BODY;
		    
            responseHeaderInfo = responseHeader.getStatus() + " " + responseHeader.getReason();
            this.responseBody = responseBody;
		}
        
		
		public void registerMessageSent(IHttpRequest request) {
		    endDate = System.currentTimeMillis();
		    state = COMPLETE;
		}
		
		
		public void registerMessageBodySentError(IHttpRequest request) {
		    endDate = System.currentTimeMillis();
            state = COMPLETE;
            isErrorOccuredBySendingBody = true;
		}
		
		
		private String getRequestBodyInfo() {
		    
		    if (requestBody == null) {
		        return "NO BODY";
		        
		    } else {
		        if (state == WAITING_FOR_RESPONSE_BODY) {
		            return requestBody.getClass().getSimpleName() + "/currentSize " + HttpServerConnection.getSizeDataReceived(requestBody);
		        } else {
		            return requestBody.getClass().getSimpleName() + "/totalSize " + HttpServerConnection.getSizeDataReceived(requestBody);
		        }
		    }		    
		}
		
		
		  
        private String getResponseBodyInfo() {
            
            if (responseBody == null) {
                return "NO BODY";
                
            } else {
                if (state == WAITING_FOR_RESPONSE_BODY) {
                    return responseBody.getClass().getSimpleName() + "/currentSize " + HttpServerConnection.getSizeWritten(responseBody);
                } else {
                    return responseBody.getClass().getSimpleName() + "/totalSize " + HttpServerConnection.getSizeWritten(responseBody);
                }
            }           
        }
		
		@Override
		public String toString() {
		    
			switch (state) {
			
                case WAITING_FOR_REQUEST_BODY:
                    String elapsed = "elapsed=" + DataConverter.toFormatedDuration(System.currentTimeMillis() - startDate);
                    return requestHeaderInfo + " (" + getRequestBodyInfo() + ") -> " + " [WAITING for REQUEST BODY " + elapsed + " " + conInfo + "]";
                    
                case WAITING_FOR_RESPONSE_HEADER:
                    elapsed = "elapsed=" + DataConverter.toFormatedDuration(System.currentTimeMillis() - startDate);
                    return requestHeaderInfo  + " (" + getRequestBodyInfo() + ") -> [WAITING for RESPONSE HEADER " + elapsed + " " + conInfo + "]";
                
                case WAITING_FOR_RESPONSE_BODY:
                    elapsed = "elapsed=" + DataConverter.toFormatedDuration(System.currentTimeMillis() - startDate);
                    return requestHeaderInfo  + " (" + getRequestBodyInfo() + ") -> " + responseHeaderInfo + " (" + getResponseBodyInfo() + ") [WAITING for RESPONSE BODY " + elapsed + " " + conInfo + "]";
                    
                // complete
                default:
                    elapsed = "elapsed=" + DataConverter.toFormatedDuration(endDate - startDate);
                
                    if (isErrorOccuredBySendingBody) {
                        return requestHeaderInfo + " (" + getRequestBodyInfo() + ") error occured by sending body -> " + responseHeaderInfo + " (" + getResponseBodyInfo() + ") [" + elapsed + " " + conInfo + "]";
                    } else { 
                        return requestHeaderInfo + " (" + getRequestBodyInfo() + ") -> " + responseHeaderInfo + " (" + getResponseBodyInfo() + ") [" + elapsed + " " + conInfo + "]";
			        }
			}
		}
	}

}
