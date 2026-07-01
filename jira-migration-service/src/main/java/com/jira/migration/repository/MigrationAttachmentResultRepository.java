package com.jira.migration.repository;

import com.jira.migration.entity.MigrationAttachmentResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MigrationAttachmentResultRepository extends JpaRepository<MigrationAttachmentResult, UUID> {

    List<MigrationAttachmentResult> findByJobIdOrderByCreatedAtAsc(UUID jobId);
}
