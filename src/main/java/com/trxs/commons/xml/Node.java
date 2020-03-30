package com.trxs.commons.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class Node
{
    protected static Logger logger = LoggerFactory.getLogger(Node.class );

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

    public abstract String getId();
    public abstract void setId(String id);

    public abstract String getName();
    public abstract void setName(String id);
}
