package com.jira.gateway.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.jira.cluster.cache.CacheInvalidationService;
import com.jira.cluster.cache.ClusterCacheManager;
import com.jira.cluster.config.ClusterProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    public static final String GATEWAY_CACHE = "gatewayCache";

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired(required = false)
    private ClusterProperties clusterProperties;

    @Autowired(required = false)
    private CacheInvalidationService cacheInvalidationService;

    @Bean
    public CacheManager cacheManager() {
        if (redisConnectionFactory != null && clusterProperties != null && cacheInvalidationService != null) {
            ClusterCacheManager manager = new ClusterCacheManager(
                    redisConnectionFactory, clusterProperties, cacheInvalidationService);
            cacheInvalidationService.setCacheManager(manager);
            return manager;
        }
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(GATEWAY_CACHE);
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .recordStats();
    }
}
