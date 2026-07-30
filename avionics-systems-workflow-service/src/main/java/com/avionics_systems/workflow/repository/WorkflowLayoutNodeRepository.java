package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.WorkflowLayoutNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowLayoutNodeRepository extends JpaRepository<WorkflowLayoutNode, UUID> {

    List<WorkflowLayoutNode> findByLayoutIdOrderBySortOrderAsc(UUID layoutId);

    List<WorkflowLayoutNode> findByLayoutId(UUID layoutId);

    List<WorkflowLayoutNode> findByStatusId(UUID statusId);

    void deleteByLayoutId(UUID layoutId);

    long countByLayoutId(UUID layoutId);
}