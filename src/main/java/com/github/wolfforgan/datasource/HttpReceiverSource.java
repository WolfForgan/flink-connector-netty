package com.github.wolfforgan.datasource;

import com.github.wolfforgan.common.http.server.handler.DefaultHttpHandler;
import com.github.wolfforgan.common.http.server.handler.HttpBodyCollector;
import com.github.wolfforgan.common.http.server.HttpServer;
import com.github.wolfforgan.common.registry.Registry;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;

public final class HttpReceiverSource extends RichParallelSourceFunction<String> {
    private final String uri;
    private final int tryPort;
    private final Registry registry;
    private final String serverName;
    private HttpServer server;

    public HttpReceiverSource(String uri, int tryPort, Registry registry, String serverName) {
        this.uri = uri;
        this.tryPort = tryPort;
        this.registry = registry;
        this.serverName = serverName;
    }

    @Override
    public void run(SourceContext<String> ctx) throws Exception {
        server = new HttpServer(new DefaultHttpHandler(new HttpBodyCollector(ctx), uri), registry);
        server.start(tryPort, serverName);
    }

    @Override
    public void cancel() {
        server.close();
    }
}
