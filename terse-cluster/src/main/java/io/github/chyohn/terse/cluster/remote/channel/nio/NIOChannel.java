package io.github.chyohn.terse.cluster.remote.channel.nio;

import io.github.chyohn.terse.cluster.remote.channel.Channel;
import io.github.chyohn.terse.cluster.remote.channel.ChannelHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class NIOChannel implements Channel {

    private final AtomicBoolean selectable = new AtomicBoolean(true);
    private final AtomicBoolean throttled = new AtomicBoolean(false);
    private final Queue<ByteBuffer> outgoingBuffers = new LinkedBlockingQueue<>();
    private final ByteBuffer lenBuffer = ByteBuffer.allocate(4);

    protected ByteBuffer incomingBuffer = lenBuffer;
    private volatile boolean closed = false;

    private final SocketChannel sock;
    private final SelectionKey sk;
    private final ChannelHandler channelHandler;

    public NIOChannel(SocketChannel sock, SelectionKey sk, ChannelHandler channelHandler) {
        this.sock = sock;
        this.sk = sk;
        this.channelHandler = channelHandler;
    }

    @Override
    public void send(Object message) {
        if (message instanceof byte[]) {
            byte[] data = (byte[]) message;
            ByteBuffer len = ByteBuffer.allocate(4);
            len.putInt(data.length);
            len.flip();
            sendBuffer(len, ByteBuffer.wrap(data));
            return;
        } else if (message instanceof ByteBuffer) {
            ByteBuffer buffer = (ByteBuffer) message;
            ByteBuffer len = ByteBuffer.allocate(4);
            len.putInt(buffer.capacity());
            len.flip();
            sendBuffer(len, buffer);
            return;
        }
        throw new IllegalArgumentException("can't support message class type: " + message.getClass());
    }

    private void sendBuffer(ByteBuffer... buffers) {
        if (log.isDebugEnabled()) {
            log.debug("Add a buffer to outgoingBuffers, sk {} is valid: {}", sk, sk.isValid());
        }
        synchronized (outgoingBuffers) {
            outgoingBuffers.addAll(Arrays.asList(buffers));
        }
        requestInterestOpsUpdate();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        if (sk != null) {
            try {
                // need to cancel this selection key from the selector
                sk.cancel();
            } catch (Exception e) {
                log.debug("ignoring exception during selectionkey cancel", e);
            }
        }

        closeSock(sock);
        channelHandler.disconnected(this);
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean isConnected() {
        return sock.isConnected();
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return channelHandler;
    }

    public void doIO() throws IOException {
        if (!isSocketOpen()) {
            log.warn("trying to do i/o on a null socket ");
            return;
        }
        if (sk.isReadable()) {
            int rc = sock.read(incomingBuffer);
            if (rc < 0) {
                handleFailedRead();
            }
            if (!incomingBuffer.hasRemaining()) {
                if (incomingBuffer == lenBuffer) { // start of next request
                    incomingBuffer.flip();
                    readLength();
                }
                readPayload();
            }
        }
        if (sk.isWritable()) {
            handleWrite();
            if (!getReadInterest() && !getWriteInterest()) {
                throw new RuntimeException("responded to info probe");
            }
        }
    }

    private void handleFailedRead() {
        throw new RuntimeException("Unable to read additional data from client,"
                + " it probably closed the socket:"
                + " address = " + sock.socket().getRemoteSocketAddress());
    }

    private void readLength() throws IOException {
        // Read the length, now get the buffer
        int len = lenBuffer.getInt();
        if (len < 0) {
            throw new IOException("Len error. "
                    + "A message from " + this.getRemoteSocketAddress() + " with advertised length of " + len);
        }
        incomingBuffer = ByteBuffer.allocate(len);
    }

    private void readPayload() throws IOException {
        if (incomingBuffer.hasRemaining()) { // have we read length bytes?
            int rc = sock.read(incomingBuffer); // sock is non-blocking, so ok
            if (rc < 0) {
                handleFailedRead();
            }
        }

        if (!incomingBuffer.hasRemaining()) { // have we read length bytes?
            incomingBuffer.flip();
            channelHandler.received(this, incomingBuffer);
            lenBuffer.clear();
            incomingBuffer = lenBuffer;
        }
    }

    private void handleWrite() throws IOException {
        if (outgoingBuffers.isEmpty()) {
            return;
        }

        ByteBuffer[] bufferList = new ByteBuffer[outgoingBuffers.size()];
        // Use gathered write call. This updates the positions of the
        // byte buffers to reflect the bytes that were written out.
        sock.write(outgoingBuffers.toArray(bufferList));

        // Remove the buffers that we have sent
        ByteBuffer bb;
        while ((bb = outgoingBuffers.peek()) != null) {
            if (bb.remaining() > 0) {
                break;
            }
            outgoingBuffers.remove();
        }
    }

    public InetSocketAddress getRemoteSocketAddress() {
        if (!sock.isOpen()) {
            return null;
        }
        return (InetSocketAddress) sock.socket().getRemoteSocketAddress();
    }

    public InetAddress getSocketAddress() {
        if (!sock.isOpen()) {
            return null;
        }
        return sock.socket().getInetAddress();
    }

    private boolean isSocketOpen() {
        return sock.isOpen();
    }

    public int getInterestOps() {
        if (!isSelectable()) {
            return 0;
        }
        int interestOps = 0;
        if (getReadInterest()) {
            interestOps |= SelectionKey.OP_READ;
        }
        if (getWriteInterest()) {
            interestOps |= SelectionKey.OP_WRITE;
        }
        return interestOps;
    }

    private void requestInterestOpsUpdate() {
        if (isSelectable()) {
//            selectorThread.addInterestOpsUpdateRequest(sk);
            sk.interestOps(getInterestOps());
            sk.selector().wakeup();
        }
    }

    private boolean getReadInterest() {
        return !throttled.get();
    }


    // Throttle acceptance of new requests. If this entailed a state change,
    // register an interest op update request with the selector.
    //
    // Don't support wait disable receive in NIO, ignore the parameter
    public void disableRecv(boolean waitDisableRecv) {
        if (throttled.compareAndSet(false, true)) {
            requestInterestOpsUpdate();
        }
    }

    // Disable throttling and resume acceptance of new requests. If this
    // entailed a state change, register an interest op update request with
    // the selector.
    public void enableRecv() {
        if (throttled.compareAndSet(true, false)) {
            requestInterestOpsUpdate();
        }
    }

    // returns whether we are interested in writing, which is determined
    // by whether we have any pending buffers on the output queue or not
    private boolean getWriteInterest() {
        return !outgoingBuffers.isEmpty();
    }

    public boolean isSelectable() {
        return sk.isValid() && selectable.get();
    }

    public void disableSelectable() {
        selectable.set(false);
        sk.interestOps(0);
    }

    public void enableSelectable() {
        selectable.set(true);
        requestInterestOpsUpdate();
    }

    public static void closeSock(SocketChannel sock) {
        if (!sock.isOpen()) {
            return;
        }

        try {
            /*
             * The following sequence of code is stupid! You would think that
             * only sock.close() is needed, but alas, it doesn't work that way.
             * If you just do sock.close() there are cases where the socket
             * doesn't actually close...
             */
            sock.socket().shutdownOutput();
        } catch (IOException e) {
            // This is a relatively common exception that we can't avoid
            log.debug("ignoring exception during output shutdown", e);
        }
        try {
            sock.socket().shutdownInput();
        } catch (IOException e) {
            // This is a relatively common exception that we can't avoid
            log.debug("ignoring exception during input shutdown", e);
        }
        try {
            sock.socket().close();
        } catch (IOException e) {
            log.debug("ignoring exception during socket close", e);
        }
        try {
            sock.close();
        } catch (IOException e) {
            log.debug("ignoring exception during socketchannel close", e);
        }
    }
}
