package com.trxs.pulse;

import com.fel.Expression;
import com.fel.common.FelBuilder;
import com.fel.context.AbstractContext;
import com.fel.context.ContextChain;
import com.fel.context.MapContext;
import com.fel.FelEngine;
import com.fel.context.FelContext;
import com.fel.security.SecurityMgr;
import com.fel.security.SecurityMgrImpl;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FelTests
{
    @Test
    public void test1()
    {
        Foo foo1 = new Foo();
        Foo foo2 = new Foo();

        foo1.setChildren(foo2);
        foo1.setName("Zeng "); foo1.setSize(6);
        foo2.setName("Ethan"); foo2.setSize(9);

        FelEngine fel = FelBuilder.bigNumberEngine();

        SecurityMgr securityMgr = fel.getSecurityMgr();

//        fel.setSecurityMgr(new SecurityMgrImpl());

        String input = "111111111111111111111111111111+22222222222222222222222222222222";
        Object value = fel.eval(input);
        Object compileValue = fel.compile(input, fel.getContext()).eval(fel.getContext());
        System.out.println("大数值计算（解释执行）:" + value);
        System.out.println("大数值计算（编译执行）:" + compileValue);

        FelContext ctx = fel.getContext();
        ctx.set("foo1", foo1);
        value = fel.eval("foo1.children.name");
        System.out.println("children:" + value);

        fel.eval("$(System).exit(1)" );

        return;
    }
}
