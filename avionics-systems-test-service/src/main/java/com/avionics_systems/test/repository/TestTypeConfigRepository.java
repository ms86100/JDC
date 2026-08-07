package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.TestTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestTypeConfigRepository extends JpaRepository<TestTypeConfig, UUID> {

    List<TestTypeConfig> findByIsActiveTrueOrderBySortOrderAsc();

    List<TestTypeConfig> findByProjectIdAndIsActiveTrueOrderBySortOrderAsc(UUID projectId);

    List<TestTypeConfig> findByProjectIdIsNullAndIsActiveTrueOrderBySortOrderAsc();

    Optional<TestTypeConfig> findByName(String name);

    boolean existsByName(String name);
}
