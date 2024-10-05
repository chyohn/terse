package io.github.chyohn.terse.cluster.remote.server;

import io.github.chyohn.terse.cluster.Cluster;
import io.github.chyohn.terse.cluster.remote.channel.ChannelHandler;
import io.github.chyohn.terse.cluster.remote.channel.Server;
import io.github.chyohn.terse.cluster.remote.channel.nio.server.NIOServer;
import java.io.IOException;
import java.net.InetSocketAddress;

public class NioServerFactory extends AbstractServerFactory {

    @Override
    protected Server doCreate(Cluster cluster, InetSocketAddress address,
                              ChannelHandler handler) {
        try {
            return new NIOServer(address, handler);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
