package com.github.wolfforgan.common.http.client;

import lombok.extern.log4j.Log4j2;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.pool.PoolStats;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class HttpClientHelper {
    private static final long KEEP_ALIVE_TIMEOUT = 600000;
    private static final int CONNECT_TIMEOUT = 2000;
    private static final int SOCKET_TIMEOUT = 2000;
    private static final int REQUEST_TIMEOUT = 2000;
    private static final int EXPIRED_CHECK_GAP = 6000;
    private static final int VALIDATE_AFTER_INACTIVITY = 2000;
    private static final int MAX_CONNECTION = 500;
    private static final int MAX_PER_ROUTE = 500;
    private static final int IDLE_CHECK_GAP = 6;
    private static PoolingHttpClientConnectionManager
            httpClientConnectionManager;
    private static CloseableHttpClient pooledHttpClient;
    private static ScheduledExecutorService monitorExecutor = null;

    public static void createHttpClientConnectionManager()
    {
        DnsResolver dnsResolver = SystemDefaultDnsResolver.INSTANCE;
        ConnectionSocketFactory plainSocketFactory =
                PlainConnectionSocketFactory.getSocketFactory();
        LayeredConnectionSocketFactory sslSocketFactory =
                SSLConnectionSocketFactory.getSocketFactory();
        Registry<ConnectionSocketFactory> registry =
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", plainSocketFactory)
                        .register("https", sslSocketFactory)
                        .build();
        httpClientConnectionManager =
                new PoolingHttpClientConnectionManager(
                        registry,
                        null,
                        null,
                        dnsResolver,
                        KEEP_ALIVE_TIMEOUT,
                        TimeUnit.MILLISECONDS);
        httpClientConnectionManager.setValidateAfterInactivity(
                VALIDATE_AFTER_INACTIVITY);
        httpClientConnectionManager.setMaxTotal(MAX_CONNECTION);
        httpClientConnectionManager.setDefaultMaxPerRoute(MAX_PER_ROUTE);
    }

    /**
     * 定时处理线程：对异常连接进行关闭
     */
    private static void startExpiredConnectionsMonitor()
    {
        int idleCheckGap = IDLE_CHECK_GAP;
        long keepAliveTimeout = KEEP_ALIVE_TIMEOUT;
        monitorExecutor = Executors.newScheduledThreadPool(1);
        monitorExecutor.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                httpClientConnectionManager.closeExpiredConnections();
                httpClientConnectionManager.closeIdleConnections(
                        keepAliveTimeout, TimeUnit.MILLISECONDS);
                PoolStats status =
                        httpClientConnectionManager.getTotalStats();
                 /*
                 log.info(" manager.getRoutes().size():" +
                manager.getRoutes().size());
                 log.info(" status.getAvailable():" +
                status.getAvailable());
                 log.info(" status.getPending():" + status.getPending());
                 log.info(" status.getLeased():" + status.getLeased());
                 log.info(" status.getMax():" + status.getMax());
                 */
            }
        }, idleCheckGap, idleCheckGap, TimeUnit.MILLISECONDS);
    }

    public static CloseableHttpClient pooledHttpClient()
    {
        if (null != pooledHttpClient)
        {
            return pooledHttpClient;
        }
        createHttpClientConnectionManager();log.info("Apache httpclient pool initialization started");
        RequestConfig.Builder requestConfigBuilder =
                RequestConfig.custom();
        requestConfigBuilder.setSocketTimeout(SOCKET_TIMEOUT);
        requestConfigBuilder.setConnectTimeout(CONNECT_TIMEOUT);
        requestConfigBuilder.setConnectionRequestTimeout(
                REQUEST_TIMEOUT);
        RequestConfig config = requestConfigBuilder.build();
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        httpClientBuilder.setConnectionManager(
                httpClientConnectionManager);
        httpClientBuilder.setDefaultRequestConfig(config);
        httpClientBuilder.setKeepAliveStrategy(
                new ConnectionKeepAliveStrategy()
                {
                    @Override
                    public long getKeepAliveDuration(
                            HttpResponse response, HttpContext context)
                    {
                        HeaderElementIterator it = new BasicHeaderElementIterator
                                (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                        while (it.hasNext())
                        {
                            HeaderElement he = it.nextElement();
                            String param = he.getName();
                            String value = he.getValue();
                            if (value != null && param.equalsIgnoreCase
                                    ("timeout"))
                            {
                                try
                                {
                                    return Long.parseLong(value) * 1000;
                                } catch (final NumberFormatException ignore){
                                }
                            }
                        }
                        return KEEP_ALIVE_TIMEOUT;
                    }
                });
        pooledHttpClient = httpClientBuilder.build();
        log.info("Apache httpclient pool initialization finisheded");
        startExpiredConnectionsMonitor();
        return pooledHttpClient;
    }

    public static String get(String url) {
        CloseableHttpClient client = pooledHttpClient();
        HttpGet httpGet = new HttpGet(url);
        return poolRequestData(url, client, httpGet);
    }

    public static String post(String url, String content) {
        CloseableHttpClient client = pooledHttpClient();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new ByteArrayEntity(content.getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_JSON));
        return poolRequestData(url, client, httpPost);
    }

    private static String poolRequestData(
            String url, CloseableHttpClient client, HttpRequest request) {
        CloseableHttpResponse response = null;
        InputStream in = null;
        String result = null;
        try
        {
            HttpHost httpHost = getHost(url);
            response = client.execute(
                    httpHost, request, HttpClientContext.create());
            HttpEntity entity = response.getEntity();
            if (entity != null)
            {
                in = entity.getContent();
                result = getStringFromInputStream(in, "utf-8");
            }
        } catch (IOException e) {
            log.error("client execution fail", e);
        } finally {
            quietlyClose(in);
            quietlyClose(response);
        }
        return result;
    }

    private static void quietlyClose(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

    private static void quietlyClose(InputStream in){
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

    private static String getStringFromInputStream(InputStream in, String s) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1000];
        int n;
        while ((n = in.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, n, s));
        }
        return sb.toString();
    }

    private static HttpHost getHost(String url) {
        Pattern p = Pattern.compile("http://[A-Za-z0-9.]+:?\\d*");
        Matcher m = p.matcher(url);
        while (m.find()) {
            String sub = url.substring(m.start(), m.end());
            return HttpHost.create(sub);
        }
        throw new RuntimeException("bad url format");
    }
}
