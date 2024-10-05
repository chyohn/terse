package io.github.chyohn.terse.cluster.remote.message;

import io.github.chyohn.terse.cluster.remote.channel.Channel;
import io.github.chyohn.terse.cluster.remote.channel.ChannelHandler;
import java.util.concurrent.CompletableFuture;

public interface MessageChannelHandler extends ChannelHandler {
    <R> CompletableFuture<R> send(Channel channel, Object msg);
}
