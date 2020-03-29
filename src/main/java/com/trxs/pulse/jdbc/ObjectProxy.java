package com.trxs.pulse.jdbc;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class ObjectProxy implements MethodInterceptor
{

    private Object target;

    public ObjectProxy(Object obj)
    {
        target = obj;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable
    {
        return method.invoke(target,objects);
    }
}
