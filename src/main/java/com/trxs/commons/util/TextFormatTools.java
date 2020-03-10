package com.trxs.commons.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TextFormatTools
{
    private static RegularExpressionTools regularExpressionTools = RegularExpressionTools.getInstance();
    private static Map<String, TemplateItem[]> templateMap = new ConcurrentHashMap<>();

    private TextFormatTools(){}

    private enum Singleton
    {
        Instance;
        private TextFormatTools singleton;
        Singleton()
        {
            singleton = new TextFormatTools();
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

    public String render(String template, Map<String,Object> parameterMap )
    {
        StringBuffer sb = new StringBuffer();
        TemplateItem[] items = templateMap.get(template);
        if ( items == null )
        {
            items = analyseTemplate(template);
            templateMap.put(template, items);
        }

        for ( int i = 0; i < items.length; ++i )
        {
            if ( items[i].isVar == 0 )
                sb.append(items[i].value);
            else
                sb.append(parameterMap.get(items[i].value));
        }
        return sb.toString();
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

        StringBuffer sb = new StringBuffer();
        TemplateItem[] items = templateMap.get(template);
        if ( items == null )
        {
            items = analyseTemplate(template);
            templateMap.put(template, items);
        }

        for ( int i = 0; i < items.length; ++i )
        {
            if ( items[i].isVar == 0 )
                sb.append(items[i].value);
            else
                sb.append( getParameterValue(items[i].value, parameters) );
        }
        return sb.toString();
    }

    private Object getParameterValue(String parameterName, Object... parameters )
    {
        try
        {
            int index = parameterName.length() == 1 ? parameterName.charAt(0)-'0' : Integer.valueOf(parameterName).intValue();
            if ( index < parameters.length ) return parameters[index];
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public TemplateItem[] analyseTemplate(String template)
    {
        TemplateItem [] templateItems;
        List< TemplateItem > templateItemList = new ArrayList<>();

        List<String> items = regularExpressionTools.splitByRegex("\\{[^\\{\\}]+\\}", template);
        items.forEach( item ->
        {
            if ( item.length() > 0 )  templateItemList.add(new TemplateItem(item));
        } );

        templateItems = new TemplateItem[templateItemList.size()];
        for ( int i = 0, max = templateItemList.size(); i < max; ++i ) templateItems[i] = templateItemList.get(i);
        return templateItems;
    }

    private static class TemplateItem
    {
        public final int isVar;
        public final String value;

        public TemplateItem(String text)
        {
            if ( text.substring(0,1).equals("{") && text.substring(text.length()-1).equals("}") )
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
}
