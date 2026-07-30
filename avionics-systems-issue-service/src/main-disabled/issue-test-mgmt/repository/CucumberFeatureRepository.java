package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.CucumberFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CucumberFeatureRepository extends JpaRepository<CucumberFeature, UUID> {

    Optional<CucumberFeature> findByFeatureKey(String featureKey);

    Optional<CucumberFeature> findByFeatureFile(String featureFile);

    Optional<CucumberFeature> findByFeatureNameAndProjectId(String featureName, UUID projectId);
}