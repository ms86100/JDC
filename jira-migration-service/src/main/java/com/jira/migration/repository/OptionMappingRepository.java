package com.jira.migration.repository;

import com.jira.migration.entity.OptionMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OptionMappingRepository extends JpaRepository<OptionMapping, UUID> {

    List<OptionMapping> findByJobId(UUID jobId);

    List<OptionMapping> findByWizardSessionId(UUID wizardSessionId);

    void deleteByJobId(UUID jobId);

    void deleteByWizardSessionId(UUID wizardSessionId);
}
