package io.github.chyohn.terse.cluster.remote.channel.nio.client;

import io.github.chyohn.terse.cluster.remote.channel.Channel;
import io.github.chyohn.terse.cluster.remote.channel.ChannelHandler;
import io.github.chyohn.terse.cluster.remote.channel.Client;
import io.github.chyohn.terse.cluster.remote.channel.nio.SelectorThread;
import io.github.chyohn.terse.cluster.remote.channel.nio.WorkerPool;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NIOClient implements Client, ChannelHandler {

    protected static WorkerPool workerPool;
    private static final SelectorThread selectorThread;

    static {
        int numCores = Runtime.getRuntime().availableProcessors();
        int numWorkerThreads = Integer.getInteger("nio.client.numWorkerThreads", 2 + numCores);
        workerPool = new WorkerPool("NIOClientWorker", numWorkerThreads);
        workerPool.start();
        try {
            selectorThread = new SelectorThread("NIO-Client-Selector", workerPool);
            selectorThread.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private final ChannelHandler channelHandler;
    private volatile boolean closed = true;
    private final InetSocketAddress address;

    private Channel channel;

    public NIOClient(InetSocketAddress address, ChannelHandler channelHandler) {
        this.channelHandler = channelHandler;

        this.address = address;
    }

    @Override
    public void start() {

        closed = false;

        CompletableFuture<Channel> future = selectorThread.addConnectingAddress(address, this);
        future.whenComplete((channel, throwable) -> {
            if (throwable != null) {
                log.error("connect error", throwable);
                disconnected(channel);
                return;
            }
            Channel oldChanel = this.channel;
            if (oldChanel != null && !oldChanel.equals(channel)) {
                oldChanel.close();
            }
            this.channel = channel;
        });
    }

    @Override
    public void send(Object message) {
        if (isClosed()) {
            throw new RuntimeException("Connection refused: " + address);
        }
        if (!isConnected()) {
            throw new RuntimeException("Connection is starting: " + address);
        }
        channel.send(message);
    }

    @Override
    public void close() {

        if (closed) {
            return;
        }

        closed = true;

        if (channel != null) {
            channel.close();
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isConnected();
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return this;
    }

    @Override
    public void connected(Channel channel) {
        this.channel = channel;
        channelHandler.connected(this);
    }

    @Override
    public void disconnected(Channel channel) {
        this.channel = null;
        close();
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
