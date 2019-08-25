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
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;



/**
 * The IFutureResponse represents the result of an asynchronous call. Methods are provided to check if 
 * the call is complete, to wait for its completion, and to retrieve the result of the computation. 
 * The result can only be retrieved using method get when the computation has completed, blocking if 
 * necessary until it is ready. Cancellation is performed by the cancel method. 
 * Additional methods are provided to determine if the task completed normally or was cancelled. 
 * Once a computation has completed, the computation cannot be cancelled. 
 * 
 * <br><br>Example:
 * <pre>
 * 
 *  HttpClient client = new HttpClient();
 *  
 *  IFutureResponse futureResponse = client.send(url);
 *  
 *  // do something else
 *  
 *  IHttpResponse response = futureResponse.getResponse();
 *  // ...
 * </pre>
 * 
 * @author grro@xlightweb.org
 */
public interface IFutureResponse extends Future<IHttpResponse> {
    
    /**
     * blocking call to retrieve the response. Often this method will be used instead ({@link Future#get()} 
     *  
     * @return the response 
     * @throws IOException             if an ioe exception occurs
     * @throws InterruptedException    if the current thread was interrupted while waiting 
     * @throws SocketTimeoutException  if an socket timeout exception occurs
     */
    public IHttpResponse getResponse() throws IOException,  InterruptedException, SocketTimeoutException; 
    
    
    /**
     * blocking call to retrieve the response. Often this method will be used instead ({@link Future#get(long, TimeUnit)}
     * 
     * @param timeout  the maximum time to wait
     * @param unit     the time unit of the timeout argument 
     * @return the response 
     * @throws IOException             if an ioe exception occurs
     * @throws InterruptedException    if the current thread was interrupted while waiting 
     * @throws SocketTimeoutException  if an socket timeout exception occurs
     */
    public IHttpResponse getResponse(long timeout, TimeUnit unit) throws IOException,  InterruptedException, SocketTimeoutException;

    
    
    /**
     * blocking call to retrieve the response. Often the {@link IFutureResponse#getResponse()} method will be used instead of this method. <br><br> 
     * This method exists for compatibility reasons
     * 
     * {@inheritDoc}
     */
    public IHttpResponse get() throws InterruptedException, ExecutionException;

    
    /**
     * blocking call to retrieve the response. Often the {@link IFutureResponse#getResponse(long, TimeUnit)} method will be used instead of this method. <br><br> 
     * This method exists for compatibility reasons
     * 
     * {@inheritDoc}
     */
    public IHttpResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
    
    
    /**
     * Returns true if this task completed. Completion may be due to normal termination, 
     * an exception, or cancellation.
     *  
     * @return true if this task completed.
     */
    public boolean isDone();
 
    
    /**
     * Attempts to cancel execution of receiving the response. This attempt will fail if the response has already 
     * received, already been cancelled, or could not be cancelled for some other reason. 
     * The mayInterruptIfRunning parameter determines whether the receiving process should be interrupted
     *  
     * @param mayInterruptIfRunning  true if the receiving process should be interrupted 
     * @return false if the receiving process could not be cancelled, typically because it has already completed normally; true otherwise
     */
    public boolean cancel(boolean mayInterruptIfRunning);
    
   
    /**
     * Returns true if this task was cancelled before it completed normally. 
     * 
     * @return  true if task was cancelled before it completed
     */
    public boolean isCancelled(); 
}
