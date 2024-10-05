package io.github.chyohn.terse.cluster.event;

public interface ClusterEventPublisher {


    <T extends ClusterEvent> void addListener(ClusterEventListener<T> listener);

    <T extends ClusterEvent> void publishEvent(T event);
}
