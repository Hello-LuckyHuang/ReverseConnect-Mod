package com.hxzhitang.reverseconnect.proxy.edgeproxy;

import com.hxzhitang.reverseconnect.Constants;
import com.hxzhitang.reverseconnect.proxy.data.ProxyPacket;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;

public class EdgeToCoreHandler extends SimpleChannelInboundHandler<ProxyPacket> {

    private final Map<String, Channel> sessionToUser;
    private final Map<Channel, String> channelToId;

    public EdgeToCoreHandler(Map<String, Channel> sessionToUser, Map<Channel, String> channelToId) {
        this.sessionToUser = sessionToUser;
        this.channelToId = channelToId;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        synchronized (EdgeProxyServer.class) {
            if (EdgeProxyServer.clientCount == 0) {
                EdgeProxyServer.coreChannel = ctx.channel();
                EdgeProxyServer.clientCount++;
            } else {
                ctx.close();
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyPacket packet) {
        String sid = packet.getSessionId();
        switch (packet.getType()) {
            case DATA:
                Channel userChannel = sessionToUser.get(sid);
                if (userChannel != null && userChannel.isActive()) {
                    userChannel.writeAndFlush(Unpooled.wrappedBuffer(packet.getData()));
                }
                break;
            case CLOSE:
                Channel ch = sessionToUser.remove(sid);
                if (ch != null) {
                    channelToId.remove(ch);
                    ch.close();
                }
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Constants.LOG.info("Core channel is offline");
        sessionToUser.forEach((id, channel) -> {
            // 与Core断开连接，关闭所有通道
            if (channel != null && channel.isActive()) {
                channel.close();
            }
        });
        sessionToUser.clear();
        channelToId.clear();

        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Constants.LOG.info("Core channel is offline");
        sessionToUser.forEach((id, channel) -> {
            // 与Core断开连接，关闭所有通道
            if (channel != null && channel.isActive()) {
                channel.close();
            }
        });
        sessionToUser.clear();
        channelToId.clear();

        synchronized (EdgeProxyServer.class) {
            EdgeProxyServer.coreChannel = null;
            EdgeProxyServer.clientCount = 0;
        }
    }
}
