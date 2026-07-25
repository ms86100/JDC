package com.jira.cluster.cache;

import com.jira.cluster.config.ClusterProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@ConditionalOnClass(RedisConnectionFactory.class)
public class CacheAutoConfiguration {

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public CacheInvalidationService cacheInvalidationService(
            StringRedisTemplate redisTemplate,
            ClusterProperties properties,
            RedisMessageListenerContainer listenerContainer) {
        return new CacheInvalidationService(redisTemplate, properties, listenerContainer);
    }

    @Bean
    @ConditionalOnBean({RedisConnectionFactory.class, CacheInvalidationService.class})
    public ClusterCacheManager clusterCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            ClusterProperties properties,
            CacheInvalidationService invalidationService) {
        ClusterCacheManager manager = new ClusterCacheManager(redisConnectionFactory, properties, invalidationService);
        invalidationService.setCacheManager(manager);
        return manager;
    }
}
