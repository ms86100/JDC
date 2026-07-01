package com.jira.migration.repository;

import com.jira.migration.entity.WizardSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WizardSessionRepository extends JpaRepository<WizardSession, UUID> {

    List<WizardSession> findByInitiatedByAndStatusOrderByUpdatedAtDesc(UUID userId, String status);

    Optional<WizardSession> findByIdAndInitiatedBy(UUID id, UUID userId);

    Optional<WizardSession> findFirstByMigrationJobId(UUID migrationJobId);
}
