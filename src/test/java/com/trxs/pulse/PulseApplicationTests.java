package com.trxs.pulse;

import com.alibaba.fastjson.JSON;
import com.trxs.commons.bean.AccessObject;
import com.trxs.commons.util.SpringUtil;
import com.trxs.pulse.data.CronExpression;
import com.trxs.pulse.service.PulseService;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.junit4.SpringRunner;
import org.xlightweb.HttpRequestHeader;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.NonBlockingBodyDataSource;
import org.xlightweb.client.HttpClient;
import org.xsocket.connection.BlockingConnectionPool;

import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PulseApplicationTests
{
    private static Logger logger = LoggerFactory.getLogger(PulseApplicationTests.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void contextLoads() throws IOException
    {
        BlockingConnectionPool pool = new BlockingConnectionPool();
        HttpClient httpClient = new HttpClient();
        MyResponseHandler respHdl = new MyResponseHandler();
        httpClient.send(new HttpRequestHeader("GET", "http://www.gmx.com/index.html"), respHdl);
        httpClient.close();
    }

    @Test
    public void testDateTime() throws InterruptedException
    {
        DateTime now = new DateTime();
        CronExpression cronExpression = new CronExpression("0/5 * 16-23 3,4 3 ? *");

        AccessObject accessObject = new AccessObject(cronExpression);

        int count = 0;
        long t0 = System.currentTimeMillis();
        for ( int i=0; i < 86400; ++i )
        {
            if ( cronExpression.checkTime(now) )
            {
                count++;
                logger.debug("{}:{}:{} {}-{}-{} week:{} -> {}", now.getHourOfDay(), now.getMinuteOfHour(), now.getSecondOfMinute(), now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), now.getDayOfWeek(), cronExpression.checkTime(now));
            }
            now = now.plusSeconds(1);
        }
        long t1 = System.currentTimeMillis();
        logger.debug("dt={}, count={}", t1-t0, count);
    }

    @Test
    public void testOptional()
    {
        List<String> numbers= Arrays.asList("ONE", "TWO", "THREE");

        Optional<String> numberOpt = numbers.stream()
                .filter(number -> "FOUR".equals(number))
                .findAny();
        boolean isPresent  = numberOpt.isPresent();
        return;
    }

    @Test
    public void testSpringUtil()
    {
        //获取对应的Bean
        Object object = SpringUtil.getBean("");
        String methodName = "";
        try
        {
            //利用反射执行对应方法
            Method method = object.getClass().getMethod(methodName);
            method.invoke(object);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static class MyResponseHandler implements IHttpResponseHandler
    {

        @Override
        public void onResponse(IHttpResponse response) throws IOException {
            int status = response.getStatus();
            NonBlockingBodyDataSource nonBlockingBodyDataSource = response.getNonBlockingBody();
        }

        @Override
        public void onException(IOException ioe) throws IOException {

        }
    }

    @Autowired
    private PulseService pulseService;

    @Test
    public void testJDBC()
    {
        // pulseService.test();
        batchInsertByStatement();
        GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
        String sql = "INSERT INTO `money` (`name`, `money`, `is_deleted`) VALUES (?, ?, ?);";
        batchUpdate(sql, new BatchPreparedStatementSetter()
        {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException
            {
                if (i == 0)
                {
                    preparedStatement.setString(1, "batch 一灰灰7");
                }
                else
                {
                    preparedStatement.setString(1, "batch 一灰灰8");
                }
                preparedStatement.setInt(2, 400);
                byte b = 0;
                preparedStatement.setByte(3, b);
            }

            @Override
            public int getBatchSize() {
                return 2;
            }
        }, generatedKeyHolder);

        List<Map<String, Object>> objectMap = generatedKeyHolder.getKeyList();
        for (Map<String, Object> map : objectMap)
        {
            System.out.println(map.get("GENERATED_KEY"));
        }
        return;
    }

    /**
     * 新增数据，并返回主键id
     *
     * @return
     */
    private int insertAndReturnId()
    {
        String sql = "INSERT INTO `money` (`name`, `money`, `is_deleted`) VALUES (?, ?, ?);";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection ->
        {
            // 指定主键
            PreparedStatement preparedStatement = connection.prepareStatement(sql, new String[]{"id"});
            preparedStatement.setString(1, "一灰灰5");
            preparedStatement.setInt(2, 500);
            byte b = 0;
            preparedStatement.setByte(3, b);

            return preparedStatement;
        }, keyHolder);
        return keyHolder.getKey().intValue();
    }


    private void batchInsertByStatement()
    {
        String sql = "INSERT INTO `money` (`name`, `money`, `is_deleted`) VALUES (?, ?, ?);";

        int[] ans = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter()
        {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException
            {
                if (i == 0)
                {
                    preparedStatement.setString(1, "batch 一灰灰5");
                }
                else
                {
                    preparedStatement.setString(1, "batch 一灰灰6");
                }
                preparedStatement.setInt(2, 300);
                byte b = 0;
                preparedStatement.setByte(3, b);

                logger.debug("{}", preparedStatement.toString());
            }

            @Override
            public int getBatchSize()
            {
                return 2;
            }
        });
        System.out.println("batch insert by statement: " + JSON.toJSONString(ans));
    }

    /*

     */
    private void batchUpdate(String sql, final BatchPreparedStatementSetter pss, final KeyHolder generatedKeyHolder)
    {
        jdbcTemplate.execute
        (
            (PreparedStatementCreator) con -> con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS),
            ps ->
            {
                try
                {
                    int batchSize = pss.getBatchSize();
                    int totalRowsAffected = 0;
                    int[] rowsAffected = new int[batchSize];
                    List generatedKeys = generatedKeyHolder.getKeyList();
                    generatedKeys.clear();
                    ResultSet keys = null;
                    for (int i = 0; i < batchSize; i++)
                    {
                        pss.setValues(ps, i);
                        rowsAffected[i] = ps.executeUpdate();
                        totalRowsAffected += rowsAffected[i];
                        try
                        {
                            keys = ps.getGeneratedKeys();
                            if (keys != null)
                            {
                                RowMapper rowMapper = new ColumnMapRowMapper();
                                RowMapperResultSetExtractor rse = new RowMapperResultSetExtractor(rowMapper, 1);
                                generatedKeys.addAll(rse.extractData(keys));
                            }
                        }
                        finally
                        {
                            JdbcUtils.closeResultSet(keys);
                        }
                    }
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("SQL batch update affected " + totalRowsAffected + " rows and returned " +
                                generatedKeys.size() + " keys");
                    }
                    return rowsAffected;
                }
                finally
                {
                    if (pss instanceof ParameterDisposer)
                    {
                        ((ParameterDisposer) pss).cleanupParameters();
                    }
                }
            }
        );
    }


}
