package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.WorkflowScreen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowScreenRepository extends JpaRepository<WorkflowScreen, UUID> {
    Optional<WorkflowScreen> findByName(String name);
    List<WorkflowScreen> findByScreenType(String screenType);
    List<WorkflowScreen> findByIsDefault(Boolean isDefault);
    List<WorkflowScreen> findByIsSystem(Boolean isSystem);
    boolean existsByName(String name);
}