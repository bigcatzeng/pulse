package com.trxs.commons.http;

import org.apache.http.Consts;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Lookup;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.auth.*;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AsyncHttpClient
{
    private static Logger logger = LoggerFactory.getLogger(AsyncHttpClient.class.getSimpleName());

    private static int       poolSize = 3000;  // 连接池最大连接数
    private static int    maxPerRoute = 1500;  // 每个主机的并发最多只有1500
    private static int  socketTimeout = 5000;  // 设置等待数据超时时间5秒钟 根据业务调整
    private static int connectTimeout = 15000; // 连接超时

    // http 代理相关参数
    private int        port = 0;
    private String     host = "";
    private String username = "";
    private String password = "";

    // 异步 http client
    private CloseableHttpAsyncClient closeableHttpAsyncClient;

    // 异步加代理的 http client
    private CloseableHttpAsyncClient proxyCloseableHttpAsyncClient;

    private AsyncHttpClient()
    {
        try
        {
            closeableHttpAsyncClient = createCloseableHttpAsyncClient(false);
            proxyCloseableHttpAsyncClient = createCloseableHttpAsyncClient(true);
            closeableHttpAsyncClient.start();
        }
        catch ( Exception e )
        {
            logger.warn("{}", e.getMessage());
            Arrays.asList(e.getStackTrace()).forEach(stackTraceElement -> logger.warn("{class:{}, method:{}, file:{}, line:{}.", stackTraceElement.getClassName(), stackTraceElement.getMethodName(), stackTraceElement.getFileName(), stackTraceElement.getLineNumber()));
        }
    }

    public static AsyncHttpClient getInstance()
    {
        return Singleton.INSTANCE.getInstance();
    }

    public CloseableHttpAsyncClient createCloseableHttpAsyncClient(boolean proxy) throws IOReactorException
    {
        SSLContext sslcontext = SSLContexts.createDefault();
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectTimeout).setSocketTimeout(socketTimeout).build();

        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, credentials);

        // 设置协议http和https对应的处理socket链接工厂的对象
        RegistryBuilder<SchemeIOSessionStrategy> registryBuilder = RegistryBuilder.create();
        registryBuilder.register("http", NoopIOSessionStrategy.INSTANCE);
        registryBuilder.register("https", new SSLIOSessionStrategy(sslcontext));
        Registry<SchemeIOSessionStrategy> sessionStrategyRegistry = registryBuilder.build();

        // 配置io线程
        IOReactorConfig ioReactorConfig = IOReactorConfig.custom().setIoThreadCount(Runtime.getRuntime().availableProcessors()).build();

        // 设置连接池大小
        ConnectingIOReactor ioReactor;
        ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
        PoolingNHttpClientConnectionManager poolingNHttpConnectionManager = new PoolingNHttpClientConnectionManager( ioReactor, null, sessionStrategyRegistry, null);

        if (poolSize > 0)
        {
            poolingNHttpConnectionManager.setMaxTotal(poolSize);
        }

        if (maxPerRoute > 0)
        {
            poolingNHttpConnectionManager.setDefaultMaxPerRoute(maxPerRoute);
        }
        else
        {
            poolingNHttpConnectionManager.setDefaultMaxPerRoute(10);
        }

        ConnectionConfig connectionConfig = ConnectionConfig.custom().
                setMalformedInputAction(CodingErrorAction.IGNORE).
                setUnmappableInputAction(CodingErrorAction.IGNORE).
                setCharset(Consts.UTF_8).build();

        Lookup<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider>create()
                .register(AuthSchemes.BASIC, new BasicSchemeFactory())
                .register(AuthSchemes.DIGEST, new DigestSchemeFactory())
                .register(AuthSchemes.NTLM, new NTLMSchemeFactory())
                .register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory())
                .register(AuthSchemes.KERBEROS, new KerberosSchemeFactory()).build();

        poolingNHttpConnectionManager.setDefaultConnectionConfig(connectionConfig);

        if (proxy)
        {
            return HttpAsyncClients.custom().setConnectionManager(poolingNHttpConnectionManager)
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .setDefaultAuthSchemeRegistry(authSchemeRegistry)
                    .setProxy(new HttpHost(host, port))
                    .setDefaultCookieStore(new BasicCookieStore())
                    .setDefaultRequestConfig(requestConfig).build();
        }
        else
        {
            return HttpAsyncClients.custom().setConnectionManager(poolingNHttpConnectionManager)
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .setDefaultAuthSchemeRegistry(authSchemeRegistry)
                    .setDefaultCookieStore(new BasicCookieStore()).build();
        }
    }

    public void httpCall( String requestUrl,
                          RequestMethod method,
                          List<BasicNameValuePair> params,
                          List<BasicNameValuePair> postBody,
                          FutureCallback callback )
    {
        Objects.requireNonNull(    requestUrl, "异步HTTP请求地址不能为空!" );
        Objects.requireNonNull(        method, "异步HTTP请求方法不能为空!" );
        Objects.requireNonNull(      callback, "异步HTTP请求回调接口不能为空!" );

        HttpRequestBase httpRequest = newHttpRequest(requestUrl, method);

        HttpClientContext localContext = HttpClientContext.create();
        BasicCookieStore cookieStore = new BasicCookieStore();

    }

    private HttpRequestBase newHttpRequest(String requestUrl, RequestMethod method)
    {
        switch(method)
        {
            case POST:
                return new HttpPost(requestUrl);
            case PUT:
                return new HttpPut(requestUrl);
            case DELETE:
                return new HttpDelete(requestUrl);
            case GET:
                return new HttpGet(requestUrl);
            default:
                return null;
        }
    }

    private enum Singleton
    {
        INSTANCE;
        private AsyncHttpClient singleton;
        Singleton()
        {
            singleton = new AsyncHttpClient();
        }
        public AsyncHttpClient getInstance()
        {
            return singleton;
        }
    }
}
