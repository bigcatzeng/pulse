package com.trxs.pulse;

import com.trxs.commons.util.SpringUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
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
