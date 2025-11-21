package com.hxzhitang.reverseconnect.proxy.edgeproxy;

import com.hxzhitang.reverseconnect.Constants;
import com.hxzhitang.reverseconnect.proxy.data.ProxyPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class UserChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final Supplier<Channel> coreChannelSupplier;
    private final Map<String, Channel> sessionToUser;
    private final Map<Channel, String> channelToId;

    public UserChannelHandler(Supplier<Channel> coreChannelSupplier, Map<String, Channel> sessionToUser, Map<Channel, String> channelToId) {
        this.coreChannelSupplier = coreChannelSupplier;
        this.sessionToUser = sessionToUser;
        this.channelToId = channelToId;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String sessionId = "sess-" + UUID.randomUUID().toString().substring(0, 8);
        boolean isSuccess = register(sessionId, ctx.channel());
        if (isSuccess) {
            Constants.LOG.info("User connected: {} | ID: {}", ctx.channel().remoteAddress(), sessionId);
        } else {
            ctx.close();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        byte[] data = new byte[msg.readableBytes()];
        msg.readBytes(data);

        Channel coreChannel = coreChannelSupplier.get();
        String sessionId = channelToId.get(ctx.channel());
        if (sessionId != null) { // 如果此通道存在
            if (coreChannel != null && coreChannel.isActive()) {
                ProxyPacket packet = new ProxyPacket(sessionId, ProxyPacket.Type.DATA, data);
                coreChannel.writeAndFlush(packet);
            } else {
                System.err.println("? Core channel not available. Dropping data for " + sessionId);
                // 可选：缓存数据、发送错误、或关闭连接
                // ctx.writeAndFlush(Unpooled.copiedBuffer("Service unavailable", CharsetUtil.UTF_8));
                ctx.close();
            }
        } else {
            // 直接让用户重新连接吧
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel coreChannel = coreChannelSupplier.get();
        String sessionId = channelToId.get(ctx.channel());
        if (sessionId != null) {
            coreChannel.writeAndFlush(new ProxyPacket(sessionId, ProxyPacket.Type.CLOSE, null));
            channelToId.remove(ctx.channel());
            sessionToUser.remove(sessionId);
        }

        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel coreChannel = coreChannelSupplier.get();
        String sessionId = channelToId.get(ctx.channel());
        if (sessionId != null) {
            coreChannel.writeAndFlush(new ProxyPacket(sessionId, ProxyPacket.Type.CLOSE, null));
            channelToId.remove(ctx.channel());
            sessionToUser.remove(sessionId);
        }
    }

    // 外部注册用
    private boolean register(String sessionId, Channel channel) {
        Channel coreChannel = coreChannelSupplier.get();
        if (coreChannel != null && coreChannel.isActive()) {
            sessionToUser.put(sessionId, channel);
            channelToId.put(channel, sessionId);
            return true;
        }

        Constants.LOG.info("Core channel not available. Cannot connect in.");
        return false;
    }
}
