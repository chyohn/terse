package io.github.chyohn.terse.cluster.remote.server;

import io.github.chyohn.terse.cluster.Cluster;
import io.github.chyohn.terse.cluster.remote.channel.ChannelHandler;
import io.github.chyohn.terse.cluster.remote.channel.Server;

public interface ServerFactory {
    Server create(Cluster cluster);
}
