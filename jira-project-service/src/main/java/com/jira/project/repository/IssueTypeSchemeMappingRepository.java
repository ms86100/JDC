package com.jira.project.repository;

import com.jira.project.entity.IssueTypeSchemeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueTypeSchemeMappingRepository extends JpaRepository<IssueTypeSchemeMapping, IssueTypeSchemeMapping.IdClass> {

    List<IssueTypeSchemeMapping> findBySchemeId(UUID schemeId);

    List<IssueTypeSchemeMapping> findBySchemeIdAndIsDefaultTrue(UUID schemeId);

    List<IssueTypeSchemeMapping> findBySchemeIdAndIssueTypeName(UUID schemeId, String issueTypeName);
}