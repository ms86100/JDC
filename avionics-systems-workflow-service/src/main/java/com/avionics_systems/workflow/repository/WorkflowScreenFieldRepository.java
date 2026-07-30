package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.WorkflowScreenField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowScreenFieldRepository extends JpaRepository<WorkflowScreenField, UUID> {
    List<WorkflowScreenField> findByTabIdOrderByOrderIndexAsc(UUID tabId);
    void deleteByTabId(UUID tabId);
}