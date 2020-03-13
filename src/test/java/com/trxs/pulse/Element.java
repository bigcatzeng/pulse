package com.trxs.pulse;

import java.util.ArrayList;
import java.util.List;

public class Element extends Node
{
    private String name;
    private List<Attribute> attributes = new ArrayList<>();
    private List<Node> contents = new ArrayList<>();

    public Element(char []chars, int length, Node parent)
    {
        super(NodeType.ELEMENT, parent);

        name = chars[1] == '/' ? new String(chars,2, length-1) : new String(chars,1, length-1);
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
}