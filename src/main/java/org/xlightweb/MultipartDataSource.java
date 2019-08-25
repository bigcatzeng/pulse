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


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.xlightweb.AbstractHttpConnection.IMultimodeExecutor;
import org.xsocket.DataConverter;
import org.xsocket.Execution;
import org.xsocket.connection.IWriteCompletionHandler;
import org.xsocket.connection.IConnection.FlushMode;



/**
 * multipart body data source
 * 
 * @author grro@xlightweb.org
 *
 */
final class MultipartDataSource extends NonBlockingBodyDataSource implements IForwardable {
    
    private static final Logger LOG = Logger.getLogger(MultipartDataSource.class.getName());
    
    private final List<ForwardablePart> parts = new ArrayList<ForwardablePart>();
    private final String dashBoundary;
    private final String closeDelimiter;
    
    
    MultipartDataSource(IHttpMessageHeader header, IMultimodeExecutor executor, String dashBoundary) throws IOException {
        super(header, executor);
        
	    this.dashBoundary = dashBoundary;
	    closeDelimiter = "\r\n" + dashBoundary + "--";
    }
    
    @Override
    boolean isForwardable() {
        return true;
    }
   
    
    void addPart(IPart part) {
        synchronized (parts) {
            parts.add(new ForwardablePart(part));
        }
    }
    
    List<IPart> getParts() {
        List<IPart> partList = new ArrayList<IPart>();
        for (ForwardablePart part : parts) {
            partList.add(part.delegate);
        }
        return partList;
    }

    public void forwardTo(final BodyDataSink bodyDataSink) throws IOException {
    	bodyDataSink.setFlushmode(FlushMode.ASYNC);

    	ListIterator<ForwardablePart> it = parts.listIterator(parts.size());
    	
    	PartWriter pw = null;
    	while (it.hasPrevious()) {
    	    ForwardablePart part = it.previous();
    	    pw = new PartWriter(pw, part, !it.hasPrevious());
    	}
    	
    	pw.forwardTo(bodyDataSink);
    }

    
    

    @Execution(Execution.NONTHREADED)
    private final class PartWriter implements IForwardable {
    	
    	private final boolean isFirstPart;
    	private final ForwardablePart part;
        private final PartWriter successor;

    	
    	public PartWriter(PartWriter successor, ForwardablePart part, boolean isFirstPart) {
    		this.successor = successor;
    		this.part = part;
    		this.isFirstPart = isFirstPart;
		}

    	
    	public void forwardTo(final BodyDataSink dataSink) throws IOException {
    	    if (LOG.isLoggable(Level.FINE)) {
    	        LOG.fine("[" + getId() + "] writing part " + part);
    	    }

    	    
    	    IWriteCompletionHandler ch = new IWriteCompletionHandler() {
                
    	        @Execution(Execution.NONTHREADED)
                public void onWritten(int written) throws IOException {
                    
                    IBodyCompleteListener cl = new IBodyCompleteListener() {
                        
                        public void onComplete() throws IOException {
                            
                            if (successor == null) {
                                if (LOG.isLoggable(Level.FINE)) {
                                    LOG.fine("[" + getId() + "] writing close delimiter");
                                }
                                
                                IWriteCompletionHandler ch2 = new IWriteCompletionHandler() {
                                    
                                    @Execution(Execution.NONTHREADED)
                                    public void onWritten(int written) throws IOException {
                                        dataSink.closeQuitly();
                                        MultipartDataSource.this.closeQuitly();
                                    }
                                    
                                    public void onException(IOException ioe) {
                                        destroy(ioe.toString());   
                                    }
                                };
                                writeLine(dataSink, closeDelimiter, ch2);
                                
                            } else {
                                successor.forwardTo(dataSink);
                            }
                        }
                    };
                    
                    part.forwardTo(dataSink, cl);
                }
                
                public void onException(IOException ioe) {
                    destroy(ioe.toString());
                }
            };
    	    
    	    if (isFirstPart) {
    	        writeLine(dataSink, dashBoundary, ch);
    	    } else {
    	        writeLine(dataSink, "\r\n" + dashBoundary, ch);
    	    }
    	}
    	
    	private void writeLine(BodyDataSink dataSink, String txt, IWriteCompletionHandler completionHandler) throws IOException {
    		dataSink.write(new ByteBuffer[] { DataConverter.toByteBuffer(txt + "\r\n", "utf-8") }, completionHandler);
    	}
    }

 
    
    boolean isFileDataSource() {
        return false;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isNetworkendpoint() {
        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    String getId() {
        return Integer.toString(hashCode());
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    boolean suspend() throws IOException {
        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    boolean resume() throws IOException {
        return false;
    }
  

    /**
     * {@inheritDoc}
     */
    @Override
    void onClose() {
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    void onDestroy(String reason) {
    }
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        int i = 0;
    	for (IPart part : parts) {
    	    if (i > 0) {
    	        sb.append("\r\n");
    	    }
    	    i++;
        	sb.append(dashBoundary + "\r\n");
    		sb.append(part);
    	}
    	
    	sb.append(closeDelimiter + "\r\n");        
        return sb.toString();
    }   
        
    

    static final class ForwardablePart extends HeaderWrapper implements IPart {
        
        private final IPart delegate;
        
        public ForwardablePart(IPart delegate) {
            super(delegate.getPartHeader());
            this.delegate = delegate;
        }

        
        @SuppressWarnings("deprecation")
        public BlockingBodyDataSource getBlockingBody() throws IOException {
            return delegate.getBlockingBody();
        }
        
        public BodyDataSource getBody() throws IOException {
            return delegate.getBody();
        }

        public NonBlockingBodyDataSource getNonBlockingBody() throws IOException {
            return delegate.getNonBlockingBody();
        }

        public IHeader getPartHeader() {
            return delegate.getPartHeader();
        }

        public boolean hasBody() {
            return delegate.hasBody();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
      
        public void forwardTo(BodyDataSink bodyDataSink, IBodyCompleteListener completeListener) throws IOException {
            bodyDataSink.write(getPartHeader() + "\r\n");
            getNonBlockingBody().forwardTo(bodyDataSink, completeListener);
        }
    }    
}