package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.SharedStepVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SharedStepVersionRepository extends JpaRepository<SharedStepVersion, UUID> {

    List<SharedStepVersion> findBySharedStepIdOrderByVersionNumberDesc(UUID sharedStepId);

    Optional<SharedStepVersion> findBySharedStepIdAndIsCurrentTrue(UUID sharedStepId);

    @Query("SELECT MAX(sv.versionNumber) FROM SharedStepVersion sv WHERE sv.sharedStepId = :sharedStepId")
    Optional<Integer> findMaxVersionBySharedStepId(@Param("sharedStepId") UUID sharedStepId);

    Optional<SharedStepVersion> findBySharedStepIdAndVersionNumber(UUID sharedStepId, Integer versionNumber);
}