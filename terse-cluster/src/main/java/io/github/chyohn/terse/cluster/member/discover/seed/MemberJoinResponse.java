package io.github.chyohn.terse.cluster.member.discover.seed;

import io.github.chyohn.terse.cluster.member.Member;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MemberJoinResponse implements Serializable {
    List<Member> members;
    String requestId;
}
