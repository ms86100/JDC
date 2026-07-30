package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.DatasetVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DatasetVariableRepository extends JpaRepository<DatasetVariable, UUID> {

    List<DatasetVariable> findByDatasetId(UUID datasetId);

    Optional<DatasetVariable> findByDatasetIdAndVariableName(UUID datasetId, String variableName);

    void deleteByDatasetId(UUID datasetId);
}