package com.jira.issue.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Configuration for Event-Driven Architecture
 * Phase 15 - Event-Driven Messaging
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:jira-issue-service}")
    private String groupId;

    // Topic names
    public static final String TOPIC_TEST_CREATED = "test-events.test-created";
    public static final String TOPIC_TEST_UPDATED = "test-events.test-updated";
    public static final String TOPIC_TEST_DELETED = "test-events.test-deleted";
    public static final String TOPIC_EXECUTION_STARTED = "test-events.execution-started";
    public static final String TOPIC_EXECUTION_COMPLETED = "test-events.execution-completed";
    public static final String TOPIC_EXECUTION_STEP_RESULT = "test-events.execution-step-result";
    public static final String TOPIC_REQUIREMENT_LINKED = "test-events.requirement-linked";
    public static final String TOPIC_DEFECT_LINKED = "test-events.defect-linked";
    public static final String TOPIC_IMPORT_COMPLETED = "test-events.import-completed";
    public static final String TOPIC_CICD_RESULT = "test-events.cicd-result";

    @Bean
    public NewTopic testCreatedTopic() {
        return TopicBuilder.name(TOPIC_TEST_CREATED)
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic testUpdatedTopic() {
        return TopicBuilder.name(TOPIC_TEST_UPDATED)
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic executionCompletedTopic() {
        return TopicBuilder.name(TOPIC_EXECUTION_COMPLETED)
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic cicdResultTopic() {
        return TopicBuilder.name(TOPIC_CICD_RESULT)
                .partitions(5).replicas(1).build();
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.jira.issue.events");
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        return factory;
    }
}