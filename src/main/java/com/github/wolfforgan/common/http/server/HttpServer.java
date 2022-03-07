package com.github.wolfforgan.common.http.server;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.github.wolfforgan.common.http.server.handler.AbstractHttpHandler;
import com.github.wolfforgan.common.registry.Registry;
import com.github.wolfforgan.common.registry.nacos.NacosRegistry;
import lombok.extern.log4j.Log4j2;
import org.apache.flink.shaded.netty4.io.netty.bootstrap.ServerBootstrap;
import org.apache.flink.shaded.netty4.io.netty.buffer.PooledByteBufAllocator;
import org.apache.flink.shaded.netty4.io.netty.channel.*;
import org.apache.flink.shaded.netty4.io.netty.channel.nio.NioEventLoopGroup;
import org.apache.flink.shaded.netty4.io.netty.channel.socket.SocketChannel;
import org.apache.flink.shaded.netty4.io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpObjectAggregator;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpServerCodec;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class HttpServer extends RegistrableServer {
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private InetSocketAddress currentAddress = null;
    private final NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final NioEventLoopGroup workerGroup = new NioEventLoopGroup();

    private final AbstractHttpHandler httpHandler;
    private final Registry registry;

    public HttpServer(AbstractHttpHandler httpHandler, Registry registry) {
        this.httpHandler = httpHandler;
        this.registry = registry;
    }

    @Override
    public synchronized InetSocketAddress startNettyServer(int portNotInUse) {
        if (!isRunning.get()) {
            log.debug("try listen on port {}", portNotInUse);
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(portNotInUse)
                    //.option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(65535));
                            ch.pipeline().addLast(httpHandler);
                        }
                    });
            ChannelFuture future;
            try {
                future = bootstrap.bind().sync();
            } catch (Exception e) {
                log.warn("fail to bind", e);
                throw new RuntimeException(e);
            }
            log.info("HTTP server listen on port {}", portNotInUse);
            Channel ch = future.channel();
            isRunning.set(true);
            currentAddress = (InetSocketAddress) ch.localAddress();
            log.debug("currentAddress: {}", currentAddress);
            register(currentAddress, registry);
            ch.closeFuture().syncUninterruptibly();
        }
        return currentAddress;
    }

    @Override
    public void close() {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
        if (hasRegister.get()) {
            if (registry instanceof NacosRegistry) {
                NacosRegistry nacosRegistry = (NacosRegistry) registry;
                try {
                    NamingService naming = NamingFactory.createNamingService(nacosRegistry.getProperties());
                    log.debug("serviceName:{}, clusterName:{}", nacosRegistry.getServiceName(), nacosRegistry.getClusterName());
                    naming.deregisterInstance(nacosRegistry.getServiceName(), registerIp, registerPort, nacosRegistry.getClusterName());
                    log.info("deregister successfully, serviceName:{}, ip:{}, port:{}, nacosServer:{}, namespace:{}, clusterName:{}",
                            nacosRegistry.getServiceName(), registerIp, registerPort, nacosRegistry.getServerAddress(), nacosRegistry.getNamespace(), nacosRegistry.getClusterName());
                } catch (NacosException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        log.info("successfully close netty server source");
    }
}
