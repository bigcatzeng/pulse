package com.trxs.pulse;

import com.trxs.commons.xml.Node;
import com.trxs.commons.xml.NodeType;

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
