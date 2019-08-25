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


import java.util.ArrayList;


import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;




/**
 * WebEvent according to http://dev.w3.org/html5/eventsource/
 * 
 * <br/><br/><b>This is an experimental implementation of the HTML5 draft and subject to change</b>
 * 
 * @author grro@xlightweb.org
 */
public class Event implements IWebMessage {
        
    private static final Logger LOG = Logger.getLogger(Event.class.getName());
    
    private static final char COLON = ':';
    private static final char SPACE = ' ';
    private static final char CR = '\r';
    private static final char LF = '\n';

    private String eventname;
    private String id;
    private Integer retry;
    private final List<String> dataList = new ArrayList<String>();
    private final List<String> commentList = new ArrayList<String>();


    
    public Event() {
        
    }

    public Event(String data) {
        addData(data);
    }
    
    
    public Event(String data, long id) {
        this(data, Long.toString(id));
    }

    
    public Event(String data, String id) {
        addData(data);
        setId(id);
    }

    
    public Event(String data, long id, String eventname) {
       this(data, Long.toString(id), eventname);
    }

    public Event(String data, String id, String eventname) {
        addData(data);
        setId(id);
        setEventname(eventname);
    }
    
    
    public boolean isCommentMessage() {
        return (getData() == null) && (getEventname() == null);  
    }
    
    
    /**
     * returns the event name
     * @return the event name or <code>null</code>
     */
    public String getEventname() {
        return eventname;
    }

    
    /**
     * sets the event name
     * @param the event name or <code>null</code>
     */
    public void setEventname(String eventname) {
        this.eventname = eventname;
    }
    
    /**
     * returns the event id 
     * @return the event id or <code>null</code>
     */
    public String getId() {
        return id;
    }
    
    
    /**
     * sets the event id  
     * @param id the event id or <code>null</code>
     */
    public void setId(String id) {
        this.id = id;
    }
    
    
    /**
     * returns the retry timeout 
     * @return the retry timeout or <code>null</code>
     */
    public Integer getRetryMillis() {
        return retry;
    }
    
    /**
     * sets the retry timeout 
     * @param retry the retry timeout or <code>null</code>
     */
    public void setRetryMillis(Integer retry) {
        this.retry = retry;
    }
    
    
    
    /**
     * adds the data 
     * @param the data 
     */
    public void addData(String data) {
        if (data != null) {
            dataList.add(data);
        }
    }


    /**
     * sets the data 
     * @param the data 
     */
    public void setData(String data) {
        dataList.clear();
        if (data != null) {
            addData(data);
        }
    }

    
    /**
     * returns the merged data 
     * @return the merged data or <code>null</code>
     */
    public String getData() {
        if (dataList.isEmpty()) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();
            for (String t : dataList) {
                sb.append(t);
            }
            
            return sb.toString();
        }
    }


   
    
    /**
     * returns the merged comments 
     * @return the merged comments or <code>null</code>
     */
    public String getComment() {
        if (commentList.isEmpty()) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();
            for (String t : commentList) {
                sb.append(t);
            }
            
            return sb.toString();
        }
    }
    
    public void addComment(String comment) {
        if (comment != null) {
            commentList.add(comment);
        }
    }
    
    public void setComment(String comment) {
        commentList.clear();
        addComment(comment);   
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (String s : commentList) {
            sb.append(": " + s + "\r\n");
        }
        
        if (eventname != null) {
            sb.append("event: " + eventname + "\r\n");
        }
        
        if (id != null) {
            sb.append("id: " + id + "\r\n");
        }
        
        if (retry != null) {
            sb.append("retry: " + retry + "\r\n");
        }

        for (String s : dataList) {
            sb.append("data: " + s + "\r\n");
        }
            
        sb.append("\r\n");
        return sb.toString();
    }
    
    
    /**
     * parses a event
     *  
     * @param event  the event string representation 
     * @return the web event 
     */
    public static Event parse(String event) {

        Event webEvent = new Event();

        if (event != null) {
                    
            String field = null;
            String value = null;
            
            boolean colonFound = false;
            boolean isComment = false;
            boolean isScanningForSpace = false;
            
            int offset = 0;
            for (int i = 0; i < event.length(); i++) {
                
                char c = event.charAt(i);
    
                
                // end of line reached?
                if ((c == CR) || (c == LF) || ((i + 1) == event.length())) {
                    
                    if ((i + 1) == event.length()) {
                        i++;
                    }
                    
                    if (isComment) {
                        webEvent.commentList.add(event.substring(offset, i));
                        
                    } else {
                        if (field == null) {
                            field = event.substring(offset, i).trim();
                        } else {
                            value = event.substring(offset, i);
                        }
                        
                        if (field.equalsIgnoreCase("event")) {
                            if (webEvent == null) {
                                webEvent = new Event();
                            }
                            webEvent.eventname = value;
                            
                        } else if (field.equalsIgnoreCase("id")) {
                            if (webEvent == null) {
                                webEvent = new Event();
                            }
                            webEvent.id = value;
                        
                        } else if (field.equalsIgnoreCase("retry")) {
                            if (webEvent == null) {
                                webEvent = new Event();
                            }
                            try {
                                webEvent.retry = Integer.parseInt(value);
                            } catch (NumberFormatException nfe) { 
                                if (LOG.isLoggable(Level.FINE)) {
                                    LOG.fine("illegal format for retry field. ignoring it");
                                }
                            }
                            
                        } else if (field.equalsIgnoreCase("data")) {
                            if (webEvent == null) {
                                webEvent = new Event();
                            }
                            webEvent.dataList.add(value);
                            
                        } else {
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.fine("unknown field \"" + field + "\". ignoring it");
                            }
                        }
                    }
    
                    // CRLF?
                    if ((c == CR) && ((i+1) < event.length()) && (event.charAt(i + 1) == LF)) {
                        i++;
                    }
                    
                    // END of event?
                    // CRLFCRLF?
                    if ((c == CR) && ((i+2) < event.length()) && (event.charAt(i + 1) == CR) && (event.charAt(i + 2) == LF)) {
                        return webEvent;
                    }
                    
                    // CRCR?
                    if ((c == CR) && ((i+1) < event.length()) && (event.charAt(i + 1) == CR)) {
                        return webEvent;
                    }                    
                    
                    // LFLF?
                    if ((c == LF) && ((i+1) < event.length()) && (event.charAt(i + 1) == LF)) {
                        return webEvent;
                    }
                    
                    offset = i + 1;
                    isComment = false;
                    isScanningForSpace = false;
                    colonFound = false;
                    
                // (first) colon?    
                } else if ((c == COLON) && (!colonFound)) {
                    colonFound = true; 
                    
                    // comment?
                    if (offset == i) {
                        isComment = true;
                        
                    } else {
                        field = event.substring(offset, i).trim();
                    }
                    
                    isScanningForSpace = true;
                    offset = i + 1;
    
                    
                // space after COLON
                } else if (isScanningForSpace) {
                    if (c == SPACE) {
                        offset++;
                    } else {
                        isScanningForSpace = false;
                    }
                }
            }
        }
        
        return webEvent;
    }              
}
