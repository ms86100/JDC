package com.jira.test.repository;

import com.jira.test.entity.DatasetVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DatasetVersionRepository extends JpaRepository<DatasetVersion, UUID> {

    List<DatasetVersion> findByDatasetIdOrderByVersionNumberDesc(UUID datasetId);

    Optional<DatasetVersion> findByDatasetIdAndIsImmutableTrue(UUID datasetId);

    @Query("SELECT MAX(dv.versionNumber) FROM DatasetVersion dv WHERE dv.datasetId = :datasetId")
    Optional<Integer> findMaxVersionByDatasetId(@Param("datasetId") UUID datasetId);

    Optional<DatasetVersion> findByDatasetIdAndVersionNumber(UUID datasetId, Integer versionNumber);
}