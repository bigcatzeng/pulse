package com.trxs.pulse;

import com.trxs.commons.util.SpringUtil;
import com.trxs.pulse.data.CronExpression;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.xlightweb.HttpRequestHeader;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.NonBlockingBodyDataSource;
import org.xlightweb.client.HttpClient;
import org.xsocket.connection.BlockingConnectionPool;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PulseApplicationTests
{
    private static Logger logger = LoggerFactory.getLogger(PulseApplicationTests.class);

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
        } catch (Exception e)
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
}
