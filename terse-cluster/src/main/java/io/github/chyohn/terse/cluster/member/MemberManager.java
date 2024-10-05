package io.github.chyohn.terse.cluster.member;

import io.github.chyohn.terse.cluster.Cluster;
import io.github.chyohn.terse.cluster.config.Environment;
import io.github.chyohn.terse.cluster.member.discover.MemberDiscover;
import io.github.chyohn.terse.cluster.member.event.ClusterMemberChangedEvent;
import io.github.chyohn.terse.cluster.member.event.ClusterMemberJoinEvent;
import io.github.chyohn.terse.cluster.member.event.ClusterMemberLeaveEvent;
import io.github.chyohn.terse.cluster.member.health.MemberInfoReportTask;
import io.github.chyohn.terse.cluster.member.health.UnhealthyMemberInfoReportTask;
import io.github.chyohn.terse.cluster.utils.InetUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static io.github.chyohn.terse.cluster.config.ConfigConstant.*;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MemberManager {

    private static final int MAX_FAIL_ACCESS_CNT = 5;
    protected static final String CONNECT_REFUSE_ERRMSG = "NioConnection refused";

    final Map<String, Member> memberOfAddress = new ConcurrentHashMap<>();
    final Cluster cluster;
    @Getter
    Member self;
    MemberDiscover discover;
    private final AtomicBoolean prepared = new AtomicBoolean(false);

    public MemberManager(Cluster cluster) {
        this.cluster = cluster;
    }

    public void prepare() {
        if (!prepared.compareAndSet(false, true)) {
            return;
        }
        Environment env = cluster.getEnvironment();
        int port = env.getProperty(CLUSTER_PORT, Integer.class, DEFAULT_CLUSTER_PORT);
        String localAddress = InetUtils.getSelfIp() + ":" + port;
        this.self = Member.of(localAddress);
        this.self.setState(MemberState.STARTING);
        memberOfAddress.put(self.getAddress(), self);

        if (discover == null) {
            discover = createMemberDiscover(cluster);
        }
        discover.start();

        MemberInfoReportTask.newSchedule(cluster);
        UnhealthyMemberInfoReportTask.newSchedule(cluster);
    }

    public void startUp() {
        self.setState(MemberState.UP);
        update(self);
    }

    private MemberDiscover createMemberDiscover(Cluster cluster) {
        Environment env = cluster.getEnvironment();
        String dc = env.getProperty(CLUSTER_MEMBER_DISCOVER_CLASS, DEFAULT_CLUSTER_MEMBER_DISCOVER_CLASS);
        try {
            Class cl = Class.forName(dc);
            Constructor constructor = cl.getConstructor(Cluster.class);
            return (MemberDiscover) constructor.newInstance(cluster);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addMember(Collection<Member> members) {
        for (Member member : members) {
            join(member);
        }
    }

    public boolean isSelf(Member member) {
        return member.equals(self) || member.getAddress().equals(self.getAddress());
    }

    public Member getMember(String address) {
        return memberOfAddress.get(address);
    }

    public Set<Member> allMembers() {
        HashSet<Member> set = new HashSet<>(memberOfAddress.values());
        set.add(self);
        return set;
    }

    public List<Member> allUpMembers() {
        return allMembers().stream()
                .filter(m -> MemberState.UP.equals(m.getState()))
                .collect(Collectors.toList());
    }

    public List<Member> allMembersWithoutSelf() {
        ArrayList<Member> set = new ArrayList<>(memberOfAddress.values());
        set.remove(self);
        return set;
    }

    public boolean update(Member newMember) {

        String address = newMember.getAddress();
        if (!memberOfAddress.containsKey(address)) {
            return false;
        }

        memberOfAddress.computeIfPresent(address, (s, member) -> {
            if (member.equals(newMember)) {
                return member;
            }
            boolean changed = Member.isBasicInfoChanged(newMember, member);
            Member.copy(newMember, member);
            if (changed) {
                notifyMemberChange(member);
            }
            return member;
        });

        return true;
    }

    public void join(Member member) {
        if (isSelf(member)) {
            return;
        }
        if (memberOfAddress.containsKey(member.getAddress())) {
            update(member);
            return;
        }
        if (MemberState.DOWN.equals(member.getState())) {
            member.setState(MemberState.SUSPICIOUS);
        }
        memberOfAddress.put(member.getAddress(), member);
        notifyMemberJoin(member);
    }

    public void leave(Member member) {
        if (!memberOfAddress.containsKey(member.getAddress())) {
            return;
        }
        Member old = memberOfAddress.remove(member.getAddress());
        notifyMemberLeave(old);
    }

    public void onSuccess(Member member, final Member receivedMember) {
        MemberState old = member.getState();
        member.setState(receivedMember.getState());
        member.setFailAccessCnt(0);
        if (Member.isChangedInExtendInfo(member, receivedMember)) {
            member.setExtendInfo(receivedMember.getExtendInfo());
            notifyMemberChange(member);
        } else if (!Objects.equals(old, member.getState())) {
            notifyMemberChange(member);
        }
    }


    public void onFail(Member member, Throwable ex) {
        MemberState old = member.getState();
        member.setState(MemberState.SUSPICIOUS);
        member.setFailAccessCnt(member.getFailAccessCnt() + 1);
        if (member.getFailAccessCnt() > MAX_FAIL_ACCESS_CNT
                || (ex != null && ex.getMessage().contains(CONNECT_REFUSE_ERRMSG))) {
            member.setState(MemberState.DOWN);
        }
        if (!Objects.equals(old, member.getState())) {
            notifyMemberChange(member);
        }
    }

    private void notifyMemberJoin(Member member) {
        if (log.isDebugEnabled()) {
            log.debug("member {} joined", member.getAddress());
        }
        cluster.publishEvent(ClusterMemberJoinEvent.builder().member(member).build());
    }

    private void notifyMemberChange(Member member) {
        if (log.isDebugEnabled()) {
            log.debug("member {} changed {}", member.getAddress(), member.getState());
        }
        cluster.publishEvent(ClusterMemberChangedEvent.builder().member(member).build());
    }

    private void notifyMemberLeave(Member member) {
        cluster.publishEvent(ClusterMemberLeaveEvent.builder().member(member).build());
    }

}
