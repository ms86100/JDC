package com.jira.test.repository;

import com.jira.test.entity.TestPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestPlanItemRepository extends JpaRepository<TestPlanItem, UUID> {

    List<TestPlanItem> findByTestPlanIdOrderByExecutionOrderAsc(UUID testPlanId);

    List<TestPlanItem> findByTestSetId(UUID testSetId);

    void deleteByTestPlanId(UUID testPlanId);
}