package io.github.chyohn.terse.cluster;

import io.github.chyohn.terse.cluster.config.ConfigConstant;
import io.github.chyohn.terse.cluster.config.DefaultEnvironment;
import io.github.chyohn.terse.cluster.config.Environment;
import io.github.chyohn.terse.cluster.event.ClusterEvent;
import io.github.chyohn.terse.cluster.event.ClusterEventListener;
import io.github.chyohn.terse.cluster.event.ClusterEventPublisher;
import io.github.chyohn.terse.cluster.event.DefaultClusterEventPublisher;
import io.github.chyohn.terse.cluster.member.MemberManager;
import io.github.chyohn.terse.cluster.remote.channel.Server;
import io.github.chyohn.terse.cluster.remote.client.RpcClientProxy;
import io.github.chyohn.terse.cluster.remote.server.ServerFactory;
import io.github.chyohn.terse.cluster.service.ServiceProcessor;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.Setter;


class DefaultCluster implements Cluster {

    @Setter
    @Getter
    private Environment environment = new DefaultEnvironment();
    @Getter
    private MemberManager memberManager;
    @Getter
    private RpcClientProxy rpcClientProxy;
    private Server server;


    private final ClusterEventPublisher clusterEventPublisher = new DefaultClusterEventPublisher();
    private final Map<Class<? extends Serializable>, ServiceProcessor> processorMap = new ConcurrentHashMap<>();
    private final AtomicBoolean prepared = new AtomicBoolean(false);
    private final AtomicBoolean readied = new AtomicBoolean(false);

    /**
     * init and prepare all components of cluster.
     * <p>
     * at this moment, this instance can send cluster request, but can't handle cluster request from other instance from cluster.
     */
    public void prepare() {
        if (!prepared.compareAndSet(false, true)) {
            return;
        }
        prepareEnvironment();
        prepareServer();
        prepareRpcClient();
        prepareMemberManager();
    }

    public boolean isPrepared() {
        return prepared.get();
    }

    public void ready() {
        if (readied.compareAndSet(false, true)) {
            memberManager.startUp();
        }
    }

    public boolean isReady() {
        return readied.get();
    }

    protected void prepareEnvironment() {
        if (environment == null) {
            environment = new DefaultEnvironment();
        }
    }

    protected void prepareServer() {
        if (server != null) {
            return;
        }
        ServerFactory factory;
        try {
            String factoryName = environment.getProperty(ConfigConstant.CLUSTER_NET_SERVER_FACTOR_CLASS, ConfigConstant.DEFAULT_CLUSTER_NET_SERVER_FACTOR_CLASS);
            factory = (ServerFactory) Class.forName(factoryName).getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        server = factory.create(this);
        server.start();
    }

    protected void prepareRpcClient() {
        if (rpcClientProxy == null) {
            rpcClientProxy = new RpcClientProxy(this);
        }
    }

    protected void prepareMemberManager() {
        memberManager = new MemberManager(this);
        memberManager.prepare();
    }

    @Override
    public <T extends ClusterEvent> void addListener(ClusterEventListener<T> listener) {
        clusterEventPublisher.addListener(listener);
    }

    @Override
    public <T extends ClusterEvent> void publishEvent(T event) {
        clusterEventPublisher.publishEvent(event);
    }

    @Override
    public void registerProcessor(Class<? extends Serializable> requestType, ServiceProcessor processor) {
        processorMap.put(requestType, processor);
    }

    @Override
    public Object process(Object request) {
        ServiceProcessor processor = processorMap.get(request.getClass());
        return processor.process(request);
    }
}

