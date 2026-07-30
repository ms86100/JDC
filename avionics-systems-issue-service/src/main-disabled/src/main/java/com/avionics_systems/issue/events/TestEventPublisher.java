package com.avionics_systems.issue.events;

import com.avionics_systems.issue.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Event Publisher Service for Test Management Events
 * Phase 15 - Event-Driven Architecture
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTestCreated(TestCreatedEvent event) {
        event.setEventId(UUID.randomUUID());
        event.setCreatedAt(LocalDateTime.now());
        send(KafkaConfig.TOPIC_TEST_CREATED, event.getTestId().toString(), event);
    }

    public void publishTestUpdated(TestUpdatedEvent event) {
        event.setEventId(UUID.randomUUID());
        event.setUpdatedAt(LocalDateTime.now());
        send(KafkaConfig.TOPIC_TEST_UPDATED, event.getTestId().toString(), event);
    }

    public void publishTestDeleted(TestDeletedEvent event) {
        event.setEventId(UUID.randomUUID());
        event.setDeletedAt(LocalDateTime.now());
        send(KafkaConfig.TOPIC_TEST_DELETED, event.getTestId().toString(), event);
    }

    public void publishExecutionStarted(ExecutionStartedEvent event) {
        event.setEventId(UUID.randomUUID());
        event.setStartedAt(LocalDateTime.now());
        send(KafkaConfig.TOPIC_EXECUTION_STARTED, event.getExecutionId().toString(), event);
    }

    public void publishExecutionCompleted(ExecutionCompletedEvent event) {
        event.setEventId(UUID.randomUUID());
        event.setCompletedAt(LocalDateTime.now());
        send(KafkaConfig.TOPIC_EXECUTION_COMPLETED, event.getExecutionId().toString(), event);
    }

    public void publishStepResult(StepResultRecordedEvent event) {
        event.setEventId(UUID.randomUUID());
        event.setRecordedAt(LocalDateTime.now());
        send(KafkaConfig.TOPIC_EXECUTION_STEP_RESULT, event.getExecutionId().toString(), event);
    }

    public void publishRequirementLinked(RequirementLinkedEvent event) {
        event.setEventId(UUID.randomUUID());
        event.setLinkedAt(LocalDateTime.now());
        send(KafkaConfig.TOPIC_REQUIREMENT_LINKED, event.getRequirementKey(), event);
    }

    public void publishDefectLinked(DefectLinkedEvent event) {
        event.setEventId(UUID.randomUUID());
        event.setLinkedAt(LocalDateTime.now());
        send(KafkaConfig.TOPIC_DEFECT_LINKED, event.getExecutionId().toString(), event);
    }

    public void publishImportCompleted(CucumberImportCompletedEvent event) {
        event.setEventId(UUID.randomUUID());
        event.setCompletedAt(LocalDateTime.now());
        send(KafkaConfig.TOPIC_IMPORT_COMPLETED, event.getImportBatchId().toString(), event);
    }

    public void publishCiCdResult(CiCdResultEvent event) {
        event.setEventId(UUID.randomUUID());
        event.setEventTime(LocalDateTime.now());
        send(KafkaConfig.TOPIC_CICD_RESULT, event.getBuildUrl(), event);
    }

    private void send(String topic, String key, Object payload) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, payload);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send event to topic {}: {}", topic, ex.getMessage());
            } else {
                log.debug("Event sent to topic {} with key {} at offset {}",
                        topic, key, result.getRecordMetadata().offset());
            }
        });
    }
}