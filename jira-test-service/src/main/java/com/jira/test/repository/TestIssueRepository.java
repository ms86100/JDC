package com.jira.test.repository;

import com.jira.test.entity.TestIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestIssueRepository extends JpaRepository<TestIssue, UUID> {

    List<TestIssue> findByProjectIdAndArchivedFalse(UUID projectId);

    List<TestIssue> findByTestSetId(UUID testSetId);

    // Note: findWithStepsById removed - TestIssue doesn't have steps relationship
    // Use TestStepRepository.findByTestIdOrderByStepOrderAsc() instead

    boolean existsByProjectIdAndName(UUID projectId, String name);

    @Query("SELECT t FROM TestIssue t JOIN RequirementLink r ON r.testId = t.id WHERE r.requirementKey = :reqKey")
    List<TestIssue> findByRequirementKey(@Param("reqKey") String reqKey);

    @Query("SELECT t FROM TestIssue t WHERE t.gherkinFeatureKey = :featureKey AND t.gherkinScenarioId = :scenarioId")
    List<TestIssue> findByGherkinKey(@Param("featureKey") String featureKey, @Param("scenarioId") String scenarioId);

    // Added for FlakyTestDetectionService
    List<TestIssue> findByProjectId(UUID projectId);

    Optional<TestIssue> findByProjectIdAndName(UUID projectId, String name);

    List<TestIssue> findByFolderId(UUID folderId);
}