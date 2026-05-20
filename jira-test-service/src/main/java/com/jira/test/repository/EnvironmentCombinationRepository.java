package com.jira.test.repository;

import com.jira.test.entity.EnvironmentCombination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EnvironmentCombinationRepository extends JpaRepository<EnvironmentCombination, UUID> {

    List<EnvironmentCombination> findByMatrixIdOrderByCombinationIndexAsc(UUID matrixId);

    List<EnvironmentCombination> findByMatrixIdAndIsValidTrue(UUID matrixId);

    List<EnvironmentCombination> findByMatrixIdAndProvisioningStatus(UUID matrixId, String provisioningStatus);

    long countByMatrixId(UUID matrixId);

    long countByMatrixIdAndIsValidTrue(UUID matrixId);

    void deleteByMatrixId(UUID matrixId);
}