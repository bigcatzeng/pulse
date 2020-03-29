package com.trxs.pulse;

import com.fel.Expression;
import com.fel.FelEngineImpl;
import com.fel.common.FelBuilder;
import com.fel.common.ObjectUtils;
import com.fel.context.AbstractContext;
import com.fel.context.ContextChain;
import com.fel.context.MapContext;
import com.fel.FelEngine;
import com.fel.context.FelContext;
import com.fel.function.CommonFunction;
import com.fel.function.Function;
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

        String exp = "foo1.children.name == null";
        Expression expression = fel.compile(exp, ctx);

        long t0 = System.currentTimeMillis();
        for ( int i = 0; i < 1000000; ++i ) value = expression.eval(ctx);
        long t1 = System.currentTimeMillis();

        System.out.println("children:" + value);

        long dt = t1 -  t0;

        System.out.println("dt:" + dt);

        System.out.println("test:" + fel.eval("4444" ));
        //fel.eval("$(System).exit(1)" );

        return;
    }

    @Test
    public void test2()
    {
        collections();
    }

    private static void calculate() {
        // 算数运算
        FelEngine fel = new FelEngineImpl();
        Object result = fel.eval("1.5898*1+75");
        System.out.println(result);

        // 逻辑运算
        Object result2 = fel.eval("1 == 2 || '1'.equals('1')");
        System.out.println(result2);
    }

    private static void object() {
        FelEngine fel = new FelEngineImpl();
        FelContext ctx = fel.getContext();
        Foo user = new Foo(1, "中国北京");
        ctx.set("user", user);

        Map<String, String> map = new HashMap<String, String>();
        map.put("name", "qqq");

        ctx.set("map", map);

        // 调用user.getName()方法。
        System.out.println(fel.eval("user.name"));

        // map.name会调用map.get("name");
        System.out.println(fel.eval("map.name"));
    }

    private static void staticMethod() {
        // 调用Math.min(1,2)
        Object eval = FelEngine.instance.eval("$('Math').min(1,2)");
        System.out.println(eval);

        // 调用Stringutils的方法
        Object eval1 = FelEngine.instance.eval("$('org.apache.commons.lang.StringUtils').isEmpty('123')");
        System.out.println(eval1);
    }

    // 访问集合
    public static void collections() {
        FelEngine fel = new FelEngineImpl();
        FelContext ctx = fel.getContext();

        // 数组
        int[] intArray = { 1, 2, 3 };
        ctx.set("intArray", intArray);
        // 获取intArray[0]
        String exp = "intArray[0]";
        System.out.println(exp + "->" + fel.eval(exp));

        // List
        List<Integer> list = Arrays.asList(1, 2, 3);
        ctx.set("list", list);
        // 获取list.get(0)
        exp = "list[0]";
        System.out.println(exp + "->" + fel.eval(exp));

        // 集合
        Collection<String> coll = Arrays.asList("a", "b", "c");
        ctx.set("coll", coll);
        // 获取集合最前面的元素。执行结果为"a"
        exp = "coll[0]";
        System.out.println(exp + "->" + fel.eval(exp));

        // 迭代器
        Iterator<String> iterator = coll.iterator();
        ctx.set("iterator", iterator);
        // 获取迭代器最前面的元素。执行结果为"a"
        exp = "iterator[0]";
        System.out.println(exp + "->" + fel.eval(exp));

        // Map
        Map<String, String> m = new HashMap<String, String>();

        Object objMap = m;

        m.put("name", "HashMap");
        ctx.set("map", objMap);
        exp = "map.name";
        System.out.println(exp + "->" + fel.eval(exp));

        // 多维数组
        int[][] intArrays = { { 11, 12 }, { 21, 22 } };
        ctx.set("intArrays", intArrays);
        exp = "intArrays[0][0]";
        System.out.println(exp + "->" + fel.eval(exp));

        // 多维综合体，支持数组、集合的任意组合。
        List<int[]> listArray = new ArrayList<int[]>();
        listArray.add(new int[] { 1, 2, 3 });
        listArray.add(new int[] { 4, 5, 6 });
        ctx.set("listArray", listArray);
        exp = "listArray[0][0]";
        System.out.println(exp + "->" + fel.eval(exp));
    }

    private static void compile() {
        FelEngine fel = new FelEngineImpl();
        FelContext ctx = fel.getContext();
        ctx.set("单价", 1.5898);
        ctx.set("数量", 1);
        ctx.set("运费", 75);
        Expression exp = fel.compile("单价*数量+运费", ctx);
        Object result = exp.eval(ctx);
        System.out.println(result);
    }

    private static void myContext() {
        FelContext ctx = new AbstractContext() {
            @Override
            public Object get(String arg0) {
                System.out.println(arg0);
                return "111222";
            }
        };

        FelEngine fel = new FelEngineImpl(ctx);
        Object eval = fel.eval("天气 + 温度");
        System.out.println(eval);
    }

    private static void newFun()
    {
        // 定义hello函数
        Function fun = new CommonFunction()
        {
            public String getName()
            {
                return "hello";
            }

            @Override
            public Object call(Object[] arguments)
            {
                Object msg = null;
                if (arguments != null && arguments.length > 0)
                {
                    msg = arguments[0];
                }
                return ObjectUtils.toString(msg);
            }

        };

        FelEngine e = new FelEngineImpl();
        // 添加函数到引擎中。
        e.addFun(fun);
        String exp = "hello(23, 'fel', 1)";

        // 解释执行
        Object eval = e.eval(exp);
        System.out.println("hello " + eval);

        // 编译执行
        Expression compile = e.compile(exp, null);
        eval = compile.eval(null);
        System.out.println("hello " + eval);
    }
}
