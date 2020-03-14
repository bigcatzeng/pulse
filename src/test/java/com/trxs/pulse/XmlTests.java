package com.trxs.pulse;

import com.trxs.commons.io.FileTools;
import com.trxs.commons.util.ObjectStack;
import com.trxs.commons.xml.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;

import static com.trxs.commons.io.FileTools.getBufferedReaderBySource;
import static com.trxs.commons.io.FileTools.getBufferedReaderByString;

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
        FileTools fileTools = FileTools.getInstance();
        ObjectStack<Element> stack = new ObjectStack<>(128);

        BufferedReader bufferedReader = getBufferedReaderBySource("/sql/pulse.xml");

        Analyser analyser = new Analyser(bufferedReader, 1024);

        Element version = analyser.getVersion(null);

        long t0 = System.nanoTime();
        TagAnalyser tempTag;
        TagAnalyser tag = new TagAnalyser();
        Element element;
        Element parent = null;

        do
        {
            tempTag = analyser.advance(tag);
            if ( tempTag == null )
            {
                parent = stack.pop();
                continue;
            }

            if ( tag.getType() == NodeType.ELEMENT )
            {
                element = tag.createElement(parent);
                if ( parent == null && element.isHeader() )
                {
                    parent = element;
                    stack.push(parent);
                    continue;
                }
                else if ( parent == null )
                {
                    throw new RuntimeException("Xml格式不对, 没有根节点!!!");
                }

                if ( element.isHeader() )
                {
                    parent.addContent(element);
                    stack.push(element);
                    parent = element;
                }
                else if ( element.isNoBody() ) // 没有内容节点 <include refid="sql_questionnaireTemplate_item"/>
                {
                    parent.addContent(element);
                }
                else if ( stack.isNotEmpty() )
                {
                    stack.pop(); parent = stack.peek();
                }
                else
                {
                    throw new RuntimeException("Xml格式不对!!!");
                }
            }
            else
            {
                XmlText xmlText = new XmlText(tag.getContent(), parent);
                if ( parent != null ) parent.addContent(xmlText);
            }
        }while (tempTag != null);
        long t1 = System.nanoTime();

        logger.debug("dt={}", (t1-t0)/1000);
        return;
    }
}
