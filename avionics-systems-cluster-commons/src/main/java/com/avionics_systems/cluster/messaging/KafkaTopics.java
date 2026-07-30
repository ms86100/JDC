package com.avionics_systems.cluster.messaging;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kafka topic names used across all micro-services.
 *
 * <p>All values are configurable via {@code app.kafka.topics.*} properties.
 * Defaults match the original hardcoded values for zero-regression.</p>
 */
@Data
@ConfigurationProperties(prefix = "app.kafka.topics")
public class KafkaTopics {

    private String issueEvents = "avionics-systems.issue.events";
    private String workflowEvents = "avionics-systems.workflow.events";
    private String notificationEvents = "avionics-systems.notification.events";
    private String auditEvents = "avionics-systems.audit.events";
    private String searchIndexEvents = "avionics-systems.search.index";
    private String userEvents = "avionics-systems.user.events";
    private String projectEvents = "avionics-systems.project.events";
}
