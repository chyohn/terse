package io.github.chyohn.terse.cluster.remote.channel.nio;

import io.github.chyohn.terse.cluster.remote.channel.Channel;
import io.github.chyohn.terse.cluster.remote.channel.ChannelHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * The SelectorThread receives newly accepted connections from the AcceptThread and is responsible for selecting for I/O
 * readiness across the connections. This thread is the only thread that performs any non-threadsafe or potentially
 * blocking calls on the selector (registering new connections and reading/writing interest ops).
 * <p>
 * Assignment of a connection to a SelectorThread is permanent and only one SelectorThread will ever interact with the
 * connection. There are 1-N SelectorThreads, with connections evenly apportioned between the SelectorThreads.
 * <p>
 * If there is a worker thread pool, when a connection has I/O to perform the SelectorThread removes it from selection
 * by clearing its interest ops and schedules the I/O for processing by a worker thread. When the work is complete, the
 * connection is placed on the ready queue to have its interest ops restored and resume selection.
 * <p>
 * If there is no worker thread pool, the SelectorThread performs the I/O directly.
 */
@Slf4j
public class SelectorThread extends AbstractSelectThread {

    private final Queue<AcceptInitData> acceptedQueue;
    private final Queue<InetSocketAddress> connectingAddressQueue;
    private final WorkerPool workerPool;

    public SelectorThread(String name, WorkerPool workerPool) throws IOException {
        super(name);
        this.workerPool = workerPool;
        acceptedQueue = new LinkedBlockingQueue<>();
        connectingAddressQueue = new LinkedBlockingQueue<>();
    }


    /**
     * The main loop for the thread selects() on the connections and dispatches ready I/O work requests, then registers
     * all pending newly accepted connections and updates any interest ops on the queue.
     */
    public void run() {
        while (!isStopped()) {
            try {
                select();
                processAcceptedConnections();
                processConnecting();
            } catch (Throwable e) {
                LOG.warn("Ignoring unexpected exception", e);
            }
        }

        // Close connections still pending on the selector. Any others
        // with in-flight work, let drain out of the work queue.
        for (SelectionKey key : selector.keys()) {
            NIOChannel channel = getAttach(key);
            if (channel.isSelectable()) {
                channel.close();
            }
            cleanupSelectionKey(key);
        }

        AcceptInitData acceptInitData;
        while ((acceptInitData = acceptedQueue.poll()) != null) {
            fastCloseSock(acceptInitData.accepted);
        }

        InetSocketAddress id;
        ChannelInitData cid;
        while ((id = connectingAddressQueue.poll()) != null) {
            if ((cid = futureOfAddress.remove(id)) != null) {
                cid.future.cancel(true);
            }
        }

        for (CompletableFuture<Channel> value : futureOfChannel.values()) {
            value.cancel(true);
        }
        futureOfChannel.clear();

        closeSelector();
        LOG.info("selector thread exited run method");
    }

    private void select() {
        try {
            selector.select();

            Set<SelectionKey> selected = selector.selectedKeys();
            ArrayList<SelectionKey> selectedList = new ArrayList<>(selected);
            Collections.shuffle(selectedList);
            Iterator<SelectionKey> selectedKeys = selectedList.iterator();
            while (!isStopped() && selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                selected.remove(key);

                if (!key.isValid()) {
                    cleanupSelectionKey(key);
                    continue;
                }
                if (key.isConnectable()) {
                    onConnected(key);
                } else if (key.isReadable() || key.isWritable()) {
                    handleIO(key);
                } else {
                    LOG.warn("Unexpected ops in select {}", key.readyOps());
                }
            }
        } catch (IOException e) {
            LOG.warn("Ignoring IOException while selecting", e);
        }
    }


    /**
     * Schedule I/O for processing on the connection associated with the given SelectionKey. If a worker thread pool is
     * not being used, I/O is run directly by this thread.
     */
    private void handleIO(SelectionKey key) {
        IOWorkRequest workRequest = new IOWorkRequest(this, key);
        NIOChannel channel = getAttach(key);
//
//        // Stop selecting this key while processing on its
//        // connection
        channel.disableSelectable();
//        key.interestOps(0);
        workerPool.schedule(workRequest);
    }

    /**
     * Place new accepted connection onto a queue for adding. Do this so only the selector thread modifies what keys are
     * registered with the selector.
     */
    public boolean addAcceptedConnection(SocketChannel accepted, ChannelHandler handler) {
        AcceptInitData data = new AcceptInitData();
        data.accepted = accepted;
        data.handler = handler;
        if (isStopped() || !acceptedQueue.offer(data)) {
            return false;
        }
        wakeupSelector();
        return true;
    }

    static class AcceptInitData {
        SocketChannel accepted;
        ChannelHandler handler;
    }
    /**
     * Iterate over the queue of accepted connections that have been assigned to this thread but not yet placed on the
     * selector.
     */
    private void processAcceptedConnections() {
        AcceptInitData data;
        while (!isStopped() && (data = acceptedQueue.poll()) != null) {
            SelectionKey key = null;
            SocketChannel sc = data.accepted;
            ChannelHandler channelHandler = data.handler;;
            try {
                key = sc.register(selector, SelectionKey.OP_READ);
                NIOChannel channel = new NIOChannel(sc, key, channelHandler);
                key.attach(channel);
                channelHandler.connected(channel);
            } catch (IOException e) {
                // register, createConnection
                cleanupSelectionKey(key);
                fastCloseSock(sc);
            }
        }
    }

    private NIOChannel getAttach(SelectionKey key) {
        return (NIOChannel) key.attachment();
    }

    private final Map<InetSocketAddress, ChannelInitData> futureOfAddress = new ConcurrentHashMap<>();
    private final Map<Channel, CompletableFuture<Channel>> futureOfChannel = new ConcurrentHashMap<>();


    public CompletableFuture<Channel> addConnectingAddress(InetSocketAddress address, ChannelHandler handler) {
        CompletableFuture<Channel> future = new CompletableFuture<>();
        ChannelInitData data = new ChannelInitData();
        data.future = future;
        data.handler = handler;
        futureOfAddress.put(address, data);
        if (isStopped() || !connectingAddressQueue.offer(address)) {
            future.cancel(true);
            futureOfAddress.remove(address);
            return future;
        }
        wakeupSelector();
        return future;
    }
    static class ChannelInitData {
        CompletableFuture<Channel> future;
        ChannelHandler handler;
    }

    private void processConnecting() {
        InetSocketAddress address;
        while (!isStopped() && (address = connectingAddressQueue.poll()) != null) {
            connect(address);
        }
    }

    private void connect(InetSocketAddress addr) {
        SelectionKey key = null;
        SocketChannel sc = null;
        NIOChannel channel = null;
        ChannelInitData initData = futureOfAddress.remove(addr);
        CompletableFuture<Channel> future = initData.future;
        ChannelHandler channelHandler = initData.handler;
        try {
            sc = createSock();
            key = sc.register(selector, SelectionKey.OP_CONNECT);
            channel = new NIOChannel(sc, key, channelHandler);
            key.attach(channel);
            futureOfChannel.put(channel, future);
            boolean immediateConnect = sc.connect(addr);
            if (immediateConnect) {
                futureOfChannel.remove(channel);
                future.complete(channel);
                channelHandler.connected(channel);
            }
        } catch (IOException e) {
            log.error("Unable to open socket to {}", addr);
            cleanupSelectionKey(key);
            fastCloseSock(sc);
            future.completeExceptionally(e);
            if (channel != null) {
                futureOfChannel.remove(channel);
            }
        }
    }

    private SocketChannel createSock() throws IOException {
        SocketChannel sock;
        sock = SocketChannel.open();
        sock.configureBlocking(false);
        sock.socket().setSoLinger(false, -1);
        sock.socket().setTcpNoDelay(true);
        return sock;
    }

    private void onConnected(SelectionKey key) {
        SocketChannel sc = null;
        NIOChannel channel = getAttach(key);
        ChannelHandler channelHandler = channel.getChannelHandler();
        CompletableFuture<Channel> future = futureOfChannel.remove(channel);
        try {
            sc = ((SocketChannel) key.channel());
            if (sc.finishConnect()) {
                future.complete(channel);
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                channelHandler.connected(channel);
            } else {
                futureOfChannel.put(channel, future);
            }
        } catch (IOException e) {
            log.error("handle io error from {}", sc.socket().getRemoteSocketAddress(), e);
            cleanupSelectionKey(key);
            fastCloseSock(sc);
            future.completeExceptionally(e);
            channelHandler.disconnected(channel);
        }
    }

    private void cleanupSelectionKey(SelectionKey key) {
        if (key != null) {
            try {
                key.cancel();
            } catch (Exception ex) {
                LOG.debug("ignoring exception during selectionkey cancel", ex);
            }
        }
    }

    private class IOWorkRequest extends WorkerPool.WorkRequest {

        private final SelectorThread selectorThread;
        private final SelectionKey key;
        private final NIOChannel channel;

        IOWorkRequest(SelectorThread selectorThread, SelectionKey key) {
            this.selectorThread = selectorThread;
            this.key = key;
            this.channel = getAttach(key);
            ;
        }

        public void doWork() throws Exception {
            if (!key.isValid()) {
                selectorThread.cleanupSelectionKey(key);
                return;
            }

            if (key.isReadable() || key.isWritable()) {
                channel.doIO();

                // Check if we shutdown or doIO() closed this connection
                if (isStopped()) {
                    channel.close();
                    return;
                }
                if (!key.isValid()) {
                    selectorThread.cleanupSelectionKey(key);
                    return;
                }
            }

            // Mark this connection as once again ready for selection
            channel.enableSelectable();
//            // Push an update request on the queue to resume selecting
//            // on the current set of interest ops, which may have changed
//            // as a result of the I/O operations we just performed.
//            if (!selectorThread.addInterestOpsUpdateRequest(key)) {
//                connection.close();
//            }
        }

        public void cleanup() {
            channel.close();
        }

    }
}
