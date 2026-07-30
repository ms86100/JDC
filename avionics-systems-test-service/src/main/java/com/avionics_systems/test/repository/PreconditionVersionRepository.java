package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.PreconditionVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreconditionVersionRepository extends JpaRepository<PreconditionVersion, UUID> {

    List<PreconditionVersion> findByPreconditionIdOrderByVersionNumberDesc(UUID preconditionId);

    Optional<PreconditionVersion> findByPreconditionIdAndVersionNumber(UUID preconditionId, Integer versionNumber);

    void deleteByPreconditionId(UUID preconditionId);
}