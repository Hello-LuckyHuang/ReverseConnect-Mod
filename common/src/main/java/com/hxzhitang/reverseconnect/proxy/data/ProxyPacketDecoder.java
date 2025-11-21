package com.hxzhitang.reverseconnect.proxy.data;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 解码字节流为 ProxyPacket，配合 LengthFieldBasedFrameDecoder 使用
 */
//public class ProxyPacketDecoder extends ByteToMessageDecoder {
//
//    @Override
//    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
//        // LengthFieldBasedFrameDecoder 已经切好帧
//        // 这里直接反序列化即可
//        if (in.readableBytes() == 0) return;
//
//        byte[] bytes = new byte[in.readableBytes()];
//        in.readBytes(bytes);
//
//        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
//             ObjectInputStream ois = new ObjectInputStream(bis)) {
//            Object obj = ois.readObject();
//            if (obj instanceof ProxyPacket) {
//                out.add((ProxyPacket) obj);
//            }
//        } catch (ClassNotFoundException e) {
//            ctx.fireExceptionCaught(new IOException("Unknown class during deserialization", e));
//        }
//    }
//}

public class ProxyPacketDecoder extends ByteToMessageDecoder {
    private static final int SESSION_ID_LENGTH = 16;
    private static final int HEADER_SIZE = SESSION_ID_LENGTH + 1 + 4; // sessionId + type + dataLen

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 等待至少 HEADER_SIZE 字节
        if (in.readableBytes() < HEADER_SIZE) return;

        // 标记开始位置
        in.markReaderIndex();

        // 读取 sessionId
        byte[] idBytes = new byte[SESSION_ID_LENGTH];
        in.readBytes(idBytes);
        String sessionId = new String(idBytes, StandardCharsets.UTF_8).trim();

        // 读取 type
        int typeOrdinal = in.readUnsignedByte();
        ProxyPacket.Type type = ProxyPacket.Type.values()[typeOrdinal];

        // 读取 data 长度
        int dataLength = in.readInt();
        if (dataLength < 0 || dataLength > 1024 * 1024) { // 防止 OOM
            ctx.close();
            return;
        }

        // 检查是否有足够的 body 数据
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex(); // 数据不足，重置
            return;
        }

        // 读取 data
        byte[] data = new byte[dataLength];
        in.readBytes(data);

        // 构造对象并输出
        ProxyPacket packet = new ProxyPacket(sessionId, type, data);
        out.add(packet);
    }
}