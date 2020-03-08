package com.trxs.commons.bean;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.trxs.commons.util.DateConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AccessObject
{
    protected static Logger logger = LoggerFactory.getLogger(AccessObject.class.getName());

    private Object self;
    private MethodAccess methodAccess;

    private String[] getProperties;
    private String[] setMethodNames;

    private String[] setProperties;
    private String[] getMethodNames;

    private static Map<Class, MethodAccess> methodAccessMap = new ConcurrentHashMap<>(32);

    public AccessObject(Object obj)
    {
        self = obj;
        final Class<?> objClass = self.getClass();
        initMethodAccess(objClass);
        initMethodNames(objClass);
    }

    private void initMethodAccess(final Class<?> objClass)
    {
        methodAccess = methodAccessMap.get(objClass);
        if ( methodAccess != null ) return;
        methodAccess = MethodAccess.get(objClass);
        methodAccessMap.put(objClass, methodAccess);
    }

    private void initMethodNames(final Class<?> objClass)
    {
        Method[] methods = objClass.getMethods();

        List<Method> setMethods = Arrays.asList(methods).stream().filter( method -> method.getName().length() > 3 && method.getName().startsWith("set")).collect(Collectors.toList());
        setMethodNames = new String[setMethods.size()];
        setProperties = new String[setMethods.size()];

        for ( int i = 0, max = setMethods.size(); i < max; ++i )
        {
            setProperties [i] = String.join("", setMethods.get(i).getName().substring(3, 4).toLowerCase(), setMethods.get(i).getName().substring(4) );
            setMethodNames[i] = setMethods.get(i).getName();
        }

        List<Method> getMethods = Arrays.asList(methods).stream().filter( method -> method.getName().length() > 3 && method.getName().startsWith("get") && ! method.getName().equals("getClass") ).collect(Collectors.toList());
        getMethodNames = new String[getMethods.size()];
        getProperties = new String[getMethods.size()];

        for ( int i = 0, max = getMethods.size(); i < max; ++i )
        {
            getProperties [i] = String.join("", getMethods.get(i).getName().substring(3, 4).toLowerCase(), getMethods.get(i).getName().substring(4) );
            getMethodNames[i] = getMethods.get(i).getName();
        }

        return;
    }

    // 属性名称首字母不区分大小写
    public Object getProperty(String propertyName)
    {
        int index = findPropertyIndex( propertyName, getProperties );

        return methodAccess.invoke(self, getMethodNames[index] );
    }

    public int findPropertyIndex( String propertyName, String[]properties )
    {
        for ( int i = 0; i < properties.length; ++i ) if ( properties[i].equalsIgnoreCase(propertyName) ) return i;
        return -1;
    }

    public AccessObject setProperty(String propertyName, Object value)
    {
        int index = findPropertyIndex( propertyName, setProperties );

        if ( index < 0 )
        {
            logger.warn("Can't found the {}->{} !!!", self.getClass().getName(), propertyName );
            return this;
        }

        Class[] parameterTypes = methodAccess.getParameterTypes()[methodAccess.getIndex(setMethodNames[index])];

        if ( value == null )
        {
            methodAccess.invoke(self, setMethodNames[index], null);
        }
        else if ( parameterTypes[0].getName().equals(value.getClass().getName()) )
        {
            methodAccess.invoke(self, setMethodNames[index], value);
        }
        else if ( value instanceof Long && parameterTypes[0].getName().equals("java.util.Integer"))
        {
            methodAccess.invoke(self, setMethodNames[index], long2int((Long) value));
        }
        else if ( value instanceof Timestamp )
        {
            if ( parameterTypes[0].getName().equals("java.util.Date") )
            {
                methodAccess.invoke(self, setMethodNames[index], value);
            }
            if ( parameterTypes[0].getName().equals("java.util.String") )
            {
                methodAccess.invoke(self, setMethodNames[index], DateConverter.dateFormat((Date) value, "yyyy-MM-dd HH:mm:ss"));
            }
        }
        else if ( value instanceof List && parameterTypes[0].getName().equals("java.util.List"))
        {
            methodAccess.invoke(self, setMethodNames[index], value);
        }
        else
        {
            logger.warn("The propertyType[{}]->{} is not equals valueType[{}]!!!", parameterTypes[0].getName(), propertyName, value.getClass().getName());
        }

        return this;
    }

    private Integer long2int( long l )
    {
        try
        {
            return Math.toIntExact(l);
        }
        catch (Exception e )
        {
            e.printStackTrace();
        }
        return null;
    }

    public static Logger getLogger() {
        return logger;
    }

    public String[] getGetProperties() {
        return getProperties;
    }

    public String[] getSetMethodNames() {
        return setMethodNames;
    }

    public String[] getSetProperties() {
        return setProperties;
    }

    public String[] getGetMethodNames() {
        return getMethodNames;
    }
}