package com.trxs.commons.jdbc;

import com.trxs.commons.bean.AccessObject;
import net.sf.cglib.beans.BeanGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.trxs.commons.jdbc.SnakeToCamelRequestParameterUtil.camelToSnake;

final public class Record
{
    protected static Logger logger = LoggerFactory.getLogger(Record.class.getName());
    private final static BeanGenerator generator = new BeanGenerator();
    private final static AtomicInteger generatorMutex = new AtomicInteger(Integer.valueOf(0));
    private final static Map<String, Map<String, Class>> tablePropertyMap = new ConcurrentHashMap<>();
    private final static Map<String, Map<String, Integer>> tablePropertySortMap = new ConcurrentHashMap<>();

    private String tableName;

    private Object data;
    private AccessObject accessData;

    private Record(String table){ tableName = table; }

    protected static void addField(String table, String propertyName, String className, Integer index)
    {
        try
        {
            Map<String, Class> propertyMap = tablePropertyMap.get(table);
            Map<String, Integer> propertySortMap = tablePropertySortMap.get(table);

            if ( propertyMap == null )
            {
                propertyMap = new ConcurrentHashMap<>();
                tablePropertyMap.put(table, propertyMap);
            }
            if ( propertySortMap == null )
            {
                propertySortMap = new ConcurrentHashMap<>();
                tablePropertySortMap.put(table, propertySortMap);
            }

            propertyMap.put(propertyName, Class.forName(className));
            propertySortMap.put(propertyName, index);
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    public static Record newInstance(String tableName)
    {
        Record record;
        try
        {
            record = new Record(tableName);
            while ( false == generatorMutex.compareAndSet(0,1) ) Thread.yield();
            record.create();
        }
        finally
        {
            generatorMutex.set(0);
        }
        return record;
    }

    /**
     *
     * @param tableName
     * @param metaMap
     *
     *     COLUMN_NAME, DATA_TYPE, TYPE_NAME, CLASS_NAME, COLUMN_SIZE, NULLABLE
     *
     */
    public static void addField(String tableName, Map<String, Object> metaMap)
    {
        String propertyName = SnakeToCamelRequestParameterUtil.snakeToCamel((String)metaMap.get("COLUMN_NAME"));
        Record.addField( tableName, propertyName, (String)metaMap.get("CLASS_NAME"), (Integer)metaMap.get("ORDINAL_POSITION") );
    }

    protected Record create()
    {
        Map<String, Class> propertyMap = tablePropertyMap.get(tableName);
        if ( propertyMap == null ) return null;

        propertyMap.forEach( (key,value) -> generator.addProperty(key, propertyMap.get(key)) );
        data = generator.create();
        accessData = new AccessObject(data);

        return this;
    }

    public String getTableName()
    {
        return tableName;
    }

    public Object getField(String fieldName)
    {
        return accessData.getProperty(fieldName);
    }

    public Record setField(String fieldName, Object value)
    {
        accessData.setProperty(fieldName, value);
        return this;
    }

    public SQLAction insertAction()
    {
        StringBuilder sqlBuilder = new StringBuilder();

        Map<String, Integer> propertySortMap = tablePropertySortMap.get(tableName);

        List<String> fieldList = new ArrayList<>();
        List<Object> valueList = new ArrayList<>();

        sqlBuilder.append("insert into ").append(tableName).append(" ( ");

        propertySortMap.entrySet().stream().sorted( Map.Entry.comparingByValue()).forEachOrdered(property ->
        {
            Object value = accessData.getProperty(property.getKey());

            if ( value != null ) fieldList.add(camelToSnake(property.getKey()));
        });

        sqlBuilder.append(String.join(", ", fieldList));

        sqlBuilder.append(" ) value ( ");

        propertySortMap.entrySet().stream().sorted( Map.Entry.comparingByValue()).forEachOrdered(property ->
        {
            Object value = accessData.getProperty(property.getKey());
            if ( value != null ) valueList.add(value);
        });

        sqlBuilder.append(String.join(", ", fieldList.stream().map( f -> "?").collect(Collectors.toList())));

        sqlBuilder.append(" );");

        return new SQLAction(sqlBuilder.toString(), valueList.toArray());
    }
}
