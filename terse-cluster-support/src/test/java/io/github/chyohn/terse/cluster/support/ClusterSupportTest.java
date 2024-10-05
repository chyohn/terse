package io.github.chyohn.terse.cluster.support;

import io.github.chyohn.terse.Terse;
import io.github.chyohn.terse.cluster.Cluster;
import io.github.chyohn.terse.cluster.IClusterClient;
import io.github.chyohn.terse.cluster.config.ConfigConstant;
import io.github.chyohn.terse.cluster.member.Member;
import io.github.chyohn.terse.cluster.support.cal.CalculateCommand;
import io.github.chyohn.terse.cluster.support.cal.CalculateReceiverFactory;
import io.github.chyohn.terse.cluster.support.cal.Op;
import io.github.chyohn.terse.command.ICommandInvoker;
import io.github.chyohn.terse.spi.ISpiFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Slf4j
class ClusterSupportTest {

    @BeforeEach
    void before() {
        System.setProperty(ConfigConstant.SEED_NODE_CONFIG_KEY, "localhost:9001,127.0.0.1:9002");
        Terse.registerReceiverFactory(new CalculateReceiverFactory());
    }
    @Test
    void cluster1() throws Exception {
        startCluster(9001);
        info();
    }

    @Test
    void cluster2() throws Exception {
        startCluster(9002);
        info();
    }

    @Test
    void cluster3() throws Exception {
        startCluster(9003);
        info();
    }

    @Test
    void request() throws Exception{

        startCluster(9004);
        Thread.sleep(5000);

        ICommandInvoker invoker = ISpiFactory.get(ICommandInvoker.class);
        int count = 5000;
        long start = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            CalculateCommand command = new CalculateCommand(1, Op.ADD, i);
            invoker.asyncRun(command, r -> {
                log.info("receive result: {} {} {} = {}", command.getX(), command.getOp().name(), command.getY(), r);
                latch.countDown();
            });
            Thread.sleep(100);
        }

        latch.await();
        System.out.println("end ---------------------- " + (System.currentTimeMillis() - start));
    }

    void startCluster(int port) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConfigConstant.CLUSTER_PORT, port);
        Terse.initCluster(config);
        Terse.readyCluster();
    }

    private void info() throws Exception {

        ClusterSupport client = (ClusterSupport)ISpiFactory.get(IClusterClient.class);
        Cluster cluster = client.getCluster();
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