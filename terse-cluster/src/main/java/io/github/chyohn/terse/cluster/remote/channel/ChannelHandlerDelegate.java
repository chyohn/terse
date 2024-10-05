package io.github.chyohn.terse.cluster.remote.channel;

public class ChannelHandlerDelegate implements ChannelHandler{

    protected final ChannelHandler delgate;

    public ChannelHandlerDelegate(ChannelHandler handler) {
        this.delgate = handler;
    }

    @Override
    public void connected(Channel channel) {
        delgate.connected(channel);
    }

    @Override
    public void disconnected(Channel channel) {
        delgate.disconnected(channel);
    }

    @Override
    public void received(Channel channel, Object message) {
        delgate.received(channel, message);
    }

    @Override
    public void caught(Channel channel, Throwable throwable) {
        delgate.caught(channel, throwable);
    }
}
