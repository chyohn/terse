package io.github.chyohn.terse.cluster.member.discover.seed;

import io.github.chyohn.terse.cluster.member.Member;
import io.github.chyohn.terse.cluster.member.MemberManager;
import io.github.chyohn.terse.cluster.service.ServiceProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
class MemberJoinProcessor implements ServiceProcessor<MemberJoinRequest, MemberJoinResponse> {

    private final MemberManager manager;

    public MemberJoinProcessor(MemberManager manager) {
        this.manager = manager;
    }

    @Override
    public MemberJoinResponse process(MemberJoinRequest memberJoinRequest) {

        Member joiner = memberJoinRequest.getMember();

        List<Member> otherMembers = manager.allUpMembers().stream()
                .filter(m -> !joiner.getAddress().equals(m.getAddress()))
                .collect(Collectors.toList());

        if (log.isDebugEnabled()) {
            log.debug("member {} joined status {}", joiner.getAddress(), joiner.getState());
        }
        manager.join(joiner);

        MemberJoinResponse response = new MemberJoinResponse();
        response.setMembers(otherMembers);
        return response;
    }

}
