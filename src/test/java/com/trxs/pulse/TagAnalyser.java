package com.trxs.pulse;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

public class TagAnalyser extends Analyser
{
    private NodeType type;
    private String content;
    private boolean isEnd;
    public NodeType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public TagAnalyser load(NodeType t, String text)
    {
        type = t;
        content = text;
        reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content.getBytes())), 4096);
        if ( t == NodeType.ELEMENT ) isEnd = content.substring(1,2).equalsIgnoreCase("/");
        return this;
    }

    public Object isEnd()
    {
        return isEnd;
    }
}
