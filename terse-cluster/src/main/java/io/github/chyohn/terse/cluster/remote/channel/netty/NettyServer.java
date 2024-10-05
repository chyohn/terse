package io.github.chyohn.terse.cluster.remote.channel.netty;

import io.github.chyohn.terse.cluster.remote.channel.Channel;
import io.github.chyohn.terse.cluster.remote.channel.ChannelHandler;
import io.github.chyohn.terse.cluster.remote.channel.Server;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
public class NettyServer implements Server, ChannelHandler {

    private ServerBootstrap bootstrap;

    private EventLoopGroup boss;
    private EventLoopGroup worker;
    private Channel channel;
    private volatile boolean closed;
    private final Map<Channel, Long> channelSet = new ConcurrentHashMap<>();

    private final ChannelHandler channelHandler;

    private final InetSocketAddress bindAddress;

    public NettyServer(InetSocketAddress bindAddress, ChannelHandler channelHandler) {
        this.channelHandler = channelHandler;
        this.bindAddress = bindAddress;
    }

    @Override
    public void start() {

        boss = createBossGroup();
        worker = createWorkerGroup();

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(boss, worker)
                .channel(NettyEventLoopFactory.serverSocketChannelClass())
                ;

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast("encoder", CodecAdaptor.INST.newEncoder())
                        .addLast("decoder", CodecAdaptor.INST.newDecoder())
                        .addLast("handler", new NettyDuplexHandler(NettyServer.this))
                ;
            }
        });
        bootstrap.option(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        ChannelFuture bindFuture = bootstrap.bind(bindAddress);
//        bindFuture.addListener(future -> {
//            Object o = future.get();
//            System.out.println(o);
//        });

        bindFuture.syncUninterruptibly();
        channel = NettyChanelAdaptor.getOrAddChannel(bindFuture.channel(), this);
    }

    @Override
    public void send(Object message) {
        for (Channel channel : channelSet.keySet()) {
            channel.send(message);
        }
    }

    public void close() {

        if (closed) {
            return;
        }
        closed = true;

        if (channel != null && !channel.isClosed()) {
            channel.close();
        }

        try {
            if (bootstrap != null) {
                long timeout = 2000L;
                long quietPeriod = Math.min(2000L, timeout);
                Future<?> bossGroupShutdownFuture = boss.shutdownGracefully(quietPeriod, timeout, MILLISECONDS);
                Future<?> workerGroupShutdownFuture =  worker.shutdownGracefully(quietPeriod, timeout, MILLISECONDS);
                bossGroupShutdownFuture.syncUninterruptibly();
                workerGroupShutdownFuture.syncUninterruptibly();
            }
        } catch (Throwable e) {
            log.warn("close error", e);
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean isConnected() {
        return !isClosed();
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return channelHandler;
    }

    @Override
    public void connected(Channel channel) {
        channelSet.remove(channel);
        channelHandler.connected(channel);
    }

    @Override
    public void disconnected(Channel channel) {
        channelSet.remove(channel);
        channelHandler.disconnected(channel);
    }

    @Override
    public void received(Channel channel, Object message) {
        channelHandler.received(channel, message);
    }

    @Override
    public void caught(Channel channel, Throwable throwable) {
        channelHandler.caught(channel, throwable);
    }

    protected EventLoopGroup createBossGroup() {
        return NettyEventLoopFactory.eventLoopGroup(1, "NettyServerBoss");
    }

    protected EventLoopGroup createWorkerGroup() {
        return NettyEventLoopFactory.eventLoopGroup(NettyEventLoopFactory.DEFAULT_IO_THREADS,
                "NettyServerWorker");
    }
}
