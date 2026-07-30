package com.avionics_systems.cluster.cache;

import com.avionics_systems.cluster.config.ClusterProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Slf4j
public class CacheInvalidationService implements MessageListener {

    private static final String SEPARATOR = "::";
    private static final String CLEAR_ALL = "__CLEAR__";

    private final StringRedisTemplate redisTemplate;
    private final String channel;
    private final String nodeId;
    private ClusterCacheManager cacheManager;

    public CacheInvalidationService(
            StringRedisTemplate redisTemplate,
            ClusterProperties properties,
            RedisMessageListenerContainer listenerContainer) {
        this.redisTemplate = redisTemplate;
        this.channel = properties.getCache().getInvalidationChannel();
        this.nodeId = properties.getNodeId();

        listenerContainer.addMessageListener(
                new MessageListenerAdapter(this, "onMessage"),
                new ChannelTopic(channel));
    }

    public void setCacheManager(ClusterCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void publishEviction(String cacheName, String key) {
        String message = nodeId + SEPARATOR + cacheName + SEPARATOR + key;
        redisTemplate.convertAndSend(channel, message);
    }

    public void publishClear(String cacheName) {
        String message = nodeId + SEPARATOR + cacheName + SEPARATOR + CLEAR_ALL;
        redisTemplate.convertAndSend(channel, message);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody());
        String[] parts = payload.split(SEPARATOR, 3);
        if (parts.length < 3) {
            return;
        }

        String senderNodeId = parts[0];
        if (nodeId.equals(senderNodeId)) {
            return;
        }

        String cacheName = parts[1];
        String key = parts[2];

        if (cacheManager != null) {
            cacheManager.handleRemoteEviction(cacheName, CLEAR_ALL.equals(key) ? null : key);
            log.debug("Remote cache invalidation from node {}: cache={}, key={}", senderNodeId, cacheName, key);
        }
    }
}
