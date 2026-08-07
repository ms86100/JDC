package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.ExploratorySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExploratorySessionRepository extends JpaRepository<ExploratorySession, UUID> {

    List<ExploratorySession> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<ExploratorySession> findByProjectIdAndStatus(UUID projectId, String status);

    List<ExploratorySession> findByTesterId(UUID testerId);

    long countByProjectId(UUID projectId);
}
