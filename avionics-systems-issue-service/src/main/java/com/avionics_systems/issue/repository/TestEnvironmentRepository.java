package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.TestEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestEnvironmentRepository extends JpaRepository<TestEnvironment, UUID> {

    List<TestEnvironment> findByProjectIdOrderBySortOrderAsc(UUID projectId);

    List<TestEnvironment> findByProjectIdAndIsActiveTrueOrderBySortOrderAsc(UUID projectId);

    Optional<TestEnvironment> findByProjectIdAndName(UUID projectId, String name);

    List<TestEnvironment> findByEnvironmentType(String environmentType);
}