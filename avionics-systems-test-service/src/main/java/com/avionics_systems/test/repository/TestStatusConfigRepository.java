package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.TestStatusConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestStatusConfigRepository extends JpaRepository<TestStatusConfig, UUID> {

    List<TestStatusConfig> findByIsActiveTrueOrderBySortOrderAsc();

    List<TestStatusConfig> findByProjectIdAndIsActiveTrueOrderBySortOrderAsc(UUID projectId);

    List<TestStatusConfig> findByProjectIdIsNullAndIsActiveTrueOrderBySortOrderAsc();

    Optional<TestStatusConfig> findByName(String name);

    boolean existsByName(String name);

    Optional<TestStatusConfig> findByIsDefaultTrue();
}
