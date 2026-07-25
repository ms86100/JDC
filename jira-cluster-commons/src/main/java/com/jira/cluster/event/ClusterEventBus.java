package com.jira.cluster.event;

import java.util.function.Consumer;

public interface ClusterEventBus {

    void publish(String channel, String message);

    void subscribe(String channel, Consumer<String> listener);

    void unsubscribe(String channel);
}
