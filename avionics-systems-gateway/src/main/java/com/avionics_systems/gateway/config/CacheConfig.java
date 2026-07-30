package com.avionics_systems.gateway.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.avionics_systems.cluster.cache.CacheInvalidationService;
import com.avionics_systems.cluster.cache.ClusterCacheManager;
import com.avionics_systems.cluster.config.ClusterProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${cache.caffeine.initial-capacity:100}")
    private int initialCapacity;

    @Value("${cache.caffeine.maximum-size:500}")
    private int maximumSize;

    @Value("${cache.caffeine.expire-after-write-minutes:10}")
    private int expireAfterWriteMinutes;

    @Value("${cache.caffeine.expire-after-access-minutes:5}")
    private int expireAfterAccessMinutes;

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
                .initialCapacity(initialCapacity)
                .maximumSize(maximumSize)
                .expireAfterWrite(expireAfterWriteMinutes, TimeUnit.MINUTES)
                .expireAfterAccess(expireAfterAccessMinutes, TimeUnit.MINUTES)
                .recordStats();
    }
}
