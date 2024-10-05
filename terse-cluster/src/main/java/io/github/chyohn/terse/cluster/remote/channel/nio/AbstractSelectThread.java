package io.github.chyohn.terse.cluster.remote.channel.nio;

import io.github.chyohn.terse.cluster.utils.ClusterThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * AbstractSelectThread is an abstract base class containing a few bits
 * of code shared by the AcceptThread (which selects on the listen socket)
 * and SelectorThread (which selects on client connections) classes.
 */
public abstract class AbstractSelectThread extends ClusterThread {

    protected static Logger LOG = LoggerFactory.getLogger(AbstractSelectThread.class);

    protected final Selector selector;
    private volatile boolean closed = false;
    public AbstractSelectThread(String name) throws IOException {
        super(name);
        // Allows the JVM to shutdown even if this thread is still running.
        setDaemon(true);
        this.selector = Selector.open();
    }

    public void close() {
        closed = true;
        if (isAlive()) {
            wakeupSelector();
        } else {
            closeSelector();
        }
    }

    protected boolean isStopped() {
        return closed;
    }

    public void wakeupSelector() {
        selector.wakeup();
    }

    /**
     * Close the selector. This should be called when the thread is about to exit and no operation is going to be
     * performed on the Selector or SelectionKey
     */
    protected void closeSelector() {
        try {
            selector.close();
        } catch (IOException e) {
            LOG.warn("ignored exception during selector close.", e);
        }
    }

    protected void fastCloseSock(SocketChannel sc) {
        if (sc != null) {
            try {
                // Hard close immediately, discarding buffers
                sc.socket().setSoLinger(true, 0);
            } catch (SocketException e) {
                LOG.warn("Unable to set socket linger to 0, socket close may stall in CLOSE_WAIT", e);
            }
        }
    }
}
