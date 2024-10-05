package io.github.chyohn.terse.cluster.remote.server;

import io.github.chyohn.terse.cluster.Cluster;
import io.github.chyohn.terse.cluster.remote.channel.ChannelHandler;
import io.github.chyohn.terse.cluster.remote.channel.Server;
import io.github.chyohn.terse.cluster.remote.channel.netty.NettyServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class NettyServerFactory extends AbstractServerFactory {

    @Override
    protected Server doCreate(Cluster cluster, InetSocketAddress address,
                              ChannelHandler handler) {
        return new NettyServer(address, handler);
    }
}
