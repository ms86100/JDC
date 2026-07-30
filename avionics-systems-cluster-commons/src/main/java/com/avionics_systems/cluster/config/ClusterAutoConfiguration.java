package com.avionics_systems.cluster.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ClusterProperties.class)
public class ClusterAutoConfiguration {
}
