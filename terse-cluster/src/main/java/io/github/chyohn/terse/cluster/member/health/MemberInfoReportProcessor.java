package io.github.chyohn.terse.cluster.member.health;

import io.github.chyohn.terse.cluster.member.MemberManager;
import io.github.chyohn.terse.cluster.service.ServiceProcessor;

class MemberInfoReportProcessor implements ServiceProcessor<MemberReportRequest, MemberReportResponse> {
    final MemberManager memberManager;

    public MemberInfoReportProcessor(MemberManager memberManager) {
        this.memberManager = memberManager;
    }

    @Override
    public MemberReportResponse process(MemberReportRequest memberReportRequest) {
        memberManager.update(memberReportRequest.getMember());
        MemberReportResponse response = new MemberReportResponse();
        response.setMember(memberManager.getSelf());
        return response;
    }
}
