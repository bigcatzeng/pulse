package com.trxs.pulse.jdbc;

import net.sf.cglib.proxy.Enhancer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectProxyFactory
{
    private static Map<Thread, Enhancer> threadEnhancerMap = new ConcurrentHashMap<>();

    public static <T>T createProxy(T target)
    {
        Thread self = Thread.currentThread();
        Enhancer enhancer = threadEnhancerMap.get(self); // new Enhancer();
        if ( enhancer == null )
        {
            enhancer = new Enhancer();
            threadEnhancerMap.put(self, enhancer);
        }
        ObjectProxy objectProxy = new ObjectProxy(target);
        enhancer.setSuperclass(target.getClass());
        enhancer.setCallback(objectProxy);
        return (T) enhancer.create();
    }
}
