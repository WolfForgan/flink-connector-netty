package com.github.wolfforgan.common.http.server.handler;

import lombok.extern.log4j.Log4j2;
import org.apache.flink.shaded.netty4.io.netty.buffer.Unpooled;
import org.apache.flink.shaded.netty4.io.netty.channel.*;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.*;
import org.apache.flink.shaded.netty4.io.netty.util.AttributeKey;
import org.apache.flink.shaded.netty4.io.netty.util.CharsetUtil;

import java.util.Objects;

@Log4j2
@ChannelHandler.Sharable
public abstract class AbstractHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    public static final AttributeKey<Boolean> KEEP_ALIVE_KEY = AttributeKey.valueOf("KEEP_ALIVE");
    public static final AttributeKey<HttpVersion> HTTP_VERSION_KEY = AttributeKey.valueOf("HTTP_VERSION");
    protected final String uri;

    public AbstractHttpHandler(String uri) {
        this.uri = uri;
    }

    protected abstract void handleRequest(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest);

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) {
        if (HttpUtil.isKeepAlive(fullHttpRequest)) {
            channelHandlerContext.channel().attr(KEEP_ALIVE_KEY).set(true);
        }
        channelHandlerContext.channel().attr(HTTP_VERSION_KEY).set(fullHttpRequest.protocolVersion());
        handleRequest(channelHandlerContext, fullHttpRequest);
    }

    protected void sendJsonContent(ChannelHandlerContext ctx, HttpResponseStatus status) {
        sendJsonContent(ctx, status, "");
    }

    protected void sendJsonContent(ChannelHandlerContext ctx, HttpResponseStatus status, String content) {
        HttpVersion version = getHttpVersion(ctx);
        FullHttpResponse response;
        if (Objects.requireNonNull(content).length() > 0) {
            response = new DefaultFullHttpResponse(
                    version, status, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
        } else {
            response = new DefaultFullHttpResponse(version, status);
        }
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        sendAndCleanupConnection(ctx, response);
    }

    private HttpVersion getHttpVersion(ChannelHandlerContext ctx) {
        return ctx.channel().attr(HTTP_VERSION_KEY).get();
    }

    private void sendAndCleanupConnection(
            ChannelHandlerContext ctx, FullHttpResponse response)
    {
        final boolean keepAlive =
                ctx.channel().attr(KEEP_ALIVE_KEY).get();
        HttpUtil.setContentLength(
                response, response.content().readableBytes());
        if (!keepAlive)
        {
            // ?????????????????????????????? connection:close ??????
            response.headers().set(
                    HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        } else if (isHTTP_1_0(ctx))
        {
            // ????????? 1.0 ??????????????????????????? connection:keep-alive ??????
            response.headers().set(
                    HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        //????????????
        ChannelFuture flushPromise = ctx.writeAndFlush(response);
        if (!keepAlive)
        {
            // ?????????????????????????????????????????????????????????
            flushPromise.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private boolean isHTTP_1_0(ChannelHandlerContext ctx) {
        HttpVersion version = getHttpVersion(ctx);
        return version.majorVersion() == 1 && version.minorVersion() == 0;
    }
}