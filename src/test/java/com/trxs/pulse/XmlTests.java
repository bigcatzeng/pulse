package com.trxs.pulse;

import com.alibaba.fastjson.JSON;
import com.trxs.pulse.jdbc.ObjectProxyFactory;
import com.trxs.commons.xml.Element;
import com.trxs.pulse.jdbc.SQLAction;
import com.trxs.pulse.jdbc.SQLRender;
import com.trxs.pulse.jdbc.SqlFormatterUtils;
import org.apache.tools.ant.types.resources.Intersect;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        Element e = root.findChildrenById("queryQualityInspectionByProperty");
        SQLRender sqlRender = new SQLRender();
        Map<String, Object> context = new HashMap<>();
        context.put("id", 333);
        context.put("qiType", 4);
        SQLAction a = sqlRender.render(e, context);

        SqlFormatterUtils sqlFormatterUtils = new SqlFormatterUtils();
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
        return;
    }

}