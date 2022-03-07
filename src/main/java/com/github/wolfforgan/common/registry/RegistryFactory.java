package com.github.wolfforgan.common.registry;

import com.github.wolfforgan.common.registry.nacos.NacosRegistry;

public class RegistryFactory {
    public static NacosRegistry fromNacos(String serverAddress, String serviceName, String group, String clusterName, String namespace) {
        return new NacosRegistry(serverAddress, serviceName, group, clusterName, namespace);
    }
}
