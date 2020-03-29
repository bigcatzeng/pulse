package com.trxs.commons.xml;

abstract public class Node
{
    private NodeType nodeType;
    private Node parentNode;

    private String id;

    private boolean isHeader = false;

    public Node(NodeType type, Node p)
    {
        nodeType = type; parentNode = p;
    }

    public NodeType getNodeType()
    {
        return nodeType;
    }

    public Node getParentNode()
    {
        return parentNode;
    }

    public boolean isHeader() {
        return isHeader;
    }

    public void setHeader(boolean header) {
        isHeader = header;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
