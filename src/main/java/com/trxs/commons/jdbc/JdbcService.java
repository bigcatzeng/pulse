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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author zengshengwen 2019 19-1-17 下午11:43
 */
public class JdbcService
{
    protected static Logger logger = LoggerFactory.getLogger(JdbcService.class.getName());
    protected static SQLProvider sqlProvider = new SQLProvider();

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    public void init()
    {
        sqlProvider.init("/templates/default.sql");
    }

    public void init( String sourcePath )
    {
        sqlProvider.init(sourcePath);
    }


    protected Connection getConnection() throws SQLException
    {
        return jdbcTemplate.getDataSource().getConnection();
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

    public int updateByMap(String key, Map<String, Object> parameters )
    {
        int rows;
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource(parameters);

        NamedParameterJdbcTemplate nameParamJdbcTemp = new NamedParameterJdbcTemplate(jdbcTemplate);

        rows = nameParamJdbcTemp.update(sqlProvider.getSQL(key), sqlParameterSource);
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
}
