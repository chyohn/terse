package io.github.chyohn.terse.cluster.member.event;

import io.github.chyohn.terse.cluster.event.ClusterEvent;
import io.github.chyohn.terse.cluster.member.Member;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClusterMemberLeaveEvent extends ClusterEvent {
    private Member member;
}
