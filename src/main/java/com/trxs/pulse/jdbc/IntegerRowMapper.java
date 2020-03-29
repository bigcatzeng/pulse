package com.trxs.pulse.jdbc;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @Author: zengshengwen
 * @Description:
 * @DateTime:  19-1-21 下午9:25
 *
 */
public class IntegerRowMapper implements RowMapper<Integer>
{
    public IntegerRowMapper(){}

    @Override
    public Integer mapRow(ResultSet resultSet, int rowNum) throws SQLException
    {
        return resultSet.getInt(1);
    }
}
