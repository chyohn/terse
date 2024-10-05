package io.github.chyohn.terse.cluster.member.health;

import io.github.chyohn.terse.cluster.Cluster;
import io.github.chyohn.terse.cluster.member.Member;
import io.github.chyohn.terse.cluster.member.MemberManager;
import io.github.chyohn.terse.cluster.member.MemberState;
import io.github.chyohn.terse.cluster.remote.client.RequestCallBack;
import io.github.chyohn.terse.cluster.utils.GlobalExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MemberInfoReportTask implements Runnable {

    protected final Cluster cluster;
    private int cursor = 0;
    private static final long DEFAULT_TASK_DELAY_MS = 2_000L;

    public MemberInfoReportTask(Cluster cluster) {
        this.cluster = cluster;
        registerProcessor(cluster);
    }

    private void registerProcessor(Cluster cluster) {
        cluster.registerProcessor(MemberReportRequest.class, new MemberInfoReportProcessor(cluster.getMemberManager()));
    }

    public static void newSchedule(Cluster cluster) {
        GlobalExecutor.scheduleByCommon(new MemberInfoReportTask(cluster), DEFAULT_TASK_DELAY_MS);
    }


    @Override
    public void run() {
        try {
            List<Member> members = cluster.getMemberManager().allMembersWithoutSelf()
                    .stream()
                    .filter(m -> !MemberState.DOWN.equals(m.getState()))
                    .collect(Collectors.toList());

            if (members.isEmpty()) {
                return;
            }

            this.cursor = (this.cursor + 1) % members.size();
            Member target = members.get(cursor);

            doReport(target);
        } finally {
            GlobalExecutor.scheduleByCommon(this, DEFAULT_TASK_DELAY_MS);
        }
    }

    protected void doReport(Member target) {
        MemberManager memberManager = cluster.getMemberManager();
        MemberReportRequest request = new MemberReportRequest();
        request.setMember(memberManager.getSelf());
        try {
            cluster.getRpcClientProxy().request(target, request, new RequestCallBack<MemberReportResponse>() {
                @Override
                public void onResponse(MemberReportResponse response) {
                    memberManager.onSuccess(target, response.getMember());
                    if (log.isDebugEnabled()) {
                        log.debug("report to {} is success", target.getAddress());
                    }
                }

                @Override
                public void onException(Throwable e) {
                    log.error("report to {} callback error", target.getAddress(), e);
                    memberManager.onFail(target, e);
                }
            });
        } catch (Throwable e) {
            log.error("report to {} error", target.getAddress(), e);
            memberManager.onFail(target, e);
        }
    }
}
