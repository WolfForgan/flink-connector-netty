package com.github.wolfforgan.common.registry.nacos;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.github.wolfforgan.common.registry.Registry;
import lombok.*;

import java.util.Properties;

@EqualsAndHashCode(callSuper = true)
public class NacosRegistry extends Registry {
    public NacosRegistry(String serverAddress, String serviceName, String clusterName, String namespace) {
        super(serverAddress, ServerType.NACOS);
        this.serviceName = serviceName;
        this.clusterName = clusterName;
        this.namespace = namespace;
    }

    @Getter
    @Setter
    private String serviceName;

    @Getter
    @Setter
    private String clusterName;

    @Getter
    @Setter
    private String namespace;

    public Properties getProperties() {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, getServerAddress());
        if (getNamespace() != null) {
            properties.put(PropertyKeyConst.NAMESPACE, getNamespace());
        }
        return properties;
    }
}
