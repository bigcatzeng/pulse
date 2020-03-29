package com.trxs.pulse.jdbc;

import com.trxs.commons.util.TextFormatTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.trxs.pulse.jdbc.SnakeToCamelParameterUtil.camelToSnake;

public class BaseService
{
    private final static Logger logger = LoggerFactory.getLogger(BaseService.class.getName());

    @Autowired
    protected SQLProvider sqlProvider;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    public BaseService() {}

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

        return jdbcTemplate.queryForObject(sqlProvider.getSqlByKey(sqlKey), requiredType, args);
    }

    public Object queryForObject(String sqlKey, Object... args)
    {
        return jdbcTemplate.queryForObject(sqlProvider.getSqlByKey(sqlKey), args, new ObjectRowMapper(sqlKey));
    }

    /**
     * 返回单个dto
     * @param sql 查询sql
     * @param queryArgs 查询参数
     * @param rowMapper dto mapper
     * @param <T> dto
     * @return dto
     */
    public <T>T queryForObject(String sql, Object[] queryArgs, RowMapper<T> rowMapper)
    {
        logger.info(sql);
        return jdbcTemplate.queryForObject(sql, queryArgs, rowMapper);
    }

    /**
     * 返回dto列表
     * @param sql 查询sql
     * @param queryArgs 查询参数
     * @param rowMapper dto mapper
     * @param <T> dto
     * @return dto list
     * */
    public <T> List<T> query(String sql, Object[] queryArgs, RowMapper<T> rowMapper)
    {
        logger.info(sql);
        return jdbcTemplate.query(sql, queryArgs, rowMapper);
    }


    public int update( String sqlKey, Object... args)
    {
        return jdbcTemplate.update(sqlProvider.getSqlByKey(sqlKey), args);
    }

    public int insert(String sqlKey, Map<String, Object> parameters, boolean requireKey)
    {
        int rows;
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource(parameters);

        NamedParameterJdbcTemplate nameParamJdbcTemp = new NamedParameterJdbcTemplate(jdbcTemplate);

        if ( requireKey )
        {
            KeyHolder keyholder=new GeneratedKeyHolder();
            rows = nameParamJdbcTemp.update(sqlProvider.getSqlByKey(sqlKey), sqlParameterSource, keyholder);
            parameters.put("lastId", keyholder.getKey());
            return rows;
        }

        rows = nameParamJdbcTemp.update(sqlProvider.getSqlByKey(sqlKey), sqlParameterSource);
        return rows;
    }

    public <T> int[][] insertObjects(String sqlKey, List<T> objects, final ParameterizedPreparedStatementSetter<T> pss)
    {
        return jdbcTemplate.batchUpdate( sqlProvider.getSqlByKey(sqlKey), objects, objects.size(), pss );
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

    public <T> PageBean<T> queryDataForPage
    (
        String sql, Object[] queryArgs, RowMapper<T> rowMapper, String countSql, Object[] countArgs, int page, int size
    )
    {
        if (page <= 0)
            throw new RuntimeException("当前页数必须大于1");
        if (size <= 0)
            throw new RuntimeException("每页大小必须大于1");
        //总共数量
        int totalSize = 0;
        try
        {
            totalSize = jdbcTemplate.queryForObject(countSql,countArgs,Integer.class);
        }
        catch (EmptyResultDataAccessException e)
        {   //queryForObject 查询不到数据会抛出 EmptyResultDataAccessException 异常
            totalSize = 0;
        }

        if (totalSize == 0)
        {
            return new PageBean();
        }

        //总页数
        int totalPage = totalSize%size == 0 ? totalSize/size : totalSize/size + 1;
        //开始位置
        int offset = (page -1)*size;
        //结束位置
        int limit = offset + size;
        sql = sql +" limit "+ limit +" offset "+offset;
        logger.info(sql);
        List<T> elements = jdbcTemplate.query(sql,queryArgs,rowMapper);
        return PageBean.<T>build(elements, totalSize, totalPage, page, size);
    }

    public Record save(Record record)
    {
        if ( record == null ) return null;

        SQLAction insertAction = record.insertAction();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        int rows = jdbcTemplate.update(connection ->
        {
            PreparedStatement ps  = connection.prepareStatement(insertAction.getSqlText(), Statement.RETURN_GENERATED_KEYS);
            Object[] parameters = insertAction.getParameters();
            for ( int i = 0, max = parameters.length; i < max; ++i )
            {
                ps.setObject(i+1, parameters[i] );
            }
            return ps;
        }, keyHolder);

        if ( rows > 0 ) record.setField( "id", keyHolder.getKey().intValue() );
        return record;
    }

    public Record modify(Record record)
    {
        if ( record == null ) return null;

        SQLAction modifyAction = record.modifyAction();

        jdbcTemplate.update(modifyAction.getSqlText(), modifyAction.getParameters());

        return record;
    }

    public int delRecordById( final String objectName, int id)
    {
        TextFormatTools textFormat = TextFormatTools.getInstance();
        final String tableName = objectName.indexOf('_') >= 0 ? objectName : camelToSnake(objectName);
        final String sqlText = textFormat.render("delete from {0} where id = ?;", tableName);
        return jdbcTemplate.update(sqlText, Integer.valueOf(id));
    }

    public int getRecordById( final String objectName, int id)
    {
        return 0;
    }

}
