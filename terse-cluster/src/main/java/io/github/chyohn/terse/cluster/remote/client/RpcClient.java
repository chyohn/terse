package io.github.chyohn.terse.cluster.remote.client;

import java.io.Serializable;

public interface RpcClient {

    <T extends Serializable> void request(Serializable request, RequestCallBack<T> callBack);

    void shutdown();

    boolean isConnected();
    void start();
}
