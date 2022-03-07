package com.github.wolfforgan.job;

import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.github.wolfforgan.common.http.client.HttpClientHelper;
import com.github.wolfforgan.common.registry.RegistryFactory;
import com.github.wolfforgan.common.registry.nacos.NacosRegistry;
import com.github.wolfforgan.entity.MapEntity;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
public class HttpReceiverJobTest {

    @org.junit.Before
    public void setUp() throws Exception {
    }

    @org.junit.After
    public void tearDown() throws Exception {
    }

    /**
     * boost a http client to send data to flink-connector-http
     * @throws InterruptedException
     */
    @Test
    public void HttpClientTest() throws InterruptedException {
        Gson gson = new GsonBuilder().create();
        ExecutorService pool = Executors.newFixedThreadPool(10);
        int index = 1000000;
        while (--index > 0)
        {
            int finalIndex = index;
            pool.submit(() ->
            {
                MapEntity data = new MapEntity(Thread.currentThread().getName(), String.valueOf(finalIndex), System.currentTimeMillis());
                String content = gson.toJson(data);
                String out = HttpClientHelper.post("http://localhost:1978/flink/source", content);
                log.info("{}, res: {}", content, out);
            });
        }
        Thread.sleep(Integer.MAX_VALUE);
    }

    @Test
    public void registerNacos() throws Exception {
        NacosRegistry registry = RegistryFactory.fromNacos("ip:port", "flink-connector-http", "group-name","cluster-name", "namespace-id");
        NamingService naming = NamingFactory.createNamingService(registry.getProperties());
        log.debug("serviceName:{}, clusterName:{}", registry.getServiceName(), registry.getClusterName());
        naming.registerInstance(registry.getServiceName(), "local ip", 1978, registry.getClusterName());
        log.info("register successfully");
    }
}