package com.trxs.commons.net;

/**
 *
 * httpclient 工厂类
 * */
public class  HttpClientFactory
{
    private static HttpSyncClient httpSyncClient = new HttpSyncClient();
    private static HttpAsyncClient httpAsyncClient = new HttpAsyncClient();

    private HttpClientFactory()
    {
    }

    private static HttpClientFactory httpClientFactory = new HttpClientFactory();

    public static HttpClientFactory getInstance()
    {

        return httpClientFactory;

    }

    public HttpAsyncClient getHttpAsyncClientPool()
    {
        return httpAsyncClient;
    }

    public HttpSyncClient getHttpSyncClientPool()
    {
        return httpSyncClient;
    }

}