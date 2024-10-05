package io.github.chyohn.terse.cluster.member.discover.seed;

import io.github.chyohn.terse.cluster.member.Member;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MemberJoinRequest implements Serializable {
    Member member;
}
