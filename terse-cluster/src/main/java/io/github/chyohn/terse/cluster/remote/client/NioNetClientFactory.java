package io.github.chyohn.terse.cluster.remote.client;

import io.github.chyohn.terse.cluster.Cluster;
import io.github.chyohn.terse.cluster.member.Member;
import io.github.chyohn.terse.cluster.remote.channel.ChannelHandler;
import io.github.chyohn.terse.cluster.remote.channel.Client;
import io.github.chyohn.terse.cluster.remote.channel.nio.client.NIOClient;
import java.net.InetSocketAddress;

public class NioNetClientFactory implements NetClientFactory {
    final Cluster cluster;
    public NioNetClientFactory(Cluster cluster) {
        this.cluster = cluster;
    }


    @Override
    public Client create(Member target, ChannelHandler handler) {
        return new NIOClient(new InetSocketAddress(target.getIp(), target.getPort()), handler);
    }
}
