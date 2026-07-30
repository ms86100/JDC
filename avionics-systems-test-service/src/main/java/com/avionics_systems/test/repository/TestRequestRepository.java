package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.TestRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestRequestRepository extends JpaRepository<TestRequest, UUID> {

    List<TestRequest> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<TestRequest> findByIssueKey(String issueKey);

    List<TestRequest> findByProjectIdAndStatus(UUID projectId, String status);

    long countByProjectId(UUID projectId);

    List<TestRequest> findByProjectId(UUID projectId);
}
