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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;




/**
 * A multivalue map
 * 
 * @author grro@xlightweb.org
 */
public class MultivalueMap implements IMultivalueMap {
    
    private static final Logger LOG = Logger.getLogger(MultivalueMap.class.getName());

    private static final Boolean NULL_BOOLEAN = null;
    private static final String NULL = UUID.randomUUID().toString();
    
    private final Map<String, List<String>> multivalueMap = new HashMap<String, List<String>>();
    private final String encoding;

    private String entity;


    
    /**
     * constructor
     * 
     * @param decodedFormParameter   the decoded form parameters 
     */
    public MultivalueMap(NameValuePair decodedFormParameter) {
    	this("utf-8", new NameValuePair[] { decodedFormParameter });
    }
    
    
    /**
     * constructor
     * 
     * @param encoding               the encoding to use
     * @param decodedFormParameter   the decoded form parameters 
     */
    public MultivalueMap(String encoding) {
    	this(encoding, (NameValuePair) null);
    }


    /**
     * constructor
     * 
     * @param decodedFormParameters   the decoded form parameters 
     */
    public MultivalueMap(NameValuePair... decodedFormParameters) {
    	this("utf-8", decodedFormParameters);
    }

    
    
    /**
     * constructor
     * 
     * @param encoding the encoding to use
     * @param decodedFormParameters   the decoded form parameters 
     */
    public MultivalueMap(String encoding, NameValuePair... decodedFormParameters) {
        this.encoding = encoding;
        
        for (NameValuePair nvp : decodedFormParameters) {
        	if (nvp != null) {
	        	if (nvp.getValue() == null) {
	        		addParameter(nvp.getName(), NULL);
	        	} else {
	        		addParameter(nvp.getName(), nvp.getValue());
	        	}
        	}
        }
    }
    
    
    /**
     * constructo  r
     * 
     * @param bodyDataSource    the entity
     * 
     * @throws IOException if the body parsing fails
     */
    public MultivalueMap(BodyDataSource bodyDataSource) throws IOException {
        this(bodyDataSource.getEncoding(), bodyDataSource.readString()); 
    }
    
    
    
    /**
     * constructor
     * 
     * @param encoding the encoding to use
     * @param decodedFormParameters   the decoded form parameters 
     */
    public MultivalueMap(String encoding, String... decodedFormParameters) {
        this.encoding = encoding;
        
        for (String kvp : decodedFormParameters) {

            int idx = kvp.indexOf("=");
            if (idx > 0) {
                String name = kvp.substring(0, idx);
                String value = kvp.substring(idx + 1, kvp.length());
                addParameter(name, value);
                
                
            } else {
                throw new RuntimeException("illegal format: '" + kvp +  "' (usage: <key>=<value>)");
            }
        }
    }
    


    
    /**
     * constructor 
     * 
     * @param encoding     the encoding
     * @param serialized   the serialized representation (mime type application/x-www-form-urlencoded)
     * @throws IOException if an exception occurs
     */
    public MultivalueMap(String encoding, String serialized) throws IOException {
        this.encoding = encoding; 
        
        for (String kvp : serialized.split("&")) {
            
            int idx = kvp.indexOf("=");
            if (idx > 0) {
                String name = kvp.substring(0, idx).trim();
                String value = kvp.substring(idx + 1, kvp.length());
                
                if (value.length() == 0) {
                    try {
                        addParameter(URLDecoder.decode(name, encoding), NULL);
                    } catch (UnsupportedEncodingException usec) {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("error occured by decoding param " + name + "=" + value + " " + usec.toString() + " ignoring it");
                        }
                    }
                } else {
                    try {
                        addParameter(URLDecoder.decode(name, encoding), URLDecoder.decode(value, encoding));
                    } catch (UnsupportedEncodingException usec) {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("error occured by decoding param " + name + "=" + value + " " + usec.toString() + " ignoring it");
                        }
                    }
                }
            } else {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("invalid format: " + kvp +  " ignoring value");
                }
            }
        }
        
    }
    
    public final void setParameter(String name, String value) {
        if (value == null) {
            value = NULL;
        }
        removeParameter(name);
        addParameter(name, value);
    }
    
    
    public final void removeParameter(String name) {
        entity = null;
        
        multivalueMap.remove(name);
    }
    
    
    public final void addParameter(String name, String value) {
        entity = null;
        
        if (value == null) {
            value = NULL;
        }

        List<String> values = multivalueMap.get(name);
        if (values == null) {
            values = new ArrayList<String>();
            multivalueMap.put(name, values);
        }
        if (value != null) {
            values.add(value);
        }
    }
    
    
    /**
     * returns the parameter name set
     * 
     * @return the parameter name set
     */
    public final Set<String> getParameterNameSet() {
        return Collections.unmodifiableSet(multivalueMap.keySet());
    }

    
    /**
     * returns the parameter values
     * 
     * @param name  the parameter name
     * @return the parameter values 
     */
    public final String[] getParameterValues(String name) {
        List<String> values = multivalueMap.get(name);
        
        if (values == null) {
            return null;
        } else {
            return values.toArray(new String[values.size()]);
        }
    }
    
    
    /**
     * returns the first parameter value 
     * 
     * @param name  the parameter name
     * @return the first parameter value 
     */
    public final String getParameter(String name) {
        List<String> values = multivalueMap.get(name);
        
        if ((values == null) || (values.isEmpty())) {
            return null;
        } else {
            String value = values.get(0);
            if (value.equals(NULL)) {
                return null;
            } else {
                return value;
            }
        }
    }
 
    
    /**
     * returns the first parameter value 
     * 
     * @param name  the parameter name
     * @return the first parameter value 
     */
    public final Integer getIntParameter(String name) {
        String s = getParameter(name);
        if (s != null) {
            return Integer.parseInt(s);
        } else {
            return null;
        }
    }
  

    /**
     * returns the first parameter value 
     * 
     * @param name        the parameter name
     * @param defaultVal  the default value
     * 
     * @return the first parameter value  
     */
    public final int getIntParameter(String name, int defaultVal) {
        String s = getParameter(name);
        if (s != null) {
            try {
                return Integer.parseInt(s);
            } catch (Exception e) {
                return defaultVal;
            }
        } else {
            return defaultVal;
        }
    }

    
  
    
    /**
     * returns the first parameter value 
     * 
     * @param name  the parameter name
     * @return the first parameter value 
     */
    public final Long getLongParameter(String name) {
        String s = getParameter(name);
        if (s != null) {
            return Long.parseLong(s);
        } else {
            return null;
        }
    }
    
    
    
    
    /**
     * returns the first parameter value 
     * 
     * @param name        the parameter name
     * @param defaultVal  the default value 
     * @return the first parameter value 
     */
    public final long getLongParameter(String name, long defaultVal) {
        String s = getParameter(name);
        if (s != null) {
            try {
                return Long.parseLong(s);
            } catch (Exception e) {
                return defaultVal;
            }
        } else {
            return defaultVal;
        }
    }
    
    
  
    /**
     * returns the first parameter value 
     * 
     * @param name  the parameter name
     * @return the first parameter value 
     */
    public final Double getDoubleParameter(String name) {
        String s = getParameter(name);
        if (s != null) {
            return Double.parseDouble(s);
        } else {
            return null;
        }
    }
    
    
    /**
     * returns the first parameter value 
     * 
     * @param name        the parameter name
     * @param defaultVal  the default value 
     * @return the first parameter value 
     */
    public final double getDoubleParameter(String name, double defaultVal) {
        String s = getParameter(name);
        if (s != null) {
            try {
                return Double.parseDouble(s);
            } catch (Exception e) {
                return defaultVal;
            }
        } else {
            return defaultVal;
        }
    }
    
   
    /**
     * returns the first parameter value 
     * 
     * @param name  the parameter name
     * @return the first parameter value 
     */
    public final Float getFloatParameter(String name) {
        String s = getParameter(name);
        if (s != null) {
            return Float.parseFloat(s);
        } else {
            return null;
        }
    }
    
    
    /**
     * returns the first parameter value 
     * 
     * @param name        the parameter name
     * @param defaultVal  the default value 
     * @return the first parameter value 
     */
    public final float getFloatParameter(String name, float defaultVal) {
        String s = getParameter(name);
        if (s != null) {
            try {
                return Float.parseFloat(s);
            } catch (Exception e) {
                return defaultVal;
            }
        } else {
            return defaultVal;
        }
    }
    
    
    
    /**
     * returns the first parameter value 
     * 
     * @param name  the parameter name
     * @return the first parameter value 
     */
    public final Boolean getBooleanParameter(String name) {
        String s = getParameter(name);
        if (s != null) {
            return Boolean.parseBoolean(s);
        } else {
            return NULL_BOOLEAN;
        }
    }
    
    
    
    /**
     * returns the first parameter value 
     * 
     * @param name        the parameter name
     * @param defaultVal  the default value
     * @return the first parameter value 
     */
    public final boolean getBooleanParameter(String name, boolean defaultVal) {
        String s = getParameter(name);
        if (s != null) {
            try {
                return Boolean.parseBoolean(s);
            } catch (Exception e) {
                return defaultVal;
            }
        } else {
            return defaultVal;
        }
    }
   
    
    
    
    /**
     * returns the serialized representation of the map (mime type application/x-www-form-urlencoded)
     * 
     * @return the serialized representation of the map  
     */
    @Override
    public String toString() {
 
        if (entity == null) {

            StringBuilder sb = new StringBuilder();
            
            for (Entry<String, List<String>> entry : multivalueMap.entrySet()) {
                
                for (String value : entry.getValue()) {
                    if (value.equals(NULL)) {
                        try {
                            sb.append(URLEncoder.encode(entry.getKey(), encoding) + "=&");
                        } catch (UnsupportedEncodingException usec) {
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.fine("error occured by encoding param " + entry.getKey() + "=" + value + " " + usec.toString() + " ignoring it");
                            }
                        }
                    } else {
                        try {
                            sb.append(URLEncoder.encode(entry.getKey(), encoding) + "="  + URLEncoder.encode(value, encoding) + "&");
                        } catch (UnsupportedEncodingException usec) {
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.fine("error occured by encoding param " + entry.getKey() + "=" + value + " " + usec.toString() + " ignoring it");
                            }
                        }
                    }
                }
            }
            
            entity = sb.toString();
            if (entity.length() > 0) {
                entity = entity.substring(0, entity.length() - 1);
            }
        }
        
        return entity;
    }
}

 