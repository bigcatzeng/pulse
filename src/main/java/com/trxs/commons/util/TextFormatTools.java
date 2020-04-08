package com.trxs.commons.util;

import com.trxs.commons.bean.BeanAccess;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.trxs.commons.unsafe.UnsafeTools.getUnsafe;

public class TextFormatTools
{
    private static RegularExpressionTools regularExpressionTools = RegularExpressionTools.getInstance();
    private static Map<String, Rendering> templateMap = new ConcurrentHashMap<>();
    private static Unsafe unsafe = getUnsafe();
    private static ObjectStack<StringBuilder> stringBuilderStack = new ObjectStack(16);
    private TextFormatTools(){}

    private enum Singleton
    {
        Instance;
        private TextFormatTools singleton;
        Singleton()
        {
            singleton = new TextFormatTools();
            stringBuilderStack.push(new StringBuilder(128));
            stringBuilderStack.push(new StringBuilder(128));
            stringBuilderStack.push(new StringBuilder(128));
            stringBuilderStack.push(new StringBuilder(128));
            stringBuilderStack.push(new StringBuilder(128));
            stringBuilderStack.push(new StringBuilder(128));
        }

        public TextFormatTools getInstance()
        {
            return singleton;
        }
    }

    public static TextFormatTools getInstance()
    {
        return Singleton.Instance.getInstance();
    }

    public ArrayList<String> parameters(String template )
    {
        Rendering context = templateMap.get(template);
        if ( context == null )
        {
            context = new Rendering(analyseTemplate(template));
            templateMap.put(template, context);
        }
        return context.varNames;
    }

    public String render(String template, Map<String,Object> parameterMap )
    {
        Rendering context = templateMap.get(template);
        if ( context == null )
        {
            context = new Rendering(analyseTemplate(template));
            templateMap.put(template, context);
        }
        return context.render(parameterMap);
    }

    public String renderSQL(String template, List<Object> objects, Map<String,Object> parameterMap)
    {
        Rendering context = templateMap.get(template);
        if ( context == null )
        {
            context = new Rendering(analyseTemplate(template));
            templateMap.put(template, context);
        }
        return context.renderSQL(objects, parameterMap);
    }

    public String renderForeachSQL(String template, List<Object> objects, Map<String,Object> contextMap, String collections, String key, int index)
    {
        Rendering context = templateMap.get(template);
        if ( context == null )
        {
            context = new Rendering(analyseTemplate(template));
            templateMap.put(template, context);
        }
        return context.renderForeachSQL(objects, contextMap, collections, key, index);
    }

    public String renderSQL(String template, List<Object> objects, Object... parameters)
    {
        Rendering context = templateMap.get(template);
        if ( context == null )
        {
            context = new Rendering(analyseTemplate(template));
            templateMap.put(template, context);
        }
        return context.renderSQL(objects, parameters);
    }
    /**
     *
     * @param template      Hi {varName}!
     * @param parameters
     * @return
     *
     */
    public String render(String template, Object... parameters)
    {
        if ( parameters == null ) return template;

        Rendering context = templateMap.get(template);
        if ( context == null )
        {
            context = new Rendering(analyseTemplate(template));
            templateMap.put(template, context);
        }
        return context.render(parameters);
    }


    public TemplateItem[] analyseTemplate(String template)
    {
        TemplateItem [] templateItems;
        ArrayList<TemplateItem> templateItemArray = splitByParameterName(template); //
        try
        {
            Field field = templateItemArray.getClass().getDeclaredField("elementData");
            long valueOffset = unsafe.objectFieldOffset(field);
            Object []value = (Object[]) unsafe.getObject(templateItemArray, valueOffset);
            templateItems = new TemplateItem[templateItemArray.size()];
            for ( int i = 0, max = templateItemArray.size(); i < max; ++i ) templateItems[i] = (TemplateItem) value[i];
            return templateItems;
        }
        catch (NoSuchFieldException e)
        {
            e.printStackTrace();
        }

        List<String> items = regularExpressionTools.splitByRegex("\\{[^\\{\\}]+\\}", template);
        items.forEach( item -> templateItemArray.add(new TemplateItem(item)));

        templateItems = new TemplateItem[items.size()];
        for ( int i = 0, max = items.size(); i < max; ++i ) templateItems[i] = templateItemArray.get(i);
        return templateItems;
    }

    public ArrayList<TemplateItem> splitByParameterName(String text)
    {
        boolean delFlag = false;
        ArrayList<TemplateItem> items = new ArrayList<>(4);
        if ( text == null || text.length() < 1 ) return items;

        final char[] chars = (char[]) unsafe.getObject(text, 12l);
        StringBuilder sb = stringBuilderStack.pop();
        if ( sb == null )
        {
            sb = new StringBuilder();delFlag = true;
        }
        sb.delete(0, sb.length());

        char ch;
        int status, startIndex=0, endIndex=0, type=0;
        String value = null;

        while ( startIndex < chars.length )
        {
            ch = chars[startIndex];
            if ( ch == '{' ) status = 1; else status = 0;
            switch (status)
            {
                case 0:
                    type = 0;
                    endIndex = skip2Char('{', startIndex, chars);
                    if ( endIndex < 0 ) endIndex = chars.length;
                    value = new String(chars, startIndex, endIndex - startIndex);
                    break;
                case 1:
                    endIndex = skip2Char('}', startIndex, chars);
                    if ( endIndex > -1 )
                    {
                        type = 1;
                        ++endIndex;
                    }
                    else
                    {
                        endIndex = chars.length;
                    }
                    value = new String(chars, startIndex+1, endIndex - startIndex - 2);
            }
            items.add(new TemplateItem(value, type));
            startIndex = endIndex;
        }

        if ( delFlag == false ) stringBuilderStack.push(sb);
        return items;
    }

    private int skip2Char(char ch, int begin, char[]chars)
    {
        for ( int i = begin, max = chars.length; i < max; ++i )
        {
            if ( ch == chars[i] ) return i;
        }
        return -1;
    }

    private static class TemplateItem
    {
        public final int isVar;
        public final String value;

        private int foreachIndex = -1;

        public TemplateItem(String text, int type)
        {
            value = text;
            isVar = type;
        }

        public TemplateItem(String text)
        {
            if ( text.charAt(0) == '{' && text.charAt(text.length()-1) == '}' )
            {
                isVar = 1;
                value = text.substring(1, text.length() - 1);
            }
            else
            {
                value = text;
                isVar = 0;
            }
        }
    }

    private static class Rendering
    {
        StringBuilder sb;
        TemplateItem[] items;
        ArrayList<String> varNames;
        private Rendering(TemplateItem[] items)
        {
            sb = new StringBuilder();
            this.items = items;
            varNames = new ArrayList(items.length);
            for ( int i = 0, j = 0, max = items.length; i < max; ++i ) if ( items[i].isVar == 1 ) varNames.add(items[i].value);
        }

        private String render(Map<String,Object> parameterMap)
        {
            int len = sb.length();
            sb.delete(0, len);
            for ( int i = 0; i < items.length; ++i )
            {
                if ( items[i].isVar == 0 )
                    sb.append(items[i].value);
                else
                    sb.append(parameterMap.get(items[i].value));
            }
            return sb.toString();
        }

        private String render(Object... parameters)
        {
            int len = sb.length();
            sb.delete(0, len);
            for ( int i = 0; i < items.length; ++i )
            {
                if ( items[i].isVar == 0 )
                    sb.append(items[i].value);
                else
                    sb.append( getParameterValue(items[i].value, parameters) );
            }
            return sb.toString();
        }

        private String renderForeachSQL(List<Object> objects, Map<String,Object> parameterMap, String collections, String key, int index)
        {
            int len = sb.length();
            sb.delete(0, len);
            Object item = parameterMap.get(collections);
            BeanAccess itemAccess = new BeanAccess(item);
            for ( int i = 0; i < items.length; ++i )
            {
                if ( items[i].isVar == 0 )
                {
                    sb.append(items[i].value);
                    continue;
                }
                sb.append( "?" );
                String varName = items[i].value;
                if ( varName.charAt(0) == '.' )
                {
                    if ( items[i].foreachIndex < 0 ) itemAccess.getPropertyIndexByName()
                }
                else
                {
                    objects.add(parameterMap.get(varName));
                }
            }
            return sb.toString();
        }

        private String renderSQL(List<Object> objects, Map<String,Object> parameterMap)
        {
            int len = sb.length();
            sb.delete(0, len);
            for ( int i = 0; i < items.length; ++i )
            {
                if ( items[i].isVar == 0 )
                {
                    sb.append(items[i].value);
                    continue;
                }
                sb.append( "?" );
                objects.add(parameterMap.get(items[i].value));
            }
            return sb.toString();
        }

        private String renderSQL(List<Object> objects, Object... parameters)
        {
            int len = sb.length();
            sb.delete(0, len);
            for ( int i = 0; i < items.length; ++i )
            {
                if ( items[i].isVar == 0 )
                {
                    sb.append(items[i].value);
                    continue;
                }

                sb.append( "?" );
                objects.add(getParameterValue(items[i].value, parameters));
            }
            return sb.toString();
        }

        private Object getParameterValue(String parameterName, Object... parameters )
        {
            int index = parameterName.length() == 1 ? parameterName.charAt(0)-'0' : Integer.valueOf(parameterName).intValue();
            if ( index < parameters.length ) return parameters[index]; else return null;
        }

        public ArrayList<String> getVarNames()
        {
            return varNames;
        }
    }
}
