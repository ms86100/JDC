package com.jira.migration.repository.field;

import com.jira.migration.entity.field.FieldConfigurationOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FieldConfigurationOverrideRepository extends JpaRepository<FieldConfigurationOverride, UUID> {

    List<FieldConfigurationOverride> findByProjectId(UUID projectId);

    Optional<FieldConfigurationOverride> findByProjectIdAndIssueTypeIdAndFieldKey(
            UUID projectId, UUID issueTypeId, String fieldKey);

    Optional<FieldConfigurationOverride> findByProjectIdAndIssueTypeIdIsNullAndFieldKey(
            UUID projectId, String fieldKey);
}
