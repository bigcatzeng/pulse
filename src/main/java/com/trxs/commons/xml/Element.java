package com.trxs.commons.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Element extends Node
{
    public static final String []  levelTwoNames = { "sql", "insert", "update", "select", "delete" };
    public static final String []levelThreeNames = { "sql", "where", "trim", "include", "foreach", "choose", "otherwise", "if" };

    private String id = "";
    private String name = "";
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
        name = name.toUpperCase();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    public boolean hasAttribute(final String name)
    {
        return attributes.stream().anyMatch( attribute -> attribute.getKey().equals(name));
    }

    public String getAttributeByName(final String name)
    {
        List<String> result = attributes.stream().filter(attribute -> attribute.getKey().equals(name)).map(attribute -> attribute.getValue()).collect(Collectors.toList());
        if ( result.size() > 0) return result.get(0); else return null;
    }

    public List<Attribute> getAttributes()
    {
        return attributes;
    }

    public void addAttribute(Attribute attribute)
    {
        if ( attribute.getKey().equals("id") ) setId(attribute.getValue());
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
        if ( noBody ) setHeader(false);
        return this;
    }

    public void renderByMap(String id, Map<String,Object> context)
    {
        Element element = findChildrenById(id);
        if ( element == null ) return;
    }

    public Element findChildrenById( String id )
    {
        if ( contents == null ) return null;
        if ( contents.size() == 0 ) return null;

        List<Node> elements = contents.stream().filter( node -> node.getNodeType() == NodeType.ELEMENT && node.getId().equals(id) ).collect(Collectors.toList());

        if ( elements.size() > 0 ) return (Element) elements.get(0); else return null;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public void setId(String id)
    {
        this.id = id;
    }
}