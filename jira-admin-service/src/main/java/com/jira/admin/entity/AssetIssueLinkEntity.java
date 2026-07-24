package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "asset_issue_links", schema = "jira_admin", indexes = {
        @Index(name = "idx_asset_issue_links_asset", columnList = "asset_id"),
        @Index(name = "idx_asset_issue_links_issue", columnList = "issue_id")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetIssueLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "link_type", length = 30)
    @Builder.Default
    private String linkType = "RELATED";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
