package com.hxzhitang.reverseconnect.proxy.edgeproxy;

import com.hxzhitang.reverseconnect.Constants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class EdgeProxyServer implements Runnable {
    private final int userListenPort;   // 用户连接端口，如 7000
    private final int coreAcceptPort;   // 等待 Core 连接的端口，如 9000

    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private Channel userServerChannel;
    protected static volatile Channel coreChannel = null; // Core 连接通道
    protected static volatile int clientCount = 0; // 连接计数

    private final Map<String, Channel> sessionToUser = new ConcurrentHashMap<>();
    private final Map<Channel, String> channelToId = new ConcurrentHashMap<>();

    ChannelFuture bindFuture1;
    ChannelFuture bindFuture2;

    public EdgeProxyServer(int userListenPort, int coreAcceptPort) {
        this.userListenPort = userListenPort;
        this.coreAcceptPort = coreAcceptPort;
    }

    public void start() throws InterruptedException {
        // Step 1: 启动用户接入服务（用户连接 Edge）
        startUserServer();

        // Step 2: 启动 Core 接入服务（Core 连接 Edge）
        startCoreAcceptor();

        // Step 3: 阻塞等待用户服务关闭
//        if (userServerChannel != null) {
//            userServerChannel.closeFuture().sync();
//        }
        bindFuture1.channel().closeFuture().sync();
        bindFuture2.channel().closeFuture().sync();
    }

    /**
     * 启动用户接入服务（监听 7000）
     */
    private void startUserServer() {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline().addLast(new UserChannelHandler(getCoreChannelSupplier(), sessionToUser, channelToId));
                    }
                });

        bindFuture1 = serverBootstrap.bind(userListenPort);
        bindFuture1.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                userServerChannel = future.channel();
                Constants.LOG.info("Edge Proxy is listening for users on port: {}", userListenPort);
            } else {
                Constants.LOG.info("Failed to bind user port: {}", userListenPort);
            }
        });
    }

    /**
     * 启动 Core 接入服务（监听 9000，等待 Core 连接）
     */
    private void startCoreAcceptor() {
        ServerBootstrap coreBootstrap = new ServerBootstrap();
        coreBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        Constants.LOG.info("CoreProxy connected from: {}", ch.remoteAddress());
//                        if (coreChannel == null)
//                            coreChannel = ch;

//                        ch.pipeline()
//                                .addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4)) // 解码长度
//                                .addLast(new ProxyPacketDecoder())
//                                .addLast(new EdgeToCoreHandler()); // 处理来自 Core 的响应
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
                        ch.pipeline().addLast(new LengthFieldPrepender(4));
                        ch.pipeline().addLast(new ObjectEncoder());
                        ch.pipeline().addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
                        ch.pipeline().addLast(new EdgeToCoreHandler(sessionToUser, channelToId));
                    }
                });

        bindFuture2 = coreBootstrap.bind(coreAcceptPort);
        bindFuture2.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                Constants.LOG.info("Edge is now accepting CoreProxy connections on port: {}", coreAcceptPort);
            } else {
                Constants.LOG.info("Failed to bind Core accept port: {}", coreAcceptPort);
                // 可重试
            }
        });
    }

    // 关闭服务
    public void close() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    /**
     * 安全获取 coreChannel（可能为 null）
     */
    private Supplier<Channel> getCoreChannelSupplier() {
        return () -> coreChannel;
    }

    public static int getClientCount() {
        return clientCount;
    }

    @Override
    public void run() {
        try {
            start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
