package io.github.chyohn.terse.cluster;

import io.github.chyohn.terse.cluster.config.Environment;
import io.github.chyohn.terse.cluster.event.ClusterEventPublisher;
import io.github.chyohn.terse.cluster.member.MemberManager;
import io.github.chyohn.terse.cluster.remote.client.RpcClientProxy;
import io.github.chyohn.terse.cluster.service.ServiceProcessor;
import java.io.Serializable;

public interface Cluster extends ClusterEventPublisher, ServiceProcessor<Object, Object> {

    static Cluster defaultCluster() {
        return new DefaultCluster();
    }

    /**
     * init and prepare all components of cluster.
     * <p>
     * at this moment, this instance can send cluster request, but can't handle cluster request from other instance from cluster.
     */
    void prepare();

    /**
     * check whether the cluster is prepared
     * @return if true, this instance can send cluster request
     */
    boolean isPrepared();

    /**
     * start after all resource prepared, like all {@link ServiceProcessor} are registered.
     * <p>
     * after this operate, this instance can receive request from other instance from cluster.
     *
     * @see #registerProcessor(Class, ServiceProcessor)
     */
    void ready();

    /**
     * check whether the cluster is ready
     * @return if true, cluster can receive request from other instance from cluster.
     */
    boolean isReady();


    /**
     * registry request processor
     *
     * @param requestType request type
     * @param processor the processor can handle the request type
     */
//    void registerProcessor(Class<? extends Request> requestType, RequestProcessor processor);
    void registerProcessor(Class<? extends Serializable> requestType, ServiceProcessor processor);

    Environment getEnvironment();

    void setEnvironment(Environment environment);

    MemberManager getMemberManager();

    RpcClientProxy getRpcClientProxy();

}

