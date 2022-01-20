package com.github.wolfforgan.common.http.server.handler;

import org.apache.flink.streaming.api.functions.source.SourceFunction;

public class HttpBodyCollector implements SourceCollector<String> {
    private final SourceFunction.SourceContext<String> ctx;

    public HttpBodyCollector(SourceFunction.SourceContext<String> ctx) {
        this.ctx = ctx;
    }

    @Override
    public void collect(String element) {
        ctx.collectWithTimestamp(element, System.currentTimeMillis());
    }
}
