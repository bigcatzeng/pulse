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







/**
 * webEventHandler Info 
 * 
 * @author grro
 */
final class IEventHandlerInfo {
    
    
    private final boolean isUnsynchronized;
    private final boolean isOnConnectMultithreaded;
    private final boolean isOnMessageMultithreaded;
    private final boolean isOnDisconnectMultithreaded;

    
    IEventHandlerInfo(Class<IEventHandler> clazz) {
        if (clazz != null) {
            boolean isMultiThreaded = HttpUtils.isHandlerMultithreaded(clazz, true);
            isUnsynchronized = IUnsynchronized.class.isAssignableFrom(clazz);
            isOnConnectMultithreaded = HttpUtils.isMethodMultithreaded(clazz, "onConnect", isMultiThreaded, IEventDataSource.class);
            isOnDisconnectMultithreaded = HttpUtils.isMethodMultithreaded(clazz, "onDisconnect", isMultiThreaded, IEventDataSource.class);
            isOnMessageMultithreaded = HttpUtils.isMethodMultithreaded(clazz, "onMessage", isMultiThreaded, IEventDataSource.class);
            
        } else {
            isUnsynchronized = true;
            isOnConnectMultithreaded = false;
            isOnMessageMultithreaded = false;
            isOnDisconnectMultithreaded = false;
        }
    }
    
    public boolean isOnConnectMultithreaded() {
        return isOnConnectMultithreaded;
    }


    public boolean isOnMessageMultithreaded() {
        return isOnMessageMultithreaded;
    }
    
    public boolean isOnDisconnectMultithreaded() {
        return isOnDisconnectMultithreaded;
    }
    
    public boolean isUnsynchronized() {
        return isUnsynchronized;
    }    
}

    