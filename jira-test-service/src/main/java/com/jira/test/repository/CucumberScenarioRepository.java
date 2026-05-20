package com.jira.test.repository;

import com.jira.test.entity.CucumberScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CucumberScenarioRepository extends JpaRepository<CucumberScenario, UUID> {

    List<CucumberScenario> findByFeatureKey(String featureKey);

    Optional<CucumberScenario> findByFeatureKeyAndScenarioName(String featureKey, String scenarioName);

    List<CucumberScenario> findByTestId(UUID testId);

    Optional<CucumberScenario> findByFeatureKeyAndScenarioIdIsNotNull(String featureKey);
}