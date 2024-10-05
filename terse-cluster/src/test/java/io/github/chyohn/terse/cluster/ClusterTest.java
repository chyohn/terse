package io.github.chyohn.terse.cluster;

import ch.qos.logback.classic.Logger;
import io.github.chyohn.terse.cluster.config.ConfigConstant;
import io.github.chyohn.terse.cluster.member.Member;
import io.github.chyohn.terse.cluster.remote.client.NettyNetClientFactory;
import io.github.chyohn.terse.cluster.remote.server.NettyServerFactory;
import org.junit.jupiter.api.Test;

class ClusterTest {

    static {
        Logger logger;
    }
    @Test
    void cluster1() throws Exception{
        System.setProperty(ConfigConstant.CLUSTER_PORT, "9001");
        start();
    }

    @Test
    void cluster2() throws Exception{
        System.setProperty(ConfigConstant.CLUSTER_PORT, "9002");
        start();
    }

    @Test
    void cluster3() throws Exception{
        System.setProperty(ConfigConstant.CLUSTER_PORT, "9003");
        start();
    }

    @Test
    void cluster4() throws Exception{
        System.setProperty(ConfigConstant.CLUSTER_PORT, "9004");
        start();
    }

    @Test
    void cluster5() throws Exception{
        System.setProperty(ConfigConstant.CLUSTER_PORT, "9005");
        start();
    }

    private void start() throws Exception {

        System.setProperty(ConfigConstant.SEED_NODE_CONFIG_KEY, "localhost:9001");
        System.setProperty(ConfigConstant.CLUSTER_NET_CLIENT_FACTORY_CLASS, NettyNetClientFactory.class.getName());
        System.setProperty(ConfigConstant.CLUSTER_NET_SERVER_FACTOR_CLASS, NettyServerFactory.class.getName());
        Cluster cluster = Cluster.defaultCluster();
        cluster.prepare();
        cluster.ready();
        while (true) {
            Thread.sleep(2000);
            StringBuilder msg = new StringBuilder("成员\t\t\t\t状态\n");
            for (Member member : cluster.getMemberManager().allMembers()) {
                msg.append(member.getAddress()).append("\t").append(member.getState().name()).append("\n");
            }
            System.out.println(msg);
        }
    }
}