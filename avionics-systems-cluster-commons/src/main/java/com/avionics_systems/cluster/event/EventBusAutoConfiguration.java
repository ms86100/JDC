package com.avionics_systems.cluster.event;

import com.avionics_systems.cluster.config.ClusterProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class EventBusAutoConfiguration {

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public ClusterEventBus redisClusterEventBus(
            StringRedisTemplate redisTemplate,
            RedisMessageListenerContainer listenerContainer,
            ClusterProperties properties) {
        return new RedisClusterEventBus(redisTemplate, listenerContainer, properties);
    }

    @Bean
    @ConditionalOnMissingBean(ClusterEventBus.class)
    public ClusterEventBus localClusterEventBus() {
        return new LocalClusterEventBus();
    }
}
