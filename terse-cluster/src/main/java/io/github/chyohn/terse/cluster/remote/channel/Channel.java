package io.github.chyohn.terse.cluster.remote.channel;

public interface Channel {
    void send(Object message);

    void close();

    boolean isClosed();

    boolean isConnected();

    ChannelHandler getChannelHandler();
}
