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


import java.util.Set;



/**
 * Multi value map definition
 * 
 * @author grro@xlightweb.org
 */
public interface IMultivalueMap  {
    
    
    /**
     * sets a parameter.   
     * 
     * @param name   the name 
     * @param value  the value
     */
    public void setParameter(String name, String value);
    

    /**
     * adds an parameter
     *  
     * @param name  the name
     * @param value the value
     */
    public void addParameter(String name, String value);

    
    /**
     * remove a parameter
     *  
     * @param name  the name
     */
    public void removeParameter(String name);
    
    

    
    /**
     * returns the parameter name set
     * 
     * @return the parameter name set
     */
    public Set<String> getParameterNameSet();
    
    
    /**
     * returns the parameter values
     * 
     * @param name  the parameter name
     * @return the parameter values 
     */
    public String[] getParameterValues(String name);
    
    
    /**
     * returns the first parameter value 
     * 
     * @param name  the parameter name
     * @return the first parameter value 
     */
    public String getParameter(String name);
    
    
    /**
     * returns the first parameter value 
     * 
     * @param name  the parameter name
     * @return the first parameter value 
     */
    public Integer getIntParameter(String name);
  

    /**
     * returns the first parameter value 
     * 
     * @param name        the parameter name
     * @param defaultVal  the default value
     * 
     * @return the first parameter value  
     */
    public int getIntParameter(String name, int defaultVal);  
    
    
    /**
     * returns the first parameter value 
     * 
     * @param name  the parameter name
     * @return the first parameter value 
     */
    public Long getLongParameter(String name);
    
    
    
    
    /**
     * returns the first parameter value 
     * 
     * @param name        the parameter name
     * @param defaultVal  the default value 
     * @return the first parameter value 
     */
    public long getLongParameter(String name, long defaultVal);
    
    
  
    /**
     * returns the first parameter value 
     * 
     * @param name  the parameter name
     * @return the first parameter value 
     */
    public Double getDoubleParameter(String name);
    
    
    /**
     * returns the first parameter value 
     * 
     * @param name        the parameter name
     * @param defaultVal  the default value 
     * @return the first parameter value 
     */
    public double getDoubleParameter(String name, double defaultVal);
    
    
    /**
     * returns the first parameter value 
     * 
     * @param name  the parameter name
     * @return the first parameter value 
     */
    public Float getFloatParameter(String name);
    
    
    /**
     * returns the first parameter value 
     * 
     * @param name        the parameter name
     * @param defaultVal  the default value 
     * @return the first parameter value 
     */
    public float getFloatParameter(String name, float defaultVal);
    
    
    
    /**
     * returns the first parameter value 
     * 
     * @param name  the parameter name
     * @return the first parameter value 
     */
    public Boolean getBooleanParameter(String name);
    
    
    /**
     * returns the first parameter value 
     * 
     * @param name        the parameter name
     * @param defaultVal  the default value
     * @return the first parameter value 
     */
    public boolean getBooleanParameter(String name, boolean defaultVal);
    
}

 