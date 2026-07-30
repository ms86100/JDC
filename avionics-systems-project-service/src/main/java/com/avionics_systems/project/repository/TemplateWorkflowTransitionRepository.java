package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.TemplateWorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TemplateWorkflowTransitionRepository extends JpaRepository<TemplateWorkflowTransition, UUID> {

    List<TemplateWorkflowTransition> findByTemplateIdOrderBySequenceAsc(UUID templateId);

    List<TemplateWorkflowTransition> findByTemplateIdAndFromStatusKey(UUID templateId, String fromStatusKey);

    List<TemplateWorkflowTransition> findByTemplateIdAndFromStatusKeyAndToStatusKey(UUID templateId, String fromStatusKey, String toStatusKey);
}