package io.github.chyohn.terse.cluster.member.discover.seed;

import io.github.chyohn.terse.cluster.Cluster;
import io.github.chyohn.terse.cluster.config.ConfigConstant;
import io.github.chyohn.terse.cluster.member.Member;
import io.github.chyohn.terse.cluster.member.MemberManager;
import io.github.chyohn.terse.cluster.member.MemberState;
import io.github.chyohn.terse.cluster.member.discover.MemberDiscover;
import io.github.chyohn.terse.cluster.remote.client.RequestCallBack;
import io.github.chyohn.terse.cluster.remote.client.RpcClientProxy;
import io.github.chyohn.terse.cluster.utils.GlobalExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class SeedNodeMemberDiscover implements MemberDiscover {
    private Set<Member> seedNodes;
    private final Cluster cluster;
    private static final long DEFAULT_SYNC_TASK_DELAY_MS = 5_000L;

    public SeedNodeMemberDiscover(Cluster cluster) {
        this.cluster = cluster;
        registerProcess(cluster);

    }

    private void registerProcess(Cluster cluster) {
        MemberJoinProcessor processor = new MemberJoinProcessor(cluster.getMemberManager());
        cluster.registerProcessor(MemberJoinRequest.class, processor);
    }

    @Override
    public void start() {

        MemberManager memberManager = cluster.getMemberManager();
        Member self = memberManager.getSelf();

        String seeds = cluster.getEnvironment().getProperty(ConfigConstant.SEED_NODE_CONFIG_KEY);
        if (seeds != null) {
            seedNodes = Arrays.stream(seeds.split(ConfigConstant.SEPARATE_BY_COMMA))
                .filter(Objects::nonNull)
                .map(n -> n.replace("127.0.0.1", self.getIp()))
                .map(n -> n.replace("localhost", self.getIp()))
                .map(n -> {
                    Member member = Member.of(n.trim());
                    member.setState(MemberState.SUSPICIOUS);
                    return member;
                })
                .collect(Collectors.toSet());
        }

        if (seedNodes == null || seedNodes.isEmpty()) {
            return;
        }
        memberManager.addMember(seedNodes);
        joinToSeedNode();
        GlobalExecutor.scheduleByCommon(new SyncTask(), DEFAULT_SYNC_TASK_DELAY_MS);
    }

    private void joinToSeedNode() {
        MemberManager memberManager = cluster.getMemberManager();
        RpcClientProxy clientProxy = cluster.getRpcClientProxy();
        MemberJoinRequest request = new MemberJoinRequest();
        request.setMember(memberManager.getSelf());
        for (Member seedNode : seedNodes) {
            if (memberManager.isSelf(seedNode)) {
                continue;
            }
            Member member = memberManager.getMember(seedNode.getAddress());
            if (member == null || MemberState.DOWN.equals(member.getState())) {
                continue;
            }
            try {
                clientProxy.request(seedNode, request, new RequestCallBack<MemberJoinResponse>() {
                    @Override
                    public void onResponse(MemberJoinResponse response) {
                        if (log.isDebugEnabled()) {
                            log.debug("joint to {} success", seedNode.getAddress());
                        }
                        for (Member joinedMember : response.getMembers()) {
                            memberManager.join(joinedMember);
                        }
                    }

                    @Override
                    public void onException(Throwable e) {
                        log.error("join to seed node[{}] callback error", seedNode.getAddress(), e);
                        memberManager.onFail(member, e);
                    }
                });
            } catch (Exception e) {
                log.error("join to seed node[{}] error", seedNode.getAddress(), e);
                memberManager.onFail(member, e);
            }
        }
    }

    private class SyncTask implements Runnable {

        @Override
        public void run() {
            try {
                joinToSeedNode();
            } catch (Exception e) {
                log.error("join to seed node task error", e);
            } finally {
                GlobalExecutor.scheduleByCommon(this, DEFAULT_SYNC_TASK_DELAY_MS);
            }
        }
    }
}
