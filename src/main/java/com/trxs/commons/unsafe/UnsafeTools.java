package com.trxs.commons.unsafe;

import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeTools
{
    public static Unsafe getUnsafe()
    {
        try
        {
            Class<?> unsafeClass = Unsafe.class;
            for (Field f : unsafeClass.getDeclaredFields())
            {
                if ("theUnsafe".equals(f.getName()))
                {
                    f.setAccessible(true);
                    return (Unsafe) f.get(null);
                }
            }
            LoggerFactory.getLogger(UnsafeTools.class).debug("no declared field: theUnsafe");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }
}
