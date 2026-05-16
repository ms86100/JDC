package com.jira.project.repository;

import com.jira.project.entity.TemplateIssueType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateIssueTypeRepository extends JpaRepository<TemplateIssueType, UUID> {

    List<TemplateIssueType> findByTemplateIdOrderBySequenceAsc(UUID templateId);

    Optional<TemplateIssueType> findByTemplateIdAndIsDefaultTrue(UUID templateId);

    List<TemplateIssueType> findByTemplateIdAndIsSubtaskFalse(UUID templateId);

    List<TemplateIssueType> findByTemplateIdAndIsSubtaskTrue(UUID templateId);
}