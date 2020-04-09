package com.trxs.commons.bean;

import com.esotericsoftware.reflectasm.MethodAccess;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BeanAccess
{
    private static Map<Class, MethodAccess> methodAccessMap = new ConcurrentHashMap<>(64);

    private Object self;
    private MethodAccess methodAccess;

    private String[] setMethodNames;
    private String[] getMethodNames;

    public BeanAccess(Object obj)
    {
        self = obj;
        final Class<?> objClass = self.getClass();
        methodAccess = methodAccessMap.get(objClass);
        if ( methodAccess == null )
        {
            methodAccess = MethodAccess.get(obj.getClass());
            methodAccessMap.put(objClass, methodAccess);
        }

        setMethodNames = new String[methodAccess.getMethodNames().length];
        getMethodNames = new String[methodAccess.getMethodNames().length];

        for ( int i = 0; i < methodAccess.getMethodNames().length; ++i )
        {
            char []nameChars = methodAccess.getMethodNames()[i].toCharArray();
            nameChars[3] = Character.toLowerCase(nameChars[3]);

            if( methodAccess.getMethodNames()[i].startsWith("get") )
                getMethodNames[i] = new String( nameChars, 3, nameChars.length-3 );
            else
                getMethodNames[i] = "";

            if ( methodAccess.getMethodNames()[i].startsWith("set") )
                setMethodNames[i] = new String( nameChars, 3, nameChars.length-3 );
            else
                setMethodNames[i] = "";
        }
        return;
    }

    public int indexForGetPropertyByName(String name)
    {
        for ( int i = 0; i < getMethodNames.length; ++i )
        {
            if ( name.equals(getMethodNames[i]) ) return i;
        }
        return -1;
    }

    public int indexForGetPropertyByName(String name, int defaultValue )
    {
        for ( int i = 0; i < getMethodNames.length; ++i )
        {
            if ( name.equals(getMethodNames[i]) ) return i;
        }
        return defaultValue;
    }

    public int indexForSetPropertyByName(String name)
    {
        for ( int i = 0; i < setMethodNames.length; ++i )
        {
            if ( name.equals(setMethodNames[i]) ) return i;
        }
        return -1;
    }

    public Object getValueByIndex(int index)
    {
        return methodAccess.invoke(self, index);
    }

    public BeanAccess setValueByIndex(int index, Object value)
    {
        methodAccess.invoke(self, index, value);
        return this;
    }

    public Object getValueByIndex(Object bean, int index)
    {
        return methodAccess.invoke(bean, index);
    }

    public BeanAccess setValueByIndex(Object bean, int index, Object value)
    {
        methodAccess.invoke(bean, index, value);
        return this;
    }
}
