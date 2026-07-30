package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_import_batch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "import_type", nullable = false, length = 30)
    private String importType;

    @Column(name = "ci_source", length = 100)
    private String ciSource;

    @Column(name = "ci_build_url")
    private String ciBuildUrl;

    @Column(name = "ci_job_name")
    private String ciJobName;

    @Column(name = "ci_build_number", length = 100)
    private String ciBuildNumber;

    @Column(length = 255)
    private String branch;

    @Column(name = "commit_sha", length = 100)
    private String commitSha;

    @Column(name = "total_tests")
    @Builder.Default
    private Integer totalTests = 0;

    @Column(name = "total_passed")
    @Builder.Default
    private Integer totalPassed = 0;

    @Column(name = "total_failed")
    @Builder.Default
    private Integer totalFailed = 0;

    @Column(name = "total_skipped")
    @Builder.Default
    private Integer totalSkipped = 0;

    @Column(length = 30)
    @Builder.Default
    private String status = "PROCESSING";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}