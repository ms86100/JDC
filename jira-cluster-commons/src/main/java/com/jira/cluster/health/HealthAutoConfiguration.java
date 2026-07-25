package com.jira.cluster.health;

import com.jira.cluster.storage.StorageProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
public class HealthAutoConfiguration {

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnClass(RedisConnectionFactory.class)
    public RedisHealthIndicator clusterRedisHealthIndicator(RedisConnectionFactory connectionFactory) {
        return new RedisHealthIndicator(connectionFactory);
    }

    @Bean
    @ConditionalOnBean(StorageProvider.class)
    public StorageHealthIndicator clusterStorageHealthIndicator(StorageProvider storageProvider) {
        return new StorageHealthIndicator(storageProvider);
    }
}
