package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.TestPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestPlanRepository extends JpaRepository<TestPlan, UUID> {

    List<TestPlan> findByProjectId(UUID projectId);

    List<TestPlan> findByProjectIdAndStatus(UUID projectId, String status);
}