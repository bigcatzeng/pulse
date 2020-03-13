package com.trxs.pulse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.MessageFormat;

public class Analyser
{
    private static Logger logger = LoggerFactory.getLogger(Analyser.class );
    protected static int defaultCapacity = 4096;
    protected static char [] skipChars = { ' ', '\t', '\r', '\n' };
    protected static char []separators = { '<', '>', '=', ' ', '\t' };

    protected BufferedReader reader;
    protected char []chars;
    protected int index, maxIndex;

    private char    frontChar = 0;
    private int   advanceChar = 0;
    private int advanceStatus = 0;

    Analyser()
    {
        index = 0;
        chars = new char[defaultCapacity];
        maxIndex = defaultCapacity - 1;
    }

    Analyser(BufferedReader bufferedReader, int capacity)
    {
        index = 0;
        reader = bufferedReader;
        chars = new char[capacity];
        maxIndex = capacity - 1;
    }

    public TagAnalyser advance(TagAnalyser tag)
    {
        String text = null;
        NodeType nodeType = null;
        try
        {
            if ( advanceChar == 0 ) init();
            switch (advanceStatus)
            {
                case 0: // read Text
                    nodeType = NodeType.TEXT;
                    advanceChar = getTokenByChar('<');
                    break;
                case 1: // read <tag>, </tag>
                    nodeType = NodeType.ELEMENT;
                    advanceChar = getTokenByChar('>'); addChar(advanceChar);
                    break;
            }
            if ( advanceChar == -1 ) return null;

            text = new String(chars,0, index);
            next();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return text != null ? tag.load(nodeType, text) : null;
    }

    public boolean init()
    {
        try
        {
            advanceChar = reader.read();
            chars[0] = (char) advanceChar; index = 1;
            if ( advanceChar == '<' ) advanceStatus = 1; else advanceStatus = 0;
            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public void next()
    {
        index = 0;
        if ( advanceStatus == 0 ) addChar(advanceChar);
        if ( advanceChar == '<' ) advanceStatus = 1; else advanceStatus = 0;
    }


    public void addChar(int c)
    {
        if ( maxIndex > index )
            chars[index++] = (char) c;
        else
            throw new RuntimeException("缓冲区溢出!");
    }


    public Element createElement(final Element parent)
    {
        int c = -1;
        Element element = null;

        char valueSeparator = 0;
        try
        {
            int status = 0;
            String attributeName = null;
            String value;
            do
            {
                switch (status)
                {
                    case 0:
                        c = skipSpaces(skipChars);
                        if ( c == '<' )
                            status = 2;
                        else
                            throw new RuntimeException("错误的xml!");
                        break;
                    case 2: // 获取节点名->pulse
                        index = 0;
                        c = getTokenBySeparators(separators);
                        if ( inArray(c, separators) )
                        {
                            element = new Element(chars, index, parent);
                            if ( c == '>' ) return element;
                            status++;
                        }
                        else
                        {
                            addChar(c);
                        }
                        break;
                    case 3: // 开始获取属性列表 跳过属性名前空格
                        c = skipSpaces(skipChars);
                        if ( c == '>' || c == '/' ) // >
                        {   // 没有属性 返回
                            if ( c == '/' )
                            {
                                int lastChar = reader.read(); // 吃掉 '>'
                                if ( lastChar == '>' ) return element;
                            }
                            else
                            {
                                return element;
                            }
                        }
                        if ( !inArray(c, separators) )
                        {
                            status++;
                        }
                        else
                            throw new RuntimeException("错误的xml节点!");
                        break;
                    case 4: // 开始获取属性名称
                        chars[0] = (char) c; index = 1;
                        c = getTokenBySeparators(separators);
                        if ( inArray(c, skipChars) )
                        {
                            c = skipSpaces(skipChars);
                        }

                        if ( c != '=' ) throw new RuntimeException(MessageFormat.format("获取属性名称失败, 缺少等号! 单是发现[{0}]", c ) );
                        attributeName = new String(chars,0, index).trim();
                        status++;
                        break;
                    case 5: // 跳过属性值前空格
                        c = skipSpaces(skipChars);
                        if ( c == '\"' || c == '\'' )
                        {
                            valueSeparator = (char) c;
                            status++;
                        }
                        else
                            throw new RuntimeException("跳过属性值前空格失败!");
                        break;
                    case 6: // 获取 属性值
                        index = 0;
                        c = getTokenByChar(valueSeparator);
                        if ( c != valueSeparator )
                        {
                            throw new RuntimeException("获取属性值失败, 缺少 " + valueSeparator );
                        }
                        value = new String(chars,0, index).trim();
                        element.addAttribute(new Attribute(attributeName, value));
                        status = 3;
                        break;
                }
                frontChar = (char) c;
            } while( c != -1 );
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public Element getVersion(final Element parent)
    {
        int c = -1;
        Element element = null;

        char valueSeparator = 0;
        try
        {
            int status = 0;
            String attributeName = null;
            String value;
            do
            {
                switch (status)
                {
                    case 0:
                        c = skipSpaces(skipChars);
                        if ( c == '<' )
                            status++;
                        else
                            throw new RuntimeException("错误的xml版本行!");
                        break;
                    case 1: // 吃掉 ? 号
                        c = reader.read();
                        if ( c == '?' )
                            status++;
                        else
                            throw new RuntimeException("缺少 '?' !");
                        break;
                    case 2: // 获取节点名->xml
                        index = 0;
                        c = getTokenBySeparators(separators);
                        if ( inArray(c,separators) )
                        {
                            element = new Element( chars, index, parent );
                            status++;
                        }
                        else
                        {
                            addChar(c);
                        }
                        break;
                    case 3: // 开始获取属性列表 跳过属性名前空格
                        c = skipSpaces(skipChars);
                        if ( c == '?' ) // >
                        {   // 没有属性 返回
                            c = reader.read(); // 吃掉 '>'
                            return element;
                        }
                        if ( !inArray(c, separators) )
                        {
                            status++;
                        }
                        else
                            throw new RuntimeException("错误的xml版本行!");
                        break;
                    case 4: // 开始获取属性名称
                        chars[0] = (char) c; index = 1;
                        c = getTokenBySeparators(separators);
                        if ( c != '=' )
                        {
                            throw new RuntimeException("获取属性名称失败, 缺少=");
                        }
                        attributeName = new String(chars,0, index).trim();
                        status++;
                        break;
                    case 5: // 跳过属性值前空格
                        c = skipSpaces(skipChars);
                        if ( c == '\"' || c == '\'' )
                        {
                            valueSeparator = (char) c;
                            status++;
                        }
                        else
                            throw new RuntimeException("跳过属性值前空格失败!");
                        break;
                    case 6: // 获取 属性值
                        index = 0;
                        c = getTokenByChar(valueSeparator);
                        if ( c != valueSeparator )
                        {
                            throw new RuntimeException("获取属性值失败, 缺少 " + valueSeparator );
                        }
                        value = new String(chars,0, index).trim();
                        element.addAttribute(new Attribute(attributeName, value));

                        status = 3;
                        break;
                }
                frontChar = (char) c;
            } while( c != -1 );
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isSeparator(int c)
    {
        for (int i = 0; i < separators.length; ++i ) if ( separators[i] == c ) return true;
        return false;
    }

    private boolean isSpace(int c)
    {
        if ( c == ' ' || c == '\t' ) return true; else return false;
    }

    public int skipSpaces( final char []skipChars) throws IOException
    {
        int c;
        do
        {
            c = reader.read();
        } while ( inArray(c, skipChars) );

        return c;
    }

    public int getTokenByChar( final int ch ) throws IOException
    {
        int c;
        while ( (c=reader.read()) != ch )
        {
            if ( c == -1 ) break;
            if ( index + 1 > chars.length ) dilatation(chars.length*2);
            chars[index++] = (char) c;
        }
        return c;
    }

    private void dilatation(int newCapacity)
    {
        char []buffer = new char[chars.length*2];
        System.arraycopy(chars, 0, buffer, 0, chars.length);
        maxIndex = buffer.length - 1;
        chars = buffer;
        logger.warn("The read buffer too small!");
    }

    public int getTokenBySeparators( final char []separators) throws IOException
    {
        int c;
        while ( ! inArray( (c = reader.read()) , separators ) )
        {
            if ( c == -1 ) break;
            if ( index + 1 > chars.length ) dilatation(chars.length*2);
            chars[index++] = (char) c;
        }
        return c;
    }

    public boolean inArray(int ch, char []separators)
    {
        for ( int i = 0; i < separators.length; ++i )
        {
            if ( ch == separators[i]) return true;
        }
        return false;
    }

}
