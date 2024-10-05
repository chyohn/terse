package io.github.chyohn.terse.cluster.remote.client;

import static io.github.chyohn.terse.cluster.config.ConfigConstant.CLUSTER_NET_CLIENT_FACTORY_CLASS;
import static io.github.chyohn.terse.cluster.config.ConfigConstant.DEFAULT_CLUSTER_NET_CLIENT_FACTORY_CLASS;

import io.github.chyohn.terse.cluster.Cluster;
import io.github.chyohn.terse.cluster.config.Environment;
import io.github.chyohn.terse.cluster.event.ClusterEvent;
import io.github.chyohn.terse.cluster.event.ClusterEventListener;
import io.github.chyohn.terse.cluster.member.Member;
import io.github.chyohn.terse.cluster.member.MemberManager;
import io.github.chyohn.terse.cluster.member.MemberState;
import io.github.chyohn.terse.cluster.remote.channel.Channel;
import io.github.chyohn.terse.cluster.remote.channel.ChannelHandlerDelegate;
import io.github.chyohn.terse.cluster.remote.channel.Client;
import io.github.chyohn.terse.cluster.remote.message.MessageChannelHandler;
import io.github.chyohn.terse.cluster.remote.message.MessageChannelHandlerImpl;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RpcClientProxy implements ClusterEventListener<ClusterEvent> {

    private final NetClientFactory netClientFactory;
    private final Cluster cluster;

    private final MessageChannelHandler handler;

    public RpcClientProxy(Cluster cluster) {
        this.cluster = cluster;
        this.handler = new MessageChannelHandlerImpl(cluster);
        this.netClientFactory = buildFactory(cluster);
        cluster.addListener(this);
    }

    public <T extends Serializable> void request(Member target, Serializable request, RequestCallBack<T> callBack) {
        getClient(target).request(request, callBack);
    }

    private RpcClient getClient(Member target) {
        RpcClient client = clientMap.get(memberKey(target));
        if (client == null) {
            throw new RuntimeException("no client instance for " + target.getAddress());
        }
        if (!client.isConnected()) {
            throw new RuntimeException("client is not connected to  " + target.getAddress());
        }
        return client;
    }

    private final Map<String, RpcClient> clientMap = new ConcurrentHashMap<>();


    private NetClientFactory buildFactory(Cluster cluster) {
        Environment env = cluster.getEnvironment();
        String fcn = env.getProperty(CLUSTER_NET_CLIENT_FACTORY_CLASS, DEFAULT_CLUSTER_NET_CLIENT_FACTORY_CLASS);
        try {
            Class fc = Class.forName(fcn);
            if (!NetClientFactory.class.isAssignableFrom(fc)) {
                throw new IllegalArgumentException(String.format("class [%s] does not implement [%s]", fcn, fc));
            }
            Constructor constructor = fc.getConstructor(Cluster.class);
            return (NetClientFactory) constructor.newInstance(cluster);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public Class<ClusterEvent> eventType() {
        return ClusterEvent.class;
    }


    private final AtomicBoolean refreshing = new AtomicBoolean(false);
    private final AtomicBoolean waitingRefresh = new AtomicBoolean(false);

    @Override
    public void onEvent(ClusterEvent event) {

        if (!refreshing.compareAndSet(false, true)) {
            waitingRefresh.compareAndSet(false, true);
            return;
        }

        refresh();
        while (waitingRefresh.compareAndSet(true, false)) {
            refresh();
        }
        refreshing.set(false);
    }

    private void refresh() {
        MemberManager memberManager = cluster.getMemberManager();
        Set<Member> memberList = memberManager.allMembersWithoutSelf().stream()
                .filter(member -> !member.equals(memberManager.getSelf()))
                .filter(member -> !MemberState.DOWN.equals(member.getState()))
                .collect(Collectors.toSet());

        // add new member client
        Set<String> memberKeys = new HashSet<>();
        for (Member member : memberList) {
            String memberKey = memberKey(member);
            memberKeys.add(memberKey);
            if (!clientMap.containsKey(memberKey)) {
                try {
                    freshNewClient(member);
                } catch (Exception e) {
                    log.error("connect to {} error", member.getAddress(), e);
                }
            }
        }

        // remove not exist member
        Iterator<Map.Entry<String, RpcClient>> iterator = clientMap.entrySet().iterator();
        Set<String> removed = new HashSet<>();
        while (iterator.hasNext()) {
            Map.Entry<String, RpcClient> entry = iterator.next();
            if (memberKeys.contains(entry.getKey())) {
                continue;
            }
            RpcClient client = entry.getValue();
            client.shutdown();
            removed.add(entry.getKey());
        }
        for (String key : removed) {
            clientMap.remove(key);
        }
    }

    private final Map<String, Integer> retryCountOfMember = new ConcurrentHashMap<>();

    private void freshNewClient(Member member) {
        MemberManager memberManager = cluster.getMemberManager();
        String memberKey = memberKey(member);
        Client client = netClientFactory.create(member, new ChannelHandlerDelegate(handler) {
            @Override
            public void disconnected(Channel channel) {
                clientMap.remove(memberKey);
                memberManager.onFail(member, null);
                int count = retryCountOfMember.computeIfAbsent(memberKey, k -> 0);
                if (log.isDebugEnabled()) {
                    log.debug("retry new client for {} {} times", member.getAddress(), count);
                }
                if (count < 10) {
                    retryCountOfMember.put(memberKey, count + 1);
                    freshNewClient(member);
                } else {
                    retryCountOfMember.remove(memberKey);
                }
                super.disconnected(channel);
            }

            @Override
            public void connected(Channel channel) {
                retryCountOfMember.remove(memberKey);
                memberManager.join(member);
                super.connected(channel);
            }
        });
        RpcClient rpcClient = new RpcClientImpl(client, handler);
        RpcClient oldRpcClient = clientMap.putIfAbsent(memberKey, rpcClient);
        if (oldRpcClient == null) {
            rpcClient.start();
        } else {
            rpcClient.shutdown();
        }

    }

    private String memberKey(Member member) {
        return "Cluster-" + member.getAddress();
    }
}
