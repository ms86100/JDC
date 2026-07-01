package com.jira.issue.repository;

import com.jira.issue.entity.CucumberScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CucumberScenarioRepository extends JpaRepository<CucumberScenario, UUID> {

    Optional<CucumberScenario> findByScenarioKey(String scenarioKey);

    List<CucumberScenario> findByFeatureKey(String featureKey);

    List<CucumberScenario> findByIssueId(UUID issueId);

    List<CucumberScenario> findByTestSetId(UUID testSetId);

    @Query("SELECT cs FROM CucumberScenario cs WHERE cs.issueId IS NULL AND cs.featureKey = :featureKey")
    List<CucumberScenario> findUnlinkedByFeature(@Param("featureKey") String featureKey);

    @Query("SELECT cs FROM CucumberScenario cs WHERE :tag = ANY(cs.tags)")
    List<CucumberScenario> findByTag(@Param("tag") String tag);

    @Query("SELECT cs FROM CucumberScenario cs WHERE cs.importBatchId = :batchId")
    List<CucumberScenario> findByImportBatchId(@Param("batchId") UUID batchId);

    @Query("SELECT DISTINCT cs.featureKey FROM CucumberScenario cs")
    List<String> findDistinctFeatureKeys();

    @Query("SELECT COUNT(cs) FROM CucumberScenario cs WHERE cs.featureKey = :featureKey")
    Long countByFeatureKey(@Param("featureKey") String featureKey);
}