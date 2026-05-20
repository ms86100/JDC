package com.jira.test.repository;

import com.jira.test.entity.TestIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestIssueRepository extends JpaRepository<TestIssue, UUID> {

    List<TestIssue> findByProjectIdAndArchivedFalse(UUID projectId);

    List<TestIssue> findByTestSetId(UUID testSetId);

    @Query("SELECT t FROM TestIssue t JOIN t.steps s WHERE t.id = :testId ORDER BY s.stepOrder ASC")
    List<TestIssue> findWithStepsById(@Param("testId") UUID testId);

    boolean existsByProjectIdAndName(UUID projectId, String name);

    @Query("SELECT t FROM TestIssue t JOIN RequirementLink r ON r.testId = t.id WHERE r.requirementKey = :reqKey")
    List<TestIssue> findByRequirementKey(@Param("reqKey") String reqKey);

    @Query("SELECT t FROM TestIssue t WHERE t.gherkinFeatureKey = :featureKey AND t.gherkinScenarioId = :scenarioId")
    List<TestIssue> findByGherkinKey(@Param("featureKey") String featureKey, @Param("scenarioId") String scenarioId);
}