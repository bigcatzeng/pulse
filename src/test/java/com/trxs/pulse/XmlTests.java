package com.trxs.pulse;

import com.alibaba.fastjson.JSON;
import com.fel.Expression;
import com.fel.FelEngineImpl;
import com.trxs.commons.bean.BeanAccess;
import com.trxs.commons.util.ConcurrentObjectStack;
import com.trxs.commons.util.ObjectStack;
import com.trxs.commons.util.TextFormatTools;
import com.trxs.pulse.jdbc.*;
import com.trxs.commons.xml.Element;
import com.trxs.sql.Tokenizer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static com.trxs.commons.io.FileTools.getBufferedReaderByString;
import static com.trxs.commons.unsafe.UnsafeTools.getUnsafe;
import static com.trxs.commons.xml.Analyser.readXmlBySource;

public class XmlTests
{
    private static Logger logger = LoggerFactory.getLogger(XmlTests.class );

    @Test
    public void test() throws IOException {
        String line1 = "我们<query id=\"modifyQuestion\" parameterName=\"question\">";
        String line2 = "\uD83D\uDE01<query id=\"modifyQuestion\" parameterName=\"question\">";

        int codePointCount1 = line1.codePointCount(0, line1.length() );
        int codePointCount2 = line2.codePointCount(0, line2.length() );

        logger.debug("{}:{}, {}:{}", line1.length(), codePointCount1, line2.length(), codePointCount2);

        Character c1 = '\uD83D';
        Character c2 = '\uDE01';

        char buffer[] = new char[1024];

        BufferedReader bf = getBufferedReaderByString(line2);

        int c;
        while ( (c = bf.read()) != -1 )
        {
            logger.debug("{}", (char) c);
        }

        logger.debug("{}, {}, {}", c1, c2, new String(buffer) );

        logger.debug("{}", line2);
    }

    @Test
    public void test0()
    {
        FelEngineImpl engine = new FelEngineImpl();

        long t0 = System.nanoTime();
        engine.getContext().set("id", Integer.valueOf(0));

        Object result = null;
        int count= 20000;
        for ( int i = 0; i < count; ++i) result = engine.eval("id != null" );

        long t1 = System.nanoTime();
        logger.debug("result={}, dt={}", result, (t1-t0)/count);
    }
    @Test
    public void test1()
    {
        Tokenizer tokenizer = new Tokenizer();

        long t0 = System.nanoTime();

        Element root = readXmlBySource("/sql/pulse.xml");

        long t1 = System.nanoTime();

        logger.debug("root name = {}, dt={}", root.getName(), (t1-t0)/1000);

        ObjectStack objectStack = new ConcurrentObjectStack<Integer>(1024 );

        for ( int i = 0; i < 1000; ++i ) objectStack.push(Integer.valueOf(i));

        t0 = System.nanoTime();
        objectStack.pop();
        t1 = System.nanoTime();

        logger.debug("Stack pop dt={}", (t1-t0)/1000);

        Map<String, Element> elementMap = new HashMap<>();

        //Element e = root.findChildrenById("");

        List<Object> objects = new ArrayList<>();

        SQLRender sqlRender = new SQLRender(root);
        Map<String, Object> context = new HashMap<>();
        context.put("id", 333);
        context.put("type", 1);
        context.put("qiType", 4);
        context.put("taskType", 4);
        context.put("planType", 4);
        context.put("list-1", objects);

        objects.add(new SsgAccountBaseLog("1s1", "1s2", "1s3", "1s4"));
        objects.add(new SsgAccountBaseLog("2s1", "2s2", "2s3", "2s4"));
        objects.add(new SsgAccountBaseLog("3s1", "3s2", "3s3", "3s4"));
        objects.add(new SsgAccountBaseLog("4s1", "4s2", "4s3", "4s4"));

        FelEngineImpl engine = new FelEngineImpl();
        String exp = "id != null";
        engine.getContext().set("id", 33);
        Expression ee = engine.compile(exp, engine.getContext());
        Object result = ee.eval(engine.getContext());

        logger.debug("result={}", result);

        SQLAction action = null;

        int count = 1;

        List<Long> timeList= new ArrayList<>(10240);
        t0 = System.nanoTime();
        timeList.add(System.nanoTime());
        for ( int i = 0; i < count; ++i)
        {
            action = sqlRender.render("addSsgAccountBaseLogWithBatch", context);
            timeList.add(System.nanoTime());
        }
        t1 = System.nanoTime();

        for ( int i = 1; i < timeList.size(); ++i )
        {
            logger.debug("dt={}\t\t{}", timeList.get(i)-timeList.get(i-1), i);
        }


        long t2, t3;
        t2 = System.nanoTime();

        Class<? extends SsgAccountBaseLog> clazz = null;
        SsgAccountBaseLog ssgAccountBaseLog = new SsgAccountBaseLog();


        ssgAccountBaseLog.setBaiduAccount("baidu1");
        ssgAccountBaseLog.setProductName("product1");
        ssgAccountBaseLog.setDataPullTime("time1");
        ssgAccountBaseLog.setMark("mark1");

        Object obj = ssgAccountBaseLog;
        BeanAccess accessBean = new BeanAccess(obj);

        int index = accessBean.indexForSetPropertyByName("baiduAccount");

        String sql=null;

        timeList.clear();
        timeList.add(System.nanoTime());
        for ( int i = 0; i < count*10; ++i )
        {
            // sql = tokenizer.zip(a.getSqlText());

            if ( index > -1 ) accessBean.setValueByIndex(index, "Hi");
            timeList.add(System.nanoTime());
        }

        for ( int i = 1; i < timeList.size(); ++i )
        {
            logger.debug("zip dt={}\t\t{}", timeList.get(i)-timeList.get(i-1), i);
        }

        return;
    }

    @Test
    public void test2()
    {
        Foo foo = new Foo();

        Foo fooNew = ObjectProxyFactory.createProxy(foo);

        fooNew.setSize(333);
        fooNew.setName("ddddd");
        logger.debug("{}, {}", fooNew.getClass().getTypeName(), JSON.toJSONString(fooNew));

        return;
    }

    @Test
    public void test3()
    {
        AtomicReference<Foo> atomicReferenceFoo = new AtomicReference<>(new Foo());

        UnaryOperator<Foo> unaryOperator = new UnaryOperator<Foo>()
        {
            @Override
            public Foo apply(Foo foo)
            {
                foo.setName("xxxx");
                return foo;
            }
        };

        long t0 = System.nanoTime();
        atomicReferenceFoo.getAndUpdate(unaryOperator);
        long t1 = System.nanoTime();

        logger.debug("dt={}", t1-t0);
        return;
    }

    @Test
    public void test4()
    {
        long t0, t1, t2;
        StringBuilder sb;
        Set<Integer> objectSet = new HashSet<>();

        t0 = System.nanoTime();
        // for ( int i = 0; i < 1; ++i )
            objectSet.add(Integer.valueOf(0));
        t1 = System.nanoTime();

        logger.debug("dt={}", t1-t0);
        boolean b;
        t1 = System.nanoTime();
        b = objectSet.contains(Integer.valueOf(333));
        t2 = System.nanoTime();

        logger.debug("dt={}, b={}", t2-t1, b);
    }

    @Test
    public void test5()
    {
        long t0, t1, t2;
//        StringBuilderObjectPool builderPool = new StringBuilderObjectPool(256);
//
//        for (int i = 0, max = builderPool.maxCapacity(); i < max; ++i) {
//            StringBuilderX stringBuilderX = new StringBuilderX(builderPool);
//            builderPool.repay(stringBuilderX);
//        }
//
//        long t0, t1, t2;
//
//        t0 = System.nanoTime();
//        StringBuilderX stringBuilderX = builderPool.borrow();
//        t1 = System.nanoTime();
//        stringBuilderX.repay(stringBuilderX);
//        t2 = System.nanoTime();
//
//        logger.debug("dt1 = {}", t1 - t0);
//        logger.debug("dt2 = {}", t2 - t1);

        ObjectStack<Integer> objectStack = new ObjectStack<>(1024);
        for (int i = 0, max = objectStack.maxCapacity(); i < max; ++i)
        {
            objectStack.push(Integer.valueOf(i));
        }

        t0 = System.nanoTime();
        Integer integer = objectStack.pop();
        t1 = System.nanoTime();
        objectStack.push(integer);
        t2 = System.nanoTime();

        logger.debug("dt1 = {}", t1 - t0);
        logger.debug("dt2 = {}", t2 - t1);

        IntegerStack integerStack = new IntegerStack(1024);
        for (int i = 0, max = objectStack.maxCapacity(); i < max; ++i)
        {
            integerStack.push(Integer.valueOf(i));
        }

        t0 = System.nanoTime();
        integer = integerStack.pop();
        t1 = System.nanoTime();
        integerStack.push(integer);
        t2 = System.nanoTime();

        logger.debug("dt1 = {}", t1 - t0);
        logger.debug("dt2 = {}", t2 - t1);
    }

    @Test
    public void test6()
    {
        String sql = "";
        String text = "INSERT     \n                 INTO ssg_account_base_logs           ( baidu_account , product_name , data_pull_time , data_pull_date , import_time , mark ) VALUES ( {baiduAccount} , {productName} , {dataPullTime} , {dataPullTime} , now ( ) , {mark} , 's sxxx ' ) , ( {baiduAccount} , {productName} , {dataPullTime} , {dataPullTime} , now ( ) , {mark} , 's sxxx ' ) , ( {baiduAccount} , {productName} , {dataPullTime} , {dataPullTime} , now ( ) , {mark} , 's sxxx ' ) , ( {baiduAccount} , {productName} , {dataPullTime} , {dataPullTime} , now ( ) , {mark} , 's sxxx ' )\n";
        Tokenizer tokenizer = new Tokenizer();

        long t0, t1;
        List<Long> timeList = new ArrayList<>(10240);

        for ( int i = 0; i < 10240; ++i )
        {
            t0 = System.nanoTime();
            sql = tokenizer.zip(text);
            t1 = System.nanoTime();
            timeList.add(t1-t0);
        }

        logger.debug("{}", sql);

        logger.debug("dt={}", timeList.get(0));
        logger.debug("dt={}", timeList.get(1));
        logger.debug("dt={}", timeList.get(2));
        logger.debug("dt={}", timeList.get(timeList.size()-3));
        logger.debug("dt={}", timeList.get(timeList.size()-2));
        logger.debug("dt={}", timeList.get(timeList.size()-1));

        logger.debug("{}", tokenizer.zip("3 + (333x7)  *  45 - ddd"));
        return;
    }

    @Test
    public void test7() throws Throwable {
        TextFormatTools textFormatTools = TextFormatTools.getInstance();
        Map<String, Object> parameterMap = new HashMap<>();

        parameterMap.put("taskType", 3);
        String template = "AND pt.task_type = {taskType}";
        String template1 = "xxxxxxxxxxxxxxxxxxx";

        String temp = null;

        long t0, t1;
        ArrayList<String> parameters;

        t0 = System.nanoTime();
        parameters = textFormatTools.parameters(template);
        t1 = System.nanoTime();
        logger.debug("parameters dt={}", t1 - t0);

        List<Long> dtList = new ArrayList<>(100000);
        int max = 10000;
        for ( int i = 0; i < max; ++i )
        {
            t0 = System.nanoTime();
            // temp = MessageFormat.format(template, 3); //
            temp = textFormatTools.render(template, parameterMap);
            t1 = System.nanoTime();
            dtList.add(t1 - t0);
        }

        dtList.forEach( dt -> logger.debug("{}", dt));
        logger.debug("{}, {}, {}, {}, {}", temp, dtList.get(0), dtList.get(1), dtList.get(2), dtList.get(3));


        Unsafe unsafe = getUnsafe();
        Field field = null;
        try
        {
            field = String.class.getDeclaredField("value");
            field.setAccessible(true);

            t0 = System.nanoTime();
            long valueOffset = unsafe.objectFieldOffset(field);
            Object value = unsafe.getObject(template1, valueOffset); // field.get(template);
            t1 = System.nanoTime();

            logger.debug( "dt={}, {}", t1-t0, value );
        }
        catch (NoSuchFieldException e)
        {
            e.printStackTrace();
        }


        return;
    }

}