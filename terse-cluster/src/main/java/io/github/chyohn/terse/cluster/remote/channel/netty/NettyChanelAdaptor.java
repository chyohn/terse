package io.github.chyohn.terse.cluster.remote.channel.netty;

import io.github.chyohn.terse.cluster.remote.channel.ChannelHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class NettyChanelAdaptor implements io.github.chyohn.terse.cluster.remote.channel.Channel {

    private static final ConcurrentMap<Channel, NettyChanelAdaptor> CHANNEL_MAP = new ConcurrentHashMap<>();
    static NettyChanelAdaptor getOrAddChannel(Channel ch, ChannelHandler handler) {
        if (ch == null) {
            return null;
        }
        NettyChanelAdaptor ret = CHANNEL_MAP.get(ch);
        if (ret == null) {
            NettyChanelAdaptor nettyChannel = new NettyChanelAdaptor(ch, handler);
            if (ch.isActive()) {
                ret = CHANNEL_MAP.putIfAbsent(ch, nettyChannel);
            }
            if (ret == null) {
                ret = nettyChannel;
            }
        }
        return ret;
    }

    static NettyChanelAdaptor removeChannel(Channel ch) {
        if (ch != null) {
            NettyChanelAdaptor nettyChannel = CHANNEL_MAP.remove(ch);
            if (nettyChannel != null) {
                nettyChannel.close();
            }
            return nettyChannel;
        }
        return null;
    }

    private final Channel nettyChannel;
    private final ChannelHandler handler;
    private volatile boolean closed = false;
    private final NettyBatchWriteQueue writeQueue;

    public NettyChanelAdaptor(Channel nettyChannel, ChannelHandler handler) {
        this.nettyChannel = nettyChannel;
        this.handler = handler;
        this.writeQueue = NettyBatchWriteQueue.createWriteQueue(nettyChannel);

    }

    @Override
    public void send(Object message) {
        ChannelFuture future = writeQueue.enqueue(message).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                ChannelHandler handler = getChannelHandler();
                if (!future.isSuccess()) {
                    Throwable t = future.cause();
                    if (t == null) {
                        return;
                    }
                    handler.caught(NettyChanelAdaptor.this, t);
                }
            }
        });
    }

    @Override
    public void close() {
        closed = true;
        nettyChannel.close();
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean isConnected() {
        return !isClosed() && nettyChannel.isOpen() && nettyChannel.isActive();
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return handler;
    }
}
