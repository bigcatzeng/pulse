package com.trxs.pulse;

public class Lex
{
    private char[] separators = { ' ', '\t', '\r', '\n', '(', ')', '{', '}' };
    public enum Type
    {
        INTEGER, FLOAT, STRING, MARKER
    }
}
