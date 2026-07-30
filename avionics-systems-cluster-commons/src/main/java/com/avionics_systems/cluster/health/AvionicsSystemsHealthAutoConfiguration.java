package com.avionics_systems.cluster.health;

import com.avionics_systems.cluster.config.ClusterProperties;
import com.avionics_systems.cluster.storage.StorageProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
public class AvionicsSystemsHealthAutoConfiguration {

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnClass(RedisConnectionFactory.class)
    public RedisHealthIndicator clusterRedisHealthIndicator(RedisConnectionFactory connectionFactory) {
        return new RedisHealthIndicator(connectionFactory);
    }

    @Bean
    @ConditionalOnBean(StorageProvider.class)
    public StorageHealthIndicator clusterStorageHealthIndicator(StorageProvider storageProvider,
                                                                ClusterProperties properties) {
        return new StorageHealthIndicator(storageProvider,
                properties.getStorage().getHealthCheckPath());
    }
}
