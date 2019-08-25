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
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xlightweb.BodyDataSink;
import org.xlightweb.IHttpRequestHeader;
import org.xlightweb.IHttpResponse;
import org.xlightweb.NonBlockingBodyDataSource;
import org.xlightweb.FutureResponseHandler;
import org.xlightweb.Supports100Continue;






/**
 * FutureResponse Handler which supports continue
 * 
 * @author grro@xlightweb.org
 */
@Supports100Continue
class FutureContinueResponseHandler extends FutureResponseHandler {
    
    private static final Logger LOG = Logger.getLogger(FutureContinueResponseHandler.class.getName());
    
        
    private final IHttpRequestHeader requestHeader;
    private final NonBlockingBodyDataSource dataSource;
    private final String id;
    private BodyDataSink dataSink;
    
    private boolean isContinueReceived = false;
    
    
    public FutureContinueResponseHandler(IHttpRequestHeader requestHeader, NonBlockingBodyDataSource dataSource, String id) {
    	this.requestHeader = requestHeader;
        this.dataSource = dataSource;
        this.id = id;
    }
    
    @Override
    public void onResponse(IHttpResponse response) throws IOException {
        if (response.getStatus() == 100) {
            if (isContinueReceived) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + id + "] got a second 100-continue response. Ignoring it");
                }
                
            } else {
            	synchronized (this) {
            		isContinueReceived = true;
            		onContinueResponse();
            	}
            }
            
        } else {
            onNon100Response(response);
        }
    }
    
    void onNon100Response(IHttpResponse response) throws IOException {
        if (isContinueReceived) {
            response.setAttribute("org.xlightweb.HttpClientConnection.100-continueReceived", true);
        }
        super.onResponse(response);
    }
    
    
    public void onContinueResponse() throws IOException {
    	if ((isContinueReceived == true) && (dataSink != null)) {
	        if (LOG.isLoggable(Level.FINE)) {
	            LOG.fine("[" + id + "] got 100-continue response. Sending request body data");
	        }
	        requestHeader.setAttribute("org.xlightweb.HttpClientConnection.100-continue.bodytransfered", true);
	        
	        HttpClientConnection.forward(dataSource, dataSink);
	        dataSink = null;
    	}
    }
    
    void setBodyDataSink(BodyDataSink dataSink) throws IOException {
    	synchronized (this) {
    		this.dataSink = dataSink;
    		onContinueResponse();
    	}
    }
}
