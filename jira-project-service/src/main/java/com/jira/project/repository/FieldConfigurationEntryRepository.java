package com.jira.project.repository;

import com.jira.project.entity.FieldConfigurationEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FieldConfigurationEntryRepository extends JpaRepository<FieldConfigurationEntry, UUID> {

    List<FieldConfigurationEntry> findBySchemeId(UUID schemeId);

    List<FieldConfigurationEntry> findBySchemeIdAndIssueTypeIdIsNull(UUID schemeId);

    List<FieldConfigurationEntry> findBySchemeIdAndIssueTypeId(UUID schemeId, UUID issueTypeId);
}
