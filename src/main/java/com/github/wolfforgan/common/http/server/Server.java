package com.github.wolfforgan.common.http.server;

import java.io.Closeable;
import java.net.InetSocketAddress;

public interface Server extends Closeable {
    InetSocketAddress startNettyServer(int portNotInUse);

    default InetSocketAddress start(int tryPort) throws Exception {
        return start(tryPort, "");
    }

    default InetSocketAddress start(int tryPort, String serverName) throws Exception {
        return NettyUtil.startServiceOnPort(tryPort, this::startNettyServer, serverName);
    }
}
