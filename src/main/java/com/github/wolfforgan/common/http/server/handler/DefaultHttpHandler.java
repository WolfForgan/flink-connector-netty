package com.github.wolfforgan.common.http.server.handler;

import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandlerContext;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.FullHttpRequest;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpMethod;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpResponseStatus;

import java.nio.charset.StandardCharsets;

public final class DefaultHttpHandler extends AbstractHttpHandler {
    public DefaultHttpHandler(SourceCollector<String> collector, String uri) {
        super(collector, uri);
    }

    @Override
    protected void handleRequest(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) {
        if (!fullHttpRequest.method().equals(HttpMethod.POST)) {   //必须是POST方法
            sendJsonContent(channelHandlerContext, HttpResponseStatus.METHOD_NOT_ALLOWED, "POST is supported only");
            return;
        }
        if (fullHttpRequest.protocolVersion().majorVersion() > 1) {   //仅支持HTTP1.0和1.1
            sendJsonContent(channelHandlerContext, HttpResponseStatus.HTTP_VERSION_NOT_SUPPORTED, "HTTP1.0 and HTTP1.1 are just supported");
            return;
        }
        if (!fullHttpRequest.uri().equals(uri)) {
            sendJsonContent(channelHandlerContext, HttpResponseStatus.NOT_FOUND);
            return;
        }
        ByteBuf content = fullHttpRequest.content();
        byte[] reqContent = new byte[content.readableBytes()];
        content.readBytes(reqContent);
        String text = new String(reqContent, StandardCharsets.UTF_8);
        collector.collect(text);
        sendJsonContent(channelHandlerContext, HttpResponseStatus.OK, "received");
    }
}
