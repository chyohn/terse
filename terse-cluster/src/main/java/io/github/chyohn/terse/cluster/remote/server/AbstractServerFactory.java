package io.github.chyohn.terse.cluster.remote.server;

import io.github.chyohn.terse.cluster.Cluster;
import io.github.chyohn.terse.cluster.config.ConfigConstant;
import io.github.chyohn.terse.cluster.config.Environment;
import io.github.chyohn.terse.cluster.remote.channel.ChannelHandler;
import io.github.chyohn.terse.cluster.remote.channel.Server;
import io.github.chyohn.terse.cluster.remote.message.MessageChannelHandlerImpl;

import java.net.InetSocketAddress;

public abstract class AbstractServerFactory implements ServerFactory {


    protected abstract Server doCreate(Cluster cluster, InetSocketAddress address, ChannelHandler handler);

    @Override
    public Server create(Cluster cluster) {
        Environment env = cluster.getEnvironment();
        int port = env.getProperty(ConfigConstant.CLUSTER_PORT, Integer.class, ConfigConstant.DEFAULT_CLUSTER_PORT);

        InetSocketAddress address = new InetSocketAddress("0.0.0.0", port);
        return doCreate(cluster, address, new MessageChannelHandlerImpl(cluster));
    }
}
