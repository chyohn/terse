package io.github.chyohn.terse.cluster.remote.channel.nio.server;

import io.github.chyohn.terse.cluster.remote.channel.ChannelHandler;
import io.github.chyohn.terse.cluster.remote.channel.nio.AbstractSelectThread;
import io.github.chyohn.terse.cluster.remote.channel.nio.SelectorThread;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * There is a single AcceptThread which accepts new connections and assigns
 * them to a SelectorThread using a simple round-robin scheme to spread
 * them across the SelectorThreads. It enforces maximum number of
 * connections per IP and attempts to cope with running out of file
 * descriptors by briefly sleeping before retrying.
 */
@Slf4j
public class AcceptThread extends AbstractSelectThread {
    private final SelectionKey acceptKey;
    private final ServerSocketChannel ssc;
    private final Collection<SelectorThread> selectorThreads;
    private Iterator<SelectorThread> selectorIterator;
    private final ChannelHandler channelHandler;

    public AcceptThread(String name, ServerSocketChannel ssc, Collection<SelectorThread> selectorThreads, ChannelHandler channelHandler) throws IOException {
        super(name);
        this.ssc = ssc;
        this.acceptKey = ssc.register(selector, SelectionKey.OP_ACCEPT);
        this.selectorThreads = Collections.unmodifiableList(new ArrayList<>(selectorThreads));
        this.channelHandler = channelHandler;
        selectorIterator = this.selectorThreads.iterator();
    }

    public void run() {
        try {
            while (!isStopped() && !ssc.socket().isClosed()) {
                try {
                    select();
                } catch (RuntimeException e) {
                    LOG.warn("Ignoring unexpected runtime exception", e);
                } catch (Exception e) {
                    LOG.warn("Ignoring unexpected exception", e);
                }
            }
        } finally {
            closeSelector();
            LOG.info("accept thread exited run method");
        }
    }

    private void select() {
        try {
            selector.select();

            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
            while (!isStopped() && selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                selectedKeys.remove();

                if (!key.isValid()) {
                    continue;
                }
                if (key.isAcceptable()) {
                    if (!doAccept()) {
                        // If unable to pull a new connection off the accept
                        // queue, pause accepting to give us time to free
                        // up file descriptors and so the accept thread
                        // doesn't spin in a tight loop.
                        pauseAccept(10);
                    }
                } else {
                    LOG.warn("Unexpected ops in accept select {}", key.readyOps());
                }
            }
        } catch (IOException e) {
            LOG.warn("Ignoring IOException while selecting", e);
        }
    }

    /**
     * Mask off the listen socket interest ops and use select() to sleep
     * so that other threads can wake us up by calling wakeup() on the
     * selector.
     */
    private void pauseAccept(long millisecs) {
        acceptKey.interestOps(0);
        try {
            selector.select(millisecs);
        } catch (IOException e) {
            // ignore
        } finally {
            acceptKey.interestOps(SelectionKey.OP_ACCEPT);
        }
    }

    /**
     * Accept new socket connections. Round-robin assigns to selector thread for
     * handling. Returns whether pulled a connection off the accept queue
     * or not. If encounters an error attempts to fast close the socket.
     *
     * @return whether was able to accept a connection or not
     */
    private boolean doAccept() {
        boolean accepted = false;
        SocketChannel sc = null;
        try {
            sc = ssc.accept();
            accepted = true;

            LOG.debug("Accepted socket connection from {}", sc.socket().getRemoteSocketAddress());
            sc.configureBlocking(false);
            // Round-robin assign this connection to a selector thread
            if (!selectorIterator.hasNext()) {
                selectorIterator = selectorThreads.iterator();
            }
            SelectorThread selectorThread = selectorIterator.next();
            if (!selectorThread.addAcceptedConnection(sc, channelHandler)) {
                throw new IOException("Unable to add connection to selector queue"
                    + (isStopped() ? " (shutdown in progress)" : ""));
            }
        } catch (Exception e) {
            log.warn("accept connection error", e);
            fastCloseSock(sc);
        }
        return accepted;
    }
}
