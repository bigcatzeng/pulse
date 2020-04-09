package com.trxs.pulse.jdbc;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.fel.Expression;
import com.fel.FelEngine;
import com.fel.FelEngineImpl;
import com.fel.context.FelContext;
import com.trxs.commons.util.ObjectStack;
import com.trxs.commons.util.TextFormatTools;
import com.trxs.commons.xml.Element;
import com.trxs.commons.xml.Node;
import com.trxs.commons.xml.XmlText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.trxs.commons.unsafe.UnsafeTools.getUnsafe;

public class SQLRender
{
    protected static Logger logger = LoggerFactory.getLogger(SQLRender.class );
    private static MethodAccess access;
    private static FelEngine engine;
    private static Unsafe unsafe = getUnsafe();
    private static long valueOffset = -1;

    private TextFormatTools textFormatTools = TextFormatTools.getInstance();

    private ObjectStack<StringBuilder> builderPool = new ObjectStack(256);

    protected final Map<String, Element> elementMap = new HashMap<>();

    static
    {
        access = MethodAccess.get(SQLRender.class);
        engine = new FelEngineImpl();
        try
        {
            Field field = ArrayList.class.getDeclaredField("elementData");
            valueOffset = unsafe.objectFieldOffset(field);
        }
        catch (NoSuchFieldException e)
        {
            e.printStackTrace();
        }
    }

    public SQLRender( Element root )
    {
        for (int i = 0, max = builderPool.maxCapacity(); i < max; ++i )
        {
            builderPool.push(new StringBuilder());
        }

        root.getContents().forEach( node ->
        {
            if ( node.getId().length() > 0 ) elementMap.put(node.getId(), (Element) node);
        });
    }

    public SQLAction render(final String id, final Map<String, Object> context )
    {

        Element element = elementMap.get(id);

        StringBuilder stringBuffer = new StringBuilder();
        ArrayList<Object> objects = new ArrayList<>();
        List<Node> nodes = element.getContents();

        nodes.forEach( node -> render(node, stringBuffer, context, objects));

        Object []args;
        if ( valueOffset > 0 )
        {
            Object []value = (Object[]) unsafe.getObject(objects, valueOffset);
            args = new Object[objects.size()];
            for ( int i = 0, max = objects.size(); i < max; ++i ) args[i] = value[i];
        }
        else
            args = objects.toArray();

        SQLAction sqlAction = new SQLAction(SQLEnum.QUERY,stringBuffer.toString(), args );

        return sqlAction;
    }

    void render(Node node, StringBuilder builder, Map<String, Object> context, List<Object> objects)
    {
        if ( node instanceof Element )
        {
            render(builder, context, objects, (Element) node);
        }
        else if ( node instanceof XmlText )
        {
            String xmlText = ((XmlText) node).getText();
            String result = textFormatTools.renderSQL(xmlText, objects, context);
            builder.append(result);
        }
    }

    void renderForeachNode(Node node, StringBuilder builder, Map<String, Object> context, List<Object> objects, String collections, int index)
    {
        if ( node instanceof Element )
        {
            render(builder, context, objects, (Element) node);
        }
        else if ( node instanceof XmlText )
        {
            String xmlText = ((XmlText) node).getText();
            String result = textFormatTools.renderForeachSQL(xmlText, objects, context, collections, index);
            builder.append(result);
        }
    }

    private void render(StringBuilder builder, Map<String, Object> context, List<Object> objects, Element element)
    {
        access.invoke(this, element.getName(), builder, context, objects, element);
    }

    private void initStringBuilder(StringBuilder builder)
    {
        builder.delete(0, builder.length());
    }

    public void WHERE(StringBuilder builder, Map<String, Object> context, List<Object> objects,  Element element)
    {
        final StringBuilder sb = builderPool.pop();
        if ( sb.length() > 0 ) sb.delete(0,sb.length());
        element.getContents().forEach(node -> render(node, sb, context, objects));
        if ( sb.length() > 0 ) builder.append("WHERE ").append(sb.toString());
        builderPool.push(sb);
    }

    public void TRIM(StringBuilder builder, Map<String, Object> context, List<Object> objects,  Element element)
    {
        final StringBuilder sb = builderPool.pop();
        if ( sb.length() > 0 ) sb.delete(0,sb.length());
        element.getContents().forEach(node -> render(node, sb, context, objects));

        if (element.hasAttribute("prefix")) builder.append(element.getAttributeByName("prefix"));

        if (element.hasAttribute("prefixOverrides"))
        {
            String[] prefixOverrides = element.getAttributeByName("prefixOverrides").split("\\|");
            skipToSpace(sb);
            for (int i = 0, len = sb.length(); i < prefixOverrides.length; ++i)
            {
                trimLeft(sb, prefixOverrides[i]);
                if (sb.length() != len) break;
            }
        }

        if (element.hasAttribute("suffixOverrides"))
        {
            String[] suffixOverrides = element.getAttributeByName("suffixOverrides").split("\\|");
            skipToSpace(sb);

            for (int i = 0, len = sb.length(); i < suffixOverrides.length; ++i) {
                trimRight(sb, suffixOverrides[i]);
                if (sb.length() != len) break;
            }
        }

        builder.append(sb.toString());
        builderPool.push(sb);
    }

    public void IF(StringBuilder builder, Map<String, Object> context, List<Object> objects,  Element element)
    {
        if ( !element.hasAttribute("test") ) return;
        Boolean result = (Boolean) eval(context, element.getAttributeByName("test"));
        if ( ! result.booleanValue() ) return;
        long t1 = System.nanoTime();

        final StringBuilder sb = builderPool.pop();
        if ( sb.length() > 0 ) sb.delete(0,sb.length());
        element.getContents().forEach(node -> render(node, sb, context, objects));
        builder.append(sb.toString());
        builderPool.push(sb);
    }

    public void INCLUDE(StringBuilder builder, Map<String, Object> context, List<Object> objects,  Element element)
    {
        Element refElement = elementMap.get(element.getAttributeByName("refid"));
        if ( refElement == null ) return;

        for ( Node node : refElement.getContents() ) render(node, builder, context, objects);
    }

    public void CHOOSE(StringBuilder builder, Map<String, Object> context, List<Object> objects,  Element element)
    {
        AtomicBoolean matchWen = new AtomicBoolean(false);

        for ( Node node : element.getContents() )
        {
            if ( node.getName().equals("WHEN") )
            {
                Element when = (Element) node;
                if ( !when.hasAttribute("test") ) return;
                Boolean result = (Boolean) eval(context, when.getAttributeByName("test"));
                if ( ! result.booleanValue() ) return;

                // 为什么要先判断, 再设置? 因为读的代价一定比写低
                if ( matchWen.get() == false ) matchWen.set( true );

                for ( Node n : when.getContents() ) render(n, builder, context, objects);
            }
            else if ( node.getName().equals("OTHERWISE") && matchWen.get() == false )
            {
                Element otherwise = (Element) node;

                for ( Node n : otherwise.getContents() )  render(n, builder, context, objects);
            }
            else if ( node instanceof XmlText )
            {
                builder.append(((XmlText) node).getText());
            }
        };
    }

    public void FOREACH(StringBuilder builder, Map<String, Object> context, List<Object> objects,  Element element)
    {
        final StringBuilder sb = builderPool.pop();

        String separator = element.getAttributeByName("separator");
        String collections = element.getAttributeByName("collections");

        List<Object> items = (List<Object>) context.get(collections);
        List<String> values = new ArrayList<>(items.size());

        for ( int i = 0, max = items.size(); i < max; ++i)
        {
            if ( sb.length() > 0 ) sb.delete(0,sb.length());
            for ( Node n : element.getContents() ) renderForeachNode(n, sb, context, objects, collections, i );
            skipToSpace(sb);
            trimRight(sb, " \t\r\n");
            values.add(sb.toString());
        }

        builder.append(String.join(separator, values));

        builderPool.push(sb);
    }

    private Map<String, Expression> expressionMap = new HashMap<>();

    private Object eval(Map<String, Object> context, String expression)
    {
        final FelContext ctx = engine.getContext();
        context.forEach( (key,value) -> ctx.set(key,value) );
        Expression compile = expressionMap.get(expression);
        if ( compile == null )
        {
            compile = engine.compile(expression, null);
            expressionMap.put(expression,compile);
        }

        Object result = compile.eval(ctx);
        return result;
    }

    private void skipToSpace(StringBuilder sb)
    {
        int i = 0;
        while( i < sb.length() && Character.isWhitespace(sb.charAt(i)) ) ++i;
        sb.delete(0, i);
    }

    private void trimLeft( StringBuilder sb, String text )
    {
        if ( text == null || sb == null) return;
        if ( text.length() == 0 || sb.length() < text.length() ) return;

        int count = 0;

        for ( int i = 0; i < text.length(); ++i )
        {
            if ( text.charAt(i) == sb.charAt(i) ) ++count; else break;
        }

        if ( count == text.length() ) sb.delete(0, count);
    }

    private void trimRight( StringBuilder sb, String text )
    {
        if ( text == null || sb == null) return;
        if ( text.length() == 0 || sb.length() < text.length() ) return;

        int count = 0;
        int index = 0;
        for ( int i = text.length()-1, j = sb.length() - 1; i >= 0; --i )
        {
            index = j--;
            if ( text.indexOf(sb.charAt(index)) > 0 ) ++count; else break;
        }

        if ( count == text.length() ) sb.delete(index, index+count);
    }
}
