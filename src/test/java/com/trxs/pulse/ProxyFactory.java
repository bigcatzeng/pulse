package com.trxs.pulse;

import com.trxs.pulse.jdbc.BaseService;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class ProxyFactory implements MethodInterceptor
{
    public final static Logger logger = LoggerFactory.getLogger(ProxyFactory.class);

    private Object target;

    public ProxyFactory(Object target)
    {
        this.target = target;
    }

    public Object getProxyInstance()
    {
        // 1.工具类
        Enhancer en = new Enhancer();

        // 2.设置父类
        en.setSuperclass(target.getClass());

        // 3.设置回调函数
        en.setCallback(this);

        //4.创建子类(代理对象)
        return en.create();
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable
    {
        if ( method.getReturnType() == null )
        {
            logger.debug("开始事务...");
            method.invoke(target, objects);
            logger.debug("提交事务...");
            return null;
        }

        //执行目标对象的方法
        logger.debug("开始事务...");
        Object returnValue = method.invoke(target, objects);
        logger.debug("提交事务...");

        return returnValue;
    }

    public static void test()
    {
        //目标对象
        //UserDao target = new UserDao();
        //代理对象
        //UserDao proxy = (UserDao)new ProxyFactory(target).getProxyInstance();
        //执行代理对象的方法
        //proxy.save();
    }
}
