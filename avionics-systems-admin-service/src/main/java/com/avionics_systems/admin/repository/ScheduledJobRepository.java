package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.ScheduledJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ScheduledJobRepository extends JpaRepository<ScheduledJobEntity, String> {
    Optional<ScheduledJobEntity> findByJobId(String jobId);
}