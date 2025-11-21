package com.hxzhitang.reverseconnect.proxy.tools;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.concurrent.TimeUnit;

public class AsyncServerConnectionChecker {

    // 连接检查回调接口
    public interface ConnectionCheckCallback {
        void onConnectionResult(String host, int port, boolean isSuccess, Throwable cause);
    }

    /**
     * 异步检查服务器连接
     * @param host 目标主机
     * @param port 目标端口
     * @param callback 连接检查回调
     * @param timeoutMillis 连接超时时间(毫秒)
     */
    public static void checkServerConnectionAsync(String host, int port,
                                                  ConnectionCheckCallback callback,
                                                  int timeoutMillis) {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMillis)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS));
                            // 可以添加其他简单的入站处理器
                        }
                    });

            // 异步连接
            ChannelFuture future = bootstrap.connect(host, port);

            // 添加监听器处理连接结果
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        // 连接成功
                        if (callback != null) {
                            callback.onConnectionResult(host, port, true, null);
                        }
                        // 立即关闭连接
                        if (future.channel().isOpen()) {
                            future.channel().close();
                        }
                    } else {
                        // 连接失败
                        if (callback != null) {
                            callback.onConnectionResult(host, port, false, future.cause());
                        }
                    }
                    // 关闭EventLoopGroup
                    group.shutdownGracefully();
                }
            });

        } catch (Exception e) {
            // 发生异常时调用回调
            if (callback != null) {
                callback.onConnectionResult(host, port, false, e);
            }
            group.shutdownGracefully();
        }
    }

    /**
     * 重载方法，使用默认超时时间(5000毫秒)
     */
    public static void checkServerConnectionAsync(String host, int port, ConnectionCheckCallback callback) {
        checkServerConnectionAsync(host, port, callback, 5000);
    }
}
