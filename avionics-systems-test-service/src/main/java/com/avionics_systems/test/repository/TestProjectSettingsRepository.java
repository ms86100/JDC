package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.TestProjectSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestProjectSettingsRepository extends JpaRepository<TestProjectSettings, UUID> {

    Optional<TestProjectSettings> findByProjectId(UUID projectId);

    boolean existsByProjectId(UUID projectId);
}
