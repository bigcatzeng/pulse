package com.trxs.pulse.jdbc;

import com.trxs.commons.bean.GeneralBeanTools;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.trxs.pulse.jdbc.SnakeToCamelParameterUtil.snakeToCamel;

/**
 * @author zengshengwen 2019 19-1-17 下午11:43
 */
public class ObjectRowMapper implements RowMapper<Object>
{
    private final static Map<String, Map<String, Class<?>>> propertyClassMap = new ConcurrentHashMap<>();

    private String sqlId;
    private Map<String, Class<?>> classMap;
    private GeneralBeanTools generalBeanTools = new GeneralBeanTools(propertyClassMap);

    public ObjectRowMapper(String sqlKey)
    {
        sqlId = sqlKey;
        classMap = propertyClassMap.get(sqlId);
    }

    @Override
    public Object mapRow(ResultSet resultSet, int rowNum) throws SQLException
    {
        if ( classMap == null )
        {
            Map<String, Class<?>> map = new ConcurrentHashMap<>();
            getClassInfo(map , resultSet.getMetaData());
            generalBeanTools = new GeneralBeanTools(map);
            propertyClassMap.put(sqlId, map); classMap = map;
        }

        return newObject(resultSet);
    }

    public void getClassInfo(Map<String, Class<?>> propertyMap, ResultSetMetaData metaData)
    {

        try
        {
            for ( int i = 1, count = metaData.getColumnCount(); i <= count; ++i )
            {
                String columnLabel = metaData.getColumnLabel(i);
                propertyMap.put(columnLabel.indexOf('_') >= 0 ? snakeToCamel(columnLabel): columnLabel, Class.forName(metaData.getColumnClassName(i)));
            }
        }
        catch (ClassNotFoundException | SQLException e)
        {
            e.printStackTrace();
        }
        return;
    }

    public Object newObject( ResultSet resultSet )
    {
        try
        {
            ResultSetMetaData metaData = resultSet.getMetaData();
            for ( int i = 1, count = metaData.getColumnCount(); i <= count; ++i )
            {
                String columnLabel = metaData.getColumnLabel(i);
                generalBeanTools.setValue(columnLabel.indexOf('_') >= 0 ? snakeToCamel(columnLabel): columnLabel, resultSet.getObject(i));
            }
            return generalBeanTools.getObject();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return null;
    }
}
