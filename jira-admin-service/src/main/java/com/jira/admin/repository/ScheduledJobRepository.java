package com.jira.admin.repository;

import com.jira.admin.entity.ScheduledJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ScheduledJobRepository extends JpaRepository<ScheduledJobEntity, String> {
    Optional<ScheduledJobEntity> findByJobId(String jobId);
}