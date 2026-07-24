package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "external_page_links", schema = "jira_issue")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalPageLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "entity_type", nullable = false, length = 20)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "application_link_id")
    private UUID applicationLinkId;

    @Column(name = "page_id", length = 200)
    private String pageId;

    @Column(name = "space_key", length = 100)
    private String spaceKey;

    @Column(name = "linked_by")
    private UUID linkedBy;

    @Column(name = "linked_at")
    private LocalDateTime linkedAt;

    @PrePersist
    protected void onCreate() {
        if (linkedAt == null) {
            linkedAt = LocalDateTime.now();
        }
    }
}
