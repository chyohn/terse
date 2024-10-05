package io.github.chyohn.terse.cluster.remote.channel;

public interface ChannelHandler {
    void connected(Channel channel);

    void disconnected(Channel channel);

    void received(Channel channel, Object message);

    void caught(Channel channel, Throwable throwable);
}
