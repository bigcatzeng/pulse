package com.trxs.commons.jdbc;

import com.trxs.commons.bean.GeneralBeanTools;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zengshengwen 2019 19-1-17 下午11:43
 */
public class ObjectRowMapper implements RowMapper<Object>
{
    final Map<String, Class<?>> propertyClassMap = new HashMap();

    public ObjectRowMapper(){}

    @Override
    public Object mapRow(ResultSet resultSet, int rowNum) throws SQLException
    {
        if ( rowNum == 0 )
        {
            getPropertyClassInfo(propertyClassMap, resultSet.getMetaData());
        }

        return newObject(propertyClassMap,resultSet);
    }

    public void getPropertyClassInfo(Map<String, Class<?>> propertyMap, ResultSetMetaData metaData)
    {

        try
        {
            for ( int i = 1, count = metaData.getColumnCount(); i <= count; ++i )
            {
                propertyMap.put(metaData.getColumnLabel(i), Class.forName(metaData.getColumnClassName(i)));
            }
        }
        catch (ClassNotFoundException | SQLException e)
        {
            e.printStackTrace();
        }
    }

    public Object newObject(Map<String, Class<?>> propertyClassMap, ResultSet resultSet )
    {
        try
        {
            GeneralBeanTools bean = new GeneralBeanTools(propertyClassMap);
            ResultSetMetaData metaData = resultSet.getMetaData();
            for ( int i = 1, count = metaData.getColumnCount(); i <= count; ++i )
            {
                bean.setValue(resultSet.getMetaData().getColumnLabel(i), resultSet.getObject(i));
            }
            return bean.getObject();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return null;
    }

}
