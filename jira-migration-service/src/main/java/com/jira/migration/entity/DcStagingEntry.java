package com.jira.migration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "dc_staging_entries", schema = "jira_migration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DcStagingEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "import_batch_id", nullable = false)
    private UUID importBatchId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "source_id", length = 100)
    private String sourceId;

    @Column(name = "source_key", length = 255)
    private String sourceKey;

    @Column(name = "validation_state", nullable = false, length = 20)
    private String validationState;

    @Column(name = "checksum", length = 128)
    private String checksum;

    @Column(name = "raw_xml", columnDefinition = "TEXT")
    private String rawXml;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parsed_payload", columnDefinition = "jsonb")
    private Map<String, Object> parsedPayload;

    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
