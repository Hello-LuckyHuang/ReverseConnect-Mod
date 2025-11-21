package com.hxzhitang.reverseconnect.proxy.coreproxy;

import com.hxzhitang.reverseconnect.proxy.data.ProxyPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CoreProxyHandler extends SimpleChannelInboundHandler<ProxyPacket> {

    private final String backendHost;
    private final int backendPort;
    private final CoreProxyMain coreProxy;

    private final Map<String, Channel> sessionToBackend = new ConcurrentHashMap<>();
    private final Map<Channel, String> backendToSession = new ConcurrentHashMap<>();

    public CoreProxyHandler(String backendHost, int backendPort, CoreProxyMain coreProxy) {
        this.backendHost = backendHost;
        this.backendPort = backendPort;
        this.coreProxy = coreProxy;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyPacket packet) {
        switch (packet.getType()) {
            case DATA:
                handleData(ctx, packet.getSessionId(), packet.getData());
                break;
            case CLOSE:
                handleClose(packet.getSessionId());
                break;
        }
    }

    private void handleData(ChannelHandlerContext ctx, String sessionId, byte[] data) {
        Channel backendChannel = sessionToBackend.get(sessionId);
        if (backendChannel == null) {
            Bootstrap bs = new Bootstrap();
            bs.group(ctx.channel().eventLoop())
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new BackendHandler(sessionId, coreProxy, sessionToBackend, backendToSession));
                        }
                    });

            ChannelFuture cf = bs.connect(backendHost, backendPort);
            cf.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    sessionToBackend.put(sessionId, future.channel());
                    backendToSession.put(future.channel(), sessionId);
                    future.channel().writeAndFlush(Unpooled.wrappedBuffer(data));
                } else {
                    sendCloseToEdge(sessionId);
                }
            });
        } else if (backendChannel.isActive()) {
            backendChannel.writeAndFlush(Unpooled.wrappedBuffer(data));
        }
    }

    protected void handleClose(String sessionId) {
        Channel backendChannel = sessionToBackend.remove(sessionId);
        if (backendChannel != null) {
            backendToSession.remove(backendChannel);
            backendChannel.close();
        }
    }

    private void sendCloseToEdge(String sessionId) {
        coreProxy.sendToEdge(new ProxyPacket(sessionId, ProxyPacket.Type.CLOSE, null));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        sessionToBackend.forEach((id, channel) -> {
            // 与Core断开连接，关闭所有通道
            if (channel != null && channel.isActive()) {
                channel.close();
            }
        });
        sessionToBackend.clear();
        backendToSession.clear();

        cause.printStackTrace();

        // 关闭并退出线程
        coreProxy.closeConnectToEdgeAndShutdown();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        sessionToBackend.forEach((id, channel) -> {
            // 与Core断开连接，关闭所有通道
            if (channel != null && channel.isActive()) {
                channel.close();
            }
        });
        sessionToBackend.clear();
        backendToSession.clear();

        // 关闭并退出线程
        coreProxy.closeConnectToEdgeAndShutdown();
    }
}
