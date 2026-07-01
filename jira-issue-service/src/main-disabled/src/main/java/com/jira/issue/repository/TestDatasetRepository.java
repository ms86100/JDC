package com.jira.issue.repository;

import com.jira.issue.entity.TestDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestDatasetRepository extends JpaRepository<TestDataset, UUID> {

    List<TestDataset> findByProjectIdOrderByNameAsc(UUID projectId);

    Optional<TestDataset> findByProjectIdAndName(UUID projectId, String name);

    List<TestDataset> findByDataFormat(String dataFormat);
}