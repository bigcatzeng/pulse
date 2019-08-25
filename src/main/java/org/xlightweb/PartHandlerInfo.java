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




import org.xsocket.Execution;


 

/** 
 * <br/><br/><b>This is a xSocket internal class and subject to change</b> 
 *
 * @author grro@xlightweb.org
 */
final class PartHandlerInfo {
	
	private boolean isHandlerInvokeOnMessageReceived = false;
    private boolean isHandlerMultithreaded = true;
	
	

	PartHandlerInfo(Class<?> clazz) {

		if (clazz == null) {
			return;
		}

		if (IHttpRequestHandler.class.isAssignableFrom(clazz)) {
		    isHandlerMultithreaded = isOnRequestMultithreaded((Class<IHttpRequestHandler>) clazz);
			isHandlerInvokeOnMessageReceived = isOnRequestInvokeOnMessageReceived((Class<IHttpRequestHandler>) clazz);
		}
	}


	static boolean isOnRequestMultithreaded(Class<IHttpRequestHandler> serverHandlerClass) {
		boolean isMultithreaded = HttpUtils.isHandlerMultithreaded(serverHandlerClass, (IPartHandler.DEFAULT_EXECUTION_MODE == Execution.MULTITHREADED));
		return HttpUtils.isMethodMultithreaded(serverHandlerClass, "onPart", isMultithreaded, IPart.class);
	}

	
	
	static boolean isOnRequestInvokeOnMessageReceived(Class<IHttpRequestHandler> handlerClass) {
		boolean invokeOnMessageReceived = HttpUtils.isInvokeOnMessageReceived(handlerClass, (IPartHandler.DEFAULT_INVOKE_ON_MODE == InvokeOn.MESSAGE_RECEIVED));
		return HttpUtils.isInvokeOnMessageReceived(handlerClass, "onPart", invokeOnMessageReceived, IPart.class);	
	}


	public boolean isHandlerMultithreaded() {
        return isHandlerMultithreaded;
    }
	
	public boolean isHandlerInvokeOnMessageReceived() {
		return  isHandlerInvokeOnMessageReceived;
	}
}
