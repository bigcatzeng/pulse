package com.trxs.pulse.jdbc;

import com.fel.FelEngine;
import com.fel.common.FelBuilder;
import com.trxs.commons.xml.Element;
import com.trxs.commons.xml.Node;
import com.trxs.commons.xml.NodeType;
import com.trxs.commons.xml.XmlText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.trxs.commons.xml.Analyser.readXmlBySource;

@Scope("singleton")
@Service
public class SQLProvider
{
    protected static Logger logger = LoggerFactory.getLogger(SQLProvider.class.getName());

    private static String sourceWithSQL = "/templates/default.xml";

    private final Map<String, Element> statementMap = new ConcurrentHashMap<>(32);
    private final Map<Thread, FelEngine> threadFelEngineMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init()
    {
        loadConfig(sourceWithSQL);
    }

    private void loadConfig(String source)
    {
        Element root = readXmlBySource(source);
        root.getContents().forEach( content ->
        {
            if ( content.getNodeType() != NodeType.ELEMENT ) return;
            Element node = (Element) content;
            String id = node.getAttributeByName("id");
            if ( id != null ) statementMap.put(id, node);
        });
        return;
    }

    public String getSqlByKey(String key )
    {
        Element element = getElementId(key);
        List<Node> contents = element.getContents();
        if ( contents.size() != 1 && contents.get(0).getNodeType() != NodeType.TEXT )
        {
            logger.warn("Can't fond the SQL setup with -> {}!", key);
            return "";
        }
        return ( (XmlText) element.getContents().get(0)).getText();
    }

    public Element getElementId(String key )
    {
        return statementMap.get(key);
    }

    public String rendering(Element element, Object... args)
    {
        FelEngine felEngine = threadFelEngineMap.get(Thread.currentThread());
        if ( felEngine == null )
        {
            felEngine = FelBuilder.engine();
            threadFelEngineMap.put(Thread.currentThread(), felEngine);
        }
        return null;
    }
}
