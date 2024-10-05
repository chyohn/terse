package io.github.chyohn.terse.cluster.remote.channel.nio.server;

import io.github.chyohn.terse.cluster.remote.channel.Channel;
import io.github.chyohn.terse.cluster.remote.channel.ChannelHandler;
import io.github.chyohn.terse.cluster.remote.channel.Server;
import io.github.chyohn.terse.cluster.remote.channel.nio.SelectorThread;
import io.github.chyohn.terse.cluster.remote.channel.nio.WorkerPool;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NIOServer implements Server, ChannelHandler {

    private boolean closed;
    private final ServerSocketChannel serverSocketChannel;
    private final AcceptThread acceptThread;
    private final Set<SelectorThread> selectorThreads = new HashSet<>();
    private final WorkerPool workerPool;
    private final ChannelHandler channelHandler;

    private final Map<Channel, Long> channelSet = new ConcurrentHashMap<>();

    public NIOServer(InetSocketAddress addr, ChannelHandler channelHandler) throws IOException {
        this.channelHandler = channelHandler;

        // worker thread
        int numCores = Runtime.getRuntime().availableProcessors();
        int numWorkerThreads = Integer.getInteger("nio.numWorkerThreads", 2 * numCores);
        workerPool = new WorkerPool("NIOWorker", numWorkerThreads);

        // selector thread
        // 32 cores sweet spot seems to be 4 selector threads
        int numSelectorThreads = Integer.getInteger("nio.numSelectorThreads",
                Math.max((int) Math.sqrt((float) numCores / 2), 1));
        if (numSelectorThreads < 1) {
            throw new RuntimeException("numSelectorThreads must be at least 1");
        }
        for (int i = 0; i < numSelectorThreads; ++i) {
            selectorThreads.add(new SelectorThread("SelectorThread-" +i, workerPool));
        }
        // start server socket
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().setReuseAddress(true);
        log.info("binding to port {}", addr);
        serverSocketChannel.socket().bind(addr);
        if (addr.getPort() == 0) {
            log.info("bound to port {}", serverSocketChannel.getLocalAddress());
        }
        serverSocketChannel.configureBlocking(false);
        acceptThread = new AcceptThread("AcceptThread-" + serverSocketChannel.socket().getInetAddress(),  serverSocketChannel, selectorThreads, this);
    }

    public void start() {
        closed = false;
        if (workerPool != null) {
            workerPool.start();
        }
        for (SelectorThread thread : selectorThreads) {
            if (thread.getState() == Thread.State.NEW) {
                thread.start();
            }
        }
        // ensure thread is started once and only once
        if (acceptThread.getState() == Thread.State.NEW) {
            acceptThread.start();
        }
    }

    
    @Override
    public void send(Object message) {
        for (Channel channel : channelSet.keySet()) {
            channel.send(message);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        // Stop queuing connection attempts
        try {
            serverSocketChannel.close();
        } catch (IOException e) {
            log.warn("Error closing listen socket", e);
        }

        if (acceptThread != null) {
            acceptThread.close();
        }
        for (SelectorThread thread : selectorThreads) {
            thread.close();
        }
        if (workerPool != null) {
            workerPool.stop();
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return this;
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
}
