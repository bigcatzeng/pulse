package com.trxs.commons.bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;

public class BeanTools
{
    protected static Logger logger = LoggerFactory.getLogger(BeanTools.class.getName());


    public static Object clone( Object toObject, Object srcObject, String ...skipProperties)
    {
        if ( toObject == null || srcObject == null ) return toObject;

        AccessObject toObjectAccess = new AccessObject(toObject);
        AccessObject srcObjectAccess = new AccessObject(srcObject);

        Arrays.asList(toObjectAccess.getSetMethodNames()).forEach(setMethodName ->
        {
            String propertyName = setMethodName.substring(3);
            if ( inList(skipProperties, propertyName) ) return;
            toObjectAccess.setProperty(propertyName, srcObjectAccess.getProperty(propertyName));
        });
        return toObject;
    }

    private static boolean inList(String []values, String check )
    {
        if ( values == null || check == null ) return false;
        for ( int i = 0; i < values.length; ++i ) if ( values[i].equalsIgnoreCase(check) ) return true;
        return false;
    }

    public static Object clone( Object toObject, Object srcObject )
    {
        if ( toObject == null || srcObject == null ) return toObject;

        AccessObject toObjectAccess = new AccessObject(toObject);
        AccessObject srcObjectAccess = new AccessObject(srcObject);

        Arrays.asList(toObjectAccess.getSetMethodNames()).forEach( setMethodName ->
        {
            String propertyName = setMethodName.substring(3);
            toObjectAccess.setProperty(propertyName, srcObjectAccess.getProperty(propertyName));
        });
        return toObject;
    }

    public static void clone( AccessObject toObjectAccess, AccessObject srcObjectAccess )
    {
        Objects.requireNonNull(toObjectAccess);
        Objects.requireNonNull(srcObjectAccess);
        Arrays.asList(toObjectAccess.getSetMethodNames()).forEach( setMethodName ->
        {
            String propertyName = setMethodName.substring(3);
            Object value = srcObjectAccess.getProperty(propertyName);
            logger.debug("propertyName:{}, value:{}", propertyName, value);
            toObjectAccess.setProperty(propertyName, value);
        });
    }
}
