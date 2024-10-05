package io.github.chyohn.terse.cluster.member.health;

import io.github.chyohn.terse.cluster.Cluster;
import io.github.chyohn.terse.cluster.member.MemberState;
import io.github.chyohn.terse.cluster.utils.GlobalExecutor;

public class UnhealthyMemberInfoReportTask extends MemberInfoReportTask{

    private static final long DEFAULT_TASK_DELAY_MS = 5_000L;

    public UnhealthyMemberInfoReportTask(Cluster cluster) {
        super(cluster);
    }


    public static void newSchedule(Cluster cluster) {
        GlobalExecutor.scheduleByCommon(new UnhealthyMemberInfoReportTask(cluster), DEFAULT_TASK_DELAY_MS);
    }

    @Override
    public void run() {
        cluster.getMemberManager().allMembersWithoutSelf().stream()
                .filter(m -> !MemberState.UP.equals(m.getState())&&!MemberState.DOWN.equals(m.getState()))
                .forEach(this::doReport);
        GlobalExecutor.scheduleByCommon(this, DEFAULT_TASK_DELAY_MS);
    }
}
