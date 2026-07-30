package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.TestComponentMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestComponentMappingRepository extends JpaRepository<TestComponentMapping, UUID> {

    List<TestComponentMapping> findByTestId(UUID testId);

    List<TestComponentMapping> findByComponentId(UUID componentId);

    Optional<TestComponentMapping> findByTestIdAndComponentId(UUID testId, UUID componentId);

    @Query("SELECT tcm.componentId FROM TestComponentMapping tcm WHERE tcm.testId = :testId")
    List<UUID> findComponentIdsByTestId(@Param("testId") UUID testId);

    @Query("SELECT tcm.testId FROM TestComponentMapping tcm WHERE tcm.componentId = :componentId")
    List<UUID> findTestIdsByComponentId(@Param("componentId") UUID componentId);

    void deleteByTestId(UUID testId);

    void deleteByComponentId(UUID componentId);
}