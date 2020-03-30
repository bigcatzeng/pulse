package com.trxs.pulse;

import com.alibaba.fastjson.JSON;
import com.trxs.commons.util.ConcurrentObjectStack;
import com.trxs.commons.util.ObjectStack;
import com.trxs.pulse.jdbc.*;
import com.trxs.commons.xml.Element;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static com.trxs.commons.io.FileTools.getBufferedReaderByString;
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
    public void test1()
    {
        long t0 = System.nanoTime();

        Element root = readXmlBySource("/sql/pulse.xml");

        long t1 = System.nanoTime();

        logger.debug("root name = {}, dt={}", root.getName(), (t1-t0)/1000);

        ObjectStack objectStack = new ConcurrentObjectStack<Integer>(1000 );

        for ( int i = 0; i < 1000; ++i ) objectStack.push(Integer.valueOf(i));

        t0 = System.nanoTime();
        objectStack.pop();
        t1 = System.nanoTime();

        logger.debug("Stack pop dt={}", (t1-t0)/1000);

        Map<String, Element> elementMap = new HashMap<>();

        //Element e = root.findChildrenById("");

        SQLRender sqlRender = new SQLRender(root);
        Map<String, Object> context = new HashMap<>();
        context.put("id", 333);
        context.put("type", 1);
        context.put("qiType", 4);
        context.put("taskType", 4);
        context.put("planType", 4);

        SQLAction a = sqlRender.render("queryQualityInspectionWithPage", context);

        SQLFormatterUtils sqlFormatterUtils = new SQLFormatterUtils();
        String sql = sqlFormatterUtils.format(a.getSqlText());

        logger.debug("\n{}", sql);
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

}