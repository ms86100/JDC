package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepMigrationRequest {

    private UUID sharedStepId;

    private Integer fromVersion;
    private Integer toVersion;

    private Boolean migrateAllTests; // If true, migrate all tests; if false, only specified
    private List<UUID> testIds; // Specific tests to migrate

    private Boolean createBackup; // Create version backup before migration

    private Boolean notifyUsers; // Send notifications about migration

    private String migrationReason;
}