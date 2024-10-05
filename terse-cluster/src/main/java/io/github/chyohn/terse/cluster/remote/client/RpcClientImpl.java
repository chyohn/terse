package io.github.chyohn.terse.cluster.remote.client;

import io.github.chyohn.terse.cluster.remote.channel.Client;
import io.github.chyohn.terse.cluster.remote.message.MessageChannelHandler;
import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class RpcClientImpl implements RpcClient {

    final Client client;
    final MessageChannelHandler handler;

    public RpcClientImpl(Client client, MessageChannelHandler handler) {
        this.client = client;
        this.handler = handler;
    }

    @Override
    public <T extends Serializable> void request(Serializable request, RequestCallBack<T> callBack) {

        CompletableFuture<Serializable> future = doRequest(request);

        BiConsumer<Serializable, ? super Throwable> action = (response, throwable) -> {
            if (throwable != null) {
                callBack.onException(throwable);
                return;
            }
            callBack.onResponse((T)response);
        };
        if (callBack.getExecutor() == null) {
            future.whenComplete(action);
            return;
        }
        future.whenCompleteAsync(action, callBack.getExecutor());
    }

    private CompletableFuture<Serializable> doRequest(Serializable request) {
        return handler.send(client, request);
    }

    @Override
    public void shutdown() {
        client.close();
    }

    @Override
    public boolean isConnected() {
        return !client.isClosed() && client.isConnected();
    }

    @Override
    public void start() {
        client.start();
    }
}
