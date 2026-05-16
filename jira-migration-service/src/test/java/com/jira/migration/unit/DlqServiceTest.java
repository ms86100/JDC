package com.jira.migration.unit;

import com.jira.migration.batch.DeadLetterQueueService;
import com.jira.migration.config.DlqConfig;
import com.jira.migration.entity.DlqEntry;
import com.jira.migration.repository.DlqEntryRepository;
import com.jira.migration.repository.EntityStatusRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeadLetterQueueService.
 * Tests DLQ operations including enqueue, retry, discard, and statistics.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Dead Letter Queue Service Tests")
class DlqServiceTest {

    @Mock
    private DlqConfig dlqConfig;

    @Mock
    private DlqEntryRepository dlqEntryRepository;

    @Mock
    private EntityStatusRepository entityStatusRepository;

    private DeadLetterQueueService dlqService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        dlqService = new DeadLetterQueueService(dlqConfig, dlqEntryRepository, entityStatusRepository, objectMapper);

        // Default config
        when(dlqConfig.isEnabled()).thenReturn(true);
        when(dlqConfig.getMaxQueueSize()).thenReturn(100);
        when(dlqConfig.isAutoRetry()).thenReturn(false);
        when(dlqConfig.getMaxAutoRetryAttempts()).thenReturn(3);
        when(dlqConfig.getAutoRetryDelayHours()).thenReturn(1L);
        when(dlqConfig.isCleanupEnabled()).thenReturn(true);
        when(dlqConfig.getRetentionDurationMs()).thenReturn(604800000L); // 7 days
    }

    @Nested
    @DisplayName("Enqueue Operations")
    class EnqueueTests {

        @Test
        @DisplayName("Should enqueue failed operation with all fields")
        void shouldEnqueueFailedOperationWithAllFields() {
            // Given
            DeadLetterQueueService.FailedOperation operation = DeadOperation.builder()
                    .operationType("CREATE_ISSUE")
                    .entityType("ISSUE")
                    .payload("{\"key\":\"TEST-1\"}")
                    .errorMessage("Connection timeout")
                    .metadata(Map.of("jobId", UUID.randomUUID().toString()))
                    .build();

            when(dlqEntryRepository.save(any(DlqEntry.class))).thenAnswer(invocation -> {
                DlqEntry entry = invocation.getArgument(0);
                entry.setId(UUID.randomUUID());
                return entry;
            });

            // When
            dlqService.enqueue(operation);

            // Then
            ArgumentCaptor<DlqEntry> captor = ArgumentCaptor.forClass(DlqEntry.class);
            verify(dlqEntryRepository, times(1)).save(captor.capture());

            DlqEntry saved = captor.getValue();
            assertThat(saved.getOperationType()).isEqualTo("CREATE_ISSUE");
            assertThat(saved.getEntityType()).isEqualTo("ISSUE");
            assertThat(saved.getPayload()).isEqualTo("{\"key\":\"TEST-1\"}");
            assertThat(saved.getErrorMessage()).isEqualTo("Connection timeout");
            assertThat(saved.getStatus()).isEqualTo(DlqEntry.DlqStatus.PENDING);
            assertThat(saved.getAttemptCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should generate unique ID for operation without ID")
        void shouldGenerateUniqueIdForOperationWithoutId() {
            // Given
            DeadLetterQueueService.FailedOperation operation = DeadLetterQueueService.FailedOperation.builder()
                    .operationType("UPDATE_ISSUE")
                    .entityType("ISSUE")
                    .build();

            when(dlqEntryRepository.save(any(DlqEntry.class))).thenAnswer(invocation -> {
                DlqEntry entry = invocation.getArgument(0);
                entry.setId(UUID.randomUUID());
                return entry;
            });

            // When
            dlqService.enqueue(operation);

            // Then
            ArgumentCaptor<DlqEntry> captor = ArgumentCaptor.forClass(DlqEntry.class);
            verify(dlqEntryRepository).save(captor.capture());

            DlqEntry saved = captor.getValue();
            assertThat(saved.getId()).isNotNull();
        }

        @Test
        @DisplayName("Should not enqueue when DLQ is disabled")
        void shouldNotEnqueueWhenDlqDisabled() {
            // Given
            when(dlqConfig.isEnabled()).thenReturn(false);
            DeadLetterQueueService.FailedOperation operation = DeadLetterQueueService.FailedOperation.builder()
                    .operationType("CREATE_ISSUE")
                    .entityType("ISSUE")
                    .build();

            // When
            dlqService.enqueue(operation);

            // Then
            verify(dlqEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should enqueue from exception context")
        void shouldEnqueueFromExceptionContext() {
            // Given
            Exception error = new RuntimeException("Database connection failed");
            Map<String, Object> metadata = Map.of("jobId", UUID.randomUUID().toString());

            when(dlqEntryRepository.save(any(DlqEntry.class))).thenAnswer(invocation -> {
                DlqEntry entry = invocation.getArgument(0);
                entry.setId(UUID.randomUUID());
                return entry;
            });

            // When
            dlqService.enqueue("CREATE_PROJECT", "PROJECT", "{\"key\":\"PROJ-1\"}", error, metadata);

            // Then
            ArgumentCaptor<DlqEntry> captor = ArgumentCaptor.forClass(DlqEntry.class);
            verify(dlqEntryRepository).save(captor.capture());

            DlqEntry saved = captor.getValue();
            assertThat(saved.getOperationType()).isEqualTo("CREATE_PROJECT");
            assertThat(saved.getEntityType()).isEqualTo("PROJECT");
            assertThat(saved.getErrorMessage()).isEqualTo("Database connection failed");
            assertThat(saved.getErrorStackTrace()).contains("RuntimeException");
        }
    }

    @Nested
    @DisplayName("Query Operations")
    class QueryTests {

        @Test
        @DisplayName("Should return paginated pending operations")
        void shouldReturnPaginatedPendingOperations() {
            // Given
            List<DlqEntry> entries = List.of(
                    createDlqEntry("OP1", "CREATE_ISSUE", DlqEntry.DlqStatus.PENDING),
                    createDlqEntry("OP2", "UPDATE_ISSUE", DlqEntry.DlqStatus.PENDING)
            );
            when(dlqEntryRepository.findPending(any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(entries, PageRequest.of(0, 10), 2));

            // When
            List<DeadLetterQueueService.FailedOperation> result = dlqService.getPending(0, 10);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo("OP1");
            assertThat(result.get(1).getId()).isEqualTo("OP2");
        }

        @Test
        @DisplayName("Should return empty list when no pending operations")
        void shouldReturnEmptyListWhenNoPendingOperations() {
            // Given
            when(dlqEntryRepository.findPending(any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            // When
            List<DeadLetterQueueService.FailedOperation> result = dlqService.getPending(0, 10);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should get operation by ID")
        void shouldGetOperationById() {
            // Given
            UUID id = UUID.randomUUID();
            DlqEntry entry = createDlqEntry(id.toString(), "CREATE_ISSUE", DlqEntry.DlqStatus.PENDING);
            when(dlqEntryRepository.findById(id)).thenReturn(Optional.of(entry));

            // When
            Optional<DeadLetterQueueService.FailedOperation> result = dlqService.get(id.toString());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getOperationType()).isEqualTo("CREATE_ISSUE");
        }

        @Test
        @DisplayName("Should return empty for non-existent ID")
        void shouldReturnEmptyForNonExistentId() {
            // Given
            UUID id = UUID.randomUUID();
            when(dlqEntryRepository.findById(id)).thenReturn(Optional.empty());

            // When
            Optional<DeadLetterQueueService.FailedOperation> result = dlqService.get(id.toString());

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Retry Operations")
    class RetryTests {

        @Test
        @DisplayName("Should perform retry and return success")
        void shouldPerformRetryAndReturnSuccess() {
            // Given
            String dlqId = UUID.randomUUID().toString();
            DeadLetterQueueService.FailedOperation operation = DeadLetterQueueService.FailedOperation.builder()
                    .id(dlqId)
                    .operationType("CREATE_ISSUE")
                    .entityType("ISSUE")
                    .attemptCount(0)
                    .status("PENDING")
                    .metadata(new HashMap<>())
                    .build();

            // Add to memory cache
            java.lang.reflect.Field cacheField = DeadLetterQueueService.class.getDeclaredField("memoryCache");
            cacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, DeadLetterQueueService.FailedOperation> cache =
                    (Map<String, DeadLetterQueueService.FailedOperation>) cacheField.get(dlqService);
            cache.put(dlqId, operation);

            when(dlqEntryRepository.findById(any())).thenReturn(Optional.empty());

            // When
            DeadLetterQueueService.RetryResult result = dlqService.retry(dlqId);

            // Then
            assertThat(result.getDlqId()).isEqualTo(dlqId);
            assertThat(result.getAttemptCount()).isEqualTo(0); // Original count
        }

        @Test
        @DisplayName("Should throw exception for non-existent operation")
        void shouldThrowExceptionForNonExistentOperation() {
            // When/Then
            org.junit.jupiter.api.Assertions.assertThrows(
                    com.jira.migration.exception.DlqOperationException.class,
                    () -> dlqService.retry("non-existent-id")
            );
        }
    }

    @Nested
    @DisplayName("Statistics")
    class StatisticsTests {

        @Test
        @DisplayName("Should calculate DLQ statistics correctly")
        void shouldCalculateDlqStatisticsCorrectly() {
            // Given
            List<DlqEntry> entries = List.of(
                    createDlqEntry("OP1", "CREATE_ISSUE", DlqEntry.DlqStatus.PENDING),
                    createDlqEntry("OP2", "CREATE_ISSUE", DlqEntry.DlqStatus.PENDING),
                    createDlqEntry("OP3", "UPDATE_ISSUE", DlqEntry.DlqStatus.COMPLETED),
                    createDlqEntry("OP4", "CREATE_ISSUE", DlqEntry.DlqStatus.PENDING)
            );
            when(dlqEntryRepository.findAll()).thenReturn(entries);
            when(dlqEntryRepository.countPending()).thenReturn(3L);

            // When
            DeadLetterQueueService.DLQStatistics stats = dlqService.getStatistics();

            // Then
            assertThat(stats.getTotalEntries()).isEqualTo(4);
            assertThat(stats.getPendingCount()).isEqualTo(3);
            assertThat(stats.getByOperationType().get("CREATE_ISSUE")).isEqualTo(3);
            assertThat(stats.getByOperationType().get("UPDATE_ISSUE")).isEqualTo(1);
            assertThat(stats.getQueueCapacity()).isEqualTo(100);
        }
    }

    // Helper method to create test DLQ entries
    private DlqEntry createDlqEntry(String id, String operationType, DlqEntry.DlqStatus status) {
        return DlqEntry.builder()
                .id(UUID.fromString(id.isEmpty() ? UUID.randomUUID().toString() : id))
                .operationType(operationType)
                .entityType("ISSUE")
                .status(status)
                .attemptCount(0)
                .firstFailure(LocalDateTime.now().minusHours(1))
                .build();
    }

    // Builder for FailedOperation
    private static class DeadOperation extends DeadLetterQueueService.FailedOperation {
        static Builder builder() {
            return new Builder();
        }

        static class Builder {
            private final Map<String, Object> metadata = new HashMap<>();
            private String id;
            private String operationType;
            private String entityType;
            private String payload;
            private String errorMessage;
            private String errorStackTrace;
            private int attemptCount = 0;
            private java.time.Instant firstFailure;
            private java.time.Instant lastAttempt;
            private java.time.Instant scheduledRetryTime;
            private String lastError;
            private String status = "PENDING";

            Builder id(String id) { this.id = id; return this; }
            Builder operationType(String op) { this.operationType = op; return this; }
            Builder entityType(String type) { this.entityType = type; return this; }
            Builder payload(String p) { this.payload = p; return this; }
            Builder errorMessage(String msg) { this.errorMessage = msg; return this; }
            Builder metadata(Map<String, Object> m) { this.metadata.putAll(m); return this; }
            DeadLetterQueueService.FailedOperation build() {
                DeadLetterQueueService.FailedOperation op = new DeadLetterQueueService.FailedOperation();
                op.setId(id);
                op.setOperationType(operationType);
                op.setEntityType(entityType);
                op.setPayload(payload);
                op.setErrorMessage(errorMessage);
                op.setMetadata(metadata);
                op.setAttemptCount(attemptCount);
                op.setFirstFailure(firstFailure);
                op.setLastAttempt(lastAttempt);
                op.setScheduledRetryTime(scheduledRetryTime);
                op.setLastError(lastError);
                op.setStatus(status);
                return op;
            }
        }
    }
}