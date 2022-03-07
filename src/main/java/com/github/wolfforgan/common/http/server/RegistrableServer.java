package com.github.wolfforgan.common.http.server;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.github.wolfforgan.common.registry.Registry;
import com.github.wolfforgan.common.registry.nacos.NacosRegistry;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public abstract class RegistrableServer implements Server {
    protected String registerIp;
    protected int registerPort;
    protected AtomicBoolean hasRegister = new AtomicBoolean(false);

    protected void register(InetSocketAddress address, Registry registry) {
        if (registry == null) {
            return;
        }
        log.debug("try to register");
        String ip = address.getAddress().getHostAddress();
        try {
            registerIp = (ip.startsWith("0") || ip.startsWith("127")) ? NettyUtil.findLocalInetAddress().getHostAddress() : ip;
        } catch (Exception e) {
            throw new RuntimeException("fail to get local IP", e);
        }
        log.debug("registerIp: {}", registerIp);
        registerPort = address.getPort();
        log.debug("port:{}", registerPort);
        if (registry instanceof NacosRegistry) {
            NacosRegistry nacosRegistry = (NacosRegistry) registry;
            try {
                NamingService naming = NamingFactory.createNamingService(nacosRegistry.getProperties());
                log.debug("serviceName:{}, clusterName:{}", nacosRegistry.getServiceName(), nacosRegistry.getClusterName());
                naming.registerInstance(nacosRegistry.getServiceName(), nacosRegistry.getGroup(), registerIp, registerPort, nacosRegistry.getClusterName());
                hasRegister.set(true);
                log.info("register successfully, serviceName:{}, ip:{}, port:{}, nacosServer:{}, namespace:{}, group:{}, clusterName:{}",
                        nacosRegistry.getServiceName(), registerIp, registerPort, nacosRegistry.getServerAddress(),
                        nacosRegistry.getNamespace(), nacosRegistry.getGroup(), nacosRegistry.getClusterName());
            } catch (NacosException e) {
                throw new RuntimeException(e);
            }
        }
        // else if (registry instanceof ...) {}
    }
}
