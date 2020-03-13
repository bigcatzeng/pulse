package com.trxs.pulse;

import org.junit.Test;

public class XmlText extends Node
{
    private String Text;

    public XmlText(String content, Node parent)
    {
        super(NodeType.TEXT, parent);
        Text = content;
    }

    public String getText()
    {
        return Text;
    }
}
