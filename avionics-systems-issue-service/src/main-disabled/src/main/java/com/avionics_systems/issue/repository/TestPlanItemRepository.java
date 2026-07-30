package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.TestPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestPlanItemRepository extends JpaRepository<TestPlanItem, UUID> {

    List<TestPlanItem> findByTestPlanIdOrderByExecutionOrderAsc(UUID testPlanId);

    List<TestPlanItem> findByTestSetId(UUID testSetId);

    Optional<TestPlanItem> findByTestPlanIdAndTestSetId(UUID testPlanId, UUID testSetId);

    @Query("SELECT COUNT(tpi) FROM TestPlanItem tpi WHERE tpi.testPlanId = :planId")
    Long countByTestPlanId(@Param("planId") UUID testPlanId);

    @Query("SELECT MAX(tpi.executionOrder) FROM TestPlanItem tpi WHERE tpi.testPlanId = :planId")
    Integer findMaxExecutionOrder(@Param("planId") UUID testPlanId);

    void deleteByTestPlanId(UUID testPlanId);

    void deleteByTestSetId(UUID testSetId);
}