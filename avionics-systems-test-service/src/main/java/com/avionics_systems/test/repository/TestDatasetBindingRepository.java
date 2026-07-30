package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.TestDatasetBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestDatasetBindingRepository extends JpaRepository<TestDatasetBinding, UUID> {

    List<TestDatasetBinding> findByTestId(UUID testId);

    Optional<TestDatasetBinding> findByTestIdAndDatasetId(UUID testId, UUID datasetId);

    void deleteByTestId(UUID testId);

    void deleteByTestIdAndDatasetId(UUID testId, UUID datasetId);

    List<TestDatasetBinding> findByDatasetId(UUID datasetId);
}