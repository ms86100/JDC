package com.avionics_systems.cluster.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.UUID;

@Data
@ConfigurationProperties(prefix = "cluster")
public class ClusterProperties {

    private String nodeId = UUID.randomUUID().toString().substring(0, 8);
    private boolean enabled = true;

    private LockConfig lock = new LockConfig();
    private StorageConfig storage = new StorageConfig();
    private CacheConfig cache = new CacheConfig();
    private EventConfig event = new EventConfig();
    private IdempotencyConfig idempotency = new IdempotencyConfig();

    @Data
    public static class LockConfig {
        private String type = "DATABASE";
        private int defaultTtlMinutes = 5;
        private int waitTimeoutSeconds = 30;
        private int retryDelayMillis = 100;

        public Duration getDefaultTtl() {
            return Duration.ofMinutes(defaultTtlMinutes);
        }

        public Duration getWaitTimeout() {
            return Duration.ofSeconds(waitTimeoutSeconds);
        }
    }

    @Data
    public static class StorageConfig {
        private String type = "LOCAL";
        private String basePath = "/var/avionics-systems/shared";

        private S3Config s3 = new S3Config();

        @Data
        public static class S3Config {
            private String endpoint = "http://minio:9000";
            private String bucket = "avionics-systems-attachments";
            private String accessKey = "minioadmin";
            private String secretKey = "minioadmin";
            private String region = "us-east-1";
            private boolean pathStyleAccess = true;
        }

        private String healthCheckPath = ".health-check";
    }

    @Data
    public static class CacheConfig {
        private boolean redisEnabled = false;
        private String invalidationChannel = "avionics-systems:cache:invalidation";
        private int caffeineMaxSize = 1000;
        private int caffeineExpireMinutes = 10;
    }

    @Data
    public static class EventConfig {
        private String channelPrefix = "avionics-systems:events:";
    }

    @Data
    public static class IdempotencyConfig {
        private String keyPrefix = "idempotency:";
        private int ttlMinutes = 30;
    }
}
