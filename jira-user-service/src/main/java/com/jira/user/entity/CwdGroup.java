package com.jira.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cwd_group", schema = "jira_admin")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CwdGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "directory_id", nullable = false)
    private UUID directoryId;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    private String description;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "lower_group_name", nullable = false)
    private String lowerGroupName;

    @Column(name = "is_global")
    private boolean isGlobal;

    @Column(name = "is_system")
    private boolean isSystem;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) createdDate = LocalDateTime.now();
        if (updatedDate == null) updatedDate = LocalDateTime.now();
        if (lowerGroupName == null && groupName != null) {
            lowerGroupName = groupName.toLowerCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
        if (lowerGroupName == null && groupName != null) {
            lowerGroupName = groupName.toLowerCase();
        }
    }
}