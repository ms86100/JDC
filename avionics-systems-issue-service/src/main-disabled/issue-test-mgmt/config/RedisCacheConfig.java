package com.avionics_systems.issue.config;

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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis & Caffeine Cache Configuration
 * Phase 13 - Performance & Scale
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    public static final String TESTS_CACHE = "tests";
    public static final String TEST_SETS_CACHE = "testSets";
    public static final String TEST_PLANS_CACHE = "testPlans";
    public static final String TEST_EXECUTIONS_CACHE = "testExecutions";
    public static final String TEST_FOLDERS_CACHE = "testFolders";
    public static final String ENVIRONMENTS_CACHE = "environments";
    public static final String TRACEABILITY_CACHE = "traceability";
    public static final String REPORTS_CACHE = "reports";
    public static final String PROJECT_CACHE = "projects";
    public static final String TEST_SUMMARY_CACHE = "testSummary";
    public static final String REQUIREMENT_COVERAGE_CACHE = "requirementCoverage";

    private static final int SHORT_TTL = 5;
    private static final int MEDIUM_TTL = 15;
    private static final int LONG_TTL = 60;

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(MEDIUM_TTL))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(TESTS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(SHORT_TTL)));
        cacheConfigurations.put(TEST_EXECUTIONS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(SHORT_TTL)));
        cacheConfigurations.put(REPORTS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(SHORT_TTL)));
        cacheConfigurations.put(TEST_SETS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(MEDIUM_TTL)));
        cacheConfigurations.put(TEST_PLANS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(MEDIUM_TTL)));
        cacheConfigurations.put(TRACEABILITY_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(MEDIUM_TTL)));
        cacheConfigurations.put(TEST_FOLDERS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(LONG_TTL)));
        cacheConfigurations.put(ENVIRONMENTS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(LONG_TTL)));
        cacheConfigurations.put(PROJECT_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(LONG_TTL)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats());
        return cacheManager;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}