package io.github.chyohn.terse.cluster.service;

public interface ServiceProcessor<Req, Res> {

    Res process(Req request);
}
