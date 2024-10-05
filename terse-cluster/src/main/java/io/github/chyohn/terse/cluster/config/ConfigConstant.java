package io.github.chyohn.terse.cluster.config;

import io.github.chyohn.terse.cluster.member.discover.seed.SeedNodeMemberDiscover;
import io.github.chyohn.terse.cluster.remote.client.NettyNetClientFactory;
import io.github.chyohn.terse.cluster.remote.codec.HessianCoder;
import io.github.chyohn.terse.cluster.remote.server.NettyServerFactory;

public interface ConfigConstant {

    // format is "ip:port,ip:port,....,ip:port"
    String SEED_NODE_CONFIG_KEY = "cluster.seed.nodes";
    String SEPARATE_BY_COMMA = ",";

    String CLUSTER_PORT = "cluster.port";
    int DEFAULT_CLUSTER_PORT = 9001;


    String CLUSTER_MEMBER_DISCOVER_CLASS = "cluster.member.discover.class";
    String DEFAULT_CLUSTER_MEMBER_DISCOVER_CLASS = SeedNodeMemberDiscover.class.getName();

    String CLUSTER_NET_CLIENT_FACTORY_CLASS = "cluster.net.client.factory.class";
    String DEFAULT_CLUSTER_NET_CLIENT_FACTORY_CLASS = NettyNetClientFactory.class.getName();

    String CLUSTER_CODER_CLASS = "cluster.coder.class";
    String DEFAULT_CLUSTER_CODER_CLASS = HessianCoder.class.getName();

    String CLUSTER_NET_SERVER_FACTOR_CLASS = "cluster.server.factory.class";
    String DEFAULT_CLUSTER_NET_SERVER_FACTOR_CLASS = NettyServerFactory.class.getName();

    String CLUSTER_IO_WORKER_NUM = "cluster.io.worker.num";
    int DEFAULT_CLUSTER_IO_WORKER_NUM = 4;

}
