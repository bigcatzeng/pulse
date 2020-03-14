package com.trxs.pulse;

import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.List;

import com.trxs.commons.io.FileTools;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

/**
 * 使用DOM解析XML文件
 * @author Administrator
 *
 */
public class XMLDemo1
{
    private static Logger logger = LoggerFactory.getLogger(XMLDemo1.class);

    private static Unsafe getUnsafe() throws Throwable
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
        throw new IllegalAccessException("no declared field: theUnsafe");
    }

    public static void main(String []args) throws Throwable
    {
        try
        {
            /*
             * 解析XML文件的基本流程
             * 1:创建SAXReader,用来读取XML
             *   文件
             * 2:指定xml文件使得SAXReader读取，
             *   并解析问文档对象Document
             * 3:获取根元素
             * 4:获取每一个元素，从而达到解析的
             *   目的。
             */
            //1
            //org.dom4j.xxxx

            SAXReader reader = new SAXReader();

            //2
            /*
             * 常用的读取方法
             * Document read(InputStream in)
             * Document read(Reader read)
             * Document read(File file)
             */
            InputStream inputStream = FileTools.getInputStream("/sql/pulse.xml");
            //2
            /*
             * read方法的作用：
             * 读取给定的xml，并将其解析转换为
             * 一个Document对象。
             * 实际上这里已经完成了对整个xml
             * 解析的工作。并将所有内容封装到了
             * Document对象中。
             * Document对象可以描述当前xml文档
             */
            long t0 = System.nanoTime();
            Document doc = reader.read(inputStream);
            long t1 = System.nanoTime();


            //3
            Element root = doc.getRootElement();

            Unsafe unsafe = getUnsafe();

            Field[] fields = Element.class.getDeclaredFields();
            for (Field field : fields)
            {
                System.out.println(field.getName() + "---offSet:" + unsafe.objectFieldOffset(field));
            }

            //4
            /*
             * Element element(String name)
             * 获取当前标签下第一个名为给定
             * 名字的标签
             *
             * List elements(String name)
             * 获取当前标签下所有给定名字的
             * 标签
             *
             * List elements()
             * 获取当前标签下的所有子标签。
             */
            List<Element> elements = root.elements();
            /*
             * 创建一个集合，用于保存xml中
             * 的每一个用户信息。我们先将
             * 用户信息取出，然后创建一个Emp
             * 实例，将信息设置到该实例的相应
             * 属性上。最终将所有emp对象存入
             * 该集合。
             */

            elements.forEach( element -> scanElement(element) );

            logger.debug("dt={}", (t1-t0)/1000);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void scanElement(Element element)
    {
        logger.debug("begin element: {}, {}", element.getName(), element.getQName());
        element.attributes().forEach( attribute -> logger.debug("attribute -> {}={}", attribute.getName(), attribute.getText()));
        element.content().forEach( content -> scanContent(content) );
        logger.debug("end element: {}, {}", element.getName(), element.getQName());
    }

    private static void scanContent(Node contentNode)
    {
        if ( contentNode.getNodeType() == 3 )
            logger.debug("{}, {}, {}", contentNode.getNodeType(), contentNode.getNodeTypeName(), contentNode.getText());
        else if ( contentNode.getNodeType() == 1 )
            scanElement((Element) contentNode);
    }
}
