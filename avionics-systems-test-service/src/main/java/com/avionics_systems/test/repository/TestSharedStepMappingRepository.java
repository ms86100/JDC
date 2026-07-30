package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.TestSharedStepMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestSharedStepMappingRepository extends JpaRepository<TestSharedStepMapping, UUID> {

    List<TestSharedStepMapping> findByTestIdOrderByTestStepIndexAsc(UUID testId);

    List<TestSharedStepMapping> findBySharedStepId(UUID sharedStepId);

    Optional<TestSharedStepMapping> findByTestIdAndTestStepIndex(UUID testId, Integer testStepIndex);

    @Query("SELECT COUNT(t) FROM TestSharedStepMapping t WHERE t.sharedStepId = :sharedStepId")
    long countBySharedStepId(@Param("sharedStepId") UUID sharedStepId);

    void deleteByTestId(UUID testId);

    void deleteBySharedStepId(UUID sharedStepId);

    @Query("SELECT DISTINCT t.testId FROM TestSharedStepMapping t WHERE t.sharedStepId = :sharedStepId")
    List<UUID> findTestIdsBySharedStepId(@Param("sharedStepId") UUID sharedStepId);
}