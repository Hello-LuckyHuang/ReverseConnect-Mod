package com.hxzhitang.reverseconnect.proxy.data;

import java.io.Serializable;

public class ProxyPacket implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        DATA,
        CLOSE
    }

    private final String sessionId;
    private final Type type;
    private final byte[] data; // 可为 null

    public ProxyPacket(String sessionId, Type type, byte[] data) {
        this.sessionId = sessionId;
        this.type = type;
        this.data = data;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Type getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "ProxyPacket{" +
                "sessionId='" + sessionId + '\'' +
                ", type=" + type +
                ", dataLength=" + (data == null ? 0 : data.length) +
                '}';
    }
}
