package com.jira.admin.repository;

import com.jira.admin.entity.WorkflowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowEntity, String> {
    Optional<WorkflowEntity> findByName(String name);
}