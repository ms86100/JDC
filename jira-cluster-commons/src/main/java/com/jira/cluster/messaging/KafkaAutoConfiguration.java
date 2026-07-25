package com.jira.cluster.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers", matchIfMissing = false)
public class KafkaAutoConfiguration {
}
