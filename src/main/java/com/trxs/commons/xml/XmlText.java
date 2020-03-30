package com.trxs.commons.xml;

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

    @Override
    public String getId()
    {
        return "";
    }

    @Override
    public void setId(String id)
    {
    }

    @Override
    public String getName()
    {
        return "";
    }

    @Override
    public void setName(String id){}
}
