package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.TestDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestDatasetRepository extends JpaRepository<TestDataset, UUID> {

    List<TestDataset> findByProjectIdAndArchivedFalse(UUID projectId);

    Optional<TestDataset> findByIdAndArchivedFalse(UUID id);

    List<TestDataset> findByProjectIdAndFolderIdAndArchivedFalse(UUID projectId, UUID folderId);

    boolean existsByProjectIdAndNameAndArchivedFalse(UUID projectId, String name);

    List<TestDataset> findByProjectIdAndNameContainingIgnoreCaseAndArchivedFalse(UUID projectId, String name);
}