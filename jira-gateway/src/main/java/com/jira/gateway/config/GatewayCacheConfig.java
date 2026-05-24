package com.jira.gateway.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Gateway Cache Configuration
 * Phase 7 - Polish & Performance
 * Provides multi-layer caching (Redis + Caffeine) for gateway-level caching
 */
@Configuration
@EnableCaching
public class GatewayCacheConfig {

    // Cache names
    public static final String SERVICE_DISCOVERY_CACHE = "serviceDiscovery";
    public static final String ROUTE_CONFIG_CACHE = "routeConfig";
    public static final String USER_SESSION_CACHE = "userSessions";
    public static final String AUTH_TOKEN_CACHE = "authTokens";
    public static final String RATE_LIMIT_CACHE = "rateLimit";

    // TTL constants
    private static final int SHORT_TTL = 5;   // 5 minutes
    private static final int MEDIUM_TTL = 15; // 15 minutes
    private static final int LONG_TTL = 60;   // 60 minutes

    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(MEDIUM_TTL))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(SERVICE_DISCOVERY_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(LONG_TTL)));
        cacheConfigurations.put(ROUTE_CONFIG_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(LONG_TTL)));
        cacheConfigurations.put(AUTH_TOKEN_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(SHORT_TTL)));
        cacheConfigurations.put(USER_SESSION_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(MEDIUM_TTL)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                SERVICE_DISCOVERY_CACHE,
                ROUTE_CONFIG_CACHE,
                AUTH_TOKEN_CACHE,
                USER_SESSION_CACHE,
                RATE_LIMIT_CACHE
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats());
        return cacheManager;
    }
}