package com.trxs.commons.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegularExpressionTools
{
    private static Logger logger = LoggerFactory.getLogger(RegularExpressionTools.class);

    private RegularExpressionTools()
    {
    }

    private enum Singleton
    {
        INSTANCE;
        private RegularExpressionTools instance;

        Singleton()
        {
            instance = new RegularExpressionTools();
        }

        public RegularExpressionTools getInstance()
        {
            return instance;
        }
    }

    public static RegularExpressionTools getInstance()
    {
        return Singleton.INSTANCE.getInstance();
    }

    public boolean matcher(String regex, String text)
    {
        Objects.requireNonNull(text);
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(text);
        if(m.find()) return true; else return false;
    }

    public List<String> splitByRegex(String regex, String text)
    {
        List<String> items = new ArrayList<>();
        Objects.requireNonNull(text);
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(text);

        int begin = 0;
        while (m.find())
        {
            items.add( text.substring(begin, m.start()));
            items.add( m.group() );
            begin = m.end();
        }
        if ( begin < text.length() ) items.add(text.substring(begin));
        return items;
    }
}
