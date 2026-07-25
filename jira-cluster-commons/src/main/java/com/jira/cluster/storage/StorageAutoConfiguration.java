package com.jira.cluster.storage;

import com.jira.cluster.config.ClusterProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "cluster.storage.type", havingValue = "S3")
    public StorageProvider s3StorageProvider(ClusterProperties properties) {
        return new S3StorageProvider(properties);
    }

    @Bean
    @ConditionalOnProperty(name = "cluster.storage.type", havingValue = "LOCAL", matchIfMissing = true)
    public StorageProvider localStorageProvider(ClusterProperties properties) {
        return new LocalStorageProvider(properties);
    }
}
