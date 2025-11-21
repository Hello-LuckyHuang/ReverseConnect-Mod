package com.hxzhitang.reverseconnect.proxy.coreproxy;

import com.hxzhitang.reverseconnect.Constants;
import com.hxzhitang.reverseconnect.proxy.data.ProxyPacket;
import com.hxzhitang.reverseconnect.proxy.tools.StatusEvent;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class CoreProxyMain implements Runnable {
    private final String edgeHost;
    private final int edgePort;
    protected final String backendHost;
    protected final int backendPort;

    private Channel edgeChannel;

    private EventLoopGroup group;

    protected final StatusEvent statusEvent;

    public CoreProxyMain(String edgeHost, int edgePort, String backendHost, int backendPort, StatusEvent statusEvent) {
        this.edgeHost = edgeHost;
        this.edgePort = edgePort;
        this.backendHost = backendHost;
        this.backendPort = backendPort;
        this.statusEvent = statusEvent;
    }

    public void start() throws Exception {
        statusEvent.updateEvent("reverse_connect.connecting", StatusEvent.Status.START);
        try {
            group = new NioEventLoopGroup();
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
//                        ch.pipeline()
//                                .addLast(new LengthFieldPrepender(4))        // 写入长度头
//                                .addLast(new ProxyPacketEncoder())
//                                .addLast(new CoreProxyHandler(backendHost, backendPort, CoreProxyMain.this));
                            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
                            ch.pipeline().addLast(new LengthFieldPrepender(4));
                            ch.pipeline().addLast(new ObjectEncoder());
                            ch.pipeline().addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
                            ch.pipeline().addLast(new CoreProxyHandler(backendHost, backendPort, CoreProxyMain.this));
                        }
                    });

            ChannelFuture future = bootstrap.connect(edgeHost, edgePort);
            edgeChannel = future.sync().channel();
            Constants.LOG.info("Core Proxy connected to Edge at {}:{}", edgeHost, edgePort);
            statusEvent.updateEvent("reverse_connect.connect_successful", StatusEvent.Status.COMPLETED);

            // 连接失败后退出
            edgeChannel.closeFuture().sync();
        } finally {
            if (group != null) {
                group.shutdownGracefully();
            }
        }
        statusEvent.updateEvent("reverse_connect.closed", StatusEvent.Status.CLOSE);
    }

    public void sendToEdge(ProxyPacket packet) {
        if (edgeChannel != null && edgeChannel.isActive()) {
            edgeChannel.writeAndFlush(packet);
        }
    }

    public void closeConnectToEdgeAndShutdown() {
        if (edgeChannel != null && edgeChannel.isActive()) {
            edgeChannel.close();
            if (group != null) {
                group.shutdownGracefully();
            }
        }
    }

    @Override
    public void run() {
        try {
            start();
        } catch (Exception e) {
            statusEvent.updateEvent("reverse_connect.failed", StatusEvent.Status.ERROR);
        }
    }
}
