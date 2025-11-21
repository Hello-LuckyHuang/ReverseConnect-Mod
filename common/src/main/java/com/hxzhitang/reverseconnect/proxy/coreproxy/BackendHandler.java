package com.hxzhitang.reverseconnect.proxy.coreproxy;

import com.hxzhitang.reverseconnect.Constants;
import com.hxzhitang.reverseconnect.proxy.data.ProxyPacket;
import com.hxzhitang.reverseconnect.proxy.tools.AsyncServerConnectionChecker;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;

public class BackendHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final String sessionId;
    private final CoreProxyMain coreProxy;

    private final Map<String, Channel> sessionToBackend;
    private final Map<Channel, String> backendToSession;

    public BackendHandler(String sessionId, CoreProxyMain coreProxy, Map<String, Channel> sessionToBackend, Map<Channel, String> backendToSession) {
        this.sessionId = sessionId;
        this.coreProxy = coreProxy;
        this.sessionToBackend = sessionToBackend;
        this.backendToSession = backendToSession;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        byte[] data = new byte[msg.readableBytes()];
        msg.readBytes(data);

        ProxyPacket packet = new ProxyPacket(sessionId, ProxyPacket.Type.DATA, data);
        coreProxy.sendToEdge(packet);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        coreProxy.sendToEdge(new ProxyPacket(sessionId, ProxyPacket.Type.CLOSE, null));
        Channel backendChannel = sessionToBackend.remove(sessionId);
        if (backendChannel != null) {
            backendToSession.remove(backendChannel);
        }

        ctx.close();

        // 如果发现后端服务器无法连接，则断开与Edge的连接。
        if (sessionToBackend.isEmpty()) {
            AsyncServerConnectionChecker.checkServerConnectionAsync(
                    coreProxy.backendHost, coreProxy.backendPort,
                    (host, port, isSuccess, cause0) -> {
                        if (!isSuccess) {
                            Constants.LOG.info("Cannot connect to backed server. Close connect to Edge.");
                            coreProxy.closeConnectToEdgeAndShutdown();
                        }
                    });
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        coreProxy.sendToEdge(new ProxyPacket(sessionId, ProxyPacket.Type.CLOSE, null));
        Channel backendChannel = sessionToBackend.remove(sessionId);
        if (backendChannel != null) {
            backendToSession.remove(backendChannel);
        }

        // 如果发现后端服务器无法连接，则断开与Edge的连接。
        if (sessionToBackend.isEmpty()) {
            AsyncServerConnectionChecker.checkServerConnectionAsync(
                    coreProxy.backendHost, coreProxy.backendPort,
                    (host, port, isSuccess, cause) -> {
                        if (!isSuccess) {
                            Constants.LOG.info("Cannot connect to backed server. Close connect to Edge.");
                            coreProxy.closeConnectToEdgeAndShutdown();
                        }
            });
        }
    }
}
