package com.trxs.pulse.jdbc;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.fel.FelEngine;
import com.fel.FelEngineImpl;
import com.fel.context.FelContext;
import com.trxs.commons.xml.Element;
import com.trxs.commons.xml.Node;
import com.trxs.commons.xml.XmlText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SQLRender
{
    private static MethodAccess access;
    private static FelEngine engine;

    static
    {
        access = MethodAccess.get(SQLRender.class);
        engine = new FelEngineImpl();
    }

    public SQLAction render(Element element, Map<String, Object> context )
    {
        StringBuilder stringBuffer = new StringBuilder();
        List<Object> objects = new ArrayList<>();
        List<Node> nodes = element.getContents();
        nodes.forEach( node -> renderNode(node, stringBuffer, context, objects));
        SQLAction sqlAction = new SQLAction(SQLEnum.QUERY,stringBuffer.toString(), null);
        return sqlAction;
    }

    void renderNode(Node node, StringBuilder builder, Map<String, Object> context, List<Object> objects)
    {
        if ( node instanceof Element )
            render(builder, context, objects, (Element) node);
        else if ( node instanceof XmlText )
            builder.append(((XmlText) node).getText());
    }

    private void render(StringBuilder builder, Map<String, Object> context, List<Object> objects, Element element)
    {
        access.invoke(this, element.getName(), builder, context, objects, element);
    }

    public void WHERE(StringBuilder builder, Map<String, Object> context, List<Object> objects,  Element element)
    {
        StringBuilder sb = new StringBuilder(256);
        element.getContents().forEach( node -> renderNode(node, sb, context, objects));
        if ( sb.length() > 0 ) builder.append("WHERE ").append(sb.toString());
    }

    public void TRIM(StringBuilder builder, Map<String, Object> context, List<Object> objects,  Element element)
    {
        StringBuilder sb = new StringBuilder(256);

        element.getContents().forEach( node -> renderNode(node, sb, context, objects));

        if ( element.hasAttribute("prefix") ) builder.append(element.getAttributeByName("prefix"));

        if ( element.hasAttribute("prefixOverrides") )
        {
            String []prefixOverrides = element.getAttributeByName("prefixOverrides").split("\\|");
            skipToSpace(sb);
            for ( int i = 0, len = sb.length(); i < prefixOverrides.length; ++i )
            {
                trimLeft(sb, prefixOverrides[i]);
                if ( sb.length() != len ) break;
            }
        }

        if ( element.hasAttribute("suffixOverrides") )
        {
            String []suffixOverrides = element.getAttributeByName("suffixOverrides").split("\\|");;
            skipToSpace(sb);

            for ( int i = 0, len = sb.length(); i < suffixOverrides.length; ++i )
            {
                trimRight(sb, suffixOverrides[i]);
                if ( sb.length() != len ) break;
            }
        }

        builder.append(sb.toString());
    }

    public void IF(StringBuilder builder, Map<String, Object> context, List<Object> objects,  Element element)
    {
        if ( !element.hasAttribute("test") ) return;
        Boolean result = (Boolean) eval(context, element.getAttributeByName("test"));
        if ( ! result.booleanValue() ) return;

        StringBuilder sb = new StringBuilder(128 );
        element.getContents().forEach( node -> renderNode(node, sb, context, objects));
        builder.append(sb.toString());
    }

    private Object eval(Map<String, Object> context, String expression)
    {
        FelContext ctx = engine.getContext();
        context.forEach( (key,value) -> ctx.set(key,value) );
        return engine.eval(expression);
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
            if ( text.charAt(i) == sb.charAt(index) ) ++count; else break;
        }

        if ( count == text.length() ) sb.delete(index, index+count);
    }
}
