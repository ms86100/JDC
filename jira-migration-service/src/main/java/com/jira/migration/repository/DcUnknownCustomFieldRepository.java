package com.jira.migration.repository;

import com.jira.migration.entity.DcUnknownCustomField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DcUnknownCustomFieldRepository extends JpaRepository<DcUnknownCustomField, UUID> {

    List<DcUnknownCustomField> findByJobId(UUID jobId);
}
