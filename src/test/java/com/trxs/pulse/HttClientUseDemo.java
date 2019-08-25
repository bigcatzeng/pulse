package com.trxs.pulse;

import java.util.ArrayList;
import java.util.List;

import com.trxs.commons.net.HttpClientService;
import org.apache.http.HttpResponse;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * http client 使用
 * */
public class HttClientUseDemo extends HttpClientService {
    private static Logger logger = LoggerFactory.getLogger(HttClientUseDemo.class);
    public static void main(String[] args) {

        new HttClientUseDemo().httpGet();
    }

    public void httpGet()
    {
        String url = "http://192.168.150.105:9018/information/bdsearch/baseinfo"; //
        // ?userStatus=2,3
        // &startDate=2019-03-26
        // &endDate=2019-03-27
        // &_offset=0
        // &pageSize=10000
        // &metrics=user_name,create_time";
        List<BasicNameValuePair> urlParams = new ArrayList<BasicNameValuePair>();
        urlParams.add(new BasicNameValuePair("userStatus", "2,3"));
        urlParams.add(new BasicNameValuePair("startDate", "2019-03-26"));
        urlParams.add(new BasicNameValuePair("endDate", "2019-03-27"));
        urlParams.add(new BasicNameValuePair("_offset", "0"));
        urlParams.add(new BasicNameValuePair("pageSize", "10000"));
        urlParams.add(new BasicNameValuePair("metrics", "user_name,create_time"));
        exeHttpReq(url, false, urlParams, null, new BiCall());

    }

    public void getConfCall() {

        String url = "http://192.168.150.105:9018/information/bdsearch/baseinfo?userStatus=2,3&startDate=2019-03-26&endDate=2019-03-27&_offset=0&pageSize=10000&metrics=user_name,create_time";

        List<BasicNameValuePair> urlParams = new ArrayList<BasicNameValuePair>();
        urlParams.add(new BasicNameValuePair("appid", "2"));
        exeHttpReq(url, false, urlParams, null, new GetConfCall());
    }

    public void exeHttpReq(String baseUrl, boolean isPost,
                           List<BasicNameValuePair> urlParams,
                           List<BasicNameValuePair> postBody,
                           FutureCallback<HttpResponse> callback) {

        try {
            System.out.println("enter exeAsyncReq");
            exeAsyncReq(baseUrl, isPost, urlParams, postBody, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    class BiCall implements FutureCallback<HttpResponse>
    {
        @Override
        public void completed(HttpResponse response)
        {
            logger.debug("getStatusCode:{}", response.getStatusLine().getStatusCode());
            logger.debug("content:{}", getHttpContent(response));
            HttpClientUtils.closeQuietly(response);
        }

        @Override
        public void failed(Exception e)
        {

        }

        @Override
        public void cancelled()
        {

        }
    }

    /**
     * 被回调的对象，给异步的httpclient使用
     *
     * */
    class GetConfCall implements FutureCallback<HttpResponse> {

        /**
         * 请求完成后调用该函数
         */
        @Override
        public void completed(HttpResponse response) {

            System.out.println(response.getStatusLine().getStatusCode());
            System.out.println(getHttpContent(response));

            HttpClientUtils.closeQuietly(response);

        }

        /**
         * 请求取消后调用该函数
         */
        @Override
        public void cancelled() {

        }

        /**
         * 请求失败后调用该函数
         */
        @Override
        public void failed(Exception e) {

        }

    }
}