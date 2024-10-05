package io.github.chyohn.terse.cluster.event;

public interface ClusterEventListener<T extends ClusterEvent> {

    Class<T> eventType();

    void onEvent(T event);

}
