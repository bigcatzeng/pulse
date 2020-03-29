package com.trxs.commons.xml;

import com.trxs.commons.util.ObjectStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.plugins.jpeg.JPEGImageReadParam;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.MessageFormat;

import static com.trxs.commons.io.FileTools.getBufferedReaderBySource;

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

    public Analyser(BufferedReader bufferedReader, int capacity)
    {
        index = 0;
        reader = bufferedReader;
        chars = new char[capacity];
        maxIndex = capacity - 1;
    }

    public static Element readXmlBySource( String source )
    {
        ObjectStack<Element> stack = new ObjectStack<>(128);

        BufferedReader bufferedReader = getBufferedReaderBySource(source); // "/sql/pulse.xml"

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

            if ( tempTag.getType() == NodeType.ANNOTATION )
            {
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
                    parent = stack.pop();
                    if ( ! parent.getName().equalsIgnoreCase(element.getName()) ) throw new RuntimeException(MessageFormat.format("{0}, xml格式错误!", tag.getContent()));
                    if ( stack.isEmpty() ) return parent;
                    parent = stack.peek();
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

        logger.debug("readXmlBySource -> dt={}", (t1-t0)/1000);

        return parent;
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
                    advanceChar = getTokenByChar('>');
                    if ( advanceChar > 0 ) addChar(advanceChar);
                    break;
            }
            if ( advanceChar == -1 ) return null;

            if ( advanceChar == -2 )
            {
                advanceChar = reader.read();
                nodeType = NodeType.ANNOTATION;
            }
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
                                if ( lastChar == '>' ) return element.setNoBody(true);
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

                        if ( c != '=' )
                        {
                            throw new RuntimeException(MessageFormat.format("获取属性名称[{0}]失败, 缺少等号! 单是发现[{1}]", new String(chars, 0, index), (char)c ) );
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
        int c, searchChar = ch;
        int saveIndex = index;
        char []prefixChars = {0,0,0};

        boolean isReadTag, isAnnotation = false;
        if ( searchChar == '>' ) isReadTag = true; else isReadTag = false;
        while ( (c=reader.read()) != searchChar )
        {
            if ( c == -1 ) break;
            if ( index + 1 > chars.length ) dilatation(chars.length*2);
            chars[index++] = (char) c;
            if ( isReadTag && index == 4 )
            {
                if ( chars[0] == '<' && chars[1] == '!' && chars[2] == '-' && chars[3] == '-' )
                {
                    searchChar = 0;
                    isAnnotation = true;
                }
            }

            if ( prefixChars[1] != 0 ) prefixChars[0] = prefixChars[1];
            if ( prefixChars[2] != 0 ) prefixChars[1] = prefixChars[2];
            prefixChars[2] = (char)c;

            if ( isAnnotation && prefixChars[1] == '-' && prefixChars[1] == '-' && prefixChars[2] == '>')
            {
                c = -2;
                break;
            }
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