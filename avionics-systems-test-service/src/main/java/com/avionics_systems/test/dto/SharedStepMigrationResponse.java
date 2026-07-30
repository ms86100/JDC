package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepMigrationResponse {

    private UUID sharedStepId;
    private String sharedStepName;

    private Integer fromVersion;
    private Integer toVersion;

    private Integer testsMigrated;
    private Integer testsFailed;

    // Affected tests details
    private List<MigratedTest> migratedTests;
    private List<MigrationFailure> failures;

    // Version details
    private SharedStepVersionDiffResponse versionDiff;

    // Metadata
    private LocalDateTime migratedAt;
    private String migrationReason;
    private Integer totalUsageCountAffected;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MigratedTest {
        private UUID testId;
        private String testName;
        private Integer mappingsUpdated;
        private Boolean testNeedsReview; // If steps changed significantly
        private String previousVersion;
        private String newVersion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MigrationFailure {
        private UUID testId;
        private String testName;
        private String errorCode;
        private String errorMessage;
    }
}