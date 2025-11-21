package com.hxzhitang.reverseconnect.proxy.tools;

@FunctionalInterface
public interface StatusEvent {
    enum Status {
        START, //  连接开始
        COMPLETED,  //  连接完成
        CLOSE, // 连接断开
        INFO,  // 消息
        ERROR  // 错误
    }
    void updateEvent(String info, Status status);
}
