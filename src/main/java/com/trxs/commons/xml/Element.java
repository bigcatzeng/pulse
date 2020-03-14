package com.trxs.commons.xml;

import java.util.ArrayList;
import java.util.List;

public class Element extends Node
{
    private String name;
    private List<Attribute> attributes = new ArrayList<>();
    private List<Node> contents = new ArrayList<>();
    private boolean isNoBody = false;

    public Element(char []chars, int length, Node parent)
    {
        super(NodeType.ELEMENT, parent);
        if ( chars[0] == 'x' && chars[1] == 'm' && chars[2] == 'l')
        {
            name = new String(chars,0, length);
        }
        else
        {
            name = chars[0] == '/' ? new String(chars,1, length-1) : new String(chars,0, length);
            setHeader(chars[0] != '/');
        }
    }

    public String getName()
    {
        return name;
    }

    public List<Attribute> getAttributes()
    {
        return attributes;
    }

    public void addAttribute(Attribute attribute)
    {
        attributes.add(attribute);
    }

    public List<Node> getContents()
    {
        return contents;
    }

    public void addContent(Node content)
    {
        contents.add(content);
    }

    public boolean isNoBody()
    {
        return isNoBody;
    }

    public Element setNoBody(boolean noBody)
    {
        isNoBody = noBody;
        return this;
    }
}