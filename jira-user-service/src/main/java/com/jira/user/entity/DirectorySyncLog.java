package com.jira.user.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "directory_sync_logs", schema = "jira_user")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectorySyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "directory_id", nullable = false)
    private UUID directoryId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "users_added")
    private int usersAdded;

    @Column(name = "users_updated")
    private int usersUpdated;

    @Column(name = "users_removed")
    private int usersRemoved;

    @Column(name = "groups_synced")
    private int groupsSynced;

    @Column(length = 20)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String errors;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) startedAt = 