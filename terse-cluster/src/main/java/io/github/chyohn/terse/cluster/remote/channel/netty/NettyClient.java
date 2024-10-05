package io.github.chyohn.terse.cluster.remote.channel.netty;

import io.github.chyohn.terse.cluster.remote.channel.Channel;
import io.github.chyohn.terse.cluster.remote.channel.ChannelHandler;
import io.github.chyohn.terse.cluster.remote.channel.Client;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class NettyClient implements Client, ChannelHandler {

    private final InetSocketAddress address;
    private final ChannelHandler channelHandler;
    private final Bootstrap bootstrap;

    private volatile boolean closed = false;
    private Channel channel;

    public NettyClient(InetSocketAddress address, ChannelHandler channelHandler) {
        this.address = address;
        this.channelHandler = channelHandler;
        bootstrap = new Bootstrap();
    }

    @Override
    public void start() {
        bootstrap.group(NettyEventLoopFactory.getGlobalClientEventLoopGroup())
                .channel(NettyEventLoopFactory.socketChannelClass())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast("encoder", CodecAdaptor.INST.newEncoder())
                                .addLast("decoder", CodecAdaptor.INST.newDecoder())
                                .addLast("handler", new NettyDuplexHandler(NettyClient.this));
                    }
                });


        ChannelFuture future = bootstrap.connect(address);
        future.addListener(f -> {
            if (future.cause() != null) {
                Throwable cause = future.cause();
                log.error( "network disconnected Failed to connect to provider server by other reason.", cause);
                channelHandler.disconnected(NettyClient.this);
                return;
            }
            Channel newChannel = NettyChanelAdaptor.getOrAddChannel(future.channel(), channelHandler);
            Channel oldChanel = this.channel;
            if (oldChanel != null && !oldChanel.equals(newChannel)) {
                oldChanel.close();
            }
            this.channel = newChannel;
        });
    }

    @Override
    public void send(Object message) {
        channel.send(message);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        if (channel != null && !channel.isClosed()) {
            channel.close();
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean isConnected() {
        return !isClosed() && channel != null && channel.isConnected();
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return channelHandler;
    }

    @Override
    public void connected(Channel channel) {
        this.channel = channel;
        channelHandler.connected(this);
    }

    @Override
    public void disconnected(Channel channel) {
        close();
        this.channel = null;
        channelHandler.disconnected(this);
    }

    @Override
    public void received(Channel channel, Object message) {
        channelHandler.received(this, message);
    }

    @Override
    public void caught(Channel channel, Throwable throwable) {
        channelHandler.caught(this, throwable);
    }
}
