package com.jira.migration.repository;

import com.jira.migration.entity.UserMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserMappingRepository extends JpaRepository<UserMapping, UUID> {

    List<UserMapping> findByJobId(UUID jobId);

    Optional<UserMapping> findByJobIdAndSourceIdentifier(UUID jobId, String sourceIdentifier);

    Optional<UserMapping> findByJobIdAndTargetUserId(UUID jobId, UUID targetUserId);

    List<UserMapping> findByJobIdAndMappingType(UUID jobId, String mappingType);

    boolean existsByJobIdAndSourceIdentifier(UUID jobId, String sourceIdentifier);
}