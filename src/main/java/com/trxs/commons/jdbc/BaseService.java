package com.trxs.commons.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class BaseService
{
    public final static Logger logger = LoggerFactory.getLogger(BaseService.class.getName());
    public final static SQLProvider sqlProvider = new SQLProvider();
    private String sourceWithSQL = "/templates/default.sql";

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    public BaseService() { sqlProvider.init(sourceWithSQL); }

    public BaseService( String source )
    {
        sourceWithSQL = source; sqlProvider.init(sourceWithSQL);
    }

    @PostConstruct
    public void initDatabaseMetaData()
    {
        try ( Connection connection = jdbcTemplate.getDataSource().getConnection() )
        {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            int    majorVersion   = databaseMetaData.getDatabaseMajorVersion();
            int    minorVersion   = databaseMetaData.getDatabaseMinorVersion();

            String productName    = databaseMetaData.getDatabaseProductName();
            String productVersion = databaseMetaData.getDatabaseProductVersion();

            logger.debug("数据库属性信息："+majorVersion+" "+minorVersion+" "+productName+" "+productVersion);

            int driverMajorVersion = databaseMetaData.getDriverMajorVersion();
            int driverMinorVersion = databaseMetaData.getDriverMinorVersion();

            logger.debug("驱动信息："+driverMajorVersion+" "+driverMinorVersion);

            String   catalog          = "pulse";
            String   schemaPattern    = null;
            String   tableNamePattern = null;
            String[] types            = null; // "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM"

            ResultSet result = databaseMetaData.getTables( catalog, schemaPattern, tableNamePattern, types );

            while(result.next())
            {
                final String tableName = result.getString(3);
                getColumns(databaseMetaData, tableName, (metaMap) ->
                {
                    Record.addField(tableName, metaMap);
                } );
            }
            // getPrimaryKeys(databaseMetaData, "p_domains", (columnName) -> logger.debug(columnName) );
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return;
    }

    private void getPrimaryKeys(DatabaseMetaData databaseMetaData, String tableName, Consumer<? super String> action)
    {
        if ( action == null ) return ;
        try
        {
            String   catalog   = null;
            String   schema    = null;

            ResultSet result = databaseMetaData.getPrimaryKeys( catalog, schema, tableName );

            while(result.next()) action.accept(result.getString(4));
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return ;
    }

    private void getColumns(DatabaseMetaData databaseMetaData, String tableName, Consumer<? super Map<String,Object>> action)
    {
        try
        {
            String   catalog   = null;
            String   schema    = null;
            ResultSet result = databaseMetaData.getColumns(catalog, schema, tableName, null );

            while(result.next())
            {
                Map<String, Object> metaDataMap = new HashMap<>();

                metaDataMap.put("ORDINAL_POSITION", result.getInt("ORDINAL_POSITION"));
                metaDataMap.put("COLUMN_NAME", result.getString("COLUMN_NAME"));
                metaDataMap.put("DATA_TYPE", result.getInt("DATA_TYPE"));
                metaDataMap.put("TYPE_NAME", result.getString("TYPE_NAME"));
                metaDataMap.put("CLASS_NAME", getClassNameByType(result.getInt("DATA_TYPE")) );
                metaDataMap.put("COLUMN_SIZE", result.getString("COLUMN_SIZE"));
                metaDataMap.put("NULLABLE", result.getString("NULLABLE"));
                action.accept(metaDataMap);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return ;
    }

    public <T> T queryValueForObject(String sqlKey, Class<T> requiredType, Object... args)
    {
        return jdbcTemplate.queryForObject(sqlProvider.getSQL(sqlKey), requiredType, args);
    }

    public Object queryForObject(String sqlKey, Object... args)
    {
        return jdbcTemplate.queryForObject(sqlProvider.getSQL(sqlKey), args, new ObjectRowMapper());
    }

    public int update( String sqlKey, Object... args)
    {
        return jdbcTemplate.update(sqlProvider.getSQL(sqlKey), args);
    }

    public int update(String newSession, Map<String, Object> parameters, boolean requireKey)
    {
        int rows;
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource(parameters);

        NamedParameterJdbcTemplate nameParamJdbcTemp = new NamedParameterJdbcTemplate(jdbcTemplate);

        if ( requireKey )
        {
            KeyHolder keyholder=new GeneratedKeyHolder();
            rows = nameParamJdbcTemp.update(sqlProvider.getSQL(newSession), sqlParameterSource, keyholder);
            parameters.put("lastId", keyholder.getKey());
            return rows;
        }

        rows = nameParamJdbcTemp.update(sqlProvider.getSQL(newSession), sqlParameterSource);
        return rows;
    }

    public <T> int[][] insertObjects(String key, List<T> objects, final ParameterizedPreparedStatementSetter<T> pss)
    {
        return jdbcTemplate.batchUpdate( sqlProvider.getSQL(key), objects, objects.size(), pss );
    }

    public void test()
    {
        jdbcTemplate.execute("select 1 from dual;");
    }

    public String getClassNameByType(int type)
    {
        String className = String.class.getName();
        switch(type)
        {
            case -7:
                className = Boolean.class.getName();
                break;
            case -6:
                className = Byte.class.getName();
                break;
            case -5:
                className = Long.class.getName();
                break;
            case -4:
            case -3:
            case -2:
                className = "byte[]";
                break;
            case 2:
            case 3:
                className = BigDecimal.class.getName();
                break;
            case 4:
                className = Integer.class.getName();
                break;
            case 5:
                className = Short.class.getName();
                break;
            case 6:
            case 8:
                className = Double.class.getName();
                break;
            case 7:
                className = Float.class.getName();
                break;
            case 91:
                className = Date.class.getName();
                break;
            case 92:
                className = Time.class.getName();
                break;
            case 93:
                className = Timestamp.class.getName();
                break;
            case 2004:
                className = Blob.class.getName();
                break;
            case 2005:
                className = Clob.class.getName();
        }

        return className;
    }
}
