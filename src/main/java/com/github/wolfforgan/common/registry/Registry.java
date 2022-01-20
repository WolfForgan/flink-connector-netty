package com.github.wolfforgan.common.registry;

import lombok.*;

import java.io.Serializable;

@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class Registry implements Serializable {
    /**
     * address of registry and discovery server
     * string format: 'ip/domain:port'
     */
    @Getter
    @Setter
    private String serverAddress;

    @Getter
    private ServerType serverType;

    public enum ServerType {
        NACOS,
        EUREKA
    }
}
