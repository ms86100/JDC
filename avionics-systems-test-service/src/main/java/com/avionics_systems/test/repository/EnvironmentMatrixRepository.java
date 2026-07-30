package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.EnvironmentMatrix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EnvironmentMatrixRepository extends JpaRepository<EnvironmentMatrix, UUID> {

    List<EnvironmentMatrix> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<EnvironmentMatrix> findByProjectIdAndIsActiveTrueOrderByCreatedAtDesc(UUID projectId);

    List<EnvironmentMatrix> findByIsActiveTrue();
}