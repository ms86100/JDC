package com.avionics_systems.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "design_item_metadata", schema = "jira_issue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignItemMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "issue_id", nullable = false, unique = true)
    private UUID issueId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "applicability", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> applicability = List.of();

    @Column(name = "supplier_sharing")
    @Builder.Default
    private Boolean supplierSharing = false;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "shared_supplier_ids", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> sharedSupplierIds = List.of();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
