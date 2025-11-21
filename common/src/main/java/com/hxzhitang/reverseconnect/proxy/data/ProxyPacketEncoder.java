package com.hxzhitang.reverseconnect.proxy.data;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

/**
 * 将 ProxyPacket 序列化为字节流，格式：[length][serialized bytes]
 */
//public class ProxyPacketEncoder extends MessageToByteEncoder<ProxyPacket> {
//
//    @Override
//    protected void encode(ChannelHandlerContext ctx, ProxyPacket packet, ByteBuf out) throws Exception {
//        byte[] bytes = serialize(packet);
//        out.writeInt(bytes.length);  // 写入长度（4 字节）
//        out.writeBytes(bytes);       // 写入序列化数据
//    }
//
//    private byte[] serialize(ProxyPacket packet) throws IOException {
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        ObjectOutputStream oos = new ObjectOutputStream(bos);
//        oos.writeObject(packet);
//        oos.flush();
//        oos.close();
//        return bos.toByteArray();
//    }
//}

public class ProxyPacketEncoder extends MessageToByteEncoder<ProxyPacket> {
    @Override
    protected void encode(ChannelHandlerContext ctx, ProxyPacket msg, ByteBuf out) {
        // 先预留4字节长度位置（会被 LengthFieldPrepender 自动填充）
        // 写入 sessionId (假设固定16字节)
        byte[] sessionIdBytes = msg.getSessionId().getBytes(StandardCharsets.UTF_8);
        out.writeBytes(sessionIdBytes); // 实际中应补全或限制长度

        // 写入 type
        out.writeByte(msg.getType().ordinal());

        // 写入 data 长度（int）
        byte[] data = msg.getData();
        out.writeInt(data.length);

        // 写入 data
        if (data.length > 0) {
            out.writeBytes(data);
        }
    }
}