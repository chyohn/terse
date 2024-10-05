package io.github.chyohn.terse.cluster.remote.channel.netty;

import io.github.chyohn.terse.cluster.remote.channel.ChannelHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class NettyDuplexHandler extends ChannelDuplexHandler {

    private final ChannelHandler handler;

    public NettyDuplexHandler(ChannelHandler handler) {
        this.handler = handler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyChanelAdaptor chanelAdaptor = NettyChanelAdaptor.getOrAddChannel(ctx.channel(), handler);
        handler.connected(chanelAdaptor);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyChanelAdaptor chanelAdaptor = NettyChanelAdaptor.removeChannel(ctx.channel());
        if (chanelAdaptor != null) {
            chanelAdaptor.close();
            handler.disconnected(chanelAdaptor);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyChanelAdaptor chanelAdaptor = NettyChanelAdaptor.getOrAddChannel(ctx.channel(), handler);
        handler.received(chanelAdaptor, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            log.warn("handle warn: {}", cause.getMessage());
        } else {
            log.error("handle error", cause);
        }
        NettyChanelAdaptor chanelAdaptor = NettyChanelAdaptor.getOrAddChannel(ctx.channel(), handler);
        handler.caught(chanelAdaptor, cause);
    }
}


