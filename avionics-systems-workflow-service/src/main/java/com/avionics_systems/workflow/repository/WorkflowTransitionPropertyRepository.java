package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.WorkflowTransitionProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowTransitionPropertyRepository extends JpaRepository<WorkflowTransitionProperty, UUID> {

    List<WorkflowTransitionProperty> findByTransitionId(UUID transitionId);

    Optional<WorkflowTransitionProperty> findByTransitionIdAndPropertyKey(UUID transitionId, String propertyKey);

    @Query("SELECT wtp FROM WorkflowTransitionProperty wtp WHERE wtp.transitionId = :transitionId AND wtp.propertyKey LIKE :prefix%")
    List<WorkflowTransitionProperty> findByTransitionIdAndPropertyKeyPrefix(UUID transitionId, String prefix);

    void deleteByTransitionId(UUID transitionId);

    boolean existsByTransitionIdAndPropertyKey(UUID transitionId, String propertyKey);
}