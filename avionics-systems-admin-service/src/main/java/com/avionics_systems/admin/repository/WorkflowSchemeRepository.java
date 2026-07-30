package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.WorkflowSchemeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WorkflowSchemeRepository extends JpaRepository<WorkflowSchemeEntity, String> {
    Optional<WorkflowSchemeEntity> findByName(String name);
}