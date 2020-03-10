package com.trxs.commons.jdbc;

import com.trxs.commons.bean.AccessObject;
import com.trxs.commons.util.TextFormatTools;
import net.sf.cglib.beans.BeanGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.text.MessageFormat;
import java.util.*;
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

    private final static String primaryKeyName = "id";

    private String tableName;
    private List<String> emptyFields = null;
    private Object data = null;
    private AccessObject accessData = null;

    private Record(String table)
    {
        if ( table == null )
        {
            logger.warn("The name of table can't be null!");
            throw new RuntimeException("Table name is null!!!");
        }

        tableName = table;
    }

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
        if ( propertyMap == null )
        {
            logger.warn("Pls create the table -> {} in database!", tableName);
            throw new RuntimeException(MessageFormat.format("The table[{0}] is not exists in databases!", tableName));
        }

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

        sqlBuilder.append("INSERT INTO ").append(tableName).append(" ( ");

        propertySortMap.entrySet().stream().sorted( Map.Entry.comparingByValue()).forEachOrdered(property ->
        {
            Object value = accessData.getProperty(property.getKey());

            if ( value != null ) fieldList.add(camelToSnake(property.getKey()));
        });

        sqlBuilder.append(String.join(", ", fieldList));

        sqlBuilder.append(" ) VALUE ( ");

        propertySortMap.entrySet().stream().sorted( Map.Entry.comparingByValue()).forEachOrdered(property ->
        {
            Object value = accessData.getProperty(property.getKey());
            if ( value != null ) valueList.add(value);
        });

        sqlBuilder.append(String.join(", ", fieldList.stream().map( f -> "?").collect(Collectors.toList())));

        sqlBuilder.append(" );");

        return new SQLAction(SQLEnum.INSERT, sqlBuilder.toString(), valueList.toArray());
    }

    public SQLAction modifyAction()
    {
        TextFormatTools textFormat = TextFormatTools.getInstance();

        StringBuilder sqlBuilder = new StringBuilder();

        Map<String, Integer> propertySortMap = tablePropertySortMap.get(tableName);

        List<String> fieldItems = new ArrayList<>();
        List<Object> valueList = new ArrayList<>();

        sqlBuilder.append("UPDATE ").append(tableName).append(" SET ");

        propertySortMap.entrySet().stream().sorted( Map.Entry.comparingByValue()).forEachOrdered(property ->
        {
            final String key = property.getKey();
            if ( key.equalsIgnoreCase(primaryKeyName) ) return;
            Object value = accessData.getProperty(key);
            if ( value != null )
            {
                valueList.add(value);
                fieldItems.add( textFormat.render("{0} = ?", camelToSnake(key) ) );
            }
            else if ( emptyFields != null && emptyFields.contains(key) )
            {
                fieldItems.add( textFormat.render("{0} = null", camelToSnake(key) ) );
            }
        });
        valueList.add(accessData.getProperty(primaryKeyName));

        sqlBuilder.append(String.join(", ", fieldItems));

        sqlBuilder.append(" WHERE ").append(primaryKeyName).append(" = ?;");

        return new SQLAction(SQLEnum.UPDATE, sqlBuilder.toString(), valueList.toArray());
    }

    /**
     * 当需要设置表中某个字段为空时, 可以调用本函数
     * @param fields 需要置空的字段
     * @return self
     *
     */
    public Record nullFields( final String... fields )
    {
        if ( fields == null ) return this;
        if ( emptyFields == null ) emptyFields = new ArrayList<>(4);

        Arrays.asList(fields).forEach( field ->
        {
            if ( emptyFields.contains(field) ) return;

            emptyFields.add(field);
            accessData.setProperty(field, null);
        } );

        return this;
    }
}
