package com.jira.project.repository;

import com.jira.project.entity.TemplateWorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TemplateWorkflowStatusRepository extends JpaRepository<TemplateWorkflowStatus, UUID> {

    List<TemplateWorkflowStatus> findByTemplateIdOrderBySequenceAsc(UUID templateId);

    List<TemplateWorkflowStatus> findByTemplateIdAndStatusCategoryOrderBySequenceAsc(UUID templateId, String statusCategory);
}