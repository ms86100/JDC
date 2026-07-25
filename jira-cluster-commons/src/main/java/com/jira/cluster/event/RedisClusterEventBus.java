package com.jira.cluster.event;

import com.jira.cluster.config.ClusterProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public class RedisClusterEventBus implements ClusterEventBus {

    private static final String SEPARATOR = "|";

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;
    private final String nodeId;
    private final Map<String, MessageListener> listeners = new ConcurrentHashMap<>();

    public RedisClusterEventBus(
            StringRedisTemplate redisTemplate,
            RedisMessageListenerContainer listenerContainer,
            ClusterProperties properties) {
        this.redisTemplate = redisTemplate;
        this.listenerContainer = listenerContainer;
        this.nodeId = properties.getNodeId();
    }

    @Override
    public void publish(String channel, String message) {
        String payload = nodeId + SEPARATOR + message;
        redisTemplate.convertAndSend("jira:events:" + channel, payload);
    }

    @Override
    public void subscribe(String channel, Consumer<String> listener) {
        String redisChannel = "jira:events:" + channel;
        MessageListener messageListener = (Message message, byte[] pattern) -> {
            String payload = new String(message.getBody());
            int sep = payload.indexOf(SEPARATOR);
            if (sep > 0) {
                String senderNode = payload.substring(0, sep);
                if (!nodeId.equals(senderNode)) {
                    String content = payload.substring(sep + 1);
                    listener.accept(content);
                }
            }
        };
        listeners.put(channel, messageListener);
        listenerContainer.addMessageListener(messageListener, new ChannelTopic(redisChannel));
        log.info("Subscribed to cluster event channel: {}", channel);
    }

    @Override
    public void unsubscribe(String channel) {
        MessageListener listener = listeners.remove(channel);
        if (listener != null) {
            listenerContainer.removeMessageListener(listener);
            log.info("Unsubscribed from cluster event channel: {}", channel);
        }
    }
}
