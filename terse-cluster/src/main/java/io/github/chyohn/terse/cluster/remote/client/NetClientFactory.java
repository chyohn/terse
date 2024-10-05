package io.github.chyohn.terse.cluster.remote.client;

import io.github.chyohn.terse.cluster.member.Member;
import io.github.chyohn.terse.cluster.remote.channel.ChannelHandler;
import io.github.chyohn.terse.cluster.remote.channel.Client;

public interface NetClientFactory {
    Client create(Member target, ChannelHandler handler);
}
