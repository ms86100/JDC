package com.jira.cluster.event;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public class LocalClusterEventBus implements ClusterEventBus {

    private final Map<String, Consumer<String>> listeners = new ConcurrentHashMap<>();

    @Override
    public void publish(String channel, String message) {
        Consumer<String> listener = listeners.get(channel);
        if (listener != null) {
            listener.accept(message);
        }
    }

    @Override
    public void subscribe(String channel, Consumer<String> listener) {
        listeners.put(channel, listener);
        log.info("Subscribed to local event channel: {} (single-node mode)", channel);
    }

    @Override
    public void unsubscribe(String channel) {
        listeners.remove(channel);
    }
}
