package com.jira.issue.repository;

import com.jira.issue.entity.CucumberFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CucumberFeatureRepository extends JpaRepository<CucumberFeature, UUID> {

    Optional<CucumberFeature> findByFeatureKey(String featureKey);

    Optional<CucumberFeature> findByFeatureFile(String featureFile);

    Optional<CucumberFeature> findByFeatureName(String featureName);
}