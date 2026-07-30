package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "evidence_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "evidence_id", nullable = false)
    private UUID evidenceId;

    @Column(name = "metadata_key", nullable = false, length = 255)
    private String metadataKey;

    @Column(name = "metadata_value", columnDefinition = "TEXT")
    private String metadataValue;

    @Column(name = "metadata_type", length = 50)
    @Builder.Default
    private String metadataType = "STRING"; // STRING, NUMBER, BOOLEAN, DATE, JSON

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}