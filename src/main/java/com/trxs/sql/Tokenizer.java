package com.trxs.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tokenizer
{
    private static Logger logger = LoggerFactory.getLogger(Tokenizer.class );

    private char [] separatorChars = { ' ', '\t', '\n', '\r', '\f', ',', '\'', '\"', '`', '+', '-', '*', '/', '%', '=', '>', '<', '(', ')', '{', '}' };
    private int[] separatorChecker;

    private final char [] spaceChars = { '\t', '\n', '\f', '\r', ' ' };
    private int[] spaceChecker;

    private String[] keys = { "INSERT", "SELECT", "UPDATE", "DELETE", "FROM", "WHERE", "ORDER", "BY", "GROUP", "LIMIT", "AS" };

    private String sourceText;
    private char[] chars;
    private int index;

    private StringBuilder textBuilder = new StringBuilder(1024);
    private StringBuilder analyserBuilder = new StringBuilder();

    public Tokenizer()
    {
        init();
    }

    public void init()
    {
        index = -1;
        spaceChecker = new int[64];
        separatorChecker = new int[128];

        for ( int i = 0; i < separatorChecker.length; ++i )
        {
            if ( i < spaceChars.length ) spaceChecker[i] = 0;
            separatorChecker[i] = 0;
        }

        for ( char ch : spaceChars ) spaceChecker[ch] = 1;
        for ( char ch : separatorChars ) separatorChecker[ch] = 1;

        Token.setupKeys(keys);
    }

    public boolean isSpace(char ch)
    {
        if ( ch > spaceChecker.length ) return false;
        return spaceChecker[ch] == 1;
    }

    public boolean isSeparator(char ch)
    {
        if ( ch > separatorChecker.length ) return false;
        return separatorChecker[ch] == 1;
    }

    public Tokenizer load(String source)
    {
        index = 0;
        sourceText = source;
        if ( analyserBuilder.length() > 0 ) analyserBuilder.delete(0, analyserBuilder.length());
        chars = sourceText.toCharArray();
        return this;
    }

    public Token advance()
    {
        int beginIndex, endIndex;

        int result = peek();
        if ( result < 0 ) return null;

        char ch = (char) result;
        beginIndex = index;

        if ( isSeparator(ch) )
        {   // 遇到分隔符返回分隔符
            if ( ch == '\'' || ch == '\"' || ch == '`' )
            {
                ++index;
                endIndex = skipToChar(ch);
                if ( index < 0 )
                    return new Token( TokenType.STRING, new String(chars, beginIndex + 1, endIndex - beginIndex) );
                else
                    return new Token( TokenType.STRING, new String(chars, beginIndex + 1, endIndex - beginIndex - 1) );
            }
            if ( ch == '{' )
            {
                ++index;
                endIndex = skipToChar('}');
                if ( index < 0 )
                    return new Token( TokenType.STRING, new String(chars, beginIndex, endIndex - beginIndex + 1) ); // Todo 需要测试
                else
                    return new Token( TokenType.STRING, new String(chars, beginIndex, endIndex - beginIndex + 1) );
            }
            return new Token( TokenType.SEPARATOR, new String(chars, index++, 1) );
        }

        // 非分隔符开头标识符
        endIndex = skipToSeparator();

        if ( endIndex >= 0 )
            return new Token( TokenType.MARKER, new String(chars, beginIndex, endIndex-beginIndex) );
        else
            return new Token( TokenType.EOF, null );
    }

    private void next()
    {
        ++index;
    }

    private int skipSpace()
    {
        if ( index < 0 ) return -1;

        while (index < chars.length) if ( isSpace(chars[index]) ) ++index; else return index;

        index = -1;
        return -1;
    }

    private int skipToSeparator()
    {
        if ( index < 0 ) return -1;

        while (index < chars.length) if ( separatorChecker[chars[index]] != 1 ) ++index; else return index;

        int result = index-1;
        index = -1;
        return result;
    }


    private int skipToChar( final char ch )
    {
        if ( index < 0 ) return -1;

        while (index < chars.length) if ( chars[index] != ch ) ++index; else return index++;

        int result = index-1;
        index = -1;
        return result;
    }

    private int peek()
    {
        if ( index < 0 ) return -1;

        int result = chars[index];
        char ch = (char) result;

        if ( isSpace(ch) )
        {
            if ( skipSpace() < 0 ) return -1;
            result = chars[index];
        }

        return result;
    }

    public String zip(String text)
    {
        Token token, previousToken = null;
        if ( textBuilder.length() > 0 ) textBuilder.delete(0,textBuilder.length());
        load(text);

        do
        {
            token = advance();
            if ( token == null ) break;

            if ( previousToken == null )
            {
                textBuilder.append(token.getText());
                previousToken = token;
                continue;
            }

            if ( previousToken.getType() != TokenType.SEPARATOR && token.getType() != TokenType.SEPARATOR )
            {
                textBuilder.append(" ");
            }

            textBuilder.append(token.getText());

            previousToken = token;
        }while (token!=null);

        return textBuilder.toString();
    }

    public void test()
    {
    }

    public String []getKeys()
    {
        return keys;
    }

    public enum  TokenType
    {
        KEY, SEPARATOR, MARKER, STRING, INTEGER, FLOAT, EOF, UNKNOWN
    }

    private static class Token
    {
        private static String[] keys;
        private TokenType type;
        private String text;

        Token(TokenType t, String v)
        {
            type = t;
            for ( String key : keys) if ( v.equalsIgnoreCase(key) ) type = TokenType.KEY;
            text = v;
        }

        public static void setupKeys(String []ks )
        {
            keys = ks;
        }

        public TokenType getType() {
            return type;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString()
        {
            return String.join(":", type.name(), text );
        }
    }
}
