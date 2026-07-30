package com.avionics_systems.cluster.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.avionics_systems.cluster.config.ClusterProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ClusterCacheManager implements CacheManager {

    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();
    private final CacheManager l2CacheManager;
    private final ClusterProperties.CacheConfig config;
    private final CacheInvalidationService invalidationService;

    public ClusterCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            ClusterProperties properties,
            CacheInvalidationService invalidationService) {
        this.config = properties.getCache();
        this.invalidationService = invalidationService;

        this.l2CacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(config.getCaffeineExpireMinutes() * 2L)))
                .build();
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, this::createTieredCache);
    }

    @Override
    public Collection<String> getCacheNames() {
        return caches.keySet();
    }

    private Cache createTieredCache(String name) {
        CaffeineCache l1 = new CaffeineCache(name,
                Caffeine.newBuilder()
                        .maximumSize(config.getCaffeineMaxSize())
                        .expireAfterWrite(config.getCaffeineExpireMinutes(), TimeUnit.MINUTES)
                        .build());

        Cache l2 = l2CacheManager.getCache(name);

        return new TieredCache(name, l1, l2, invalidationService);
    }

    private static class TieredCache implements Cache {

        private final String name;
        private final CaffeineCache l1;
        private final Cache l2;
        private final CacheInvalidationService invalidationService;

        TieredCache(String name, CaffeineCache l1, Cache l2,
                    CacheInvalidationService invalidationService) {
            this.name = name;
            this.l1 = l1;
            this.l2 = l2;
            this.invalidationService = invalidationService;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return l1.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            ValueWrapper l1Result = l1.get(key);
            if (l1Result != null) {
                return l1Result;
            }
            if (l2 != null) {
                ValueWrapper l2Result = l2.get(key);
                if (l2Result != null) {
                    l1.put(key, l2Result.get());
                    return l2Result;
                }
            }
            return null;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            T l1Result = l1.get(key, type);
            if (l1Result != null) {
                return l1Result;
            }
            if (l2 != null) {
                T l2Result = l2.get(key, type);
                if (l2Result != null) {
                    l1.put(key, l2Result);
                    return l2Result;
                }
            }
            return null;
        }

        @Override
        public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
            ValueWrapper l1Result = l1.get(key);
            if (l1Result != null) {
                @SuppressWarnings("unchecked")
                T value = (T) l1Result.get();
                return value;
            }
            if (l2 != null) {
                ValueWrapper l2Result = l2.get(key);
                if (l2Result != null) {
                    @SuppressWarnings("unchecked")
                    T value = (T) l2Result.get();
                    l1.put(key, value);
                    return value;
                }
            }
            try {
                T value = valueLoader.call();
                put(key, value);
                return value;
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
        }

        @Override
        public void put(Object key, Object value) {
            l1.put(key, value);
            if (l2 != null) {
                l2.put(key, value);
            }
        }

        @Override
        public void evict(Object key) {
            l1.evict(key);
            if (l2 != null) {
                l2.evict(key);
            }
            if (invalidationService != null) {
                invalidationService.publishEviction(name, key.toString());
            }
        }

        @Override
        public void clear() {
            l1.clear();
            if (l2 != null) {
                l2.clear();
            }
            if (invalidationService != null) {
                invalidationService.publishClear(name);
            }
        }

        public void evictLocal(Object key) {
            l1.evict(key);
        }

        public void clearLocal() {
            l1.clear();
        }
    }

    public void handleRemoteEviction(String cacheName, String key) {
        Cache cache = caches.get(cacheName);
        if (cache instanceof TieredCache tiered) {
            if (key == null) {
                tiered.clearLocal();
            } else {
                tiered.evictLocal(key);
            }
        }
    }
}
