package io.github.chyohn.terse.cluster.member.health;

import io.github.chyohn.terse.cluster.member.Member;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MemberReportResponse implements Serializable {
    Member member;
    String requestId;
}
