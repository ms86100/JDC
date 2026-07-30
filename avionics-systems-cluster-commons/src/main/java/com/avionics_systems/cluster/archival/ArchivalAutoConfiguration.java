package com.avionics_systems.cluster.archival;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "cluster.archival.enabled", havingValue = "true")
@EnableConfigurationProperties(ArchivalProperties.class)
public class ArchivalAutoConfiguration {
}
