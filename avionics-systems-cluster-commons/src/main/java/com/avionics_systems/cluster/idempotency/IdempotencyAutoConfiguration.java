package com.avionics_systems.cluster.idempotency;

import com.avionics_systems.cluster.config.ClusterProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnClass(StringRedisTemplate.class)
public class IdempotencyAutoConfiguration {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public IdempotencyService idempotencyService(StringRedisTemplate redisTemplate,
                                                  ClusterProperties properties) {
        return new IdempotencyService(redisTemplate, properties);
    }
}
