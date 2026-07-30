package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.WorkflowPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowPermissionRepository extends JpaRepository<WorkflowPermission, UUID> {

    List<WorkflowPermission> findByWorkflowId(UUID workflowId);

    List<WorkflowPermission> findByWorkflowIdAndPermissionType(UUID workflowId, String permissionType);

    List<WorkflowPermission> findByPermissionTargetTypeAndPermissionTargetId(String targetType, UUID targetId);

    void deleteByWorkflowId(UUID workflowId);

    long countByWorkflowId(UUID workflowId);
}