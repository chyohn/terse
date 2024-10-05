package io.github.chyohn.terse.cluster.remote.message;

import io.github.chyohn.terse.cluster.Cluster;
import io.github.chyohn.terse.cluster.remote.channel.Channel;
import io.github.chyohn.terse.cluster.remote.codec.Coder;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageChannelHandlerImpl implements MessageChannelHandler {
    private final Cluster cluster;
    private final Coder coder;
    private final Map<String, CompletableFuture> futureOfReqId = new ConcurrentHashMap<>();
    private final Map<String, Channel> channelOfReqId = new ConcurrentHashMap<>();

    public MessageChannelHandlerImpl(Cluster cluster) {
        this.cluster = cluster;
        this.coder = Coder.getInstance(cluster.getEnvironment());
    }


    public <R> CompletableFuture<R> send(Channel channel, Object msg) {
        String uuid = UUID.randomUUID().toString();
        Request request = new Request();
        request.setId(uuid);
        request.setData(msg);
        byte[] data;
        try {
            data = coder.encode(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        CompletableFuture<R> future = new CompletableFuture<>();
        this.addFuture(request.getId(), channel, future);
        try {
            channel.send(data);
        } catch (Throwable e) {
            removeFuture(request.getId());
            throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
        }
        return future;
    }

    @Override
    public void connected(Channel channel) {

    }

    @Override
    public void disconnected(Channel channel) {
        Set<String> ids = new HashSet<>();
        for (Entry<String, Channel> entry : channelOfReqId.entrySet()) {
            if (!entry.getValue().equals(channel)) {
                continue;
            }
            String id = entry.getKey();
            ids.add(id);
            CompletableFuture future = futureOfReqId.remove(id);
            if (future != null) {
                future.cancel(true);
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        for (String id : ids) {
            channelOfReqId.remove(id);
        }

    }

    @Override
    public void received(Channel channel, Object message) {
        ByteBuffer requestBuffer = (ByteBuffer) message;
        Object object = null;
        try {
            object = coder.decode(requestBuffer.array());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (object instanceof Request) {
            onReceiveRequest(channel, (Request) object);
        } else if (object instanceof Response) {
            onReceiveResponse((Response) object);
        } else {
            throw new RuntimeException("not support message type of " + (object == null ? null : object.getClass()));
        }
    }

    @Override
    public void caught(Channel channel, Throwable throwable) {

    }

    private void onReceiveResponse(Response res) {
        String reqId = res.getId();
        CompletableFuture<Object> future = removeFuture(reqId);
        if (future == null) {
            log.warn("dead message for req id： {}, res: {}", reqId, res);
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("handle req[{}] future", reqId);
        }
        if (res.getE() != null) {
            future.completeExceptionally(res.getE());
        } else {
            future.complete(res.getData());
        }
    }

    private void onReceiveRequest(Channel channel, Request request) {
        Response response = new Response();
        try {
            response.setId(request.getId());
            Object result = cluster.process(request.getData());
            if (result instanceof CompletableFuture) {
                CompletableFuture<Object> future = (CompletableFuture<Object>) result;
                future.whenComplete((res, throwable) -> {
                    try {
                        response.setData(res);
                        response.setE(throwable);
                        byte[] data = coder.encode(response);
                        channel.send(data);
                    } catch (IOException e) {
                        log.error("async response[{}] error by", response.getId(), e);
                        handleRequestError(channel, response, e);
                    }
                });
            } else {
                response.setData(result);
                byte[] data = coder.encode(response);
                channel.send(data);
            }
        } catch (Throwable e) {
            log.error("cluster handle request[{}] error by", response.getId(), e);
            handleRequestError(channel, response, e);
        }

    }

    private void handleRequestError(Channel channel, Response response, Throwable e) {
        try {
            response.setE(e);
            byte[] data = coder.encode(response);
            channel.send(data);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void addFuture(String id, Channel channel, CompletableFuture future) {
        if (log.isDebugEnabled()) {
            log.debug("add req[{}] response future", id);
        }
        futureOfReqId.put(id, future);
        channelOfReqId.put(id, channel);
    }

    private CompletableFuture removeFuture(String id) {
        channelOfReqId.remove(id);
        return futureOfReqId.remove(id);
    }
}
