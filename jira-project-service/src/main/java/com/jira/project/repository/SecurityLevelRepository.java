package com.jira.project.repository;

import com.jira.project.entity.SecurityLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityLevelRepository extends JpaRepository<SecurityLevel, UUID> {
    List<SecurityLevel> findBySchemeId(UUID schemeId);
    List<SecurityLevel> findByProjectIdOrderBySequence(UUID projectId);
}