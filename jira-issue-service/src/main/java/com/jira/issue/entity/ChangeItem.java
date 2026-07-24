package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "change_items", schema = "jira_issue")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "change_group_id", nullable = false)
    private UUID changeGroupId;

    @Column(name = "field_type", length = 50)
    @Builder.Default
    private String fieldType = "jira";

    @Column(nullable = false, length = 100)
    private String field;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "old_string", columnDefinition = "TEXT")
    private String oldString;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "new_string", columnDefinition = "TEXT")
    private String newString;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}