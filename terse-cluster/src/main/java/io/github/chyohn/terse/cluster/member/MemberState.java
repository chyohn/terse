package io.github.chyohn.terse.cluster.member;

public enum MemberState {

    STARTING,
    // ready for service
    UP,
    // 可疑
    SUSPICIOUS,
    DOWN;
}

