package com.trxs.pulse;

public class Node
{
    private NodeType nodeType;
    private Node parentNode;

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
}
