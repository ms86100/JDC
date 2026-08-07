package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.ExecutionStatusConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExecutionStatusConfigRepository extends JpaRepository<ExecutionStatusConfig, UUID> {

    List<ExecutionStatusConfig> findByIsActiveTrueOrderBySortOrderAsc();

    List<ExecutionStatusConfig> findByProjectIdAndIsActiveTrueOrderBySortOrderAsc(UUID projectId);

    List<ExecutionStatusConfig> findByProjectIdIsNullAndIsActiveTrueOrderBySortOrderAsc();

    Optional<ExecutionStatusConfig> findByName(String name);

    boolean existsByName(String name);
}
