package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.WorkflowLayoutEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowLayoutEdgeRepository extends JpaRepository<WorkflowLayoutEdge, UUID> {

    List<WorkflowLayoutEdge> findByLayoutIdOrderBySortOrderAsc(UUID layoutId);

    List<WorkflowLayoutEdge> findByLayoutId(UUID layoutId);

    List<WorkflowLayoutEdge> findByTransitionId(UUID transitionId);

    void deleteByLayoutId(UUID layoutId);

    void deleteByTransitionId(UUID transitionId);

    long countByLayoutId(UUID layoutId);
}