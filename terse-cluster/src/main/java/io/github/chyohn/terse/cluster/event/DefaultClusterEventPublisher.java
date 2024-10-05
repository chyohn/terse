package io.github.chyohn.terse.cluster.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultClusterEventPublisher implements ClusterEventPublisher {


    private final Map<Class<? extends ClusterEvent>, List<ClusterEventListener<? extends ClusterEvent>>> listenersMap = new HashMap<>();

    @Override
    public <T extends ClusterEvent> void addListener(ClusterEventListener<T> listener) {
        listenersMap.computeIfAbsent(listener.eventType(), k -> {
            return new ArrayList<>();
        }).add(listener);
    }

    @Override
    public  <T extends ClusterEvent> void publishEvent(T event) {
        List<ClusterEventListener<ClusterEvent>> listeners = getListeners(event.getClass());
        for (ClusterEventListener<ClusterEvent> listener : listeners) {
            listener.onEvent(event);
        }
    }

    private List<ClusterEventListener<ClusterEvent>> getListeners(Class<? extends ClusterEvent> eventType) {
        return listenersMap.keySet().stream()
                .filter(k -> k.isAssignableFrom(eventType))
                .flatMap(k -> listenersMap.get(k).stream().map(l -> (ClusterEventListener<ClusterEvent>) l))
                .collect(Collectors.toList());
    }
}
